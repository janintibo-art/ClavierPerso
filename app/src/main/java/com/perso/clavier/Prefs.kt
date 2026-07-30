package com.perso.clavier

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

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

    // ----- Couleurs par touche -----

    /** Couleur specifique d'une touche, ou null si elle suit la couleur generale. */
    fun keyColor(label: String): Int? {
        val v = sp.getInt("kc_" + label, 0)
        return if (v == 0) null else v
    }

    fun setKeyColor(label: String, color: Int?) {
        val e = sp.edit()
        if (color == null) e.remove("kc_" + label) else e.putInt("kc_" + label, color)
        e.apply()
    }

    fun clearKeyColors() {
        val e = sp.edit()
        sp.all.keys.filter { it.startsWith("kc_") || it.startsWith("kb_") }.forEach { e.remove(it) }
        e.apply()
    }

    /** Luminosite d'une touche : 50 = moitie, 100 = normal, 200 = double. */
    fun keyBrightness(label: String): Int = sp.getInt("kb_" + label, 100)

    fun setKeyBrightness(label: String, value: Int) {
        sp.edit().putInt("kb_" + label, value).apply()
    }

    // ----- Luminosite globale -----

    /** Luminosite des touches (30 a 200, 100 = normal). */
    var brightness: Int
        get() = sp.getInt("brightness", 100)
        set(v) { sp.edit().putInt("brightness", v).apply() }

    /** Luminosite de l'image de fond (30 a 200, 100 = normal). */
    var bgBrightness: Int
        get() = sp.getInt("bg_brightness", 100)
        set(v) { sp.edit().putInt("bg_brightness", v).apply() }

    /** Flou de l'image de fond (0 a 100). */
    var bgBlur: Int
        get() = sp.getInt("bg_blur", 0)
        set(v) { sp.edit().putInt("bg_blur", v).apply() }

    /** Saturation de l'image de fond (0 = noir et blanc, 100 = normal). */
    var bgSaturation: Int
        get() = sp.getInt("bg_saturation", 100)
        set(v) { sp.edit().putInt("bg_saturation", v).apply() }

    // ----- Mode RGB -----

    /** 0 = desactive, 1 = vague arc-en-ciel, 2 = respiration, 3 = reactif a la frappe, 4 = cascade */
    var rgbMode: Int
        get() = sp.getInt("rgb_mode", 0)
        set(v) { sp.edit().putInt("rgb_mode", v).apply() }

    /** Vitesse de l'animation RGB (10 a 200). */
    var rgbSpeed: Int
        get() = sp.getInt("rgb_speed", 60)
        set(v) { sp.edit().putInt("rgb_speed", v).apply() }

    /** Intensite du RGB : 0 = couleurs du theme, 100 = arc-en-ciel pur. */
    var rgbIntensity: Int
        get() = sp.getInt("rgb_intensity", 70)
        set(v) { sp.edit().putInt("rgb_intensity", v).apply() }

    /** Le texte des touches suit aussi les couleurs RGB. */
    var rgbText: Boolean
        get() = sp.getBoolean("rgb_text", false)
        set(v) { sp.edit().putBoolean("rgb_text", v).apply() }

    // ----- Image d'arrière-plan -----

    var bgImageEnabled: Boolean
        get() = sp.getBoolean("bg_image", false)
        set(v) { sp.edit().putBoolean("bg_image", v).apply() }

    var bgDim: Int
        get() = sp.getInt("bg_dim", 30)
        set(v) { sp.edit().putInt("bg_dim", v).apply() }

    // ----- Raccourcis texte -----

    fun shortcuts(): Map<String, String> {
        return try {
            val obj = JSONObject(sp.getString("shortcuts", "{}") ?: "{}")
            val map = LinkedHashMap<String, String>()
            for (k in obj.keys()) map[k] = obj.getString(k)
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun putShortcut(key: String, value: String) {
        val obj = JSONObject(sp.getString("shortcuts", "{}") ?: "{}")
        obj.put(key, value)
        sp.edit().putString("shortcuts", obj.toString()).apply()
    }

    fun removeShortcut(key: String) {
        val obj = JSONObject(sp.getString("shortcuts", "{}") ?: "{}")
        obj.remove(key)
        sp.edit().putString("shortcuts", obj.toString()).apply()
    }

    // ----- Historique du presse-papiers -----

    fun clips(): List<Pair<String, Boolean>> {
        return try {
            val arr = JSONArray(sp.getString("clips", "[]") ?: "[]")
            val out = ArrayList<Pair<String, Boolean>>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(o.getString("t") to o.optBoolean("p", false))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveClips(list: List<Pair<String, Boolean>>) {
        val arr = JSONArray()
        list.forEach { (t, pin) ->
            arr.put(JSONObject().put("t", t).put("p", pin))
        }
        sp.edit().putString("clips", arr.toString()).apply()
    }

    fun addClip(text: String) {
        if (text.isBlank() || text.length > 5000) return
        val list = clips().toMutableList()
        val existing = list.firstOrNull { it.first == text }
        val pinned = existing?.second ?: false
        list.removeAll { it.first == text }
        list.add(0, text to pinned)
        val pins = list.filter { it.second }
        val others = list.filter { !it.second }.take(20)
        saveClips(pins + others)
    }

    fun togglePinClip(text: String) {
        saveClips(clips().map { if (it.first == text) it.first to !it.second else it })
    }

    fun clearUnpinnedClips() {
        saveClips(clips().filter { it.second })
    }

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
