package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class Role {
    PROPRIETAIRE,
    GERANT
}

enum class CategorieProduit(val key: String, val label: String) {
    BIERES("bieres", "Bières"),
    GAZEUSES("gazeuses", "Gazeuses"),
    VINS_SPIRITUEUX("vins_spiritueux", "Vins & Liqueurs"),
    PLATS_GRILLADES("plats_grillades", "Grillades"),
    DIVERS("divers", "Divers");

    companion object {
        fun fromKey(key: String): CategorieProduit =
            entries.firstOrNull { it.key == key } ?: DIVERS
    }
}

enum class ModePaiement(val key: String, val label: String) {
    ESPECES("especes", "Espèces"),
    MTN_MOMO("mtn_momo", "MTN MoMo"),
    ORANGE_MONEY("orange_money", "Orange Money"),
    CREDIT("credit", "Crédit / Ardoise");

    companion object {
        fun fromKey(key: String?): ModePaiement? =
            entries.firstOrNull { it.key == key }
    }
}

enum class StatutVente(val key: String) {
    OUVERTE("ouverte"),
    PAYEE("payee"),
    CREDIT("credit"),
    ANNULEE("annulee");

    companion object {
        fun fromKey(key: String): StatutVente =
            entries.firstOrNull { it.key == key } ?: OUVERTE
    }
}

enum class TypePerte(val key: String, val label: String) {
    CASSE("casse", "Casse"),
    OFFERT("offert", "Offert / VIP"),
    PERIME("perime", "Périmé");

    companion object {
        fun fromKey(key: String): TypePerte =
            entries.firstOrNull { it.key == key } ?: CASSE
    }
}

enum class CategorieDepense(val key: String, val label: String) {
    GLACE("glace", "Glace"),
    CHARBON_GAZ("charbon_gaz", "Charbon & Gaz"),
    TRANSPORT("transport", "Transport"),
    SALAIRE("salaire", "Salaires"),
    FACTURE("facture", "Facture"),
    AUTRE("autre", "Autre");

    companion object {
        fun fromKey(key: String): CategorieDepense =
            entries.firstOrNull { it.key == key } ?: AUTRE
    }
}

@IgnoreExtraProperties
data class MaquisSettings(
    @DocumentId val id: String = "",
    val maquisName: String = "MaquisPro",
    val ownerPin: String = "1234",
    val seededInitialData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "maquisName" to maquisName,
        "ownerPin" to ownerPin,
        "seededInitialData" to seededInitialData,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Produit(
    @DocumentId val id: String = "",
    val nom: String = "",
    val categorie: String = CategorieProduit.BIERES.key,
    val unitesParCasier: Long = 12L,
    val prixAchatUnitaire: Long = 0L,
    val prixVenteUnitaire: Long = 0L,
    val stockActuelUnites: Long = 0L,
    val seuilAlerteUnites: Long = 12L,
    val estConsigne: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "nom" to nom,
        "categorie" to categorie,
        "unitesParCasier" to unitesParCasier,
        "prixAchatUnitaire" to prixAchatUnitaire,
        "prixVenteUnitaire" to prixVenteUnitaire,
        "stockActuelUnites" to stockActuelUnites,
        "seuilAlerteUnites" to seuilAlerteUnites,
        "estConsigne" to estConsigne,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class SuiviEmballage(
    @DocumentId val id: String = "",
    val typeEmballage: String = "",
    val casiersVidesEnCave: Long = 0L,
    val bouteillesVidesVrac: Long = 0L,
    val casiersChezFournisseur: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "typeEmballage" to typeEmballage,
        "casiersVidesEnCave" to casiersVidesEnCave,
        "bouteillesVidesVrac" to bouteillesVidesVrac,
        "casiersChezFournisseur" to casiersChezFournisseur,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class LigneVente(
    val produitId: String = "",
    val nom: String = "",
    val quantite: Long = 0L,
    val prixVente: Long = 0L,
    val prixAchat: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "produitId" to produitId,
        "nom" to nom,
        "quantite" to quantite,
        "prixVente" to prixVente,
        "prixAchat" to prixAchat
    )
}

@IgnoreExtraProperties
data class Vente(
    @DocumentId val id: String = "",
    val numeroTicket: String = "",
    val tableNom: String = "Comptoir",
    val statut: String = StatutVente.OUVERTE.key,
    val lignes: List<LigneVente> = emptyList(),
    val totalVente: Long = 0L,
    val coutTotalAchat: Long = 0L,
    val modePaiement: String? = null,
    val clientNom: String? = null,
    val clientTel: String? = null,
    val serveurNom: String = "Gérant",
    val closedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "numeroTicket" to numeroTicket,
            "tableNom" to tableNom,
            "statut" to statut,
            "lignes" to lignes.map { it.toMap() },
            "totalVente" to totalVente,
            "coutTotalAchat" to coutTotalAchat,
            "modePaiement" to modePaiement,
            "clientNom" to clientNom?.takeIf { it.isNotBlank() },
            "clientTel" to clientTel?.takeIf { it.isNotBlank() },
            "serveurNom" to serveurNom,
            "closedAt" to if (statut == StatutVente.PAYEE.key || statut == StatutVente.CREDIT.key) FieldValue.serverTimestamp() else null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class PerteStock(
    @DocumentId val id: String = "",
    val produitId: String = "",
    val produitNom: String = "",
    val type: String = TypePerte.CASSE.key,
    val quantite: Long = 1L,
    val valeurPerdue: Long = 0L,
    val motif: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "produitId" to produitId,
        "produitNom" to produitNom,
        "type" to type,
        "quantite" to quantite,
        "valeurPerdue" to valeurPerdue,
        "motif" to motif,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Depense(
    @DocumentId val id: String = "",
    val libelle: String = "",
    val categorie: String = CategorieDepense.AUTRE.key,
    val montant: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "libelle" to libelle,
        "categorie" to categorie,
        "montant" to montant,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class SyncedCollection<T>(
    val items: List<T> = emptyList(),
    val pendingIds: Set<String> = emptySet()
) {
    val pendingCount: Int get() = pendingIds.size
}
