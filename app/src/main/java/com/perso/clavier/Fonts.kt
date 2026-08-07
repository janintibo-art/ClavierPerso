package com.perso.clavier

import android.graphics.Typeface

/** Polices disponibles pour les touches. */
object Fonts {

    val names = listOf(
        "Standard", "Sans condensé", "Serif", "Monospace",
        "Léger", "Gras", "Italique", "Serif gras"
    )

    fun get(index: Int): Typeface = when (index) {
        1 -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        2 -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        3 -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        4 -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        5 -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        6 -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        7 -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        else -> Typeface.DEFAULT
    }
}
