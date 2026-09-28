package app.uamo.ynotes

import androidx.compose.foundation.background
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import app.uamo.ynotes.ui.AppNavigation
import app.uamo.ynotes.ui.NotesViewModel
import app.uamo.ynotes.ui.theme.YNotesTheme
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.utils.SoundManager

class MainActivity : FragmentActivity() {

    private var pendingNavigateRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        pendingNavigateRoute = intent?.getStringExtra("navigate_route")
        SoundManager.init(applicationContext)
        setContent {
            val context = LocalContext.current
            val sharedPref = remember { context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
            val themeTypeIndex = remember { mutableStateOf(sharedPref.getInt("APP_THEME", 0)) }
            val currentTheme = AppThemeType.entries.getOrElse(themeTypeIndex.value) { AppThemeType.AMOLED }

            YNotesTheme(themeType = currentTheme) {
                val notesViewModel: NotesViewModel = viewModel()
                
                androidx.compose.runtime.DisposableEffect(notesViewModel) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                                notesViewModel.scheduleAutoLock(3000L)
                            }
                            androidx.lifecycle.Lifecycle.Event.ON_START -> {
                                notesViewModel.cancelAutoLock()
                            }
                            else -> {}
                        }
                    }
                    val processLifecycle = androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
                    processLifecycle.addObserver(observer)
                    onDispose {
                        processLifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    
                    val safeZonePassword = remember { 
                        mutableStateOf(sharedPref.getString("SAFE_ZONE_PWD", "") ?: "") 
                    }
                    
                    val safeZoneTriggerMode = remember {
                        mutableStateOf(sharedPref.getInt("SAFE_ZONE_TRIGGER_MODE", 0))
                    }
                    
                    val isBiometricEnabled = remember {
                        mutableStateOf(sharedPref.getBoolean("BIOMETRIC_ENABLED", false))
                    }
                    
                    val hasSeenWelcome = sharedPref.getBoolean("HAS_SEEN_WELCOME", false)
                    val startDestination = if (hasSeenWelcome) "home" else "welcome"
                    
                    val appShortcutMode = remember { mutableStateOf(sharedPref.getInt("APP_SHORTCUT_MODE", 0)) }

                    AppNavigation(
                        viewModel = notesViewModel,
                        startDestination = startDestination,
                        safeZonePassword = safeZonePassword,
                        safeZoneTriggerMode = safeZoneTriggerMode,
                        isBiometricEnabled = isBiometricEnabled,
                        isAppHidingEnabled = appShortcutMode,
                        currentThemeType = themeTypeIndex,
                        onSaveSafeZone = { newPwd, mode ->
                            val editor = sharedPref.edit()
                            editor.putString("SAFE_ZONE_PWD", newPwd)
                            editor.putInt("SAFE_ZONE_TRIGGER_MODE", mode)
                            editor.apply()
                            safeZonePassword.value = newPwd
                            safeZoneTriggerMode.value = mode
                        },
                        onBiometricToggle = { enabled ->
                            sharedPref.edit().putBoolean("BIOMETRIC_ENABLED", enabled).apply()
                            isBiometricEnabled.value = enabled
                        },
                        onAppHidingToggle = { mode ->
                            sharedPref.edit().putInt("APP_SHORTCUT_MODE", mode).apply()
                            appShortcutMode.value = mode
                        },
                        onThemeChanged = { newThemeIdx ->
                            sharedPref.edit().putInt("APP_THEME", newThemeIdx).apply()
                            themeTypeIndex.value = newThemeIdx
                        },
                        onWelcomeCompleted = {
                            sharedPref.edit().putBoolean("HAS_SEEN_WELCOME", true).apply()
                        },
                        pendingDeepLinkRoute = pendingNavigateRoute,
                        onDeepLinkHandled = { pendingNavigateRoute = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.getStringExtra("navigate_route")?.let { route ->
            pendingNavigateRoute = route
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}
