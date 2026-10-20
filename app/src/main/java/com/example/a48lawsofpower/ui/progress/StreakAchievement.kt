package com.example.a48lawsofpower.ui.progress

import com.example.a48lawsofpower.data.ReadingDayEntity
import com.example.a48lawsofpower.data.STREAK_MIN_SECONDS
import com.example.a48lawsofpower.data.computeStats

/**
 * A streak day the reader has earned but has not yet been congratulated for.
 *
 * The overlay is shown for this value and for nothing else, which is what makes "once per
 * achievement, once per launch" fall out of the data instead of needing a UI flag: there is exactly
 * one of these outstanding at a time, and it stops being outstanding the moment [day] is written to
 * the settings store as acknowledged.
 *
 * @property streak the streak *including* [day], on the same rules the Profile screen reports, so
 *   the card and the figure directly beneath it can never disagree.
 * @property day the newest banked day - the acknowledgment key, and the reason a second card for
 *   the same day is impossible.
 * @property isFirstEver true when no achievement has ever been acknowledged on this device. The
 *   card says "STREAK UNLOCKED" for that one and only that one.
 */
data class StreakAchievement(
    val streak: Int,
    val day: Long,
    val isFirstEver: Boolean,
)

/**
 * What to say about a streak of this length.
 *
 * The voice is the app's own - court, discipline, compounding - and it deliberately never implies
 * the reader has fallen behind. There is no "don't break it!" copy here. Guilt is a bad way to
 * keep someone reading, and a shame-driven streak is exactly the mechanic that makes people quit
 * the app entirely.
 */
fun encouragementFor(streak: Int): String = when {
    streak >= 100 -> "A hundred unbroken days. The court does not forget this."
    streak >= 60 -> "Two months without a single day missed."
    streak >= 30 -> "A full month. Discipline, compounded."
    streak >= 21 -> "Three weeks. This is no longer an effort."
    streak >= 14 -> "A fortnight, unbroken."
    streak >= 7 -> "A full week of study."
    streak >= 3 -> "It is becoming a habit."
    streak >= 2 -> "Two in a row. The pattern holds."
    else -> "The streak begins."
}

/**
 * Folds the day rows into the one achievement the overlay is about, or null when there is none.
 *
 * This is deliberately *not* an event fired at the moment of crossing. A crossing is a thing that
 * happens in one process, in one session, and can be missed - the reader can leave the law before
 * the card lands, the process can die mid-animation, or the days themselves can arrive afterwards
 * by restore. Deriving it from the table instead means every one of those is still pending on the
 * next launch, and [lastCelebratedDay] is what stops the ones that were seen from showing again.
 *
 * Three rules, in the order they matter:
 *  - **Only today or yesterday.** The newest banked day has to be the anchor of a run that is still
 *    live. A day older than that belongs to a streak the Profile screen reports as zero, and
 *    congratulating on it would put a number on screen that the rest of the app contradicts.
 *  - **Only newer than the acknowledgment.** Null means nothing has ever been celebrated on this
 *    device. A [lastCelebratedDay] ahead of the newest row (a clock set backwards) stays quiet for
 *    the same reason: nothing new has been earned.
 *  - **The streak comes from [computeStats], with [STREAK_MIN_SECONDS]**, so the card, the Profile
 *    figure, and the week strip are three views of one fold rather than three opinions.
 */
fun pendingStreakAchievement(
    days: List<ReadingDayEntity>,
    today: Long,
    lastCelebratedDay: Long?,
): StreakAchievement? {
    val banked = days.filter { it.secondsRead >= STREAK_MIN_SECONDS }
    val newest = banked.maxOfOrNull { it.day } ?: return null

    if (newest < today - 1) return null
    if (lastCelebratedDay != null && newest <= lastCelebratedDay) return null

    val streak = computeStats(
        days = days.map { it.day },
        today = today,
        lawsByDay = days.associate { it.day to it.lawsRead },
        secondsByDay = days.associate { it.day to it.secondsRead },
        minSecondsPerDay = STREAK_MIN_SECONDS,
    ).currentStreak

    // Unreachable for a newest day of today or yesterday - both anchor a run - but a row dated into
    // the future does reach here, and a card announcing a streak of zero would be worse than none.
    if (streak <= 0) return null

    return StreakAchievement(
        streak = streak,
        day = newest,
        isFirstEver = lastCelebratedDay == null,
    )
}
