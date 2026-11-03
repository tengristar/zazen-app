package com.example.zazentimer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zazentimer.data.MeditationStore
import com.example.zazentimer.model.GongMode
import com.example.zazentimer.model.MeditationRecord
import com.example.zazentimer.model.SessionPreset
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val sessionPresets = listOf(
    SessionPreset("5 min", 5),
    SessionPreset("10 min", 10),
    SessionPreset("15 min", 15),
    SessionPreset("20 min", 20),
    SessionPreset("30 min", 30),
    SessionPreset("45 min", 45)
)

@Composable
fun ZazenTimerScreen() {
    val context = LocalContext.current
    val store = remember { MeditationStore(context) }
    var selectedMinutes by remember { mutableIntStateOf(20) }
    var remainingSeconds by remember { mutableIntStateOf(20 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedGong by remember { mutableStateOf(store.loadGong()) }
    var history by remember { mutableStateOf(store.loadHistory()) }
    var monthStart by remember { mutableLongStateOf(startOfMonth(System.currentTimeMillis())) }
    var selectedDay by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }

    LaunchedEffect(selectedGong) { store.saveGong(selectedGong) }
    LaunchedEffect(history) { store.saveHistory(history) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (!isRunning) return@LaunchedEffect
        if (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
        } else {
            isRunning = false
            val completedAt = System.currentTimeMillis()
            history = history + MeditationRecord(
                timestamp = completedAt,
                minutes = selectedMinutes,
                presetLabel = "$selectedMinutes min"
            )
            selectedDay = startOfDay(completedAt)
            monthStart = startOfMonth(completedAt)
            store.playSelectedGong(selectedGong)
        }
    }

    val progress by animateFloatAsState(
        targetValue = (1f - remainingSeconds.toFloat() / (selectedMinutes * 60f)).coerceIn(0f, 1f),
        label = "sessionProgress"
    )
    val selectedDayRecords = history.filter { sameDay(it.timestamp, selectedDay) }
    val totalMinutes = history.sumOf { it.minutes }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ZA-ZEN",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "A quiet moment",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Make space to breathe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = when {
                            isRunning -> "IN SESSION"
                            remainingSeconds == 0 -> "SESSION COMPLETE"
                            else -> "READY WHEN YOU ARE"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatDuration(remainingSeconds),
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 46.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Light
                            )
                            Text(
                                text = "remaining",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sessionPresets.forEach { preset ->
                            val selected = preset.minutes == selectedMinutes
                            if (selected) {
                                Button(
                                    onClick = { if (!isRunning) setPreset(preset.minutes) { selectedMinutes = it; remainingSeconds = it * 60 } },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                                ) { Text(preset.label) }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        isRunning = false
                                        selectedMinutes = preset.minutes
                                        remainingSeconds = preset.minutes * 60
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text(preset.label) }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (remainingSeconds == 0) remainingSeconds = selectedMinutes * 60
                                isRunning = !isRunning
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (isRunning) "Pause" else "Begin session")
                        }
                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                remainingSeconds = selectedMinutes * 60
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Reset") }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeading(title = "Your practice", subtitle = "A little consistency goes a long way")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Sessions", history.size.toString(), Modifier.weight(1f))
                        StatCard("Minutes", totalMinutes.toString(), Modifier.weight(1f))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeading(title = "Closing sound", subtitle = "Choose a gentle end-of-session cue")
                    GongPicker(
                        selected = selectedGong,
                        onSelect = { mode ->
                            selectedGong = mode
                            if (mode != GongMode.NONE) store.playSelectedGong(mode)
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeading(title = "Meditation history", subtitle = "Tap a day to revisit your practice")
                    CalendarHistoryCard(
                        history = history,
                        monthStart = monthStart,
                        selectedDay = selectedDay,
                        onMonthChange = { offset ->
                            monthStart = addMonths(monthStart, offset)
                            val firstDay = startOfMonth(monthStart)
                            selectedDay = firstDay
                        },
                        onDaySelected = { selectedDay = startOfDay(it) }
                    )
                    if (selectedDayRecords.isEmpty()) {
                        Text(
                            text = "No sessions on ${formatShortDate(selectedDay)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = formatLongDate(selectedDay),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        selectedDayRecords.sortedByDescending { it.timestamp }.forEach { record ->
                            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(record.timestamp)
                            Text(
                                text = "${record.minutes} min · $time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarHistoryCard(
    history: List<MeditationRecord>,
    monthStart: Long,
    selectedDay: Long,
    onMonthChange: (Int) -> Unit,
    onDaySelected: (Long) -> Unit
) {
    val month = Calendar.getInstance().apply { timeInMillis = monthStart }
    val first = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(first.time)
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
    val leading = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val sessionDays = history.map { dayKey(it.timestamp) }.toSet()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { onMonthChange(-1) }, shape = CircleShape) { Text("‹") }
        Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = { onMonthChange(1) }, shape = CircleShape) { Text("›") }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { weekday ->
            Text(
                text = weekday,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    repeat(6) { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(7) { weekday ->
                val dayNumber = week * 7 + weekday - leading + 1
                if (dayNumber !in 1..daysInMonth) {
                    Box(Modifier.weight(1f).height(42.dp))
                } else {
                    val day = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNumber) }
                    val timestamp = day.timeInMillis
                    val selected = sameDay(timestamp, selectedDay)
                    val completed = dayKey(timestamp) in sessionDays
                    Box(modifier = Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = { onDaySelected(timestamp) },
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = when {
                                selected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    dayNumber.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (completed) {
                                    Box(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                                            .size(4.dp).background(
                                                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun setPreset(minutes: Int, update: (Int) -> Unit) = update(minutes)

private fun formatDuration(totalSeconds: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

private fun formatLongDate(timestamp: Long): String =
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(timestamp)

private fun formatShortDate(timestamp: Long): String =
    SimpleDateFormat("d MMMM", Locale.getDefault()).format(timestamp)

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun startOfMonth(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addMonths(timestamp: Long, delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.MONTH, delta)
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

private fun sameDay(first: Long, second: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = first }
    val b = Calendar.getInstance().apply { timeInMillis = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun dayKey(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(timestamp)
