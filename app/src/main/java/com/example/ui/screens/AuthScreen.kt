package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.MaquisAmber
import com.example.ui.theme.MaquisBg
import com.example.ui.theme.MaquisBorder
import com.example.ui.theme.MaquisEmerald
import com.example.ui.theme.MaquisRose
import com.example.ui.theme.MaquisSurface
import com.example.ui.theme.MaquisSurfaceAlt
import com.example.ui.theme.MaquisTextPrimary
import com.example.ui.theme.MaquisTextSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSession = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSession = false
            },
            scope = coroutineScope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaquisBg)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Art Banner with seamless night fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_maquis_hero_1794830028814),
                    contentDescription = "Illustration d'un maquis et grillades la nuit",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaquisBg.copy(alpha = 0.45f),
                                    MaquisBg
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaquisBg.copy(alpha = 0.85f))
                        .border(1.dp, MaquisBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaquisEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MP",
                            color = MaquisBg,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "MaquisPro",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaquisTextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pilotez votre Maquis, Cave & Grillades",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaquisTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Caisse rapide 0 ms même sans réseau, gestion au casier et bénéfice net réel synchronisé sur le téléphone du patron.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaquisTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                FeatureHighlightRow(
                    icon = Icons.Filled.PointOfSale,
                    tint = MaquisEmerald,
                    title = "Caisse multi-tables & Mobile Money",
                    subtitle = "Espèces avec calcul de monnaie, MTN MoMo, Orange Money, ardoises & reçus WhatsApp."
                )
                Spacer(Modifier.height(10.dp))
                FeatureHighlightRow(
                    icon = Icons.Filled.Inventory2,
                    tint = MaquisAmber,
                    title = "Stock au casier, unité & consignes",
                    subtitle = "Entrées par casier, suivi des emballages vides et déclaration de casse."
                )
                Spacer(Modifier.height(10.dp))
                FeatureHighlightRow(
                    icon = Icons.Filled.TrendingUp,
                    tint = MaquisEmerald,
                    title = "Bilan Propriétaire verrouillé par PIN",
                    subtitle = "Synchronisation Cloud temps réel avec sauvegarde locale hors-ligne."
                )

                Spacer(Modifier.height(24.dp))

                if (errorMessage != null) {
                    Surface(
                        color = MaquisRose.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaquisRose.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaquisRose,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading && !isCheckingSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaquisEmerald,
                        contentColor = MaquisBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading || isCheckingSession) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaquisBg,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isCheckingSession) "Vérification..." else "Connexion en cours...",
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Icon(Icons.Filled.CloudDone, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Continuer avec Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Utilisez le même compte Google sur la tablette du bar et le téléphone du propriétaire pour synchroniser en direct.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaquisTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaquisSurface)
            .border(1.dp, MaquisBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaquisTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaquisTextSecondary
            )
        }
    }
}
