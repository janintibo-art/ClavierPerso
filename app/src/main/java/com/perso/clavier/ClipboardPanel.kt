package com.perso.clavier

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ClipboardPanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onPaste: (String) -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

    private val list = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(10), dp(4), dp(10), dp(10))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(prefs.colorBg)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        header.addView(TextView(context).apply {
            text = "✕  Fermer"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorTextOnAccent)
            background = GradientDrawable().apply {
                setColor(prefs.colorAccent)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { onBack() }
        })
        header.addView(TextView(context).apply {
            text = "📋 Presse-papiers"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorText)
            setPadding(dp(14), 0, 0, 0)
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(context).apply {
            text = "🗑️ Vider"
            textSize = 14f
            setTextColor(prefs.colorText)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                prefs.clearUnpinnedClips()
                rebuild()
            }
        })
        addView(header)

        val scroll = ScrollView(context)
        scroll.addView(list)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        rebuild()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rebuild() {
        list.removeAllViews()
        val clips = prefs.clips()
        if (clips.isEmpty()) {
            list.addView(TextView(context).apply {
                text = "Rien pour l'instant.\nTout ce que tu copieras apparaîtra ici.\nAppuie sur 📌 pour épingler tes favoris (email, adresse, IBAN…)."
                textSize = 14f
                setTextColor(prefs.colorText)
                setPadding(dp(10), dp(20), dp(10), dp(10))
            })
            return
        }
        // Épinglés d'abord
        val sorted = clips.filter { it.second } + clips.filter { !it.second }
        sorted.forEach { (text, pinned) ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(prefs.colorKey)
                    cornerRadius = dp(10).toFloat()
                }
                setPadding(dp(14), dp(10), dp(8), dp(10))
                val lp = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(6)
                layoutParams = lp
            }
            row.addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(prefs.colorText)
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { onPaste(text) }
            })
            row.addView(TextView(context).apply {
                this.text = "📌"
                textSize = 17f
                alpha = if (pinned) 1f else 0.35f
                setPadding(dp(10), dp(4), dp(6), dp(4))
                setOnClickListener {
                    prefs.togglePinClip(text)
                    rebuild()
                }
            })
            list.addView(row)
        }
    }
}
