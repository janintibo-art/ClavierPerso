package com.perso.clavier

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {

    private val appContext = context.applicationContext
    private val sp = appContext.getSharedPreferences("clavier", Context.MODE_PRIVATE)

    companion object {
        /** Caches partages : evitent de relire et reparser le JSON a chaque touche. */
        private var countsCache: Map<String, Int>? = null
        private var bigramCache: HashMap<String, List<String>>? = null
        private var trigramCache: HashMap<String, List<String>>? = null

        fun invalidateCaches() {
            countsCache = null
            bigramCache = null
            trigramCache = null
        }
    }

    // ----- Couleurs personnalisées (défaut v36 : thème Anarchie) -----

    private fun getC(key: String, def: String) = sp.getInt(key, Color.parseColor(def))
    private fun setC(key: String, v: Int) = sp.edit().putInt(key, v).apply()

    var colorBg: Int
        get() = getC("c_bg", "#0A0A0A")
        set(v) = setC("c_bg", v)

    var colorKey: Int
        get() = getC("c_key", "#1A0E0E")
        set(v) = setC("c_key", v)

    var colorSpecial: Int
        get() = getC("c_special", "#140A0A")
        set(v) = setC("c_special", v)

    var colorAccent: Int
        get() = getC("c_accent", "#E01B24")
        set(v) = setC("c_accent", v)

    var colorText: Int
        get() = getC("c_text", "#F5E6E6")
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

    // ----- Relief et style des touches -----

    /** 0 = plat, 1 = ombre portee, 2 = relief 3D, 3 = creux, 4 = contour, 5 = verre, 6 = neon */
    var keyStyle: Int
        get() = sp.getInt("key_style", 1)
        set(v) { sp.edit().putInt("key_style", v).apply() }

    /** Intensite de l'effet de relief (0 a 100). */
    var reliefDepth: Int
        get() = sp.getInt("relief_depth", 45)
        set(v) { sp.edit().putInt("relief_depth", v).apply() }

    /** Arrondi des coins des touches, en dp. */
    var cornerRadius: Int
        get() = sp.getInt("corner_radius", 9)
        set(v) { sp.edit().putInt("corner_radius", v).apply() }

    /** Espacement entre les touches, en dp. */
    var keySpacing: Int
        get() = sp.getInt("key_spacing", 3)
        set(v) { sp.edit().putInt("key_spacing", v).apply() }

    /** Epaisseur du contour des touches, en dixiemes de dp (0 = aucun). */
    var borderWidth: Int
        get() = sp.getInt("border_width", 0)
        set(v) { sp.edit().putInt("border_width", v).apply() }

    /** Couleur du contour ; 0 = derivee de la couleur d'accent. */
    var borderColor: Int
        get() = sp.getInt("border_color", 0)
        set(v) { sp.edit().putInt("border_color", v).apply() }

    /** Degrade vertical sur les touches (0 = aucun, 100 = marque). */
    var gradientStrength: Int
        get() = sp.getInt("gradient", 0)
        set(v) { sp.edit().putInt("gradient", v).apply() }

    /** Halo lumineux autour du texte des touches. */
    var textGlow: Int
        get() = sp.getInt("text_glow", 0)
        set(v) { sp.edit().putInt("text_glow", v).apply() }

    /** Ombre portee du texte. */
    var textShadow: Boolean
        get() = sp.getBoolean("text_shadow", false)
        set(v) { sp.edit().putBoolean("text_shadow", v).apply() }

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

    /** L'historique peut être totalement désactivé, sans empêcher le collage normal. */
    var clipboardHistoryEnabled: Boolean
        get() = sp.getBoolean("clipboard_history", true)
        set(v) {
            sp.edit().putBoolean("clipboard_history", v).apply()
            if (!v) clearUnpinnedClips()
        }

    /** Durée de conservation des éléments non épinglés : 0 = jamais, sinon en heures. */
    var clipboardExpireHours: Int
        get() = sp.getInt("clipboard_expire_h", 24)
        set(v) { sp.edit().putInt("clipboard_expire_h", v.coerceAtLeast(0)).apply() }

    private data class ClipEntry(val text: String, val pinned: Boolean, val time: Long)

    private fun clipEntries(prune: Boolean = true): List<ClipEntry> {
        return try {
            val arr = JSONArray(sp.getString("clips", "[]") ?: "[]")
            val out = ArrayList<ClipEntry>()
            val now = System.currentTimeMillis()
            val maxAge = clipboardExpireHours.takeIf { it > 0 }?.toLong()?.times(60L * 60L * 1000L)
            var changed = false
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val text = o.optString("t")
                if (text.isBlank()) continue
                val pinned = o.optBoolean("p", false)
                val time = o.optLong("ts", now)
                val expired = !pinned && maxAge != null && now - time > maxAge
                if (prune && expired) {
                    changed = true
                    continue
                }
                out.add(ClipEntry(text, pinned, time))
            }
            if (changed) saveClipEntries(out)
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clips(): List<Pair<String, Boolean>> =
        clipEntries().map { it.text to it.pinned }

    private fun saveClipEntries(list: List<ClipEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().put("t", e.text).put("p", e.pinned).put("ts", e.time))
        }
        sp.edit().putString("clips", arr.toString()).apply()
    }

    fun addClip(text: String) {
        if (!clipboardHistoryEnabled || text.isBlank() || text.length > 5000) return
        val list = clipEntries().toMutableList()
        val existing = list.firstOrNull { it.text == text }
        val pinned = existing?.pinned ?: false
        list.removeAll { it.text == text }
        list.add(0, ClipEntry(text, pinned, System.currentTimeMillis()))
        val pins = list.filter { it.pinned }
        val others = list.filter { !it.pinned }.take(20)
        saveClipEntries(pins + others)
    }

    fun togglePinClip(text: String) {
        saveClipEntries(clipEntries().map {
            if (it.text == text) it.copy(pinned = !it.pinned) else it
        })
    }

    fun clearUnpinnedClips() {
        saveClipEntries(clipEntries(prune = false).filter { it.pinned })
    }

    /**
     * N'afficher le clavier que si l'utilisateur touche vraiment le champ.
     * Evite qu'il surgisse quand une application donne le focus toute seule
     * (retour dans Termux, collage, changement d'ecran...).
     */
    var showOnlyOnTap: Boolean
        get() = sp.getBoolean("show_on_tap", true)
        set(v) { sp.edit().putBoolean("show_on_tap", v).apply() }

    /** Double appui sur un mot : le selectionner. */
    var doubleTapSelect: Boolean
        get() = sp.getBoolean("dbl_tap_select", true)
        set(v) { sp.edit().putBoolean("dbl_tap_select", v).apply() }

    /** Hauteur du clavier en pourcentage (redimensionnement global). */
    var keyboardScale: Int
        get() = sp.getInt("kb_scale", 100)
        set(v) { sp.edit().putInt("kb_scale", v).apply() }

    /** Clavier flottant, deplacable a l'ecran. */
    var floating: Boolean
        get() = sp.getBoolean("floating", false)
        set(v) { sp.edit().putBoolean("floating", v).apply() }

    /** Glisser le doigt sur les lettres pour ecrire un mot. */
    var swipeEnabled: Boolean
        get() = sp.getBoolean("swipe", true)
        set(v) { sp.edit().putBoolean("swipe", v).apply() }

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

    /** Barre du haut limitée aux actions principales, le reste passe dans ⋯. */
    var compactToolbar: Boolean
        get() = sp.getBoolean("compact_toolbar", true)
        set(v) { sp.edit().putBoolean("compact_toolbar", v).apply() }

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

    /**
     * En mode prive, bloquer aussi les outils en ligne demandes explicitement
     * (IA, traduction, GIF). Desactive par defaut : ne rien APPRENDRE est
     * indispensable, mais refuser une action que l'utilisateur demande lui-meme
     * est une gene inutile.
     */
    var privateBlocksOnline: Boolean
        get() = sp.getBoolean("private_blocks_online", false)
        set(v) { sp.edit().putBoolean("private_blocks_online", v).apply() }

    var incognitoFields: Boolean
        get() = sp.getBoolean("incognito", true)
        set(v) { sp.edit().putBoolean("incognito", v).apply() }

    /** Autorise les services web publics de secours quand aucune clé personnelle ne répond. */
    var allowPublicFallbacks: Boolean
        get() = sp.getBoolean("public_fallbacks", true)
        set(v) { sp.edit().putBoolean("public_fallbacks", v).apply() }

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
        get() = SecretStore.getOrMigrate(appContext, "ai_key", "ai_key")
        set(v) { SecretStore.put(appContext, "ai_key", v) }

    var aiBaseUrl: String
        get() = sp.getString("ai_base", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
        set(v) { sp.edit().putString("ai_base", v.trim()).apply() }

    var aiModel: String
        get() = sp.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(v) { sp.edit().putString("ai_model", v.trim()).apply() }

    var deeplKey: String
        get() = SecretStore.getOrMigrate(appContext, "deepl_key", "deepl_key")
        set(v) { SecretStore.put(appContext, "deepl_key", v) }

    var googleTranslateKey: String
        get() = SecretStore.getOrMigrate(appContext, "gtrans_key", "gtrans_key")
        set(v) { SecretStore.put(appContext, "gtrans_key", v) }

    /** 0 = Giphy, 1 = Klipy (Tenor a ferme le 30 juin 2026). */
    var gifProvider: Int
        get() = sp.getInt("gif_provider", 0)
        set(v) { sp.edit().putInt("gif_provider", v).apply() }

    var gifKey: String
        get() = SecretStore.getOrMigrate(appContext, "gif_key", "gif_key")
        set(v) { SecretStore.put(appContext, "gif_key", v) }

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

    /**
     * Enchainements a deux mots (trigrammes) : ce qui suit « mot1 mot2 ».
     * Bien plus precis qu'un seul mot de contexte.
     */
    fun learnTrigram(w1: String, w2: String, next: String) {
        if (!learningEnabled) return
        if (w1.length < 2 || w2.length < 2 || next.length < 2) return
        try {
            val key = w1.lowercase() + " " + w2.lowercase()
            val obj = JSONObject(sp.getString("trigrams", "{}") ?: "{}")
            val nexts = obj.optJSONObject(key) ?: JSONObject()
            nexts.put(next, nexts.optInt(next, 0) + 1)
            while (nexts.length() > 4) {
                val worst = nexts.keys().asSequence().minByOrNull { nexts.optInt(it, 0) } ?: break
                nexts.remove(worst)
            }
            obj.put(key, nexts)
            if (obj.length() > 4000) {
                obj.keys().asSequence().take(800).toList().forEach { obj.remove(it) }
            }
            sp.edit().putString("trigrams", obj.toString()).apply()
            trigramCache = null
        } catch (_: Exception) {
        }
    }

    /** Suites probables apres « w1 w2 ». */
    fun nextAfterTwo(w1: String, w2: String): List<String> {
        val key = w1.lowercase() + " " + w2.lowercase()
        trigramCache?.let { c -> c[key]?.let { return it } }
        val result = try {
            val obj = JSONObject(sp.getString("trigrams", "{}") ?: "{}")
            val nexts = obj.optJSONObject(key)
            if (nexts == null) emptyList()
            else nexts.keys().asSequence()
                .map { it to nexts.optInt(it, 0) }
                .sortedByDescending { it.second }
                .map { it.first }
                .toList()
        } catch (e: Exception) {
            emptyList<String>()
        }
        val cache = trigramCache ?: HashMap<String, List<String>>().also { trigramCache = it }
        if (cache.size > 300) cache.clear()
        cache[key] = result
        return result
    }

    /** Apprentissage en masse des trigrammes (import de SMS, de textes...). */
    fun learnTrigramsBulk(map: Map<String, Map<String, Int>>) {
        if (map.isEmpty()) return
        try {
            val obj = JSONObject(sp.getString("trigrams", "{}") ?: "{}")
            for ((key, nexts) in map) {
                val cur = obj.optJSONObject(key) ?: JSONObject()
                for ((w, c) in nexts) cur.put(w, cur.optInt(w, 0) + c)
                while (cur.length() > 4) {
                    val worst = cur.keys().asSequence().minByOrNull { cur.optInt(it, 0) } ?: break
                    cur.remove(worst)
                }
                obj.put(key, cur)
            }
            if (obj.length() > 6000) {
                obj.keys().asSequence().take(1500).toList().forEach { obj.remove(it) }
            }
            sp.edit().putString("trigrams", obj.toString()).apply()
            trigramCache = null
        } catch (_: Exception) {
        }
    }

    fun trigramCount(): Int = try {
        JSONObject(sp.getString("trigrams", "{}") ?: "{}").length()
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
        sp.edit().remove("freq").remove("bigrams").remove("trigrams").remove("learned").apply()
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
