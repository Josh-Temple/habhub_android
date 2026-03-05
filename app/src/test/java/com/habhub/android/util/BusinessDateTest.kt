package com.habhub.android.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BusinessDateTest {

    @Test
    fun `uses same calendar date when boundary is midnight`() {
        val now = LocalDateTime.of(2026, 1, 10, 0, 30)

        val actual = businessDate(dayBoundaryHour = 0, now = now)

        assertEquals(LocalDate.of(2026, 1, 10), actual)
    }

    @Test
    fun `uses previous date before configured boundary hour`() {
        val now = LocalDateTime.of(2026, 1, 10, 2, 59)

        val actual = businessDate(dayBoundaryHour = 3, now = now)

        assertEquals(LocalDate.of(2026, 1, 9), actual)
    }

    @Test
    fun `uses current date from boundary hour and after`() {
        val now = LocalDateTime.of(2026, 1, 10, 3, 0)

        val actual = businessDate(dayBoundaryHour = 3, now = now)

        assertEquals(LocalDate.of(2026, 1, 10), actual)
    }

    @Test
    fun `coerces invalid boundary values`() {
        val now = LocalDateTime.of(2026, 1, 10, 23, 0)

        val actual = businessDate(dayBoundaryHour = 99, now = now)

        assertEquals(LocalDate.of(2026, 1, 10), actual)
    }
}
