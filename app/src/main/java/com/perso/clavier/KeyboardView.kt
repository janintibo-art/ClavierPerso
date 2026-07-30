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
import java.io.File

class KeyboardView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        fun onText(text: String)
        fun onDelete()
        fun onEnter()
        fun onEmojiToggle()
        fun onPaste()
        fun onSettings()
        fun onLangSwitch()
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
        const val CODE_SUG = -11 // -11, -12, -13
        const val BG_FILE = "bg_image"
    }

    class Key(val label: String, val code: Int, val weight: Float = 1f)

    private var shift = true
    private var caps = false
    private var symbols = false
    private var lastShiftTap = 0L

    private var prefs = Prefs(context)
    private var bgBitmap: Bitmap? = null

    var suggestions: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private var pressedKey: Key? = null
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
                    bgBitmap = BitmapFactory.decodeFile(f.absolutePath, opts2)
                } catch (_: Exception) {
                }
            }
        }
        requestLayout()
        invalidate()
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

    private fun withOpacity(color: Int): Int {
        val a = prefs.keyOpacity.coerceIn(15, 100) * 255 / 100
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    private fun barHeight() = dp(44f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (barHeight() + dp(prefs.keyHeight.toFloat()) * rows().size + dp(12f)).toInt()
        setMeasuredDimension(w, h)
    }

    private fun displayLabel(key: Key): String {
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
            canvas.drawBitmap(bmp, src, Rect(0, 0, width, height), null)
            canvas.drawColor(Color.argb(prefs.bgDim.coerceIn(0, 90) * 255 / 100, 0, 0, 0))
        } else {
            canvas.drawColor(prefs.colorBg)
        }

        keyRects.clear()

        // ---------- Barre du haut : émoji, coller, suggestions, réglages ----------
        val barH = barHeight()
        val slot = dp(44f)
        val iconSize = sp(19f)

        fun topItem(key: Key, rect: RectF, textSizePx: Float) {
            keyRects.add(key to rect)
            if (key === pressedKey) {
                keyPaint.color = withOpacity(prefs.colorAccent)
                canvas.drawRoundRect(
                    RectF(rect.left + dp(3f), rect.top + dp(5f), rect.right - dp(3f), rect.bottom - dp(5f)),
                    dp(8f), dp(8f), keyPaint
                )
            }
            textPaint.color = if (key === pressedKey) prefs.colorTextOnAccent else prefs.colorText
            textPaint.textSize = textSizePx
            val ty = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
            val label = if (key.label.length > 14) key.label.take(13) + "…" else key.label
            canvas.drawText(label, rect.centerX(), ty, textPaint)
        }

        topItem(Key("😀", CODE_EMOJI), RectF(0f, 0f, slot, barH), iconSize)
        topItem(Key("📋", CODE_PASTE), RectF(slot, 0f, slot * 2, barH), iconSize)
        topItem(Key("⚙️", CODE_SETTINGS), RectF(width - slot, 0f, width.toFloat(), barH), iconSize)

        val sugLeft = slot * 2
        val sugRight = width - slot
        val sugW = (sugRight - sugLeft) / 3f
        linePaint.color = (0x30 shl 24) or (prefs.colorText and 0xFFFFFF)
        linePaint.strokeWidth = dp(1f)
        for (i in 0 until 3) {
            val r = RectF(sugLeft + sugW * i, 0f, sugLeft + sugW * (i + 1), barH)
            if (i < suggestions.size) {
                topItem(Key(suggestions[i], CODE_SUG - i), r, sp(15f))
            }
            if (i > 0) canvas.drawLine(r.left, dp(10f), r.left, barH - dp(10f), linePaint)
        }
        canvas.drawLine(sugLeft, dp(10f), sugLeft, barH - dp(10f), linePaint)
        canvas.drawLine(sugRight.toFloat(), dp(10f), sugRight.toFloat(), barH - dp(10f), linePaint)

        // ---------- Touches ----------
        val rowH = dp(prefs.keyHeight.toFloat())
        val margin = dp(3f)
        val sidePad = dp(4f)
        val radius = dp(9f)
        val usable = width - sidePad * 2
        var y = barH + dp(6f)

        val now = System.currentTimeMillis()
        val flashing = flashKey != null && now < flashUntil
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
                keyPaint.color = withOpacity(
                    when {
                        isAccent -> prefs.colorAccent
                        key.code < 0 -> prefs.colorSpecial
                        else -> prefs.colorKey
                    }
                )
                canvas.drawRoundRect(rect, radius, radius, keyPaint)

                textPaint.color = if (isAccent) prefs.colorTextOnAccent else prefs.colorText
                textPaint.textSize = if (key.label.length > 2)
                    sp(prefs.textSize * 0.62f) else sp(prefs.textSize.toFloat())
                val ty = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(displayLabel(key), rect.centerX(), ty, textPaint)

                x += kw
            }
            y += rowH
        }

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
                keyPaint.color = prefs.colorAccent
                canvas.drawRoundRect(bubble, dp(10f), dp(10f), keyPaint)
                textPaint.color = prefs.colorTextOnAccent
                textPaint.textSize = sp(prefs.textSize * 1.35f)
                val ty = bubble.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(displayLabel(pk), bubble.centerX(), ty, textPaint)
            }
        }

        if (flashing) {
            postInvalidateDelayed(flashUntil - now + 20)
        }
    }

    private fun keyAt(x: Float, y: Float): Key? =
        keyRects.firstOrNull { it.second.contains(x, y) }?.first

    private val repeatDelete = object : Runnable {
        override fun run() {
            longPressConsumed = true
            listener.onDelete()
            handler.postDelayed(this, 60)
        }
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
                pressedKey = key
                longPressConsumed = false
                feedback()
                if (key.code == CODE_DEL) {
                    handler.postDelayed(repeatDelete, 400)
                } else if (key.code >= 0 && !symbols &&
                    Layouts.accents(prefs.langIndex.coerceIn(0, 2)).containsKey(key.label)
                ) {
                    handler.postDelayed(accentRunnable, 380)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                val key = pressedKey
                pressedKey = null
                if (key != null) {
                    flashKey = key
                    flashUntil = System.currentTimeMillis() + 160
                }
                invalidate()
                if (key != null && !longPressConsumed &&
                    event.actionMasked == MotionEvent.ACTION_UP
                ) {
                    handleKey(key)
                }
            }
        }
        return true
    }

    private fun handleKey(key: Key) {
        when {
            key.code <= CODE_SUG -> listener.onSuggestion(key.label)
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
            audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.6f)
        }
    }
}
