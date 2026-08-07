package com.perso.clavier

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fournisseur de GIF.
 * L'API Tenor a ete definitivement arretee par Google le 30 juin 2026 :
 * on utilise donc Giphy ou Klipy, au choix de l'utilisateur.
 */
object GifProvider {

    const val GIPHY = 0
    const val KLIPY = 1

    /** (apercu, gif a envoyer) */
    class Item(val preview: String, val gif: String)

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 9000
            readTimeout = 14000
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

    private fun urlFor(prefs: Prefs, query: String): String? {
        val key = prefs.gifKey.trim()
        if (key.isEmpty()) return null
        val q = URLEncoder.encode(query, "UTF-8")
        return when (prefs.gifProvider) {
            KLIPY ->
                if (query.isEmpty())
                    "https://api.klipy.com/api/v1/$key/gifs/trending?per_page=24&customer_id=anarchie"
                else
                    "https://api.klipy.com/api/v1/$key/gifs/search?q=$q&per_page=24&customer_id=anarchie"
            else ->
                if (query.isEmpty())
                    "https://api.giphy.com/v1/gifs/trending?api_key=$key&limit=24&rating=pg-13"
                else
                    "https://api.giphy.com/v1/gifs/search?api_key=$key&q=$q&limit=24&rating=pg-13&lang=fr"
        }
    }

    fun search(prefs: Prefs, query: String): List<Item> {
        val url = urlFor(prefs, query) ?: return emptyList()
        val body = get(url)
        val parsed = when (prefs.gifProvider) {
            KLIPY -> parseKlipy(body)
            else -> parseGiphy(body)
        }
        return parsed.ifEmpty { parseGeneric(body) }
    }

    private fun parseGiphy(body: String): List<Item> {
        val out = ArrayList<Item>()
        try {
            val data = JSONObject(body).optJSONArray("data") ?: return out
            for (i in 0 until data.length()) {
                val images = data.getJSONObject(i).optJSONObject("images") ?: continue
                val preview = images.optJSONObject("fixed_width_small")?.optString("url")
                    ?: images.optJSONObject("preview_gif")?.optString("url")
                    ?: images.optJSONObject("fixed_width")?.optString("url")
                val full = images.optJSONObject("downsized")?.optString("url")
                    ?: images.optJSONObject("fixed_width")?.optString("url")
                    ?: images.optJSONObject("original")?.optString("url")
                if (!preview.isNullOrBlank() && !full.isNullOrBlank()) out.add(Item(preview, full))
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun parseKlipy(body: String): List<Item> {
        val out = ArrayList<Item>()
        try {
            val root = JSONObject(body)
            val data = root.optJSONObject("data")
            val arr: JSONArray = data?.optJSONArray("data")
                ?: data?.optJSONArray("items")
                ?: root.optJSONArray("data")
                ?: return out
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val file = item.optJSONObject("file") ?: continue
                fun url(size: String): String? =
                    file.optJSONObject(size)?.optJSONObject("gif")?.optString("url")
                        ?: file.optJSONObject(size)?.optString("url")
                val preview = url("sm") ?: url("xs") ?: url("md") ?: url("hd")
                val full = url("md") ?: url("hd") ?: url("sm")
                if (!preview.isNullOrBlank() && !full.isNullOrBlank()) out.add(Item(preview, full))
            }
        } catch (_: Exception) {
        }
        return out
    }

    /**
     * Filet de securite : si le format de reponse change, on recupere
     * simplement toutes les adresses de GIF presentes dans le JSON.
     */
    private fun parseGeneric(body: String): List<Item> {
        val urls = LinkedHashSet<String>()
        val regex = Regex("https?://[^\"\\s\\\\]+\\.gif[^\"\\s\\\\]*")
        for (m in regex.findAll(body)) urls.add(m.value)
        return urls.take(24).map { Item(it, it) }
    }

    fun test(prefs: Prefs): String {
        if (prefs.gifKey.isBlank()) return "❌ Aucune clé GIF renseignée"
        return try {
            val n = search(prefs, "").size
            if (n > 0) "✅ " + n + " GIF reçus" else "❌ Réponse vide (clé invalide ?)"
        } catch (e: Exception) {
            "❌ " + (e.message ?: "échec de connexion")
        }
    }
}
