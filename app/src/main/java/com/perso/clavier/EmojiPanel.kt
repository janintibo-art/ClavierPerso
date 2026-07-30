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

        // Onglets de catégories
        val tabs = LinearLayout(context).apply { orientation = HORIZONTAL }
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
            text = "ABC"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(prefs.colorText)
            setPadding(dp(24), dp(10), dp(24), dp(10))
            setBackgroundColor(prefs.colorSpecial)
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

    private fun showCategory(index: Int) {
        grid.removeAllViews()
        val emojis = categories[index].second.filter { it.isNotBlank() }
        emojis.chunked(8).forEach { chunk ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            chunk.forEach { emoji ->
                row.addView(TextView(context).apply {
                    text = emoji
                    textSize = 26f
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(0, dp(8), 0, dp(8))
                    setOnClickListener { onEmoji(emoji) }
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
