package com.perso.clavier

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object Calculator {

    /** Évalue "12*45", "12,5+3", "(2+3)*4"… Renvoie null si ce n'est pas un calcul valide. */
    fun eval(expr: String): String? {
        val clean = expr
            .replace('×', '*')
            .replace('÷', '/')
            .replace(',', '.')
            .replace(" ", "")
        if (clean.isEmpty()) return null
        if (!clean.any { it in "+-*/" }) return null
        if (!clean.any { it.isDigit() }) return null
        if (clean.last() in "+-*/(") return null
        return try {
            val v = Parser(clean).parse()
            when {
                v.isNaN() || v.isInfinite() -> null
                v == floor(v) && abs(v) < 1e15 -> v.toLong().toString()
                else -> String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun parse(): Double {
            val v = expr()
            if (i < s.length) throw IllegalArgumentException("caractère inattendu")
            return v
        }

        private fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val t = term()
                v = if (op == '+') v + t else v - t
            }
            return v
        }

        private fun term(): Double {
            var v = factor()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]
                val f = factor()
                v = if (op == '*') v * f else v / f
            }
            return v
        }

        private fun factor(): Double {
            if (i < s.length && s[i] == '-') {
                i++
                return -factor()
            }
            if (i < s.length && s[i] == '(') {
                i++
                val v = expr()
                if (i < s.length && s[i] == ')') i++ else throw IllegalArgumentException("parenthèse")
                return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (start == i) throw IllegalArgumentException("nombre attendu")
            return s.substring(start, i).toDouble()
        }
    }
}
