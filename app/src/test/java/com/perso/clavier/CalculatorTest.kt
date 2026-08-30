package com.perso.clavier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {
    @Test fun multiplication() = assertEquals("540", Calculator.eval("12*45"))
    @Test fun precedence() = assertEquals("14", Calculator.eval("2+3*4"))
    @Test fun parentheses() = assertEquals("20", Calculator.eval("(2+3)*4"))
    @Test fun frenchDecimal() = assertEquals("15.5", Calculator.eval("12,5+3"))
    @Test fun unicodeOperators() = assertEquals("6", Calculator.eval("18÷3"))
    @Test fun negativeValue() = assertEquals("-6", Calculator.eval("-2*3"))
    @Test fun divisionByZeroIsRejected() = assertNull(Calculator.eval("1/0"))
    @Test fun plainNumberIsNotAFormula() = assertNull(Calculator.eval("123"))
}
