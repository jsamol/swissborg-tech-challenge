package com.example.swissborg_tech_challange.util

import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class NumberTest {

    @Test
    fun `should round and scale BigDecimal`() {
        assertEquals(BigDecimal.valueOf(1.03), BigDecimal.valueOf(1.02623897).round())
        assertEquals(BigDecimal.valueOf(1.02), BigDecimal.valueOf(1.02423897).round())
        assertEquals(BigDecimal.valueOf(0.02423897), BigDecimal.valueOf(0.02423897).round())
        assertEquals(BigDecimal.valueOf(0.02423898), BigDecimal.valueOf(0.024238975).round())
        assertEquals(BigDecimal.valueOf(0.02423897), BigDecimal.valueOf(0.024238974).round())

        assertEquals(BigDecimal.valueOf(-1.03), BigDecimal.valueOf(-1.02623897).round())
        assertEquals(BigDecimal.valueOf(-1.02), BigDecimal.valueOf(-1.02423897).round())
        assertEquals(BigDecimal.valueOf(-0.02423897), BigDecimal.valueOf(-0.02423897).round())
        assertEquals(BigDecimal.valueOf(-0.02423898), BigDecimal.valueOf(-0.024238975).round())
        assertEquals(BigDecimal.valueOf(-0.02423897), BigDecimal.valueOf(-0.024238974).round())
    }

    @Test
    fun `should round and scale BigDecimal with specified floating points`() {
        assertEquals(BigDecimal.valueOf(1.026), BigDecimal.valueOf(1.02623897).round(floatingPoints = 3))
        assertEquals(BigDecimal.valueOf(1.025), BigDecimal.valueOf(1.02453897).round(floatingPoints = 3))
        assertEquals(BigDecimal.valueOf(0.024), BigDecimal.valueOf(0.02423897).round(floatingPoints = 3))
        assertEquals(BigDecimal.valueOf(0.025), BigDecimal.valueOf(0.02483897).round(floatingPoints = 3))

        assertEquals(BigDecimal.valueOf(-1.026), BigDecimal.valueOf(-1.02623897).round(floatingPoints = 3))
        assertEquals(BigDecimal.valueOf(-1.025), BigDecimal.valueOf(-1.02453897).round(floatingPoints = 3))
        assertEquals(BigDecimal.valueOf(-0.024), BigDecimal.valueOf(-0.02423897).round(floatingPoints = 3))
        assertEquals(BigDecimal.valueOf(-0.025), BigDecimal.valueOf(-0.024838975).round(floatingPoints = 3))
    }

    @Test
    fun `should transform BigDecimal to String without an additional sign`() {
        assertEquals("1.03", BigDecimal.valueOf(1.03).toString())
        assertEquals("1.03", BigDecimal.valueOf(1.03).toString(withSign = false))
        assertEquals("-1.03", BigDecimal.valueOf(-1.03).toString())
        assertEquals("-1.03", BigDecimal.valueOf(-1.03).toString(withSign = false))
    }

    @Test
    fun `should transform BigDecimal to String with an additional sign`() {
        assertEquals("+1.03", BigDecimal.valueOf(1.03).toString(withSign = true))
        assertEquals("-1.03", BigDecimal.valueOf(-1.03).toString(withSign = true))
    }
}