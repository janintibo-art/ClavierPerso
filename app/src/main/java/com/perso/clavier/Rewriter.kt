package com.perso.clavier

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Rewriter {

    /** Consigne utilisee par le bouton « corriger » de la barre d'outils. */
    const val FIX_INSTRUCTION =
        "en corrigeant TOUTES les fautes d'orthographe, de grammaire, de conjugaison, " +
                "d'accord et de ponctuation, ainsi que les accents manquants. " +
                "Ne change NI le sens, NI le style, NI le niveau de langage, NI les emojis. " +
                "Ne rajoute rien et ne reformule pas"

    val styles = listOf(
        "✅ Corriger les fautes" to FIX_INSTRUCTION,
        "😊 Plus poli" to "en le rendant plus poli et courtois",
        "💼 Plus professionnel" to "dans un style professionnel adapté au travail",
        "😂 Plus drôle" to "en le rendant drôle et léger",
        "✂️ Plus court" to "en le raccourcissant au maximum tout en gardant le sens",
        "❤️ Plus romantique" to "dans un style romantique et affectueux",
        "📖 Plus détaillé" to "en le développant avec plus de détails et de précisions"
    )

    fun rewrite(prefs: Prefs, text: String, instruction: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 1. IA personnelle si une cle est configuree : bien plus fiable
        if (AiClient.isConfigured(prefs)) {
            val out = AiClient.ask(
                prefs,
                "Tu réécris des messages. Réponds UNIQUEMENT avec le message réécrit, " +
                        "sans guillemets, sans explication, sans préambule. " +
                        "Garde la langue d'origine du message.",
                "Réécris ce message " + instruction + " :\n\n" + trimmed
            )
            if (!out.isNullOrBlank()) return clean(out, trimmed)
        }

        // 2. Service public gratuit en secours
        return try {
            val prompt = "Réécris le message suivant " + instruction + ". " +
                    "Garde la même langue. Réponds UNIQUEMENT avec le message réécrit, " +
                    "sans guillemets ni explication.\n\nMessage : " + trimmed
            val url = "https://text.pollinations.ai/" +
                    URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            }
            val code = conn.responseCode
            val out = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299 || out.isBlank()) null else clean(out, trimmed)
        } catch (e: Exception) {
            null
        }
    }

    /** Retire les guillemets ou preambules que l'IA ajoute parfois. */
    private fun clean(out: String, original: String): String? {
        var r = out.trim().trim('"', '«', '»', '\u201C', '\u201D').trim()
        val colon = r.indexOf(':')
        if (colon in 1..40 && r.take(colon).none { it == '.' } &&
            r.take(colon).lowercase().let {
                it.contains("voici") || it.contains("corrig") || it.contains("version")
            }
        ) {
            r = r.substring(colon + 1).trim().trim('"').trim()
        }
        // Garde-fou : une reponse absurdement longue n'est pas une correction
        if (r.isBlank() || r.length > original.length * 4 + 120) return null
        return r
    }
}
