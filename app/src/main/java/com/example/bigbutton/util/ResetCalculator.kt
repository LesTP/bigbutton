package com.example.bigbutton.util

import java.util.Calendar

/**
 * Utility object for calculating widget reset timing.
 *
 * Reset logic:
 * - lastChanged is the anchor timestamp (when user last marked Done)
 * - nextResetDateTime = date of lastChanged + periodDays, at resetHour:resetMinute
 * - If current time >= nextResetDateTime, reset is due
 */
object ResetCalculator {

    /**
     * Determines if a reset is due based on current state.
     *
     * @param lastChanged Timestamp (epoch ms) of last state change
     * @param periodDays Number of days in the reset period
     * @param resetHour Hour of day (0-23) when reset should occur
     * @param resetMinute Minute of hour (0-59) when reset should occur
     * @return true if reset is due, false otherwise
     */
    fun shouldReset(lastChanged: Long, periodDays: Int, resetHour: Int, resetMinute: Int): Boolean {
        if (lastChanged == 0L) {
            // Never been set, no reset needed
            return false
        }

        val nextResetTime = calculateNextResetTime(lastChanged, periodDays, resetHour, resetMinute)
        return System.currentTimeMillis() >= nextResetTime
    }

    /**
     * Calculates the next reset time based on last change.
     *
     * The reset time defines a daily boundary. If lastChanged is before the reset time
     * on its day, the "logical day" is considered the previous day. This ensures that
     * marking done shortly before reset time will trigger reset at that upcoming time,
     * not a full period later.
     *
     * @param lastChanged Timestamp (epoch ms) of last state change
     * @param periodDays Number of days in the reset period
     * @param resetHour Hour of day (0-23) when reset should occur
     * @param resetMinute Minute of hour (0-59) when reset should occur
     * @return Timestamp (epoch ms) of next reset time
     */
    fun calculateNextResetTime(lastChanged: Long, periodDays: Int, resetHour: Int, resetMinute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = lastChanged

            // Set to the reset time on the day of lastChanged
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val resetTimeOnSameDay = calendar.timeInMillis

        // If lastChanged was BEFORE the reset time on that day,
        // the action belongs to the "previous logical day", so we add (periodDays - 1).
        // If lastChanged was AFTER the reset time, add full periodDays.
        if (lastChanged < resetTimeOnSameDay) {
            calendar.add(Calendar.DAY_OF_YEAR, periodDays - 1)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, periodDays)
        }

        return calendar.timeInMillis
    }

    /**
     * Calculates the next occurrence of reset time from now.
     * Used when scheduling alarms.
     *
     * @param resetHour Hour of day (0-23) when reset should occur
     * @param resetMinute Minute of hour (0-59) when reset should occur
     * @return Timestamp (epoch ms) of next occurrence of reset time
     */
    fun calculateNextResetTimeFromNow(resetHour: Int, resetMinute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If this time has already passed today, move to tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * Calculates the start of the current period.
     * Used when deleting completion events on manual reset.
     *
     * @param currentTime Current timestamp (epoch ms)
     * @param periodDays Number of days in the reset period
     * @param resetHour Hour of day (0-23) when reset occurs
     * @param resetMinute Minute of hour (0-59) when reset occurs
     * @return Timestamp (epoch ms) of current period start
     */
    fun calculateCurrentPeriodStart(
        currentTime: Long,
        periodDays: Int,
        resetHour: Int,
        resetMinute: Int
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If current time is before today's reset time,
        // the period started yesterday at reset time
        if (currentTime < calendar.timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Walk back (periodDays - 1) more days to find period start
        calendar.add(Calendar.DAY_OF_YEAR, -(periodDays - 1))

        return calendar.timeInMillis
    }
}
