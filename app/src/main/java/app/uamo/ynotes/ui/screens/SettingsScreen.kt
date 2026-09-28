package app.uamo.ynotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import app.uamo.ynotes.ui.components.CustomIcons
import app.uamo.ynotes.utils.BackupManager
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

    // Backup & Restore states
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var backupPasswordInput by remember { mutableStateOf("") }
    var backupPasswordConfirmInput by remember { mutableStateOf("") }
    var isBackupPasswordVisible by remember { mutableStateOf(false) }
    var validatedExportPassword by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingBackup by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var showBackupResultDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pwd = validatedExportPassword
        if (uri != null && !pwd.isNullOrEmpty()) {
            isProcessingBackup = true
            coroutineScope.launch {
                val result = BackupManager.exportBackup(context, uri, pwd)
                isProcessingBackup = false
                validatedExportPassword = null
                when (result) {
                    is BackupManager.BackupResult.Success -> {
                        backupStatusMessage = "Copia exportada con éxito:\n${result.notesCount} notas y ${result.booksCount} cuadernos protegidos con AES-256-GCM."
                        showBackupResultDialog = true
                    }
                    is BackupManager.BackupResult.Error -> {
                        backupStatusMessage = result.message
                        showBackupResultDialog = true
                    }
                }
            }
        } else {
            validatedExportPassword = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            backupPasswordInput = ""
            showImportPasswordDialog = true
        }
    }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE) }
    var isWidgetsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("WIDGETS_ENABLED", true)) }
    var widgetShowSafeZone by remember { mutableStateOf(sharedPrefs.getBoolean("widget_show_safe_zone", false)) }
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
            packageInfo.versionName ?: "1.0.4"
        } catch (e: Exception) {
            "1.0.4"
        }
    }

    BackHandler {
        onNavigateBack()
    }

    // Dialog for SafeZone configuration
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
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = inputMode == 0, onClick = { inputMode = 0 })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escribir la contraseña en el buscador", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (canAuthenticate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputMode = 1 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = inputMode == 1, onClick = { inputMode = 1 })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mantener presionado Buscar (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputMode = 2 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = inputMode == 2, onClick = { inputMode = 2 })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mantener presionado Ajustes (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputMode = 3 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = inputMode == 3, onClick = { inputMode = 3 })
                            Spacer(modifier = Modifier.width(8.dp))
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
        if (androidx.compose.foundation.isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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
                            onNavigateBack()
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 🎨 CATEGORY 1: APARIENCIA
                SettingsCategorySection(
                    title = "Apariencia",
                    icon = Icons.Default.Palette
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Estilo de la interfaz",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentThemeType) {
                                0 -> "Material You Modern (Por defecto)"
                                1 -> "Google Notes"
                                2 -> "Samsung Notes"
                                else -> "Material You Modern"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val themeLabels = listOf("Modern", "Google", "Samsung")
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
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // 🔊 CATEGORY: SONIDO & EXPERIENCIA
                SettingsCategorySection(
                    title = "Sonido & Sensaciones",
                    icon = Icons.Default.VolumeUp
                ) {
                    Column {
                        SettingSwitchItem(
                            title = "Sonidos de la app",
                            subtitle = if (isSoundEnabled) "Feedback sonoro sutil al tocar notas, borrar o entrar a la zona segura" else "Silenciado (sin efectos de sonido)",
                            icon = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            checked = isSoundEnabled,
                            onCheckedChange = { enabled ->
                                SoundManager.setSoundEnabled(context, enabled)
                                if (enabled) {
                                    SoundManager.playTap()
                                }
                            }
                        )
                    }
                }

                // 🔒 CATEGORY 2: SEGURIDAD & ZONA SEGURA
                if (showSecurityOptions) {
                    SettingsCategorySection(
                        title = "Seguridad & Bóveda",
                        icon = Icons.Default.Security
                    ) {
                        Column {
                            if (currentPassword.isNotEmpty()) {
                                // Biometric switch
                                SettingSwitchItem(
                                    title = "Bloqueo por huella",
                                    subtitle = if (canAuthenticate) "Usa huella digital para desbloquear la app" else "No disponible en este dispositivo",
                                    icon = Icons.Default.Fingerprint,
                                    checked = isBiometricEnabled,
                                    enabled = canAuthenticate,
                                    onCheckedChange = onBiometricToggle
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                                // App Shortcuts / Hiding mode
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
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
                                                "Accesos Directos (App Hiding)",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                when (isAppHidingEnabled) {
                                                    0 -> "Desactivado"
                                                    1 -> "Pestaña superior en Zona Segura"
                                                    2 -> "Vista principal de la app"
                                                    else -> "Desactivado"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
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
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                            }

                            // Create / Change SafeZone password
                            SettingActionItem(
                                title = if (currentPassword.isEmpty()) "Crear Zona Segura" else "Modificar Acceso a Bóveda",
                                subtitle = if (currentPassword.isEmpty()) "Inactiva (Toca para crear tu contraseña cifrada)" else "Activa y protegida con AES-256-GCM",
                                icon = Icons.Default.EnhancedEncryption,
                                iconTint = if (currentPassword.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                onClick = {
                                    inputPassword = currentPassword
                                    inputMode = currentTriggerMode
                                    showDialog = true
                                }
                            )
                        }
                    }
                }

                // 📱 CATEGORY 3: ORGANIZACIÓN & WIDGETS
                SettingsCategorySection(
                    title = "Organización & Widgets",
                    icon = Icons.Default.Widgets
                ) {
                    Column {
                        // Notebooks switch
                        SettingSwitchItem(
                            title = "Sistema de Cuadernos",
                            subtitle = if (isBooksEnabled) "Organiza tus notas en carpetas y libros" else "Desactivado",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            checked = isBooksEnabled,
                            onCheckedChange = onBooksToggle
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                        // SafeZone Widgets switch
                        SettingSwitchItem(
                            title = "Widget en Pantalla de Inicio",
                            subtitle = if (isWidgetsEnabled) "Permite sincronizar notas con el widget Glance" else "Widgets desactivados",
                            icon = Icons.Default.Widgets,
                            checked = isWidgetsEnabled,
                            onCheckedChange = { enabled ->
                                isWidgetsEnabled = enabled
                                sharedPrefs.edit().putBoolean("WIDGETS_ENABLED", enabled).apply()
                                coroutineScope.launch(Dispatchers.IO) {
                                    YNotesWidget().updateAll(context)
                                }
                            }
                        )

                        // Widget content switch (Normal vs SafeZone notes) - only available from Safe Zone
                        if (isFromSafeZone) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                            SettingSwitchItem(
                                title = "Contenido del Widget",
                                subtitle = if (widgetShowSafeZone) "Mostrando Notas Secretas" else "Mostrando Notas Normales",
                                icon = Icons.Default.FilterList,
                                checked = widgetShowSafeZone,
                                onCheckedChange = { enabled ->
                                    widgetShowSafeZone = enabled
                                    sharedPrefs.edit().putBoolean("widget_show_safe_zone", enabled).apply()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        YNotesWidget().updateAll(context)
                                    }
                                }
                            )
                        }
                    }
                }

                // 💾 CATEGORY: COPIA DE SEGURIDAD & DATOS
                SettingsCategorySection(
                    title = "Copia de Seguridad & Datos",
                    icon = Icons.Default.CloudSync
                ) {
                    Column {
                        SettingActionItem(
                            title = "Exportar Copia Cifrada",
                            subtitle = "Copia protegida con contraseña (.ynote) con todas tus notas, secretos y fotos",
                            icon = Icons.Default.CloudUpload,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                SoundManager.playTap()
                                backupPasswordInput = ""
                                backupPasswordConfirmInput = ""
                                showExportPasswordDialog = true
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingActionItem(
                            title = "Restaurar Copia Cifrada",
                            subtitle = "Recupera tus notas desde un archivo de respaldo .ynote",
                            icon = Icons.Default.CloudDownload,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                SoundManager.playTap()
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }

                // ℹ️ CATEGORY 4: INFORMACIÓN & DONACIONES
                SettingsCategorySection(
                    title = "Información & Proyecto",
                    icon = Icons.Default.Info
                ) {
                    Column {
                        SettingActionItem(
                            title = "Versión instalada",
                            subtitle = "v$appVersion · Edición yNotes (id: y.notes)",
                            icon = Icons.Default.CheckCircle,
                            iconTint = MaterialTheme.colorScheme.primary,
                            showChevron = false,
                            onClick = null
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingActionItem(
                            title = "Acerca de la app",
                            subtitle = "yNotes desarrollada por uamo11 & renovada con Compose y Material 3",
                            icon = Icons.Default.Code,
                            iconTint = MaterialTheme.colorScheme.primary,
                            showChevron = false,
                            onClick = null
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingActionItem(
                            title = "Donaciones y Apoyo",
                            subtitle = "Apóyame para publicar la app en Google Play Store",
                            icon = CustomIcons.GooglePlay,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showDonationDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = {
                Text(
                    text = "Apoyar el Proyecto",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("¿Con qué plataforma deseas realizar tu aporte para llevar yNotes a la Google Play Store?")
            },
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

    // ── Export Password Dialog ──
    if (showExportPasswordDialog) {
        var exportError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Exportar Copia Cifrada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    Text(
                        "Protege tu copia de seguridad con una contraseña. Se cifrarán todas tus notas (públicas y secretas), libros y fotos con AES-256-GCM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backupPasswordInput,
                        onValueChange = { backupPasswordInput = it; exportError = null },
                        label = { Text("Contraseña de respaldo") },
                        singleLine = true,
                        visualTransformation = if (isBackupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isBackupPasswordVisible = !isBackupPasswordVisible }) {
                                Icon(
                                    imageVector = if (isBackupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPasswordConfirmInput,
                        onValueChange = { backupPasswordConfirmInput = it; exportError = null },
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        visualTransformation = if (isBackupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (exportError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(exportError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SoundManager.playTap()
                        if (backupPasswordInput.length < 4) {
                            exportError = "El código de acceso debe tener al menos 4 dígitos/caracteres."
                            return@Button
                        }
                        if (backupPasswordInput != backupPasswordConfirmInput) {
                            exportError = "Las contraseñas no coinciden."
                            return@Button
                        }
                        validatedExportPassword = backupPasswordInput
                        showExportPasswordDialog = false
                        val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                        exportLauncher.launch("ynotes_backup_$dateStr.ynote")
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    SoundManager.playTap()
                    showExportPasswordDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Import Password Dialog ──
    if (showImportPasswordDialog) {
        var importError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showImportPasswordDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restaurar Copia Cifrada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    Text(
                        "Introduce la contraseña maestra (mínimo 4 caracteres) con la que se protegió el archivo de respaldo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backupPasswordInput,
                        onValueChange = { backupPasswordInput = it; importError = null },
                        label = { Text("Contraseña de respaldo") },
                        singleLine = true,
                        visualTransformation = if (isBackupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isBackupPasswordVisible = !isBackupPasswordVisible }) {
                                Icon(
                                    imageVector = if (isBackupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(importError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SoundManager.playTap()
                        if (backupPasswordInput.length < 4) {
                            importError = "El código de acceso debe tener al menos 4 caracteres."
                            return@Button
                        }
                        val uri = pendingImportUri ?: return@Button
                        val pwd = backupPasswordInput
                        showImportPasswordDialog = false
                        isProcessingBackup = true
                        coroutineScope.launch {
                            val result = BackupManager.restoreBackup(context, uri, pwd)
                            isProcessingBackup = false
                            when (result) {
                                is BackupManager.BackupResult.Success -> {
                                    backupStatusMessage = "Restauración completada con éxito:\n${result.notesCount} notas y ${result.booksCount} cuadernos restaurados."
                                    showBackupResultDialog = true
                                }
                                is BackupManager.BackupResult.Error -> {
                                    backupStatusMessage = result.message
                                    showBackupResultDialog = true
                                }
                            }
                        }
                    }
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    SoundManager.playTap()
                    showImportPasswordDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Processing Dialog ──
    if (isProcessingBackup) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Procesando copia de seguridad") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Cifrando/descifrando datos...", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
    }

    // ── Result Dialog ──
    if (showBackupResultDialog) {
        AlertDialog(
            onDismissRequest = { showBackupResultDialog = false },
            title = { Text("Copia de Seguridad") },
            text = { Text(backupStatusMessage ?: "") },
            confirmButton = {
                Button(onClick = { showBackupResultDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
fun SettingsCategorySection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                SoundManager.playTap()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                SoundManager.playTap()
                onCheckedChange(it)
            },
            enabled = enabled
        )
    }
}

@Composable
fun SettingActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable {
            SoundManager.playTap()
            onClick()
        } else Modifier)
        .padding(horizontal = 16.dp, vertical = 14.dp)

    Row(
        modifier = rowModifier,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
