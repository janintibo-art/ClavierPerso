package com.perso.clavier

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class EmojiPanel(
    context: Context,
    private val prefs: Prefs,
    panelHeight: Int,
    private val onEmoji: (String) -> Unit,
    private val onBack: () -> Unit,
    private val onDelete: () -> Unit
) : LinearLayout(context) {

    private val categories: List<Pair<String, List<String>>> = listOf(
        "😀" to "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👻 💀 👽 🤖 💩 🤡".split(" "),
        "👋" to "👋 🤚 ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 💪 🦾 🖕 ✍️ 💅 🤳".split(" "),
        "❤️" to "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 💌 💋 😻 💑 💏".split(" "),
        "🐶" to "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🕷️ 🐢 🐍 🦎 🐙 🦑 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐘 🦒 🦓 🐆 🐒 🦍".split(" "),
        "🍎" to "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🍆 🥔 🥕 🌽 🌶️ 🥒 🥬 🥦 🍞 🥐 🥖 🧀 🥚 🍳 🥞 🧇 🥓 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍣 🍱 🍤 🍚 🍦 🍰 🎂 🧁 🍫 🍬 🍭 🍪 🍩 ☕ 🍵 🥤 🍺 🍷 🥂".split(" "),
        "⚽" to "⚽ 🏀 🏈 ⚾ 🎾 🏐 🎱 🏓 🏸 ⛳ 🏹 🎣 🥊 🛹 🛼 🎿 🏆 🥇 🥈 🥉 🎮 🎲 🎯 🎳 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🎻 🎬 🎨 🧩 ♟️ 🚴 🏊 🏄 🧗 🤸".split(" "),
        "📱" to "📱 💻 ⌨️ 🖥️ 🖨️ 📷 🎥 📺 ⏰ 🔋 🔌 💡 🔦 🕯️ 💰 💳 💎 🔧 🔨 ⚙️ 🔑 🔒 📌 📎 ✂️ 📚 📖 ✏️ 🖊️ 📝 📅 📦 🎁 🚗 🚕 🚌 🏍️ 🚲 ✈️ 🚀 🚢 🏠 🏢 🏥 🏫 ⛪ 🗼 🗽".split(" "),
        "✨" to "✨ ⭐ 🌟 💫 ⚡ 🔥 💥 ☀️ 🌤️ ⛅ 🌧️ ⛈️ 🌈 ❄️ ☃️ 💧 🌊 🌸 🌺 🌻 🌹 🌷 🌴 🌵 🍀 🍁 🎉 🎊 🎈 🎀 ✅ ❌ ❓ ❗ 💯 ⚠️ ♻️ 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🌍 🌙 💤".split(" ")
    )

    private val content = ScrollView(context)
    private val grid = LinearLayout(context).apply { orientation = VERTICAL }

    init {
        orientation = VERTICAL
        setBackgroundColor(prefs.colorBg)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight)

        // Barre de recherche
        val search = android.widget.EditText(context).apply {
            hint = "Rechercher un emoji…"
            setHintTextColor((0x80 shl 24) or (prefs.colorText and 0xFFFFFF))
            setTextColor(prefs.colorText)
            textSize = 14f
            maxLines = 1
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(sq: android.text.Editable?) {
                    val q = sq.toString().trim()
                    if (q.isEmpty()) showCategory(currentTab) else showSearch(q)
                }
                override fun beforeTextChanged(sq: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(sq: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        addView(search)

        // Onglets : récents puis catégories
        val tabs = LinearLayout(context).apply { orientation = HORIZONTAL }
        tabs.addView(TextView(context).apply {
            text = "🕘"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { showRecents() }
        })
        categories.forEachIndexed { index, (icon, _) ->
            tabs.addView(TextView(context).apply {
                text = icon
                textSize = 22f
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setOnClickListener { showCategory(index) }
            })
        }
        addView(HorizontalScrollView(context).apply { addView(tabs) })

        content.addView(grid)
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // Barre du bas : retour ABC + suppression
        val bottom = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        bottom.addView(TextView(context).apply {
            text = "✕  ABC"
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(prefs.colorTextOnAccent)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(prefs.colorAccent)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setOnClickListener { onBack() }
        })
        bottom.addView(TextView(context).apply {
            text = ""
            layoutParams = LayoutParams(0, 1, 1f)
        })
        bottom.addView(TextView(context).apply {
            text = "⌫"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(prefs.colorText)
            setPadding(dp(24), dp(10), dp(24), dp(10))
            setBackgroundColor(prefs.colorSpecial)
            setOnClickListener { onDelete() }
        })
        addView(bottom)

        showCategory(0)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private var currentTab = 0

    private fun showRecents() {
        val recents = prefs.recentEmojis()
        if (recents.isEmpty()) {
            grid.removeAllViews()
            grid.addView(TextView(context).apply {
                text = "Aucun emoji récent pour l'instant."
                textSize = 14f
                setTextColor(prefs.colorText)
                setPadding(dp(14), dp(20), dp(14), dp(10))
            })
            return
        }
        display(recents)
    }

    private fun showSearch(query: String) {
        val q = Dictionary.normalize(query)
        val found = LinkedHashSet<String>()
        // Correspondance par mot-cle
        EmojiSuggest.forWord(query)?.let { found.add(it) }
        for ((_, list) in categories) {
            for (e in list) if (e.isNotBlank() && found.size < 40) {
                // On garde tout si la recherche est courte, sinon on filtre par mot-cle
                if (q.length <= 1) found.add(e)
            }
        }
        val keyed = EmojiKeywords.search(query)
        keyed.forEach { found.add(it) }
        if (found.isEmpty()) {
            grid.removeAllViews()
            grid.addView(TextView(context).apply {
                text = "Aucun emoji trouvé pour « " + query + " »"
                textSize = 14f
                setTextColor(prefs.colorText)
                setPadding(dp(14), dp(20), dp(14), dp(10))
            })
            return
        }
        display(found.toList())
    }

    private fun showCategory(index: Int) {
        currentTab = index
        display(categories[index].second.filter { it.isNotBlank() })
    }

    private fun display(emojis: List<String>) {
        grid.removeAllViews()
        emojis.chunked(8).forEach { chunk ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            chunk.forEach { emoji ->
                row.addView(TextView(context).apply {
                    text = emoji
                    textSize = 26f
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(0, dp(8), 0, dp(8))
                    setOnClickListener {
                        prefs.addRecentEmoji(emoji)
                        onEmoji(emoji)
                    }
                })
            }
            // Compléter la dernière rangée pour garder l'alignement
            repeat(8 - chunk.size) {
                row.addView(TextView(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            grid.addView(row)
        }
        content.scrollTo(0, 0)
    }
}
