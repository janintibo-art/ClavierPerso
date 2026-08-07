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
import android.widget.FrameLayout
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File
import kotlin.concurrent.thread

class KeyboardService : InputMethodService(), KeyboardView.Listener {

    private var container: FrameLayout? = null
    private var keyboardView: KeyboardView? = null
    private var panel: View? = null
    private var translateTarget: Pair<String, String>? = null
    private var aiMode: AiModes.Mode? = null
    private var aiUndo: Pair<String, String>? = null
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
        val frame = FrameLayout(this)
        val kb = KeyboardView(this, this)
        frame.addView(kb)
        keyboardView = kb
        container = frame
        return frame
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

    private fun showPanel(view: View) {
        hidePanel()
        keyboardView?.visibility = View.GONE
        container?.addView(view)
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
                this, Prefs(this), panelHeight(),
                onCommit = { file -> commitGif(file) },
                onBack = { hidePanel() }
            )
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
                resync()
                updateSuggestions()
                Toast.makeText(this, "Prêt à envoyer (⌫ pour annuler)", Toast.LENGTH_SHORT).show()
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
            val display = if (prefix.length >= 2 && !res.typedIsKnown && list.isNotEmpty())
                listOf(prefix) + list.take(2) else list
            mainHandler.post {
                if (seq != suggestionSeq) return@post
                keyboardView?.suggestions = display
                keyboardView?.highlightIndex = if (display.size > 1 && display[0] == prefix) 1 else -1
                pendingCorrection = res.correction
            }
        }
    }

    private fun learn(word: String) {
        if (word.length >= 2 && word.all { it.isLetter() || it == '\'' }) {
            prefs().learnWord(word.lowercase(), previousWord())
        }
    }

    // ---------- Callbacks du clavier ----------

    override fun onText(text: String) {
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
        val word = raw.trim('\u201C', '\u201D', '"')
        val ic = currentInputConnection ?: return
        val prefix = currentPrefix()
        val prev = previousWord()
        ic.beginBatchEdit()
        if (prefix.isNotEmpty()) ic.deleteSurroundingText(prefix.length, 0)
        ic.commitText("$word ", 1)
        ic.endBatchEdit()
        val p = prefs()
        // Un mot choisi volontairement compte double dans l'apprentissage
        p.learnWord(word.lowercase(), prev)
        p.learnWord(word.lowercase(), prev)
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
