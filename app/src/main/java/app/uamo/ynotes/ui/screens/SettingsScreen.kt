package app.uamo.ynotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import app.uamo.ynotes.ui.components.CustomIcons
import app.uamo.ynotes.utils.SoundManager
import app.uamo.ynotes.widget.YNotesWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isFromSafeZone: Boolean,
    currentPassword: String,
    currentTriggerMode: Int,
    onSaveSafeZone: (String, Int) -> Unit,
    isBiometricEnabled: Boolean,
    isAppHidingEnabled: Int,
    isBooksEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onAppHidingToggle: (Int) -> Unit,
    currentThemeType: Int,
    onThemeChanged: (Int) -> Unit,
    onBooksToggle: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputPassword by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(currentTriggerMode) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
    var isWidgetsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("WIDGETS_ENABLED", true)) }
    var isMarkdownEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("MARKDOWN_ENABLED", true)) }
    val isSoundEnabled by SoundManager.isSoundEnabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val isSafeZoneCreated = currentPassword.isNotEmpty()
    val showSecurityOptions = if (isFromSafeZone) true else !isSafeZoneCreated

    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.18"
        } catch (e: Exception) {
            "1.0.18"
        }
    }

    fun safeExit() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackClickTime > 400L) {
            lastBackClickTime = now
            onNavigateBack()
        }
    }

    BackHandler {
        safeExit()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (currentPassword.isEmpty()) "Crear Zona Segura" else "Configurar Zona Segura") },
            text = {
                Column {
                    Text("Selecciona cómo accederás a la Zona Segura:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inputMode = 0 }
                    ) {
                        RadioButton(selected = inputMode == 0, onClick = { inputMode = 0 })
                        Text("Escribir la contraseña en el buscador", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (canAuthenticate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputMode = 1 }
                        ) {
                            RadioButton(selected = inputMode == 1, onClick = { inputMode = 1 })
                            Text("Mantener presionado Buscar (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputMode = 2 }
                        ) {
                            RadioButton(selected = inputMode == 2, onClick = { inputMode = 2 })
                            Text("Mantener presionado Ajustes (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputMode = 3 }
                        ) {
                            RadioButton(selected = inputMode == 3, onClick = { inputMode = 3 })
                            Text("Mantener presionado Añadir Nota (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    if (inputMode == 0) {
                        OutlinedTextField(
                            value = inputPassword,
                            onValueChange = { inputPassword = it },
                            label = { Text("Contraseña (Requerida)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (currentPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (inputMode == 0) "Nota: Guarda con el campo vacío para eliminar la Zona Segura." else "Nota: Selecciona 'Escribir contraseña' y guarda vacío para eliminar la Zona Segura.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalPassword = if (inputMode != 0 && inputPassword.isBlank()) {
                        if (currentPassword.isNotEmpty()) currentPassword else "biometric_safe_zone"
                    } else {
                        inputPassword.trim()
                    }
                    onSaveSafeZone(finalPassword, inputMode)
                    showDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val dynamicColorScheme = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        MaterialTheme.colorScheme
    }

    val colorScheme = if (isFromSafeZone) MaterialTheme.colorScheme else dynamicColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Text(
                            text = if (isFromSafeZone) "Ajustes de Zona Segura" else "Ajustes",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            SoundManager.playTap()
                            safeExit()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (showSecurityOptions) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SEGURIDAD",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column {
                            if (currentPassword.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = canAuthenticate) { onBiometricToggle(!isBiometricEnabled) }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Bloqueo por huella",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (canAuthenticate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (canAuthenticate) "Usa tu huella para acceder a la Zona Segura" else "No disponible en este dispositivo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (canAuthenticate) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    Switch(
                                        checked = isBiometricEnabled,
                                        onCheckedChange = onBiometricToggle,
                                        enabled = canAuthenticate
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                "Accesos Directos",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                when (isAppHidingEnabled) {
                                                    0 -> "Desactivado"
                                                    1 -> "Pestaña superior en Zona Segura"
                                                    2 -> "Vista principal de la app"
                                                    else -> "Desactivado"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val modeLabels = listOf("Desactivado", "Activado", "Reversa")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        modeLabels.forEachIndexed { index, label ->
                                            val isSelected = isAppHidingEnabled == index
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onAppHidingToggle(index) },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                            }

                            SettingItem(
                                title = if (currentPassword.isEmpty()) "Crear Zona Segura" else "Configurar Zona Segura",
                                subtitle = if (currentPassword.isEmpty()) "Inactiva (Toca para crear)" else "Activa (Protegida)",
                                icon = Icons.Default.Security,
                                iconTint = if (currentPassword.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                onClick = {
                                    inputPassword = currentPassword
                                    inputMode = currentTriggerMode
                                    showDialog = true
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "GENERAL",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Books switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBooksToggle(!isBooksEnabled) }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Libros (Categorías)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isBooksEnabled) "Activado" else "Desactivado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isBooksEnabled,
                                onCheckedChange = onBooksToggle
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                        // Sound switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = !isSoundEnabled
                                    SoundManager.setSoundEnabled(context, next)
                                    if (next) SoundManager.playTap()
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sonidos de la app",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isSoundEnabled) "Activado" else "Silenciado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSoundEnabled,
                                onCheckedChange = { enabled ->
                                    SoundManager.setSoundEnabled(context, enabled)
                                    if (enabled) SoundManager.playTap()
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                        // Widgets switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newState = !isWidgetsEnabled
                                    isWidgetsEnabled = newState
                                    sharedPrefs.edit().putBoolean("WIDGETS_ENABLED", newState).apply()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        YNotesWidget().updateAll(context)
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Widgets de Notas",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isWidgetsEnabled) "Activado" else "Desactivado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isWidgetsEnabled,
                                onCheckedChange = { enabled ->
                                    isWidgetsEnabled = enabled
                                    sharedPrefs.edit().putBoolean("WIDGETS_ENABLED", enabled).apply()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        YNotesWidget().updateAll(context)
                                    }
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                        // Markdown switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newState = !isMarkdownEnabled
                                    isMarkdownEnabled = newState
                                    sharedPrefs.edit().putBoolean("MARKDOWN_ENABLED", newState).apply()
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Formato Markdown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isMarkdownEnabled) "Activado por defecto" else "Desactivado (actívalo por nota en ⋮)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isMarkdownEnabled,
                                onCheckedChange = { enabled ->
                                    isMarkdownEnabled = enabled
                                    sharedPrefs.edit().putBoolean("MARKDOWN_ENABLED", enabled).apply()
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                        // Theme Selector (v1.0.3 layout)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Apariencia",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        when (currentThemeType) {
                                            0 -> "Actual (Por defecto)"
                                            1 -> "Google Notes"
                                            2 -> "Samsung Notes"
                                            else -> "Actual (Por defecto)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val themeLabels = listOf("Actual", "Google", "Samsung")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                themeLabels.forEachIndexed { index, label ->
                                    val isSelected = currentThemeType == index
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onThemeChanged(index) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))

                        SettingItem(
                            title = "Acerca de la app",
                            subtitle = "yNotes desarrollada para ti",
                            icon = Icons.Default.Info,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                        SettingItem(
                            title = "Versión",
                            subtitle = "$appVersion (AMOLED Edition)",
                            icon = Icons.Default.Build,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                        SettingItem(
                            title = "Donaciones.. Llegar a PlayStore <3",
                            subtitle = "Apóyame para subir la app a la Play Store",
                            icon = CustomIcons.GooglePlay,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showDonationDialog = true }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDonationDialog) {
            AlertDialog(
                onDismissRequest = { showDonationDialog = false },
                title = { Text("Donaciones", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) },
                text = { Text("¿Con qué plataforma deseas apoyar el proyecto?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.binance.com/uni-qr/EVqNr5tY"))
                            context.startActivity(intent)
                            showDonationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCD535), contentColor = Color.Black)
                    ) {
                        Icon(CustomIcons.Binance, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Binance")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/shhkk"))
                            context.startActivity(intent)
                            showDonationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00457C), contentColor = Color.White)
                    ) {
                        Icon(CustomIcons.PayPal, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PayPal")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
