package app.uamo.ynotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import app.uamo.ynotes.ui.components.CustomIcons
import app.uamo.ynotes.widget.YNotesWidget
import androidx.glance.appwidget.updateAll
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
    var inputMode by remember { mutableStateOf(0) }
    var showDonationDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("yNotesPrefs", android.content.Context.MODE_PRIVATE) }
    var isWidgetsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("WIDGETS_ENABLED", true)) }
    val coroutineScope = rememberCoroutineScope()

    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Separa las opciones: en la zona normal solo vemos "Crear" (si no existe). 
    // En la zona segura vemos "Configurar" y "Huella".
    val isSafeZoneCreated = currentPassword.isNotEmpty()
    val showSecurityOptions = if (isFromSafeZone) true else !isSafeZoneCreated

    BackHandler {
        onNavigateBack()
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
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { inputMode = 0 }) {
                        RadioButton(selected = inputMode == 0, onClick = { inputMode = 0 })
                        Text("Escribir la contraseña en el buscador", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (canAuthenticate) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { inputMode = 1 }) {
                            RadioButton(selected = inputMode == 1, onClick = { inputMode = 1 })
                            Text("Mantener presionado Buscar (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { inputMode = 2 }) {
                            RadioButton(selected = inputMode == 2, onClick = { inputMode = 2 })
                            Text("Mantener presionado Ajustes (+ Huella)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { inputMode = 3 }) {
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
                    ),
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
                Spacer(modifier = Modifier.height(60.dp))
                
                Text(
                    text = if (isFromSafeZone) "Zona Segura" else "Ajustes",
                    style = TextStyle(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
    
                Card(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    
                    if (showSecurityOptions) {
                        if (currentPassword.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (canAuthenticate) "Usa huella para abrir la aplicación" else "No disponible en este dispositivo", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = onBiometricToggle,
                                    enabled = canAuthenticate
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                        "Widgets de Zona Segura", 
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (isWidgetsEnabled) "Permite ver notas en el widget tras huella (10s)" else "Widgets desactivados", 
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
                            
                            // App Shortcut Mode Selector
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
                                                1 -> "Apps como acceso rápido"
                                                2 -> "Apps como vista principal"
                                                else -> "Desactivado"
                                            },
                                            style = MaterialTheme.typography.bodyMedium, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                // 3-option selector
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
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    "Sistema de Libros", 
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isBooksEnabled) "Activado (Organiza tus notas)" else "Desactivado", 
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
                        
                        var widgetShowSafeZone by remember { mutableStateOf(sharedPrefs.getBoolean("widget_show_safe_zone", false)) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    "Notas del Widget", 
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (widgetShowSafeZone) "Mostrando Notas Secretas" else "Mostrando Notas Normales", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = widgetShowSafeZone,
                                onCheckedChange = { 
                                    if (!isFromSafeZone && it) {
                                        android.widget.Toast.makeText(context, "Actívalo desde la Zona Segura", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        widgetShowSafeZone = it
                                        sharedPrefs.edit().putBoolean("widget_show_safe_zone", it).apply()
                                    }
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                        
                        SettingItem(
                            title = if (currentPassword.isEmpty()) "Crear Zona Segura" else "Cambiar Contraseña", 
                            subtitle = if (currentPassword.isEmpty()) "Inactiva (Toca para crear)" else "Activa (La Zona Segura está protegida)",
                            icon = Icons.Default.EnhancedEncryption,
                            iconTint = if (currentPassword.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            onClick = { 
                                inputPassword = currentPassword
                                showDialog = true 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    }

                    // Theme Selector
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
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        subtitle = "1.0.0 (AMOLED Edition)", 
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
fun SettingItem(title: String, subtitle: String, icon: ImageVector, iconTint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
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
