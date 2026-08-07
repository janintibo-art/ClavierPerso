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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
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

    private fun fetchBytes(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
        return conn.inputStream.readBytes().also { conn.disconnect() }
    }

    private fun search(q: String) {
        if (prefs.gifKey.isBlank()) {
            status.text = "Ajoute une clé GIF gratuite dans les réglages du clavier " +
                    "(section GIF). L'API Tenor a fermé le 30 juin 2026."
            return
        }
        status.text = "Chargement…"
        thread {
            val items = try {
                GifProvider.search(prefs, q)
            } catch (e: Exception) {
                null
            }
            main.post {
                if (items == null) {
                    status.text = "Connexion impossible. Vérifie ta clé dans les réglages."
                } else if (items.isEmpty()) {
                    status.text = "Aucun GIF trouvé"
                } else {
                    status.text = ""
                    build(items.map { it.preview to it.gif })
                }
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
