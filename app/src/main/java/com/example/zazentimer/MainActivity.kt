package com.example.zazentimer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazentimer.ui.theme.ZazenTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val PREFS_NAME = "zazen_timer_prefs"
private const val PREF_HISTORY = "meditation_history"
private const val PREF_GONG = "selected_gong"

enum class GongMode(val label: String) {
    NONE("None"),
    BELL("Bell"),
    GONG("Gong"),
    CHIME("Chime")
}

data class SessionPreset(val label: String, val minutes: Int)

data class MeditationRecord(
    val timestamp: Long,
    val minutes: Int,
    val presetLabel: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZazenTheme {
                ZazenTimerApp()
            }
        }
    }
}

@Composable
fun ZazenTimerApp() {
    ZazenTimerScreen()
}

@Composable
fun ZazenTimerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

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
    var selectedGong by remember { mutableStateOf(loadSelectedGong(prefs)) }
    var history by remember { mutableStateOf(loadMeditationHistory(prefs)) }
    var currentMonthStart by remember { mutableLongStateOf(startOfMonth(System.currentTimeMillis())) }
    var selectedDay by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }

    LaunchedEffect(selectedGong) {
        saveSelectedGong(prefs, selectedGong)
    }

    LaunchedEffect(history) {
        saveMeditationHistory(prefs, history)
    }

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
            playSelectedGong(context, selectedGong)
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

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
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
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Gong",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GongMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedGong == mode,
                                onClick = {
                                    selectedGong = mode
                                    if (mode != GongMode.NONE) {
                                        playSelectedGong(context, mode)
                                    }
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }

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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "Sessions", value = totalSessions.toString())
                        StatBox(label = "Minutes", value = totalMinutes.toString())
                    }
                }

                CalendarHistoryCard(
                    history = history,
                    currentMonthStart = currentMonthStart,
                    selectedDay = selectedDay,
                    onMonthChange = { monthOffset ->
                        currentMonthStart = addMonths(currentMonthStart, monthOffset)
                        val cal = Calendar.getInstance().apply { timeInMillis = currentMonthStart }
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val monthStart = cal.timeInMillis
                        val firstDayOfMonth = startOfDay(monthStart)
                        selectedDay = firstDayOfMonth
                    },
                    onDaySelected = { dayMillis ->
                        selectedDay = startOfDay(dayMillis)
                    }
                )

                if (selectedDayRecords.isEmpty()) {
                    Text(
                        text = "No meditations recorded for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${formatLongDate(selectedDay)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        selectedDayRecords.sortedByDescending { it.timestamp }.forEach { record ->
                            val dateText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(record.timestamp)
                            Text(
                                text = "• ${record.minutes} min at $dateText",
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
    currentMonthStart: Long,
    selectedDay: Long,
    onMonthChange: (Int) -> Unit,
    onDaySelected: (Long) -> Unit
) {
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonthStart)
    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onMonthChange(-1) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("←")
                }

                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = { onMonthChange(1) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("→")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekdays.forEach { weekday ->
                    Text(
                        text = weekday,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val cal = Calendar.getInstance().apply { timeInMillis = currentMonthStart }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val firstOfMonth = cal.clone() as Calendar
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstWeekdayIndex = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7

            val totalCells = firstWeekdayIndex + daysInMonth
            val rowsNeeded = ((totalCells + 6) / 7)

            for (row in 0 until rowsNeeded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstWeekdayIndex + 1

                        if (dayNumber <= 0 || dayNumber > daysInMonth) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(" ")
                            }
                        } else {
                            val dayCal = Calendar.getInstance().apply {
                                timeInMillis = firstOfMonth.timeInMillis
                                set(Calendar.DAY_OF_MONTH, dayNumber)
                            }
                            val dayMillis = dayCal.timeInMillis
                            val hasSession = history.any { sameDay(it.timestamp, dayMillis) }
                            val isSelected = sameDay(dayMillis, selectedDay)

                            Button(
                                onClick = { onDaySelected(dayMillis) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = dayNumber.toString())
                                    if (hasSession) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .size(5.dp)
                                                .background(
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                                    shape = RoundedCornerShape(100)
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

private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
}

private fun triggerVibration(vibrator: Vibrator?) {
    if (vibrator == null) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(1000L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(1000L)
    }
}

private fun loadMeditationHistory(prefs: android.content.SharedPreferences): List<MeditationRecord> {
    val raw = prefs.getString(PREF_HISTORY, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw.split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { entry ->
            val parts = entry.split("|", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            runCatching {
                MeditationRecord(
                    timestamp = parts[0].toLong(),
                    minutes = parts[1].toInt(),
                    presetLabel = parts[2]
                )
            }.getOrNull()
        }
}

private fun saveMeditationHistory(prefs: android.content.SharedPreferences, history: List<MeditationRecord>) {
    val serialized = history.joinToString("\n") { "${it.timestamp}|${it.minutes}|${it.presetLabel}" }
    prefs.edit().putString(PREF_HISTORY, serialized).apply()
}

private fun loadSelectedGong(prefs: android.content.SharedPreferences): GongMode {
    val current = prefs.getString(PREF_GONG, GongMode.BELL.name) ?: GongMode.BELL.name
    return runCatching { GongMode.valueOf(current) }.getOrDefault(GongMode.BELL)
}

private fun saveSelectedGong(prefs: android.content.SharedPreferences, gong: GongMode) {
    prefs.edit().putString(PREF_GONG, gong.name).apply()
}

private fun playSelectedGong(context: Context, gongMode: GongMode) {
    if (gongMode == GongMode.NONE) return

    val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 90)
    try {
        when (gongMode) {
            GongMode.BELL -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            GongMode.GONG -> {
                toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 800)
            }
            GongMode.CHIME -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 350)
            else -> Unit
        }
    } catch (_: Exception) {
        // ignored: devices can reject some tones silently
    } finally {
        toneGenerator.release()
    }
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

private fun sameMonth(first: Long, second: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = first }
    val b = Calendar.getInstance().apply { timeInMillis = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
}
