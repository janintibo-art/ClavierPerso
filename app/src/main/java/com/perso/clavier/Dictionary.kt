package com.perso.clavier

import android.content.Context
import kotlin.math.abs
import kotlin.math.min

object Dictionary {

    private val cache = HashMap<Int, List<Pair<String, String>>>() // normalisé -> mot
    private val files = listOf("dict_fr.txt", "dict_en.txt", "dict_es.txt")

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s.lowercase()) {
            sb.append(
                when (ch) {
                    'à', 'â', 'ä', 'á' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'î', 'ï', 'í' -> 'i'
                    'ô', 'ö', 'ó' -> 'o'
                    'ù', 'û', 'ü', 'ú' -> 'u'
                    'ç' -> 'c'
                    'ñ' -> 'n'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    private fun entries(context: Context, lang: Int): List<Pair<String, String>> =
        cache.getOrPut(lang) {
            try {
                context.assets.open(files[lang.coerceIn(0, 2)])
                    .bufferedReader()
                    .readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .map { normalize(it) to it }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Distance d'édition (Levenshtein) avec sortie anticipée. */
    private fun editDistance(a: String, b: String, maxDist: Int): Int {
        if (abs(a.length - b.length) > maxDist) return maxDist + 1
        if (a == b) return 0
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
                if (cur[j] < rowMin) rowMin = cur[j]
            }
            if (rowMin > maxDist) return maxDist + 1
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length]
    }

    fun suggest(context: Context, lang: Int, prefix: String, max: Int = 3): List<String> {
        if (prefix.length < 2) return emptyList()
        val p = normalize(prefix.take(24))

        val learned = Prefs(context).learnedWords.map { normalize(it) to it }
        val all = learned + entries(context, lang)
        val out = LinkedHashSet<String>()

        // 1. Complétion : le mot commence par ce qu'on a tapé (accents ignorés)
        for ((n, w) in all) {
            if (out.size >= max) break
            if (n.startsWith(p) && w != prefix) out.add(w)
        }

        // 2. Correction d'orthographe : mots proches (1 ou 2 fautes)
        if (out.size < max) {
            val maxDist = if (p.length <= 4) 1 else 2
            val scored = ArrayList<Pair<Int, String>>()
            for ((n, w) in all) {
                if (w in out || w == prefix) continue
                val d = editDistance(p, n, maxDist)
                if (d <= maxDist) scored.add(d to w)
            }
            scored.sortBy { it.first * 100 + it.second.length }
            for ((_, w) in scored) {
                if (out.size >= max) break
                out.add(w)
            }
        }

        // 3. Faute en début de mot : le début du mot ressemble à ce qu'on a tapé
        if (out.size < max) {
            for ((n, w) in all) {
                if (out.size >= max) break
                if (w in out || n.length <= p.length) continue
                if (editDistance(p, n.substring(0, p.length), 1) <= 1) out.add(w)
            }
        }

        return out.toList()
    }
}
