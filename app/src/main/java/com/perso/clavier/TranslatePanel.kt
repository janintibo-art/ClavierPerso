package com.perso.clavier

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class TranslatePanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onPick: (String, String) -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

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
            text = "ABC"
            textSize = 15f
            setTextColor(prefs.colorText)
            setBackgroundColor(prefs.colorSpecial)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { onBack() }
        })
        header.addView(TextView(context).apply {
            text = "🌍 Traduire vers…"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorText)
            setPadding(dp(14), 0, 0, 0)
        })
        addView(header)

        val list = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(10))
        }
        Translator.languages.forEach { (code, name) ->
            list.addView(TextView(context).apply {
                text = name
                textSize = 16f
                setTextColor(prefs.colorText)
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(prefs.colorKey)
                    cornerRadius = dp(10).toFloat()
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
                val lp = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(6)
                layoutParams = lp
                setOnClickListener { onPick(code, name) }
            })
        }
        val scroll = ScrollView(context)
        scroll.addView(list)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
