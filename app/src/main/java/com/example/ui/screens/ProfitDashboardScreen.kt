package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ModePaiement
import com.example.data.model.Role
import com.example.data.model.Vente
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MaquisAmber
import com.example.ui.theme.MaquisBg
import com.example.ui.theme.MaquisBorder
import com.example.ui.theme.MaquisEmerald
import com.example.ui.theme.MaquisEmeraldDark
import com.example.ui.theme.MaquisMomoYellow
import com.example.ui.theme.MaquisOrangeMoney
import com.example.ui.theme.MaquisRose
import com.example.ui.theme.MaquisRoseDark
import com.example.ui.theme.MaquisSky
import com.example.ui.theme.MaquisSurface
import com.example.ui.theme.MaquisSurfaceAlt
import com.example.ui.theme.MaquisTextPrimary
import com.example.ui.theme.MaquisTextSecondary
import com.example.ui.viewmodel.MaquisUiState
import com.example.ui.viewmodel.PeriodeBilan
import com.example.util.formatFCFA
import com.example.util.formatStockCasierUnite

@Composable
fun ProfitDashboardScreen(
    state: MaquisUiState,
    onUnlockWithPin: (String) -> Boolean,
    onLockToGerant: () -> Unit,
    onSetPeriode: (PeriodeBilan) -> Unit,
    onSyncNow: () -> Unit,
    onReglerArdoise: (Vente, ModePaiement) -> Unit,
    onUpdatePin: (String) -> Unit
) {
    if (state.role != Role.PROPRIETAIRE) {
        OwnerPinGate(
            onUnlock = onUnlockWithPin
        )
        return
    }

    val metrics = remember(state.ventes, state.depenses, state.pertes, state.periodeBilan) {
        state.computeProfitMetrics()
    }
    var ardoiseToSettle by remember { mutableStateOf<Vente?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaquisBg)
    ) {
        // 1. Period Filter + Cloud Sync Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaquisSurface)
                        .border(1.dp, MaquisBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PeriodeBilan.entries.forEach { p ->
                        val active = state.periodeBilan == p
                        Surface(
                            onClick = { onSetPeriode(p) },
                            color = if (active) MaquisEmerald else Color.Transparent,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = p.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaquisBg else MaquisTextSecondary
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = onSyncNow,
                    color = if (state.pendingSyncCount > 0) MaquisAmber.copy(alpha = 0.16f) else MaquisSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (state.pendingSyncCount > 0) MaquisAmber.copy(alpha = 0.45f) else MaquisBorder
                    ),
                    modifier = Modifier.height(40.dp).testTag("sync_now_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Synchroniser",
                            tint = if (state.pendingSyncCount > 0) MaquisAmber else MaquisEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (state.pendingSyncCount > 0) "${state.pendingSyncCount} en attente" else "À jour (Cloud)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.pendingSyncCount > 0) MaquisAmber else MaquisEmerald
                        )
                    }
                }
            }
        }

        // 2. Hero Card: Bénéfice Net Réel
        item {
            val isPositive = metrics.beneficeNetReel >= 0L
            val gradient = Brush.linearGradient(
                colors = if (isPositive) {
                    listOf(MaquisEmeraldDark, MaquisSurface)
                } else {
                    listOf(MaquisRoseDark, MaquisSurface)
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(gradient)
                    .border(
                        1.5.dp,
                        if (isPositive) MaquisEmerald.copy(alpha = 0.55f) else MaquisRose.copy(alpha = 0.55f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                null,
                                tint = if (isPositive) MaquisEmerald else MaquisRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "BÉNÉFICE NET RÉEL (POCHE)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPositive) MaquisEmerald else MaquisRose
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaquisBg.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${metrics.nbVentes} ventes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaquisTextPrimary
                            )
                        }
                    }

                    Text(
                        text = formatFCFA(metrics.beneficeNetReel),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        color = MaquisTextPrimary,
                        modifier = Modifier.testTag("net_profit_value")
                    )

                    Text(
                        text = "Ventes (${formatFCFA(metrics.argentEntreTotal)}) − Coût d'achat (${formatFCFA(metrics.coutMarchandises)}) − Dépenses (${formatFCFA(metrics.totalDepenses)}) − Pertes (${formatFCFA(metrics.totalPertes)}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaquisTextSecondary
                    )
                }
            }
        }

        // 3. 4 Simple KPI Cards (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Argent entré (Ventes)",
                        value = formatFCFA(metrics.argentEntreTotal),
                        subtitle = "Encaissé : ${formatFCFA(metrics.argentEncaisseReel)}",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accent = MaquisEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Coût des produits",
                        value = formatFCFA(metrics.coutMarchandises),
                        subtitle = "À garder pour racheter le stock",
                        icon = Icons.Filled.Inventory2,
                        accent = MaquisSky,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Dépenses",
                        value = formatFCFA(metrics.totalDepenses),
                        subtitle = "Glace, charbon, transport...",
                        icon = Icons.Filled.ArrowDownward,
                        accent = MaquisAmber,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Pertes & Casses",
                        value = formatFCFA(metrics.totalPertes),
                        subtitle = "Casses, offerts, périmés",
                        icon = Icons.Filled.Warning,
                        accent = MaquisRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Cash & Mobile Money Breakdown
        item {
            Surface(
                color = MaquisSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaquisBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "OÙ SE TROUVE L'ARGENT ENCAISSÉ ?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaquisTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentSplitPill(
                            label = "Espèces",
                            amount = metrics.especesTotal,
                            accent = MaquisEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentSplitPill(
                            label = "MTN MoMo",
                            amount = metrics.mtnMomoTotal,
                            accent = MaquisMomoYellow,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentSplitPill(
                            label = "Orange Money",
                            amount = metrics.orangeMoneyTotal,
                            accent = MaquisOrangeMoney,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Critical Stock Alerts
        if (state.alertesStock.isNotEmpty()) {
            item {
                Surface(
                    color = MaquisRoseDark.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaquisRose.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = MaquisRose, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "STOCK CRITIQUE : À COMMANDER (${state.alertesStock.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaquisRose
                            )
                        }
                        state.alertesStock.forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaquisBg.copy(alpha = 0.65f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prod.nom, color = MaquisTextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = formatStockCasierUnite(prod.stockActuelUnites, prod.unitesParCasier),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaquisRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Open Ardoises (Credits to recover)
        if (state.ardoisesOuvertes.isNotEmpty()) {
            item {
                val totalArdoises = state.ardoisesOuvertes.sumOf { it.totalVente }
                Surface(
                    color = MaquisAmber.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaquisAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HowToReg, null, tint = MaquisAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ARDOISES À RÉCUPÉRER (${formatFCFA(totalArdoises)})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaquisAmber
                            )
                        }
                        state.ardoisesOuvertes.forEach { vente ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaquisBg.copy(alpha = 0.7f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vente.clientNom ?: "Client", color = MaquisTextPrimary, fontWeight = FontWeight.Bold)
                                    if (!vente.clientTel.isNullOrBlank()) {
                                        Text(vente.clientTel, color = MaquisTextSecondary, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = formatFCFA(vente.totalVente),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaquisAmber
                                    )
                                }
                                Button(
                                    onClick = { ardoiseToSettle = vente },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("Encaisser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Top Profitable & Top Sold Products
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaquisSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaquisBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = MaquisEmerald, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("TOP PRODUITS RENTABLES", style = MaterialTheme.typography.labelLarge, color = MaquisEmerald)
                        }
                        if (metrics.topRentables.isEmpty()) {
                            Text("Aucune vente sur cette période.", color = MaquisTextSecondary, fontSize = 13.sp)
                        } else {
                            metrics.topRentables.forEachIndexed { idx, stat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 1}. ${stat.nom} (${stat.quantite})", color = MaquisTextPrimary, fontSize = 13.sp)
                                    Text(
                                        "+${formatFCFA(stat.margeNette)}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaquisEmerald,
                                        fontSize = 13.sp
                                    )
                                }
                                if (idx < metrics.topRentables.lastIndex) {
                                    HorizontalDivider(color = MaquisBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = MaquisSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaquisBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = MaquisSky, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("TOP PRODUITS LES PLUS VENDUS", style = MaterialTheme.typography.labelLarge, color = MaquisSky)
                        }
                        if (metrics.topVendus.isEmpty()) {
                            Text("Aucune vente sur cette période.", color = MaquisTextSecondary, fontSize = 13.sp)
                        } else {
                            metrics.topVendus.forEachIndexed { idx, stat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 1}. ${stat.nom}", color = MaquisTextPrimary, fontSize = 13.sp)
                                    Text(
                                        "${stat.quantite} u • ${formatFCFA(stat.chiffreAffaires)}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaquisSky,
                                        fontSize = 13.sp
                                    )
                                }
                                if (idx < metrics.topVendus.lastIndex) {
                                    HorizontalDivider(color = MaquisBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Owner Security Actions (Change PIN / Lock back to Server)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showChangePinDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaquisBorder),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Changer PIN", color = MaquisTextPrimary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onLockToGerant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaquisAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.Lock, null, tint = MaquisAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verrouiller", color = MaquisAmber, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (ardoiseToSettle != null) {
        BackHandler { ardoiseToSettle = null }
        SettleArdoiseDialog(
            vente = ardoiseToSettle!!,
            onDismiss = { ardoiseToSettle = null },
            onSettle = { mode ->
                onReglerArdoise(ardoiseToSettle!!, mode)
                ardoiseToSettle = null
            }
        )
    }

    if (showChangePinDialog) {
        BackHandler { showChangePinDialog = false }
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onConfirm = { newPin ->
                onUpdatePin(newPin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
private fun OwnerPinGate(
    onUnlock: (String) -> Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaquisBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaquisAmber.copy(alpha = 0.15f))
                    .border(1.dp, MaquisAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaquisAmber, modifier = Modifier.size(34.dp))
            }

            Text(
                text = "Espace Propriétaire Protégé",
                style = MaterialTheme.typography.titleLarge,
                color = MaquisTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Les prix d'achat et les bénéfices réels sont masqués pour les serveurs. Entrez le PIN Patron (démo : 1234).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaquisTextSecondary,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    if (it.length <= 8) {
                        pinInput = it.filter { c -> c.isDigit() }
                        pinError = false
                    }
                },
                label = { Text("Code PIN Propriétaire") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_pin_input")
            )

            if (pinError) {
                Text(
                    text = "Code PIN incorrect.",
                    color = MaquisRose,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Button(
                onClick = {
                    if (!onUnlock(pinInput)) {
                        pinError = true
                    } else {
                        pinInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaquisAmber, contentColor = MaquisBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("unlock_owner_button")
            ) {
                Icon(Icons.Filled.LockOpen, null)
                Spacer(Modifier.width(8.dp))
                Text("Déverrouiller le Bilan", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaquisSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaquisBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaquisTextSecondary)
            }
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = accent
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaquisTextSecondary)
        }
    }
}

@Composable
private fun PaymentSplitPill(
    label: String,
    amount: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaquisBg)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatFCFA(amount),
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaquisTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettleArdoiseDialog(
    vente: Vente,
    onDismiss: () -> Unit,
    onSettle: (ModePaiement) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaquisSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaquisBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Régler l'ardoise", style = MaterialTheme.typography.titleLarge, color = MaquisTextPrimary)
                Text(
                    text = "${vente.clientNom ?: "Client"} • ${formatFCFA(vente.totalVente)}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaquisAmber
                )
                listOf(ModePaiement.ESPECES, ModePaiement.MTN_MOMO, ModePaiement.ORANGE_MONEY).forEach { mode ->
                    Button(
                        onClick = { onSettle(mode) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Encaisser par ${mode.label}", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    Text("Annuler", color = MaquisTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaquisSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaquisBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Nouveau PIN Propriétaire", style = MaterialTheme.typography.titleLarge, color = MaquisTextPrimary)
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 8) newPin = it.filter { c -> c.isDigit() } },
                    label = { Text("4 à 8 chiffres") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Annuler", color = MaquisTextSecondary)
                    }
                    Button(
                        onClick = { onConfirm(newPin) },
                        enabled = newPin.length in 4..8,
                        colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Valider", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
