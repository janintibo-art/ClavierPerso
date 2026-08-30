package com.perso.clavier

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File
import kotlin.concurrent.thread

class KeyboardService : InputMethodService(), KeyboardView.Listener {

    private var container: android.widget.LinearLayout? = null
    private var keyboardView: KeyboardView? = null
    private var panel: View? = null
    private var translateTarget: Pair<String, String>? = null
    private var aiMode: AiModes.Mode? = null
    private var aiUndo: Pair<String, String>? = null
    private var lastAiResult: String? = null
    private var incognito = false
    private var pendingSmsCode: String? = null
    private var langVotes = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Etat local de la saisie : evite de relire le champ de texte a chaque touche
     * (chaque lecture est un appel inter-processus, c'est ce qui rendait la frappe molle).
     */
    private val composing = StringBuilder()
    private var lastWord = ""
    private var expectedCursor = -1
    private var lastAutoCorrect: Pair<String, String>? = null
    private var lastSpaceTime = 0L
    private var prefsCache: Prefs? = null

    private fun prefs(): Prefs = prefsCache ?: Prefs(this).also { prefsCache = it }

    /** Resynchronise le buffer avec le vrai contenu du champ. */
    private fun resync() {
        val ic = currentInputConnection
        val before = ic?.getTextBeforeCursor(96, 0)?.toString() ?: ""
        composing.setLength(0)
        composing.append(before.takeLastWhile { it.isLetter() || it == '\'' })
        val rest = before.dropLast(composing.length).trimEnd()
        lastWord = rest.takeLastWhile { it.isLetter() || it == '\'' }.lowercase()
        lastAutoCorrect = null
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClip() }

    override fun onCreate() {
        super.onCreate()
        try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .addPrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        hidePanel()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun captureClip() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return
            if (clip.itemCount == 0) return
            val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
            Prefs(this).addClip(text)
        } catch (_: Exception) {
        }
    }

