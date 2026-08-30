package com.perso.clavier

import android.content.Context
import kotlin.math.abs
import kotlin.math.min

/**
 * Moteur de suggestions.
 * Optimise pour repondre en quelques millisecondes meme avec un gros dictionnaire :
 * - index par premiere lettre et par longueur (on ne parcourt jamais tout le dictionnaire)
 * - liste triee + recherche dichotomique pour les completions
 * - distance de Damerau-Levenshtein ponderee par la proximite des touches
 */
object Dictionary {

    class Entry(val norm: String, val word: String, val rank: Int)

    private class Index(entries: List<Entry>) {
        /** Triee par forme normalisee : permet la recherche dichotomique des completions. */
        val sorted: List<Entry> = entries.sortedBy { it.norm }
        /** Index par (premiere lettre, longueur) : reduit fortement l'espace de recherche. */
        val byFirstLen: Map<Long, List<Entry>> =
            entries.groupBy { (it.norm[0].code.toLong() shl 8) or it.norm.length.toLong() }
        val norms: HashSet<String> = HashSet(entries.map { it.norm })
        val byNorm: HashMap<String, Entry> = HashMap<String, Entry>().apply {
            entries.forEach { if (!containsKey(it.norm)) put(it.norm, it) }
        }
    }

    private val cache = HashMap<Int, Index>()
    private val files = listOf("dict_fr.txt", "dict_en.txt", "dict_es.txt")

    // ---------- Proximite des touches (les fautes de frappe sont des touches voisines) ----------

    private val azerty = arrayOf("azertyuiop", "qsdfghjklm", "wxcvbn")
    private val qwerty = arrayOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

    private val neighborsCache = HashMap<Int, Map<Char, Set<Char>>>()

