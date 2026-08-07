package com.perso.clavier

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client compatible OpenAI : fonctionne avec OpenAI, Groq, Mistral, DeepSeek,
 * OpenRouter, Together, ou tout serveur exposant /chat/completions.
 */
object AiClient {

    fun isConfigured(prefs: Prefs): Boolean = prefs.aiKey.isNotBlank()

    /** Envoie une consigne et renvoie la reponse texte, ou null en cas d'echec. */
    fun ask(prefs: Prefs, system: String, user: String): String? {
        val key = prefs.aiKey.trim()
        if (key.isEmpty()) return null
        val base = prefs.aiBaseUrl.trim().trimEnd('/').ifEmpty { "https://api.openai.com/v1" }
        val model = prefs.aiModel.trim().ifEmpty { "gpt-4o-mini" }

        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", 0.3)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", user))
                })
            }
            val conn = (URL("$base/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) return null

            val json = JSONObject(text)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val msg = choices.getJSONObject(0).optJSONObject("message") ?: return null
            msg.optString("content").trim().trim('"').ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /** Message d'erreur lisible pour le bouton « Tester ». */
    fun test(prefs: Prefs): String {
        if (prefs.aiKey.isBlank()) return "Aucune clé IA renseignée"
        val r = ask(prefs, "Tu réponds en un mot.", "Dis simplement : OK")
        return if (r != null) "✅ IA connectée (réponse : $r)"
        else "❌ Échec : vérifie la clé, l'URL et le modèle"
    }
}
