package com.example.myexercise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexercise.data.local.entity.DailySummaryEntity
import com.example.myexercise.ui.theme.HeatmapLevel0Dark
import com.example.myexercise.ui.theme.HeatmapLevel0Light
import com.example.myexercise.ui.theme.HeatmapLevel1Dark
import com.example.myexercise.ui.theme.HeatmapLevel1Light
import com.example.myexercise.ui.theme.HeatmapLevel2Dark
import com.example.myexercise.ui.theme.HeatmapLevel2Light
import com.example.myexercise.ui.theme.HeatmapLevel3Dark
import com.example.myexercise.ui.theme.HeatmapLevel3Light
import com.example.myexercise.ui.theme.HeatmapLevel4Dark
import com.example.myexercise.ui.theme.HeatmapLevel4Light
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HeatmapCalendar(
    summaries: List<DailySummaryEntity>,
    weeksCount: Int = 16,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val today = remember { LocalDate.now() }
    val todayStr = remember { today.format(dateFormatter) }

    // Map summary by date
    val summaryMap = remember(summaries) {
        summaries.associateBy { it.date }
    }

    var selectedDate by remember { mutableStateOf(today) }
    val selectedSummary = summaryMap[selectedDate.format(dateFormatter)]

    // Automatically scroll to the right (most recent weeks) on initial composition
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    // Determine starting Monday for the 16-week window
    // Start with the Monday of (weeksCount - 1) weeks ago
    val startMonday = remember(today) {
        today.minusWeeks((weeksCount - 1).toLong()).with(DayOfWeek.MONDAY)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "運動ヒートマップ（草カレンダー）",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "過去${weeksCount}週間",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap Grid with horizontal scroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Day of week labels (Mon, Wed, Fri)
                Column(
                    modifier = Modifier.padding(end = 6.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf("月", "", "水", "", "金", "", "")
                    days.forEach { dayText ->
                        Box(
                            modifier = Modifier.size(15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayText.isNotEmpty()) {
                                Text(
                                    text = dayText,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Scrollable cells
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (weekIndex in 0 until weeksCount) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (dayIndex in 0..6) {
                                val cellDate = startMonday.plusWeeks(weekIndex.toLong()).plusDays(dayIndex.toLong())
                                val cellDateStr = cellDate.format(dateFormatter)
                                val isFuture = cellDate.isAfter(today)
                                val isToday = cellDate == today
                                val isSelected = cellDate == selectedDate

                                val level = if (isFuture) -1 else (summaryMap[cellDateStr]?.achievementLevel ?: 0)
                                val cellColor = getHeatmapColor(level, isDark)

                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isToday) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(3.dp)
                                                )
                                            } else if (isSelected) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(3.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable(enabled = !isFuture) {
                                            selectedDate = cellDate
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend (Less -> More)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                for (lvl in 0..4) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(lvl, isDark))
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "More",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected date detail card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)")),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedDate == today) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "本日",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val statusText = when {
                            selectedSummary?.isGoalMet == true -> "運動達成！ 🎉 (Lv.${selectedSummary.achievementLevel})"
                            selectedSummary != null && (selectedSummary.stepCount > 0 || selectedSummary.workoutCount > 0) -> "記録あり (目標までもう少し)"
                            else -> "記録なし"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedSummary?.isGoalMet == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "歩数",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedSummary?.stepCount ?: 0} 歩",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "自重トレ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedSummary?.workoutCount ?: 0} 回",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getHeatmapColor(level: Int, isDark: Boolean): Color {
    return if (isDark) {
        when (level) {
            -1 -> Color(0xFF0D1117) // Future
            0 -> HeatmapLevel0Dark
            1 -> HeatmapLevel1Dark
            2 -> HeatmapLevel2Dark
            3 -> HeatmapLevel3Dark
            4 -> HeatmapLevel4Dark
            else -> HeatmapLevel0Dark
        }
    } else {
        when (level) {
            -1 -> Color(0xFFF0F0F0) // Future
            0 -> HeatmapLevel0Light
            1 -> HeatmapLevel1Light
            2 -> HeatmapLevel2Light
            3 -> HeatmapLevel3Light
            4 -> HeatmapLevel4Light
            else -> HeatmapLevel0Light
        }
    }
}
