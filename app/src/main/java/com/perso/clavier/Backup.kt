package com.perso.clavier

import android.content.Context
import org.json.JSONObject

/** Sauvegarde et restauration complete des reglages du clavier. */
object Backup {

    private const val VERSION = 1

    fun export(context: Context): String {
        val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)
        val root = JSONObject()
        root.put("_version", VERSION)
        root.put("_app", "Anarchie Clavier")
        val data = JSONObject()
        for ((k, v) in sp.all) {
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
            val root = JSONObject(text)
            val data = root.optJSONObject("data") ?: return -1
            val sp = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)
            val e = sp.edit()
            var n = 0
            for (key in data.keys()) {
                when (val v = data.get(key)) {
                    is String -> e.putString(key, v)
                    is Int -> e.putInt(key, v)
                    is Boolean -> e.putBoolean(key, v)
                    is Long -> e.putInt(key, v.toInt())
                    is Double -> e.putInt(key, v.toInt())
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
                "clés API et toutes les options."
    }
}
