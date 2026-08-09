package app.uamo.ynotes.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.uamo.ynotes.ui.screens.*
import app.uamo.ynotes.utils.SharedTransitionProvider
import app.uamo.ynotes.utils.SharedTransitionContainer
import app.uamo.ynotes.utils.sharedComposable

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun AppNavigation(
    viewModel: NotesViewModel,
    startDestination: String,
    safeZonePassword: MutableState<String>,
    safeZoneTriggerMode: State<Int>,
    isBiometricEnabled: MutableState<Boolean>,
    isAppHidingEnabled: MutableState<Int>,
    currentThemeType: State<Int>,
    onSaveSafeZone: (String, Int) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onAppHidingToggle: (Int) -> Unit,
    onThemeChanged: (Int) -> Unit,
    onWelcomeCompleted: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    val publicNotes by viewModel.publicNotes.collectAsStateWithLifecycle()
    val secretNotes by viewModel.secretNotes.collectAsStateWithLifecycle()
    val isBooksEnabled by viewModel.isBooksEnabled.collectAsStateWithLifecycle()
    
    val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
    val homeSortOrder by viewModel.homeSortOrder.collectAsStateWithLifecycle()
    val safeSearchQuery by viewModel.safeSearchQuery.collectAsStateWithLifecycle()
    val safeSortOrder by viewModel.safeSortOrder.collectAsStateWithLifecycle()
    
    val executor = ContextCompat.getMainExecutor(context)

    // Full safe zone request (respects biometric setting)
    val onRequestSafeZone: () -> Unit = {
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.unlockSafeZone()
                            navController.navigate("safe_zone")
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Zona Segura")
                    .setSubtitle("Confirma tu identidad para acceder")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                viewModel.unlockSafeZone()
                navController.navigate("safe_zone")
            }
        } else {
            viewModel.unlockSafeZone()
            navController.navigate("safe_zone")
        }
    }

    // Biometric-only: skips password, goes straight to fingerprint
    val onRequestSafeZoneBiometric: () -> Unit = {
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.unlockSafeZone()
                            navController.navigate("safe_zone")
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Zona Segura")
                    .setSubtitle("Confirma tu huella para acceder")
                    .setNegativeButtonText("Cancelar")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                // Fallback if biometric not available
                viewModel.unlockSafeZone()
                navController.navigate("safe_zone")
            }
        } else {
            viewModel.unlockSafeZone()
            navController.navigate("safe_zone")
        }
    }

    val isSafeZoneUnlocked by viewModel.isSafeZoneUnlocked.collectAsStateWithLifecycle()

    LaunchedEffect(isSafeZoneUnlocked) {
        if (!isSafeZoneUnlocked) {
            val currentRoute = navController.currentDestination?.route ?: ""
            val isSecretBookNote = if (currentRoute.startsWith("book_notes")) {
                val bookId = navController.currentBackStackEntry?.arguments?.getString("bookId")
                viewModel.books.value.find { it.id == bookId }?.isSecret == true
            } else false

            if (currentRoute == "safe_zone" || 
                currentRoute.startsWith("editor/secret") || 
                currentRoute == "settings/safe_zone" || 
                currentRoute == "books/secret" || 
                isSecretBookNote) {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    SharedTransitionProvider {
        NavHost(navController = navController, startDestination = startDestination) {
            
            sharedComposable("welcome") {
            WelcomeScreen(
                onStart = {
                    onWelcomeCompleted()
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        sharedComposable("home") {
            HomeScreen(
                notes = publicNotes,
                searchQuery = homeSearchQuery,
                onSearchQueryChange = { viewModel.homeSearchQuery.value = it },
                sortOrder = homeSortOrder,
                onSortOrderChange = { viewModel.homeSortOrder.value = it },
                safeZonePassword = safeZonePassword.value,
                safeZoneTriggerMode = safeZoneTriggerMode.value,
                isBiometricEnabled = isBiometricEnabled.value,
                onRequestSafeZone = onRequestSafeZone,
                onRequestSafeZoneBiometric = onRequestSafeZoneBiometric,
                onAddNote = {
                    navController.navigate("editor/public/new")
                },
                onNoteClick = { note ->
                    navController.navigate("editor/public/${note.id}")
                },
                onSettingsClick = {
                    navController.navigate("settings/home")
                },
                isBooksEnabled = isBooksEnabled,
                onBooksClick = {
                    navController.navigate("books/public")
                },
                onTrashClick = {
                    navController.navigate("trash")
                }
            )
        }

        sharedComposable("safe_zone") {
            SafeZoneScreen(
                notes = secretNotes,
                searchQuery = safeSearchQuery,
                onSearchQueryChange = { viewModel.safeSearchQuery.value = it },
                sortOrder = safeSortOrder,
                onSortOrderChange = { viewModel.safeSortOrder.value = it },
                isAppHidingEnabled = isAppHidingEnabled.value,
                onDeactivateSafeZone = {
                    viewModel.lockSafeZone()
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAddNote = {
                    navController.navigate("editor/secret/new")
                },
                onNoteClick = { note ->
                    navController.navigate("editor/secret/${note.id}")
                },
                onSettingsClick = {
                    navController.navigate("settings/safe_zone")
                },
                isBooksEnabled = isBooksEnabled,
                onBooksClick = {
                    navController.navigate("books/secret")
                },
                onTrashClick = {
                    navController.navigate("trash")
                }
            )
        }

        sharedComposable(
            route = "editor/{type}/{noteId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("noteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "public"
            val isSecret = type == "secret"
            val noteId = backStackEntry.arguments?.getString("noteId")
            
            val editingNote = if (noteId != "new") {
                if (isSecret) secretNotes.find { it.id == noteId }
                else publicNotes.find { it.id == noteId }
            } else null

            val books by viewModel.books.collectAsStateWithLifecycle()

            EditorScreen(
                editingNote = editingNote,
                isSecret = isSecret,
                isBooksEnabled = isBooksEnabled,
                books = books.filter { it.isSecret == isSecret },
                onSave = { id, title, body, color, isPinned, bookId, isBodyHidden, mediaFiles, expiresAt, isWidgetSpecial ->
                    // id is always the stableNoteId from EditorScreen (never null)
                    // Preserve createdAt from existing note if it exists
                    val existingCreatedAt = if (isSecret) secretNotes.find { it.id == id }?.createdAt
                                           else publicNotes.find { it.id == id }?.createdAt
                    viewModel.saveNote(
                        id = id,
                        title = title,
                        body = body,
                        isSecret = isSecret,
                        color = color,
                        isPinned = isPinned,
                        bookId = bookId,
                        isBodyHidden = isBodyHidden,
                        existingCreatedAt = existingCreatedAt,
                        mediaFiles = mediaFiles,
                        expiresAt = expiresAt,
                        isWidgetSpecial = isWidgetSpecial
                    )
                },
                onDelete = { id ->
                    viewModel.deleteNote(id)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        sharedComposable(
            route = "settings/{from}",
            arguments = listOf(navArgument("from") { type = NavType.StringType })
        ) { backStackEntry ->
            val from = backStackEntry.arguments?.getString("from") ?: "home"
            val isFromSafeZone = from == "safe_zone"
            
            SettingsScreen(
                isFromSafeZone = isFromSafeZone,
                currentPassword = safeZonePassword.value,
                currentTriggerMode = safeZoneTriggerMode.value,
                onSaveSafeZone = { newPwd, mode ->
                    onSaveSafeZone(newPwd, mode)
                    if (newPwd.isEmpty() && isFromSafeZone) {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                isBiometricEnabled = isBiometricEnabled.value,
                isAppHidingEnabled = isAppHidingEnabled.value,
                onBiometricToggle = { enabled ->
                    onBiometricToggle(enabled)
                },
                onAppHidingToggle = { mode ->
                    onAppHidingToggle(mode)
                },
                currentThemeType = currentThemeType.value,
                onThemeChanged = { newThemeIdx ->
                    onThemeChanged(newThemeIdx)
                },
                isBooksEnabled = isBooksEnabled,
                onBooksToggle = { enabled ->
                    viewModel.toggleBooksEnabled(enabled)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        sharedComposable(
            route = "books/{zone}",
            arguments = listOf(navArgument("zone") { type = NavType.StringType })
        ) { backStackEntry ->
            val zone = backStackEntry.arguments?.getString("zone") ?: "public"
            val isSecret = zone == "secret"
            val allBooks by viewModel.books.collectAsStateWithLifecycle()
            val books = allBooks.filter { it.isSecret == isSecret }
            
            BooksScreen(
                books = books,
                onAddBook = { name, color ->
                    viewModel.saveBook(id = null, name = name, color = color, iconName = "MenuBook", isSecret = isSecret)
                },
                onDeleteBook = { id ->
                    viewModel.deleteBook(id)
                },
                onBookClick = { bookId ->
                    navController.navigate("book_notes/$bookId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        sharedComposable(
            route = "book_notes/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@sharedComposable
            val books by viewModel.books.collectAsStateWithLifecycle()
            val book = books.find { it.id == bookId } ?: return@sharedComposable

            val allNotes = if (book.isSecret) secretNotes else publicNotes
            val bookNotes = allNotes.filter { it.bookId == bookId }

            BookNotesScreen(
                book = book,
                notes = bookNotes,
                onNoteClick = { note ->
                    navController.navigate("editor/${if (book.isSecret) "secret" else "public"}/${note.id}")
                },
                onAddNote = {
                    navController.navigate("editor/${if (book.isSecret) "secret" else "public"}/new")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        sharedComposable("trash") {
            val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
            TrashScreen(
                deletedNotes = deletedNotes,
                onRestore = { id ->
                    viewModel.restoreFromTrash(id)
                },
                onDeletePermanently = { id ->
                    viewModel.deleteNotePermanently(id)
                },
                onEmptyTrash = {
                    viewModel.emptyTrash()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
    }
}
