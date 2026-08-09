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
        val isBiometricEnabled = sharedPrefs.getBoolean("BIOMETRIC_ENABLED", true)
        
        if (!isBiometricEnabled) {
            // If biometrics are disabled globally, we could just unlock it directly,
            // or we could show a toast that biometrics must be enabled.
            // Let's assume the user wants it to always ask for biometric if they enabled it.
            // But if it's disabled, maybe just allow unlock.
            unlockWidget()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
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
        sharedPrefs.edit()
            .putBoolean("widget_unlocked", true)
            // Unlocked for 60 seconds
            .putLong("widget_unlock_expiry", System.currentTimeMillis() + 60_000)
            .apply()

        CoroutineScope(Dispatchers.IO).launch {
            YNotesWidget().updateAll(this@WidgetAuthActivity)
            finish()
        }
    }
}
