package com.perso.clavier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import java.io.File
import kotlin.math.abs
import kotlin.math.sin

class KeyboardView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        fun onText(text: String)
        fun onDelete()
        fun onEnter()
        fun onEmojiToggle()
        fun onPaste()
        fun onSettings()
        fun onLangSwitch()
        fun onGifToggle()
        fun onTranslateToggle()
        fun onFixSpelling()
        fun onAiToggle()
        fun onNavPanel()
        fun onAiFollowUp(instruction: String)
        fun onMoveCursor(delta: Int)
        fun onClipboardPanel()
        fun onRewrite()
        fun onSuggestion(word: String)
    }

    companion object {
        const val CODE_SHIFT = -1
        const val CODE_DEL = -2
        const val CODE_SYM = -3
        const val CODE_SPACE = -4
        const val CODE_ENTER = -5
        const val CODE_EMOJI = -6
        const val CODE_PASTE = -7
        const val CODE_SETTINGS = -8
        const val CODE_LANG = -9
        const val CODE_GIF = -10
        const val CODE_TRANSLATE = -14
        const val CODE_FIX = -15
        const val CODE_AI = -16
        const val CODE_NAV = -17
        // Les suggestions occupent une plage FERMEE : -11, -12, -13.
        // Tout nouveau code doit rester en dehors de cette plage.
        const val CODE_SUG = -11
        const val CODE_SUG_LAST = -13
        const val BG_FILE = "bg_image"
    }

    class Key(val label: String, val code: Int, val weight: Float = 1f)

    private var shift = true
    private var caps = false
    private var symbols = false
    private var lastShiftTap = 0L

    private var prefs = Prefs(context)
    private var bgBitmap: Bitmap? = null

    /** Thème imposé par l'application en cours d'utilisation (null = couleurs habituelles). */
    var appTheme: Theme? = null
        set(value) {
            field = value
            invalidate()
        }

    private fun colBg() = appTheme?.bg ?: prefs.colorBg
    private fun colKey() = appTheme?.key ?: prefs.colorKey
    private fun colSpecial() = appTheme?.special ?: prefs.colorSpecial
    private fun colAccent() = appTheme?.accent ?: prefs.colorAccent
    private fun colText() = appTheme?.text ?: prefs.colorText
    private fun colTextAccent() = appTheme?.textOnAccent ?: prefs.colorTextOnAccent

    var translateMode: String? = null
        set(value) {
            field = value
            invalidate()
        }

    /** Nom court du mode IA actif, ou null. */
    var aiMode: String? = null
        set(value) {
            field = value
            invalidate()
        }

    var suggestions: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    /** Index de la suggestion mise en avant (celle appliquee par la correction auto). */
    var highlightIndex: Int = -1
        set(value) {
            field = value
            invalidate()
        }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    /** Quand non nul, un appui sur une touche appelle ce callback au lieu d'ecrire (mode edition couleur). */
    var editModeListener: ((String) -> Unit)? = null

    private val startTime = System.currentTimeMillis()
    private var animating = false
    private val pressTimes = HashMap<String, Long>()

    /** Effet visuel en cours sur une touche. */
    private class Ripple(val rect: RectF, val start: Long, val label: String)

    private val ripples = ArrayList<Ripple>()
    private val effectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pressedKey: Key? = null
    private var instantDone = false
    private var downX = 0f
    private var spaceCursor = false
    private var cursorAnchor = 0f
    private var flashKey: Key? = null
    private var flashUntil = 0L
    private var longPressConsumed = false
    private val keyRects = mutableListOf<Pair<Key, RectF>>()

    init {
        refresh()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity

    fun refresh() {
        prefs = Prefs(context)
        textPaint.typeface = Fonts.get(prefs.fontIndex)
        bgBitmap = null
        if (prefs.bgImageEnabled) {
            val f = File(context.filesDir, BG_FILE)
            if (f.exists()) {
                try {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.absolutePath, opts)
                    var sample = 1
                    while (opts.outWidth / sample > 1440) sample *= 2
                    val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
                    var bmp = BitmapFactory.decodeFile(f.absolutePath, opts2)
                    val blur = prefs.bgBlur
                    if (bmp != null && blur > 0) {
                        bmp = blurBitmap(bmp, blur)
                    }
                    bgBitmap = bmp
                } catch (_: Exception) {
                }
            }
        }
        requestLayout()
        invalidate()
    }

    private fun blurBitmap(src: Bitmap, amount: Int): Bitmap {
        return try {
            val radius = (amount / 100f * 24f).coerceIn(1f, 25f)
            val input = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
            val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
            val rs = RenderScript.create(context)
            val inAlloc = Allocation.createFromBitmap(rs, input)
            val outAlloc = Allocation.createFromBitmap(rs, output)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(radius)
            script.setInput(inAlloc)
            script.forEach(outAlloc)
            outAlloc.copyTo(output)
            rs.destroy()
            output
        } catch (e: Exception) {
            // Repli universel : reduction puis agrandissement = flou
            try {
                val factor = (1 + amount / 12).coerceIn(2, 12)
                val small = Bitmap.createScaledBitmap(
                    src,
                    (src.width / factor).coerceAtLeast(1),
                    (src.height / factor).coerceAtLeast(1),
                    true
                )
                Bitmap.createScaledBitmap(small, src.width, src.height, true)
            } catch (e2: Exception) {
                src
            }
        }
    }

    fun autoShift() {
        if (!caps) {
            shift = true
            invalidate()
        }
    }

    // ---------- Construction des rangées ----------

    private fun letterKey(s: String) = Key(s, s[0].code)

    private fun bottomRow(): List<Key> = listOf(
        Key(if (symbols) "ABC" else "?123", CODE_SYM, 1.4f),
        Key("🌐", CODE_LANG, 1f),
        Key(",", ','.code, 1f),
        Key(Layouts.languages[prefs.langIndex.coerceIn(0, 2)], CODE_SPACE, 3.4f),
        Key(".", '.'.code, 1f),
        Key("⏎", CODE_ENTER, 1.5f)
    )

    private fun rows(): List<List<Key>> {
        if (symbols) {
            return listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { letterKey(it) },
                listOf("@", "#", "€", "_", "&", "-", "+", "(", ")", "/").map { letterKey(it) },
                listOf(Key("=", '='.code, 1.4f)) +
                        listOf("*", "\"", "'", ":", ";", "!", "?").map { letterKey(it) } +
                        listOf(Key("⌫", CODE_DEL, 1.4f)),
                bottomRow()
            )
        }
        val lang = prefs.langIndex.coerceIn(0, 2)
        val base = Layouts.rows(lang)
        val list = mutableListOf<List<Key>>()
        if (prefs.numberRow) {
            list.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { letterKey(it) })
        }
        list.add(base[0].map { letterKey(it) })
        list.add(base[1].map { letterKey(it) })
        list.add(
            listOf(Key("⇧", CODE_SHIFT, 1.4f)) +
                    base[2].map { letterKey(it) } +
                    listOf(Key("⌫", CODE_DEL, 1.4f))
        )
        list.add(bottomRow())
        return list
    }

    /** Applique un facteur de luminosite (100 = inchange). */
    private fun applyBrightness(color: Int, percent: Int): Int {
        if (percent == 100) return color
        val f = percent / 100f
        return Color.argb(
            Color.alpha(color),
            (Color.red(color) * f).toInt().coerceIn(0, 255),
            (Color.green(color) * f).toInt().coerceIn(0, 255),
            (Color.blue(color) * f).toInt().coerceIn(0, 255)
        )
    }

    private fun mix(a: Int, b: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) * (1 - r) + Color.red(b) * r).toInt(),
            (Color.green(a) * (1 - r) + Color.green(b) * r).toInt(),
            (Color.blue(a) * (1 - r) + Color.blue(b) * r).toInt()
        )
    }

    /** Couleur RGB animee pour une touche, ou null si le mode RGB est desactive. */
    private fun rgbColor(base: Int, label: String, rect: RectF, rowIndex: Int): Int? {
        val mode = prefs.rgbMode
        if (mode == 0) return null
        val t = (System.currentTimeMillis() - startTime) / 1000.0
        val speed = prefs.rgbSpeed / 100.0
        val hue: Float
        var value = 1f
        when (mode) {
            1 -> { // Vague arc-en-ciel horizontale
                val pos = if (width > 0) rect.centerX() / width else 0f
                hue = (((t * speed * 60) + pos * 360) % 360).toFloat()
            }
            2 -> { // Respiration : toutes les touches ensemble
                hue = ((t * speed * 40) % 360).toFloat()
                value = (0.55 + 0.45 * sin(t * speed * 2.2)).toFloat()
            }
            3 -> { // Reactif : la touche s'illumine quand on la frappe
                val pos = if (width > 0) rect.centerX() / width else 0f
                hue = (((t * speed * 30) + pos * 200) % 360).toFloat()
                val last = pressTimes[label]
                val age = if (last == null) 9999L else System.currentTimeMillis() - last
                value = if (age < 700) (0.35f + 0.65f * (1f - age / 700f)) else 0.35f
            }
            else -> { // Cascade verticale
                hue = (((t * speed * 60) + rowIndex * 45) % 360).toFloat()
            }
        }
        val rainbow = Color.HSVToColor(floatArrayOf(hue, 0.85f, value.coerceIn(0.15f, 1f)))
        return mix(base, rainbow, prefs.rgbIntensity / 100f)
    }

    private fun withOpacity(color: Int): Int {
        val a = prefs.keyOpacity.coerceIn(15, 100) * 255 / 100
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    private fun toolsHeight() = dp(40f)
    private fun sugHeight() =
        if (prefs.suggestionsEnabled || translateMode != null || aiMode != null) dp(42f) else 0f
    private fun barHeight() = toolsHeight() + sugHeight()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (barHeight() + dp(prefs.keyHeight.toFloat()) * rows().size + dp(12f)).toInt()
        setMeasuredDimension(w, h)
    }

    private fun displayLabel(key: Key): String {
        if (key.code == CODE_ENTER && (translateMode != null || aiMode != null)) return "➜"
        if (key.code == CODE_SHIFT) return if (caps) "⇪" else "⇧"
        if (key.code >= 0 && !symbols && (shift || caps)) return key.label.uppercase()
        return key.label
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bgBitmap
        if (bmp != null && width > 0 && height > 0) {
            val scale = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
            val sw = width / scale
            val sh = height / scale
            val sx = (bmp.width - sw) / 2f
            val sy = (bmp.height - sh) / 2f
            val src = Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
            val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            val br = prefs.bgBrightness / 100f
            val sat = prefs.bgSaturation / 100f
            if (br != 1f || sat != 1f) {
                val cm = ColorMatrix()
                cm.setSaturation(sat)
                if (br != 1f) {
                    val scale = ColorMatrix(
                        floatArrayOf(
                            br, 0f, 0f, 0f, 0f,
                            0f, br, 0f, 0f, 0f,
                            0f, 0f, br, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    cm.postConcat(scale)
                }
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(bmp, src, Rect(0, 0, width, height), bgPaint)
            canvas.drawColor(Color.argb(prefs.bgDim.coerceIn(0, 90) * 255 / 100, 0, 0, 0))
        } else {
            canvas.drawColor(colBg())
        }

        keyRects.clear()

        // ---------- Barre 1 : outils ----------
        val toolsH = toolsHeight()
        val sugH = sugHeight()

        /** Dessine un element de barre en ajustant le texte a la largeur disponible. */
        fun topItem(key: Key, rect: RectF, textSizePx: Float, active: Boolean = false) {
            keyRects.add(key to rect)
            val lit = key === pressedKey || active
            if (lit) {
                keyPaint.color = withOpacity(colAccent())
                canvas.drawRoundRect(
                    RectF(rect.left + dp(3f), rect.top + dp(4f), rect.right - dp(3f), rect.bottom - dp(4f)),
                    dp(8f), dp(8f), keyPaint
                )
            }
            textPaint.color = if (lit) colTextAccent() else colText()

            // Reduire puis tronquer pour ne jamais deborder sur le voisin
            val avail = rect.width() - dp(8f)
            var size = textSizePx
            textPaint.textSize = size
            var label = key.label
            while (textPaint.measureText(label) > avail && size > textSizePx * 0.7f) {
                size -= dp(0.7f)
                textPaint.textSize = size
            }
            if (textPaint.measureText(label) > avail) {
                while (label.length > 1 && textPaint.measureText(label + "…") > avail) {
                    label = label.dropLast(1)
                }
                label += "…"
            }
            val ty = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
            canvas.drawText(label, rect.centerX(), ty, textPaint)
        }

        val tools = listOf(
            Key("😀", CODE_EMOJI),
            Key("GIF", CODE_GIF),
            Key("📋", CODE_PASTE),
            Key("✅", CODE_FIX),
            Key("🤖", CODE_AI),
            Key("🌍", CODE_TRANSLATE),
            Key("⚙️", CODE_SETTINGS)
        )
        val toolW = width / tools.size.toFloat()
        tools.forEachIndexed { i, key ->
            val r = RectF(toolW * i, 0f, toolW * (i + 1), toolsH)
            val size = if (key.label == "GIF") sp(14f) else sp(18f)
            val on = (key.code == CODE_TRANSLATE && translateMode != null) ||
                    (key.code == CODE_AI && aiMode != null)
            topItem(key, r, size, active = on)
        }

        linePaint.color = (0x28 shl 24) or (colText() and 0xFFFFFF)
        linePaint.strokeWidth = dp(1f)

        // ---------- Barre 2 : suggestions (pleine largeur) ----------
        if (sugH > 0f) {
            canvas.drawLine(dp(10f), toolsH, width - dp(10f), toolsH, linePaint)
            val tm = translateMode
            val am = aiMode
            // Le bandeau lance l'action ; le ✕ a droite quitte le mode.
            val exitW = dp(52f)
            if (tm != null) {
                topItem(
                    Key("➜ Traduire en $tm", CODE_ENTER),
                    RectF(0f, toolsH, width - exitW, toolsH + sugH), sp(15f), active = true
                )
                topItem(
                    Key("✕", CODE_TRANSLATE),
                    RectF(width - exitW, toolsH, width.toFloat(), toolsH + sugH), sp(16f)
                )
            } else if (am != null) {
                topItem(
                    Key("➜ $am", CODE_ENTER),
                    RectF(0f, toolsH, width - exitW, toolsH + sugH), sp(15f), active = true
                )
                topItem(
                    Key("✕", CODE_AI),
                    RectF(width - exitW, toolsH, width.toFloat(), toolsH + sugH), sp(16f)
                )
            } else {
                val sugW = width / 3f
                for (i in 0 until 3) {
                    val r = RectF(sugW * i, toolsH, sugW * (i + 1), toolsH + sugH)
                    if (i < suggestions.size) {
                        val label = if (i == 0 && highlightIndex == 1)
                            "\u201C" + suggestions[i] + "\u201D" else suggestions[i]
                        topItem(
                            Key(label, CODE_SUG - i), r, sp(15f),
                            active = i == highlightIndex
                        )
                    }
                    if (i > 0 && suggestions.size > i) {
                        canvas.drawLine(r.left, toolsH + dp(9f), r.left, toolsH + sugH - dp(9f), linePaint)
                    }
                }
            }
        }

        // ---------- Touches ----------
        val rowH = dp(prefs.keyHeight.toFloat())
        val margin = dp(3f)
        val sidePad = dp(4f)
        val radius = dp(9f)
        val usable = width - sidePad * 2
        var y = barHeight() + dp(4f)

        val now = System.currentTimeMillis()
        val flashing = flashKey != null && now < flashUntil
        var rowIndex = 0
        for (row in rows()) {
            val totalW = row.map { it.weight }.sum()
            var x = sidePad
            for (key in row) {
                val kw = usable * key.weight / totalW
                val rect = RectF(x + margin, y + margin, x + kw - margin, y + rowH - margin)
                keyRects.add(key to rect)

                val isAccent = key === pressedKey ||
                        (flashing && key === flashKey) ||
                        key.code == CODE_ENTER ||
                        (key.code == CODE_SHIFT && (shift || caps))
                var base = when {
                    isAccent -> colAccent()
                    key.code < 0 -> colSpecial()
                    else -> colKey()
                }
                // Couleur specifique a cette touche
                prefs.keyColor(key.label)?.let { if (!isAccent) base = it }
                // Animation RGB
                val rgb = if (!isAccent) rgbColor(base, key.label, rect, rowIndex) else null
                if (rgb != null) base = rgb
                // Luminosite : globale x luminosite propre a la touche
                val bright = prefs.brightness * prefs.keyBrightness(key.label) / 100
                base = applyBrightness(base, bright)

                keyPaint.color = withOpacity(base)
                canvas.drawRoundRect(rect, radius, radius, keyPaint)

                textPaint.color = when {
                    isAccent -> colTextAccent()
                    rgb != null && prefs.rgbText -> rgb
                    else -> colText()
                }
                textPaint.textSize = if (key.label.length > 2)
                    sp(prefs.textSize * 0.62f) else sp(prefs.textSize.toFloat())
                val ty = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(displayLabel(key), rect.centerX(), ty, textPaint)

                x += kw
            }
            y += rowH
            rowIndex++
        }

        drawRipples(canvas)

        // Bulle d'apercu au-dessus de la touche pressee
        val pk = pressedKey
        if (pk != null && pk.code >= 0 && prefs.keyPopup) {
            val rect = keyRects.firstOrNull { it.first === pk }?.second
            if (rect != null) {
                val bw = maxOf(rect.width() * 1.25f, dp(46f))
                val bh = dp(prefs.keyHeight.toFloat())
                val cx = rect.centerX().coerceIn(bw / 2, width - bw / 2)
                var bottom = rect.top - dp(4f)
                var top = bottom - bh
                if (top < 0f) { top = 0f; bottom = bh }
                val bubble = RectF(cx - bw / 2, top, cx + bw / 2, bottom)
                keyPaint.color = colAccent()
                canvas.drawRoundRect(bubble, dp(10f), dp(10f), keyPaint)
                textPaint.color = colTextAccent()
                textPaint.textSize = sp(prefs.textSize * 1.35f)
                val ty = bubble.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(displayLabel(pk), bubble.centerX(), ty, textPaint)
            }
        }

        if (flashing) {
            postInvalidateDelayed(flashUntil - now + 20)
        }

        // Animation continue quand le mode RGB est actif
        if (prefs.rgbMode != 0 && isShown) {
            animating = true
            postInvalidateDelayed(40)
        } else {
            animating = false
        }
    }

    private fun keyAt(x: Float, y: Float): Key? {
        keyRects.firstOrNull { it.second.contains(x, y) }?.let { return it.first }
        // Tolerance : rattraper les appuis qui tombent entre deux touches
        val margin = dp(prefs.touchMargin.toFloat())
        if (margin <= 0f) return null
        var best: Key? = null
        var bestDist = Float.MAX_VALUE
        for ((key, r) in keyRects) {
            val dx = when {
                x < r.left -> r.left - x
                x > r.right -> x - r.right
                else -> 0f
            }
            val dy = when {
                y < r.top -> r.top - y
                y > r.bottom -> y - r.bottom
                else -> 0f
            }
            if (dx > margin || dy > margin) continue
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                best = key
            }
        }
        return best
    }

    /** Touches qui peuvent partir des le contact (pas celles a appui long ou a glissement). */
    private fun isInstantKey(key: Key): Boolean = when (key.code) {
        CODE_DEL, CODE_SPACE, CODE_ENTER, CODE_PASTE, CODE_EMOJI, CODE_GIF,
        CODE_SETTINGS, CODE_TRANSLATE, CODE_LANG, CODE_SHIFT, CODE_SYM -> false
        else -> key.code >= 0 &&
                !(!symbols && Layouts.accents(prefs.langIndex.coerceIn(0, 2)).containsKey(key.label))
    }

    private fun rectOf(key: Key): RectF? = keyRects.firstOrNull { it.first === key }?.second

    private fun addRipple(key: Key) {
        if (prefs.pressEffect == 0) return
        val r = rectOf(key) ?: return
        if (ripples.size > 12) ripples.removeAt(0)
        ripples.add(Ripple(RectF(r), System.currentTimeMillis(), key.label))
        invalidate()
    }

    private fun effectColor(): Int {
        val c = prefs.pressEffectColor
        return if (c == 0) prefs.colorAccent else c
    }

    /** Dessine les effets de frappe par-dessus les touches. */
    private fun drawRipples(canvas: Canvas) {
        if (ripples.isEmpty()) return
        val now = System.currentTimeMillis()
        val duration = prefs.pressEffectDuration.coerceIn(80, 900).toLong()
        val mode = prefs.pressEffect
        val color = effectColor()
        val it = ripples.iterator()
        var alive = false
        while (it.hasNext()) {
            val rp = it.next()
            val age = now - rp.start
            if (age > duration) {
                it.remove()
                continue
            }
            alive = true
            val t = age.toFloat() / duration
            val fade = 1f - t
            when (mode) {
                1 -> { // Couleur : la touche se teinte puis revient
                    effectPaint.style = Paint.Style.FILL
                    effectPaint.color = Color.argb((190 * fade).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                    canvas.drawRoundRect(rp.rect, dp(9f), dp(9f), effectPaint)
                }
                2 -> { // Onde qui s'etend depuis le centre
                    effectPaint.style = Paint.Style.FILL
                    effectPaint.color = Color.argb((140 * fade).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                    canvas.save()
                    canvas.clipRect(rp.rect)
                    val maxR = maxOf(rp.rect.width(), rp.rect.height()) * 0.9f
                    canvas.drawCircle(rp.rect.centerX(), rp.rect.centerY(), maxR * t, effectPaint)
                    canvas.restore()
                }
                3 -> { // Zoom : cadre qui grandit et s'efface
                    effectPaint.style = Paint.Style.STROKE
                    effectPaint.strokeWidth = dp(2.5f) * fade
                    effectPaint.color = Color.argb((230 * fade).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                    val g = dp(10f) * t
                    canvas.drawRoundRect(
                        RectF(rp.rect.left - g, rp.rect.top - g, rp.rect.right + g, rp.rect.bottom + g),
                        dp(11f), dp(11f), effectPaint
                    )
                }
                4 -> { // Eclat : halo lumineux
                    effectPaint.style = Paint.Style.FILL
                    val glow = (110 * fade).toInt()
                    for (i in 3 downTo 1) {
                        val g = dp(4f) * i * (0.4f + t)
                        effectPaint.color = Color.argb(glow / i, Color.red(color), Color.green(color), Color.blue(color))
                        canvas.drawRoundRect(
                            RectF(rp.rect.left - g, rp.rect.top - g, rp.rect.right + g, rp.rect.bottom + g),
                            dp(12f), dp(12f), effectPaint
                        )
                    }
                }
                else -> { // Etincelles qui jaillissent
                    effectPaint.style = Paint.Style.FILL
                    effectPaint.color = Color.argb((235 * fade).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                    val cx = rp.rect.centerX()
                    val cy = rp.rect.centerY()
                    val dist = dp(24f) * t
                    for (i in 0 until 6) {
                        val a = (i * 60 + rp.label.hashCode() % 30) * Math.PI / 180.0
                        canvas.drawCircle(
                            cx + (Math.cos(a) * dist).toFloat(),
                            cy + (Math.sin(a) * dist).toFloat(),
                            dp(2.6f) * fade, effectPaint
                        )
                    }
                }
            }
        }
        effectPaint.style = Paint.Style.FILL
        if (alive) postInvalidateDelayed(16)
    }

    private val repeatDelete = object : Runnable {
        override fun run() {
            longPressConsumed = true
            listener.onDelete()
            handler.postDelayed(this, 60)
        }
    }

    private val clipPanelRunnable = Runnable {
        longPressConsumed = true
        pressedKey = null
        invalidate()
        listener.onClipboardPanel()
    }

    private val rewriteRunnable = Runnable {
        longPressConsumed = true
        pressedKey = null
        invalidate()
        listener.onRewrite()
    }

    private val navPanelRunnable = Runnable {
        longPressConsumed = true
        pressedKey = null
        invalidate()
        listener.onNavPanel()
    }

    private val completeRunnable = Runnable {
        longPressConsumed = true
        pressedKey = null
        invalidate()
        listener.onAiFollowUp("__complete__")
    }

    private val accentRunnable = Runnable {
        val key = pressedKey ?: return@Runnable
        val acc = Layouts.accents(prefs.langIndex.coerceIn(0, 2))[key.label] ?: return@Runnable
        longPressConsumed = true
        val t = if (shift || caps) acc.uppercase() else acc
        listener.onText(t)
        if (shift && !caps) shift = false
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val key = keyAt(event.x, event.y) ?: return true
                val edit = editModeListener
                if (edit != null) {
                    pressedKey = key
                    invalidate()
                    return true
                }
                pressTimes[key.label] = System.currentTimeMillis()
                pressedKey = key
                downX = event.x
                spaceCursor = false
                longPressConsumed = false
                feedback()
                addRipple(key)

                // Delais adaptes a la sensibilite, avec un plancher :
                // sans lui, une sensibilite elevee transformait un simple appui en appui long.
                val f = prefs.sensitivity.coerceIn(30, 200) / 100f
                fun delay(base: Int, floor: Int = 300) = (base * f).toLong().coerceAtLeast(floor.toLong())
                when {
                    key.code == CODE_DEL -> handler.postDelayed(repeatDelete, delay(400, 320))
                    key.code == CODE_PASTE -> handler.postDelayed(clipPanelRunnable, delay(450, 400))
                    key.code == CODE_SYM -> handler.postDelayed(navPanelRunnable, delay(450, 400))
                    key.code == CODE_AI -> handler.postDelayed(completeRunnable, delay(500, 450))
                    key.code == CODE_ENTER -> handler.postDelayed(rewriteRunnable, delay(500, 450))
                    key.code >= 0 && !symbols &&
                            Layouts.accents(prefs.langIndex.coerceIn(0, 2)).containsKey(key.label) ->
                        handler.postDelayed(accentRunnable, delay(380, 300))
                }

                // Frappe instantanee : la lettre part des le contact
                if (prefs.instantKey && isInstantKey(key)) {
                    handleKey(key)
                    instantDone = true
                } else {
                    instantDone = false
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressedKey?.code == CODE_SPACE) {
                    val dx = event.x - downX
                    val threshold = dp(10f + prefs.sensitivity / 12f)
                    if (!spaceCursor && abs(dx) > threshold) {
                        spaceCursor = true
                        handler.removeCallbacksAndMessages(null)
                        cursorAnchor = event.x
                    }
                    if (spaceCursor) {
                        val step = dp(11f)
                        while (event.x - cursorAnchor > step) {
                            listener.onMoveCursor(1)
                            cursorAnchor += step
                        }
                        while (event.x - cursorAnchor < -step) {
                            listener.onMoveCursor(-1)
                            cursorAnchor -= step
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val edit = editModeListener
                if (edit != null) {
                    val k = pressedKey
                    pressedKey = null
                    invalidate()
                    if (k != null && event.actionMasked == MotionEvent.ACTION_UP) edit(k.label)
                    return true
                }
                handler.removeCallbacksAndMessages(null)
                val key = pressedKey
                pressedKey = null
                if (key != null) {
                    flashKey = key
                    flashUntil = System.currentTimeMillis() + 160
                }
                invalidate()
                if (key != null && !longPressConsumed && !spaceCursor && !instantDone &&
                    event.actionMasked == MotionEvent.ACTION_UP
                ) {
                    handleKey(key)
                }
                instantDone = false
                spaceCursor = false
            }
        }
        return true
    }

    private fun handleKey(key: Key) {
        when {
            key.code <= CODE_SUG && key.code >= CODE_SUG_LAST ->
                listener.onSuggestion(key.label)
            key.code == CODE_SHIFT -> {
                val now = System.currentTimeMillis()
                when {
                    caps -> { caps = false; shift = false }
                    now - lastShiftTap < 500 -> { caps = true; shift = false }
                    else -> shift = !shift
                }
                lastShiftTap = now
            }
            key.code == CODE_DEL -> listener.onDelete()
            key.code == CODE_SYM -> symbols = !symbols
            key.code == CODE_SPACE -> listener.onText(" ")
            key.code == CODE_ENTER -> listener.onEnter()
            key.code == CODE_EMOJI -> listener.onEmojiToggle()
            key.code == CODE_PASTE -> listener.onPaste()
            key.code == CODE_SETTINGS -> listener.onSettings()
            key.code == CODE_LANG -> listener.onLangSwitch()
            key.code == CODE_GIF -> listener.onGifToggle()
            key.code == CODE_TRANSLATE -> listener.onTranslateToggle()
            key.code == CODE_FIX -> listener.onFixSpelling()
            key.code == CODE_AI -> listener.onAiToggle()
            else -> {
                var t = key.label
                if (!symbols && (shift || caps)) t = t.uppercase()
                listener.onText(t)
                if (shift && !caps) shift = false
            }
        }
        invalidate()
        requestLayout()
    }

    private fun feedback() {
        if (prefs.vibration) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (prefs.sound) {
            val type = prefs.soundType
            if (type == 0) {
                audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, prefs.soundVolume / 100f)
            } else {
                val special = pressedKey?.code?.let { it < 0 } ?: false
                KeySounds.play(type, prefs.soundVolume, special)
            }
        }
    }
}
