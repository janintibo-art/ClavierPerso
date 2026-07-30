package com.perso.clavier

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var themesContainer: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 14): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(32))
            setBackgroundColor(Color.parseColor("#F4F5F7"))
        }

        root.addView(TextView(this).apply {
            text = "⌨️ Clavier Perso"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
        })
        root.addView(TextView(this).apply {
            text = "1. Active le clavier  •  2. Choisis-le par défaut  •  3. Personnalise-le !"
            textSize = 14f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, dp(6), 0, dp(16))
        })

        root.addView(button("① Activer le clavier") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("② Choisir comme clavier par défaut") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        })

        root.addView(section("Zone de test"))
        root.addView(EditText(this).apply {
            hint = "Tape ici pour tester le clavier…"
            background = rounded(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        })

        root.addView(section("Thèmes (${Themes.list.size})"))
        themesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(themesContainer)
        buildThemes()

        root.addView(section("Options"))
        root.addView(switchRow("Vibration des touches", prefs.vibration) { prefs.vibration = it })
        root.addView(switchRow("Son des touches", prefs.sound) { prefs.sound = it })

        root.addView(section("Hauteur des touches"))
        root.addView(seek(40, 68, prefs.keyHeight) { prefs.keyHeight = it })

        root.addView(section("Taille du texte"))
        root.addView(seek(14, 26, prefs.textSize) { prefs.textSize = it })

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun section(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#202124"))
        setPadding(0, dp(24), 0, dp(10))
    }

    private fun button(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        background = rounded(Color.parseColor("#4A6CF7"))
        setPadding(dp(16), dp(14), dp(16), dp(14))
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(10)
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    private fun switchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.WHITE)
            setPadding(dp(16), dp(8), dp(12), dp(8))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp

            addView(TextView(this@SettingsActivity).apply {
                this.text = text
                textSize = 15f
                setTextColor(Color.parseColor("#202124"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Switch(this@SettingsActivity).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, v -> onChange(v) }
            })
        }

    private fun seek(min: Int, max: Int, value: Int, onChange: (Int) -> Unit): SeekBar =
        SeekBar(this).apply {
            this.max = max - min
            progress = (value - min).coerceIn(0, max - min)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onChange(p + min)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

    private fun buildThemes() {
        themesContainer.removeAllViews()
        val selected = prefs.themeIndex
        Themes.list.forEachIndexed { index, theme ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(theme.bg)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }

            // Petites touches de prévisualisation
            val colors = listOf(theme.key, theme.key, theme.accent, theme.special)
            colors.forEach { c ->
                row.addView(android.view.View(this).apply {
                    background = rounded(c, 6)
                    val lp = LinearLayout.LayoutParams(dp(26), dp(26))
                    lp.rightMargin = dp(6)
                    layoutParams = lp
                })
            }

            row.addView(TextView(this).apply {
                text = theme.name
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(theme.text)
                setPadding(dp(8), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            if (index == selected) {
                row.addView(TextView(this).apply {
                    text = "✓"
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(theme.accent)
                })
            }

            row.setOnClickListener {
                prefs.themeIndex = index
                buildThemes()
                Toast.makeText(this, "Thème « ${theme.name} » appliqué", Toast.LENGTH_SHORT).show()
            }

            themesContainer.addView(row)
        }
    }
}
