package com.perso.clavier

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)

    companion object {
        /** Caches partages : evitent de relire et reparser le JSON a chaque touche. */
        private var countsCache: Map<String, Int>? = null
        private var bigramCache: HashMap<String, List<String>>? = null

        fun invalidateCaches() {
            countsCache = null
            bigramCache = null
        }
    }

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

    // ----- Sensibilite -----

    /** 0 = classique (au relachement), 1 = instantane (des le contact). */
    var instantKey: Boolean
        get() = sp.getBoolean("instant_key", false)
        set(v) { sp.edit().putBoolean("instant_key", v).apply() }

    /** 30 = tres reactif, 200 = plus tolerant. Pilote les delais d'appui long. */
    var sensitivity: Int
        get() = sp.getInt("sensitivity", 100)
        set(v) { sp.edit().putInt("sensitivity", v).apply() }

    /** Marge invisible autour des touches, en dp. */
    var touchMargin: Int
        get() = sp.getInt("touch_margin", 10)
        set(v) { sp.edit().putInt("touch_margin", v).apply() }

    // ----- Effet visuel a la frappe -----

    /** 0 = aucun, 1 = couleur, 2 = onde, 3 = zoom, 4 = eclat, 5 = etincelles */
    var pressEffect: Int
        get() = sp.getInt("press_effect", 1)
        set(v) { sp.edit().putInt("press_effect", v).apply() }

    /** Duree de l'effet en millisecondes. */
    var pressEffectDuration: Int
        get() = sp.getInt("press_duration", 260)
        set(v) { sp.edit().putInt("press_duration", v).apply() }

    /** Couleur de l'effet ; 0 = utiliser la couleur d'accent. */
    var pressEffectColor: Int
        get() = sp.getInt("press_color", 0)
        set(v) { sp.edit().putInt("press_color", v).apply() }

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

    /**
     * N'afficher le clavier que si l'utilisateur touche vraiment le champ.
     * Evite qu'il surgisse quand une application donne le focus toute seule
     * (retour dans Termux, collage, changement d'ecran...).
     */
    var showOnlyOnTap: Boolean
        get() = sp.getBoolean("show_on_tap", true)
        set(v) { sp.edit().putBoolean("show_on_tap", v).apply() }

    /** Appui long sur ⌫ : efface mot par mot apres 1,6 s. */
    var deleteByWord: Boolean
        get() = sp.getBoolean("del_word", true)
        set(v) { sp.edit().putBoolean("del_word", v).apply() }

    /** Duree de la vibration, en millisecondes (0 = silencieux). */
    var vibrationMs: Int
        get() = sp.getInt("vibration_ms", 12)
        set(v) { sp.edit().putInt("vibration_ms", v).apply() }

    /** Marge sous le clavier, en dp (utile avec la navigation gestuelle). */
    var bottomPadding: Int
        get() = sp.getInt("bottom_pad", 0)
        set(v) { sp.edit().putInt("bottom_pad", v).apply() }

    /** 0 = pleine largeur, 1 = compact a gauche, 2 = compact a droite. */
    var oneHandMode: Int
        get() = sp.getInt("one_hand", 0)
        set(v) { sp.edit().putInt("one_hand", v).apply() }

    /** Indices des caracteres secondaires affiches sur les touches. */
    var showSecondary: Boolean
        get() = sp.getBoolean("show_secondary", true)
        set(v) { sp.edit().putBoolean("show_secondary", v).apply() }

    /** Mode simple : barre d'outils reduite, comme un clavier classique. */
    var simpleMode: Boolean
        get() = sp.getBoolean("simple_mode", false)
        set(v) { sp.edit().putBoolean("simple_mode", v).apply() }

    /** Premier lancement : pour proposer l'assistant de configuration. */
    var firstRun: Boolean
        get() = sp.getBoolean("first_run", true)
        set(v) { sp.edit().putBoolean("first_run", v).apply() }

    // ----- Sons, police, themes par application -----

    /** Index dans KeySounds.names. */
    var soundType: Int
        get() = sp.getInt("sound_type", 0)
        set(v) { sp.edit().putInt("sound_type", v).apply() }

    var soundVolume: Int
        get() = sp.getInt("sound_volume", 60)
        set(v) { sp.edit().putInt("sound_volume", v).apply() }

    /** Index dans Fonts.names. */
    var fontIndex: Int
        get() = sp.getInt("font", 0)
        set(v) { sp.edit().putInt("font", v).apply() }

    /** Thème associé à une application (nom de paquet -> index de thème). */
    fun appThemes(): Map<String, Int> {
        return try {
            val obj = JSONObject(sp.getString("app_themes", "{}") ?: "{}")
            val m = HashMap<String, Int>()
            for (k in obj.keys()) m[k] = obj.optInt(k, -1)
            m
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setAppTheme(pkg: String, themeIndex: Int?) {
        try {
            val obj = JSONObject(sp.getString("app_themes", "{}") ?: "{}")
            if (themeIndex == null) obj.remove(pkg) else obj.put(pkg, themeIndex)
            sp.edit().putString("app_themes", obj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    // ----- Ecriture assistee -----

    var emojiSuggestions: Boolean
        get() = sp.getBoolean("emoji_sug", true)
        set(v) { sp.edit().putBoolean("emoji_sug", v).apply() }

    var smsCodeDetection: Boolean
        get() = sp.getBoolean("sms_code", true)
        set(v) { sp.edit().putBoolean("sms_code", v).apply() }

    var autoLanguage: Boolean
        get() = sp.getBoolean("auto_lang", false)
        set(v) { sp.edit().putBoolean("auto_lang", v).apply() }

    var incognitoFields: Boolean
        get() = sp.getBoolean("incognito", true)
        set(v) { sp.edit().putBoolean("incognito", v).apply() }

    /** Emojis recemment utilises, du plus recent au plus ancien. */
    fun recentEmojis(): List<String> =
        (sp.getString("recent_emojis", "") ?: "").split("\u0001").filter { it.isNotBlank() }

    fun addRecentEmoji(e: String) {
        val list = ArrayList(recentEmojis())
        list.remove(e)
        list.add(0, e)
        while (list.size > 32) list.removeAt(list.size - 1)
        sp.edit().putString("recent_emojis", list.joinToString("\u0001")).apply()
    }

    // ----- Cles de services -----

    /** Cle IA (compatible OpenAI, Groq, Mistral, OpenRouter...). */
    var aiKey: String
        get() = sp.getString("ai_key", "") ?: ""
        set(v) { sp.edit().putString("ai_key", v.trim()).apply() }

    var aiBaseUrl: String
        get() = sp.getString("ai_base", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
        set(v) { sp.edit().putString("ai_base", v.trim()).apply() }

    var aiModel: String
        get() = sp.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(v) { sp.edit().putString("ai_model", v.trim()).apply() }

    var deeplKey: String
        get() = sp.getString("deepl_key", "") ?: ""
        set(v) { sp.edit().putString("deepl_key", v.trim()).apply() }

    var googleTranslateKey: String
        get() = sp.getString("gtrans_key", "") ?: ""
        set(v) { sp.edit().putString("gtrans_key", v.trim()).apply() }

    /** 0 = Giphy, 1 = Klipy (Tenor a ferme le 30 juin 2026). */
    var gifProvider: Int
        get() = sp.getInt("gif_provider", 0)
        set(v) { sp.edit().putInt("gif_provider", v).apply() }

    var gifKey: String
        get() = sp.getString("gif_key", "") ?: ""
        set(v) { sp.edit().putString("gif_key", v.trim()).apply() }

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

    // ----- Apprentissage : frequence des mots + mot suivant -----

    var learningEnabled: Boolean
        get() = sp.getBoolean("learning", true)
        set(v) { sp.edit().putBoolean("learning", v).apply() }

    /** Mot -> nombre de fois ou l'utilisateur l'a ecrit. Mis en cache memoire. */
    fun wordCounts(): Map<String, Int> {
        countsCache?.let { return it }
        val map = try {
            val obj = JSONObject(sp.getString("freq", "{}") ?: "{}")
            val m = HashMap<String, Int>(obj.length())
            for (k in obj.keys()) m[k] = obj.optInt(k, 1)
            m
        } catch (e: Exception) {
            emptyMap<String, Int>()
        }
        countsCache = map
        return map
    }

    val learnedWords: Set<String>
        get() = wordCounts().keys

    /** Enregistre un mot ecrit par l'utilisateur (et le lien avec le mot precedent). */
    fun learnWord(word: String, previous: String? = null) {
        if (!learningEnabled) return
        val w = word.trim()
        if (w.length < 2 || w.length > 30) return
        try {
            val obj = JSONObject(sp.getString("freq", "{}") ?: "{}")
            // +2 par mot ecrit : le clavier s'adapte plus vite
            val count = obj.optInt(w, 0) + 2
            obj.put(w, count)
            // Elagage : on retire les mots vus une seule fois quand c'est trop gros
            if (obj.length() > 5000) {
                val rares = obj.keys().asSequence().filter { obj.optInt(it, 0) <= 1 }.toList()
                rares.take(400).forEach { obj.remove(it) }
            }
            val e = sp.edit().putString("freq", obj.toString())

            if (!previous.isNullOrBlank() && previous.length >= 2) {
                val big = JSONObject(sp.getString("bigrams", "{}") ?: "{}")
                val key = previous.lowercase()
                val nexts = big.optJSONObject(key) ?: JSONObject()
                nexts.put(w, nexts.optInt(w, 0) + 1)
                // Garder au maximum 6 suites par mot
                if (nexts.length() > 6) {
                    val worst = nexts.keys().asSequence().minByOrNull { nexts.optInt(it, 0) }
                    if (worst != null) nexts.remove(worst)
                }
                big.put(key, nexts)
                if (big.length() > 800) {
                    val first = big.keys().asSequence().firstOrNull()
                    if (first != null) big.remove(first)
                }
                e.putString("bigrams", big.toString())
            }
            e.apply()
            invalidateCaches()
        } catch (_: Exception) {
        }
    }

    /**
     * Apprentissage en masse : fusionne d'un coup des milliers de mots.
     * Un seul acces disque au lieu d'un par mot (indispensable pour l'import).
     */
    fun learnBulk(counts: Map<String, Int>, bigrams: Map<String, Map<String, Int>>) {
        if (counts.isEmpty() && bigrams.isEmpty()) return
        try {
            val freq = JSONObject(sp.getString("freq", "{}") ?: "{}")
            for ((w, c) in counts) {
                if (w.length < 2 || w.length > 30) continue
                freq.put(w, freq.optInt(w, 0) + c)
            }
            // Elagage si la memoire devient trop grosse : on garde les plus utilises
            if (freq.length() > 5000) {
                val all = freq.keys().asSequence().map { it to freq.optInt(it, 0) }.toList()
                val keep = all.sortedByDescending { it.second }.take(4000).map { it.first }.toHashSet()
                all.forEach { if (it.first !in keep) freq.remove(it.first) }
            }

            val big = JSONObject(sp.getString("bigrams", "{}") ?: "{}")
            for ((prev, nexts) in bigrams) {
                if (prev.length < 2) continue
                val obj = big.optJSONObject(prev) ?: JSONObject()
                for ((next, c) in nexts) {
                    obj.put(next, obj.optInt(next, 0) + c)
                }
                // Au maximum 6 suites memorisees par mot
                while (obj.length() > 6) {
                    val worst = obj.keys().asSequence().minByOrNull { obj.optInt(it, 0) } ?: break
                    obj.remove(worst)
                }
                big.put(prev, obj)
            }
            if (big.length() > 3000) {
                val extra = big.keys().asSequence().toList().take(big.length() - 3000)
                extra.forEach { big.remove(it) }
            }

            sp.edit()
                .putString("freq", freq.toString())
                .putString("bigrams", big.toString())
                .apply()
            invalidateCaches()
        } catch (_: Exception) {
        }
    }

    /** Nombre d'enchainements de mots memorises. */
    fun bigramCount(): Int = try {
        JSONObject(sp.getString("bigrams", "{}") ?: "{}").length()
    } catch (e: Exception) {
        0
    }

    /** Mots qui suivent souvent [previous], du plus frequent au moins frequent. */
    fun nextWords(previous: String): List<String> {
        val key = previous.lowercase()
        bigramCache?.let { cache -> cache[key]?.let { return it } }
        val result = try {
            val big = JSONObject(sp.getString("bigrams", "{}") ?: "{}")
            val nexts = big.optJSONObject(key)
            if (nexts == null) emptyList()
            else nexts.keys().asSequence()
                .map { it to nexts.optInt(it, 0) }
                .sortedByDescending { it.second }
                .map { it.first }
                .toList()
        } catch (e: Exception) {
            emptyList<String>()
        }
        val cache = bigramCache ?: HashMap<String, List<String>>().also { bigramCache = it }
        if (cache.size > 200) cache.clear()
        cache[key] = result
        return result
    }

    /** Ajoute des mots saisis manuellement : poids fort pour qu'ils sortent en premier. */
    fun addManualWords(words: List<String>): Int {
        val counts = HashMap<String, Int>()
        for (w in words) {
            val t = w.trim()
            if (t.length in 2..30) counts[t] = 30
        }
        if (counts.isEmpty()) return 0
        learnBulk(counts, emptyMap())
        return counts.size
    }

    fun removeLearnedWord(word: String) {
        try {
            val obj = JSONObject(sp.getString("freq", "{}") ?: "{}")
            obj.remove(word)
            sp.edit().putString("freq", obj.toString()).apply()
            invalidateCaches()
        } catch (_: Exception) {
        }
    }

    fun forgetLearnedWords() {
        sp.edit().remove("freq").remove("bigrams").remove("learned").apply()
        invalidateCaches()
    }

    // ----- Saisie assistee -----

    /** Corrige automatiquement le mot quand on tape espace. */
    var autoCorrect: Boolean
        get() = sp.getBoolean("auto_correct", true)
        set(v) { sp.edit().putBoolean("auto_correct", v).apply() }

    /** Majuscule automatique en debut de phrase. */
    var autoCapitalize: Boolean
        get() = sp.getBoolean("auto_cap", true)
        set(v) { sp.edit().putBoolean("auto_cap", v).apply() }

    /** Double espace = point + espace. */
    var doubleSpacePeriod: Boolean
        get() = sp.getBoolean("double_space", true)
        set(v) { sp.edit().putBoolean("double_space", v).apply() }

    // ----- Options -----

    var vibration: Boolean
        get() = sp.getBoolean("vibration", true)
        set(v) { sp.edit().putBoolean("vibration", v).apply() }

    var sound: Boolean
        get() = sp.getBoolean("sound", false)
        set(v) { sp.edit().putBoolean("sound", v).apply() }

    var keyHeight: Int
        get() = sp.getInt("height", 56)
        set(v) { sp.edit().putInt("height", v).apply() }

    var textSize: Int
        get() = sp.getInt("textSize", 20)
        set(v) { sp.edit().putInt("textSize", v).apply() }
}
