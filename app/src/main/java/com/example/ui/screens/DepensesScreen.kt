package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.CategorieDepense
import com.example.data.model.Role
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MaquisAmber
import com.example.ui.theme.MaquisBg
import com.example.ui.theme.MaquisBorder
import com.example.ui.theme.MaquisRose
import com.example.ui.theme.MaquisSurface
import com.example.ui.theme.MaquisSurfaceAlt
import com.example.ui.theme.MaquisTextPrimary
import com.example.ui.theme.MaquisTextSecondary
import com.example.ui.viewmodel.MaquisUiState
import com.example.util.formatFCFA

@Composable
fun DepensesScreen(
    state: MaquisUiState,
    onAjouterDepense: (String, CategorieDepense, Long) -> Unit,
    onSupprimerDepense: (String) -> Unit
) {
    var libelle by remember { mutableStateOf("") }
    var montantText by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(CategorieDepense.GLACE) }

    val totalDepenses = remember(state.depenses) {
        state.depenses.sumOf { it.montant }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaquisBg)
    ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ReceiptLong, null, tint = MaquisAmber)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Enregistrer une Dépense",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaquisAmber
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CategorieDepense.entries) { cat ->
                            val selected = selectedCat == cat
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCat = cat },
                                label = { Text(cat.label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaquisAmber,
                                    selectedLabelColor = MaquisBg,
                                    containerColor = MaquisSurfaceAlt,
                                    labelColor = MaquisTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaquisBorder
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = libelle,
                        onValueChange = { libelle = it },
                        label = { Text("Ex: 2 sacs de glace, Charbon, Taxi...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_label_input")
                    )

                    OutlinedTextField(
                        value = montantText,
                        onValueChange = { montantText = it.filter { c -> c.isDigit() } },
                        label = { Text("Montant en FCFA (ex: 2000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input")
                    )

                    val montant = montantText.toLongOrNull() ?: 0L
                    Button(
                        onClick = {
                            if (libelle.isNotBlank() && montant > 0L) {
                                onAjouterDepense(libelle, selectedCat, montant)
                                libelle = ""
                                montantText = ""
                            }
                        },
                        enabled = libelle.isNotBlank() && montant > 0L,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaquisAmber,
                            contentColor = MaquisBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_expense_button")
                    ) {
                        Text("Enregistrer Dépense", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaquisAmber.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL DÉPENSES (${state.depenses.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaquisAmber
                )
                Text(
                    text = "-${formatFCFA(totalDepenses)}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaquisAmber
                )
            }
        }

        if (state.depenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune dépense enregistrée.",
                        color = MaquisTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(state.depenses, key = { it.id }) { dep ->
                val cat = CategorieDepense.fromKey(dep.categorie)
                Surface(
                    color = MaquisSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaquisBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dep.libelle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaquisTextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaquisTextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-${formatFCFA(dep.montant)}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaquisAmber
                            )
                            if (state.role == Role.PROPRIETAIRE) {
                                IconButton(
                                    onClick = { onSupprimerDepense(dep.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Supprimer dépense",
                                        tint = MaquisRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
