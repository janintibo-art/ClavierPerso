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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClip() }

    override fun onCreate() {
        super.onCreate()
        try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .addPrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }
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

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        hidePanel()
        captureClip()
        keyboardView?.refresh()
        keyboardView?.translateMode = translateTarget?.second
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
            val result = Translator.translate(full, target.first, sourceLang())
            mainHandler.post {
                if (result == null) {
                    Toast.makeText(this, "Traduction impossible (connexion ?)", Toast.LENGTH_SHORT).show()
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

    private fun doRewrite(instruction: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(4000, 0)?.toString() ?: ""
        val full = before + after
        if (full.isBlank()) return
        Toast.makeText(this, "✨ Reformulation…", Toast.LENGTH_SHORT).show()
        thread {
            val result = Rewriter.rewrite(full, instruction)
            mainHandler.post {
                if (result == null) {
                    Toast.makeText(this, "Reformulation impossible (connexion ?)", Toast.LENGTH_SHORT).show()
                    return@post
                }
                val c = currentInputConnection ?: return@post
                c.beginBatchEdit()
                c.deleteSurroundingText(before.length, after.length)
                c.commitText(result, 1)
                c.endBatchEdit()
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

    private fun currentPrefix(): String {
        val before = currentInputConnection?.getTextBeforeCursor(48, 0)?.toString() ?: return ""
        return before.takeLastWhile { it.isLetter() }
    }

    private fun updateSuggestions() {
        val kb = keyboardView ?: return
        if (!Prefs(this).suggestionsEnabled) {
            kb.suggestions = emptyList()
            return
        }
        val prefix = currentPrefix()
        var list = Dictionary.suggest(this, Prefs(this).langIndex, prefix)
        if (prefix.isNotEmpty() && prefix.first().isUpperCase()) {
            list = list.map { w -> w.replaceFirstChar { it.uppercase() } }
        }
        kb.suggestions = list
    }

    private fun learn(word: String) {
        if (word.length >= 3 && word.all { it.isLetter() }) {
            Prefs(this).learnWord(word)
        }
    }

    // ---------- Callbacks du clavier ----------

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return

        // Calculatrice : "12*45" puis "=" insere "=540"
        if (text == "=") {
            val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
            val expr = before.takeLastWhile { it in "0123456789+-*/×÷.,() " }.trim()
            val result = Calculator.eval(expr)
            if (result != null) {
                ic.commitText("=$result", 1)
                updateSuggestions()
                return
            }
        }

        if (text.length == 1 && !text[0].isLetter()) {
            // Raccourcis texte : "slt" + espace -> texte complet
            if (expandShortcut(ic, text)) {
                updateSuggestions()
                return
            }
            learn(currentPrefix())
        }
        ic.commitText(text, 1)
        updateSuggestions()
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        updateSuggestions()
    }

    override fun onEnter() {
        if (translateTarget != null) {
            doTranslate()
            return
        }
        currentInputConnection?.let { expandShortcut(it, "") }
        learn(currentPrefix())
        val handled = sendDefaultEditorAction(true)
        if (!handled) {
            currentInputConnection?.commitText("\n", 1)
        }
        updateSuggestions()
    }

    override fun onSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val prefix = currentPrefix()
        if (prefix.isNotEmpty()) {
            ic.deleteSurroundingText(prefix.length, 0)
        }
        ic.commitText("$word ", 1)
        learn(word.lowercase())
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
