package com.perso.clavier

import android.content.Context
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Reconnaissance du glissement (« swipe ») : transforme un trace continu
 * sur les touches en mot.
 *
 * Principe : on extrait du trace la suite des lettres survolees, en ne gardant
 * que celles ou le doigt marque un changement de direction ou ralentit
 * (ce sont les lettres reellement visees). On compare ensuite cette suite
 * aux mots du dictionnaire.
 */
object SwipeEngine {

    /** Position du centre de chaque touche, pour mesurer la proximite du trace. */
    class KeyPos(val label: String, val cx: Float, val cy: Float)

    class Candidate(val word: String, val score: Int)

    /**
     * Analyse le trace.
     * [path] : points parcourus par le doigt.
     * [keys] : centres des touches lettres.
     */
    fun recognize(
        context: Context,
        lang: Int,
        path: List<FloatArray>,
        keys: List<KeyPos>,
        keyWidth: Float,
        max: Int = 3
    ): List<String> {
        if (path.size < 4 || keys.isEmpty()) return emptyList()

        val visited = lettersOnPath(path, keys, keyWidth)
        if (visited.isEmpty()) return emptyList()

        val first = visited.first()
        val last = visited.last()
        val pivots = pivotLetters(path, keys, keyWidth)
        val sequence = visited

        val prefs = Prefs(context)
        val counts = prefs.wordCounts()
        val results = ArrayList<Candidate>()

        fun consider(word: String, personalBonus: Int) {
            // Les lettres doublees (mm de « comment ») ne sont parcourues qu'une fois
            // par le doigt : on compare donc sur la forme sans doublons.
            val n = dedup(Dictionary.normalize(word))
            if (n.length < 2) return
            if (n.first() != first || n.last() != last) return
            // Toutes les lettres du mot doivent apparaitre dans l'ordre du trace
            if (!isSubsequence(n, sequence)) return
            var score = 0
            // Les lettres « pivots » (changement de direction) doivent etre dans le mot
            for (p in pivots) if (!n.contains(p)) score += 45
            // Ecart de longueur entre le mot et le trace
            score += abs(n.length - pivots.size.coerceAtLeast(2)) * 6
            score += compactness(n, sequence)
            score -= personalBonus
            results.add(Candidate(word, score))
        }

        // 1. Mots personnels d'abord
        for ((w, c) in counts) consider(w, 60 + (c * 4).coerceAtMost(120))

        // 2. Dictionnaire
        for (e in Dictionary.entriesFor(context, lang)) {
            consider(e.word, -(e.rank / 400))
        }

        return results
            .sortedBy { it.score }
            .distinctBy { Dictionary.normalize(it.word) }
            .take(max)
            .map { it.word }
    }

    /** Suite des lettres survolees par le trace, sans repetition consecutive. */
    private fun lettersOnPath(
        path: List<FloatArray>, keys: List<KeyPos>, keyWidth: Float
    ): String {
        val sb = StringBuilder()
        val radius = keyWidth * 0.62f
        for (pt in path) {
            var best: KeyPos? = null
            var bestD = Float.MAX_VALUE
            for (k in keys) {
                val d = hypot(pt[0] - k.cx, pt[1] - k.cy)
                if (d < bestD) {
                    bestD = d
                    best = k
                }
            }
            if (best != null && bestD <= radius) {
                val c = best.label.firstOrNull() ?: continue
                if (sb.isEmpty() || sb.last() != c) sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Lettres ou le doigt change nettement de direction ou ralentit :
     * ce sont celles que l'utilisateur visait vraiment.
     */
    private fun pivotLetters(
        path: List<FloatArray>, keys: List<KeyPos>, keyWidth: Float
    ): List<Char> {
        val out = ArrayList<Char>()
        if (path.size < 3) return out

        fun letterAt(pt: FloatArray): Char? {
            var best: KeyPos? = null
            var bestD = Float.MAX_VALUE
            for (k in keys) {
                val d = hypot(pt[0] - k.cx, pt[1] - k.cy)
                if (d < bestD) {
                    bestD = d
                    best = k
                }
            }
            return if (best != null && bestD <= keyWidth * 0.62f) best.label.firstOrNull() else null
        }

        letterAt(path.first())?.let { out.add(it) }

        var i = 2
        while (i < path.size - 1) {
            val a = path[i - 2]
            val b = path[i]
            val c = path[i + 1]
            val v1x = b[0] - a[0]
            val v1y = b[1] - a[1]
            val v2x = c[0] - b[0]
            val v2y = c[1] - b[1]
            val n1 = hypot(v1x, v1y)
            val n2 = hypot(v2x, v2y)
            if (n1 > 1f && n2 > 1f) {
                val cos = (v1x * v2x + v1y * v2y) / (n1 * n2)
                // Angle marque (< ~135°) ou net ralentissement
                if (cos < 0.55f || n2 < n1 * 0.35f) {
                    letterAt(b)?.let { ch -> if (out.isEmpty() || out.last() != ch) out.add(ch) }
                }
            }
            i++
        }
        letterAt(path.last())?.let { ch -> if (out.isEmpty() || out.last() != ch) out.add(ch) }
        return out
    }

    /** Supprime les lettres identiques consecutives. */
    private fun dedup(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) if (sb.isEmpty() || sb.last() != c) sb.append(c)
        return sb.toString()
    }

    /** Verifie que [word] est une sous-suite de [seq] (lettres dans l'ordre). */
    private fun isSubsequence(word: String, seq: String): Boolean {
        var i = 0
        for (c in seq) {
            if (i < word.length && word[i] == c) i++
            if (i == word.length) return true
        }
        return i == word.length
    }

    /** Penalise les mots dont les lettres sont tres eparpillees dans le trace. */
    private fun compactness(word: String, seq: String): Int {
        var i = 0
        var gaps = 0
        var lastPos = -1
        for ((pos, c) in seq.withIndex()) {
            if (i < word.length && word[i] == c) {
                if (lastPos >= 0) gaps += (pos - lastPos - 1)
                lastPos = pos
                i++
            }
        }
        return min(gaps * 3, 90)
    }
}
