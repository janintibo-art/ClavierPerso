package com.perso.clavier

object Layouts {

    val languages = listOf("Français", "English", "Español")

    fun rows(lang: Int): List<List<String>> = when (lang) {
        1 -> listOf(
            "q w e r t y u i o p",
            "a s d f g h j k l",
            "z x c v b n m"
        )
        2 -> listOf(
            "q w e r t y u i o p",
            "a s d f g h j k l ñ",
            "z x c v b n m"
        )
        else -> listOf(
            "a z e r t y u i o p",
            "q s d f g h j k l m",
            "w x c v b n '"
        )
    }.map { it.split(" ") }

    fun accents(lang: Int): Map<String, String> = when (lang) {
        1 -> mapOf("a" to "á", "e" to "é", "i" to "í", "o" to "ó", "u" to "ú")
        2 -> mapOf("n" to "ñ", "a" to "á", "e" to "é", "i" to "í", "o" to "ó", "u" to "ú")
        else -> mapOf(
            "e" to "é", "a" to "à", "u" to "ù", "i" to "î",
            "o" to "ô", "c" to "ç", "'" to "\""
        )
    }
}
