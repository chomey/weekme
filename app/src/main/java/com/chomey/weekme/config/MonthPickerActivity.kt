package com.chomey.weekme.config

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.chomey.weekme.widget.WeekMeWidget
import com.chomey.weekme.widget.WeekOffsetKey
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MonthPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra("EXTRA_WIDGET_ID", -1)
        val currentWeekOffset = intent.getIntExtra("EXTRA_WEEK_OFFSET", 0)

        if (appWidgetId == -1) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MonthPickerScreen(
                        currentWeekOffset = currentWeekOffset,
                        onWeekSelected = { newOffset ->
                            selectWeek(appWidgetId, newOffset)
                        },
                    )
                }
            }
        }
    }

    private fun selectWeek(appWidgetId: Int, weekOffset: Int) {
        val context = this
        MainScope().launch {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[WeekOffsetKey] = weekOffset
            }
            WeekMeWidget().update(context, glanceId)
            finish()
        }
    }
}

private val GoogleBlue = Color(0xFF1A73E8)
private val WeekHighlightBg = Color(0xFFE8F0FE)

@Composable
private fun MonthPickerScreen(
    currentWeekOffset: Int,
    onWeekSelected: (Int) -> Unit,
) {
    val today = LocalDate.now()
    val currentWidgetStart = today.plusWeeks(currentWeekOffset.toLong())

    var displayedMonth by remember {
        mutableStateOf(YearMonth.from(currentWidgetStart))
    }
    var showYearPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Month/Year header with navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {
                displayedMonth = displayedMonth.minusMonths(1)
            }) {
                Text("◀", fontSize = 20.sp)
            }

            Text(
                text = "${displayedMonth.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showYearPicker = true },
            )

            TextButton(onClick = {
                displayedMonth = displayedMonth.plusMonths(1)
            }) {
                Text("▶", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val headers = listOf("S", "M", "T", "W", "T", "F", "S")
            for (header in headers) {
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val monthStart = displayedMonth.atDay(1)
        val daysInMonth = displayedMonth.lengthOfMonth()
        val firstDayOffset = if (monthStart.dayOfWeek == DayOfWeek.SUNDAY) 0
            else monthStart.dayOfWeek.value
        val totalCells = firstDayOffset + daysInMonth
        val numRows = (totalCells + 6) / 7

        for (row in 0 until numRows) {
            val rowStartDate = monthStart.minusDays(firstDayOffset.toLong()).plusDays((row * 7).toLong())

            // Highlight the row containing the widget's start date
            val isCurrentWidgetWeek = (0 until 7).any { col ->
                val cellDate = rowStartDate.plusDays(col.toLong())
                cellDate == currentWidgetStart
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCurrentWidgetWeek) WeekHighlightBg else Color.Transparent)
                    .clickable {
                        val representativeDate = (0 until 7)
                            .map { rowStartDate.plusDays(it.toLong()) }
                            .firstOrNull { YearMonth.from(it) == displayedMonth }
                            ?: rowStartDate

                        val offset = computeWeekOffset(today, representativeDate)
                        onWeekSelected(offset)
                    }
                    .padding(vertical = 8.dp),
            ) {
                for (col in 0 until 7) {
                    val cellDate = rowStartDate.plusDays(col.toLong())
                    val cellDayOfMonth = cellDate.dayOfMonth
                    val isInMonth = YearMonth.from(cellDate) == displayedMonth
                    val isToday = cellDate == today

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isToday && isInMonth) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GoogleBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = cellDayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        } else {
                            Text(
                                text = if (isInMonth) cellDayOfMonth.toString() else "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showYearPicker) {
        YearPickerDialog(
            currentYear = displayedMonth.year,
            onYearSelected = { year ->
                displayedMonth = displayedMonth.withYear(year)
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false },
        )
    }
}

/**
 * Compute the week offset such that today.plusWeeks(offset) falls in the
 * same calendar row (Sunday-Saturday) as the target date.
 */
private fun computeWeekOffset(today: LocalDate, targetDate: LocalDate): Int {
    val targetSunday = targetDate.minusDays(
        if (targetDate.dayOfWeek == DayOfWeek.SUNDAY) 0L
        else targetDate.dayOfWeek.value.toLong()
    )
    val todaySunday = today.minusDays(
        if (today.dayOfWeek == DayOfWeek.SUNDAY) 0L
        else today.dayOfWeek.value.toLong()
    )
    return ChronoUnit.WEEKS.between(todaySunday, targetSunday).toInt()
}

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val years = (currentYear - 10)..(currentYear + 10)
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        listState.scrollToItem(10)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year") },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier.height(300.dp),
            ) {
                items(years.toList()) { year ->
                    Text(
                        text = year.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onYearSelected(year) }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        fontSize = 18.sp,
                        fontWeight = if (year == currentYear) FontWeight.Bold else FontWeight.Normal,
                        color = if (year == currentYear) GoogleBlue else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {},
    )
}
