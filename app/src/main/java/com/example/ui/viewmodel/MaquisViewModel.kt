package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategorieDepense
import com.example.data.model.CategorieProduit
import com.example.data.model.Depense
import com.example.data.model.LigneVente
import com.example.data.model.MaquisSettings
import com.example.data.model.ModePaiement
import com.example.data.model.PerteStock
import com.example.data.model.Produit
import com.example.data.model.Role
import com.example.data.model.StatutVente
import com.example.data.model.SuiviEmballage
import com.example.data.model.TypePerte
import com.example.data.model.Vente
import com.example.data.repository.MaquisRepository
import com.example.util.formatFCFA
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

enum class MaquisTab(val label: String) {
    CAISSE("Caisse"),
    STOCK("Stock"),
    DEPENSES("Dépenses"),
    BILAN("Bilan")
}

enum class PeriodeBilan(val label: String) {
    JOUR("Aujourd'hui"),
    SEMAINE("7 Jours"),
    MOIS("30 Jours")
}

data class PaidReceipt(
    val ticketNum: String,
    val tableNom: String,
    val lignes: List<LigneVente>,
    val total: Long,
    val mode: ModePaiement
)

data class ProductStat(
    val nom: String,
    val quantite: Long,
    val chiffreAffaires: Long,
    val margeNette: Long
)

data class ProfitMetrics(
    val argentEntreTotal: Long = 0L,
    val argentEncaisseReel: Long = 0L,
    val creditsARecuperer: Long = 0L,
    val coutMarchandises: Long = 0L,
    val totalDepenses: Long = 0L,
    val totalPertes: Long = 0L,
    val beneficeNetReel: Long = 0L,
    val especesTotal: Long = 0L,
    val mtnMomoTotal: Long = 0L,
    val orangeMoneyTotal: Long = 0L,
    val nbVentes: Int = 0,
    val topRentables: List<ProductStat> = emptyList(),
    val topVendus: List<ProductStat> = emptyList()
)

private data class LocalSessionState(
    val activeTab: MaquisTab = MaquisTab.CAISSE,
    val role: Role = Role.GERANT,
    val activeTable: String = "Comptoir",
    val selectedCategory: CategorieProduit? = null,
    val panierLocal: List<LigneVente> = emptyList(),
    val activeOpenVenteId: String? = null,
    val lastPaidReceipt: PaidReceipt? = null,
    val periodeBilan: PeriodeBilan = PeriodeBilan.JOUR,
    val snackbarMessage: String? = null,
    val isSyncing: Boolean = false
)

