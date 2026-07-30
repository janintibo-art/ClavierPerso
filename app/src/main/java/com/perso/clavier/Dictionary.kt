package com.perso.clavier

import android.content.Context

object Dictionary {

    private val cache = HashMap<Int, List<String>>()
    private val files = listOf("dict_fr.txt", "dict_en.txt", "dict_es.txt")

    private fun words(context: Context, lang: Int): List<String> =
        cache.getOrPut(lang) {
            try {
                context.assets.open(files[lang.coerceIn(0, 2)])
                    .bufferedReader()
                    .readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    fun suggest(context: Context, lang: Int, prefix: String, max: Int = 3): List<String> {
        if (prefix.length < 2) return emptyList()
        val p = prefix.lowercase()
        val out = LinkedHashSet<String>()

        // Mots appris de l'utilisateur en premier
        Prefs(context).learnedWords
            .filter { it.startsWith(p) && it != p }
            .sortedBy { it.length }
            .take(max)
            .forEach { out.add(it) }

        for (w in words(context, lang)) {
            if (out.size >= max) break
            if (w.startsWith(p) && w != p) out.add(w)
        }
        return out.toList()
    }
}
