package com.perso.clavier

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Panneau GIF. Il s'affiche AU-DESSUS du clavier : celui-ci reste utilisable
 * pour taper la recherche (les touches sont redirigees vers ce panneau).
 */
class GifPanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onCommit: (File) -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val grid = LinearLayout(context).apply { orientation = VERTICAL }
    private val queryView = TextView(context)
    private val status = TextView(context).apply {
        setTextColor(prefs.colorText)
        textSize = 12f
        setPadding(dp(12), dp(2), dp(12), dp(2))
    }

    private val query = StringBuilder()
    private val worker = Executors.newFixedThreadPool(4)
    private val searchSeq = AtomicInteger(0)

    private val categories = listOf(
        "🔥 Tendances" to "", "😂 Rire" to "rire", "❤️ Amour" to "amour",
        "👍 OK" to "ok", "🙏 Merci" to "merci", "👋 Salut" to "salut",
        "😭 Triste" to "triste", "🎉 Fête" to "fete", "😮 Wow" to "wow",
        "💃 Danse" to "danse", "🐱 Chat" to "chat", "🐶 Chien" to "chien"
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(prefs.colorBg)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight)

        // Barre du haut : fermer + zone de recherche
        val topBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(2))
        }
        topBar.addView(TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorTextOnAccent)
            background = GradientDrawable().apply {
                setColor(prefs.colorAccent)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { onBack() }
        })
        queryView.apply {
            text = "Tape sur le clavier pour chercher…"
            textSize = 15f
            maxLines = 1
            setTextColor(prefs.colorText)
            alpha = 0.6f
            background = GradientDrawable().apply {
                setColor(prefs.colorKey)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(8)
                rightMargin = dp(8)
            }
        }
        topBar.addView(queryView)
        topBar.addView(TextView(context).apply {
            text = "🔍"
            textSize = 18f
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { search(query.toString()) }
        })
        addView(topBar)

        // Categories rapides (sans clavier)
        val cats = LinearLayout(context).apply { orientation = HORIZONTAL }
        categories.forEach { (label, q) ->
            cats.addView(TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(prefs.colorText)
                background = GradientDrawable().apply {
                    setColor(prefs.colorSpecial)
                    cornerRadius = dp(14).toFloat()
                }
                setPadding(dp(12), dp(7), dp(12), dp(7))
                val lp = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dp(4), dp(4), dp(0), dp(4))
                layoutParams = lp
                setOnClickListener {
                    query.setLength(0)
                    query.append(q)
                    refreshQueryView()
                    search(q)
                }
            })
        }
        addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(cats)
        })

        addView(status)

        val scroll = ScrollView(context)
        scroll.addView(grid)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        search("")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---------- Saisie renvoyee par le clavier ----------

    fun appendQuery(text: String) {
        query.append(text)
        refreshQueryView()
    }

    fun deleteQuery() {
        if (query.isNotEmpty()) query.deleteCharAt(query.length - 1)
        refreshQueryView()
    }

    fun runSearch() = search(query.toString())

    private fun refreshQueryView() {
        if (query.isEmpty()) {
            queryView.text = "Tape sur le clavier pour chercher…"
            queryView.alpha = 0.6f
        } else {
            queryView.text = query.toString()
            queryView.alpha = 1f
        }
    }

    // ---------- Reseau ----------

    private fun fetchBytes(url: String, maxBytes: Int): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
        try {
            val announced = conn.contentLengthLong
            if (announced > maxBytes) throw IllegalStateException("Fichier trop volumineux")
            val out = ByteArrayOutputStream(minOf(maxBytes, 512 * 1024))
            conn.inputStream.use { input ->
                val buf = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    total += n
                    if (total > maxBytes) throw IllegalStateException("Fichier trop volumineux")
                    out.write(buf, 0, n)
                }
            }
            return out.toByteArray()
        } finally {
            conn.disconnect()
        }
    }

    private fun search(q: String) {
        val seq = searchSeq.incrementAndGet()
        status.text = "Chargement…"
        worker.submit {
            // La cle est dechiffree via Keystore : on la lit hors du thread principal.
            if (prefs.gifKey.isBlank()) {
                main.post {
                    if (seq == searchSeq.get() && isAttachedToWindow) {
                        status.text = "Ajoute une clé GIF gratuite dans les réglages (section GIF)."
                    }
                }
                return@submit
            }
            val items = try {
                GifProvider.search(prefs, q)
            } catch (_: Exception) {
                null
            }
            main.post {
                if (seq != searchSeq.get() || !isAttachedToWindow) return@post
                when {
                    items == null -> status.text = "Connexion impossible. Vérifie ta clé."
                    items.isEmpty() -> status.text = "Aucun GIF trouvé"
                    else -> {
                        status.text = ""
                        build(items.map { it.preview to it.gif }, seq)
                    }
                }
            }
        }
    }

    private fun build(items: List<Pair<String, String>>, seq: Int) {
        grid.removeAllViews()
        items.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            pair.forEach { (previewUrl, gifUrl) ->
                val iv = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(prefs.colorSpecial)
                    layoutParams = LayoutParams(0, dp(100), 1f).apply {
                        setMargins(dp(4), dp(4), dp(4), dp(4))
                    }
                    setOnClickListener { download(gifUrl) }
                }
                row.addView(iv)
                worker.submit {
                    try {
                        val bytes = fetchBytes(previewUrl, 5 * 1024 * 1024)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) main.post {
                            if (seq == searchSeq.get() && isAttachedToWindow) iv.setImageBitmap(bmp)
                        }
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
        worker.submit {
            try {
                val bytes = fetchBytes(gifUrl, 18 * 1024 * 1024)
                val dir = File(context.cacheDir, "gifs").apply { mkdirs() }
                val file = File(dir, "gif_${gifUrl.hashCode()}.gif")
                file.writeBytes(bytes)
                main.post {
                    if (!isAttachedToWindow) return@post
                    status.text = ""
                    onCommit(file)
                }
            } catch (_: Exception) {
                main.post { if (isAttachedToWindow) status.text = "Échec du téléchargement" }
            }
        }
    }

    override fun onDetachedFromWindow() {
        searchSeq.incrementAndGet()
        worker.shutdownNow()
        super.onDetachedFromWindow()
    }
}
