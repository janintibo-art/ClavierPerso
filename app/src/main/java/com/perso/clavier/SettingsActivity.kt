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
        const val PICK_SHORTCUTS = 43
        const val PICK_TEXT_FILE = 44
        const val REQ_CONTACTS = 51
        const val REQ_SMS = 52
        const val REQ_SMS_CODE = 53
        const val PICK_CHAT = 45
        const val PICK_BACKUP = 46
    }

    private lateinit var prefs: Prefs
    private lateinit var themesContainer: LinearLayout
    private lateinit var colorsContainer: LinearLayout
    private lateinit var shortcutsContainer: LinearLayout
    private var keyEditMode = false
    private var memoryRefresher: (() -> Unit)? = null
    private lateinit var appThemesInfo: TextView
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

        override fun onFixSpelling() {
            Toast.makeText(this@SettingsActivity, "La correction IA fonctionne dans le vrai clavier", Toast.LENGTH_SHORT).show()
        }

        override fun onAiToggle() {
            Toast.makeText(this@SettingsActivity, "L'assistant IA s'utilise dans le vrai clavier (🤖)", Toast.LENGTH_SHORT).show()
        }

        override fun onNavPanel() {
            Toast.makeText(this@SettingsActivity, "Le pavé de navigation s'ouvre dans le vrai clavier (appui long sur ?123)", Toast.LENGTH_SHORT).show()
        }

        override fun onAiFollowUp(instruction: String) {
            Toast.makeText(this@SettingsActivity, "Disponible dans le vrai clavier", Toast.LENGTH_SHORT).show()
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
        preview.suggestions = listOf("corection", "correction", "corrections")
        preview.highlightIndex = 1

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
        root.addView(hint("Assombrir l'image (voile noir)"))
        root.addView(seek(0, 90, prefs.bgDim) {
            prefs.bgDim = it
            preview.refresh()
        })
        root.addView(hint("Luminosité de l'image"))
        root.addView(seek(30, 200, prefs.bgBrightness) {
            prefs.bgBrightness = it
            preview.refresh()
        })
        root.addView(hint("Saturation (0 = noir et blanc)"))
        root.addView(seek(0, 100, prefs.bgSaturation) {
            prefs.bgSaturation = it
            preview.refresh()
        })
        root.addView(hint("Flou de l'image"))
        root.addView(seek(0, 100, prefs.bgBlur) {
            prefs.bgBlur = it
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
        root.addView(switchRow("Correction automatique (sur espace)", prefs.autoCorrect) {
            prefs.autoCorrect = it
        })
        root.addView(hint("Un retour arrière juste après annule la correction et remet ton mot."))
        root.addView(switchRow("Majuscule automatique en début de phrase", prefs.autoCapitalize) {
            prefs.autoCapitalize = it
        })
        root.addView(switchRow("Double espace = point", prefs.doubleSpacePeriod) {
            prefs.doubleSpacePeriod = it
        })
        root.addView(switchRow("Clavier intelligent (apprend tes mots)", prefs.learningEnabled) {
            prefs.learningEnabled = it
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

        // ----- Sensibilite -----
        root.addView(section("Sensibilité du clavier ⚡"))
        root.addView(switchRow("Frappe instantanée (dès le contact du doigt)", prefs.instantKey) {
            prefs.instantKey = it
            preview.refresh()
        })
        root.addView(hint("Réactivité : à gauche = très rapide (appuis longs plus courts), à droite = plus tolérant"))
        root.addView(seek(30, 200, prefs.sensitivity) {
            prefs.sensitivity = it
            preview.refresh()
        })
        root.addView(hint("Tolérance de zone : rattrape les appuis qui tombent entre deux touches"))
        root.addView(seek(0, 16, prefs.touchMargin) {
            prefs.touchMargin = it
            preview.refresh()
        })

        // ----- Effet de frappe -----
        root.addView(section("Effet à la frappe ✨"))
        root.addView(hint("Teste directement sur l'aperçu ci-dessus !"))
        val effects = listOf("Aucun", "Couleur", "Onde", "Zoom", "Éclat", "Étincelles")
        val effectRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        effects.forEachIndexed { index, name ->
            effectRow.addView(TextView(this).apply {
                text = name
                textSize = 11f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = rounded(Color.parseColor(if (index == prefs.pressEffect) "#4A6CF7" else "#9AA0A6"), 10)
                setPadding(dp(2), dp(10), dp(2), dp(10))
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (index > 0) lp.leftMargin = dp(4)
                layoutParams = lp
                setOnClickListener {
                    prefs.pressEffect = index
                    preview.refresh()
                    for (i in 0 until effectRow.childCount) {
                        (effectRow.getChildAt(i) as TextView).background =
                            rounded(Color.parseColor(if (i == index) "#4A6CF7" else "#9AA0A6"), 10)
                    }
                }
            })
        }
        root.addView(effectRow)
        root.addView(hint("Durée de l'effet"))
        root.addView(seek(80, 900, prefs.pressEffectDuration) {
            prefs.pressEffectDuration = it
            preview.refresh()
        })
        val effectColorRow = colorRow(
            "Couleur de l'effet",
            { if (prefs.pressEffectColor == 0) prefs.colorAccent else prefs.pressEffectColor }
        ) { prefs.pressEffectColor = it }
        root.addView(effectColorRow)

        // ----- Luminosite -----
        root.addView(section("Luminosité des touches"))
        root.addView(hint("Assombris ou illumine toutes les touches d'un coup."))
        root.addView(seek(30, 200, prefs.brightness) {
            prefs.brightness = it
            preview.refresh()
        })

        // ----- Mode RGB -----
        root.addView(section("Mode RGB 🌈"))
        root.addView(hint("Comme un vrai clavier gamer : les touches changent de couleur en continu."))
        val rgbModes = listOf("Éteint", "Vague", "Respiration", "Réactif", "Cascade")
        val rgbRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rgbModes.forEachIndexed { index, name ->
            rgbRow.addView(TextView(this).apply {
                text = name
                textSize = 12f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = rounded(Color.parseColor(if (index == prefs.rgbMode) "#4A6CF7" else "#9AA0A6"), 10)
                setPadding(dp(4), dp(10), dp(4), dp(10))
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (index > 0) lp.leftMargin = dp(5)
                layoutParams = lp
                setOnClickListener {
                    prefs.rgbMode = index
                    preview.refresh()
                    for (i in 0 until rgbRow.childCount) {
                        (rgbRow.getChildAt(i) as TextView).background =
                            rounded(Color.parseColor(if (i == index) "#4A6CF7" else "#9AA0A6"), 10)
                    }
                }
            })
        }
        root.addView(rgbRow)
        root.addView(hint("Vitesse de l'animation"))
        root.addView(seek(10, 200, prefs.rgbSpeed) {
            prefs.rgbSpeed = it
            preview.refresh()
        })
        root.addView(hint("Intensité des couleurs (0 = couleurs du thème, 100 = arc-en-ciel)"))
        root.addView(seek(0, 100, prefs.rgbIntensity) {
            prefs.rgbIntensity = it
            preview.refresh()
        })
        root.addView(switchRow("Le texte suit aussi les couleurs RGB", prefs.rgbText) {
            prefs.rgbText = it
            preview.refresh()
        })

        // ----- Couleur touche par touche -----
        root.addView(section("Couleur touche par touche"))
        root.addView(hint("Active le mode édition, puis appuie sur une touche de l'aperçu pour lui donner sa propre couleur et sa luminosité."))
        val editBtn = button("✏️ Activer le mode édition") { }
        editBtn.setOnClickListener {
            keyEditMode = !keyEditMode
            if (keyEditMode) {
                preview.editModeListener = { label -> editKeyDialog(label) }
                editBtn.text = "✅ Mode édition actif — appuie sur une touche"
                editBtn.background = rounded(Color.parseColor("#0F9D58"))
            } else {
                preview.editModeListener = null
                editBtn.text = "✏️ Activer le mode édition"
                editBtn.background = rounded(Color.parseColor("#4A6CF7"))
            }
        }
        root.addView(editBtn)
        root.addView(button("♻️ Réinitialiser toutes les touches") {
            prefs.clearKeyColors()
            preview.refresh()
            Toast.makeText(this, "Couleurs des touches réinitialisées", Toast.LENGTH_SHORT).show()
        })

        // ----- Memoire du clavier -----
        root.addView(section("Mémoire du clavier 🧠"))
        val memoryInfo = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(dp(4), dp(4), dp(4), dp(2))
        }
        fun refreshMemory() {
            val counts = prefs.wordCounts()
            val top = counts.entries.sortedByDescending { it.value }.take(8)
                .joinToString(", ") { it.key + " (" + it.value + ")" }
            memoryInfo.text = if (counts.isEmpty())
                "Le clavier n'a encore rien appris. Utilise un bouton ci-dessous pour lui donner ton vocabulaire d'un coup."
            else
                counts.size.toString() + " mots appris, " + prefs.bigramCount() +
                        " enchaînements.\nTes plus utilisés : " + top
        }
        refreshMemory()
        memoryRefresher = { refreshMemory() }
        root.addView(memoryInfo)
        root.addView(hint("Plutôt que d'attendre des semaines, donne-lui directement ton vocabulaire :"))
        root.addView(button("💬 Apprendre de mes SMS envoyés") { importSms() })
        root.addView(button("👥 Apprendre les noms de mes contacts") { importContacts() })
        root.addView(button("📱 Importer une conversation exportée") { importChatFile() })
        root.addView(button("📝 Coller un texte que j'ai écrit") { importTextDialog() })
        root.addView(button("📖 Importer le dictionnaire Android") { importUserDict() })
        root.addView(button("🗑️ Oublier tous les mots appris") {
            prefs.forgetLearnedWords()
            refreshMemory()
            Toast.makeText(this, "Mémoire effacée", Toast.LENGTH_SHORT).show()
        })

        // ----- Police -----
        root.addView(section("Police des touches"))
        root.addView(choiceRow(Fonts.names, prefs.fontIndex, 11f) {
            prefs.fontIndex = it
            preview.refresh()
        })

        // ----- Sons -----
        root.addView(section("Son de frappe 🔊"))
        root.addView(hint("Active « Son des touches » dans Options, puis choisis un son :"))
        root.addView(choiceRow(KeySounds.names, prefs.soundType, 10f) {
            prefs.soundType = it
            prefs.sound = true
            preview.refresh()
        })
        root.addView(hint("Volume du son"))
        root.addView(seek(10, 100, prefs.soundVolume) {
            prefs.soundVolume = it
            preview.refresh()
        })

        // ----- Theme par application -----
        root.addView(section("Thème par application"))
        root.addView(hint("Associe un thème à une application : le clavier changera d'apparence automatiquement."))
        root.addView(button("➕ Associer une application à un thème") { appThemeDialog() })
        appThemesInfo = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(dp(4), dp(6), dp(4), dp(2))
        }
        root.addView(appThemesInfo)
        refreshAppThemes()

        // ----- Ecriture assistee -----
        root.addView(section("Écriture assistée"))
        root.addView(switchRow("Suggestions d'emojis pendant la frappe", prefs.emojiSuggestions) {
            prefs.emojiSuggestions = it
        })
        root.addView(switchRow("Détecter les codes reçus par SMS", prefs.smsCodeDetection) {
            prefs.smsCodeDetection = it
            if (it && checkSelfPermission(android.Manifest.permission.READ_SMS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) requestPermissions(arrayOf(android.Manifest.permission.READ_SMS), REQ_SMS_CODE)
        })
        root.addView(switchRow("Changer de langue automatiquement", prefs.autoLanguage) {
            prefs.autoLanguage = it
        })
        root.addView(switchRow("Mode privé dans les champs sensibles", prefs.incognitoFields) {
            prefs.incognitoFields = it
        })
        root.addView(hint("Mode privé : aucun mot appris ni suggéré dans les champs mot de passe et numéros."))
        root.addView(hint("Appui long sur ?123 : pavé de navigation (flèches, annuler, copier, coller).\nAppui long sur 🤖 : l'IA complète ta phrase."))

        // ----- Sauvegarde -----
        root.addView(section("Sauvegarde et restauration 💾"))
        root.addView(hint(Backup.summary(this)))
        root.addView(button("💾 Sauvegarder mes réglages") { exportBackup() })
        root.addView(button("♻️ Restaurer une sauvegarde") {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
            startActivityForResult(Intent.createChooser(intent, "Choisir la sauvegarde"), PICK_BACKUP)
        })

        root.addView(section("Raccourcis texte"))
        root.addView(hint("Tape le raccourci puis espace : il est remplace par le texte complet. Exemple : slt -> Salut, ca va ?"))
        shortcutsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(shortcutsContainer)
        buildShortcuts()
        root.addView(button("+ Ajouter un raccourci") { addShortcutDialog() })
        root.addView(button("📥 Importer mes raccourcis Samsung") { importDialog() })
        root.addView(button("📤 Exporter mes raccourcis") { exportDialog() })

        root.addView(section("Astuces du clavier"))
        root.addView(hint("- Glisse ton doigt sur la barre Espace pour deplacer le curseur\n- Appui long sur 📋 : historique du presse-papiers (epingle tes favoris avec 📌)\n- Appui long sur ⏎ : reformulation IA du message (poli, pro, drole...)\n- Tape un calcul puis = : le resultat s'ecrit tout seul (ex : 12*45=540)"))

        // ----- Cles de services -----
        root.addView(section("Clés IA et services 🔑"))
        root.addView(hint(
            "Une clé IA améliore énormément la traduction, la correction (✅) et la reformulation. " +
                    "Compatible OpenAI, Groq, Mistral, DeepSeek, OpenRouter…"
        ))
        root.addView(hint(
            "Astuce : une clé qui commence par gsk_ vient de Groq, xai- vient de xAI Grok, " +
                    "sk- de OpenAI. Utilise le bouton ci-dessous pour remplir URL et modèle."
        ))
        root.addView(keyField("Clé IA", prefs.aiKey) { prefs.aiKey = it })
        root.addView(keyField("URL de l'API IA", prefs.aiBaseUrl) { prefs.aiBaseUrl = it })
        root.addView(keyField("Modèle IA", prefs.aiModel) { prefs.aiModel = it })
        root.addView(hint(
            "Assistant 🤖 : touche 🤖 → choisis un mode (recherche Google, mot d'excuse, " +
                    "email pro, résumé, idées…), écris ta demande, appuie sur ➜. " +
                    "Le résultat remplace ta demande, prêt à envoyer. ⌫ annule."
        ))
        root.addView(button("⚡ Remplir automatiquement (Groq, xAI…)") { aiPresetDialog() })
        root.addView(button("🧪 Tester la clé IA") {
            Thread {
                val r = AiClient.test(Prefs(this))
                runOnUiThread { alert("Test IA", r) }
            }.start()
        })

        root.addView(hint(
            "⚠️ Aucune clé de traduction n'est nécessaire : si ta clé IA fonctionne, " +
                    "la traduction marche déjà. Les deux champs ci-dessous sont facultatifs " +
                    "(DeepL est gratuit jusqu'à 500 000 caractères par mois sur deepl.com/pro-api)."
        ))
        root.addView(keyField("Clé DeepL (optionnelle)", prefs.deeplKey) { prefs.deeplKey = it })
        root.addView(keyField("Clé Google Traduction (optionnelle)", prefs.googleTranslateKey) {
            prefs.googleTranslateKey = it
        })
        root.addView(button("🧪 Tester la traduction") {
            Thread {
                val r = Translator.test(Prefs(this))
                runOnUiThread { alert("Test traduction", r) }
            }.start()
        })

        root.addView(section("GIF"))
        root.addView(hint(
            "L'API Tenor a été fermée par Google le 30 juin 2026. Choisis un fournisseur " +
                    "et colle une clé gratuite : Giphy (developers.giphy.com) ou Klipy (klipy.com/developers)."
        ))
        val gifProviders = listOf("Giphy", "Klipy")
        val gifRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        gifProviders.forEachIndexed { index, name ->
            gifRow.addView(TextView(this).apply {
                text = name
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = rounded(Color.parseColor(if (index == prefs.gifProvider) "#4A6CF7" else "#9AA0A6"))
                setPadding(dp(8), dp(10), dp(8), dp(10))
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (index > 0) lp.leftMargin = dp(8)
                layoutParams = lp
                setOnClickListener {
                    prefs.gifProvider = index
                    for (i in 0 until gifRow.childCount) {
                        (gifRow.getChildAt(i) as TextView).background =
                            rounded(Color.parseColor(if (i == index) "#4A6CF7" else "#9AA0A6"))
                    }
                }
            })
        }
        root.addView(gifRow)
        root.addView(keyField("Clé GIF", prefs.gifKey) { prefs.gifKey = it })
        root.addView(button("🧪 Tester les GIF") {
            Thread {
                val r = GifProvider.test(Prefs(this))
                runOnUiThread { alert("Test GIF", r) }
            }.start()
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
        if (requestCode == PICK_BACKUP && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                applyBackup(text)
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible de lire le fichier", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (requestCode == PICK_CHAT && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                handleChatText(text)
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible de lire le fichier", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (requestCode == PICK_TEXT_FILE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                handleChatText(text)
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible de lire le fichier", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (requestCode == PICK_SHORTCUTS && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                applyImport(ShortcutImporter.parse(text))
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible de lire le fichier", Toast.LENGTH_SHORT).show()
            }
            return
        }
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

    /** Rangée de boutons de choix exclusifs. */
    private fun choiceRow(
        labels: List<String>, selected: Int, textSize: Float, onPick: (Int) -> Unit
    ): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val perRow = if (labels.size > 4) 4 else labels.size
        val buttons = ArrayList<TextView>()
        labels.chunked(perRow).forEachIndexed { rowIdx, chunk ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEachIndexed { i, name ->
                val index = rowIdx * perRow + i
                val tv = TextView(this).apply {
                    text = name
                    this.textSize = textSize
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = rounded(
                        Color.parseColor(if (index == selected) "#4A6CF7" else "#9AA0A6"), 10
                    )
                    setPadding(dp(3), dp(10), dp(3), dp(10))
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    lp.setMargins(dp(3), dp(3), dp(3), dp(3))
                    layoutParams = lp
                    setOnClickListener {
                        onPick(index)
                        buttons.forEachIndexed { j, b ->
                            b.background = rounded(
                                Color.parseColor(if (j == index) "#4A6CF7" else "#9AA0A6"), 10
                            )
                        }
                    }
                }
                buttons.add(tv)
                row.addView(tv)
            }
            repeat(perRow - chunk.size) {
                row.addView(TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                })
            }
            outer.addView(row)
        }
        return outer
    }

    private fun refreshAppThemes() {
        val map = prefs.appThemes()
        appThemesInfo.text = if (map.isEmpty()) "Aucune association pour l'instant."
        else map.entries.joinToString("\n") { (pkg, idx) ->
            appLabel(pkg) + "  →  " + Themes.get(idx).name + "   (touche pour retirer)"
        }
        appThemesInfo.setOnClickListener {
            if (map.isEmpty()) return@setOnClickListener
            val entries = map.entries.toList()
            val labels = entries.map { appLabel(it.key) + " → " + Themes.get(it.value).name }
            AlertDialog.Builder(this)
                .setTitle("Retirer une association")
                .setItems(labels.toTypedArray()) { _, which ->
                    prefs.setAppTheme(entries[which].key, null)
                    refreshAppThemes()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun appThemeDialog() {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .sortedBy { appLabel(it).lowercase() }
        if (apps.isEmpty()) {
            Toast.makeText(this, "Aucune application trouvée", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = apps.map { appLabel(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choisis une application")
            .setItems(labels) { _, which ->
                val pkg = apps[which]
                val themeNames = Themes.list.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Thème pour " + labels[which])
                    .setItems(themeNames) { _, t ->
                        prefs.setAppTheme(pkg, t)
                        refreshAppThemes()
                        Toast.makeText(this, labels[which] + " → " + themeNames[t], Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
            .show()
    }

    private fun exportBackup() {
        val text = Backup.export(this)
        val field = EditText(this).apply {
            setText(text.take(4000))
            minLines = 4
            textSize = 10f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Sauvegarde")
            .setMessage("Copie ce texte et garde-le en lieu sûr (note, mail, fichier).")
            .setView(ScrollView(this).apply { addView(field) })
            .setPositiveButton("Copier tout") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Sauvegarde clavier", text))
                Toast.makeText(this, "Sauvegarde copiée ✅", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Coller une sauvegarde") { _, _ -> importBackupDialog() }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun importBackupDialog() {
        val field = EditText(this).apply {
            hint = "Colle ici le texte de ta sauvegarde…"
            minLines = 5
            gravity = Gravity.TOP
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Restaurer")
            .setView(ScrollView(this).apply { addView(field) })
            .setPositiveButton("Restaurer") { _, _ -> applyBackup(field.text.toString()) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun applyBackup(text: String) {
        val n = Backup.import(this, text)
        if (n < 0) {
            Toast.makeText(this, "Sauvegarde illisible", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "✅ " + n + " réglages restaurés. Rouvre les réglages.", Toast.LENGTH_LONG).show()
        recreate()
    }

    /** Remplit l'URL et le modèle selon le fournisseur choisi. */
    private fun aiPresetDialog() {
        val presets = listOf(
            Triple("Groq (gratuit, très rapide)", "https://api.groq.com/openai/v1", "openai/gpt-oss-20b"),
            Triple("Groq (qualité supérieure)", "https://api.groq.com/openai/v1", "openai/gpt-oss-120b"),
            Triple("xAI Grok", "https://api.x.ai/v1", "grok-4.5"),
            Triple("xAI Grok (rapide, moins cher)", "https://api.x.ai/v1", "grok-4.1-fast"),
            Triple("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
            Triple("Mistral", "https://api.mistral.ai/v1", "mistral-small-latest"),
            Triple("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
            Triple("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini")
        )
        val labels = presets.map { it.first + "\n" + it.third }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Ton fournisseur d'IA")
            .setItems(labels) { _, which ->
                val (name, url, model) = presets[which]
                prefs.aiBaseUrl = url
                prefs.aiModel = model
                Toast.makeText(this, name + " configuré. Colle ta clé puis teste.", Toast.LENGTH_LONG).show()
                recreate()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun alert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun keyField(label: String, value: String, onChange: (String) -> Unit): EditText =
        EditText(this).apply {
            hint = label
            setText(value)
            textSize = 14f
            maxLines = 1
            background = rounded(Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(sq: Editable?) { onChange(sq.toString().trim()) }
                override fun beforeTextChanged(sq: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(sq: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }

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

    // ---------- Amorcage du vocabulaire ----------

    private fun runImport(label: String, work: () -> VocabularyImporter.Report) {
        Toast.makeText(this, "Analyse en cours…", Toast.LENGTH_SHORT).show()
        Thread {
            val report = try {
                work()
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (report == null || report.words == 0) {
                    Toast.makeText(this, "Rien trouvé dans " + label, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "✅ " + report.toString(), Toast.LENGTH_LONG).show()
                }
                memoryRefresher?.invoke()
            }
        }.start()
    }

    private fun hasPermission(perm: String): Boolean =
        checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun importSms() {
        if (!hasPermission(android.Manifest.permission.READ_SMS)) {
            AlertDialog.Builder(this)
                .setTitle("Apprendre de tes messages")
                .setMessage(
                    "Le clavier va lire tes SMS ENVOYÉS pour apprendre ta façon d'écrire : " +
                            "tes mots, tes expressions, tes tournures.\n\n" +
                            "Rien n'est envoyé sur internet, tout reste sur ton téléphone. " +
                            "Les messages ne sont pas conservés, seuls les mots sont comptés."
                )
                .setPositiveButton("Autoriser") { _, _ ->
                    requestPermissions(arrayOf(android.Manifest.permission.READ_SMS), REQ_SMS)
                }
                .setNegativeButton("Annuler", null)
                .show()
            return
        }
        runImport("tes messages") { VocabularyImporter.importSentMessages(this) }
    }

    private fun importContacts() {
        if (!hasPermission(android.Manifest.permission.READ_CONTACTS)) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
            return
        }
        runImport("tes contacts") { VocabularyImporter.importContacts(this) }
    }

    private fun importUserDict() {
        runImport("le dictionnaire Android") { VocabularyImporter.importUserDictionary(this) }
    }

    private fun importChatFile() {
        AlertDialog.Builder(this)
            .setTitle("Importer une conversation")
            .setMessage(
                "Dans WhatsApp : ouvre une discussion → ⋮ → Plus → Exporter la discussion → " +
                        "Sans les médias → enregistre le fichier.\n\n" +
                        "Le clavier te demandera ensuite quel nom est le tien, " +
                        "et n'apprendra QUE tes propres messages."
            )
            .setPositiveButton("Choisir le fichier") { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                startActivityForResult(Intent.createChooser(intent, "Choisir l'export"), PICK_CHAT)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /** Analyse le texte, demande qui est l'utilisateur, puis apprend ses messages. */
    private fun handleChatText(text: String) {
        if (text.length < 40) {
            Toast.makeText(this, "Fichier vide ou trop court", Toast.LENGTH_SHORT).show()
            return
        }
        val parsed = ChatLogImporter.parse(text)
        if (!ChatLogImporter.isChatExport(parsed)) {
            // Pas un export de conversation : on apprend tout le texte
            runImport("ce fichier") {
                VocabularyImporter.learnFromText(this, text, 2, "ton fichier")
            }
            return
        }
        val senders = ChatLogImporter.senders(parsed)
        if (senders.isEmpty()) {
            runImport("ce fichier") {
                VocabularyImporter.learnFromText(this, text, 2, "ton fichier")
            }
            return
        }
        val labels = senders.map { it.first + "  (" + it.second + " messages)" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Lequel est toi ?")
            .setItems(labels) { _, which ->
                val me = senders[which].first
                val mine = parsed.bySender[me]?.toString() ?: ""
                if (mine.length < 20) {
                    Toast.makeText(this, "Pas assez de texte pour ce nom", Toast.LENGTH_SHORT).show()
                    return@setItems
                }
                runImport("tes messages") {
                    VocabularyImporter.learnFromText(this, mine, 3, "tes messages de « " + me + " »")
                }
            }
            .setNeutralButton("Tout apprendre") { _, _ ->
                runImport("ce fichier") {
                    VocabularyImporter.learnFromText(this, text, 2, "toute la conversation")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun importTextDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        layout.addView(TextView(this).apply {
            text = "Colle ici un texte que tu as écrit : une conversation WhatsApp, " +
                    "des notes, des mails… Plus le texte est long, mieux le clavier te connaîtra."
            textSize = 13f
            setTextColor(Color.parseColor("#5F6368"))
        })
        val field = EditText(this).apply {
            hint = "Colle ton texte ici…"
            minLines = 5
            gravity = Gravity.TOP
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        layout.addView(field)
        AlertDialog.Builder(this)
            .setTitle("Apprendre d'un texte")
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton("Apprendre") { _, _ ->
                val text = field.text.toString()
                if (text.length < 20) {
                    Toast.makeText(this, "Texte trop court", Toast.LENGTH_SHORT).show()
                } else {
                    handleChatText(text)
                }
            }
            .setNeutralButton("Depuis un fichier") { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "text/*" }
                startActivityForResult(Intent.createChooser(intent, "Choisir un fichier texte"), PICK_TEXT_FILE)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Toast.makeText(this, "Autorisation refusée", Toast.LENGTH_SHORT).show()
            return
        }
        when (requestCode) {
            REQ_SMS -> runImport("tes messages") { VocabularyImporter.importSentMessages(this) }
            REQ_CONTACTS -> runImport("tes contacts") { VocabularyImporter.importContacts(this) }
        }
    }

    private fun applyImport(pairs: List<ShortcutImporter.Pair2>) {
        if (pairs.isEmpty()) {
            Toast.makeText(this, "Aucun raccourci reconnu dans ce texte", Toast.LENGTH_LONG).show()
            return
        }
        val preview = pairs.take(6).joinToString("\n") { it.key + "  →  " + it.value }
        val more = if (pairs.size > 6) "\n… et " + (pairs.size - 6) + " autres" else ""
        AlertDialog.Builder(this)
            .setTitle(pairs.size.toString() + " raccourcis trouvés")
            .setMessage(preview + more)
            .setPositiveButton("Tout importer") { _, _ ->
                val n = ShortcutImporter.save(this, pairs)
                buildShortcuts()
                Toast.makeText(this, n.toString() + " raccourcis importés ✅", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun importDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        layout.addView(TextView(this).apply {
            text = "Ouvre les raccourcis du clavier Samsung, sélectionne la liste et copie-la, " +
                    "puis colle-la ici. Formats acceptés : « slt = Salut », « slt : Salut », " +
                    "« slt → Salut », ou une ligne sur deux."
            textSize = 13f
            setTextColor(Color.parseColor("#5F6368"))
        })
        val field = EditText(this).apply {
            hint = "Colle ici tes raccourcis…"
            minLines = 5
            gravity = Gravity.TOP
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        layout.addView(field)

        val scroll = ScrollView(this).apply { addView(layout) }

        AlertDialog.Builder(this)
            .setTitle("Importer des raccourcis")
            .setView(scroll)
            .setPositiveButton("Analyser") { _, _ ->
                applyImport(ShortcutImporter.parse(field.text.toString()))
            }
            .setNeutralButton("Depuis un fichier") { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/csv", "application/json"))
                }
                startActivityForResult(Intent.createChooser(intent, "Choisir un fichier"), PICK_SHORTCUTS)
            }
            .setNegativeButton("Ouvrir réglages Samsung") { _, _ -> openSamsungSettings() }
            .show()

        // Tentative de lecture directe (fonctionne uniquement sur téléphone rooté)
        val auto = ShortcutImporter.tryReadSamsung()
        if (auto.isNotEmpty()) applyImport(auto)
    }

    private fun openSamsungSettings() {
        val candidates = listOf(
            "com.samsung.android.honeyboard" to "com.samsung.android.honeyboard.settings.SettingsActivity",
            "com.sec.android.inputmethod" to "com.sec.android.inputmethod.implement.setting.SettingsActivity"
        )
        for ((pkg, cls) in candidates) {
            try {
                startActivity(Intent().apply {
                    setClassName(pkg, cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            } catch (_: Exception) {
            }
        }
        try {
            val launch = packageManager.getLaunchIntentForPackage("com.samsung.android.honeyboard")
            if (launch != null) {
                startActivity(launch)
                return
            }
        } catch (_: Exception) {
        }
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        Toast.makeText(
            this,
            "Clavier Samsung → Saisie intelligente → Raccourcis de texte",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun exportDialog() {
        val text = ShortcutImporter.export(this)
        if (text.isBlank()) {
            Toast.makeText(this, "Aucun raccourci à exporter", Toast.LENGTH_SHORT).show()
            return
        }
        val field = EditText(this).apply {
            setText(text)
            minLines = 5
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Mes raccourcis")
            .setView(ScrollView(this).apply { addView(field) })
            .setPositiveButton("Copier") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Raccourcis", text))
                Toast.makeText(this, "Copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Fermer", null)
            .show()
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

    private fun editKeyDialog(label: String) {
        val current = prefs.keyColor(label)
        val items = arrayOf(
            "🎨 Choisir une couleur pour cette touche",
            "💡 Régler la luminosité de cette touche",
            "↩️ Remettre la couleur du thème"
        )
        AlertDialog.Builder(this)
            .setTitle("Touche « " + label + " »")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> colorPicker("Couleur de « " + label + " »", current ?: prefs.colorKey) { picked ->
                        prefs.setKeyColor(label, picked)
                        preview.refresh()
                    }
                    1 -> brightnessDialog(label)
                    else -> {
                        prefs.setKeyColor(label, null)
                        prefs.setKeyBrightness(label, 100)
                        preview.refresh()
                    }
                }
            }
            .show()
    }

    private fun brightnessDialog(label: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        layout.addView(TextView(this).apply {
            text = "Sombre  ←→  Lumineux"
            textSize = 13f
        })
        var value = prefs.keyBrightness(label)
        layout.addView(SeekBar(this).apply {
            max = 170
            progress = (value - 30).coerceIn(0, 170)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        value = p + 30
                        prefs.setKeyBrightness(label, value)
                        preview.refresh()
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })
        AlertDialog.Builder(this)
            .setTitle("Luminosité de « " + label + " »")
            .setView(layout)
            .setPositiveButton("OK", null)
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
