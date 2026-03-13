package com.chomey.weekme.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CalendarRepository(private val contentResolver: ContentResolver) {

    fun getEventsForWeek(startDate: LocalDate): Map<LocalDate, List<CalendarEvent>> {
        val endDate = startDate.plusDays(7)
        val zone = ZoneId.systemDefault()

        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .let { ContentUris.appendId(it, startMillis) }
            .let { ContentUris.appendId(it, endMillis) }
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val events = mutableListOf<CalendarEvent>()

        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            while (cursor.moveToNext()) {
                val event = cursorToEvent(cursor, zone, startDate, endDate)
                if (event != null) {
                    events.add(event)
                }
            }
        }

        // Expand multi-day events into per-day entries
        val dayMap = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
        for (i in 0L until 7L) {
            dayMap[startDate.plusDays(i)] = mutableListOf()
        }

        for (event in events) {
            val eventStart = event.startDate
            val eventEnd = event.endDate

            var day = maxOf(eventStart, startDate)
            val lastDay = minOf(eventEnd, endDate.minusDays(1))

            while (!day.isAfter(lastDay)) {
                if (dayMap.containsKey(day)) {
                    val isStart = day == eventStart
                    dayMap[day]!!.add(
                        event.copy(
                            isMultiDayStart = isStart && eventStart != eventEnd,
                            isMultiDayContinuation = !isStart,
                        )
                    )
                }
                day = day.plusDays(1)
            }
        }

        return dayMap
    }

    private fun cursorToEvent(
        cursor: Cursor,
        zone: ZoneId,
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): CalendarEvent? {
        val eventId = cursor.getLong(0)
        val title = cursor.getString(1) ?: "(No title)"
        val beginMillis = cursor.getLong(2)
        val endMillis = cursor.getLong(3)
        val allDay = cursor.getInt(4) == 1
        val color = cursor.getInt(5)
        val calendarId = cursor.getLong(6)

        val startInstant = Instant.ofEpochMilli(beginMillis)
        val endInstant = Instant.ofEpochMilli(endMillis)

        val startDate: LocalDate
        val endDate: LocalDate
        val startTime: LocalTime?
        val endTime: LocalTime?

        if (allDay) {
            // All-day events are stored in UTC
            startDate = startInstant.atZone(ZoneId.of("UTC")).toLocalDate()
            endDate = endInstant.atZone(ZoneId.of("UTC")).toLocalDate().minusDays(1)
            startTime = null
            endTime = null
        } else {
            val startZoned = startInstant.atZone(zone)
            val endZoned = endInstant.atZone(zone)
            startDate = startZoned.toLocalDate()
            endDate = endZoned.toLocalDate()
            startTime = startZoned.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
            endTime = endZoned.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
        }

        return CalendarEvent(
            id = eventId,
            title = title,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            isAllDay = allDay,
            color = color,
            calendarId = calendarId,
        )
    }
}