    override fun onCreateInputView(): View {
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        val kb = KeyboardView(this, this)
        root.addView(kb)
        keyboardView = kb
        container = root
        return root
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Bouton Retour : on ferme d'abord le panneau ouvert (emojis, GIF, presse-papiers...)
        if (keyCode == KeyEvent.KEYCODE_BACK && panel != null) {
            hidePanel()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // Le curseur a bouge autrement que par notre frappe : on se resynchronise
        if (newSelStart != expectedCursor) {
            resync()
            updateSuggestions()
        }
        expectedCursor = newSelStart
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefsCache = null
        val p = prefs()

        // Thème associé à l'application en cours
        val pkg = info?.packageName
        val idx = if (pkg != null) p.appThemes()[pkg] else null
        keyboardView?.appTheme = if (idx != null) Themes.get(idx) else null

        // Champ sensible : on n'apprend rien et on masque les suggestions
        val type = info?.inputType ?: 0
        val variation = type and android.text.InputType.TYPE_MASK_VARIATION
        val cls = type and android.text.InputType.TYPE_MASK_CLASS
        incognito = p.incognitoFields && (
                variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
                        cls == android.text.InputType.TYPE_CLASS_PHONE
                )

        // Code recu par SMS
        pendingSmsCode = if (!incognito && p.smsCodeDetection &&
            checkSelfPermission(android.Manifest.permission.READ_SMS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) SmsCode.latest(this) else null

        resync()
        hidePanel()
        captureClip()
        keyboardView?.refresh()
        keyboardView?.translateMode = translateTarget?.second
        keyboardView?.aiMode = aiMode?.short
        keyboardView?.autoShift()
        updateSuggestions()
    }

    // ---------- Panneaux (émojis / GIF) ----------

    private fun panelHeight(): Int {
        val kb = keyboardView
        return if (kb != null && kb.height > 0) kb.height
        else (300 * resources.displayMetrics.density).toInt()
    }

    private fun showPanel(view: View, keepKeyboard: Boolean = false) {
        hidePanel()
        if (keepKeyboard) {
            // Le panneau se place au-dessus : le clavier reste utilisable
            container?.addView(view, 0)
        } else {
            keyboardView?.visibility = View.GONE
            container?.addView(view)
        }
        panel = view
    }

    private fun hidePanel() {
        val p = panel ?: return
        container?.removeView(p)
        panel = null
        keyboardView?.visibility = View.VISIBLE
    }

    override fun onEmojiToggle() {
        if (panel is EmojiPanel) {
            hidePanel()
            return
        }
        showPanel(
            EmojiPanel(
                this, Prefs(this), panelHeight(),
                onEmoji = { emoji -> currentInputConnection?.commitText(emoji, 1) },
                onBack = { hidePanel() },
                onDelete = { onDelete() }
            )
        )
    }

    override fun onGifToggle() {
        if (panel is GifPanel) {
            hidePanel()
            return
        }
        showPanel(
            GifPanel(
                this, prefs(), (panelHeight() * 0.95f).toInt(),
                onCommit = { file -> commitGif(file) },
                onBack = { hidePanel() }
            ),
            keepKeyboard = true
        )
    }

    override fun onTranslateToggle() {
        if (translateTarget != null) {
            translateTarget = null
            keyboardView?.translateMode = null
            Toast.makeText(this, "Mode traduction désactivé", Toast.LENGTH_SHORT).show()
            return
        }
        if (panel is TranslatePanel) {
            hidePanel()
            return
        }
        showPanel(
            TranslatePanel(
                this, Prefs(this), panelHeight(),
                onPick = { code, name ->
                    translateTarget = code to name
                    keyboardView?.translateMode = name
                    hidePanel()
                    Toast.makeText(
                        this,
                        "🌍 Écris ton message puis appuie sur ➜ pour l'envoyer en $name",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onBack = { hidePanel() }
            )
        )
    }

    private fun sourceLang() = listOf("fr", "en", "es")[Prefs(this).langIndex.coerceIn(0, 2)]

    private fun doTranslate() {
        val ic = currentInputConnection ?: return
        val target = translateTarget ?: return
        val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(4000, 0)?.toString() ?: ""
        val full = before + after
        if (full.isBlank()) {
            Toast.makeText(this, "Écris d'abord ton message", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Traduction…", Toast.LENGTH_SHORT).show()
        thread {
            val result = Translator.translate(prefs(), full, target.first, sourceLang())
            mainHandler.post {
                if (result == null) {
                    Toast.makeText(
                        this,
                        "Traduction impossible. Ajoute une clé IA ou DeepL dans les réglages.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@post
                }
                val c = currentInputConnection ?: return@post
                c.beginBatchEdit()
                c.deleteSurroundingText(before.length, after.length)
                c.commitText(result, 1)
                c.endBatchEdit()
                sendDefaultEditorAction(true)
                updateSuggestions()
            }
        }
    }

    override fun onMoveCursor(delta: Int) {
        sendDownUpKeyEvents(
            if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        )
    }

    override fun onClipboardPanel() {
        if (panel is ClipboardPanel) {
            hidePanel()
            return
        }
        showPanel(
            ClipboardPanel(
                this, Prefs(this), panelHeight(),
                onPaste = { text ->
                    currentInputConnection?.commitText(text, 1)
                    hidePanel()
                    updateSuggestions()
                },
                onBack = { hidePanel() }
            )
        )
    }

    override fun onRewrite() {
        if (panel is RewritePanel) {
            hidePanel()
            return
        }
        val before = currentInputConnection?.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = currentInputConnection?.getTextAfterCursor(4000, 0)?.toString() ?: ""
        if ((before + after).isBlank()) {
            Toast.makeText(this, "Écris d'abord ton message, puis appui long sur ⏎", Toast.LENGTH_SHORT).show()
            return
        }
        showPanel(
            RewritePanel(
                this, Prefs(this), panelHeight(),
                onPick = { instruction ->
                    hidePanel()
                    doRewrite(instruction)
                },
                onBack = { hidePanel() }
            )
        )
    }

    override fun onAiToggle() {
        if (aiMode != null) {
            aiMode = null
            keyboardView?.aiMode = null
            Toast.makeText(this, "Mode IA désactivé", Toast.LENGTH_SHORT).show()
            return
        }
        if (panel is AiPanel) {
            hidePanel()
            return
        }
        showPanel(
            AiPanel(
                this, prefs(), panelHeight(),
                onPick = { mode ->
                    aiMode = mode
                    keyboardView?.aiMode = mode.short
                    hidePanel()
                    Toast.makeText(
                        this,
                        "🤖 " + mode.label + " : " + mode.hint + ", puis ➜",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onBack = { hidePanel() }
            )
        )
    }

    /** Envoie la demande a l'IA et remplace le texte par la reponse. */
    private fun runAi() {
        val ic = currentInputConnection ?: return
        val mode = aiMode ?: return
        val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(4000, 0)?.toString() ?: ""
        val request = (before + after).trim()
        if (request.length < 3) {
            Toast.makeText(this, "Écris d'abord ta demande", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "🤖 " + mode.short + "…", Toast.LENGTH_SHORT).show()
        thread {
            val raw = AiClient.generate(prefs(), mode.system, request)
            mainHandler.post {
                if (raw == null) {
                    Toast.makeText(
                        this,
                        "Échec de l'IA. Ajoute une clé IA dans les réglages du clavier.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@post
                }
                val result = AiClient.cleanOutput(raw)
                if (result.isBlank()) {
                    Toast.makeText(this, "Réponse vide, réessaie", Toast.LENGTH_SHORT).show()
                    return@post
                }
                val c = currentInputConnection ?: return@post
                c.beginBatchEdit()
                c.deleteSurroundingText(before.length, after.length)
                c.commitText(result, 1)
                c.endBatchEdit()
                // Un retour arriere juste apres restaure la demande d'origine
                aiUndo = request to result
                lastAiResult = result
                resync()
                updateSuggestions()
                Toast.makeText(this, "Prêt à envoyer (⌫ pour annuler)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Efface le mot entier a gauche du curseur. */
    override fun onDeleteWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(120, 0)?.toString() ?: ""
        if (before.isEmpty()) return
        // On enleve les espaces finaux, puis le mot
        val trimmed = before.trimEnd()
        val spaces = before.length - trimmed.length
        val word = trimmed.takeLastWhile { !it.isWhitespace() }
        val count = (spaces + word.length).coerceAtLeast(1)
        ic.deleteSurroundingText(count, 0)
        resync()
        updateSuggestions()
    }

    /** Dictee vocale : ouvre la reconnaissance vocale du systeme. */
    override fun onVoiceInput() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            // Bascule vers le clavier vocal du systeme s'il existe
            val voice = imm.enabledInputMethodList.firstOrNull {
                it.packageName.contains("google", true) &&
                        it.id.contains("voice", true)
            }
            if (voice != null) {
                switchInputMethod(voice.id)
                return
            }
        } catch (_: Exception) {
        }
        Toast.makeText(
            this,
            "Aucune saisie vocale trouvée. Active « Google Voice Typing » dans les réglages Android.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onNavPanel() {
        if (panel is NavPanel) {
            hidePanel()
            return
        }
        showPanel(
            NavPanel(this, prefs(), panelHeight(),
                onAction = { doNavAction(it) },
                onBack = { hidePanel() })
        )
    }

    private fun doNavAction(action: String) {
        val ic = currentInputConnection ?: return
        when (action) {
            "left" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            "right" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            "up" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP)
            "down" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN)
            "home" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_HOME)
            "end" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_END)
            "top" -> {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_HOME)
                repeat(40) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP) }
            }
            "bottom" -> repeat(40) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN) }
            "del" -> onDelete()
            "forwardDel" -> ic.deleteSurroundingText(0, 1)
            "selectAll" -> ic.performContextMenuAction(android.R.id.selectAll)
            "copy" -> ic.performContextMenuAction(android.R.id.copy)
            "cut" -> ic.performContextMenuAction(android.R.id.cut)
            "paste" -> ic.performContextMenuAction(android.R.id.paste)
            "undo" -> sendCtrl(KeyEvent.KEYCODE_Z, false)
            "redo" -> sendCtrl(KeyEvent.KEYCODE_Z, true)
        }
        resync()
        updateSuggestions()
    }

    /** Envoie Ctrl+Z ou Ctrl+Maj+Z (annuler / rétablir). */
    private fun sendCtrl(keyCode: Int, shift: Boolean) {
        val ic = currentInputConnection ?: return
        var meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        val now = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    /** Retouche la derniere reponse de l'IA, ou complete la phrase en cours. */
    override fun onAiFollowUp(instruction: String) {
        if (instruction == "__complete__") {
            completeSentence()
            return
        }
        aiFollowUp(instruction)
    }

    private fun aiFollowUp(instruction: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(4000, 0)?.toString() ?: ""
        val text = (before + after).trim()
        if (text.isEmpty()) return
        Toast.makeText(this, "🤖 …", Toast.LENGTH_SHORT).show()
        thread {
            val raw = AiClient.generate(
                prefs(),
                "Tu retouches un texte. Réponds UNIQUEMENT avec le texte final, " +
                        "sans guillemets ni explication.",
                instruction + " :\n\n" + text
            )
            mainHandler.post {
                if (raw == null) {
                    Toast.makeText(this, "Échec de l'IA", Toast.LENGTH_SHORT).show()
                    return@post
                }
                val result = AiClient.cleanOutput(raw)
                val c = currentInputConnection ?: return@post
                c.beginBatchEdit()
                c.deleteSurroundingText(before.length, after.length)
                c.commitText(result, 1)
                c.endBatchEdit()
                aiUndo = text to result
                lastAiResult = result
                resync()
                updateSuggestions()
            }
        }
    }

    /** Complete la phrase en cours d'ecriture (appui long sur 🤖). */
    private fun completeSentence() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(2000, 0)?.toString() ?: ""
        if (before.trim().length < 4) {
            Toast.makeText(this, "Commence ta phrase, puis appui long sur 🤖", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "🤖 Je complète…", Toast.LENGTH_SHORT).show()
        thread {
            val raw = AiClient.generate(
                prefs(),
                "Tu complètes le texte de l'utilisateur. Réponds UNIQUEMENT avec la SUITE " +
                        "du texte, sans répéter ce qui est déjà écrit, sans guillemets. " +
                        "Une à deux phrases maximum, dans la même langue et le même ton.",
                before
            )
            mainHandler.post {
                if (raw == null) {
                    Toast.makeText(this, "Échec de l'IA", Toast.LENGTH_SHORT).show()
                    return@post
                }
                var suite = AiClient.cleanOutput(raw)
                if (suite.isBlank()) return@post
                if (!before.endsWith(" ") && !suite.startsWith(" ") &&
                    !suite.startsWith(",") && !suite.startsWith(".")
                ) suite = " " + suite
                currentInputConnection?.commitText(suite, 1)
                aiUndo = null
                resync()
                updateSuggestions()
            }
        }
    }

    override fun onFixSpelling() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(4000, 0)?.toString() ?: ""
        if ((before + after).trim().length < 3) {
            Toast.makeText(this, "Écris d'abord ton message", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "✅ Correction en cours…", Toast.LENGTH_SHORT).show()
        doRewrite(Rewriter.FIX_INSTRUCTION)
    }

    private fun doRewrite(instruction: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(4000, 0)?.toString() ?: ""
        val full = before + after
        if (full.isBlank()) return
        Toast.makeText(this, "✨ Reformulation…", Toast.LENGTH_SHORT).show()
        thread {
            val result = Rewriter.rewrite(prefs(), full, instruction)
            mainHandler.post {
                if (result == null) {
                    val hint = if (AiClient.isConfigured(prefs()))
                        "Échec : vérifie ta clé IA dans les réglages"
                    else
                        "Échec : ajoute une clé IA dans les réglages pour un résultat fiable"
                    Toast.makeText(this, hint, Toast.LENGTH_LONG).show()
                    return@post
                }
                val c = currentInputConnection ?: return@post
                c.beginBatchEdit()
                c.deleteSurroundingText(before.length, after.length)
                c.commitText(result, 1)
                c.endBatchEdit()
                resync()
                updateSuggestions()
            }
        }
    }

    private fun expandShortcut(ic: InputConnection, trailing: String): Boolean {
        val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
        val token = before.takeLastWhile { !it.isWhitespace() }
        if (token.isEmpty()) return false
        val expansion = Prefs(this).shortcuts()[token] ?: return false
        ic.deleteSurroundingText(token.length, 0)
        ic.commitText(expansion + trailing, 1)
        return true
    }

    private fun commitGif(file: File) {
        if (Build.VERSION.SDK_INT < 25) {
            Toast.makeText(this, "GIF non pris en charge sur cette version d'Android", Toast.LENGTH_SHORT).show()
            return
        }
        val mimeTypes = currentInputEditorInfo?.contentMimeTypes
        val supported = mimeTypes?.any { ClipDescription.compareMimeTypes("image/gif", it) } == true
        if (!supported) {
            Toast.makeText(this, "Cette application n'accepte pas les GIF ici", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val info = InputContentInfo(uri, ClipDescription("GIF", arrayOf("image/gif")))
            currentInputConnection?.commitContent(
                info,
                InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                null
            )
            hidePanel()
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible d'envoyer le GIF", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Suggestions ----------

    private fun currentPrefix(): String = composing.toString()

    private fun previousWord(): String = lastWord

    private var suggestionSeq = 0
    private var pendingCorrection: String? = null
    private val suggestionRunnable = Runnable { computeSuggestions() }

    /** Programme un calcul de suggestions (regroupe les frappes rapides). */
    private fun updateSuggestions() {
        mainHandler.removeCallbacks(suggestionRunnable)
        mainHandler.postDelayed(suggestionRunnable, 25)
    }

    private fun computeSuggestions() {
        val kb = keyboardView ?: return
        if (!prefs().suggestionsEnabled) {
            kb.suggestions = emptyList()
            pendingCorrection = null
            return
        }
        // Code de verification recu par SMS : priorite absolue
        val code = pendingSmsCode
        if (code != null) {
            kb.suggestions = listOf("\uD83D\uDCE9 $code", "\u2715")
            kb.highlightIndex = 0
            pendingCorrection = null
            return
        }

        // Juste apres une reponse de l'IA : propositions de suivi
        if (lastAiResult != null) {
            kb.suggestions = listOf("✂️ Plus court", "🌍 En anglais", "🔁 Autre version")
            kb.highlightIndex = -1
            pendingCorrection = null
            return
        }

        val prefix = currentPrefix()
        val prev = previousWord()
        if (prefix.isEmpty() && prev.isEmpty()) {
            kb.suggestions = emptyList()
            pendingCorrection = null
            return
        }
        val lang = prefs().langIndex
        val seq = ++suggestionSeq
        thread {
            val res = try {
                Dictionary.suggest(this, lang, prefix, 3, prev)
            } catch (e: Exception) {
                null
            } ?: return@thread
            var list = res.words
            if (prefix.isNotEmpty() && prefix.first().isUpperCase()) {
                list = list.map { w -> w.replaceFirstChar { it.uppercase() } }
            }
            // Le mot tape reste affiche a gauche pour pouvoir le garder d'un doigt
            var display = if (prefix.length >= 2 && !res.typedIsKnown && list.isNotEmpty())
                listOf(prefix) + list.take(2) else list
            // Emoji correspondant au mot ecrit
            if (prefs().emojiSuggestions) {
                EmojiSuggest.forWord(prefix)?.let { emo ->
                    display = (listOf(emo) + display).take(3)
                }
            }
            mainHandler.post {
                if (seq != suggestionSeq) return@post
                keyboardView?.suggestions = display
                keyboardView?.highlightIndex = if (display.size > 1 && display[0] == prefix) 1 else -1
                pendingCorrection = res.correction
            }
        }
    }

    /**
     * Detection automatique de la langue : si plusieurs mots d'affilee sont inconnus
     * dans la langue courante mais connus dans une autre, on bascule.
     */
    private fun checkLanguage(word: String) {
        if (word.length < 4) return
        val current = prefs().langIndex
        if (Dictionary.contains(this, current, word)) {
            langVotes = 0
            return
        }
        var other = -1
        for (i in 0..2) {
            if (i != current && Dictionary.contains(this, i, word)) {
                other = i
                break
            }
        }
        if (other < 0) {
            langVotes = 0
            return
        }
        langVotes++
        if (langVotes >= 3) {
            langVotes = 0
            prefs().langIndex = other
            keyboardView?.refresh()
            Toast.makeText(this, "🌐 " + Layouts.languages[other], Toast.LENGTH_SHORT).show()
        }
    }

    private fun learn(word: String) {
        if (incognito) return
        if (word.length >= 2 && word.all { it.isLetter() || it == '\'' }) {
            prefs().learnWord(word.lowercase(), previousWord())
        }
    }

    // ---------- Callbacks du clavier ----------

    override fun onText(text: String) {
        // Le panneau GIF est ouvert : la frappe alimente la recherche
        (panel as? GifPanel)?.let { gif ->
            if (text == " ") gif.runSearch() else gif.appendQuery(text)
            return
        }
        val ic = currentInputConnection ?: return
        val p = prefs()

        // --- Calculatrice : "12*45" puis "=" ---
        if (text == "=") {
            val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
            val expr = before.takeLastWhile { it in "0123456789+-*/×÷.,() " }.trim()
            val result = Calculator.eval(expr)
            if (result != null) {
                ic.commitText("=$result", 1)
                composing.setLength(0)
                updateSuggestions()
                return
            }
        }

        val isLetter = text.length == 1 && (text[0].isLetter() || text[0] == '\'')

        if (isLetter) {
            ic.commitText(text, 1)
            composing.append(text)
            lastAutoCorrect = null
            aiUndo = null
            lastAiResult = null
            updateSuggestions()
            return
        }

        // --- Fin de mot ---
        val word = composing.toString()

        // Double espace = point (comme sur Samsung)
        if (text == " " && word.isEmpty() && p.doubleSpacePeriod) {
            val now = System.currentTimeMillis()
            if (now - lastSpaceTime < 600) {
                val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
                if (before.endsWith(" ") && before.length == 2 && before[0].isLetterOrDigit()) {
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    lastSpaceTime = 0
                    keyboardView?.autoShift()
                    updateSuggestions()
                    return
                }
            }
            lastSpaceTime = now
        }

        // Raccourcis texte
        if (word.isNotEmpty() && expandShortcut(ic, text)) {
            composing.setLength(0)
            lastWord = ""
            updateSuggestions()
            return
        }

        // Correction automatique du mot qui vient d'etre ecrit
        var written = word
        val correction = pendingCorrection
        if (p.autoCorrect && word.length >= 3 && correction != null &&
            !correction.equals(word, ignoreCase = true) && (text == " " || text in ".,!?;:")
        ) {
            val fixed = if (word.first().isUpperCase())
                correction.replaceFirstChar { it.uppercase() } else correction
            ic.beginBatchEdit()
            ic.deleteSurroundingText(word.length, 0)
            ic.commitText(fixed + text, 1)
            ic.endBatchEdit()
            lastAutoCorrect = word to fixed
            written = fixed
        } else {
            ic.commitText(text, 1)
            lastAutoCorrect = null
        }

        if (written.isNotEmpty()) {
            learn(written)
            lastWord = written.lowercase()
            if (p.autoLanguage) checkLanguage(written)
        }
        composing.setLength(0)
        pendingCorrection = null

        // Majuscule automatique apres une fin de phrase
        if (p.autoCapitalize && text in ".!?") {
            keyboardView?.autoShift()
        }
        updateSuggestions()
    }

    override fun onDelete() {
        (panel as? GifPanel)?.let {
            it.deleteQuery()
            return
        }
        val ic = currentInputConnection ?: return

        // Retour arriere juste apres une generation IA : on remet la demande d'origine
        val undo = aiUndo
        if (undo != null) {
            val (original, generated) = undo
            aiUndo = null
            val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
            if (before.endsWith(generated)) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(generated.length, 0)
                ic.commitText(original, 1)
                ic.endBatchEdit()
                resync()
                updateSuggestions()
                Toast.makeText(this, "Demande restaurée", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Retour arriere juste apres une correction auto : on remet le mot d'origine
        val auto = lastAutoCorrect
        if (auto != null) {
            val (original, fixed) = auto
            ic.beginBatchEdit()
            ic.deleteSurroundingText(fixed.length + 1, 0)
            ic.commitText(original, 1)
            ic.endBatchEdit()
            composing.setLength(0)
            composing.append(original)
            lastAutoCorrect = null
            updateSuggestions()
            return
        }

        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            resync()
        } else {
            ic.deleteSurroundingText(1, 0)
            if (composing.isNotEmpty()) {
                composing.deleteCharAt(composing.length - 1)
            } else {
                resync()
            }
        }
        updateSuggestions()
    }

    override fun onEnter() {
        (panel as? GifPanel)?.let {
            it.runSearch()
            return
        }
        if (aiMode != null) {
            runAi()
            return
        }
        if (translateTarget != null) {
            doTranslate()
            return
        }
        currentInputConnection?.let { expandShortcut(it, "") }
        learn(currentPrefix())
        composing.setLength(0)
        lastAutoCorrect = null
        val handled = sendDefaultEditorAction(true)
        if (!handled) {
            currentInputConnection?.commitText("\n", 1)
        }
        updateSuggestions()
    }

    override fun onSuggestion(raw: String) {
        val ic = currentInputConnection ?: return

        // Code recu par SMS
        val code = pendingSmsCode
        if (code != null) {
            pendingSmsCode = null
            if (raw == "\u2715") {
                SmsCode.dismiss(code)
            } else {
                ic.commitText(code, 1)
                SmsCode.dismiss(code)
                resync()
            }
            updateSuggestions()
            return
        }

        // Suivi apres une reponse de l'IA
        if (lastAiResult != null && raw.length > 2 && !raw.first().isLetter()) {
            when {
                raw.contains("court") -> aiFollowUp("Raccourcis ce texte au maximum en gardant le sens")
                raw.contains("anglais") -> aiFollowUp("Traduis ce texte en anglais")
                raw.contains("Autre") -> aiFollowUp("Propose une autre version, différente, du même texte")
                else -> {}
            }
            return
        }

        val word = raw.trim('\u201C', '\u201D', '"')

        // Emoji propose
        if (word.isNotEmpty() && !word.first().isLetterOrDigit() && word.length <= 3) {
            ic.commitText(word, 1)
            prefs().addRecentEmoji(word)
            lastAiResult = null
            updateSuggestions()
            return
        }

        val prefix = currentPrefix()
        val prev = previousWord()
        ic.beginBatchEdit()
        if (prefix.isNotEmpty()) ic.deleteSurroundingText(prefix.length, 0)
        ic.commitText("$word ", 1)
        ic.endBatchEdit()
        if (!incognito) {
            val p = prefs()
            p.learnWord(word.lowercase(), prev)
            p.learnWord(word.lowercase(), prev)
        }
        composing.setLength(0)
        lastWord = word.lowercase()
        lastAutoCorrect = null
        pendingCorrection = null
        updateSuggestions()
    }

    override fun onPaste() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this)
            if (!text.isNullOrEmpty()) {
                currentInputConnection?.commitText(text, 1)
                updateSuggestions()
                return
            }
        }
        Toast.makeText(this, "Presse-papiers vide", Toast.LENGTH_SHORT).show()
    }

    override fun onSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onLangSwitch() {
        val prefs = Prefs(this)
        prefs.langIndex = (prefs.langIndex + 1) % Layouts.languages.size
        keyboardView?.refresh()
        updateSuggestions()
        Toast.makeText(this, "🌐 ${Layouts.languages[prefs.langIndex]}", Toast.LENGTH_SHORT).show()
    }
}
