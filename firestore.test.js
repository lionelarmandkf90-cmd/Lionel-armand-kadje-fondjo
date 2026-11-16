const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = () => new Date(Date.now() - 2000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write produits", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("produits").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("produits").doc("p1").set({
      nom: "33 Export",
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's produits or ventes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("produits").doc("p1").set({
      nom: "33 Export 65cl",
      categorie: "bieres",
      unitesParCasier: 12,
      prixAchatUnitaire: 475,
      prixVenteUnitaire: 700,
      stockActuelUnites: 42,
      seuilAlerteUnites: 12,
      estConsigne: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("produits").doc("p1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("produits").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("depenses").doc("d1").set({
      libelle: "Glace",
      categorie: "glace",
      montant: 1500,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Owner can create and update a produit, but Shadow Update with ghost field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("produits").doc("p1");

  await assertSucceeds(
    prodRef.set({
      nom: "33 Export 65cl",
      categorie: "bieres",
      unitesParCasier: 12,
      prixAchatUnitaire: 475,
      prixVenteUnitaire: 700,
      stockActuelUnites: 42,
      seuilAlerteUnites: 12,
      estConsigne: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    prodRef.update({
      stockActuelUnites: 39,
      updatedAt: pastDate(),
    })
  );

  await assertFails(
    prodRef.update({
      stockActuelUnites: 38,
      ghostAdmin: true,
      updatedAt: pastDate(),
    })
  );
});

test("Vente lifecycle: ouverte -> payee succeeds, then further update on payee fails (terminal state lock)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const venteRef = aliceDb.collection("users").doc(ALICE_UID).collection("ventes").doc("v1");

  await assertSucceeds(
    venteRef.set({
      numeroTicket: "T-001",
      tableNom: "Table 2",
      statut: "ouverte",
      lignes: [{ produitId: "p1", nom: "33 Export 65cl", quantite: 3, prixVente: 700, prixAchat: 475 }],
      totalVente: 2100,
      coutTotalAchat: 1425,
      serveurNom: "Jean",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    venteRef.update({
      statut: "payee",
      modePaiement: "mtn_momo",
      closedAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Terminal state lock: cannot modify a paid ticket
  await assertFails(
    venteRef.update({
      totalVente: 0,
      updatedAt: pastDate(),
    })
  );

  // Cannot delete a sale
  await assertFails(venteRef.delete());
});

test("Vente credit (ardoise): requires clientNom, can transition credit -> payee", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badCreditRef = aliceDb.collection("users").doc(ALICE_UID).collection("ventes").doc("v_bad");

  // Missing clientNom must fail
  await assertFails(
    badCreditRef.set({
      numeroTicket: "T-002",
      tableNom: "Comptoir",
      statut: "credit",
      lignes: [{ produitId: "p1", nom: "33 Export 65cl", quantite: 2, prixVente: 700, prixAchat: 475 }],
      totalVente: 1400,
      coutTotalAchat: 950,
      serveurNom: "Jean",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const goodCreditRef = aliceDb.collection("users").doc(ALICE_UID).collection("ventes").doc("v_good");
  await assertSucceeds(
    goodCreditRef.set({
      numeroTicket: "T-003",
      tableNom: "Table VIP",
      statut: "credit",
      lignes: [{ produitId: "p1", nom: "33 Export 65cl", quantite: 2, prixVente: 700, prixAchat: 475 }],
      totalVente: 1400,
      coutTotalAchat: 950,
      modePaiement: "credit",
      clientNom: "M. Tchamda",
      clientTel: "677001122",
      serveurNom: "Jean",
      closedAt: pastDate(),
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Paying off the ardoise
  await assertSucceeds(
    goodCreditRef.update({
      statut: "payee",
      modePaiement: "especes",
      closedAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Pertes are append-only (cannot update or delete) and Depenses enforce positive montant", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const perteRef = aliceDb.collection("users").doc(ALICE_UID).collection("pertes").doc("loss1");

  await assertSucceeds(
    perteRef.set({
      produitId: "p1",
      produitNom: "33 Export 65cl",
      type: "casse",
      quantite: 1,
      valeurPerdue: 475,
      motif: "Tombée au bar",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertFails(perteRef.update({ quantite: 0, updatedAt: pastDate() }));
  await assertFails(perteRef.delete());

  const depRef = aliceDb.collection("users").doc(ALICE_UID).collection("depenses").doc("d1");
  await assertFails(
    depRef.set({
      libelle: "Glace",
      categorie: "glace",
      montant: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    depRef.set({
      libelle: "3 sacs de glace",
      categorie: "glace",
      montant: 1500,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});
