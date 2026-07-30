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
    }

    companion object {
        const val CODE_SHIFT = -1
        const val CODE_DEL = -2
        const val CODE_SYM = -3
        const val CODE_SPACE = -4
        const val CODE_ENTER = -5
        const val BG_FILE = "bg_image"
    }

    class Key(val label: String, val code: Int, val weight: Float = 1f)

    private val accents = mapOf(
        "e" to "é", "a" to "à", "u" to "ù", "i" to "î",
        "o" to "ô", "c" to "ç", "n" to "ñ", "'" to "\""
    )

    private val lettersRows: List<List<Key>> = listOf(
        listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p").map { Key(it, it[0].code) },
        listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m").map { Key(it, it[0].code) },
        listOf(Key("⇧", CODE_SHIFT, 1.4f)) +
                listOf("w", "x", "c", "v", "b", "n", "'").map { Key(it, it[0].code) } +
                listOf(Key("⌫", CODE_DEL, 1.4f)),
        listOf(
            Key("?123", CODE_SYM, 1.6f),
            Key(",", ','.code),
            Key("Espace", CODE_SPACE, 4.2f),
            Key(".", '.'.code),
            Key("⏎", CODE_ENTER, 1.6f)
        )
    )

    private val symbolsRows: List<List<Key>> = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { Key(it, it[0].code) },
        listOf("@", "#", "€", "_", "&", "-", "+", "(", ")", "/").map { Key(it, it[0].code) },
        listOf(Key("=", '='.code, 1.4f)) +
                listOf("*", "\"", "'", ":", ";", "!", "?").map { Key(it, it[0].code) } +
                listOf(Key("⌫", CODE_DEL, 1.4f)),
        listOf(
            Key("ABC", CODE_SYM, 1.6f),
            Key(",", ','.code),
            Key("Espace", CODE_SPACE, 4.2f),
            Key(".", '.'.code),
            Key("⏎", CODE_ENTER, 1.6f)
        )
    )

    private var shift = true
    private var caps = false
    private var symbols = false
    private var lastShiftTap = 0L

    private var prefs = Prefs(context)
    private var bgBitmap: Bitmap? = null

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private var pressedKey: Key? = null
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

    private fun rows() = if (symbols) symbolsRows else lettersRows

    private fun withOpacity(color: Int): Int {
        val a = prefs.keyOpacity.coerceIn(15, 100) * 255 / 100
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (dp(prefs.keyHeight.toFloat()) * rows().size + dp(14f)).toInt()
        setMeasuredDimension(w, h)
    }

    private fun displayLabel(key: Key): String {
        if (key.code == CODE_SHIFT) return if (caps) "⇪" else "⇧"
        if (key.code >= 0 && !symbols && (shift || caps)) return key.label.uppercase()
        return key.label
    }

    override fun onDraw(canvas: Canvas) {
        // Arrière-plan : image ou couleur unie
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

        val rowH = dp(prefs.keyHeight.toFloat())
        val margin = dp(3f)
        val sidePad = dp(4f)
        val radius = dp(9f)
        val usable = width - sidePad * 2
        var y = dp(7f)

        for (row in rows()) {
            val totalW = row.map { it.weight }.sum()
            var x = sidePad
            for (key in row) {
                val kw = usable * key.weight / totalW
                val rect = RectF(x + margin, y + margin, x + kw - margin, y + rowH - margin)
                keyRects.add(key to rect)

                val isAccent = key === pressedKey ||
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
                    sp(prefs.textSize * 0.7f) else sp(prefs.textSize.toFloat())
                val ty = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(displayLabel(key), rect.centerX(), ty, textPaint)

                x += kw
            }
            y += rowH
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
        val acc = accents[key.label] ?: return@Runnable
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
                } else if (key.code >= 0 && !symbols && accents.containsKey(key.label)) {
                    handler.postDelayed(accentRunnable, 380)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                val key = pressedKey
                pressedKey = null
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
        when (key.code) {
            CODE_SHIFT -> {
                val now = System.currentTimeMillis()
                when {
                    caps -> { caps = false; shift = false }
                    now - lastShiftTap < 300 -> { caps = true; shift = false }
                    else -> shift = !shift
                }
                lastShiftTap = now
            }
            CODE_DEL -> listener.onDelete()
            CODE_SYM -> symbols = !symbols
            CODE_SPACE -> listener.onText(" ")
            CODE_ENTER -> listener.onEnter()
            else -> {
                var t = key.label
                if (!symbols && (shift || caps)) t = t.uppercase()
                listener.onText(t)
                if (shift && !caps) shift = false
            }
        }
        invalidate()
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
