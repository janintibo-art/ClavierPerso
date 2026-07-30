package com.perso.clavier

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.Toast

class KeyboardService : InputMethodService(), KeyboardView.Listener {

    private var container: FrameLayout? = null
    private var keyboardView: KeyboardView? = null
    private var emojiPanel: EmojiPanel? = null

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
        hideEmoji()
        keyboardView?.refresh()
        keyboardView?.autoShift()
        updateSuggestions()
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
        if (text.length == 1 && !text[0].isLetter()) {
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

    // ---------- Émojis ----------

    override fun onEmojiToggle() {
        if (emojiPanel != null) {
            hideEmoji()
            return
        }
        val frame = container ?: return
        val kb = keyboardView ?: return
        val h = if (kb.height > 0) kb.height
        else (300 * resources.displayMetrics.density).toInt()
        val panel = EmojiPanel(
            this, Prefs(this), h,
            onEmoji = { emoji -> currentInputConnection?.commitText(emoji, 1) },
            onBack = { hideEmoji() },
            onDelete = { onDelete() }
        )
        kb.visibility = View.GONE
        frame.addView(panel)
        emojiPanel = panel
    }

    private fun hideEmoji() {
        val panel = emojiPanel ?: return
        container?.removeView(panel)
        emojiPanel = null
        keyboardView?.visibility = View.VISIBLE
    }
}
