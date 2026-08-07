package com.perso.clavier

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Pave de navigation et d'edition du texte. */
class NavPanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onAction: (String) -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(prefs.colorBg)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }
        header.addView(key("✕", "close", 1f, accent = true))
        header.addView(TextView(context).apply {
            text = "Navigation et édition"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(prefs.colorText)
            setPadding(dp(12), 0, 0, 0)
        })
        addView(header)

        addView(row(
            key("↶", "undo"), key("↷", "redo"),
            key("⤒", "top"), key("⤓", "bottom")
        ))
        addView(row(
            key("⇤", "home"), key("↑", "up"), key("⇥", "end"), key("⌫", "del")
        ))
        addView(row(
            key("←", "left"), key("↓", "down"), key("→", "right"), key("⌦", "forwardDel")
        ))
        addView(row(
            key("Tout", "selectAll"), key("Copier", "copy"),
            key("Couper", "cut"), key("Coller", "paste")
        ))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun row(vararg views: TextView): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(6), dp(3), dp(6), dp(3))
            views.forEach { addView(it) }
        }

    private fun key(label: String, action: String, weight: Float = 1f, accent: Boolean = false): TextView =
        TextView(context).apply {
            text = label
            textSize = if (label.length > 2) 14f else 20f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (accent) prefs.colorTextOnAccent else prefs.colorText)
            background = GradientDrawable().apply {
                setColor(if (accent) prefs.colorAccent else prefs.colorKey)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(6), dp(14), dp(6), dp(14))
            val lp = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            lp.setMargins(dp(4), dp(4), dp(4), dp(4))
            layoutParams = lp
            setOnClickListener { if (action == "close") onBack() else onAction(action) }
        }
}
