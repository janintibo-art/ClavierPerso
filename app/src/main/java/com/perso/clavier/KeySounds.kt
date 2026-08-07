package com.perso.clavier

import android.media.AudioManager
import android.media.ToneGenerator

/** Sons de frappe synthetises (aucun fichier audio necessaire). */
object KeySounds {

    val names = listOf("Système", "Clic", "Mécanique", "Machine à écrire", "Bulle", "Doux", "Néon")

    private var tone: ToneGenerator? = null
    private var lastVolume = -1

    private fun generator(volume: Int): ToneGenerator? {
        if (tone == null || volume != lastVolume) {
            try {
                tone?.release()
            } catch (_: Exception) {
            }
            tone = try {
                ToneGenerator(AudioManager.STREAM_SYSTEM, volume.coerceIn(1, 100))
            } catch (e: Exception) {
                null
            }
            lastVolume = volume
        }
        return tone
    }

    /**
     * Joue le son choisi. [type] correspond a l'index dans [names].
     * Le type 0 (systeme) est gere par l'appelant via AudioManager.
     */
    fun play(type: Int, volume: Int, isSpecialKey: Boolean) {
        val g = generator(volume) ?: return
        try {
            when (type) {
                1 -> g.startTone(ToneGenerator.TONE_PROP_BEEP, 18)
                2 -> g.startTone(
                    if (isSpecialKey) ToneGenerator.TONE_CDMA_ABBR_ALERT
                    else ToneGenerator.TONE_CDMA_PIP, 22
                )
                3 -> g.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 30)
                4 -> g.startTone(ToneGenerator.TONE_PROP_ACK, 26)
                5 -> g.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 20)
                6 -> g.startTone(
                    if (isSpecialKey) ToneGenerator.TONE_CDMA_HIGH_SS
                    else ToneGenerator.TONE_CDMA_MED_SS, 24
                )
                else -> {}
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            tone?.release()
        } catch (_: Exception) {
        }
        tone = null
        lastVolume = -1
    }
}
