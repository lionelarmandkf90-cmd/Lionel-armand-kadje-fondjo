package com.example.util

import com.example.data.model.LigneVente
import com.example.data.model.ModePaiement
import java.text.NumberFormat
import java.util.Locale

private val frenchFormatter: NumberFormat = NumberFormat.getIntegerInstance(Locale.FRANCE)

fun formatFCFA(montant: Long): String {
    return "${frenchFormatter.format(montant)} FCFA"
}

fun formatNumberFR(value: Long): String {
    return frenchFormatter.format(value)
}

fun formatStockCasierUnite(totalUnites: Long, unitesParCasier: Long): String {
    if (unitesParCasier <= 1L) return "$totalUnites unités"
    val casiers = totalUnites / unitesParCasier
    val reste = totalUnites % unitesParCasier
    return when {
        casiers == 0L -> "$reste btl"
        reste == 0L -> "$casiers casier${if (casiers > 1L) "s" else ""}"
        else -> "$casiers cs + $reste btl"
    }
}

fun buildWhatsAppReceiptText(
    maquisName: String,
    tableNom: String,
    ticketNum: String,
    lignes: List<LigneVente>,
    total: Long,
    mode: ModePaiement
): String {
    val lignesText = lignes.joinToString("\n") { l ->
        "- ${l.quantite}x ${l.nom} : ${formatFCFA(l.quantite * l.prixVente)}"
    }
    return buildString {
        append("*REÇU — $maquisName*\n")
        append("Ticket : $ticketNum • $tableNom\n")
        append("------------------\n")
        append(lignesText)
        append("\n------------------\n")
        append("*TOTAL PAYÉ : ${formatFCFA(total)}*\n")
        append("Mode : ${mode.label}\n")
        append("Merci de votre visite !")
    }
}
