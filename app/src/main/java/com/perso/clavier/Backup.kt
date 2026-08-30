package com.perso.clavier

import android.content.Context
import org.json.JSONObject

/** Sauvegarde et restauration complete des reglages du clavier. */
object Backup {

    private const val VERSION = 1

    /** Données optionnelles et secrets qui ne doivent jamais sortir dans une sauvegarde normale. */
    private val optionalSkip = setOf("clips", "recent_emojis")
    private val alwaysSkip = setOf(
        // Compatibilité avec d'anciennes v35 : ne jamais exporter une ancienne clé encore en clair.
        "ai_key", "deepl_key", "gtrans_key", "gif_key"
    )

    fun export(context: Context, includeClips: Boolean = false): String {
        val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)
        val root = JSONObject()
        root.put("_version", VERSION)
        root.put("_app", "Anarchie Clavier")
        val data = JSONObject()
        for ((k, v) in sp.all) {
            if (k in alwaysSkip) continue
            if (k in optionalSkip && !includeClips) continue
            when (v) {
                is String -> data.put(k, v)
                is Int -> data.put(k, v)
                is Boolean -> data.put(k, v)
                is Long -> data.put(k, v)
                is Float -> data.put(k, v.toDouble())
                is Set<*> -> data.put(k, JSONObject().put("_set", v.joinToString("\u0001")))
                else -> {}
            }
        }
        root.put("data", data)
        return root.toString(2)
    }

    /** Renvoie le nombre de reglages restaures, ou -1 si le fichier est invalide. */
    fun import(context: Context, text: String): Int {
        return try {
            val cleaned = text.trim().let {
                if (it.startsWith("{")) it else it.substring(it.indexOf('{'))
            }
            val root = JSONObject(cleaned)
            val data = root.optJSONObject("data") ?: root
            val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)
            val e = sp.edit()
            var n = 0
            for (key in data.keys()) {
                val v = data.get(key)
                if (key in alwaysSkip && v is String) {
                    // Une ancienne sauvegarde v35 peut contenir les clés : on les migre
                    // directement vers le stockage Keystore, jamais vers les prefs en clair.
                    SecretStore.put(context, key, v)
                    e.remove(key)
                    n++
                    continue
                }
                when (v) {
                    is String -> e.putString(key, v)
                    is Int -> e.putInt(key, v)
                    is Boolean -> e.putBoolean(key, v)
                    is Long -> e.putLong(key, v)
                    is Double -> e.putFloat(key, v.toFloat())
                    is JSONObject -> {
                        val s = v.optString("_set")
                        if (s.isNotEmpty()) e.putStringSet(key, s.split("\u0001").toSet())
                    }
                    else -> {}
                }
                n++
            }
            e.apply()
            Prefs.invalidateCaches()
            n
        } catch (e: Exception) {
            -1
        }
    }

    fun summary(context: Context): String {
        val p = Prefs(context)
        return "Thème et couleurs, " + p.shortcuts().size + " raccourcis, " +
                p.wordCounts().size + " mots appris, " + p.clips().size + " éléments copiés, " +
                "et toutes les options. Les clés API privées ne sont jamais incluses."
    }
}
