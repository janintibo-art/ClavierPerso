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

    @Volatile var lastProvider: String = ""
        private set

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
            val out = msg.optString("content").trim().trim('"').ifEmpty { null }
            if (out != null) {
                lastProvider = try { URL(base).host.ifBlank { "API IA configurée" } }
                catch (_: Exception) { "API IA configurée" }
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Genere un texte : utilise la cle si elle existe, sinon un service public gratuit.
     * Renvoie null si tout echoue.
     */
    fun generate(prefs: Prefs, system: String, user: String): String? {
        ask(prefs, system, user)?.let { return it }
        if (!prefs.allowPublicFallbacks) return null
        return try {
            val prompt = system + "\n\n" + user
            val url = "https://text.pollinations.ai/" +
                    java.net.URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 35000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            }
            val code = conn.responseCode
            val out = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) null else out.trim().ifEmpty { null }?.also {
                lastProvider = "Pollinations (public)"
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Nettoie les preambules et guillemets que l'IA ajoute parfois. */
    fun cleanOutput(raw: String): String {
        var r = raw.trim()
        // Retire les blocs de code markdown (```bash … ```)
        if (r.startsWith("```")) {
            r = r.removePrefix("```").substringAfter('\n', "").substringBeforeLast("```").trim()
        }
        r = r.replace("```", "").trim()
        r = r.trim('"', '«', '»', '\u201C', '\u201D').trim()
        val firstLine = r.substringBefore('\n')
        val low = firstLine.lowercase()
        if (firstLine.length <= 60 && firstLine.endsWith(":") &&
            (low.contains("voici") || low.contains("réponse") || low.contains("resultat") ||
                    low.contains("résultat") || low.contains("requête") || low.contains("suggestion"))
        ) {
            r = r.substringAfter('\n').trim()
        }
        return r.trim().trim('"').trim()
    }

    /** Message d'erreur lisible pour le bouton « Tester ». */
    fun test(prefs: Prefs): String {
        if (prefs.aiKey.isBlank()) return "Aucune clé IA renseignée"
        val r = ask(prefs, "Tu réponds en un mot.", "Dis simplement : OK")
        return if (r != null) "✅ IA connectée (réponse : $r)"
        else "❌ Échec : vérifie la clé, l'URL et le modèle"
    }
}
