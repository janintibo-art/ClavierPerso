package com.perso.clavier

import android.content.Context
import android.net.Uri

/** Detecte un code de verification recu par SMS dans les dernieres minutes. */
object SmsCode {

    private var lastFound: String? = null
    private var lastCheck = 0L
    private var dismissed: String? = null

    private val codeRegex = Regex("\\b(\\d{4,8})\\b")
    private val keywords = listOf(
        "code", "vérification", "verification", "confirmation", "otp",
        "sécurité", "securite", "identifiant", "valider", "authentification"
    )

    fun dismiss(code: String) {
        dismissed = code
    }

    /** Renvoie le code recu il y a moins de 5 minutes, ou null. */
    fun latest(context: Context): String? {
        val now = System.currentTimeMillis()
        // On ne relit pas la base a chaque frappe
        if (now - lastCheck < 15000) return lastFound
        lastCheck = now
        lastFound = null
        try {
            context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("body", "date"),
                null, null, "date DESC LIMIT 5"
            )?.use { c ->
                val bi = c.getColumnIndex("body")
                val di = c.getColumnIndex("date")
                while (c.moveToNext()) {
                    val date = if (di >= 0) c.getLong(di) else 0L
                    if (now - date > 5 * 60 * 1000L) break
                    val body = c.getString(bi) ?: continue
                    val low = body.lowercase()
                    if (keywords.none { low.contains(it) }) continue
                    val m = codeRegex.find(body) ?: continue
                    val code = m.groupValues[1]
                    if (code == dismissed) continue
                    lastFound = code
                    break
                }
            }
        } catch (_: Exception) {
        }
        return lastFound
    }
}
