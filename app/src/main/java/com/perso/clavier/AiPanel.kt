package com.perso.clavier

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AiPanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onPick: (AiModes.Mode) -> Unit,
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
            text = "✕"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorTextOnAccent)
            background = GradientDrawable().apply {
                setColor(prefs.colorAccent)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(15), dp(12), dp(15), dp(12))
            setOnClickListener { onBack() }
        })
        header.addView(TextView(context).apply {
            text = "🤖 Assistant IA"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorText)
            setPadding(dp(14), 0, 0, 0)
        })
        addView(header)

        addView(TextView(context).apply {
            text = "Choisis un mode, écris ta demande, puis appuie sur ➜ : " +
                    "le résultat remplace ta demande."
            textSize = 12f
            setTextColor(prefs.colorText)
            alpha = 0.75f
            setPadding(dp(14), 0, dp(14), dp(6))
        })

        val list = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10), dp(2), dp(10), dp(10))
        }
        AiModes.list.forEach { mode ->
            list.addView(LinearLayout(context).apply {
                orientation = VERTICAL
                background = GradientDrawable().apply {
                    setColor(prefs.colorKey)
                    cornerRadius = dp(10).toFloat()
                }
                setPadding(dp(16), dp(11), dp(16), dp(11))
                val lp = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(6)
                layoutParams = lp
                addView(TextView(context).apply {
                    text = mode.label
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(prefs.colorText)
                })
                addView(TextView(context).apply {
                    text = mode.hint
                    textSize = 12f
                    setTextColor(prefs.colorText)
                    alpha = 0.7f
                })
                setOnClickListener { onPick(mode) }
            })
        }
        val scroll = ScrollView(context)
        scroll.addView(list)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
