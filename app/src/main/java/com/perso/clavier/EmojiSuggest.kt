package com.perso.clavier

/** Propose un emoji quand le mot ecrit correspond a un sens connu. */
object EmojiSuggest {

    private val map: Map<String, String> = mapOf(
        "amour" to "❤️", "coeur" to "❤️", "aime" to "❤️", "love" to "❤️", "bisous" to "😘",
        "bisou" to "😘", "rire" to "😂", "drole" to "😂", "mdr" to "😂", "lol" to "😂",
        "ptdr" to "🤣", "sourire" to "😊", "content" to "😊", "heureux" to "😄", "joie" to "🥳",
        "triste" to "😢", "pleure" to "😭", "peur" to "😱", "colere" to "😡", "enerve" to "😠",
        "fatigue" to "😴", "dormir" to "😴", "sommeil" to "🥱", "malade" to "🤒", "froid" to "🥶",
        "chaud" to "🥵", "soleil" to "☀️", "pluie" to "🌧️", "neige" to "❄️", "orage" to "⛈️",
        "feu" to "🔥", "eau" to "💧", "mer" to "🌊", "plage" to "🏖️", "montagne" to "⛰️",
        "voyage" to "✈️", "avion" to "✈️", "voiture" to "🚗", "train" to "🚆", "velo" to "🚲",
        "maison" to "🏠", "travail" to "💼", "bureau" to "💼", "ecole" to "🏫", "argent" to "💰",
        "cadeau" to "🎁", "anniversaire" to "🎂", "gateau" to "🎂", "fete" to "🎉", "noel" to "🎄",
        "musique" to "🎵", "film" to "🎬", "photo" to "📷", "jeu" to "🎮", "sport" to "⚽",
        "foot" to "⚽", "course" to "🏃", "cafe" to "☕", "the" to "🍵", "biere" to "🍺",
        "vin" to "🍷", "manger" to "🍽️", "pizza" to "🍕", "burger" to "🍔", "pain" to "🥖",
        "fromage" to "🧀", "chocolat" to "🍫", "glace" to "🍦", "fruit" to "🍎", "pomme" to "🍎",
        "chien" to "🐶", "chat" to "🐱", "oiseau" to "🐦", "poisson" to "🐟", "fleur" to "🌸",
        "arbre" to "🌳", "telephone" to "📱", "message" to "💬", "mail" to "📧", "ordinateur" to "💻",
        "merci" to "🙏", "bravo" to "👏", "super" to "👍", "genial" to "🤩", "parfait" to "👌",
        "oui" to "✅", "non" to "❌", "attention" to "⚠️", "important" to "❗", "question" to "❓",
        "idee" to "💡", "temps" to "⏰", "heure" to "⏰", "rendez" to "📅", "cle" to "🔑",
        "bonjour" to "👋", "salut" to "👋", "coucou" to "👋", "bonne" to "😊", "nuit" to "🌙",
        "lune" to "🌙", "etoile" to "⭐", "medecin" to "🩺", "hopital" to "🏥", "police" to "👮",
        "bebe" to "👶", "famille" to "👨‍👩‍👧", "ami" to "🤝", "mariage" to "💍", "boulot" to "💼"
    )

    /** Emoji correspondant au mot, ou null. */
    fun forWord(word: String): String? {
        if (word.length < 3) return null
        return map[Dictionary.normalize(word)]
    }
}
