package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CategorieDepense
import com.example.data.model.CategorieProduit
import com.example.data.model.LigneVente
import com.example.data.model.ModePaiement
import com.example.data.model.Produit
import com.example.data.model.TypePerte
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MaquisRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProduitAndObserve_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = MaquisRepository(firestore, auth)
        val prodId = "prod_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val produit = Produit(
            id = prodId,
            nom = "33 Export 65cl",
            categorie = CategorieProduit.BIERES.key,
            unitesParCasier = 12,
            prixAchatUnitaire = 475,
            prixVenteUnitaire = 700,
            stockActuelUnites = 24,
            seuilAlerteUnites = 12,
            estConsigne = true
        )

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createProduit(produit) }
        assertEquals(prodId, createdId)

        val collection = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProduits().first { col -> col.items.any { it.id == prodId } }
        }
        assertTrue(collection.items.any { it.id == prodId && it.nom == "33 Export 65cl" })
    }

    @Test
    fun fullPosWorkflow_tableOuverteEncaissementPerteDepense_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = MaquisRepository(firestore, auth)
        val prodId = "prod_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val produit = Produit(
            id = prodId,
            nom = "Castel Beer 65cl",
            categorie = CategorieProduit.BIERES.key,
            unitesParCasier = 12,
            prixAchatUnitaire = 475,
            prixVenteUnitaire = 700,
            stockActuelUnites = 30,
            seuilAlerteUnites = 12,
            estConsigne = true
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.createProduit(produit) }

        // Open a table
        val lignes = listOf(LigneVente(prodId, "Castel Beer 65cl", 3, 700, 475))
        val venteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveOrUpdateTableOuverte(
                tableNom = "Table 2",
                lignes = lignes,
                serveurNom = "Jean",
                existingVente = null,
                produitsSnapshot = listOf(produit),
                ticketNumber = "T-TEST-1"
            )
        }

        val ventesSnap = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeVentes().first { col -> col.items.any { it.id == venteId } }
        }
        val openedVente = ventesSnap.items.first { it.id == venteId }

        // Cash out table
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.encaisserVente(
                tableNom = "Table 2",
                lignes = lignes,
                modePaiement = ModePaiement.MTN_MOMO,
                clientNom = null,
                clientTel = null,
                serveurNom = "Jean",
                existingVente = openedVente,
                produitsSnapshot = listOf(produit.copy(stockActuelUnites = 27)),
                ticketNumber = "T-TEST-1"
            )
        }

        // Declare loss and add expense
        val perteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.declarerPerte(produit.copy(stockActuelUnites = 27), 1, TypePerte.CASSE, "Tombée")
        }
        val depenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ajouterDepense("Glace", CategorieDepense.GLACE, 1500)
        }
        assertTrue(perteId.isNotBlank() && depenseId.isNotBlank())
    }

    @Test
    fun observeProduits_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        auth.signOut()

        // Directly query Alice's path without auth to verify Firestore rules reject unauthenticated reads
        val exception = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document(uid).collection("produits").get().let { task ->
                    com.google.android.gms.tasks.Tasks.await(task)
                }
            }
        }.exceptionOrNull()

        val firestoreEx = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceProduits() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = MaquisRepository(firestore, auth)
        val prodId = "prod_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createProduit(
                Produit(
                    id = prodId,
                    nom = "Bière Privée",
                    categorie = CategorieProduit.BIERES.key,
                    unitesParCasier = 12,
                    prixAchatUnitaire = 500,
                    prixVenteUnitaire = 800,
                    stockActuelUnites = 12,
                    seuilAlerteUnites = 6
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        val exception = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                com.google.android.gms.tasks.Tasks.await(
                    firestore.collection("users").document(aliceUid).collection("produits").get()
                )
            }
        }.exceptionOrNull()

        val firestoreEx = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_maquis@test.com"
        const val BOB_EMAIL = "bob_maquis@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
