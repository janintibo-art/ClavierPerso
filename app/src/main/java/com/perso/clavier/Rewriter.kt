package com.perso.clavier

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Rewriter {

    val styles = listOf(
        "😊 Plus poli" to "en le rendant plus poli et courtois",
        "💼 Plus professionnel" to "dans un style professionnel adapté au travail",
        "😂 Plus drôle" to "en le rendant drôle et léger",
        "✂️ Plus court" to "en le raccourcissant au maximum tout en gardant le sens",
        "❤️ Plus romantique" to "dans un style romantique et affectueux",
        "✅ Corriger l'orthographe" to "en corrigeant uniquement l'orthographe et la grammaire, sans changer le style ni le sens"
    )

    fun rewrite(text: String, instruction: String): String? {
        val prompt = "Réécris le message suivant $instruction. " +
                "Garde la même langue que le message d'origine. " +
                "Réponds UNIQUEMENT avec le message réécrit, sans guillemets ni explication.\n\n" +
                "Message : $text"
        return try {
            val url = "https://text.pollinations.ai/" +
                    URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 25000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val out = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
                .trim().trim('"').trim()
            if (out.isBlank()) null else out
        } catch (e: Exception) {
            null
        }
    }
}
