package ar.trenar.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Argentine national holidays (feriados nacionales), per Ley 27.399. Covers the fixed
 * (inamovibles) dates, the Easter-based ones (Viernes Santo + Carnaval), and the movable
 * (trasladables) ones with the Monday-shift rule. On these days the trains run the
 * Sunday/holiday timetable. "Días no laborables" and tourism bridges are intentionally
 * excluded (they are not feriados; normal service runs).
 */
object ArHolidays {

    // (month, day) of the fixed, non-moving holidays
    private val fixed = setOf(
        1 to 1,    // Año Nuevo
        3 to 24,   // Día de la Memoria
        4 to 2,    // Malvinas
        5 to 1,    // Día del Trabajador
        5 to 25,   // Revolución de Mayo
        6 to 20,   // Belgrano (inamovible)
        7 to 9,    // Independencia
        12 to 8,   // Inmaculada Concepción
        12 to 25,  // Navidad
    )

    // base (month, day) of the trasladables (may shift to a nearby Monday)
    private val movableBases = listOf(
        6 to 17,   // Güemes
        8 to 17,   // San Martín
        10 to 12,  // Diversidad Cultural
        11 to 20,  // Soberanía Nacional
    )

    fun isHoliday(date: LocalDate): Boolean {
        if ((date.monthValue to date.dayOfMonth) in fixed) return true

        val easter = easterSunday(date.year)
        if (date == easter.minusDays(2)) return true   // Viernes Santo
        if (date == easter.minusDays(48)) return true  // Carnaval (lunes)
        if (date == easter.minusDays(47)) return true  // Carnaval (martes)

        return movableBases.any { (m, d) -> observed(LocalDate.of(date.year, m, d)) == date }
    }

    /** Where a trasladable holiday is actually observed (Ley 27.399 Monday-shift rule). */
    private fun observed(base: LocalDate): LocalDate = when (base.dayOfWeek) {
        DayOfWeek.TUESDAY -> base.minusDays(1)    // -> previous Monday
        DayOfWeek.WEDNESDAY -> base.minusDays(2)  // -> previous Monday
        DayOfWeek.THURSDAY -> base.plusDays(4)    // -> next Monday
        DayOfWeek.FRIDAY -> base.plusDays(3)      // -> next Monday
        else -> base                              // Mon/Sat/Sun stay
    }

    /** Easter Sunday via the anonymous Gregorian algorithm (Meeus/Jones/Butcher). */
    private fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val mm = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * mm + 114) / 31
        val day = ((h + l - 7 * mm + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
