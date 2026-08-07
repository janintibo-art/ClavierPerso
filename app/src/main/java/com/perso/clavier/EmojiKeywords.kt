package com.perso.clavier

/** Recherche d'emoji par mot-cle (francais et anglais). */
object EmojiKeywords {

    private val map: List<Pair<String, String>> = listOf(
        "😀" to "sourire content heureux smile", "😂" to "rire mdr lol drole larmes",
        "🤣" to "ptdr mort de rire", "😊" to "sourire timide content",
        "😍" to "amour yeux coeur adore", "🥰" to "amour tendresse calin",
        "😘" to "bisou bise embrasse kiss", "😉" to "clin oeil complice",
        "😎" to "cool lunettes soleil classe", "🤔" to "reflechir pense doute question",
        "😢" to "triste pleure larme", "😭" to "pleure sanglot triste",
        "😡" to "colere enerve rage furieux", "😱" to "peur choc surprise cri",
        "😴" to "dormir fatigue sommeil", "🥱" to "baille fatigue ennui",
        "🤒" to "malade fievre", "🤮" to "vomi degout", "🥳" to "fete anniversaire joie",
        "😅" to "gene rire sueur", "🙃" to "ironie envers", "😏" to "malice sourire narquois",
        "🙄" to "yeux ciel agace", "😬" to "gene grimace", "🤗" to "calin embrasse",
        "🤫" to "chut silence secret", "🤯" to "explose tete choc",
        "❤️" to "amour coeur rouge", "💔" to "coeur brise chagrin", "💕" to "coeurs amour",
        "👍" to "pouce bien ok super daccord", "👎" to "pouce bas non mauvais",
        "👏" to "applaudir bravo felicitations", "🙏" to "merci priere stp svp",
        "💪" to "muscle force courage", "🤝" to "accord main serrer deal",
        "👋" to "salut bonjour coucou au revoir", "✌️" to "paix victoire",
        "🔥" to "feu chaud top genial", "⭐" to "etoile favori", "✨" to "brillant magie",
        "💯" to "cent parfait total", "✅" to "oui valide fait coche", "❌" to "non erreur faux",
        "⚠️" to "attention danger alerte", "❗" to "important exclamation",
        "❓" to "question interrogation", "💡" to "idee ampoule",
        "🎉" to "fete bravo celebration", "🎂" to "anniversaire gateau",
        "🎁" to "cadeau surprise", "🎄" to "noel sapin", "🍀" to "chance trefle",
        "☀️" to "soleil beau temps", "🌙" to "lune nuit", "🌧️" to "pluie",
        "❄️" to "neige froid hiver", "🌈" to "arc en ciel", "⛈️" to "orage",
        "🚗" to "voiture auto", "✈️" to "avion voyage vacances", "🚆" to "train",
        "🚲" to "velo", "🏠" to "maison chez moi", "🏥" to "hopital",
        "🍕" to "pizza", "🍔" to "burger", "🍟" to "frites", "🥖" to "pain baguette",
        "☕" to "cafe", "🍺" to "biere apero", "🍷" to "vin", "🍎" to "pomme fruit",
        "🍫" to "chocolat", "🍦" to "glace", "🍽️" to "manger repas restaurant",
        "🐶" to "chien toutou", "🐱" to "chat", "🐦" to "oiseau", "🐟" to "poisson",
        "🌸" to "fleur printemps", "🌳" to "arbre nature",
        "⚽" to "foot football", "🏀" to "basket", "🎮" to "jeu video console",
        "🎵" to "musique", "🎬" to "film cinema", "📷" to "photo",
        "📱" to "telephone portable", "💻" to "ordinateur", "📧" to "mail email",
        "💬" to "message discussion", "📅" to "calendrier rendez vous date",
        "⏰" to "heure reveil temps", "💰" to "argent money", "🔑" to "cle",
        "👶" to "bebe", "👨‍👩‍👧" to "famille", "💍" to "mariage bague",
        "🚀" to "fusee rapide decollage", "👀" to "yeux regarde", "🧠" to "cerveau reflechir",
        "💤" to "dodo sommeil", "🤖" to "robot ia", "👻" to "fantome halloween",
        "💩" to "caca merde", "🖕" to "doigt insulte", "😈" to "diable malice"
    )

    fun search(query: String): List<String> {
        val q = Dictionary.normalize(query)
        if (q.isEmpty()) return emptyList()
        val exact = ArrayList<String>()
        val partial = ArrayList<String>()
        for ((emoji, words) in map) {
            val list = words.split(" ")
            when {
                list.any { it == q } -> exact.add(emoji)
                list.any { it.startsWith(q) } -> partial.add(emoji)
                q.length >= 3 && words.contains(q) -> partial.add(emoji)
            }
        }
        return (exact + partial).distinct().take(40)
    }
}
