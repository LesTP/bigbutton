package com.example.bigbutton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigbutton.data.BigButtonDatabase
import com.example.bigbutton.data.TrackingMetadata
import com.example.bigbutton.util.ResetCalculator
import com.example.bigbutton.widget.BigButtonStateDefinition
import com.example.bigbutton.widget.dataStore
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.ZoneId

// Colors for day status
private val ColorCompleted = Color(0xFF4CAF50)  // Green
private val ColorMissed = Color(0xFFF44336)      // Red
private val ColorInProgress = Color(0xFF9E9E9E) // Grey

// Date range to display (past and future weeks from today)
private const val WEEKS_BEFORE = 52  // ~1 year back
private const val WEEKS_AFTER = 4    // ~1 month forward

/**
 * Represents the status of a day for calendar coloring.
 */
private enum class DayStatus {
    COMPLETED,    // Green - finalized as done
    MISSED,       // Red - finalized as not done
    IN_PROGRESS,  // Grey - current period, not yet finalized
    NO_DATA       // Transparent - before tracking, future, or gap
}

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Load settings from DataStore
    val periodDays by context.dataStore.data
        .map { it[BigButtonStateDefinition.Keys.PERIOD_DAYS] ?: BigButtonStateDefinition.DEFAULT_PERIOD_DAYS }
        .collectAsState(initial = BigButtonStateDefinition.DEFAULT_PERIOD_DAYS)

    val resetHour by context.dataStore.data
        .map { it[BigButtonStateDefinition.Keys.RESET_HOUR] ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR }
        .collectAsState(initial = BigButtonStateDefinition.DEFAULT_RESET_HOUR)

    val resetMinute by context.dataStore.data
        .map { it[BigButtonStateDefinition.Keys.RESET_MINUTE] ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE }
        .collectAsState(initial = BigButtonStateDefinition.DEFAULT_RESET_MINUTE)

    // Load data from Room database
    var trackingStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var finalizedDays by remember { mutableStateOf<Map<LocalDate, Boolean>>(emptyMap()) }

    // Calculate date range for display
    val today = LocalDate.now()
    val startDate = today.minusWeeks(WEEKS_BEFORE.toLong())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val endDate = today.plusWeeks(WEEKS_AFTER.toLong())
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    // Load database data
    LaunchedEffect(Unit) {
        val db = BigButtonDatabase.getDatabase(context)
        val dao = db.bigButtonDao()

        // Load tracking start date
        val startDateStr = dao.getMetadata(TrackingMetadata.KEY_TRACKING_START_DATE)
        trackingStartDate = startDateStr?.let { LocalDate.parse(it) }

        // Load finalized days for the visible range
        val days = dao.getDaysInRange(startDate.toString(), endDate.toString())
        finalizedDays = days.associate { LocalDate.parse(it.date) to it.completed }
    }

    // Calculate current period boundaries
    val currentPeriodStart = remember(periodDays, resetHour, resetMinute) {
        val startMillis = ResetCalculator.calculateCurrentPeriodStart(
            System.currentTimeMillis(),
            periodDays,
            resetHour,
            resetMinute
        )
        java.time.Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    // Generate list of weeks
    val weeks = remember(startDate, endDate) {
        generateWeeks(startDate, endDate)
    }

    // Find index of current week for initial scroll
    val currentWeekIndex = remember(weeks, today) {
        weeks.indexOfFirst { week ->
            week.any { it == today }
        }.coerceAtLeast(0)
    }

    // Scroll so that (current week + 1) aligns to bottom of viewport
    // This ensures current week is visible with past weeks above and future weeks below
    LaunchedEffect(currentWeekIndex) {
        if (currentWeekIndex > 0) {
            // Target item: the week after current week
            val targetWeekIndex = currentWeekIndex + 1

            // Count month headers before target week (weeks containing the 1st of a month)
            val monthHeadersBeforeTarget = weeks.take(targetWeekIndex).count { week ->
                week.any { it.dayOfMonth == 1 }
            }

            // Actual item index in LazyColumn = week index + month headers
            val actualTargetItemIndex = targetWeekIndex + monthHeadersBeforeTarget

            // First scroll to target to ensure layout is measured
            listState.scrollToItem(actualTargetItemIndex)

            // Now adjust to align target to bottom of viewport
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

            // Calculate average item height from visible items
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val avgItemHeight = visibleItems.map { it.size }.average().toInt()
                if (avgItemHeight > 0) {
                    // Calculate how many items fit in viewport
                    val itemsPerViewport = viewportHeight / avgItemHeight
                    // Scroll to index that puts target week near bottom
                    val scrollToIndex = (actualTargetItemIndex - itemsPerViewport + 2).toInt().coerceAtLeast(0)
                    listState.scrollToItem(scrollToIndex)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Day-of-week header (sticky)
        DayOfWeekHeader()

        // Scrollable calendar
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            weeks.forEachIndexed { index, week ->
                // Add month header if this week starts a new month
                val firstDayOfWeek = week.first()
                val showMonthHeader = index == 0 ||
                    firstDayOfWeek.dayOfMonth <= 7 && firstDayOfWeek.dayOfMonth == week.minOf {
                        if (it.month == firstDayOfWeek.month) it.dayOfMonth else Int.MAX_VALUE
                    }

                // Check if any day in this week is the 1st of a month
                val monthStartDay = week.find { it.dayOfMonth == 1 }
                if (monthStartDay != null) {
                    item(key = "month_${monthStartDay}") {
                        MonthHeader(date = monthStartDay)
                    }
                }

                item(key = "week_$index") {
                    WeekRow(
                        week = week,
                        today = today,
                        trackingStartDate = trackingStartDate,
                        currentPeriodStart = currentPeriodStart,
                        finalizedDays = finalizedDays
                    )
                }
            }
        }
    }
}

/**
 * Generates a list of weeks (each week is a list of 7 LocalDates).
 */
private fun generateWeeks(startDate: LocalDate, endDate: LocalDate): List<List<LocalDate>> {
    val weeks = mutableListOf<List<LocalDate>>()
    var currentDate = startDate

    while (currentDate <= endDate) {
        val week = (0 until 7).map { currentDate.plusDays(it.toLong()) }
        weeks.add(week)
        currentDate = currentDate.plusWeeks(1)
    }

    return weeks
}

/**
 * Header row showing day-of-week labels.
 */
@Composable
private fun DayOfWeekHeader() {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Month header displayed inline in the calendar.
 */
@Composable
private fun MonthHeader(date: LocalDate) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Text(
        text = date.format(formatter),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * A row of 7 day cells representing one week.
 */
@Composable
private fun WeekRow(
    week: List<LocalDate>,
    today: LocalDate,
    trackingStartDate: LocalDate?,
    currentPeriodStart: LocalDate,
    finalizedDays: Map<LocalDate, Boolean>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        week.forEach { date ->
            val status = getDayStatus(
                date = date,
                today = today,
                trackingStartDate = trackingStartDate,
                currentPeriodStart = currentPeriodStart,
                finalizedDays = finalizedDays
            )

            DayCell(
                date = date,
                status = status,
                isToday = date == today,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Determines the status of a day for coloring.
 */
private fun getDayStatus(
    date: LocalDate,
    today: LocalDate,
    trackingStartDate: LocalDate?,
    currentPeriodStart: LocalDate,
    finalizedDays: Map<LocalDate, Boolean>
): DayStatus {
    // Future days - no color
    if (date > today) {
        return DayStatus.NO_DATA
    }

    // Before tracking started - no color
    if (trackingStartDate == null || date < trackingStartDate) {
        return DayStatus.NO_DATA
    }

    // Check if finalized
    finalizedDays[date]?.let { completed ->
        return if (completed) DayStatus.COMPLETED else DayStatus.MISSED
    }

    // Current period (not finalized yet) - grey
    if (date >= currentPeriodStart && date <= today) {
        return DayStatus.IN_PROGRESS
    }

    // Gap/abandoned period - no color
    return DayStatus.NO_DATA
}

/**
 * Individual day cell with date number and background color.
 * - Today gets a colored border ring
 * - In-progress days get a shadow/glow effect
 */
@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        DayStatus.COMPLETED -> ColorCompleted
        DayStatus.MISSED -> ColorMissed
        DayStatus.IN_PROGRESS -> ColorInProgress
        DayStatus.NO_DATA -> Color.Transparent
    }

    val textColor = when {
        status != DayStatus.NO_DATA -> Color.White
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val todayBorderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            // Shadow for in-progress days (current period glow)
            .then(
                if (status == DayStatus.IN_PROGRESS) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = ColorInProgress,
                        spotColor = ColorInProgress
                    )
                } else {
                    Modifier
                }
            )
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            // Border ring for today
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 2.dp,
                        color = todayBorderColor,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
