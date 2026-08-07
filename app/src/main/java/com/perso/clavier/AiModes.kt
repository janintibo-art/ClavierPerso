package com.perso.clavier

/** Modes de l'assistant IA du clavier. */
object AiModes {

    class Mode(
        val label: String,
        val short: String,
        val hint: String,
        val system: String
    )

    private const val RULE =
        "Réponds UNIQUEMENT avec le texte final, prêt à être envoyé tel quel. " +
                "Pas de préambule, pas de guillemets, pas d'explication, pas de titre, " +
                "pas de formule du type « voici ». Écris en français sauf demande contraire."

    val list = listOf(
        Mode(
            "💬 Demande libre", "IA",
            "Demande ce que tu veux : « écris un poème sur la mer »",
            "Tu es un assistant intégré à un clavier. L'utilisateur écrit une demande, " +
                    "tu produis directement le texte demandé. $RULE"
        ),
        Mode(
            "🔍 Recherche Google", "Recherche",
            "Décris ce que tu cherches : « les papillons »",
            "Tu transformes la demande de l'utilisateur en UNE requête de recherche Google " +
                    "efficace : mots-clés précis, sans phrase complète, sans ponctuation inutile. " +
                    "Réponds uniquement avec la requête, sur une seule ligne."
        ),
        Mode(
            "🙏 Mot d'excuse", "Excuse",
            "Explique la situation : « en retard, réveil cassé »",
            "Tu rédiges un mot d'excuse court, poli et crédible à partir de la situation " +
                    "décrite. Ton respectueux, 2 à 4 phrases. $RULE"
        ),
        Mode(
            "📧 Email professionnel", "Email pro",
            "Dis l'objectif : « demander un congé le 12 mai »",
            "Tu rédiges un email professionnel complet et courtois (formule d'appel, " +
                    "corps clair, formule de politesse). $RULE"
        ),
        Mode(
            "💬 Réponse à un message", "Réponse",
            "Colle le message reçu, ou décris quoi répondre",
            "Tu rédiges une réponse naturelle et adaptée au message ou à la situation " +
                    "décrite par l'utilisateur. Ton chaleureux et humain. $RULE"
        ),
        Mode(
            "📝 Résumer", "Résumé",
            "Colle le texte à résumer",
            "Tu résumes le texte fourni en gardant uniquement l'essentiel, " +
                    "en quelques phrases claires. $RULE"
        ),
        Mode(
            "📖 Expliquer simplement", "Explication",
            "Écris le sujet à expliquer",
            "Tu expliques le sujet demandé de façon simple et concrète, " +
                    "comme à quelqu'un qui découvre. Court et clair. $RULE"
        ),
        Mode(
            "💡 Donne-moi des idées", "Idées",
            "Le thème : « cadeau pour ma mère »",
            "Tu proposes une courte liste d'idées concrètes et variées sur le thème donné. " +
                    "Une idée par ligne, avec un tiret. $RULE"
        ),
        Mode(
            "🎂 Message de vœux", "Vœux",
            "L'occasion et la personne : « anniversaire, mon frère »",
            "Tu rédiges un message de vœux chaleureux et personnel selon l'occasion. $RULE"
        ),
        Mode(
            "😂 Blague / réplique drôle", "Drôle",
            "Le sujet de la blague",
            "Tu écris une blague ou une réplique drôle et courte sur le sujet donné. $RULE"
        )
    )

    fun byShort(short: String): Mode? = list.firstOrNull { it.short == short }
}
