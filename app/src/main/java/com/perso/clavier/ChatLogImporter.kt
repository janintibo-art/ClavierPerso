package com.perso.clavier

/**
 * Analyse un export de conversation (WhatsApp, Telegram, Messenger, SMS...)
 * et permet de n'apprendre QUE les messages ecrits par l'utilisateur.
 */
object ChatLogImporter {

    /** Lignes de service a ignorer. */
    private val noise = listOf(
        "médias omis", "media omitted", "message supprimé", "message deleted",
        "chiffrés de bout en bout", "end-to-end encrypted", "a rejoint", "a quitté",
        "image absente", "sticker omis", "gif omis", "audio omis", "vidéo omise",
        "appel manqué", "missed call", "vous avez créé", "a changé"
    )

    /**
     * Reconnait :
     *   12/05/2024, 21:13 - Janintibo: Salut
     *   [12/05/2024 21:13:05] Janintibo : Salut
     *   05/12/2024, 9:13 PM - Janintibo: Salut
     */
    private val line = Regex(
        "^\\[?\\s*\\d{1,4}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4},?\\s+\\d{1,2}:\\d{2}(?::\\d{2})?\\s*" +
                "(?:[AaPp]\\.?[Mm]\\.?)?\\s*\\]?\\s*[-–—]?\\s*([^:]{1,40}?)\\s*:\\s*(.*)$"
    )

    /** Format simple « Nom: message », utilise par certains exports. */
    private val simpleLine = Regex("^([\\p{L}][\\p{L} .'\\-]{0,38}?)\\s*:\\s{1,4}(.+)$")

    class Parsed(val bySender: Map<String, StringBuilder>, val messageCount: Map<String, Int>)

    fun parse(raw: String): Parsed {
        val texts = HashMap<String, StringBuilder>()
        val counts = HashMap<String, Int>()
        var current: String? = null

        for (rawLine in raw.lineSequence()) {
            val l = rawLine.trim()
            if (l.isEmpty()) continue

            var sender: String? = null
            var body: String? = null

            val m = line.find(l)
            if (m != null) {
                sender = m.groupValues[1].trim()
                body = m.groupValues[2].trim()
            } else {
                val m2 = simpleLine.find(l)
                // On evite de confondre avec une phrase contenant « : »
                if (m2 != null && !m2.groupValues[1].contains("  ") &&
                    m2.groupValues[1].split(" ").size <= 4
                ) {
                    sender = m2.groupValues[1].trim()
                    body = m2.groupValues[2].trim()
                }
            }

            if (sender != null && body != null) {
                current = sender
                counts[sender] = (counts[sender] ?: 0) + 1
                if (isUseful(body)) {
                    texts.getOrPut(sender) { StringBuilder() }.append(body).append("\n")
                }
            } else if (current != null && l.length > 1) {
                // Suite d'un message sur plusieurs lignes
                if (isUseful(l)) {
                    texts.getOrPut(current) { StringBuilder() }.append(l).append("\n")
                }
            }
        }
        return Parsed(texts, counts)
    }

    private fun isUseful(body: String): Boolean {
        if (body.length < 2) return false
        val low = body.lowercase()
        if (noise.any { low.contains(it) }) return false
        if (low.startsWith("http")) return false
        return body.any { it.isLetter() }
    }

    /** Expediteurs tries par nombre de messages : le plus actif est souvent l'utilisateur. */
    fun senders(p: Parsed): List<Pair<String, Int>> =
        p.messageCount.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .map { it.key to it.value }

    /** Si aucun expediteur n'est detecte, on apprend tout le texte. */
    fun isChatExport(p: Parsed): Boolean = p.messageCount.isNotEmpty() &&
            p.messageCount.values.sum() >= 5
}
