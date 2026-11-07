package app.purrhunt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ClockTest {
    @Test
    fun `next reminder later today`() {
        val now = LocalDateTime.of(2026, 11, 2, 10, 0) // Monday
        assertEquals(LocalDateTime.of(2026, 11, 2, 18, 0), ReminderTime.next(now, setOf(1), 18, 0))
    }

    @Test
    fun `next reminder skips to the next selected day`() {
        val now = LocalDateTime.of(2026, 11, 2, 19, 0) // Monday, after 18:00
        assertEquals(LocalDateTime.of(2026, 11, 4, 18, 0), ReminderTime.next(now, setOf(1, 3), 18, 0))
        assertEquals(LocalDateTime.of(2026, 11, 9, 18, 0), ReminderTime.next(now, setOf(1), 18, 0))
        assertNull(ReminderTime.next(now, emptySet(), 18, 0))
    }

    @Test
    fun `night resolution`() {
        assertTrue(NightClock.isNight(23))
        assertTrue(NightClock.isNight(3))
        assertFalse(NightClock.isNight(12))
        assertFalse(NightClock.resolveNight(Location.BASEMENT, true, NightMode.ALWAYS, 23))
        assertTrue(NightClock.resolveNight(Location.MEADOW, false, NightMode.AUTO, 23))
        assertFalse(NightClock.resolveNight(Location.MEADOW, true, NightMode.NEVER, 23))
        assertTrue(NightClock.resolveNight(Location.MEADOW, false, NightMode.ALWAYS, 12))
    }

    @Test
    fun `seasons by month`() {
        assertEquals(Season.WINTER, Season.ofMonth(1))
        assertEquals(Season.AUTUMN, Season.ofMonth(10))
        assertEquals(Season.SUMMER, Season.ofMonth(7))
    }

    @Test
    fun `heatmap round trip`() {
        val cells = IntArray(HeatmapGrid.COLS * HeatmapGrid.ROWS)
        cells[HeatmapGrid.cellIndex(0.99f, 0.99f)] = 3
        cells[HeatmapGrid.cellIndex(0f, 0f)] = 1
        val decoded = HeatmapGrid.decode(HeatmapGrid.encode(cells))
        assertEquals(cells.toList(), decoded.toList())
        assertEquals(6, HeatmapGrid.sum(listOf(decoded, decoded))[cells.size - 1])
    }
}
