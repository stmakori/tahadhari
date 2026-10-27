package ke.tahadhari.ui.mobile

import ke.tahadhari.domain.Dates
import ke.tahadhari.ui.Strings

/** The two "Niko salama" check-in messages. */
enum class CheckInKind { SAFE, NEED_HELP }

/**
 * Builds the short message a user shares, e.g.
 * "Niko salama. Kaunti ya Kisumu, 27 Oktoba 14:30. — kupitia Tahadhari".
 * Pure: pass the clock in. Time is Nairobi local time (UTC+3, no daylight saving).
 *
 * @param countyName county name without "County", or null if none is chosen yet.
 */
fun checkInMessage(kind: CheckInKind, countyName: String?, epochMillis: Long, strings: Strings): String {
    val place = countyName?.let(strings.countyLabel)
    val time = checkInTimeText(epochMillis, strings)
    return when (kind) {
        CheckInKind.SAFE -> strings.checkInSafeMessage(place, time)
        CheckInKind.NEED_HELP -> strings.checkInHelpMessage(place, time)
    }
}

/** "27 Oct 14:30" / "27 Oktoba 14:30" in Nairobi time. */
fun checkInTimeText(epochMillis: Long, strings: Strings): String {
    val iso = Dates.isoDateFromEpochMillis(epochMillis)
    val (_, month, day) = iso.split("-").map { it.toInt() }
    val minuteOfDay = (epochMillis + Dates.NAIROBI_UTC_OFFSET_MINUTES * 60_000L).floorDiv(60_000L).mod(1440L).toInt()
    val hhmm = "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"
    return strings.checkInTime(day, month, hhmm)
}
