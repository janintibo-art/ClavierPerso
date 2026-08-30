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

    /**
     * Caractere secondaire obtenu par appui long, comme sur les claviers classiques :
     * la rangee du haut donne les chiffres, les autres des symboles utiles.
     */
    private val secondaryMap: Map<String, String> = mapOf(
        // Rangee du haut : chiffres (AZERTY et QWERTY)
        "a" to "1", "z" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
        "q" to "1", "w" to "2",
        // Deuxieme rangee : symboles courants
        "s" to "@", "d" to "#", "f" to "&", "g" to "*",
        "h" to "-", "j" to "+", "k" to "(", "l" to ")", "m" to "\"",
        // Troisieme rangee
        "x" to "%", "c" to "/", "v" to "=", "b" to "_", "n" to ":",
        "'" to "!", "," to ";", "." to "?"
    )

    fun secondary(label: String): String? = secondaryMap[label.lowercase()]

    fun accents(lang: Int): Map<String, String> = when (lang) {
        1 -> mapOf("a" to "á", "e" to "é", "i" to "í", "o" to "ó", "u" to "ú")
        2 -> mapOf("n" to "ñ", "a" to "á", "e" to "é", "i" to "í", "o" to "ó", "u" to "ú")
        else -> mapOf(
            "e" to "é", "a" to "à", "u" to "ù", "i" to "î",
            "o" to "ô", "c" to "ç", "'" to "\""
        )
    }
}
