package com.habhub.android.util

import java.time.LocalDate
import java.time.LocalDateTime

fun businessDate(dayBoundaryHour: Int, now: LocalDateTime = LocalDateTime.now()): LocalDate {
    return if (now.hour < dayBoundaryHour.coerceIn(0, 23)) now.toLocalDate().minusDays(1) else now.toLocalDate()
}
