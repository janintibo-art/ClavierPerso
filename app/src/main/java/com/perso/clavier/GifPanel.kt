package com.perso.clavier

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class GifPanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onCommit: (File) -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

    private val main = Handler(Looper.getMainLooper())
    private val grid = LinearLayout(context).apply { orientation = VERTICAL }
    private val status = TextView(context).apply {
        setTextColor(prefs.colorText)
        textSize = 13f
        setPadding(dp(12), dp(4), dp(12), dp(4))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(prefs.colorBg)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight)

        val topBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(2))
        }
        topBar.addView(TextView(context).apply {
            text = "✕"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(prefs.colorTextOnAccent)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(prefs.colorAccent)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(15), dp(12), dp(15), dp(12))
            setOnClickListener { onBack() }
        })
        val searchField = EditText(context).apply {
            hint = "Rechercher un GIF…"
            setHintTextColor((0x80 shl 24) or (prefs.colorText and 0xFFFFFF))
            setTextColor(prefs.colorText)
            textSize = 15f
            maxLines = 1
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(8); rightMargin = dp(8)
            }
        }
        topBar.addView(searchField)
        topBar.addView(TextView(context).apply {
            text = "🔍"
            textSize = 20f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { search(searchField.text.toString().trim()) }
        })
        addView(topBar)
        addView(status)

        val scroll = ScrollView(context)
        scroll.addView(grid)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        search("")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---------- Réseau ----------

    private fun fetchText(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        return conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
    }

    private fun fetchBytes(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        return conn.inputStream.readBytes().also { conn.disconnect() }
    }

    private fun apiUrls(q: String): List<String> {
        val urls = mutableListOf<String>()
        val key = prefs.tenorKey.trim()
        val enc = URLEncoder.encode(q, "UTF-8")
        if (key.isNotEmpty()) {
            urls.add(
                if (q.isEmpty())
                    "https://tenor.googleapis.com/v2/featured?key=$key&limit=24&media_filter=tinygif,tinygifpreview"
                else
                    "https://tenor.googleapis.com/v2/search?q=$enc&key=$key&limit=24&media_filter=tinygif,tinygifpreview"
            )
        }
        urls.add(
            if (q.isEmpty())
                "https://g.tenor.com/v1/trending?key=LIVDSRZULELA&limit=24"
            else
                "https://g.tenor.com/v1/search?q=$enc&key=LIVDSRZULELA&limit=24"
        )
        return urls
    }

    /** Retourne des paires (urlAperçu, urlGif). Gère les formats Tenor v1 et v2. */
    private fun parse(json: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val results = JSONObject(json).optJSONArray("results") ?: return out
        for (i in 0 until results.length()) {
            val obj = results.getJSONObject(i)
            val mf = obj.optJSONObject("media_formats")
            if (mf != null) { // v2
                val tiny = mf.optJSONObject("tinygif") ?: continue
                val gifUrl = tiny.optString("url")
                if (gifUrl.isEmpty()) continue
                val preview = mf.optJSONObject("tinygifpreview")?.optString("url")
                    ?.takeIf { it.isNotEmpty() } ?: gifUrl
                out.add(preview to gifUrl)
            } else { // v1
                val media = obj.optJSONArray("media") ?: continue
                if (media.length() == 0) continue
                val tiny = media.getJSONObject(0).optJSONObject("tinygif") ?: continue
                val gifUrl = tiny.optString("url")
                if (gifUrl.isEmpty()) continue
                val preview = tiny.optString("preview").takeIf { it.isNotEmpty() } ?: gifUrl
                out.add(preview to gifUrl)
            }
        }
        return out
    }

    private fun search(q: String) {
        status.text = "Chargement…"
        thread {
            for (url in apiUrls(q)) {
                try {
                    val items = parse(fetchText(url))
                    if (items.isNotEmpty()) {
                        main.post {
                            status.text = ""
                            build(items)
                        }
                        return@thread
                    }
                } catch (_: Exception) {
                }
            }
            main.post {
                status.text = "Impossible de charger les GIF. Vérifie ta connexion, ou ajoute une clé Tenor gratuite dans les réglages du clavier."
            }
        }
    }

    private fun build(items: List<Pair<String, String>>) {
        grid.removeAllViews()
        items.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            pair.forEach { (previewUrl, gifUrl) ->
                val iv = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(prefs.colorSpecial)
                    layoutParams = LayoutParams(0, dp(110), 1f).apply {
                        setMargins(dp(4), dp(4), dp(4), dp(4))
                    }
                    setOnClickListener { download(gifUrl) }
                }
                row.addView(iv)
                thread {
                    try {
                        val bytes = fetchBytes(previewUrl)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) main.post { iv.setImageBitmap(bmp) }
                    } catch (_: Exception) {
                    }
                }
            }
            if (pair.size == 1) {
                row.addView(TextView(context).apply {
                    layoutParams = LayoutParams(0, 1, 1f)
                })
            }
            grid.addView(row)
        }
    }

    private fun download(gifUrl: String) {
        status.text = "Envoi du GIF…"
        thread {
            try {
                val bytes = fetchBytes(gifUrl)
                val dir = File(context.cacheDir, "gifs").apply { mkdirs() }
                val file = File(dir, "gif_${gifUrl.hashCode()}.gif")
                file.writeBytes(bytes)
                main.post {
                    status.text = ""
                    onCommit(file)
                }
            } catch (_: Exception) {
                main.post { status.text = "Échec du téléchargement du GIF" }
            }
        }
    }
}
