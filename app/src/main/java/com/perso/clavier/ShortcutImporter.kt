package com.perso.clavier

import android.content.Context
import java.io.File

/**
 * Import de raccourcis texte depuis d'autres claviers (Samsung, Gboard, SwiftKey...)
 * ou depuis un fichier de sauvegarde.
 */
object ShortcutImporter {

    /** Emplacements connus des raccourcis Samsung (lisibles seulement si le telephone est root). */
    private val samsungPaths = listOf(
        "/data/data/com.samsung.android.honeyboard/databases/TextShortcut.db",
        "/data/data/com.samsung.android.honeyboard/databases/HoneyBoardTextShortcut.db",
        "/data/data/com.sec.android.inputmethod/databases/TextShortcut.db",
        "/data/user/0/com.samsung.android.honeyboard/databases/TextShortcut.db"
    )

    class Pair2(val key: String, val value: String)

    /**
     * Analyse un texte colle et en extrait les paires raccourci / texte.
     * Formats reconnus :
     *   slt = Salut ca va ?
     *   slt : Salut ca va ?
     *   slt -> Salut ca va ?
     *   slt , Salut ca va ?      (CSV)
     *   slt <tabulation> Salut ca va ?
     *   slt
     *   Salut ca va ?            (deux lignes alternees)
     */
    fun parse(raw: String): List<Pair2> {
        val lines = raw.split("\n")
            .map { it.trim().trim('"') }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val separators = listOf("\t", "=>", "->", "\u2192", "=", ":", ";", "|", ",")

        // 1) Choisir le separateur qui structure la majorite des lignes.
        //    Sans ce test global, une virgule dans le texte ("Salut, ca va ?")
        //    serait prise pour un separateur.
        var best: String? = null
        var bestCount = 0
        for (sep in separators) {
            var n = 0
            for (line in lines) {
                val at = line.indexOf(sep)
                if (at in 1..24) {
                    val k = line.substring(0, at).trim().trim('"', '\'')
                    val v = line.substring(at + sep.length).trim()
                    if (isValidKey(k) && v.isNotEmpty()) n++
                }
            }
            if (n > bestCount) {
                best = sep
                bestCount = n
            }
        }

        val out = ArrayList<Pair2>()
        val threshold = maxOf(2, (lines.size * 0.6).toInt())

        if (best != null && bestCount >= threshold) {
            for (line in lines) {
                val at = line.indexOf(best)
                if (at < 1 || at > 24) continue
                val k = line.substring(0, at).trim().trim('"', '\'')
                val v = line.substring(at + best.length).trim().trim('"', '\'')
                if (isValidKey(k) && v.isNotEmpty()) out.add(Pair2(k, v))
            }
        } else {
            // 2) Format « une ligne sur deux » : c'est ce qu'on obtient
            //    en copiant la liste des raccourcis Samsung.
            var j = 0
            while (j + 1 < lines.size) {
                val k = lines[j]
                val v = lines[j + 1]
                if (isValidKey(k) && v.isNotEmpty()) out.add(Pair2(k, v))
                j += 2
            }
        }
        return dedupe(out)
    }

    private fun isValidKey(k: String): Boolean =
        k.isNotEmpty() && k.length <= 24 && !k.contains(" ") && !k.contains("\t")

    private fun dedupe(list: List<Pair2>): List<Pair2> {
        val seen = HashSet<String>()
        return list.filter { seen.add(it.key.lowercase()) }
    }

    /** Enregistre les paires et renvoie le nombre reellement ajoute. */
    fun save(context: Context, pairs: List<Pair2>): Int {
        val prefs = Prefs(context)
        var n = 0
        for (p in pairs) {
            prefs.putShortcut(p.key, p.value)
            n++
        }
        return n
    }

    /**
     * Tente de lire directement la base Samsung.
     * Ne fonctionne que si le fichier est accessible (telephone root) : Android isole
     * normalement les donnees de chaque application.
     */
    fun tryReadSamsung(): List<Pair2> {
        for (path in samsungPaths) {
            try {
                val f = File(path)
                if (!f.exists() || !f.canRead()) continue
                val bytes = f.readBytes()
                val text = String(bytes, Charsets.UTF_8)
                val out = ArrayList<Pair2>()
                // Les chaines lisibles d'une base SQLite : on recupere les paires plausibles
                val tokens = Regex("[\\p{L}\\p{N}\\p{P} ]{2,120}").findAll(text)
                    .map { it.value.trim() }
                    .filter { it.length in 2..120 }
                    .toList()
                var i = 0
                while (i + 1 < tokens.size) {
                    val k = tokens[i]
                    val v = tokens[i + 1]
                    if (isValidKey(k) && v.length > k.length) out.add(Pair2(k, v))
                    i += 2
                }
                if (out.isNotEmpty()) return dedupe(out)
            } catch (_: Exception) {
            }
        }
        return emptyList()
    }

    /** Exporte les raccourcis au format « raccourci = texte ». */
    fun export(context: Context): String =
        Prefs(context).shortcuts().entries.joinToString("\n") { "${it.key} = ${it.value}" }
}
