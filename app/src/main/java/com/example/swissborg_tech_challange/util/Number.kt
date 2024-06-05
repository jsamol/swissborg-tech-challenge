package com.example.swissborg_tech_challange.util

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.round(floatingPoints: Int? = null): BigDecimal {
    val scale = floatingPoints ?: if (this >= BigDecimal.ONE) 2 else 8
    return setScale(scale, RoundingMode.CEILING).stripTrailingZeros()
}

fun BigDecimal.toString(withSign: Boolean): String {
    val sign = if (this > BigDecimal.ZERO) "+" else ""
    return if (withSign) sign + toPlainString() else toPlainString()
}