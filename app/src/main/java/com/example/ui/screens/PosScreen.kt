package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategorieProduit
import com.example.data.model.ModePaiement
import com.example.data.model.Produit
import com.example.data.model.TypePerte
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MaquisAmber
import com.example.ui.theme.MaquisBg
import com.example.ui.theme.MaquisBorder
import com.example.ui.theme.MaquisEmerald
import com.example.ui.theme.MaquisEmeraldDark
import com.example.ui.theme.MaquisMomoYellow
import com.example.ui.theme.MaquisOrangeMoney
import com.example.ui.theme.MaquisRose
import com.example.ui.theme.MaquisSurface
import com.example.ui.theme.MaquisSurfaceAlt
import com.example.ui.theme.MaquisTextPrimary
import com.example.ui.theme.MaquisTextSecondary
import com.example.ui.viewmodel.MaquisUiState
import com.example.ui.viewmodel.PaidReceipt
import com.example.util.buildWhatsAppReceiptText
import com.example.util.formatFCFA
import com.example.util.formatNumberFR
import com.example.util.formatStockCasierUnite

private val PREDEFINED_TABLES = listOf("Comptoir", "Table 1", "Table 2", "Table 3", "Table VIP", "Terrasse")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    state: MaquisUiState,
    onSelectTable: (String) -> Unit,
    onSelectCategory: (CategorieProduit?) -> Unit,
    onAddToCart: (Produit) -> Unit,
    onUpdateCartQuantity: (String, Long) -> Unit,
    onSaveTableOuverte: () -> Unit,
    onEncaisserVente: (ModePaiement, String?, String?) -> Unit,
    onDeclarerPerte: (Produit, Long, TypePerte, String) -> Unit,
    onDismissReceipt: () -> Unit
) {
    val context = LocalContext.current

    var showTicketSheet by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showPerteDialog by remember { mutableStateOf(false) }

    val filteredProduits = remember(state.produits, state.selectedCategory) {
        if (state.selectedCategory == null) state.produits
        else state.produits.filter { it.categorie == state.selectedCategory.key }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaquisBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Tables & Open Additions Strip + Breakage Quick Button
            Surface(
                color = MaquisSurface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TABLES (${state.tablesOuvertes.size} EN COURS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaquisTextSecondary
                        )
                        Surface(
                            onClick = { showPerteDialog = true },
                            color = MaquisRose.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaquisRose.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("declare_loss_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = "Déclarer casse",
                                    tint = MaquisRose,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Casse / Offert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaquisRose,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PREDEFINED_TABLES) { nomTable ->
                            val openVente = state.tablesOuvertes.firstOrNull { it.tableNom == nomTable }
                            val isActive = state.activeTable == nomTable

                            val bgColor = when {
                                isActive -> MaquisEmerald
                                openVente != null -> MaquisAmber.copy(alpha = 0.18f)
                                else -> MaquisSurfaceAlt
                            }
                            val borderColor = when {
                                isActive -> MaquisEmerald
                                openVente != null -> MaquisAmber.copy(alpha = 0.55f)
                                else -> MaquisBorder
                            }
                            val textColor = when {
                                isActive -> MaquisBg
                                openVente != null -> MaquisAmber
                                else -> MaquisTextPrimary
                            }

                            Surface(
                                onClick = { onSelectTable(nomTable) },
                                color = bgColor,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("table_chip_$nomTable")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = nomTable,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = textColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (openVente != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isActive) MaquisBg.copy(alpha = 0.2f) else MaquisAmber)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = formatNumberFR(openVente.totalVente),
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaquisBg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Category Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChipItem(
                        label = "Tout",
                        selected = state.selectedCategory == null,
                        icon = { Icon(Icons.Filled.LocalBar, null, modifier = Modifier.size(16.dp)) },
                        onClick = { onSelectCategory(null) }
                    )
                }
                val mainCats = listOf(
                    CategorieProduit.BIERES,
                    CategorieProduit.GAZEUSES,
                    CategorieProduit.VINS_SPIRITUEUX,
                    CategorieProduit.PLATS_GRILLADES,
                    CategorieProduit.DIVERS
                )
                items(mainCats) { cat ->
                    val iconVec = when (cat) {
                        CategorieProduit.BIERES -> Icons.Filled.SportsBar
                        CategorieProduit.GAZEUSES -> Icons.Filled.LocalCafe
                        CategorieProduit.VINS_SPIRITUEUX -> Icons.Filled.WineBar
                        CategorieProduit.PLATS_GRILLADES -> Icons.Filled.Restaurant
                        CategorieProduit.DIVERS -> Icons.Filled.ShoppingBag
                    }
                    CategoryChipItem(
                        label = cat.label,
                        selected = state.selectedCategory == cat,
                        icon = { Icon(iconVec, null, modifier = Modifier.size(16.dp)) },
                        onClick = { onSelectCategory(cat) }
                    )
                }
            }

            // 3. Last Paid Receipt Banner (WhatsApp Sharing)
            AnimatedVisibility(visible = state.lastPaidReceipt != null) {
                state.lastPaidReceipt?.let { receipt ->
                    WhatsAppReceiptBanner(
                        receipt = receipt,
                        onShareWhatsApp = {
                            val msg = buildWhatsAppReceiptText(
                                maquisName = state.settings.maquisName,
                                tableNom = receipt.tableNom,
                                ticketNum = receipt.ticketNum,
                                lignes = receipt.lignes,
                                total = receipt.total,
                                mode = receipt.mode
                            )
                            val waIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/?text=${Uri.encode(msg)}")
                            )
                            runCatching { context.startActivity(waIntent) }.onFailure {
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(fallback, "Envoyer le reçu"))
                            }
                        },
                        onDismiss = onDismissReceipt
                    )
                }
            }

            // 4. Tactile Product Grid
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 900.dp -> 4
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }
                val bottomPad = if (state.panierLocal.isNotEmpty()) 96.dp else 16.dp

                if (filteredProduits.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Aucun produit dans cette catégorie",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaquisTextSecondary
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = bottomPad
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProduits, key = { it.id }) { produit ->
                            val qtePanier = state.panierLocal.firstOrNull { it.produitId == produit.id }?.quantite ?: 0L
                            ProductPosTile(
                                produit = produit,
                                qtePanier = qtePanier,
                                onClick = { onAddToCart(produit) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Floating Active Cart Bar
        AnimatedVisibility(
            visible = state.panierLocal.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Surface(
                color = MaquisSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, MaquisEmerald.copy(alpha = 0.65f)),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTicketSheet = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaquisEmerald.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ShoppingBag,
                                contentDescription = "Voir le ticket",
                                tint = MaquisEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaquisEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.nombreArticlesPanier.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaquisBg
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${state.activeTable} • Détail",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaquisTextSecondary
                            )
                            Text(
                                text = formatFCFA(state.totalPanier),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaquisTextPrimary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.activeTable != "Comptoir") {
                            OutlinedButton(
                                onClick = onSaveTableOuverte,
                                border = BorderStroke(1.dp, MaquisAmber),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaquisAmber),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("save_table_button")
                            ) {
                                Text("Sur Table", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = { showCheckoutSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaquisEmerald,
                                contentColor = MaquisBg
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("checkout_button")
                        ) {
                            Text("Encaisser", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Ticket Drawer Sheet
    if (showTicketSheet) {
        BackHandler { showTicketSheet = false }
        ModalBottomSheet(
            onDismissRequest = { showTicketSheet = false },
            containerColor = MaquisSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Addition : ${state.activeTable}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaquisTextPrimary
                    )
                    IconButton(onClick = { showTicketSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = MaquisTextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.panierLocal, key = { it.produitId }) { ligne ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaquisSurfaceAlt)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ligne.nom,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaquisTextPrimary
                                )
                                Text(
                                    text = "${formatFCFA(ligne.prixVente)} × ${ligne.quantite} = ${formatFCFA(ligne.prixVente * ligne.quantite)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaquisEmerald
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = { onUpdateCartQuantity(ligne.produitId, -1L) },
                                    color = MaquisSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (ligne.quantite == 1L) Icons.Filled.Delete else Icons.Filled.Remove,
                                            contentDescription = "Retirer",
                                            tint = if (ligne.quantite == 1L) MaquisRose else MaquisTextPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = ligne.quantite.toString(),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaquisTextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                Surface(
                                    onClick = { onUpdateCartQuantity(ligne.produitId, 1L) },
                                    color = MaquisEmerald,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = MaquisBg)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveTableOuverte()
                            showTicketSheet = false
                        },
                        border = BorderStroke(1.dp, MaquisAmber),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaquisAmber),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Laisser ouvert", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showTicketSheet = false
                            showCheckoutSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Encaisser ${formatNumberFR(state.totalPanier)}", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // Checkout Modal Bottom Sheet
    if (showCheckoutSheet) {
        BackHandler { showCheckoutSheet = false }
        CheckoutBottomSheet(
            tableNom = state.activeTable,
            total = state.totalPanier,
            onDismiss = { showCheckoutSheet = false },
            onConfirm = { mode, clientNom, clientTel ->
                onEncaisserVente(mode, clientNom, clientTel)
                showCheckoutSheet = false
            }
        )
    }

    // Loss / Breakage Dialog
    if (showPerteDialog && state.produits.isNotEmpty()) {
        BackHandler { showPerteDialog = false }
        DeclareLossDialog(
            produits = state.produits,
            onDismiss = { showPerteDialog = false },
            onSubmit = { prod, qte, type, motif ->
                onDeclarerPerte(prod, qte, type, motif)
                showPerteDialog = false
            }
        )
    }
}

@Composable
private fun CategoryChipItem(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = icon,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaquisTextPrimary,
            selectedLabelColor = MaquisBg,
            selectedLeadingIconColor = MaquisBg,
            containerColor = MaquisSurface,
            labelColor = MaquisTextSecondary,
            iconColor = MaquisTextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaquisBorder
        ),
        modifier = Modifier.heightIn(min = 40.dp)
    )
}

@Composable
private fun WhatsAppReceiptBanner(
    receipt: PaidReceipt,
    onShareWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaquisEmeraldDark.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaquisEmerald.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Encaissé (${receipt.tableNom})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaquisEmerald
                )
                Text(
                    text = formatFCFA(receipt.total),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaquisTextPrimary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("whatsapp_receipt_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reçu WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = MaquisTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ProductPosTile(
    produit: Produit,
    qtePanier: Long,
    onClick: () -> Unit
) {
    val enRupture = produit.stockActuelUnites <= 0L
    val stockBas = produit.stockActuelUnites <= produit.seuilAlerteUnites

    val containerColor = when {
        enRupture -> MaquisSurface.copy(alpha = 0.45f)
        qtePanier > 0L -> MaquisEmeraldDark.copy(alpha = 0.45f)
        else -> MaquisSurface
    }
    val borderColor = when {
        enRupture -> MaquisBorder.copy(alpha = 0.4f)
        qtePanier > 0L -> MaquisEmerald
        else -> MaquisBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(if (qtePanier > 0L) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = !enRupture, onClick = onClick)
            .padding(12.dp)
            .testTag("product_tile_${produit.id}")
    ) {
        if (qtePanier > 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaquisEmerald),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = qtePanier.toString(),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaquisBg
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(end = if (qtePanier > 0L) 28.dp else 0.dp)) {
                Text(
                    text = produit.nom,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enRupture) MaquisTextSecondary else MaquisTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                val badgeBg = when {
                    enRupture -> MaquisRose.copy(alpha = 0.2f)
                    stockBas -> MaquisAmber.copy(alpha = 0.2f)
                    else -> MaquisSurfaceAlt
                }
                val badgeText = when {
                    enRupture -> MaquisRose
                    stockBas -> MaquisAmber
                    else -> MaquisTextSecondary
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (enRupture) "Rupture" else formatStockCasierUnite(produit.stockActuelUnites, produit.unitesParCasier),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeText
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = formatFCFA(produit.prixVenteUnitaire),
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enRupture) MaquisTextSecondary else MaquisEmerald
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutBottomSheet(
    tableNom: String,
    total: Long,
    onDismiss: () -> Unit,
    onConfirm: (ModePaiement, String?, String?) -> Unit
) {
    var modePaiement by remember { mutableStateOf(ModePaiement.ESPECES) }
    var billetRecuText by remember { mutableStateOf("") }
    var clientNom by remember { mutableStateOf("") }
    var clientTel by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val billetRecu = billetRecuText.toLongOrNull() ?: 0L
    val monnaieARendre = (billetRecu - total).coerceAtLeast(0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaquisSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ENCAISSEMENT • $tableNom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaquisTextSecondary
                    )
                    Text(
                        text = formatFCFA(total),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaquisEmerald
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Annuler", tint = MaquisTextSecondary)
                }
            }

            // Payment mode grid (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentModeCard(
                        label = "Espèces",
                        icon = Icons.Filled.Payments,
                        selected = modePaiement == ModePaiement.ESPECES,
                        accentColor = MaquisEmerald,
                        onClick = { modePaiement = ModePaiement.ESPECES },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentModeCard(
                        label = "MTN MoMo",
                        icon = Icons.Filled.PhoneAndroid,
                        selected = modePaiement == ModePaiement.MTN_MOMO,
                        accentColor = MaquisMomoYellow,
                        onClick = { modePaiement = ModePaiement.MTN_MOMO },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentModeCard(
                        label = "Orange Money",
                        icon = Icons.Filled.PhoneAndroid,
                        selected = modePaiement == ModePaiement.ORANGE_MONEY,
                        accentColor = MaquisOrangeMoney,
                        onClick = { modePaiement = ModePaiement.ORANGE_MONEY },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentModeCard(
                        label = "Crédit / Ardoise",
                        icon = Icons.Filled.PersonAdd,
                        selected = modePaiement == ModePaiement.CREDIT,
                        accentColor = MaquisRose,
                        onClick = { modePaiement = ModePaiement.CREDIT },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (modePaiement == ModePaiement.ESPECES) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaquisSurfaceAlt)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Billet donné par le client (calcul monnaie)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaquisTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2000L, 5000L, 10000L).forEach { billet ->
                            OutlinedButton(
                                onClick = { billetRecuText = billet.toString() },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaquisBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text(formatNumberFR(billet), fontFamily = JetBrainsMonoFamily, color = MaquisTextPrimary)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = billetRecuText,
                        onValueChange = { billetRecuText = it.filter { c -> c.isDigit() } },
                        label = { Text("Ou saisir montant reçu (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaquisEmerald,
                            unfocusedBorderColor = MaquisBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (billetRecu >= total && billetRecu > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monnaie à rendre :", fontWeight = FontWeight.Bold, color = MaquisAmber)
                            Text(
                                text = formatFCFA(monnaieARendre),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaquisAmber
                            )
                        }
                    }
                }
            }

            if (modePaiement == ModePaiement.CREDIT) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaquisRose.copy(alpha = 0.1f))
                        .border(1.dp, MaquisRose.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = clientNom,
                        onValueChange = {
                            clientNom = it
                            validationError = null
                        },
                        label = { Text("Nom complet du client *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientTel,
                        onValueChange = { clientTel = it },
                        label = { Text("Téléphone WhatsApp (ex: 677001122)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (validationError != null) {
                Text(validationError!!, color = MaquisRose, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    if (modePaiement == ModePaiement.CREDIT && clientNom.isBlank()) {
                        validationError = "Le nom du client est obligatoire pour ouvrir une ardoise."
                        return@Button
                    }
                    onConfirm(
                        modePaiement,
                        clientNom.takeIf { modePaiement == ModePaiement.CREDIT },
                        clientTel.takeIf { modePaiement == ModePaiement.CREDIT }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_payment_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("CONFIRMER L'ENCAISSEMENT", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PaymentModeCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) accentColor else MaquisSurfaceAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) accentColor else MaquisBorder),
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaquisBg else MaquisTextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaquisBg else MaquisTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeclareLossDialog(
    produits: List<Produit>,
    onDismiss: () -> Unit,
    onSubmit: (Produit, Long, TypePerte, String) -> Unit
) {
    var selectedProd by remember { mutableStateOf(produits.first()) }
    var selectedType by remember { mutableStateOf(TypePerte.CASSE) }
    var quantite by remember { mutableLongStateOf(1L) }
    var motif by remember { mutableStateOf("Bouteille cassée en salle") }
    var expandedDropdown by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaquisRose)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Déclarer une perte de stock",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaquisRose
                    )
                }

                // Product picker button
                Surface(
                    onClick = { expandedDropdown = !expandedDropdown },
                    color = MaquisSurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaquisBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedProd.nom} (Reste : ${selectedProd.stockActuelUnites})",
                            color = MaquisTextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                AnimatedVisibility(visible = expandedDropdown) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaquisBg)
                            .border(1.dp, MaquisBorder, RoundedCornerShape(12.dp))
                    ) {
                        items(produits) { prod ->
                            Text(
                                text = "${prod.nom} (${prod.stockActuelUnites})",
                                color = MaquisTextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProd = prod
                                        expandedDropdown = false
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypePerte.entries.forEach { t ->
                        val active = selectedType == t
                        Surface(
                            onClick = { selectedType = t },
                            color = if (active) MaquisRose else MaquisSurfaceAlt,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = t.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaquisTextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaquisSurfaceAlt)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantité (unités)", color = MaquisTextPrimary, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { quantite = (quantite - 1L).coerceAtLeast(1L) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Moins", tint = MaquisTextPrimary)
                        }
                        Text(
                            text = quantite.toString(),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaquisTextPrimary
                        )
                        IconButton(onClick = { quantite += 1L }) {
                            Icon(Icons.Filled.Add, contentDescription = "Plus", tint = MaquisTextPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text("Motif") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Annuler", color = MaquisTextSecondary)
                    }
                    Button(
                        onClick = { onSubmit(selectedProd, quantite, selectedType, motif) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaquisRose, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
