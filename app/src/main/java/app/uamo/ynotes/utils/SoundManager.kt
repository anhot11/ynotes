package app.uamo.ynotes.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import app.uamo.ynotes.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * SoundManager provides low-latency, tactile auditory feedback for yNotes.
 *
 * It uses SoundPool configured with USAGE_ASSISTANCE_SONIFICATION to:
 * 1. Respect device silent, vibrate, and DND modes automatically.
 * 2. Play subtle, organic sounds with zero UI thread overhead.
 * 3. Provide subtle micro-variations (pitch modulation) on tap sounds to prevent ear fatigue.
 * 4. Persist user preferences in SharedPreferences ("SOUND_EFFECTS_ENABLED", default: true).
 */
object SoundManager {

    private const val PREFS_NAME = "yNotesPrefs"
    const val PREF_SOUND_ENABLED = "SOUND_EFFECTS_ENABLED"

    private var soundPool: SoundPool? = null
    private var tapSoundId: Int = 0
    private var deleteSoundId: Int = 0
    private var unlockSoundId: Int = 0

    private val loadedSounds = ConcurrentHashMap.newKeySet<Int>()
    private var isInitialized = false

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    /**
     * Initializes the SoundPool and pre-loads the audio cues into native memory.
     */
    fun init(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Read initial preference (default: true)
        _isSoundEnabled.value = prefs.getBoolean(PREF_SOUND_ENABLED, true)

        if (prefsListener == null) {
            prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == PREF_SOUND_ENABLED) {
                    _isSoundEnabled.value = sharedPreferences.getBoolean(PREF_SOUND_ENABLED, true)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        }

        if (isInitialized && soundPool != null) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            loadedSounds.add(sampleId)
                        }
                    }
                }

            soundPool?.let { pool ->
                tapSoundId = pool.load(appContext, R.raw.sound_tap, 1)
                deleteSoundId = pool.load(appContext, R.raw.sound_delete, 1)
                unlockSoundId = pool.load(appContext, R.raw.sound_unlock, 1)
            }

            isInitialized = true
        } catch (_: Throwable) {
            // Graceful fallback if device audio hardware is unavailable
        }
    }

    /**
     * Updates and persists the sound effects setting.
     */
    fun setSoundEnabled(context: Context, enabled: Boolean) {
        _isSoundEnabled.value = enabled
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_SOUND_ENABLED, enabled).apply()
    }

    /**
     * Plays the subtle selection "tap" sound with micro-pitch modulation.
     * Duration: ~50ms.
     */
    fun playTap() {
        if (!_isSoundEnabled.value) return
        val pool = soundPool ?: return
        if (tapSoundId == 0 || !loadedSounds.contains(tapSoundId)) return

        try {
            // Subtle pitch modulation (+/- 4%) so rapid consecutive taps feel alive and organic
            val pitch = 0.96f + (Random.nextFloat() * 0.08f)
            val volume = 0.70f + (Random.nextFloat() * 0.06f)
            pool.play(tapSoundId, volume, volume, 1, 0, pitch)
        } catch (_: Throwable) {}
    }

    /**
     * Plays the gentle note deletion "whoosh" sound.
     * Duration: ~180ms.
     */
    fun playDelete() {
        if (!_isSoundEnabled.value) return
        val pool = soundPool ?: return
        if (deleteSoundId == 0 || !loadedSounds.contains(deleteSoundId)) return

        try {
            pool.play(deleteSoundId, 0.78f, 0.78f, 2, 0, 1.0f)
        } catch (_: Throwable) {}
    }

    /**
     * Plays the harmonic two-tone chime when Safe Zone / Vault is unlocked.
     * Duration: ~260ms.
     */
    fun playUnlock() {
        if (!_isSoundEnabled.value) return
        val pool = soundPool ?: return
        if (unlockSoundId == 0 || !loadedSounds.contains(unlockSoundId)) return

        try {
            pool.play(unlockSoundId, 0.85f, 0.85f, 3, 0, 1.0f)
        } catch (_: Throwable) {}
    }

    /**
     * Clean up SoundPool when the application process terminates.
     */
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            loadedSounds.clear()
            isInitialized = false
        } catch (_: Throwable) {}
    }
}
