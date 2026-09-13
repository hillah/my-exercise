package com.example.myexercise

import com.example.myexercise.data.local.ExerciseDao
import com.example.myexercise.data.local.entity.DailySummaryEntity
import com.example.myexercise.data.local.entity.ExerciseLogEntity
import com.example.myexercise.data.local.entity.ExerciseTypeEntity
import com.example.myexercise.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExerciseRepositoryTest {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun testStreakCalculationWhenTodayCompleted() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        val yesterdayStr = today.minusDays(1).format(dateFormatter)
        val twoDaysAgoStr = today.minusDays(2).format(dateFormatter)

        val summaries = listOf(
            DailySummaryEntity(date = todayStr, isGoalMet = true, workoutCount = 10),
            DailySummaryEntity(date = yesterdayStr, isGoalMet = true, workoutCount = 20),
            DailySummaryEntity(date = twoDaysAgoStr, isGoalMet = true, stepCount = 6000),
            DailySummaryEntity(date = today.minusDays(3).format(dateFormatter), isGoalMet = false)
        )

        val fakeDao = FakeExerciseDao(summaries)
        val repository = ExerciseRepository(fakeDao)

        val streak = repository.calculateCurrentStreak()
        assertEquals(3, streak)
    }

    @Test
    fun testStreakCalculationWhenTodayNotCompletedYet() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        val yesterdayStr = today.minusDays(1).format(dateFormatter)
        val twoDaysAgoStr = today.minusDays(2).format(dateFormatter)

        val summaries = listOf(
            DailySummaryEntity(date = todayStr, isGoalMet = false),
            DailySummaryEntity(date = yesterdayStr, isGoalMet = true, workoutCount = 20),
            DailySummaryEntity(date = twoDaysAgoStr, isGoalMet = true, stepCount = 6000),
            DailySummaryEntity(date = today.minusDays(3).format(dateFormatter), isGoalMet = false)
        )

        val fakeDao = FakeExerciseDao(summaries)
        val repository = ExerciseRepository(fakeDao)

        val streak = repository.calculateCurrentStreak()
        assertEquals(2, streak)
    }

    @Test
    fun testStreakCalculationWhenBroken() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        val yesterdayStr = today.minusDays(1).format(dateFormatter)

        val summaries = listOf(
            DailySummaryEntity(date = todayStr, isGoalMet = false),
            DailySummaryEntity(date = yesterdayStr, isGoalMet = false)
        )

        val fakeDao = FakeExerciseDao(summaries)
        val repository = ExerciseRepository(fakeDao)

        val streak = repository.calculateCurrentStreak()
        assertEquals(0, streak)
    }

    private class FakeExerciseDao(
        private val summaries: List<DailySummaryEntity>
    ) : ExerciseDao {
        override fun getAllExerciseTypes(): Flow<List<ExerciseTypeEntity>> = flowOf(emptyList())
        override suspend fun getExerciseTypesList(): List<ExerciseTypeEntity> = emptyList()
        override suspend fun getExerciseTypeById(id: Long): ExerciseTypeEntity? = null
        override suspend fun insertExerciseTypes(types: List<ExerciseTypeEntity>) {}
        override suspend fun getExerciseTypeCount(): Int = 0
        override suspend fun insertLog(log: ExerciseLogEntity): Long = 1L
        override fun getLogsByDateFlow(date: String): Flow<List<ExerciseLogEntity>> = flowOf(emptyList())
        override suspend fun getLogsByDate(date: String): List<ExerciseLogEntity> = emptyList()
        override suspend fun getTotalWorkoutRepsByDate(date: String): Int? = null
        override suspend fun getTotalStretchSecondsByDate(date: String): Int? = null
        override suspend fun deleteLogById(logId: Long) {}
        override suspend fun getDailySummary(date: String): DailySummaryEntity? = summaries.find { it.date == date }
        override fun getDailySummaryFlow(date: String): Flow<DailySummaryEntity?> = flowOf(summaries.find { it.date == date })
        override fun getSummariesInRangeFlow(startDate: String, endDate: String): Flow<List<DailySummaryEntity>> = flowOf(summaries)
        override suspend fun getRecentSummaries(endDate: String, limit: Int): List<DailySummaryEntity> = summaries
        override suspend fun upsertDailySummary(summary: DailySummaryEntity) {}
        override suspend fun deleteExerciseTypeById(id: Long) {}
    }
}
