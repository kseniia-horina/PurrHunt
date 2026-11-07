package app.purrhunt.domain

import java.time.LocalDateTime

object ReminderTime {
    /** The next moment strictly after [now] on one of [days] (ISO, Monday = 1) at [hour]:[minute]. */
    fun next(now: LocalDateTime, days: Set<Int>, hour: Int, minute: Int): LocalDateTime? {
        if (days.isEmpty()) return null
        val today = now.toLocalDate().atTime(hour, minute)
        for (offset in 0..7) {
            val candidate = today.plusDays(offset.toLong())
            if (candidate.isAfter(now) && candidate.dayOfWeek.value in days) return candidate
        }
        return null
    }
}
