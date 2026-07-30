package com.perso.clavier

import android.content.Context
import android.graphics.Color

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)

    // ----- Couleurs personnalisées (défaut : thème Sombre) -----

    private fun getC(key: String, def: String) = sp.getInt(key, Color.parseColor(def))
    private fun setC(key: String, v: Int) = sp.edit().putInt(key, v).apply()

    var colorBg: Int
        get() = getC("c_bg", "#1F2227")
        set(v) = setC("c_bg", v)

    var colorKey: Int
        get() = getC("c_key", "#33373E")
        set(v) = setC("c_key", v)

    var colorSpecial: Int
        get() = getC("c_special", "#282C33")
        set(v) = setC("c_special", v)

    var colorAccent: Int
        get() = getC("c_accent", "#4A6CF7")
        set(v) = setC("c_accent", v)

    var colorText: Int
        get() = getC("c_text", "#E8EAED")
        set(v) = setC("c_text", v)

    var colorTextOnAccent: Int
        get() = getC("c_text_accent", "#FFFFFF")
        set(v) = setC("c_text_accent", v)

    fun applyTheme(t: Theme) {
        colorBg = t.bg
        colorKey = t.key
        colorSpecial = t.special
        colorAccent = t.accent
        colorText = t.text
        colorTextOnAccent = t.textOnAccent
    }

    // ----- Image d'arrière-plan -----

    var bgImageEnabled: Boolean
        get() = sp.getBoolean("bg_image", false)
        set(v) { sp.edit().putBoolean("bg_image", v).apply() }

    var bgDim: Int
        get() = sp.getInt("bg_dim", 30)
        set(v) { sp.edit().putInt("bg_dim", v).apply() }

    var tenorKey: String
        get() = sp.getString("tenor_key", "") ?: ""
        set(v) { sp.edit().putString("tenor_key", v).apply() }

    var keyPopup: Boolean
        get() = sp.getBoolean("key_popup", true)
        set(v) { sp.edit().putBoolean("key_popup", v).apply() }

    var keyOpacity: Int
        get() = sp.getInt("key_opacity", 100)
        set(v) { sp.edit().putInt("key_opacity", v).apply() }


    // ----- Langue, chiffres, suggestions -----

    var langIndex: Int
        get() = sp.getInt("lang", 0)
        set(v) { sp.edit().putInt("lang", v).apply() }

    var numberRow: Boolean
        get() = sp.getBoolean("number_row", true)
        set(v) { sp.edit().putBoolean("number_row", v).apply() }

    var suggestionsEnabled: Boolean
        get() = sp.getBoolean("suggestions", true)
        set(v) { sp.edit().putBoolean("suggestions", v).apply() }

    val learnedWords: Set<String>
        get() = sp.getStringSet("learned", emptySet()) ?: emptySet()

    fun learnWord(w: String) {
        val word = w.lowercase()
        val set = HashSet(learnedWords)
        if (set.size >= 600 && word !in set) return
        if (set.add(word)) {
            sp.edit().putStringSet("learned", set).apply()
        }
    }

    // ----- Options -----

    var vibration: Boolean
        get() = sp.getBoolean("vibration", true)
        set(v) { sp.edit().putBoolean("vibration", v).apply() }

    var sound: Boolean
        get() = sp.getBoolean("sound", false)
        set(v) { sp.edit().putBoolean("sound", v).apply() }

    var keyHeight: Int
        get() = sp.getInt("height", 52)
        set(v) { sp.edit().putInt("height", v).apply() }

    var textSize: Int
        get() = sp.getInt("textSize", 20)
        set(v) { sp.edit().putInt("textSize", v).apply() }
}
