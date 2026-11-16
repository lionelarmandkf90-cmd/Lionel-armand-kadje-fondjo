package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategorieProduit
import com.example.data.model.Produit
import com.example.data.model.Role
import com.example.data.model.SuiviEmballage
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MaquisAmber
import com.example.ui.theme.MaquisBg
import com.example.ui.theme.MaquisBorder
import com.example.ui.theme.MaquisEmerald
import com.example.ui.theme.MaquisRose
import com.example.ui.theme.MaquisSky
import com.example.ui.theme.MaquisSurface
import com.example.ui.theme.MaquisSurfaceAlt
import com.example.ui.theme.MaquisTextPrimary
import com.example.ui.theme.MaquisTextSecondary
import com.example.ui.viewmodel.MaquisUiState
import com.example.util.formatFCFA
import com.example.util.formatStockCasierUnite

@Composable
fun StockScreen(
    state: MaquisUiState,
    onRestockProduit: (Produit, Long, Long?) -> Unit,
    onCreateProduit: (String, CategorieProduit, Long, Long, Long, Long, Long, Boolean) -> Unit,
    onUpdateEmballage: (SuiviEmballage, Long, Long, Long) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Boissons & Plats, 1 = Emballages Consignés
    var showCreateProductDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaquisBg)
    ) {
        // Sub-navigation Segmented Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaquisSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubSegmentButton(
                label = "Produits & Casiers",
                selected = subTab == 0,
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f)
            )
            SubSegmentButton(
                label = "Emballages (${state.emballages.size})",
                selected = subTab == 1,
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        if (subTab == 0) {
            ProduitsStockTab(
                state = state,
                onRestockProduit = onRestockProduit,
                onOpenCreateProduct = { showCreateProductDialog = true }
            )
        } else {
            EmballagesTab(
                emballages = state.emballages,
                onUpdateEmballage = onUpdateEmballage
            )
        }
    }

    if (showCreateProductDialog) {
        BackHandler { showCreateProductDialog = false }
        CreateProductDialog(
            onDismiss = { showCreateProductDialog = false },
            onConfirm = { nom, cat, upc, pAchat, pVente, stockInit, seuil, consigne ->
                onCreateProduit(nom, cat, upc, pAchat, pVente, stockInit, seuil, consigne)
                showCreateProductDialog = false
            }
        )
    }
}

