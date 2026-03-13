package com.chomey.weekme.data

import java.time.LocalDate
import java.time.LocalTime

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val color: Int,
    val calendarId: Long,
    val isMultiDayStart: Boolean = false,
    val isMultiDayContinuation: Boolean = false,
)
