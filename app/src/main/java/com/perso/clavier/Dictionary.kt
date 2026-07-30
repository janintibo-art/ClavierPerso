package com.perso.clavier

import android.content.Context
import kotlin.math.abs
import kotlin.math.min

object Dictionary {

    private val cache = HashMap<Int, List<Entry>>()
    private val known = HashMap<Int, HashSet<String>>()
    private val files = listOf("dict_fr.txt", "dict_en.txt", "dict_es.txt")

    /** rank = position dans le fichier : plus c'est petit, plus le mot est frequent. */
    class Entry(val norm: String, val word: String, val rank: Int)

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s.lowercase()) {
            sb.append(
                when (ch) {
                    'à', 'â', 'ä', 'á', 'ã' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'î', 'ï', 'í' -> 'i'
                    'ô', 'ö', 'ó', 'õ' -> 'o'
                    'ù', 'û', 'ü', 'ú' -> 'u'
                    'ç' -> 'c'
                    'ñ' -> 'n'
                    '\'', '-' -> ' '
                    else -> ch
                }
            )
        }
        return sb.toString().replace(" ", "")
    }

    private fun entries(context: Context, lang: Int): List<Entry> {
        val l = lang.coerceIn(0, 2)
        return cache.getOrPut(l) {
            try {
                val list = ArrayList<Entry>()
                val set = HashSet<String>()
                context.assets.open(files[l]).bufferedReader().forEachLine { raw ->
                    val w = raw.trim()
                    if (w.isNotEmpty() && w.length > 1) {
                        val n = normalize(w)
                        if (set.add(n)) list.add(Entry(n, w, list.size))
                    }
                }
                known[l] = set
                list
            } catch (e: Exception) {
                known[l] = HashSet()
                emptyList()
            }
        }
    }

    /** Distance d'edition avec sortie anticipee. */
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

    /**
     * Renvoie jusqu'a [max] suggestions.
     * - Si le mot tape est deja correct : on propose surtout des completions.
     * - Sinon : les corrections passent en premier.
     */
    fun suggest(context: Context, lang: Int, prefix: String, max: Int = 3): List<String> {
        if (prefix.length < 2) return emptyList()
        val l = lang.coerceIn(0, 2)
        val p = normalize(prefix.take(24))
        if (p.isEmpty()) return emptyList()

        val all = entries(context, l)
        val isKnown = known[l]?.contains(p) == true
        val learned = Prefs(context).learnedWords
        val exact = all.firstOrNull { it.norm == p }

        // score : plus petit = meilleur
        val scored = HashMap<String, Int>()

        fun offer(word: String, score: Int) {
            if (word.equals(prefix, ignoreCase = true)) return
            val old = scored[word]
            if (old == null || score < old) scored[word] = score
        }

        // 0. Accent oublie : proposer la forme correcte en priorite absolue
        if (exact != null && !exact.word.equals(prefix, ignoreCase = true)) {
            offer(exact.word, 0)
        }

        // 1. Completions (le mot commence par ce qui est tape)
        for (e in all) {
            if (e.norm.length > p.length && e.norm.startsWith(p)) {
                val bonus = if (isKnown) 60 else 120
                offer(e.word, bonus + (e.norm.length - p.length) * 4 + e.rank / 40)
            }
        }
        for (w in learned) {
            val n = normalize(w)
            if (n.length > p.length && n.startsWith(p)) offer(w, 100)
        }

        // 2. Corrections (mots proches)
        val maxDist = when {
            p.length <= 3 -> 1
            p.length <= 7 -> 2
            else -> 3
        }
        for (e in all) {
            if (abs(e.norm.length - p.length) > maxDist) continue
            val d = editDistance(p, e.norm, maxDist)
            if (d in 1..maxDist) {
                val base = if (isKnown) 600 else 0
                offer(e.word, base + d * 160 + e.rank / 40)
            }
        }

        // 3. Faute en debut de mot plus long (ex : "corection" -> "corrections")
        if (scored.size < max) {
            for (e in all) {
                if (e.norm.length <= p.length) continue
                if (editDistance(p, e.norm.substring(0, p.length), 1) <= 1) {
                    offer(e.word, 900 + e.rank / 40)
                }
            }
        }

        return scored.entries
            .sortedBy { it.value }
            .take(max)
            .map { it.key }
    }
}
