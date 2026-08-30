package com.perso.clavier

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stockage local des clés privées avec Android Keystore (AES/GCM).
 *
 * Les secrets ne sont plus conservés en clair dans les préférences principales,
 * ce qui évite aussi qu'ils se retrouvent dans les sauvegardes JSON ordinaires.
 */
object SecretStore {
    private const val STORE = "clavier_secrets"
    private const val KEY_ALIAS = "anarchie_clavier_api_keys_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun put(context: Context, name: String, value: String) {
        val trimmed = value.trim()
        val sp = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        if (trimmed.isEmpty()) {
            sp.edit().remove(name).apply()
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val payload = Base64.encodeToString(cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            sp.edit().putString(name, "$iv:$payload").apply()
        } catch (_: Exception) {
            // En cas d'erreur Keystore, on n'écrit volontairement pas le secret en clair.
        }
    }

    fun get(context: Context, name: String): String {
        val sp = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val encoded = sp.getString(name, null) ?: return ""
        return try {
            val parts = encoded.split(':', limit = 2)
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val data = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    /** Migre silencieusement une ancienne clé stockée en clair dans les préférences v35. */
    fun getOrMigrate(context: Context, secureName: String, legacyName: String): String {
        val secured = get(context, secureName)
        if (secured.isNotEmpty()) return secured
        val legacy = context.getSharedPreferences("clavier", Context.MODE_PRIVATE)
            .getString(legacyName, "")?.trim().orEmpty()
        if (legacy.isNotEmpty()) {
            put(context, secureName, legacy)
            context.getSharedPreferences("clavier", Context.MODE_PRIVATE)
                .edit().remove(legacyName).apply()
        }
        return legacy
    }
}
