package com.perso.clavier

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Translator {

    val languages = listOf(
        "fr" to "Français", "en" to "English", "es" to "Español", "de" to "Deutsch",
        "it" to "Italiano", "pt" to "Português", "nl" to "Nederlands", "pl" to "Polski",
        "ru" to "Русский", "uk" to "Українська", "ar" to "العربية", "tr" to "Türkçe",
        "zh-CN" to "中文", "ja" to "日本語", "ko" to "한국어", "hi" to "हिन्दी"
    )

    fun languageName(code: String): String =
        languages.firstOrNull { it.first == code }?.second ?: code

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 9000
            readTimeout = 12000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            setRequestProperty("Accept", "application/json")
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        return text
    }

    /**
     * Traduit en essayant plusieurs services dans l'ordre.
     * Renvoie le texte traduit, ou null si tous echouent.
     */
    fun translate(prefs: Prefs, text: String, target: String, sourceHint: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 1. IA personnelle (si une cle est renseignee) : la meilleure qualite
        if (AiClient.isConfigured(prefs)) {
            val out = AiClient.ask(
                prefs,
                "Tu es un traducteur. Traduis le message de l'utilisateur en " +
                        languageName(target) + " (code " + target + "). " +
                        "Conserve le ton et les emojis. " +
                        "Réponds UNIQUEMENT avec la traduction, sans guillemets ni commentaire.",
                trimmed
            )
            if (!out.isNullOrBlank()) return out
        }

        // 2. DeepL (si cle renseignee)
        if (prefs.deeplKey.isNotBlank()) {
            try {
                val r = deepl(prefs.deeplKey.trim(), trimmed, target)
                if (r != null) return r
            } catch (_: Exception) {
            }
        }

        // 3. Google (cle officielle si fournie, sinon point d'acces public)
        if (prefs.googleTranslateKey.isNotBlank()) {
            try {
                val r = googleOfficial(prefs.googleTranslateKey.trim(), trimmed, target)
                if (r != null) return r
            } catch (_: Exception) {
            }
        }
        try {
            val r = googlePublic(trimmed, target)
            if (r != null) return r
        } catch (_: Exception) {
        }

        // 4. Services libres de secours
        for (host in listOf("lingva.ml", "lingva.lunar.icu", "translate.plausibility.cloud")) {
            try {
                val r = lingva(host, trimmed, target, sourceHint)
                if (r != null) return r
            } catch (_: Exception) {
            }
        }
        try {
            return mymemory(trimmed, target, sourceHint)
        } catch (_: Exception) {
        }
        return null
    }

    private fun googlePublic(text: String, target: String): String? {
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" +
                target + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8")
        val root = JSONArray(get(url))
        val segments = root.optJSONArray(0) ?: return null
        val sb = StringBuilder()
        for (i in 0 until segments.length()) {
            segments.optJSONArray(i)?.let { sb.append(it.optString(0)) }
        }
        return sb.toString().ifBlank { null }
    }

    private fun googleOfficial(key: String, text: String, target: String): String? {
        val url = "https://translation.googleapis.com/language/translate/v2?key=" + key +
                "&target=" + target + "&q=" + URLEncoder.encode(text, "UTF-8")
        val obj = JSONObject(get(url))
        val arr = obj.optJSONObject("data")?.optJSONArray("translations") ?: return null
        if (arr.length() == 0) return null
        return arr.getJSONObject(0).optString("translatedText").ifBlank { null }
            ?.replace("&#39;", "'")?.replace("&quot;", "\"")?.replace("&amp;", "&")
    }

    private fun deepl(key: String, text: String, target: String): String? {
        val host = if (key.endsWith(":fx")) "api-free.deepl.com" else "api.deepl.com"
        val tgt = target.uppercase().replace("ZH-CN", "ZH")
        val conn = (URL("https://$host/v2/translate").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 9000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Authorization", "DeepL-Auth-Key $key")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        val body = "text=" + URLEncoder.encode(text, "UTF-8") + "&target_lang=" + tgt
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) return null
        val arr = JSONObject(resp).optJSONArray("translations") ?: return null
        if (arr.length() == 0) return null
        return arr.getJSONObject(0).optString("text").ifBlank { null }
    }

    private fun lingva(host: String, text: String, target: String, source: String): String? {
        val tgt = target.substringBefore("-").lowercase()
        val url = "https://$host/api/v1/" + source + "/" + tgt + "/" +
                URLEncoder.encode(text, "UTF-8").replace("+", "%20")
        val obj = JSONObject(get(url))
        return obj.optString("translation").ifBlank { null }
    }

    private fun mymemory(text: String, target: String, source: String): String? {
        val url = "https://api.mymemory.translated.net/get?q=" +
                URLEncoder.encode(text.take(480), "UTF-8") +
                "&langpair=" + URLEncoder.encode("$source|$target", "UTF-8")
        val obj = JSONObject(get(url))
        val out = obj.optJSONObject("responseData")?.optString("translatedText") ?: return null
        if (out.isBlank() || out.startsWith("PLEASE SELECT", true) ||
            out.contains("INVALID", true) || out.contains("QUERY LENGTH LIMIT")
        ) return null
        return out
    }

    /** Diagnostic pour le bouton « Tester la traduction ». */
    fun test(prefs: Prefs): String {
        val sb = StringBuilder()
        if (AiClient.isConfigured(prefs)) {
            val r = AiClient.ask(prefs, "Traducteur. Réponds uniquement par la traduction.", "Traduis en anglais : bonjour")
            sb.append(if (r != null) "IA : ✅ ($r)\n" else "IA : ❌\n")
        } else sb.append("IA : non configurée\n")
        if (prefs.deeplKey.isNotBlank()) {
            sb.append(if (runCatching { deepl(prefs.deeplKey.trim(), "bonjour", "en") }.getOrNull() != null)
                "DeepL : ✅\n" else "DeepL : ❌\n")
        }
        sb.append(if (runCatching { googlePublic("bonjour", "en") }.getOrNull() != null)
            "Google public : ✅\n" else "Google public : ❌\n")
        var lingvaOk = false
        for (h in listOf("lingva.ml", "lingva.lunar.icu", "translate.plausibility.cloud")) {
            if (runCatching { lingva(h, "bonjour", "en", "fr") }.getOrNull() != null) {
                lingvaOk = true
                sb.append("Lingva ($h) : ✅\n")
                break
            }
        }
        if (!lingvaOk) sb.append("Lingva : ❌\n")
        sb.append(if (runCatching { mymemory("bonjour", "en", "fr") }.getOrNull() != null)
            "MyMemory : ✅" else "MyMemory : ❌")
        return sb.toString()
    }
}
