package com.perso.clavier

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Translator {

    val languages = listOf(
        "fr" to "Français",
        "en" to "English",
        "es" to "Español",
        "de" to "Deutsch",
        "it" to "Italiano",
        "pt" to "Português",
        "nl" to "Nederlands",
        "pl" to "Polski",
        "ru" to "Русский",
        "uk" to "Українська",
        "ar" to "العربية",
        "tr" to "Türkçe",
        "zh-CN" to "中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "hi" to "हिन्दी"
    )

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        return conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
    }

    /** Traduit le texte. Renvoie null en cas d'échec des deux services. */
    fun translate(text: String, target: String, sourceHint: String): String? {
        return try {
            google(text, target)
        } catch (e: Exception) {
            try {
                mymemory(text, target, sourceHint)
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun google(text: String, target: String): String {
        val url = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=auto&tl=$target&dt=t&q=" +
                URLEncoder.encode(text, "UTF-8")
        val root = JSONArray(fetch(url))
        val segments = root.getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until segments.length()) {
            val seg = segments.optJSONArray(i) ?: continue
            sb.append(seg.optString(0))
        }
        val out = sb.toString()
        if (out.isBlank()) throw IllegalStateException("réponse vide")
        return out
    }

    private fun mymemory(text: String, target: String, source: String): String {
        val url = "https://api.mymemory.translated.net/get?q=" +
                URLEncoder.encode(text, "UTF-8") +
                "&langpair=" + URLEncoder.encode("$source|$target", "UTF-8")
        val obj = JSONObject(fetch(url))
        val out = obj.getJSONObject("responseData").getString("translatedText")
        if (out.isBlank()) throw IllegalStateException("réponse vide")
        return out
    }
}
