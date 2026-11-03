package com.example.zazentimer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazentimer.data.MeditationStore
import com.example.zazentimer.model.GongMode
import com.example.zazentimer.model.MeditationRecord
import com.example.zazentimer.model.SessionPreset
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ZazenTimerScreen() {
    val context = LocalContext.current
    val store = remember { MeditationStore(context) }

    val presets = listOf(
        SessionPreset("5 min", 5),
        SessionPreset("10 min", 10),
        SessionPreset("15 min", 15),
        SessionPreset("20 min", 20),
        SessionPreset("30 min", 30),
        SessionPreset("45 min", 45)
    )

    var selectedMinutes by remember { mutableIntStateOf(20) }
    var remainingSeconds by remember { mutableIntStateOf(selectedMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedGong by remember { mutableStateOf(store.loadGong()) }
    var history by remember { mutableStateOf(store.loadHistory()) }
    var currentMonthStart by remember { mutableLongStateOf(startOfMonth(System.currentTimeMillis())) }
    var selectedDay by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }

    LaunchedEffect(selectedGong) { store.saveGong(selectedGong) }
    LaunchedEffect(history) { store.saveHistory(history) }

    val totalSessions = history.size
    val totalMinutes = history.sumOf { it.minutes }
    val selectedDayRecords = history.filter { sameDay(it.timestamp, selectedDay) }

    val totalSeconds = selectedMinutes * 60
    val elapsedRatio = if (totalSeconds == 0) 0f else 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = elapsedRatio.coerceIn(0f, 1f),
        label = "timerProgress"
    )

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            val completion = MeditationRecord(
                timestamp = System.currentTimeMillis(),
                minutes = selectedMinutes,
                presetLabel = "$selectedMinutes min"
            )
            history = history + completion
            selectedDay = startOfDay(completion.timestamp)
            currentMonthStart = startOfMonth(completion.timestamp)
            store.playSelectedGong(selectedGong)
        }
    }

    val timerLabel = when {
        remainingSeconds == 0 -> "Session complete"
        isRunning -> "Meditating"
        else -> "Ready"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "ZA-ZEN TIMER",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "by tengristar",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )

                Text(
                    text = timerLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(260.dp),
                        strokeWidth = 18.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDuration(remainingSeconds),
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "remaining",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = preset.minutes == selectedMinutes
                        OutlinedButton(
                            onClick = {
                                selectedMinutes = preset.minutes
                                remainingSeconds = preset.minutes * 60
                                isRunning = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(preset.label)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (remainingSeconds == 0) {
                                remainingSeconds = selectedMinutes * 60
                            }
                            isRunning = !isRunning
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (isRunning) "Pause" else "Start")
                    }

                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            remainingSeconds = selectedMinutes * 60
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Reset")
                    }
                }

                GongPicker(
                    selected = selectedGong,
                    onSelect = { mode ->
                        selectedGong = mode
                        if (mode != GongMode.NONE) {
                            store.playSelectedGong(mode)
                        }
                    }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Stats",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(label = "Sessions", value = totalSessions.toString())
                        StatBox(label = "Minutes", value = totalMinutes.toString())
                    }
                }

                val monthCalendar = Calendar.getInstance().apply { timeInMillis = currentMonthStart }
                CalendarHistoryCard(
                    history = history,
                    month = monthCalendar,
                    selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDay },
                    onMonthChange = { delta ->
                        currentMonthStart = addMonths(currentMonthStart, delta)
                        val cal = Calendar.getInstance().apply { timeInMillis = currentMonthStart }
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        selectedDay = cal.timeInMillis
                    },
                    onDaySelected = { day -> selectedDay = day.timeInMillis }
                )

                if (selectedDayRecords.isEmpty()) {
                    Text(
                        text = "No sessions recorded for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatLongDate(selectedDay),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        selectedDayRecords.sortedByDescending { it.timestamp }.forEach { record ->
                            val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(record.timestamp)
                            Text(
                                text = "• ${record.minutes} min at $timeText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarHistoryCard(
    history: List<MeditationRecord>,
    month: Calendar,
    selectedDay: Calendar,
    onMonthChange: (Int) -> Unit,
    onDaySelected: (Calendar) -> Unit
) {
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val first = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val leadingDays = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val sessionDays = history.map { dayKey(it.timestamp) }.toSet()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { onMonthChange(-1) }) { Text("‹") }
                Text(monthLabel, style = MaterialTheme.typography.titleMedium)
                Button(onClick = { onMonthChange(1) }) { Text("›") }
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
                        val dayNumber = week * 7 + weekday - leadingDays + 1
                        if (dayNumber !in 1..daysInMonth) {
                            Box(Modifier.weight(1f).height(42.dp))
                        } else {
                            val day = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNumber) }
                            val hasSession = dayKey(day.timeInMillis) in sessionDays
                            val isSelected = sameDay(day.timeInMillis, selectedDay.timeInMillis)

                            Button(
                                onClick = { onDaySelected(day) },
                                modifier = Modifier.weight(1f).height(42.dp).padding(2.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(dayNumber.toString())
                                    if (hasSession) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 1.dp)
                                                .size(4.dp)
                                                .background(
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
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
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun formatLongDate(timestamp: Long): String {
    return SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.getDefault()).format(timestamp)
}

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun startOfMonth(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun addMonths(timestamp: Long, months: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.add(Calendar.MONTH, months)
    return cal.timeInMillis
}

private fun sameDay(first: Long, second: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = first }
    val b = Calendar.getInstance().apply { timeInMillis = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun dayKey(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(timestamp)