data class MaquisUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val settings: MaquisSettings = MaquisSettings(),
    val produits: List<Produit> = emptyList(),
    val emballages: List<SuiviEmballage> = emptyList(),
    val ventes: List<Vente> = emptyList(),
    val pertes: List<PerteStock> = emptyList(),
    val depenses: List<Depense> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isOnline: Boolean = true,
    val activeTab: MaquisTab = MaquisTab.CAISSE,
    val role: Role = Role.GERANT,
    val activeTable: String = "Comptoir",
    val selectedCategory: CategorieProduit? = null,
    val panierLocal: List<LigneVente> = emptyList(),
    val activeOpenVenteId: String? = null,
    val lastPaidReceipt: PaidReceipt? = null,
    val periodeBilan: PeriodeBilan = PeriodeBilan.JOUR,
    val snackbarMessage: String? = null,
    val isSyncing: Boolean = false
) {
    val tablesOuvertes: List<Vente>
        get() = ventes.filter { it.statut == StatutVente.OUVERTE.key }

    val ardoisesOuvertes: List<Vente>
        get() = ventes.filter { it.statut == StatutVente.CREDIT.key }

    val alertesStock: List<Produit>
        get() = produits.filter { it.stockActuelUnites <= it.seuilAlerteUnites }

    val totalPanier: Long
        get() = panierLocal.sumOf { it.prixVente * it.quantite }

    val nombreArticlesPanier: Long
        get() = panierLocal.sumOf { it.quantite }

    fun computeProfitMetrics(): ProfitMetrics {
        val now = Date()
        val calNow = Calendar.getInstance().apply { time = now }

        fun isWithinPeriod(ts: Timestamp?): Boolean {
            val date = ts?.toDate() ?: now
            return when (periodeBilan) {
                PeriodeBilan.JOUR -> {
                    val calItem = Calendar.getInstance().apply { time = date }
                    calItem.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                        calItem.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                }
                PeriodeBilan.SEMAINE -> {
                    val diffMs = now.time - date.time
                    diffMs <= 7L * 24L * 3600L * 1000L
                }
                PeriodeBilan.MOIS -> {
                    val diffMs = now.time - date.time
                    diffMs <= 30L * 24L * 3600L * 1000L
                }
            }
        }

        val ventesPeriod = ventes.filter {
            it.statut != StatutVente.OUVERTE.key &&
                it.statut != StatutVente.ANNULEE.key &&
                isWithinPeriod(it.createdAt)
        }
        val depensesPeriod = depenses.filter { isWithinPeriod(it.createdAt) }
        val pertesPeriod = pertes.filter { isWithinPeriod(it.createdAt) }

        val argentEntreTotal = ventesPeriod.sumOf { it.totalVente }
        val argentEncaisseReel = ventesPeriod.filter { it.statut == StatutVente.PAYEE.key }.sumOf { it.totalVente }
        val creditsARecuperer = ventesPeriod.filter { it.statut == StatutVente.CREDIT.key }.sumOf { it.totalVente }

        val coutMarchandises = ventesPeriod.sumOf { it.coutTotalAchat }
        val totalDepenses = depensesPeriod.sumOf { it.montant }
        val totalPertes = pertesPeriod.sumOf { it.valeurPerdue }

        val beneficeNetReel = argentEntreTotal - coutMarchandises - totalDepenses - totalPertes

        val especesTotal = ventesPeriod
            .filter { it.statut == StatutVente.PAYEE.key && it.modePaiement == ModePaiement.ESPECES.key }
            .sumOf { it.totalVente }
        val mtnMomoTotal = ventesPeriod
            .filter { it.statut == StatutVente.PAYEE.key && it.modePaiement == ModePaiement.MTN_MOMO.key }
            .sumOf { it.totalVente }
        val orangeMoneyTotal = ventesPeriod
            .filter { it.statut == StatutVente.PAYEE.key && it.modePaiement == ModePaiement.ORANGE_MONEY.key }
            .sumOf { it.totalVente }

        val statsMap = linkedMapOf<String, ProductStat>()
        ventesPeriod.forEach { v ->
            v.lignes.forEach { l ->
                val existing = statsMap[l.produitId]
                val qte = (existing?.quantite ?: 0L) + l.quantite
                val ca = (existing?.chiffreAffaires ?: 0L) + (l.prixVente * l.quantite)
                val marge = (existing?.margeNette ?: 0L) + ((l.prixVente - l.prixAchat) * l.quantite)
                statsMap[l.produitId] = ProductStat(l.nom, qte, ca, marge)
            }
        }

        return ProfitMetrics(
            argentEntreTotal = argentEntreTotal,
            argentEncaisseReel = argentEncaisseReel,
            creditsARecuperer = creditsARecuperer,
            coutMarchandises = coutMarchandises,
            totalDepenses = totalDepenses,
            totalPertes = totalPertes,
            beneficeNetReel = beneficeNetReel,
            especesTotal = especesTotal,
            mtnMomoTotal = mtnMomoTotal,
            orangeMoneyTotal = orangeMoneyTotal,
            nbVentes = ventesPeriod.size,
            topRentables = statsMap.values.sortedByDescending { it.margeNette }.take(4),
            topVendus = statsMap.values.sortedByDescending { it.quantite }.take(4)
        )
    }
}

