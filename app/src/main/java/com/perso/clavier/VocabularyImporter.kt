package com.perso.clavier

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.UserDictionary

/**
 * Amorce le vocabulaire personnel du clavier a partir de ce que l'utilisateur
 * a deja ecrit, au lieu d'attendre des semaines d'apprentissage.
 * Toutes les donnees restent sur le telephone.
 */
object VocabularyImporter {

    class Report(val words: Int, val pairs: Int, val source: String) {
        override fun toString(): String =
            "$words mots et $pairs enchaînements appris depuis $source"
    }

    /** Decoupe un texte en mots (apostrophes conservees : aujourd'hui, j'ai). */
    private fun tokenize(text: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (ch in text) {
            if (ch.isLetter() || (ch == '\'' && sb.isNotEmpty())) {
                sb.append(ch)
            } else {
                if (sb.length >= 2) out.add(sb.toString().lowercase().trim('\''))
                sb.setLength(0)
                // La ponctuation forte coupe la chaine des enchainements
                if (ch in ".!?\n") out.add("\u0000")
            }
        }
        if (sb.length >= 2) out.add(sb.toString().lowercase().trim('\''))
        return out
    }

    /**
     * Analyse un texte et enregistre mots + enchainements.
     * [weight] permet de compter davantage les sources tres personnelles.
     */
    fun learnFromText(context: Context, text: String, weight: Int = 1, source: String): Report {
        val tokens = tokenize(text)
        val counts = HashMap<String, Int>()
        val bigrams = HashMap<String, HashMap<String, Int>>()
        var previous: String? = null

        for (t in tokens) {
            if (t == "\u0000") {
                previous = null
                continue
            }
            if (t.length < 2 || t.length > 24) {
                previous = null
                continue
            }
            counts[t] = (counts[t] ?: 0) + weight
            val p = previous
            if (p != null) {
                val m = bigrams.getOrPut(p) { HashMap() }
                m[t] = (m[t] ?: 0) + weight
            }
            previous = t
        }
        // On ignore les mots vus une seule fois dans un petit texte : trop de fautes de frappe
        if (tokens.size > 400) {
            counts.entries.removeAll { it.value <= 1 }
        }
        Prefs(context).learnBulk(counts, bigrams)
        return Report(counts.size, bigrams.values.sumOf { it.size }, source)
    }

    /** Dictionnaire personnel Android (mots ajoutes via « ajouter au dictionnaire »). */
    fun importUserDictionary(context: Context): Report {
        val counts = HashMap<String, Int>()
        try {
            context.contentResolver.query(
                UserDictionary.Words.CONTENT_URI,
                arrayOf(UserDictionary.Words.WORD, UserDictionary.Words.FREQUENCY),
                null, null, null
            )?.use { c ->
                val wi = c.getColumnIndex(UserDictionary.Words.WORD)
                val fi = c.getColumnIndex(UserDictionary.Words.FREQUENCY)
                while (c.moveToNext()) {
                    val w = c.getString(wi)?.trim()?.lowercase() ?: continue
                    if (w.length < 2 || w.length > 24) continue
                    val f = if (fi >= 0) c.getInt(fi).coerceIn(1, 255) else 100
                    counts[w] = 5 + f / 25
                }
            }
        } catch (_: Exception) {
        }
        if (counts.isNotEmpty()) Prefs(context).learnBulk(counts, emptyMap())
        return Report(counts.size, 0, "le dictionnaire personnel Android")
    }

    /** Noms et prenoms des contacts : tres utile pour ecrire des messages. */
    fun importContacts(context: Context): Report {
        val counts = HashMap<String, Int>()
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                val i = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val name = c.getString(i) ?: continue
                    for (part in name.split(" ", "-", "'")) {
                        val w = part.trim()
                        if (w.length in 2..24 && w.all { it.isLetter() }) {
                            // Les prenoms gardent leur majuscule
                            counts[w] = (counts[w] ?: 0) + 6
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        if (counts.isNotEmpty()) Prefs(context).learnBulk(counts, emptyMap())
        return Report(counts.size, 0, "tes contacts")
    }

    /**
     * Messages ENVOYES : c'est la meilleure source, ce sont tes propres mots,
     * ta facon d'ecrire et tes tournures de phrases.
     */
    fun importSentMessages(context: Context, limit: Int = 3000): Report {
        val sb = StringBuilder()
        var n = 0
        try {
            context.contentResolver.query(
                Uri.parse("content://sms/sent"),
                arrayOf("body"),
                null, null, "date DESC"
            )?.use { c ->
                val i = c.getColumnIndex("body")
                while (c.moveToNext() && n < limit) {
                    val body = c.getString(i) ?: continue
                    sb.append(body).append("\n")
                    n++
                }
            }
        } catch (_: Exception) {
        }
        if (sb.isEmpty()) return Report(0, 0, "tes messages envoyés")
        // Poids 2 : ce sont vraiment les mots de l'utilisateur
        return learnFromText(context, sb.toString(), 2, "$n messages envoyés")
    }
}
