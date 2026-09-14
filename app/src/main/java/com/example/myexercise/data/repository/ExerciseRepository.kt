package com.example.myexercise.data.repository

import com.example.myexercise.data.local.ExerciseDao
import com.example.myexercise.data.local.AppDatabase
import com.example.myexercise.data.local.entity.DailySummaryEntity
import com.example.myexercise.data.local.entity.ExerciseLogEntity
import com.example.myexercise.data.local.entity.ExerciseTypeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExerciseRepository(
    private val exerciseDao: ExerciseDao
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getTodayString(): String = LocalDate.now().format(dateFormatter)

    suspend fun ensureDefaultTypes() = withContext(Dispatchers.IO) {
        val existing = exerciseDao.getExerciseTypesList()
        if (existing.isEmpty()) {
            exerciseDao.insertExerciseTypes(AppDatabase.DEFAULT_TYPES)
        } else {
            // If duplicate names exist, keep the first one and delete the rest
            val duplicates = existing.groupBy { it.name }.filter { it.value.size > 1 }
            for ((_, group) in duplicates) {
                val toDelete = group.drop(1)
                for (item in toDelete) {
                    exerciseDao.deleteExerciseTypeById(item.id)
                }
            }
        }
    }

    fun getAllExerciseTypes(): Flow<List<ExerciseTypeEntity>> =
        exerciseDao.getAllExerciseTypes().map { list ->
            list.distinctBy { it.name }
        }

    fun getTodaySummary(): Flow<DailySummaryEntity?> {
        val today = getTodayString()
        return exerciseDao.getDailySummaryFlow(today)
    }

    fun getTodayLogs(): Flow<List<ExerciseLogEntity>> {
        val today = getTodayString()
        return exerciseDao.getLogsByDateFlow(today)
    }

    fun getHeatmapSummaries(weeks: Int = 16): Flow<List<DailySummaryEntity>> {
        val today = LocalDate.now()
        val startDate = today.minusWeeks(weeks.toLong()).format(dateFormatter)
        val endDate = today.format(dateFormatter)
        return exerciseDao.getSummariesInRangeFlow(startDate, endDate)
    }

    suspend fun logQuickExercise(type: ExerciseTypeEntity) = withContext(Dispatchers.IO) {
        val today = getTodayString()
        exerciseDao.logExerciseAndRecalculate(
            typeId = type.id,
            typeName = type.name,
            count = type.defaultCount,
            unit = type.unit,
            date = today
        )
    }

    suspend fun updateHealthData(
        steps: Int?,
        activeMinutes: Int?,
        date: String = getTodayString()
    ) = withContext(Dispatchers.IO) {
        val existing = exerciseDao.getDailySummary(date) ?: DailySummaryEntity(date = date)
        val totalReps = exerciseDao.getTotalWorkoutRepsByDate(date) ?: 0
        val totalStretchSecs = exerciseDao.getTotalStretchSecondsByDate(date) ?: 0

        // 同一日内で歩数や運動時間が0や減少に誤って上書きされるのを防ぐ
        val finalSteps = when {
            steps != null && steps > 0 -> maxOf(steps, existing.stepCount)
            steps != null && steps == 0 -> if (existing.stepCount > 0) existing.stepCount else 0
            else -> existing.stepCount
        }
        val finalActiveMinutes = when {
            activeMinutes != null && activeMinutes > 0 -> maxOf(activeMinutes, existing.activeMinutes)
            activeMinutes != null && activeMinutes == 0 -> if (existing.activeMinutes > 0) existing.activeMinutes else 0
            else -> existing.activeMinutes
        }

        val hasExercise = totalReps > 0 || totalStretchSecs >= 30
        val isGoalMet = hasExercise || finalSteps >= 5000 || finalActiveMinutes >= 15
        val level = when {
            finalSteps >= 15000 || (totalReps >= 50 && finalSteps >= 8000) -> 4
            finalSteps >= 10000 || (totalReps >= 30 && finalSteps >= 5000) -> 3
            isGoalMet && (totalReps >= 20 || totalStretchSecs >= 60 || finalSteps >= 5000) -> 2
            isGoalMet -> 1
            else -> 0
        }

        exerciseDao.upsertDailySummary(
            existing.copy(
                stepCount = finalSteps,
                activeMinutes = finalActiveMinutes,
                workoutCount = totalReps,
                stretchSeconds = totalStretchSecs,
                isGoalMet = isGoalMet,
                achievementLevel = level,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Calculate current streak (consecutive days of completed goals).
     * If today is completed, streak includes today.
     * If today is not completed yet, streak checks from yesterday backwards.
     */
    suspend fun calculateCurrentStreak(): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        val yesterdayStr = today.minusDays(1).format(dateFormatter)

        // Fetch past 100 summaries
        val recentSummaries = exerciseDao.getRecentSummaries(todayStr, 100).associateBy { it.date }

        val todaySummary = recentSummaries[todayStr]
        val yesterdaySummary = recentSummaries[yesterdayStr]

        var streak = 0
        var checkDate = if (todaySummary?.isGoalMet == true) {
            streak = 1
            today.minusDays(1)
        } else if (yesterdaySummary?.isGoalMet == true) {
            streak = 1
            today.minusDays(2)
        } else {
            return@withContext 0
        }

        while (true) {
            val dateStr = checkDate.format(dateFormatter)
            val summary = recentSummaries[dateStr]
            if (summary?.isGoalMet == true) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        streak
    }
}