private data class FirestoreDataBundle(
    val settings: MaquisSettings = MaquisSettings(),
    val produits: List<Produit> = emptyList(),
    val emballages: List<SuiviEmballage> = emptyList(),
    val ventes: List<Vente> = emptyList(),
    val pertes: List<PerteStock> = emptyList(),
    val depenses: List<Depense> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class MaquisViewModel(
    private val repository: MaquisRepository,
    private val currentUserId: String,
    isOnlineFlow: StateFlow<Boolean>
) : ViewModel() {

    private val localState = MutableStateFlow(LocalSessionState())
    private val streamError = MutableStateFlow<String?>(null)

    private val dataBundle = combine(
        repository.observeSettings().catch { e -> handleStreamException("settings", e); emit(null) },
        repository.observeProduits().catch { e -> handleStreamException("produits", e); emit(com.example.data.model.SyncedCollection()) },
        repository.observeEmballages().catch { e -> handleStreamException("emballages", e); emit(com.example.data.model.SyncedCollection()) },
        repository.observeVentes().catch { e -> handleStreamException("ventes", e); emit(com.example.data.model.SyncedCollection()) },
        repository.observePertes().catch { e -> handleStreamException("pertes", e); emit(com.example.data.model.SyncedCollection()) },
        repository.observeDepenses().catch { e -> handleStreamException("depenses", e); emit(com.example.data.model.SyncedCollection()) },
        streamError
    ) { values ->
        val settings = values[0] as? MaquisSettings ?: MaquisSettings()
        @Suppress("UNCHECKED_CAST")
        val prods = values[1] as com.example.data.model.SyncedCollection<Produit>
        @Suppress("UNCHECKED_CAST")
        val embs = values[2] as com.example.data.model.SyncedCollection<SuiviEmballage>
        @Suppress("UNCHECKED_CAST")
        val ventes = values[3] as com.example.data.model.SyncedCollection<Vente>
        @Suppress("UNCHECKED_CAST")
        val pertes = values[4] as com.example.data.model.SyncedCollection<PerteStock>
        @Suppress("UNCHECKED_CAST")
        val depenses = values[5] as com.example.data.model.SyncedCollection<Depense>
        val err = values[6] as? String

        val totalPending = prods.pendingCount + embs.pendingCount + ventes.pendingCount +
            pertes.pendingCount + depenses.pendingCount

        FirestoreDataBundle(
            settings = settings,
            produits = prods.items,
            emballages = embs.items,
            ventes = ventes.items,
            pertes = pertes.items,
            depenses = depenses.items,
            pendingSyncCount = totalPending,
            isLoading = false,
            errorMessage = err
        )
    }

    val uiState: StateFlow<MaquisUiState> = combine(
        dataBundle,
        localState,
        isOnlineFlow
    ) { bundle, local, online ->
        MaquisUiState(
            isLoading = bundle.isLoading,
            errorMessage = bundle.errorMessage,
            settings = bundle.settings,
            produits = bundle.produits,
            emballages = bundle.emballages,
            ventes = bundle.ventes,
            pertes = bundle.pertes,
            depenses = bundle.depenses,
            pendingSyncCount = bundle.pendingSyncCount,
            isOnline = online,
            activeTab = local.activeTab,
            role = local.role,
            activeTable = local.activeTable,
            selectedCategory = local.selectedCategory,
            panierLocal = local.panierLocal,
            activeOpenVenteId = local.activeOpenVenteId,
            lastPaidReceipt = local.lastPaidReceipt,
            periodeBilan = local.periodeBilan,
            snackbarMessage = local.snackbarMessage,
            isSyncing = local.isSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MaquisUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            try {
                repository.ensureSeededCatalogIfEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Initial catalog seed check failed (will retry when online): ${e.message}")
            }
        }
    }

    private fun handleStreamException(source: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $source for user $currentUserId", root)
        streamError.value = root.localizedMessage ?: "Erreur de chargement ($source)"
    }

    fun selectTab(tab: MaquisTab) {
        localState.update { it.copy(activeTab = tab) }
    }

    fun selectCategory(category: CategorieProduit?) {
        localState.update { it.copy(selectedCategory = category) }
    }

    fun setPeriodeBilan(periode: PeriodeBilan) {
        localState.update { it.copy(periodeBilan = periode) }
    }

    fun selectTable(tableNom: String) {
        val openVente = uiState.value.tablesOuvertes.firstOrNull { it.tableNom == tableNom }
        localState.update {
            it.copy(
                activeTable = tableNom,
                panierLocal = openVente?.lignes ?: emptyList(),
                activeOpenVenteId = openVente?.id
            )
        }
    }

    fun addToCart(produit: Produit) {
        if (produit.stockActuelUnites <= 0L) return
        val existingOpenVente = uiState.value.ventes.firstOrNull { it.id == localState.value.activeOpenVenteId }
        val alreadyOnTable = existingOpenVente?.lignes?.firstOrNull { it.produitId == produit.id }?.quantite ?: 0L
        val maxAllowed = produit.stockActuelUnites + alreadyOnTable

        localState.update { state ->
            val currentList = state.panierLocal
            val existingLine = currentList.firstOrNull { it.produitId == produit.id }
            val updated = if (existingLine != null) {
                if (existingLine.quantite >= maxAllowed) currentList
                else currentList.map { l ->
                    if (l.produitId == produit.id) l.copy(quantite = l.quantite + 1L) else l
                }
            } else {
                currentList + LigneVente(
                    produitId = produit.id,
                    nom = produit.nom,
                    quantite = 1L,
                    prixVente = produit.prixVenteUnitaire,
                    prixAchat = produit.prixAchatUnitaire
                )
            }
            state.copy(panierLocal = updated)
        }
    }

    fun updateCartQuantity(produitId: String, delta: Long) {
        val produit = uiState.value.produits.firstOrNull { it.id == produitId }
        val existingOpenVente = uiState.value.ventes.firstOrNull { it.id == localState.value.activeOpenVenteId }
        val alreadyOnTable = existingOpenVente?.lignes?.firstOrNull { it.produitId == produitId }?.quantite ?: 0L
        val maxAllowed = (produit?.stockActuelUnites ?: Long.MAX_VALUE) + alreadyOnTable

        localState.update { state ->
            val updated = state.panierLocal.mapNotNull { line ->
                if (line.produitId != produitId) line
                else {
                    val nextQte = line.quantite + delta
                    when {
                        nextQte <= 0L -> null
                        nextQte > maxAllowed -> line
                        else -> line.copy(quantite = nextQte)
                    }
                }
            }
            state.copy(panierLocal = updated)
        }
    }

    fun saveTableOuverte() {
        val snapshot = uiState.value
        val lignes = snapshot.panierLocal
        if (lignes.isEmpty()) return
        val tableNom = snapshot.activeTable
        val existingVente = snapshot.ventes.firstOrNull { it.id == snapshot.activeOpenVenteId }
        val serveurNom = if (snapshot.role == Role.PROPRIETAIRE) "Patron" else "Jean (Gérant)"
        val ticketNum = existingVente?.numeroTicket ?: "T-${(snapshot.ventes.size + 1).toString().padStart(3, '0')}"

        // Clear immediately if Comptoir; keep visible for named table
        localState.update { state ->
            val msg = if (snapshot.isOnline) "$tableNom enregistrée" else "$tableNom sauvée hors-ligne"
            if (tableNom == "Comptoir") {
                state.copy(panierLocal = emptyList(), activeOpenVenteId = null, snackbarMessage = msg)
            } else {
                state.copy(snackbarMessage = msg)
            }
        }

        viewModelScope.launch {
            try {
                val id = repository.saveOrUpdateTableOuverte(
                    tableNom = tableNom,
                    lignes = lignes,
                    serveurNom = serveurNom,
                    existingVente = existingVente,
                    produitsSnapshot = snapshot.produits,
                    ticketNumber = ticketNum
                )
                if (tableNom != "Comptoir") {
                    localState.update { it.copy(activeOpenVenteId = id) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur sauvegarde table") }
            }
        }
    }

    fun encaisserVente(
        modePaiement: ModePaiement,
        clientNom: String?,
        clientTel: String?
    ) {
        val snapshot = uiState.value
        val lignes = snapshot.panierLocal
        if (lignes.isEmpty()) return
        if (modePaiement == ModePaiement.CREDIT && clientNom.isNullOrBlank()) {
            localState.update { it.copy(snackbarMessage = "Nom du client obligatoire pour une ardoise.") }
            return
        }

        val tableNom = snapshot.activeTable
        val existingVente = snapshot.ventes.firstOrNull { it.id == snapshot.activeOpenVenteId }
        val serveurNom = if (snapshot.role == Role.PROPRIETAIRE) "Patron" else "Jean (Gérant)"
        val ticketNum = existingVente?.numeroTicket ?: "T-${(snapshot.ventes.size + 1).toString().padStart(3, '0')}"
        val total = lignes.sumOf { it.prixVente * it.quantite }

        val receipt = PaidReceipt(
            ticketNum = ticketNum,
            tableNom = tableNom,
            lignes = lignes,
            total = total,
            mode = modePaiement
        )

        localState.update {
            it.copy(
                panierLocal = emptyList(),
                activeOpenVenteId = null,
                lastPaidReceipt = receipt,
                snackbarMessage = if (snapshot.isOnline) {
                    "Encaissé (${formatFCFA(total)})"
                } else {
                    "Encaissé hors-ligne (${formatFCFA(total)})"
                }
            )
        }

        viewModelScope.launch {
            try {
                repository.encaisserVente(
                    tableNom = tableNom,
                    lignes = lignes,
                    modePaiement = modePaiement,
                    clientNom = clientNom,
                    clientTel = clientTel,
                    serveurNom = serveurNom,
                    existingVente = existingVente,
                    produitsSnapshot = snapshot.produits,
                    ticketNumber = ticketNum
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur encaissement") }
            }
        }
    }

    fun reglerArdoise(vente: Vente, modePaiement: ModePaiement) {
        localState.update { it.copy(snackbarMessage = "Ardoise de ${vente.clientNom} réglée") }
        viewModelScope.launch {
            try {
                repository.reglerArdoise(vente, modePaiement)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur règlement ardoise") }
            }
        }
    }

    fun declarerPerte(produit: Produit, quantite: Long, type: TypePerte, motif: String) {
        if (quantite <= 0L) return
        localState.update {
            it.copy(snackbarMessage = "Perte enregistrée : ${quantite}x ${produit.nom}")
        }
        viewModelScope.launch {
            try {
                repository.declarerPerte(produit, quantite, type, motif)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur déclaration perte") }
            }
        }
    }

    fun approvisionnerCasier(produit: Produit, casiers: Long, prixCasier: Long?) {
        if (casiers <= 0L) return
        val unites = casiers * produit.unitesParCasier.coerceAtLeast(1L)
        localState.update {
            it.copy(snackbarMessage = "+$unites unités ajoutées à ${produit.nom}")
        }
        viewModelScope.launch {
            try {
                repository.restockProduit(produit, casiers, prixCasier)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur réapprovisionnement") }
            }
        }
    }

    fun creerProduit(
        nom: String,
        categorie: CategorieProduit,
        unitesParCasier: Long,
        prixAchatUnitaire: Long,
        prixVenteUnitaire: Long,
        stockInitial: Long,
        seuilAlerte: Long,
        estConsigne: Boolean
    ) {
        if (nom.isBlank()) return
        localState.update { it.copy(snackbarMessage = "Produit \"$nom\" ajouté au catalogue") }
        viewModelScope.launch {
            try {
                repository.createProduit(
                    Produit(
                        nom = nom.trim(),
                        categorie = categorie.key,
                        unitesParCasier = unitesParCasier.coerceAtLeast(1L),
                        prixAchatUnitaire = prixAchatUnitaire.coerceAtLeast(0L),
                        prixVenteUnitaire = prixVenteUnitaire.coerceAtLeast(0L),
                        stockActuelUnites = stockInitial.coerceAtLeast(0L),
                        seuilAlerteUnites = seuilAlerte.coerceAtLeast(0L),
                        estConsigne = estConsigne
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur création produit") }
            }
        }
    }

    fun updateEmballage(emballage: SuiviEmballage, enCave: Long, vrac: Long, fournisseur: Long) {
        viewModelScope.launch {
            try {
                repository.updateEmballageCounts(emballage, enCave, vrac, fournisseur)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur mise à jour emballages") }
            }
        }
    }

    fun ajouterDepense(libelle: String, categorie: CategorieDepense, montant: Long) {
        if (libelle.isBlank() || montant <= 0L) return
        localState.update { it.copy(snackbarMessage = "Dépense enregistrée (-${formatFCFA(montant)})") }
        viewModelScope.launch {
            try {
                repository.ajouterDepense(libelle, categorie, montant)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur ajout dépense") }
            }
        }
    }

    fun supprimerDepense(depenseId: String) {
        localState.update { it.copy(snackbarMessage = "Dépense supprimée") }
        viewModelScope.launch {
            try {
                repository.supprimerDepense(depenseId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur suppression dépense") }
            }
        }
    }

    fun unlockOwnerWithPin(pinInput: String): Boolean {
        val expectedPin = uiState.value.settings.ownerPin.ifBlank { "1234" }
        return if (pinInput == expectedPin) {
            localState.update {
                it.copy(role = Role.PROPRIETAIRE, snackbarMessage = "Mode Propriétaire activé")
            }
            true
        } else {
            false
        }
    }

    fun lockToGerant() {
        localState.update {
            it.copy(role = Role.GERANT, snackbarMessage = "Mode Gérant / Serveur activé")
        }
    }

    fun updateOwnerPin(newPin: String) {
        if (newPin.length !in 4..8 || !newPin.all { it.isDigit() }) {
            localState.update { it.copy(snackbarMessage = "Le PIN doit contenir 4 à 8 chiffres.") }
            return
        }
        localState.update { it.copy(snackbarMessage = "Code PIN Propriétaire mis à jour") }
        viewModelScope.launch {
            try {
                repository.updateOwnerPin(newPin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update { it.copy(snackbarMessage = e.localizedMessage ?: "Erreur mise à jour PIN") }
            }
        }
    }

    fun syncNow() {
        if (!uiState.value.isOnline) {
            localState.update { it.copy(snackbarMessage = "Pas de réseau : données protégées en local.") }
            return
        }
        localState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val ok = repository.forceSyncWait()
            localState.update {
                it.copy(
                    isSyncing = false,
                    snackbarMessage = if (ok) "Synchronisation Cloud terminée" else "Synchro en attente de réseau"
                )
            }
        }
    }

    fun dismissReceiptBanner() {
        localState.update { it.copy(lastPaidReceipt = null) }
    }

    fun clearSnackbar() {
        localState.update { it.copy(snackbarMessage = null) }
    }

    private companion object {
        const val TAG = "MaquisViewModel"
    }
}
