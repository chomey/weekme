package com.chomey.weekme.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.chomey.weekme.config.MonthPickerActivity
import com.chomey.weekme.data.CalendarEvent
import com.chomey.weekme.data.CalendarRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

val WeekOffsetKey = intPreferencesKey("week_offset")

class WeekMeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = CalendarRepository(context.contentResolver)

        provideContent {
            val weekOffset = currentState(WeekOffsetKey) ?: 0
            val today = LocalDate.now()
            val startDate = today.plusWeeks(weekOffset.toLong())

            val weekEvents = try {
                repo.getEventsForWeek(startDate)
            } catch (_: SecurityException) {
                emptyMap()
            }

            val orderedDays = (0 until 7).map { startDate.plusDays(it.toLong()) }

            GlanceTheme {
                WeekGrid(
                    today = today,
                    startDate = startDate,
                    orderedDays = orderedDays,
                    weekEvents = weekEvents,
                    weekOffset = weekOffset,
                )
            }
        }
    }
}

class PrevWeekAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[WeekOffsetKey] ?: 0
            prefs[WeekOffsetKey] = current - 1
        }
        WeekMeWidget().update(context, glanceId)
    }
}

class NextWeekAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[WeekOffsetKey] ?: 0
            prefs[WeekOffsetKey] = current + 1
        }
        WeekMeWidget().update(context, glanceId)
    }
}

class TodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WeekOffsetKey] = 0
        }
        WeekMeWidget().update(context, glanceId)
    }
}

val WeekOffsetParam = ActionParameters.Key<Int>("week_offset_param")

class OpenMonthPickerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val weekOffset = parameters[WeekOffsetParam] ?: 0
        val intent = Intent(context, MonthPickerActivity::class.java).apply {
            putExtra("EXTRA_WIDGET_ID", appWidgetId)
            putExtra("EXTRA_WEEK_OFFSET", weekOffset)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private val Blue = Color(0xFF1A73E8)
private val LightBlue = Color(0xFF8AB4F8)
private val TodayBgLight = Color(0xFFE8F0FE)
private val TodayBgDark = Color(0xFF1E3A5F)
private val CellBgLight = Color(0xFFF5F5F5)
private val CellBgDark = Color(0xFF2D2D2D)
private val DimLight = Color(0xFF9E9E9E)
private val DimDark = Color(0xFF757575)
private val NavBgLight = Color(0xFFE0E0E0)
private val NavBgDark = Color(0xFF3A3A3A)

@Composable
private fun WeekGrid(
    today: LocalDate,
    startDate: LocalDate,
    orderedDays: List<LocalDate>,
    weekEvents: Map<LocalDate, List<CalendarEvent>>,
    weekOffset: Int,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(4.dp),
    ) {
        // Navigation bar
        NavBar(startDate = startDate, weekOffset = weekOffset)

        Spacer(modifier = GlanceModifier.height(2.dp))

        // 4 rows of day cells
        for (row in 0 until 4) {
            if (row > 0) {
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
            ) {
                for (col in 0 until 2) {
                    if (col > 0) {
                        Spacer(modifier = GlanceModifier.width(2.dp))
                    }
                    val cellIndex = row * 2 + col

                    if (cellIndex < 7) {
                        val day = orderedDays[cellIndex]
                        val events = weekEvents[day] ?: emptyList()
                        DayCell(
                            date = day,
                            events = events,
                            isToday = day == today,
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                        )
                    } else {
                        MiniMonthCell(
                            today = today,
                            startDate = startDate,
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBar(
    startDate: LocalDate,
    weekOffset: Int,
) {
    val endDate = startDate.plusDays(6)
    val monthFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val label = if (startDate.month == endDate.month) {
        "${startDate.format(monthFmt)} – ${endDate.dayOfMonth}"
    } else {
        "${startDate.format(monthFmt)} – ${endDate.format(monthFmt)}"
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous week button
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .fillMaxHeight()
                .cornerRadius(8.dp)
                .background(ColorProvider(day = NavBgLight, night = NavBgDark))
                .clickable(actionRunCallback<PrevWeekAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "<",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.width(2.dp))

        // Today button
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .fillMaxHeight()
                .cornerRadius(8.dp)
                .background(ColorProvider(day = Blue, night = LightBlue))
                .clickable(actionRunCallback<TodayAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "TODAY",
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.Black),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.width(2.dp))

        // Week label (tap to open month picker)
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .cornerRadius(8.dp)
                .background(ColorProvider(day = CellBgLight, night = CellBgDark))
                .clickable(
                    actionRunCallback<OpenMonthPickerAction>(
                        actionParametersOf(WeekOffsetParam to weekOffset)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$label ▼",
                style = TextStyle(
                    color = if (weekOffset == 0) GlanceTheme.colors.onSurface
                        else ColorProvider(day = Blue, night = LightBlue),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.width(2.dp))

        // Next week button
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .fillMaxHeight()
                .cornerRadius(8.dp)
                .background(ColorProvider(day = NavBgLight, night = NavBgDark))
                .clickable(actionRunCallback<NextWeekAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ">",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    events: List<CalendarEvent>,
    isToday: Boolean,
    modifier: GlanceModifier,
) {
    val headerColor = if (isToday) {
        ColorProvider(day = Blue, night = LightBlue)
    } else {
        GlanceTheme.colors.onSurface
    }

    val cellBg = if (isToday) {
        ColorProvider(day = TodayBgLight, night = TodayBgDark)
    } else {
        ColorProvider(day = CellBgLight, night = CellBgDark)
    }

    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(
            "content://com.android.calendar/time/${
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }"
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Column(
        modifier = modifier
            .background(cellBg)
            .padding(6.dp)
            .clickable(actionStartActivity(calendarIntent)),
    ) {
        val dayName = date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
        val dayNum = date.dayOfMonth.toString()

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$dayNum $dayName",
                style = TextStyle(
                    color = headerColor,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                ),
            )
            if (events.size > 3) {
                Text(
                    text = " (${events.size})",
                    style = TextStyle(
                        color = ColorProvider(day = DimLight, night = DimDark),
                        fontSize = 9.sp,
                    ),
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        LazyColumn(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
        ) {
            items(events, itemId = { it.id }) { event ->
                EventRow(event)
            }
            if (events.size > 3) {
                item {
                    Text(
                        text = "▼",
                        style = TextStyle(
                            color = ColorProvider(day = DimLight, night = DimDark),
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.getDefault())

    val displayText = when {
        event.isMultiDayContinuation -> "\u2504 ${event.title}"
        event.isAllDay -> event.title
        event.startTime != null -> {
            val time = event.startTime.format(timeFormatter).lowercase()
            "$time ${event.title}"
        }
        else -> event.title
    }

    val eventColor = if (event.color != 0) {
        ColorProvider(day = Color(event.color), night = Color(event.color))
    } else {
        GlanceTheme.colors.primary
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(12.dp)
                .background(eventColor),
        ) {}

        Text(
            text = displayText,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 10.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 3.dp),
        )
    }
}

@Composable
private fun MiniMonthCell(
    today: LocalDate,
    startDate: LocalDate,
    modifier: GlanceModifier,
) {
    // Show the month of the currently viewed week's start
    val refDate = startDate
    val monthStart = refDate.withDayOfMonth(1)
    val daysInMonth = refDate.lengthOfMonth()
    val firstDayOffset = if (monthStart.dayOfWeek == DayOfWeek.SUNDAY) 0
        else monthStart.dayOfWeek.value
    val monthName = refDate.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())

    val headerColor = GlanceTheme.colors.onSurface
    val todayColor = ColorProvider(day = Blue, night = LightBlue)
    val dimColor = ColorProvider(day = DimLight, night = DimDark)

    Column(
        modifier = modifier
            .background(ColorProvider(day = CellBgLight, night = CellBgDark))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$monthName ${refDate.year}",
            style = TextStyle(
                color = headerColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Spacer(modifier = GlanceModifier.height(2.dp))

        val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            for (header in dayHeaders) {
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = header,
                        style = TextStyle(
                            color = dimColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(1.dp))

        val totalCells = firstDayOffset + daysInMonth
        val numRows = (totalCells + 6) / 7

        for (row in 0 until numRows) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellNum = row * 7 + col - firstDayOffset + 1
                    Box(
                        modifier = GlanceModifier.defaultWeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cellNum in 1..daysInMonth) {
                            val isCurrentDay = cellNum == today.dayOfMonth &&
                                refDate.month == today.month && refDate.year == today.year
                            Text(
                                text = cellNum.toString(),
                                style = TextStyle(
                                    color = if (isCurrentDay) todayColor else headerColor,
                                    fontSize = 8.sp,
                                    fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
