package app.uamo.ynotes.widget

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
        val areWidgetsEnabled = sharedPrefs.getBoolean("WIDGETS_ENABLED", true)
        if (!areWidgetsEnabled) {
            Toast.makeText(applicationContext, "Los widgets están desactivados en los Ajustes", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isBiometricEnabled = sharedPrefs.getBoolean("BIOMETRIC_ENABLED", true)
        
        if (!isBiometricEnabled) {
            unlockWidget()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockWidget()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fallo de autenticación", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Notas")
            .setSubtitle("Usa tu huella para ver las notas en el widget")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun unlockWidget() {
        val sharedPrefs = getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
        val expiryTime = System.currentTimeMillis() + 10_000L // Unlocked for 10 seconds
        sharedPrefs.edit()
            .putBoolean("widget_unlocked", true)
            .putLong("widget_unlock_expiry", expiryTime)
            .apply()

        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            YNotesWidget().updateAll(appContext)
            // Auto-relock after 10 seconds
            kotlinx.coroutines.delay(10_050L)
            val currentExpiry = sharedPrefs.getLong("widget_unlock_expiry", 0)
            if (System.currentTimeMillis() >= currentExpiry) {
                sharedPrefs.edit()
                    .putBoolean("widget_unlocked", false)
                    .putLong("widget_unlock_expiry", 0)
                    .apply()
                YNotesWidget().updateAll(appContext)
            }
        }
        finish()
    }
}