@Composable
private fun SubSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaquisEmerald else MaquisSurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaquisEmerald else MaquisBorder),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaquisBg else MaquisTextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProduitsStockTab(
    state: MaquisUiState,
    onRestockProduit: (Produit, Long, Long?) -> Unit,
    onOpenCreateProduct: () -> Unit
) {
    val isOwner = state.role == Role.PROPRIETAIRE
    var selectedProduit by remember(state.produits) {
        mutableStateOf(state.produits.firstOrNull())
    }
    var showPicker by remember { mutableStateOf(false) }
    var nbCasiersText by remember { mutableStateOf("1") }
    var prixCasierText by remember { mutableStateOf("") }

    LaunchedEffect(selectedProduit?.id) {
        selectedProduit?.let { prod ->
            prixCasierText = (prod.prixAchatUnitaire * prod.unitesParCasier).toString()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Restock Card
        item {
            Surface(
                color = MaquisSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaquisBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalShipping, null, tint = MaquisEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "+ Entrée Casier / Pack",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaquisEmerald
                            )
                        }
                        if (isOwner) {
                            OutlinedButton(
                                onClick = onOpenCreateProduct,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaquisEmerald.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp).testTag("new_product_button")
                            ) {
                                Icon(Icons.Filled.Add, null, tint = MaquisEmerald, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Produit", color = MaquisEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedProduit != null) {
                        val prod = selectedProduit!!
                        Surface(
                            onClick = { showPicker = !showPicker },
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
                                Column {
                                    Text(prod.nom, color = MaquisTextPrimary, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${prod.unitesParCasier} unités / casier • Stock : ${formatStockCasierUnite(prod.stockActuelUnites, prod.unitesParCasier)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaquisTextSecondary
                                    )
                                }
                                Icon(Icons.Filled.ArrowDropDown, null, tint = MaquisTextSecondary)
                            }
                        }

                        AnimatedVisibility(visible = showPicker) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaquisBg)
                                    .border(1.dp, MaquisBorder, RoundedCornerShape(12.dp))
                            ) {
                                state.produits.forEach { item ->
                                    Text(
                                        text = "${item.nom} (${item.unitesParCasier}/casier)",
                                        color = MaquisTextPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedProduit = item
                                                showPicker = false
                                            }
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = nbCasiersText,
                                onValueChange = { nbCasiersText = it.filter { c -> c.isDigit() } },
                                label = { Text("Nb casiers") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (isOwner) {
                                OutlinedTextField(
                                    value = prixCasierText,
                                    onValueChange = { prixCasierText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Prix achat/casier") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }

                        val casiersCount = (nbCasiersText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                        val addedUnits = casiersCount * prod.unitesParCasier
                        if (addedUnits > 0L) {
                            Text(
                                text = "Soit +$addedUnits unités ajoutées au stock${if (prod.estConsigne) " (casiers vides ajustés auto)" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaquisEmerald
                            )
                        }

                        Button(
                            onClick = {
                                if (casiersCount > 0L) {
                                    val prixCasier = if (isOwner) prixCasierText.toLongOrNull() else null
                                    onRestockProduit(prod, casiersCount, prixCasier)
                                    nbCasiersText = "1"
                                }
                            },
                            enabled = casiersCount > 0L,
                            colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("add_stock_button")
                        ) {
                            Text("Ajouter au Stock", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "INVENTAIRE TEMPS RÉEL (${state.produits.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaquisTextSecondary
            )
        }

        items(state.produits, key = { it.id }) { prod ->
            val lowStock = prod.stockActuelUnites <= prod.seuilAlerteUnites
            Surface(
                color = MaquisSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (lowStock) MaquisRose.copy(alpha = 0.45f) else MaquisBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = prod.nom,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaquisTextPrimary
                            )
                            if (lowStock) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Warning, null, tint = MaquisRose, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                append("Vente : ${formatFCFA(prod.prixVenteUnitaire)}")
                                if (isOwner) {
                                    append(" • Achat : ${formatFCFA(prod.prixAchatUnitaire)}")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaquisTextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatStockCasierUnite(prod.stockActuelUnites, prod.unitesParCasier),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (lowStock) MaquisRose else MaquisEmerald
                        )
                        Text(
                            text = "${prod.stockActuelUnites} unités",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaquisTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmballagesTab(
    emballages: List<SuiviEmballage>,
    onUpdateEmballage: (SuiviEmballage, Long, Long, Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = MaquisSurfaceAlt.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaquisBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inventory, null, tint = MaquisAmber)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Suivez vos casiers vides en cave, bouteilles en vrac et consignes chez le fournisseur.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaquisTextSecondary
                    )
                }
            }
        }

        items(emballages, key = { it.id }) { emb ->
            Surface(
                color = MaquisSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaquisBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = emb.typeEmballage,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaquisTextPrimary
                    )
                    EmballageCounterRow(
                        label = "Casiers vides en cave",
                        value = emb.casiersVidesEnCave,
                        accent = MaquisEmerald,
                        onDecrement = {
                            onUpdateEmballage(emb, (emb.casiersVidesEnCave - 1L).coerceAtLeast(0L), emb.bouteillesVidesVrac, emb.casiersChezFournisseur)
                        },
                        onIncrement = {
                            onUpdateEmballage(emb, emb.casiersVidesEnCave + 1L, emb.bouteillesVidesVrac, emb.casiersChezFournisseur)
                        }
                    )
                    EmballageCounterRow(
                        label = "Bouteilles vides (vrac)",
                        value = emb.bouteillesVidesVrac,
                        accent = MaquisSky,
                        onDecrement = {
                            onUpdateEmballage(emb, emb.casiersVidesEnCave, (emb.bouteillesVidesVrac - 1L).coerceAtLeast(0L), emb.casiersChezFournisseur)
                        },
                        onIncrement = {
                            onUpdateEmballage(emb, emb.casiersVidesEnCave, emb.bouteillesVidesVrac + 1L, emb.casiersChezFournisseur)
                        }
                    )
                    EmballageCounterRow(
                        label = "Casiers chez fournisseur",
                        value = emb.casiersChezFournisseur,
                        accent = MaquisAmber,
                        onDecrement = {
                            onUpdateEmballage(emb, emb.casiersVidesEnCave, emb.bouteillesVidesVrac, (emb.casiersChezFournisseur - 1L).coerceAtLeast(0L))
                        },
                        onIncrement = {
                            onUpdateEmballage(emb, emb.casiersVidesEnCave, emb.bouteillesVidesVrac, emb.casiersChezFournisseur + 1L)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmballageCounterRow(
    label: String,
    value: Long,
    accent: androidx.compose.ui.graphics.Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaquisSurfaceAlt)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaquisTextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuer", tint = MaquisTextSecondary)
            }
            Text(
                text = value.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = accent,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.Add, contentDescription = "Augmenter", tint = accent)
            }
        }
    }
}

@Composable
private fun CreateProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, CategorieProduit, Long, Long, Long, Long, Long, Boolean) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var categorie by remember { mutableStateOf(CategorieProduit.BIERES) }
    var unitesParCasier by remember { mutableStateOf("12") }
    var prixAchatUnit by remember { mutableStateOf("") }
    var prixVenteUnit by remember { mutableStateOf("") }
    var stockInitial by remember { mutableStateOf("24") }
    var seuilAlerte by remember { mutableStateOf("12") }
    var estConsigne by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaquisSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaquisBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Nouveau Produit", style = MaterialTheme.typography.titleLarge, color = MaquisTextPrimary)
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom du produit *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(CategorieProduit.BIERES, CategorieProduit.GAZEUSES, CategorieProduit.PLATS_GRILLADES).forEach { cat ->
                        Surface(
                            onClick = { categorie = cat },
                            color = if (categorie == cat) MaquisEmerald else MaquisSurfaceAlt,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    cat.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (categorie == cat) MaquisBg else MaquisTextSecondary
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitesParCasier,
                        onValueChange = { unitesParCasier = it.filter { c -> c.isDigit() } },
                        label = { Text("Unités/casier") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockInitial,
                        onValueChange = { stockInitial = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock initial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prixAchatUnit,
                        onValueChange = { prixAchatUnit = it.filter { c -> c.isDigit() } },
                        label = { Text("Achat unit.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prixVenteUnit,
                        onValueChange = { prixVenteUnit = it.filter { c -> c.isDigit() } },
                        label = { Text("Vente unit. *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Emballage consigné ?", color = MaquisTextPrimary, style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = estConsigne, onCheckedChange = { estConsigne = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Annuler", color = MaquisTextSecondary)
                    }
                    Button(
                        onClick = {
                            val pv = prixVenteUnit.toLongOrNull() ?: 0L
                            if (nom.isNotBlank() && pv > 0L) {
                                onConfirm(
                                    nom,
                                    categorie,
                                    (unitesParCasier.toLongOrNull() ?: 1L).coerceAtLeast(1L),
                                    prixAchatUnit.toLongOrNull() ?: 0L,
                                    pv,
                                    stockInitial.toLongOrNull() ?: 0L,
                                    seuilAlerte.toLongOrNull() ?: 6L,
                                    estConsigne
                                )
                            }
                        },
                        enabled = nom.isNotBlank() && (prixVenteUnit.toLongOrNull() ?: 0L) > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = MaquisEmerald, contentColor = MaquisBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Créer", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
