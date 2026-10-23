package app.uamo.ynotes.ui

import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.uamo.ynotes.ui.screens.*
import app.uamo.ynotes.utils.SharedTransitionProvider
import app.uamo.ynotes.utils.sharedComposable

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
    onWelcomeCompleted: () -> Unit,
    pendingDeepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var lastNavTimestamp by remember { mutableLongStateOf(0L) }

    fun safeNavigate(route: String, fromEntry: NavBackStackEntry? = null) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavTimestamp < 400L) return
        if (fromEntry != null && !fromEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (navController.currentDestination?.route == route) return
        lastNavTimestamp = now
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun safePopBackStack(fromEntry: NavBackStackEntry? = null) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavTimestamp < 400L) return
        if (fromEntry != null && !fromEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (navController.previousBackStackEntry != null) {
            lastNavTimestamp = now
            navController.popBackStack()
        }
    }

    LaunchedEffect(pendingDeepLinkRoute) {
        if (!pendingDeepLinkRoute.isNullOrBlank()) {
            navController.navigate(pendingDeepLinkRoute) {
                launchSingleTop = true
            }
            onDeepLinkHandled()
        }
    }

    val isBooksEnabled by viewModel.isBooksEnabled.collectAsStateWithLifecycle()
    val isSafeZoneUnlocked by viewModel.isSafeZoneUnlocked.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()

    val executor = remember(context) { ContextCompat.getMainExecutor(context) }

    val navigateToSafeZone = {
        viewModel.unlockSafeZone()
        safeNavigate("safe_zone")
    }

    // Full safe zone request (respects biometric setting)
    val onRequestSafeZone: () -> Unit = {
        if (isBiometricEnabled.value && activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                try {
                    val biometricPrompt = BiometricPrompt(activity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                navigateToSafeZone()
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                                    navigateToSafeZone()
                                }
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Zona Segura")
                        .setSubtitle("Confirma tu identidad para acceder")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    navigateToSafeZone()
                }
            } else {
                navigateToSafeZone()
            }
        } else {
            navigateToSafeZone()
        }
    }

    // Biometric-only: skips password, goes straight to fingerprint
    val onRequestSafeZoneBiometric: () -> Unit = {
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuthStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            val canAuthCredential = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuthStrong == BiometricManager.BIOMETRIC_SUCCESS) {
                try {
                    val biometricPrompt = BiometricPrompt(activity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                navigateToSafeZone()
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Zona Segura")
                        .setSubtitle("Confirma tu huella para acceder")
                        .setNegativeButtonText("Cancelar")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    onRequestSafeZone()
                }
            } else if (canAuthCredential == BiometricManager.BIOMETRIC_SUCCESS) {
                onRequestSafeZone()
            } else {
                navigateToSafeZone()
            }
        } else {
            navigateToSafeZone()
        }
    }

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
                    launchSingleTop = true
                }
            }
        }
    }

    SharedTransitionProvider {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            sharedComposable("welcome") {
                WelcomeScreen(
                    onStart = {
                        onWelcomeCompleted()
                        navController.navigate("home") {
                            popUpTo("welcome") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            sharedComposable("home") { backStackEntry ->
                val publicNotes by viewModel.publicNotes.collectAsStateWithLifecycle()
                val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
                val homeSortOrder by viewModel.homeSortOrder.collectAsStateWithLifecycle()

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
                        safeNavigate("editor/public/new", backStackEntry)
                    },
                    onNoteClick = { note ->
                        safeNavigate("editor/public/${note.id}", backStackEntry)
                    },
                    onSettingsClick = {
                        safeNavigate("settings/home", backStackEntry)
                    },
                    isBooksEnabled = isBooksEnabled,
                    books = books,
                    onBooksClick = {
                        safeNavigate("books/public", backStackEntry)
                    },
                    onTrashClick = {
                        safeNavigate("trash", backStackEntry)
                    },
                    onDeleteNotes = { ids ->
                        viewModel.deleteNotes(ids)
                    },
                    onRestoreNote = { id ->
                        viewModel.restoreFromTrash(id)
                    },
                    onToggleWidgetSpecial = { id ->
                        viewModel.toggleWidgetSpecial(id)
                    },
                    onSetNotesWidgetSpecial = { ids, isSpecial ->
                        viewModel.setNotesWidgetSpecial(ids, isSpecial)
                    }
                )
            }

            sharedComposable("safe_zone") { backStackEntry ->
                val secretNotes by viewModel.secretNotes.collectAsStateWithLifecycle()
                val safeSearchQuery by viewModel.safeSearchQuery.collectAsStateWithLifecycle()
                val safeSortOrder by viewModel.safeSortOrder.collectAsStateWithLifecycle()
                val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()

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
                            launchSingleTop = true
                        }
                    },
                    onAddNote = {
                        safeNavigate("editor/secret/new", backStackEntry)
                    },
                    onNoteClick = { note ->
                        safeNavigate("editor/secret/${note.id}", backStackEntry)
                    },
                    onSettingsClick = {
                        safeNavigate("settings/safe_zone", backStackEntry)
                    },
                    isBooksEnabled = isBooksEnabled,
                    hiddenApps = hiddenApps,
                    onToggleHiddenApp = { pkg, name -> viewModel.toggleHiddenApp(pkg, name) },
                    onRemoveHiddenApp = { pkg -> viewModel.removeHiddenApp(pkg) },
                    onBooksClick = {
                        safeNavigate("books/secret", backStackEntry)
                    },
                    onTrashClick = {
                        safeNavigate("trash", backStackEntry)
                    }
                )
            }

            sharedComposable(
                route = "editor/{type}/{noteId}?bookId={bookId}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("noteId") { type = NavType.StringType },
                    navArgument("bookId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "public"
                val isSecret = type == "secret"
                val noteId = backStackEntry.arguments?.getString("noteId")
                val bookIdArg = backStackEntry.arguments?.getString("bookId")

                // Resolve note once on entry without subscribing to continuous auto-save emissions
                val initialNote = remember(noteId, isSecret) {
                    if (noteId != null && noteId != "new") {
                        val list = if (isSecret) viewModel.secretNotes.value else viewModel.publicNotes.value
                        list.find { it.id == noteId }
                    } else null
                }
                val zoneBooks = remember(books, isSecret) {
                    books.filter { it.isSecret == isSecret }
                }

                EditorScreen(
                    editingNote = initialNote,
                    initialBookId = bookIdArg,
                    isSecret = isSecret,
                    isBooksEnabled = isBooksEnabled,
                    books = zoneBooks,
                    onSave = { id, title, body, color, isPinned, bookId, isBodyHidden, mediaFiles, expiresAt, isWidgetSpecial ->
                        val existingCreatedAt = initialNote?.createdAt
                            ?: (if (isSecret) viewModel.secretNotes.value else viewModel.publicNotes.value)
                                .find { it.id == id }?.createdAt
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
                        safePopBackStack(backStackEntry)
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
                                launchSingleTop = true
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
                        safePopBackStack(backStackEntry)
                    }
                )
            }

            sharedComposable(
                route = "books/{zone}",
                arguments = listOf(navArgument("zone") { type = NavType.StringType })
            ) { backStackEntry ->
                val zone = backStackEntry.arguments?.getString("zone") ?: "public"
                val isSecret = zone == "secret"
                val zoneBooks = remember(books, isSecret) {
                    books.filter { it.isSecret == isSecret }
                }

                BooksScreen(
                    books = zoneBooks,
                    onAddBook = { name, color ->
                        viewModel.saveBook(id = null, name = name, color = color, iconName = "MenuBook", isSecret = isSecret)
                    },
                    onDeleteBook = { id ->
                        viewModel.deleteBook(id)
                    },
                    onBookClick = { bookId ->
                        safeNavigate("book_notes/$bookId", backStackEntry)
                    },
                    onNavigateBack = {
                        safePopBackStack(backStackEntry)
                    }
                )
            }

            sharedComposable(
                route = "book_notes/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: return@sharedComposable
                val book = books.find { it.id == bookId } ?: return@sharedComposable

                val publicNotes by viewModel.publicNotes.collectAsStateWithLifecycle()
                val secretNotes by viewModel.secretNotes.collectAsStateWithLifecycle()
                val allNotes = if (book.isSecret) secretNotes else publicNotes
                val bookNotes = remember(allNotes, bookId) {
                    allNotes.filter { it.bookId == bookId }
                }

                BookNotesScreen(
                    book = book,
                    notes = bookNotes,
                    onNoteClick = { note ->
                        safeNavigate("editor/${if (book.isSecret) "secret" else "public"}/${note.id}?bookId=${book.id}", backStackEntry)
                    },
                    onAddNote = {
                        safeNavigate("editor/${if (book.isSecret) "secret" else "public"}/new?bookId=${book.id}", backStackEntry)
                    },
                    onNavigateBack = {
                        safePopBackStack(backStackEntry)
                    }
                )
            }

            sharedComposable("trash") { backStackEntry ->
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
                        safePopBackStack(backStackEntry)
                    }
                )
            }
        }
    }
}
