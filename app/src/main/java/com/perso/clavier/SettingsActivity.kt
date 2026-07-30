package com.perso.clavier

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File

class SettingsActivity : Activity() {

    companion object {
        const val PICK_IMAGE = 42
    }

    private lateinit var prefs: Prefs
    private lateinit var themesContainer: LinearLayout
    private lateinit var colorsContainer: LinearLayout
    private lateinit var shortcutsContainer: LinearLayout
    private lateinit var preview: KeyboardView
    private lateinit var testField: EditText

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 14): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private val previewListener = object : KeyboardView.Listener {
        override fun onText(text: String) {
            testField.append(text)
        }

        override fun onDelete() {
            val t = testField.text
            if (t.isNotEmpty()) t.delete(t.length - 1, t.length)
        }

        override fun onEnter() {
            testField.append("\n")
        }

        override fun onEmojiToggle() {
            Toast.makeText(this@SettingsActivity, "Les emojis s'ouvrent dans le vrai clavier", Toast.LENGTH_SHORT).show()
        }

        override fun onPaste() {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                testField.append(clip.getItemAt(0).coerceToText(this@SettingsActivity))
            }
        }

        override fun onSettings() {}

        override fun onGifToggle() {
            Toast.makeText(this@SettingsActivity, "Les GIF s'ouvrent dans le vrai clavier", Toast.LENGTH_SHORT).show()
        }

        override fun onTranslateToggle() {
            Toast.makeText(this@SettingsActivity, "Le mode traduction s'active dans le vrai clavier", Toast.LENGTH_SHORT).show()
        }

        override fun onMoveCursor(delta: Int) {
            val pos = (testField.selectionStart + delta).coerceIn(0, testField.length())
            testField.setSelection(pos)
        }

        override fun onClipboardPanel() {
            Toast.makeText(this@SettingsActivity, "L'historique s'ouvre dans le vrai clavier (appui long sur 📋)", Toast.LENGTH_SHORT).show()
        }

        override fun onRewrite() {
            Toast.makeText(this@SettingsActivity, "La reformulation IA s'ouvre dans le vrai clavier (appui long sur ⏎)", Toast.LENGTH_SHORT).show()
        }

        override fun onLangSwitch() {
            prefs.langIndex = (prefs.langIndex + 1) % Layouts.languages.size
            preview.refresh()
            Toast.makeText(this@SettingsActivity, Layouts.languages[prefs.langIndex], Toast.LENGTH_SHORT).show()
        }

        override fun onSuggestion(word: String) {
            testField.append(word + " ")
        }
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
            text = "Anarchie Clavier"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#202124"))
        })
        root.addView(TextView(this).apply {
            text = "Suggestions, emojis, presse-papiers, 3 langues, et tout est personnalisable"
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

        // ----- Aperçu en direct -----
        root.addView(section("Aperçu en direct"))
        testField = EditText(this).apply {
            hint = "Tape sur l'aperçu ci-dessous, le texte s'écrit ici…"
            background = rounded(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(testField)

        preview = KeyboardView(this, previewListener)
        val previewCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#DADCE0"), 18)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
            addView(preview)
        }
        root.addView(previewCard)
        preview.suggestions = listOf("bonjour", "bonne", "bonsoir")

        // ----- Thèmes prédéfinis -----
        root.addView(section("Thèmes prédéfinis (${Themes.list.size})"))
        root.addView(hint("Un thème remplit les couleurs, que tu peux ensuite modifier une par une plus bas."))
        themesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(themesContainer)
        buildThemes()

        // ----- Couleurs personnalisées -----
        root.addView(section("Couleurs personnalisées"))
        colorsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(colorsContainer)
        buildColorRows()

        // ----- Image d'arrière-plan -----
        root.addView(section("Image d'arrière-plan"))
        root.addView(button("🖼️ Choisir une image") {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "Choisir une image"), PICK_IMAGE)
        })
        root.addView(button("Retirer l'image") {
            prefs.bgImageEnabled = false
            File(filesDir, KeyboardView.BG_FILE).delete()
            preview.refresh()
            Toast.makeText(this, "Image retirée", Toast.LENGTH_SHORT).show()
        })
        root.addView(hint("Assombrir l'image (pour la lisibilité)"))
        root.addView(seek(0, 90, prefs.bgDim) {
            prefs.bgDim = it
            preview.refresh()
        })
        root.addView(hint("Opacité des touches (baisse-la pour voir l'image à travers)"))
        root.addView(seek(15, 100, prefs.keyOpacity) {
            prefs.keyOpacity = it
            preview.refresh()
        })

        // ----- Langue -----
        root.addView(section("Langue du clavier"))
        val langRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Layouts.languages.forEachIndexed { index, name ->
            langRow.addView(TextView(this).apply {
                text = name
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = rounded(Color.parseColor(if (index == prefs.langIndex) "#4A6CF7" else "#9AA0A6"))
                setPadding(dp(8), dp(10), dp(8), dp(10))
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (index > 0) lp.leftMargin = dp(8)
                layoutParams = lp
                setOnClickListener {
                    prefs.langIndex = index
                    preview.refresh()
                    for (i in 0 until langRow.childCount) {
                        (langRow.getChildAt(i) as TextView).background =
                            rounded(Color.parseColor(if (i == index) "#4A6CF7" else "#9AA0A6"))
                    }
                }
            })
        }
        root.addView(langRow)
        root.addView(hint("Tu peux aussi changer de langue avec la touche du globe sur le clavier."))

        // ----- Options -----
        root.addView(section("Options"))
        root.addView(switchRow("Rangée de chiffres au-dessus des lettres", prefs.numberRow) {
            prefs.numberRow = it
            preview.refresh()
        })
        root.addView(switchRow("Suggestions de mots", prefs.suggestionsEnabled) {
            prefs.suggestionsEnabled = it
        })
        root.addView(switchRow("Bulle d'aperçu au-dessus de la touche", prefs.keyPopup) {
            prefs.keyPopup = it
            preview.refresh()
        })
        root.addView(switchRow("Vibration des touches", prefs.vibration) {
            prefs.vibration = it
            preview.refresh()
        })
        root.addView(switchRow("Son des touches", prefs.sound) {
            prefs.sound = it
            preview.refresh()
        })

        root.addView(section("Raccourcis texte"))
        root.addView(hint("Tape le raccourci puis espace : il est remplace par le texte complet. Exemple : slt -> Salut, ca va ?"))
        shortcutsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(shortcutsContainer)
        buildShortcuts()
        root.addView(button("+ Ajouter un raccourci") { addShortcutDialog() })

        root.addView(section("Astuces du clavier"))
        root.addView(hint("- Glisse ton doigt sur la barre Espace pour deplacer le curseur\n- Appui long sur 📋 : historique du presse-papiers (epingle tes favoris avec 📌)\n- Appui long sur ⏎ : reformulation IA du message (poli, pro, drole...)\n- Tape un calcul puis = : le resultat s'ecrit tout seul (ex : 12*45=540)"))

        root.addView(section("GIF"))
        root.addView(hint("La touche GIF du clavier utilise Tenor. Si la recherche ne fonctionne pas, colle ici une cle API Tenor gratuite (console.cloud.google.com, API Tenor)."))
        root.addView(EditText(this).apply {
            hint = "Cle API Tenor (optionnelle)"
            setText(prefs.tenorKey)
            background = rounded(Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            maxLines = 1
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(sq: Editable?) {
                    prefs.tenorKey = sq.toString().trim()
                }
                override fun beforeTextChanged(sq: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(sq: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        })

        root.addView(section("Hauteur des touches"))
        root.addView(seek(40, 68, prefs.keyHeight) {
            prefs.keyHeight = it
            preview.refresh()
        })

        root.addView(section("Taille du texte"))
        root.addView(seek(14, 26, prefs.textSize) {
            prefs.textSize = it
            preview.refresh()
        })

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    File(filesDir, KeyboardView.BG_FILE).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                prefs.bgImageEnabled = true
                preview.refresh()
                Toast.makeText(this, "Image appliquée 🖼️", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Construction de l'interface ----------

    private fun section(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#202124"))
        setPadding(0, dp(24), 0, dp(10))
    }

    private fun hint(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#5F6368"))
        setPadding(dp(4), dp(4), dp(4), dp(2))
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
        Themes.list.forEachIndexed { _, theme ->
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

            listOf(theme.key, theme.key, theme.accent, theme.special).forEach { c ->
                row.addView(View(this).apply {
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

            row.setOnClickListener {
                prefs.applyTheme(theme)
                buildColorRows()
                preview.refresh()
                Toast.makeText(this, "Thème « ${theme.name} » appliqué", Toast.LENGTH_SHORT).show()
            }

            themesContainer.addView(row)
        }
    }

    private fun buildColorRows() {
        colorsContainer.removeAllViews()
        colorsContainer.addView(colorRow("Fond du clavier", { prefs.colorBg }) { prefs.colorBg = it })
        colorsContainer.addView(colorRow("Touches", { prefs.colorKey }) { prefs.colorKey = it })
        colorsContainer.addView(colorRow("Touches spéciales", { prefs.colorSpecial }) { prefs.colorSpecial = it })
        colorsContainer.addView(colorRow("Accent (Entrée, Maj)", { prefs.colorAccent }) { prefs.colorAccent = it })
        colorsContainer.addView(colorRow("Texte des touches", { prefs.colorText }) { prefs.colorText = it })
        colorsContainer.addView(colorRow("Texte sur accent", { prefs.colorTextOnAccent }) { prefs.colorTextOnAccent = it })
    }

    private fun colorRow(label: String, get: () -> Int, set: (Int) -> Unit): LinearLayout {
        val swatch = View(this).apply {
            background = rounded(get(), 8)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(30))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.WHITE)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp

            addView(TextView(this@SettingsActivity).apply {
                text = label
                textSize = 15f
                setTextColor(Color.parseColor("#202124"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(swatch)

            setOnClickListener {
                colorPicker(label, get()) { picked ->
                    set(picked)
                    swatch.background = rounded(picked, 8)
                    preview.refresh()
                }
            }
        }
    }

    private fun buildShortcuts() {
        shortcutsContainer.removeAllViews()
        val map = prefs.shortcuts()
        if (map.isEmpty()) {
            shortcutsContainer.addView(hint("Aucun raccourci pour l'instant."))
            return
        }
        map.forEach { (key, value) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.WHITE)
                setPadding(dp(14), dp(10), dp(8), dp(10))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(6)
                layoutParams = lp
            }
            row.addView(TextView(this).apply {
                text = key
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#4A6CF7"))
            })
            row.addView(TextView(this).apply {
                text = "  ->  " + value
                textSize = 14f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.parseColor("#202124"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = "✕"
                textSize = 16f
                setTextColor(Color.parseColor("#B00020"))
                setPadding(dp(12), dp(4), dp(8), dp(4))
                setOnClickListener {
                    prefs.removeShortcut(key)
                    buildShortcuts()
                }
            })
            shortcutsContainer.addView(row)
        }
    }

    private fun addShortcutDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val keyField = EditText(this).apply { hint = "Raccourci (ex : slt)" }
        val valueField = EditText(this).apply { hint = "Texte complet (ex : Salut, ca va ?)" }
        layout.addView(keyField)
        layout.addView(valueField)
        AlertDialog.Builder(this)
            .setTitle("Nouveau raccourci")
            .setView(layout)
            .setPositiveButton("Ajouter") { _, _ ->
                val k = keyField.text.toString().trim()
                val v = valueField.text.toString().trim()
                if (k.isNotEmpty() && v.isNotEmpty() && !k.contains(" ")) {
                    prefs.putShortcut(k, v)
                    buildShortcuts()
                } else {
                    Toast.makeText(this, "Le raccourci ne doit pas contenir d'espace", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun colorPicker(title: String, current: Int, onPicked: (Int) -> Unit) {
        var r = Color.red(current)
        var g = Color.green(current)
        var b = Color.blue(current)
        var updating = false

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }

        val previewView = View(this).apply {
            background = rounded(current, 10)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
            )
        }
        layout.addView(previewView)

        val hex = EditText(this).apply {
            setText(String.format("#%02X%02X%02X", r, g, b))
        }

        lateinit var barR: SeekBar
        lateinit var barG: SeekBar
        lateinit var barB: SeekBar

        fun refreshPreview(fromHex: Boolean) {
            previewView.background = rounded(Color.rgb(r, g, b), 10)
            if (!fromHex) {
                updating = true
                hex.setText(String.format("#%02X%02X%02X", r, g, b))
                updating = false
            }
        }

        fun makeBar(label: String, init: Int, onCh: (Int) -> Unit): SeekBar {
            layout.addView(TextView(this).apply {
                text = label
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
            val bar = SeekBar(this).apply {
                max = 255
                progress = init
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) {
                            onCh(p)
                            refreshPreview(false)
                        }
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            layout.addView(bar)
            return bar
        }

        barR = makeBar("Rouge", r) { r = it }
        barG = makeBar("Vert", g) { g = it }
        barB = makeBar("Bleu", b) { b = it }

        layout.addView(TextView(this).apply {
            text = "Code hexadécimal"
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })
        hex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                try {
                    val c = Color.parseColor(s.toString().trim())
                    r = Color.red(c); g = Color.green(c); b = Color.blue(c)
                    barR.progress = r; barG.progress = g; barB.progress = b
                    refreshPreview(true)
                } catch (_: Exception) {
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, bb: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, bb: Int, c: Int) {}
        })
        layout.addView(hex)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("OK") { _, _ -> onPicked(Color.rgb(r, g, b)) }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
