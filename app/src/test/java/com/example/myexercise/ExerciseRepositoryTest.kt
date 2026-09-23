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

    @Test
    fun testUpdateHealthDataPreservesExistingStepsWhenSyncReturnsZeroOrNull() = runTest {
        val today = LocalDate.now().format(dateFormatter)
        val initialSummary = DailySummaryEntity(
            date = today,
            stepCount = 8500,
            activeMinutes = 40,
            workoutCount = 10,
            isGoalMet = true,
            achievementLevel = 2
        )
        val fakeDao = FakeExerciseDao(listOf(initialSummary))
        val repository = ExerciseRepository(fakeDao)

        // Attempt sync with 0 steps (e.g. error or sensor lag)
        repository.updateHealthData(steps = 0, activeMinutes = 0, date = today)
        assertEquals(8500, fakeDao.getDailySummary(today)?.stepCount)
        assertEquals(40, fakeDao.getDailySummary(today)?.activeMinutes)

        // Attempt sync with null steps
        repository.updateHealthData(steps = null, activeMinutes = null, date = today)
        assertEquals(8500, fakeDao.getDailySummary(today)?.stepCount)
        assertEquals(40, fakeDao.getDailySummary(today)?.activeMinutes)

        // Legitimate increase to 9200 steps
        repository.updateHealthData(steps = 9200, activeMinutes = 45, date = today)
        assertEquals(9200, fakeDao.getDailySummary(today)?.stepCount)
        assertEquals(45, fakeDao.getDailySummary(today)?.activeMinutes)
    }

    @Test
    fun testPastDateHealthDataUpdateRestoresStreak() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        val yesterdayStr = today.minusDays(1).format(dateFormatter)
        val twoDaysAgoStr = today.minusDays(2).format(dateFormatter)

        // Initial state: Two days ago was completed, but yesterday was NOT completed (e.g. Garmin sync delay)
        val initialSummaries = listOf(
            DailySummaryEntity(date = twoDaysAgoStr, isGoalMet = true, workoutCount = 20),
            DailySummaryEntity(date = yesterdayStr, isGoalMet = false, stepCount = 0),
            DailySummaryEntity(date = todayStr, isGoalMet = true, workoutCount = 10)
        )
        val fakeDao = FakeExerciseDao(initialSummaries)
        val repository = ExerciseRepository(fakeDao)

        // Without yesterday, streak is only today (1 day)
        assertEquals(1, repository.calculateCurrentStreak())

        // Now Garmin syncs delayed data for yesterday (6,500 steps)
        repository.updateHealthData(steps = 6500, activeMinutes = 30, date = yesterdayStr)

        val updatedYesterday = fakeDao.getDailySummary(yesterdayStr)
        assertEquals(6500, updatedYesterday?.stepCount)
        assertEquals(true, updatedYesterday?.isGoalMet)

        // Streak is now connected across 3 consecutive days!
        val updatedStreak = repository.calculateCurrentStreak()
        assertEquals(3, updatedStreak)
    }

    private class FakeExerciseDao(
        initialSummaries: List<DailySummaryEntity>
    ) : ExerciseDao {
        private val summaryMap = initialSummaries.associateBy { it.date }.toMutableMap()

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
        override suspend fun getDailySummary(date: String): DailySummaryEntity? = summaryMap[date]
        override fun getDailySummaryFlow(date: String): Flow<DailySummaryEntity?> = flowOf(summaryMap[date])
        override fun getSummariesInRangeFlow(startDate: String, endDate: String): Flow<List<DailySummaryEntity>> = flowOf(summaryMap.values.toList())
        override suspend fun getRecentSummaries(endDate: String, limit: Int): List<DailySummaryEntity> = summaryMap.values.toList()
        override suspend fun upsertDailySummary(summary: DailySummaryEntity) {
            summaryMap[summary.date] = summary
        }
        override suspend fun deleteExerciseTypeById(id: Long) {}
    }
}
