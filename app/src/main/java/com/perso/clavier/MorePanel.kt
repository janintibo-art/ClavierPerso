package com.perso.clavier

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Menu secondaire de la barre d'outils v36 : garde le clavier principal plus lisible. */
class MorePanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onAction: (String) -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(prefs.colorBg)
        setPadding(dp(12), dp(8), dp(12), dp(10))
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight)

        addView(TextView(context).apply {
            text = "Outils"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorText)
            setPadding(dp(6), dp(2), dp(6), dp(7))
        })

        val list = LinearLayout(context).apply { orientation = VERTICAL }
        val actions = listOf(
            Triple("GIF", "GIF et recherche", "gif"),
            Triple("↔", "Navigation / édition", "nav"),
            Triple("🌐", "Changer la langue", "lang"),
            Triple("⚙️", "Réglages", "settings")
        )

        actions.forEach { (icon, label, action) ->
            list.addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(prefs.colorKey)
                    cornerRadius = dp(12).toFloat()
                }
                setPadding(dp(12), dp(9), dp(12), dp(9))
                val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(6)
                layoutParams = lp

                addView(TextView(context).apply {
                    text = icon
                    textSize = 19f
                    gravity = Gravity.CENTER
                    setTextColor(prefs.colorText)
                    layoutParams = LayoutParams(dp(44), LayoutParams.WRAP_CONTENT)
                })
                addView(TextView(context).apply {
                    text = label
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(prefs.colorText)
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                })
                setOnClickListener { onAction(action) }
            })
        }

        addView(ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(list)
        }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        addView(TextView(context).apply {
            text = "✕  Retour au clavier"
            textSize = 15f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorTextOnAccent)
            background = GradientDrawable().apply {
                setColor(prefs.colorAccent)
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(14), dp(11), dp(14), dp(11))
            setOnClickListener { onBack() }
        })
    }
}