    private fun neighbors(lang: Int): Map<Char, Set<Char>> = neighborsCache.getOrPut(lang) {
        val rows = if (lang == 0) azerty else qwerty
        val pos = HashMap<Char, Pair<Int, Int>>()
        rows.forEachIndexed { r, row -> row.forEachIndexed { c, ch -> pos[ch] = r to c } }
        val map = HashMap<Char, Set<Char>>()
        for ((ch, p) in pos) {
            val set = HashSet<Char>()
            for ((ch2, p2) in pos) {
                if (ch == ch2) continue
                if (abs(p.first - p2.first) <= 1 && abs(p.second - p2.second) <= 1) set.add(ch2)
            }
            map[ch] = set
        }
        map
    }

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s.lowercase()) {
            when (ch) {
                'à', 'â', 'ä', 'á', 'ã' -> sb.append('a')
                'é', 'è', 'ê', 'ë' -> sb.append('e')
                'î', 'ï', 'í' -> sb.append('i')
                'ô', 'ö', 'ó', 'õ' -> sb.append('o')
                'ù', 'û', 'ü', 'ú' -> sb.append('u')
                'ç' -> sb.append('c')
                'ñ' -> sb.append('n')
                '\'', '-', ' ' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun index(context: Context, lang: Int): Index {
        val l = lang.coerceIn(0, 2)
        return cache.getOrPut(l) {
            val list = ArrayList<Entry>(12000)
            val seen = HashSet<String>(12000)
            try {
                context.assets.open(files[l]).bufferedReader().forEachLine { raw ->
                    val w = raw.trim()
                    if (w.length > 1) {
                        val n = normalize(w)
                        if (n.length > 1 && seen.add(n)) list.add(Entry(n, w, list.size))
                    }
                }
            } catch (_: Exception) {
            }
            Index(list)
        }
    }

    /** Entrees du dictionnaire, pour la reconnaissance du glissement. */
    fun entriesFor(context: Context, lang: Int): List<Entry> = index(context, lang).sorted

    fun contains(context: Context, lang: Int, word: String): Boolean =
        index(context, lang).norms.contains(normalize(word))

    // ---------- Distance ponderee ----------

    /**
     * Damerau-Levenshtein en unites de 10.
     * Une substitution par une touche voisine ne coute que 6 : c'est la faute la plus frequente.
     */
    private fun distance(a: String, b: String, maxCost: Int, near: Map<Char, Set<Char>>): Int {
        val n = a.length
        val m = b.length
        if (abs(n - m) * 10 > maxCost) return maxCost + 1
        val prev2 = IntArray(m + 1)
        var prev = IntArray(m + 1) { it * 10 }
        var cur = IntArray(m + 1)
        var beforePrev = prev2
        for (i in 1..n) {
            cur[0] = i * 10
            var rowMin = cur[0]
            val ca = a[i - 1]
            for (j in 1..m) {
                val cb = b[j - 1]
                val sub = when {
                    ca == cb -> 0
                    near[ca]?.contains(cb) == true -> 6
                    else -> 10
                }
                var v = min(min(cur[j - 1] + 10, prev[j] + 10), prev[j - 1] + sub)
                // Transposition : "bnojour" -> "bonjour"
                if (i > 1 && j > 1 && ca == b[j - 2] && a[i - 2] == cb) {
                    v = min(v, beforePrev[j - 2] + 7)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > maxCost) return maxCost + 1
            val tmp = beforePrev
            beforePrev = prev
            prev = cur
            cur = tmp
        }
        return prev[m]
    }

    // ---------- Suggestions ----------

    class Result(val words: List<String>, val correction: String?, val typedIsKnown: Boolean)

    fun suggest(
        context: Context,
        lang: Int,
        prefix: String,
        max: Int = 3,
        previousWord: String = ""
    ): Result {
        val l = lang.coerceIn(0, 2)
        val prefs = Prefs(context)
        val p = normalize(prefix.take(24))

        // Rien de tape : proposer la suite habituelle du mot precedent
        if (p.isEmpty()) {
            if (previousWord.isBlank()) return Result(emptyList(), null, true)
            val personal = prefs.nextWords(previousWord)
            val model = if (l == 0) LanguageModel.nextWords(context, previousWord) else emptyList()
            // Les habitudes personnelles d'abord, completees par le modele
            val merged = LinkedHashSet<String>()
            personal.forEach { merged.add(it) }
            model.forEach { merged.add(it) }
            return Result(merged.take(max).toList(), null, true)
        }

        val idx = index(context, l)
        val near = neighbors(l)
        val counts = prefs.wordCounts()
        val nextAfterPrev = if (previousWord.isNotBlank()) prefs.nextWords(previousWord) else emptyList()
        // Modele de langue embarque (francais uniquement)
        val useLm = l == 0
        val lmNext = if (useLm && previousWord.isNotBlank())
            LanguageModel.nextWords(context, previousWord) else emptyList()
        val typedIsKnown = idx.norms.contains(p) || counts.containsKey(prefix.lowercase())

        // score : plus petit = meilleur
        val scored = HashMap<String, Int>(64)
        val distances = HashMap<String, Int>(32)
        fun offer(word: String, score: Int, dist: Int = -1) {
            if (word.length < 2) return
            if (word.equals(prefix, ignoreCase = true)) return
            val old = scored[word]
            if (old == null || score < old) scored[word] = score
            if (dist >= 0) {
                val od = distances[word]
                if (od == null || dist < od) distances[word] = dist
            }
        }

        // --- 0. Accent oublie ("ecole" -> "école") ---
        idx.byNorm[p]?.let { e ->
            if (!e.word.equals(prefix, ignoreCase = true)) offer(e.word, -50, 0)
        }

        // --- 1. Mots personnels : plus tu les ecris, plus ils remontent, des la 1re lettre ---
        for ((w, count) in counts) {
            val n = normalize(w)
            if (n.length >= p.length && n.startsWith(p)) {
                // Un mot personnel bat toujours le dictionnaire general
                var score = 40 - (count * 22).coerceAtMost(320)
                val bi = nextAfterPrev.indexOf(w)
                if (bi >= 0) score -= (160 - bi * 25)
                if (n == p) score += 45
                offer(w, score)
            }
        }
        // Mots personnels proches (faute de frappe sur un mot appris)
        if (p.length >= 4) {
            for ((w, count) in counts) {
                val n = normalize(w)
                if (n == p || n.startsWith(p)) continue
                if (abs(n.length - p.length) > 2) continue
                val d = distance(p, n, 20, near)
                if (d in 1..20) offer(w, 60 + d * 10 - (count * 8).coerceAtMost(120))
            }
        }

        // --- 1 ter. Passe prioritaire : mots attendus apres le mot precedent ---
        // Sans cela ils se perdent dans la masse des completions possibles.
        if (useLm) {
            for ((rank2, w) in lmNext.withIndex()) {
                val n = normalize(w)
                if (n.length >= p.length && n.startsWith(p)) {
                    val real = idx.byNorm[n]?.word ?: w
                    offer(real, -120 + rank2 * 10)
                }
            }
        }

        // --- 2. Completions par recherche dichotomique ---
        var lo = 0
        var hi = idx.sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (idx.sorted[mid].norm < p) lo = mid + 1 else hi = mid
        }
        var examined = 0
        var i = lo
        while (i < idx.sorted.size && examined < 3000) {
            val e = idx.sorted[i]
            if (!e.norm.startsWith(p)) break
            examined++
            i++
            if (e.norm.length == p.length) continue
            val base = if (typedIsKnown) 60 else 110
            var sc = base + (e.norm.length - p.length) * 4 + e.rank / 45
            if (useLm) {
                // Un mot courant du francais remonte
                sc -= LanguageModel.frequency(context, e.word) / 2
                // Un mot qui suit habituellement le precedent remonte beaucoup
                val bi = lmNext.indexOf(e.word.lowercase())
                if (bi >= 0) sc -= (90 - bi * 12)
            }
            offer(e.word, sc)
        }

        // --- 3. Corrections : uniquement les mots de longueur proche (index par longueur) ---
        val maxCost = when {
            p.length <= 3 -> 12
            p.length <= 6 -> 22
            else -> 30
        }
        val span = if (p.length <= 4) 1 else 2
        val firstOk = HashSet<Char>().apply {
            add(p[0])
            near[p[0]]?.let { addAll(it) }
        }
        for (len in (p.length - span)..(p.length + span)) {
            if (len < 2) continue
            for (fc in firstOk) {
                val key = (fc.code.toLong() shl 8) or len.toLong()
                val bucket = idx.byFirstLen[key] ?: continue
                for (e in bucket) {
                    val d = distance(p, e.norm, maxCost, near)
                    if (d in 1..maxCost) {
                        val base = if (typedIsKnown) 520 else 0
                        offer(e.word, base + d * 14 + e.rank / 45, d)
                    }
                }
            }
        }

        // --- 4. Faute au milieu d'un mot plus long ("anniversair" -> "anniversaire") ---
        if (scored.size < max) {
            var j = 0
            var k = lo
            while (k < idx.sorted.size && j < 40) {
                val e = idx.sorted[k]
                k++
                if (e.norm.isEmpty() || e.norm[0] != p[0]) {
                    if (e.norm.isNotEmpty() && e.norm[0] > p[0]) break else continue
                }
                if (e.norm.length <= p.length) continue
                if (distance(p, e.norm.substring(0, p.length), 10, near) <= 10) {
                    offer(e.word, 800 + e.rank / 45)
                    j++
                }
            }
        }

        val ordered = scored.entries.sortedBy { it.value }
        val best = ordered.firstOrNull()

        // Correction automatique : uniquement pour une faute simple et evidente
        // (une lettre en trop/en moins, une lettre voisine, ou deux lettres inversees)
        val correction = if (!typedIsKnown && best != null && p.length >= 4 &&
            (distances[best.key] ?: 99) <= 10
        ) best.key else null

        return Result(ordered.take(max).map { it.key }, correction, typedIsKnown)
    }
}
