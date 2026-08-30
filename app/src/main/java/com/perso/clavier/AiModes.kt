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
            "💬 3 réponses rapides", "Réponses",
            "Colle le message reçu : 3 réponses possibles",
            "L'utilisateur te donne un message qu'il a reçu. " +
                    "Propose exactement 3 réponses courtes et naturelles, très différentes " +
                    "entre elles (une positive, une neutre ou évasive, une négative). " +
                    "Une par ligne, numérotées 1. 2. 3. Rien d'autre."
        ),
        Mode(
            "🎯 Adapter le ton", "Ton",
            "Ton message + à qui : « ... pour mon patron »",
            "L'utilisateur écrit un message et précise à qui il s'adresse. " +
                    "Réécris le message avec le niveau de langue adapté au destinataire " +
                    "(familier pour un ami, soutenu pour un supérieur). " +
                    "Réponds UNIQUEMENT avec le message réécrit."
        ),
        Mode(
            "🌍 Traduire ma réponse", "Trad-rép",
            "Écris ta réponse en français, elle part traduite",
            "L'utilisateur écrit une réponse en français à un message qu'il a reçu " +
                    "dans une autre langue. Traduis sa réponse dans la langue du message " +
                    "reçu si elle est déductible du contexte, sinon en anglais. " +
                    "Réponds UNIQUEMENT avec la traduction, sans guillemets ni commentaire."
        ),
        Mode(
            "📋 Développer mes notes", "Notes",
            "Des mots-clés : « rdv 14h dentiste annuler »",
            "L'utilisateur donne des mots-clés télégraphiques. Rédige à partir d'eux " +
                    "un message complet, poli et naturel, sans rien inventer d'essentiel. " +
                    "Réponds UNIQUEMENT avec le message final."
        ),
        Mode(
            "🔎 Extraire l'essentiel", "Extraire",
            "Colle un long texte : dates, montants, adresses",
            "Extrait du texte fourni les informations concrètes : dates, heures, " +
                    "montants, adresses, numéros, noms et actions à faire. " +
                    "Présente-les en liste courte, une par ligne, avec un tiret. " +
                    "N'ajoute aucun commentaire."
        ),
        Mode(
            "✍️ Écrire à ma façon", "Mon style",
            "Ce que tu veux dire, écrit avec tes tournures",
            "Rédige le message demandé en imitant la façon d'écrire de l'utilisateur " +
                    "telle qu'elle apparaît dans les exemples fournis : vocabulaire, " +
                    "longueur des phrases, ponctuation, niveau de langue. " +
                    "Réponds UNIQUEMENT avec le message final."
        ),
        Mode(
            "💻 Commande Termux", "Termux",
            "Décris ce que tu veux faire : « créer un dépôt git »",
            "Tu es un assistant Termux sur Android. L'utilisateur décrit ce qu'il veut faire, " +
                    "tu réponds avec LA ou LES commandes à coller directement dans Termux, " +
                    "une par ligne, dans l'ordre. Utilise pkg pour installer les paquets. " +
                    "Aucune explication, aucun commentaire, aucun bloc de code markdown, " +
                    "aucun symbole $ en début de ligne. Uniquement les commandes."
        ),
        Mode(
            "👨‍💻 Écrire du code", "Code",
            "Décris le code voulu : « une fonction qui trie un tableau »",
            "Tu es un assistant de programmation. Tu écris le code demandé, prêt à être collé. " +
                    "Réponds UNIQUEMENT avec le code, sans bloc markdown, sans explication " +
                    "avant ou après. Ajoute seulement de courts commentaires dans le code " +
                    "si c'est utile à la compréhension."
        ),
        Mode(
            "🐞 Expliquer une erreur", "Erreur",
            "Colle le message d'erreur reçu",
            "L'utilisateur colle une erreur (Termux, Gradle, Git, Android, Python…). " +
                    "Tu expliques en une ou deux phrases simples ce qui ne va pas, " +
                    "puis tu donnes la ou les commandes exactes pour corriger, une par ligne. " +
                    "Pas de bloc markdown, pas de longue théorie."
        ),
        Mode(
            "🌿 Commande Git", "Git",
            "Ce que tu veux faire : « annuler mon dernier commit »",
            "Tu réponds avec la ou les commandes git exactes pour réaliser ce que " +
                    "l'utilisateur décrit, une par ligne, sans explication, sans bloc markdown, " +
                    "sans symbole $ en début de ligne."
        ),
        Mode(
            "😂 Blague / réplique drôle", "Drôle",
            "Le sujet de la blague",
            "Tu écris une blague ou une réplique drôle et courte sur le sujet donné. $RULE"
        )
    )

    fun byShort(short: String): Mode? = list.firstOrNull { it.short == short }
}
