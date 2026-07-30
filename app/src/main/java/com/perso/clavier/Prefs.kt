package com.perso.clavier

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = sp.getInt("theme", 1)
        set(v) { sp.edit().putInt("theme", v).apply() }

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
