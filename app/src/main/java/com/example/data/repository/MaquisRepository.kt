package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CategorieDepense
import com.example.data.model.CategorieProduit
import com.example.data.model.Depense
import com.example.data.model.LigneVente
import com.example.data.model.MaquisSettings
import com.example.data.model.ModePaiement
import com.example.data.model.PerteStock
import com.example.data.model.Produit
import com.example.data.model.StatutVente
import com.example.data.model.SuiviEmballage
import com.example.data.model.SyncedCollection
import com.example.data.model.TypePerte
import com.example.data.model.Vente
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MaquisRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Utilisateur non connecté via Google Sign-In.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeSettings(): Flow<MaquisSettings?> = flow {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("settings").document(CONFIG_DOC_ID)
        emitAll(
            docRef.snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(MaquisSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeProduits(): Flow<SyncedCollection<Produit>> = flow {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("produits")
        emitAll(
            colRef.snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    val items = snap.toObjects(Produit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val pending = snap.documents
                        .filter { it.metadata.hasPendingWrites() }
                        .map { it.id }
                        .toSet()
                    SyncedCollection(items.sortedBy { it.nom }, pending)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    fun observeEmballages(): Flow<SyncedCollection<SuiviEmballage>> = flow {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("emballages")
        emitAll(
            colRef.snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    val items = snap.toObjects(SuiviEmballage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val pending = snap.documents.filter { it.metadata.hasPendingWrites() }.map { it.id }.toSet()
                    SyncedCollection(items.sortedBy { it.typeEmballage }, pending)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    fun observeVentes(): Flow<SyncedCollection<Vente>> = flow {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("ventes")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    val items = snap.toObjects(Vente::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val pending = snap.documents.filter { it.metadata.hasPendingWrites() }.map { it.id }.toSet()
                    SyncedCollection(items, pending)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    fun observePertes(): Flow<SyncedCollection<PerteStock>> = flow {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("pertes")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    val items = snap.toObjects(PerteStock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val pending = snap.documents.filter { it.metadata.hasPendingWrites() }.map { it.id }.toSet()
                    SyncedCollection(items, pending)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    fun observeDepenses(): Flow<SyncedCollection<Depense>> = flow {
        val uid = requireUserId()
        val colRef = userDoc(uid).collection("depenses")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    val items = snap.toObjects(Depense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val pending = snap.documents.filter { it.metadata.hasPendingWrites() }.map { it.id }.toSet()
                    SyncedCollection(items, pending)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureSeededCatalogIfEmpty() {
        val uid = requireUserId()
        val settingsRef = userDoc(uid).collection("settings").document(CONFIG_DOC_ID)
        try {
            val existingSettings = settingsRef.get().await()
            if (existingSettings.exists() && existingSettings.getBoolean("seededInitialData") == true) {
                return
            }
            val existingProducts = userDoc(uid).collection("produits").limit(1).get().await()
            if (!existingProducts.isEmpty) {
                if (!existingSettings.exists()) {
                    settingsRef.set(MaquisSettings(seededInitialData = true).toCreateMap()).await()
                }
                return
            }

            val batch = db.batch()
            batch.set(settingsRef, MaquisSettings(seededInitialData = true).toCreateMap())

            initialProduits().forEach { (id, prod) ->
                val ref = userDoc(uid).collection("produits").document(id)
                batch.set(ref, prod.toCreateMap())
            }
            initialEmballages().forEach { (id, emb) ->
                val ref = userDoc(uid).collection("emballages").document(id)
                batch.set(ref, emb.toCreateMap())
            }
            initialDepenses().forEach { (id, dep) ->
                val ref = userDoc(uid).collection("depenses").document(id)
                batch.set(ref, dep.toCreateMap())
            }
            initialPertes().forEach { (id, perte) ->
                val ref = userDoc(uid).collection("pertes").document(id)
                batch.set(ref, perte.toCreateMap())
            }
            initialVentes().forEach { (id, vente) ->
                val ref = userDoc(uid).collection("ventes").document(id)
                batch.set(ref, vente.toCreateMap())
            }

            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, settingsRef.path, auth)
            throw e
        }
    }

    suspend fun updateOwnerPin(newPin: String) {
        val uid = requireUserId()
        val settingsRef = userDoc(uid).collection("settings").document(CONFIG_DOC_ID)
        try {
            settingsRef.update(
                mapOf(
                    "ownerPin" to newPin,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, settingsRef.path, auth)
            throw e
        }
    }

    suspend fun createProduit(produit: Produit): String {
        val uid = requireUserId()
        val docId = produit.id.takeIf { it.isNotBlank() } ?: "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = userDoc(uid).collection("produits").document(docId)
        try {
            docRef.set(produit.toCreateMap()).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun restockProduit(
        produit: Produit,
        casiersAjoutes: Long,
        nouveauPrixAchatCasier: Long?
    ) {
        val uid = requireUserId()
        require(produit.id.isNotBlank()) { "Produit ID manquant" }
        val unitesAjoutees = casiersAjoutes * produit.unitesParCasier.coerceAtLeast(1L)
        val nouveauStock = (produit.stockActuelUnites + unitesAjoutees).coerceAtLeast(0L)
        val nouveauPrixUnit = if (nouveauPrixAchatCasier != null && nouveauPrixAchatCasier > 0L) {
            nouveauPrixAchatCasier / produit.unitesParCasier.coerceAtLeast(1L)
        } else {
            produit.prixAchatUnitaire
        }

        val docRef = userDoc(uid).collection("produits").document(produit.id)
        try {
            docRef.update(
                mapOf(
                    "stockActuelUnites" to nouveauStock,
                    "prixAchatUnitaire" to nouveauPrixUnit,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // If product is returnable (consigné), adjust matching empty crates if present
            if (produit.estConsigne && casiersAjoutes > 0L) {
                val targetEmbId = if (produit.unitesParCasier >= 24L) "emb_24_33" else "emb_12_65"
                val embRef = userDoc(uid).collection("emballages").document(targetEmbId)
                val embSnap = embRef.get().await()
                if (embSnap.exists()) {
                    val enCave = (embSnap.getLong("casiersVidesEnCave") ?: 0L)
                    val chezFournisseur = (embSnap.getLong("casiersChezFournisseur") ?: 0L)
                    embRef.update(
                        mapOf(
                            "casiersVidesEnCave" to (enCave - casiersAjoutes).coerceAtLeast(0L),
                            "casiersChezFournisseur" to (chezFournisseur + casiersAjoutes).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateEmballageCounts(
        emballage: SuiviEmballage,
        casiersVidesEnCave: Long,
        bouteillesVidesVrac: Long,
        casiersChezFournisseur: Long
    ) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("emballages").document(emballage.id)
        try {
            docRef.update(
                mapOf(
                    "casiersVidesEnCave" to casiersVidesEnCave.coerceAtLeast(0L),
                    "bouteillesVidesVrac" to bouteillesVidesVrac.coerceAtLeast(0L),
                    "casiersChezFournisseur" to casiersChezFournisseur.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun saveOrUpdateTableOuverte(
        tableNom: String,
        lignes: List<LigneVente>,
        serveurNom: String,
        existingVente: Vente?,
        produitsSnapshot: List<Produit>,
        ticketNumber: String
    ): String {
        val uid = requireUserId()
        require(lignes.isNotEmpty()) { "Le panier ne peut pas être vide." }
        val totalVente = lignes.sumOf { it.prixVente * it.quantite }
        val coutTotalAchat = lignes.sumOf { it.prixAchat * it.quantite }

        val batch = db.batch()
        val anciennesLignes = existingVente?.lignes ?: emptyList()

        // Differential stock adjustment
        produitsSnapshot.forEach { prod ->
            val qteNouvelle = lignes.firstOrNull { it.produitId == prod.id }?.quantite ?: 0L
            val qteAncienne = anciennesLignes.firstOrNull { it.produitId == prod.id }?.quantite ?: 0L
            val delta = qteNouvelle - qteAncienne
            if (delta != 0L) {
                val prodRef = userDoc(uid).collection("produits").document(prod.id)
                val nextStock = (prod.stockActuelUnites - delta).coerceAtLeast(0L)
                batch.update(
                    prodRef,
                    mapOf(
                        "stockActuelUnites" to nextStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        val venteId = existingVente?.id?.takeIf { it.isNotBlank() }
            ?: "vente_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val venteRef = userDoc(uid).collection("ventes").document(venteId)

        if (existingVente != null && existingVente.id.isNotBlank()) {
            batch.update(
                venteRef,
                mapOf(
                    "tableNom" to tableNom,
                    "lignes" to lignes.map { it.toMap() },
                    "totalVente" to totalVente,
                    "coutTotalAchat" to coutTotalAchat,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            val newVente = Vente(
                numeroTicket = ticketNumber,
                tableNom = tableNom,
                statut = StatutVente.OUVERTE.key,
                lignes = lignes,
                totalVente = totalVente,
                coutTotalAchat = coutTotalAchat,
                serveurNom = serveurNom
            )
            batch.set(venteRef, newVente.toCreateMap())
        }

        try {
            batch.commit().await()
            return venteId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, venteRef.path, auth)
            throw e
        }
    }

    suspend fun encaisserVente(
        tableNom: String,
        lignes: List<LigneVente>,
        modePaiement: ModePaiement,
        clientNom: String?,
        clientTel: String?,
        serveurNom: String,
        existingVente: Vente?,
        produitsSnapshot: List<Produit>,
        ticketNumber: String
    ): String {
        val uid = requireUserId()
        require(lignes.isNotEmpty()) { "Aucune ligne à encaisser." }
        if (modePaiement == ModePaiement.CREDIT) {
            require(!clientNom.isNullOrBlank()) { "Le nom du client est obligatoire pour une ardoise." }
        }

        val totalVente = lignes.sumOf { it.prixVente * it.quantite }
        val coutTotalAchat = lignes.sumOf { it.prixAchat * it.quantite }
        val statutFinal = if (modePaiement == ModePaiement.CREDIT) StatutVente.CREDIT else StatutVente.PAYEE

        val batch = db.batch()
        val anciennesLignes = existingVente?.lignes ?: emptyList()

        produitsSnapshot.forEach { prod ->
            val qteNouvelle = lignes.firstOrNull { it.produitId == prod.id }?.quantite ?: 0L
            val qteAncienne = anciennesLignes.firstOrNull { it.produitId == prod.id }?.quantite ?: 0L
            val delta = qteNouvelle - qteAncienne
            if (delta != 0L) {
                val prodRef = userDoc(uid).collection("produits").document(prod.id)
                val nextStock = (prod.stockActuelUnites - delta).coerceAtLeast(0L)
                batch.update(
                    prodRef,
                    mapOf(
                        "stockActuelUnites" to nextStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        val venteId = existingVente?.id?.takeIf { it.isNotBlank() }
            ?: "vente_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val venteRef = userDoc(uid).collection("ventes").document(venteId)

        if (existingVente != null && existingVente.id.isNotBlank()) {
            val updates = mutableMapOf<String, Any?>(
                "tableNom" to tableNom,
                "lignes" to lignes.map { it.toMap() },
                "totalVente" to totalVente,
                "coutTotalAchat" to coutTotalAchat,
                "statut" to statutFinal.key,
                "modePaiement" to modePaiement.key,
                "clientNom" to clientNom?.takeIf { it.isNotBlank() },
                "clientTel" to clientTel?.takeIf { it.isNotBlank() },
                "closedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.update(venteRef, updates)
        } else {
            val newVente = Vente(
                numeroTicket = ticketNumber,
                tableNom = tableNom,
                statut = statutFinal.key,
                lignes = lignes,
                totalVente = totalVente,
                coutTotalAchat = coutTotalAchat,
                modePaiement = modePaiement.key,
                clientNom = clientNom,
                clientTel = clientTel,
                serveurNom = serveurNom
            )
            batch.set(venteRef, newVente.toCreateMap())
        }

        try {
            batch.commit().await()
            return venteId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, venteRef.path, auth)
            throw e
        }
    }

    suspend fun reglerArdoise(vente: Vente, modePaiement: ModePaiement) {
        val uid = requireUserId()
        require(vente.id.isNotBlank()) { "Vente ID manquant" }
        require(vente.statut == StatutVente.CREDIT.key) { "Cette vente n'est pas une ardoise ouverte." }
        val venteRef = userDoc(uid).collection("ventes").document(vente.id)
        try {
            venteRef.update(
                mapOf(
                    "statut" to StatutVente.PAYEE.key,
                    "modePaiement" to modePaiement.key,
                    "closedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, venteRef.path, auth)
            throw e
        }
    }

    suspend fun declarerPerte(
        produit: Produit,
        quantite: Long,
        type: TypePerte,
        motif: String
    ): String {
        val uid = requireUserId()
        require(quantite >= 1L) { "Quantité invalide" }
        val perteId = "loss_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val perteRef = userDoc(uid).collection("pertes").document(perteId)
        val prodRef = userDoc(uid).collection("produits").document(produit.id)

        val perte = PerteStock(
            produitId = produit.id,
            produitNom = produit.nom,
            type = type.key,
            quantite = quantite,
            valeurPerdue = quantite * produit.prixAchatUnitaire,
            motif = motif.ifBlank { type.label }
        )

        val batch = db.batch()
        batch.set(perteRef, perte.toCreateMap())
        batch.update(
            prodRef,
            mapOf(
                "stockActuelUnites" to (produit.stockActuelUnites - quantite).coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
            return perteId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, perteRef.path, auth)
            throw e
        }
    }

    suspend fun ajouterDepense(
        libelle: String,
        categorie: CategorieDepense,
        montant: Long
    ): String {
        val uid = requireUserId()
        require(libelle.isNotBlank() && montant > 0L) { "Libellé et montant positif requis." }
        val depId = "dep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val depRef = userDoc(uid).collection("depenses").document(depId)
        val depense = Depense(
            libelle = libelle.trim(),
            categorie = categorie.key,
            montant = montant
        )
        try {
            depRef.set(depense.toCreateMap()).await()
            return depId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, depRef.path, auth)
            throw e
        }
    }

    suspend fun supprimerDepense(depenseId: String) {
        val uid = requireUserId()
        val depRef = userDoc(uid).collection("depenses").document(depenseId)
        try {
            depRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, depRef.path, auth)
            throw e
        }
    }

    suspend fun forceSyncWait(): Boolean {
        return try {
            db.waitForPendingWrites().await()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: error
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
    }

    companion object {
        private const val CONFIG_DOC_ID = "config"

        fun initialProduits(): List<Pair<String, Produit>> = listOf(
            "p1" to Produit(nom = "33 Export 65cl", categorie = CategorieProduit.BIERES.key, unitesParCasier = 12, prixAchatUnitaire = 475, prixVenteUnitaire = 700, stockActuelUnites = 42, seuilAlerteUnites = 12, estConsigne = true),
            "p2" to Produit(nom = "Castel Beer 65cl", categorie = CategorieProduit.BIERES.key, unitesParCasier = 12, prixAchatUnitaire = 475, prixVenteUnitaire = 700, stockActuelUnites = 9, seuilAlerteUnites = 12, estConsigne = true),
            "p3" to Produit(nom = "Guinness Smooth 33cl", categorie = CategorieProduit.BIERES.key, unitesParCasier = 24, prixAchatUnitaire = 550, prixVenteUnitaire = 900, stockActuelUnites = 36, seuilAlerteUnites = 24, estConsigne = true),
            "p4" to Produit(nom = "Kadji Beer 65cl", categorie = CategorieProduit.BIERES.key, unitesParCasier = 12, prixAchatUnitaire = 500, prixVenteUnitaire = 750, stockActuelUnites = 24, seuilAlerteUnites = 12, estConsigne = true),
            "p5" to Produit(nom = "Top Pamplemousse 50cl", categorie = CategorieProduit.GAZEUSES.key, unitesParCasier = 24, prixAchatUnitaire = 280, prixVenteUnitaire = 500, stockActuelUnites = 48, seuilAlerteUnites = 12, estConsigne = false),
            "p6" to Produit(nom = "Malta Guinness", categorie = CategorieProduit.GAZEUSES.key, unitesParCasier = 24, prixAchatUnitaire = 350, prixVenteUnitaire = 600, stockActuelUnites = 14, seuilAlerteUnites = 12, estConsigne = true),
            "p7" to Produit(nom = "Eau Minérale 1.5L", categorie = CategorieProduit.GAZEUSES.key, unitesParCasier = 6, prixAchatUnitaire = 250, prixVenteUnitaire = 500, stockActuelUnites = 18, seuilAlerteUnites = 6, estConsigne = false),
            "p8" to Produit(nom = "Vin Rouge Baron 75cl", categorie = CategorieProduit.VINS_SPIRITUEUX.key, unitesParCasier = 6, prixAchatUnitaire = 2800, prixVenteUnitaire = 5000, stockActuelUnites = 5, seuilAlerteUnites = 6, estConsigne = false),
            "p9" to Produit(nom = "Whisky Black Label (Verre)", categorie = CategorieProduit.VINS_SPIRITUEUX.key, unitesParCasier = 12, prixAchatUnitaire = 1200, prixVenteUnitaire = 2500, stockActuelUnites = 10, seuilAlerteUnites = 4, estConsigne = false),
            "p10" to Produit(nom = "Poisson Braisé + Miondo", categorie = CategorieProduit.PLATS_GRILLADES.key, unitesParCasier = 1, prixAchatUnitaire = 2200, prixVenteUnitaire = 4000, stockActuelUnites = 15, seuilAlerteUnites = 3, estConsigne = false),
            "p11" to Produit(nom = "Brochettes Soja (Portion)", categorie = CategorieProduit.PLATS_GRILLADES.key, unitesParCasier = 1, prixAchatUnitaire = 600, prixVenteUnitaire = 1200, stockActuelUnites = 20, seuilAlerteUnites = 5, estConsigne = false),
            "p12" to Produit(nom = "Poulet Braisé Entier", categorie = CategorieProduit.PLATS_GRILLADES.key, unitesParCasier = 1, prixAchatUnitaire = 3200, prixVenteUnitaire = 5500, stockActuelUnites = 8, seuilAlerteUnites = 2, estConsigne = false)
        )

        fun initialEmballages(): List<Pair<String, SuiviEmballage>> = listOf(
            "emb_12_65" to SuiviEmballage(typeEmballage = "Casier 12 × 65cl", casiersVidesEnCave = 14, bouteillesVidesVrac = 18, casiersChezFournisseur = 6),
            "emb_24_33" to SuiviEmballage(typeEmballage = "Casier 24 × 33cl", casiersVidesEnCave = 8, bouteillesVidesVrac = 9, casiersChezFournisseur = 3)
        )

        fun initialDepenses(): List<Pair<String, Depense>> = listOf(
            "d_demo_1" to Depense(libelle = "3 sacs de glace", categorie = CategorieDepense.GLACE.key, montant = 1500),
            "d_demo_2" to Depense(libelle = "Sac de charbon grillades", categorie = CategorieDepense.CHARBON_GAZ.key, montant = 2500)
        )

        fun initialPertes(): List<Pair<String, PerteStock>> = listOf(
            "loss_demo_1" to PerteStock(produitId = "p2", produitNom = "Castel Beer 65cl", type = TypePerte.CASSE.key, quantite = 1, valeurPerdue = 475, motif = "Bouteille tombée au bar")
        )

        fun initialVentes(): List<Pair<String, Vente>> = listOf(
            "v_demo_1" to Vente(
                numeroTicket = "T-001",
                tableNom = "Table 1",
                statut = StatutVente.PAYEE.key,
                lignes = listOf(
                    LigneVente("p1", "33 Export 65cl", 4, 700, 475),
                    LigneVente("p10", "Poisson Braisé + Miondo", 1, 4000, 2200)
                ),
                totalVente = 6800,
                coutTotalAchat = 4100,
                modePaiement = ModePaiement.MTN_MOMO.key,
                serveurNom = "Jean (Gérant)"
            ),
            "v_demo_2" to Vente(
                numeroTicket = "T-002",
                tableNom = "Table VIP",
                statut = StatutVente.OUVERTE.key,
                lignes = listOf(
                    LigneVente("p3", "Guinness Smooth 33cl", 3, 900, 550),
                    LigneVente("p12", "Poulet Braisé Entier", 1, 5500, 3200)
                ),
                totalVente = 8200,
                coutTotalAchat = 4850,
                serveurNom = "Jean (Gérant)"
            )
        )
    }
}
