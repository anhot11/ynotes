package app.uamo.ynotes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.ui.components.CustomIcons
import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.ui.theme.AuroraSecondary
import kotlinx.coroutines.launch

data class OnboardingStep(
    val badge: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradient: Brush,
    val features: List<Pair<String, String>>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val isDark = isSystemInDarkTheme()

    val steps = remember {
        listOf(
            OnboardingStep(
                badge = "PRODUCTIVIDAD",
                title = "Captura ideas al instante",
                description = "Escribe notas con formato Markdown, listas de tareas, imágenes adjuntas y modo inmersivo sin distracciones.",
                icon = CustomIcons.NotebookEditOutline,
                gradient = AuroraPrimary,
                features = listOf(
                    "Modo Enfoque" to "Pantalla completa para escribir sin distracciones",
                    "Barra Markdown" to "Negrita, cursiva, listas, títulos y citas con un toque",
                    "Adjuntos visuales" to "Agrega múltiples fotos directamente a tus notas"
                )
            ),
            OnboardingStep(
                badge = "MÁXIMA PRIVACIDAD",
                title = "Zona Segura AES-256",
                description = "Tus notas secretas y aplicaciones se resguardan bajo cifrado militar simétrico AES-256-GCM y biometría.",
                icon = CustomIcons.ShieldLockOutline,
                gradient = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFF8B5CF6))),
                features = listOf(
                    "Cifrado local" to "LINX Crypt con llaves protegidas por el hardware",
                    "Desbloqueo biométrico" to "Acceso instantáneo con tu huella digital",
                    "Ocultar aplicaciones" to "Lanza y camufla apps dentro de tu bóveda"
                )
            ),
            OnboardingStep(
                badge = "ORGANIZACIÓN TOTAL",
                title = "Cuadernos y Widgets",
                description = "Categoriza tus apuntes en cuadernos temáticos y ancla notas clave en la pantalla de inicio con widgets Glance.",
                icon = CustomIcons.WidgetsOutline,
                gradient = AuroraSecondary,
                features = listOf(
                    "Sistema de Cuadernos" to "Agrupa y personaliza tus proyectos por libros",
                    "Widgets Dinámicos" to "Visualiza tus notas directamente en el inicio",
                    "Papelera Segura" to "Recupera o elimina notas con total confianza"
                )
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Bar: Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Logo / Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "yNotes",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                if (pagerState.currentPage < 2) {
                    TextButton(onClick = onStart) {
                        Text(
                            text = "Omitir",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Interactive Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glowing Icon Orb
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(step.gradient)
                            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Category Pill Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = step.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Feature highlights container card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            step.features.forEach { (featureTitle, featureDesc) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = featureTitle,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = featureDesc,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expanding Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isCurrent = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isCurrent) 28.dp else 8.dp,
                            animationSpec = spring(dampingRatio = 0.7f),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                // Next / Get Started Button
                var isBtnPressed by remember { mutableStateOf(false) }
                val btnScale by animateFloatAsState(
                    targetValue = if (isBtnPressed) 0.94f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "btnScale"
                )

                val isLastPage = pagerState.currentPage == 2

                Box(
                    modifier = Modifier
                        .scale(btnScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AuroraPrimary)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .pointerInput(isLastPage) {
                            detectTapGestures(
                                onPress = {
                                    isBtnPressed = true
                                    tryAwaitRelease()
                                    isBtnPressed = false
                                },
                                onTap = {
                                    if (isLastPage) {
                                        onStart()
                                    } else {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLastPage) "Empezar ahora" else "Siguiente",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
