package com.perso.clavier

import android.content.Context

/**
 * Modele de langue statistique embarque : frequence des mots et enchainements
 * les plus courants du francais. Il remplace un modele neuronal, pour un poids
 * de quelques kilo-octets et un temps de reponse immediat.
 */
object LanguageModel {

    private class Entry(val freq: Int, val next: List<Pair<String, Int>>)

    private var loaded = false
    private val table = HashMap<String, Entry>(512)
    private var maxFreq = 1

    private fun load(context: Context) {
        if (loaded) return
        loaded = true
        try {
            context.assets.open("lm_fr.txt").bufferedReader().forEachLine { line ->
                val parts = line.split(' ')
                if (parts.size < 2) return@forEachLine
                val word = parts[0]
                val freq = parts[1].toIntOrNull() ?: return@forEachLine
                val next = if (parts.size >= 3) {
                    parts[2].split(',').mapNotNull { pair ->
                        val kv = pair.split(':')
                        if (kv.size == 2) {
                            val n = kv[1].toIntOrNull()
                            if (n != null) kv[0] to n else null
                        } else null
                    }
                } else emptyList()
                table[word] = Entry(freq, next)
                if (freq > maxFreq) maxFreq = freq
            }
        } catch (_: Exception) {
        }
    }

    /** Frequence relative d'un mot : 0 (inconnu) a 100 (tres courant). */
    fun frequency(context: Context, word: String): Int {
        load(context)
        val e = table[word.lowercase()] ?: return 0
        return (e.freq * 100 / maxFreq).coerceIn(1, 100)
    }

    /** Mots qui suivent habituellement [previous], du plus probable au moins probable. */
    fun nextWords(context: Context, previous: String): List<String> {
        load(context)
        val e = table[previous.lowercase()] ?: return emptyList()
        return e.next.map { it.first }
    }

    /** Probabilite relative que [word] suive [previous] : 0 a 100. */
    fun transition(context: Context, previous: String, word: String): Int {
        load(context)
        val e = table[previous.lowercase()] ?: return 0
        val total = e.next.sumOf { it.second }
        if (total == 0) return 0
        val hit = e.next.firstOrNull { it.first.equals(word, true) } ?: return 0
        return (hit.second * 100 / total).coerceIn(1, 100)
    }
}
