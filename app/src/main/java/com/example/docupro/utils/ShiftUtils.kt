package com.example.docupro.utils

import java.time.LocalDateTime
import java.time.LocalTime

object ShiftUtils {
    fun getShiftStart(shiftStartTime: String): LocalDateTime {
        val now = LocalDateTime.now()
        val time = try { LocalTime.parse(shiftStartTime) } catch (e: Exception) { LocalTime.of(21, 0) }
        var start = now.with(time)
        // If it's 2 AM and shift started at 9 PM, shift start was yesterday
        if (now.toLocalTime().isBefore(time) && time.hour > 12) {
            start = start.minusDays(1)
        }
        return start
    }

    fun getShiftEnd(shiftStartTime: String, shiftEndTime: String): LocalDateTime {
        val start = getShiftStart(shiftStartTime)
        val end = try { LocalTime.parse(shiftEndTime) } catch (e: Exception) { LocalTime.of(7, 30) }
        var endDateTime = start.with(end)
        // If shift ends at 7 AM but started at 9 PM, end time is tomorrow relative to start
        if (end.isBefore(LocalTime.parse(shiftStartTime))) {
            endDateTime = endDateTime.plusDays(1)
        }
        return endDateTime
    }
}