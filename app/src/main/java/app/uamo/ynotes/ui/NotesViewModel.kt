package app.uamo.ynotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.data.applySortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import app.uamo.ynotes.utils.CryptoManager
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.widget.YNotesWidgetReceiver
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = NoteDatabase.getDatabase(application).noteDao()
    private val sharedPrefs = application.getSharedPreferences("yNotesPrefs", android.content.Context.MODE_PRIVATE)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            MediaManager.cleanOrphanedTempFiles(application)
        }
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                noteDao.deleteExpiredNotes()
                delay(60_000) // check every minute
            }
        }
    }

    private val _isBooksEnabled = MutableStateFlow(sharedPrefs.getBoolean("isBooksEnabled", false))
    val isBooksEnabled: StateFlow<Boolean> = _isBooksEnabled.asStateFlow()

    fun toggleBooksEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("isBooksEnabled", enabled).apply()
        _isBooksEnabled.value = enabled
    }

    // ──────────────────────────────────────────────
    // FILTER STATE
    // ──────────────────────────────────────────────
    val homeSearchQuery = MutableStateFlow("")
    val homeSortOrder = MutableStateFlow(app.uamo.ynotes.data.SortOrder.DATE_MODIFIED_DESC)
    
    val safeSearchQuery = MutableStateFlow("")
    val safeSortOrder = MutableStateFlow(app.uamo.ynotes.data.SortOrder.DATE_MODIFIED_DESC)

    // ──────────────────────────────────────────────
    // PUBLIC NOTES — Eagerly cached for instant load
    // ──────────────────────────────────────────────
    val publicNotes: StateFlow<List<NoteEntity>> = combine(
        noteDao.getPublicNotes(),
        homeSearchQuery,
        homeSortOrder
    ) { notes, query, order ->
        val filtered = if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.body.contains(query, ignoreCase = true)
        }
        filtered.applySortOrder(order)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ──────────────────────────────────────────────
    // SAFE ZONE — Locked by default, decrypt on unlock
    // ──────────────────────────────────────────────
    private val _isSafeZoneUnlocked = MutableStateFlow(false)
    val isSafeZoneUnlocked: StateFlow<Boolean> = _isSafeZoneUnlocked.asStateFlow()

    // Raw encrypted notes from DB (always flowing)
    private val _rawSecretNotes: StateFlow<List<NoteEntity>> = noteDao.getSecretNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Decrypted cache — only populated when unlocked
    private val _decryptedSecretNotes = MutableStateFlow<List<NoteEntity>>(emptyList())

    // Public-facing: combines lock state with decrypted cache
    val secretNotes: StateFlow<List<NoteEntity>> = combine(
        _isSafeZoneUnlocked,
        _decryptedSecretNotes,
        safeSearchQuery,
        safeSortOrder
    ) { unlocked, notes, query, order ->
        if (unlocked) {
            val filtered = if (query.isBlank()) notes
            else notes.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.body.contains(query, ignoreCase = true)
            }
            filtered.applySortOrder(order)
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ──────────────────────────────────────────────
    // DELETED NOTES
    // ──────────────────────────────────────────────
    private val deletedNotesDecryptedCache = java.util.concurrent.ConcurrentHashMap<String, NoteEntity>()
    private val decryptedSecretNotesCache = java.util.concurrent.ConcurrentHashMap<String, NoteEntity>()

    val deletedNotes: StateFlow<List<NoteEntity>> = noteDao.getDeletedNotes()
        .combine(_isSafeZoneUnlocked) { notes, unlocked ->
            if (unlocked) {
                val secretNotes = notes.filter { it.isSecret }
                val toDecrypt = secretNotes.filter { 
                    !deletedNotesDecryptedCache.containsKey(it.id) || 
                    deletedNotesDecryptedCache[it.id]?.updatedAt != it.updatedAt 
                }
                
                if (toDecrypt.isNotEmpty()) {
                    val decrypted = withContext(Dispatchers.Default) {
                        CryptoManager.decryptBatch(toDecrypt)
                    }
                    decrypted.forEach { deletedNotesDecryptedCache[it.id] = it }
                }
                
                notes.map { note -> 
                    if (note.isSecret) deletedNotesDecryptedCache[note.id] ?: note else note
                }
            } else {
                notes.map { note ->
                    if (note.isSecret) {
                        note.copy(title = "🔒", body = "")
                    } else {
                        note
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val books: StateFlow<List<BookEntity>> = noteDao.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,  // Also cached eagerly
            initialValue = emptyList()
        )

    // ──────────────────────────────────────────────
    // SAFE ZONE SESSION CONTROL
    // ──────────────────────────────────────────────

    /**
     * Called after successful biometric authentication.
     * Incrementally decrypts secret notes into memory without re-decrypting unchanged notes.
     */
    private var syncJob: kotlinx.coroutines.Job? = null

    fun unlockSafeZone() {
        _isSafeZoneUnlocked.value = true
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _rawSecretNotes.collect { rawNotes ->
                if (_isSafeZoneUnlocked.value) {
                    val toDecrypt = rawNotes.filter { note ->
                        decryptedSecretNotesCache[note.id]?.updatedAt != note.updatedAt
                    }
                    if (toDecrypt.isNotEmpty()) {
                        val newlyDecrypted = withContext(Dispatchers.Default) {
                            CryptoManager.decryptBatch(toDecrypt)
                        }
                        newlyDecrypted.forEach { decryptedSecretNotesCache[it.id] = it }
                    }
                    val validIds = rawNotes.map { it.id }.toSet()
                    decryptedSecretNotesCache.keys.retainAll(validIds)
                    _decryptedSecretNotes.value = rawNotes.mapNotNull { decryptedSecretNotesCache[it.id] }
                }
            }
        }
    }

    /**
     * Called when user exits Safe Zone.
     * Clears decrypted data and keys from memory immediately.
     */
    fun lockSafeZone() {
        _isSafeZoneUnlocked.value = false
        _decryptedSecretNotes.value = emptyList()
        decryptedSecretNotesCache.clear()
        deletedNotesDecryptedCache.clear()
        CryptoManager.clearKeyCache()
        MediaManager.clearCache()
        MediaManager.cleanOrphanedTempFiles(getApplication())
        cancelAutoLock()
    }

    private var autoLockJob: Job? = null

    /**
     * Starts a timer when the app goes into the background.
     * If the user doesn't return within [delayMillis] (default 3000ms = 3 seconds),
     * the Safe Zone will be automatically locked and decrypted notes purged from memory.
     */
    fun scheduleAutoLock(delayMillis: Long = 3000L) {
        if (!_isSafeZoneUnlocked.value) return
        autoLockJob?.cancel()
        autoLockJob = viewModelScope.launch {
            delay(delayMillis)
            lockSafeZone()
        }
    }

    /**
     * Cancels the pending auto-lock timer when the app returns to the foreground.
     */
    fun cancelAutoLock() {
        autoLockJob?.cancel()
        autoLockJob = null
    }

    // ──────────────────────────────────────────────
    // NOTE OPERATIONS
    // ──────────────────────────────────────────────

    private fun notifyWidgetUpdate() {
        try {
            val app = getApplication<Application>()
            app.sendBroadcast(
                android.content.Intent(app, YNotesWidgetReceiver::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
            )
        } catch (_: Throwable) {}
    }

    fun saveNote(
        id: String?, 
        title: String, 
        body: String, 
        isSecret: Boolean, 
        color: Long = 0L, 
        isPinned: Boolean = false, 
        bookId: String? = null,
        isBodyHidden: Boolean = false,
        existingCreatedAt: Long? = null,
        mediaFiles: String = "",
        expiresAt: Long? = null,
        isWidgetSpecial: Boolean = false
    ) {
        if (title.isBlank() && body.isBlank()) return
        
        val noteId = id ?: UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        viewModelScope.launch(Dispatchers.Default) {
            val (finalTitle, finalBody) = if (isSecret) {
                CryptoManager.encryptFields(title, body)
            } else {
                Pair(title, body)
            }
            
            withContext(Dispatchers.IO) {
                noteDao.insertNote(
                    NoteEntity(
                        id = noteId,
                        title = finalTitle,
                        body = finalBody,
                        isSecret = isSecret,
                        color = color,
                        createdAt = existingCreatedAt ?: currentTime,
                        updatedAt = currentTime,
                        isPinned = isPinned,
                        bookId = bookId,
                        isDeleted = false,
                        isBodyHidden = isBodyHidden,
                        mediaFiles = mediaFiles,
                        expiresAt = expiresAt,
                        isWidgetSpecial = isWidgetSpecial
                    )
                )
                notifyWidgetUpdate()
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.moveToTrash(id)
            notifyWidgetUpdate()
        }
    }

    fun deleteNotes(ids: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { noteDao.moveToTrash(it) }
            notifyWidgetUpdate()
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.restoreFromTrash(id)
            notifyWidgetUpdate()
        }
    }

    fun deleteNotePermanently(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = deletedNotes.value.find { it.id == id }
            if (note != null) {
                MediaManager.deleteNoteMedia(getApplication(), note.id, note.isSecret)
            }
            noteDao.deleteNote(id)
            notifyWidgetUpdate()
        }
    }
    
    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            val trashNotes = deletedNotes.value
            trashNotes.forEach { note ->
                MediaManager.deleteNoteMedia(getApplication(), note.id, note.isSecret)
            }
            noteDao.emptyTrash()
            notifyWidgetUpdate()
        }
    }

    fun saveBook(id: String?, name: String, color: Long, iconName: String, isSecret: Boolean = false) {
        if (name.isBlank()) return
        val bookId = id ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            noteDao.insertBook(
                BookEntity(
                    id = bookId,
                    name = name,
                    color = color,
                    iconName = iconName,
                    isSecret = isSecret
                )
            )
        }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch {
            noteDao.removeBookFromNotes(id)
            noteDao.deleteBook(id)
        }
    }
}
