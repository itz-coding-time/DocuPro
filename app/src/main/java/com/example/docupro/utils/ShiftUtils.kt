package com.example.docupro.utils

import java.time.LocalDateTime
import java.time.LocalTime

object ShiftUtils {

    fun getShiftStart(shiftStartStr: String): LocalDateTime {
        val startTime = try { LocalTime.parse(shiftStartStr) } catch (_: Exception) { LocalTime.now() }
        val now = LocalDateTime.now()

        // If it's early morning (e.g. 2 AM) and the shift started at 10 PM, the start time was yesterday
        return if (now.toLocalTime().isBefore(startTime) && startTime.hour > 12) {
            LocalDateTime.of(now.toLocalDate().minusDays(1), startTime)
        } else {
            LocalDateTime.of(now.toLocalDate(), startTime)
        }
    }

    fun getShiftEnd(shiftStartStr: String, shiftEndStr: String): LocalDateTime {
        val start = getShiftStart(shiftStartStr)
        val endTime = try { LocalTime.parse(shiftEndStr) } catch (_: Exception) { LocalTime.now().plusHours(8) }

        var end = LocalDateTime.of(start.toLocalDate(), endTime)

        // If end time is technically before start time (e.g. 06:00 is before 22:00), it rolled over to the next day
        if (endTime.isBefore(start.toLocalTime())) {
            end = end.plusDays(1)
        }
        return end
    }

    fun isCurrentlyInShift(startStr: String, endStr: String): Boolean {
        val now = LocalDateTime.now()
        val start = getShiftStart(startStr)
        val end = getShiftEnd(startStr, endStr)

        // Return true if current time is within the shift block
        return !now.isBefore(start) && !now.isAfter(end)
    }
}