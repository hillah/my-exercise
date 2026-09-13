package com.example.myexercise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.myexercise.data.local.entity.DailySummaryEntity
import com.example.myexercise.data.local.entity.ExerciseLogEntity
import com.example.myexercise.data.local.entity.ExerciseTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    // --- Exercise Types ---
    @Query("SELECT * FROM exercise_types ORDER BY displayOrder ASC, id ASC")
    fun getAllExerciseTypes(): Flow<List<ExerciseTypeEntity>>

    @Query("SELECT * FROM exercise_types ORDER BY displayOrder ASC, id ASC")
    suspend fun getExerciseTypesList(): List<ExerciseTypeEntity>

    @Query("SELECT * FROM exercise_types WHERE id = :id LIMIT 1")
    suspend fun getExerciseTypeById(id: Long): ExerciseTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseTypes(types: List<ExerciseTypeEntity>)

    @Query("SELECT COUNT(*) FROM exercise_types")
    suspend fun getExerciseTypeCount(): Int

    @Query("DELETE FROM exercise_types WHERE id = :id")
    suspend fun deleteExerciseTypeById(id: Long)


    // --- Exercise Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExerciseLogEntity): Long

    @Query("SELECT * FROM exercise_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getLogsByDateFlow(date: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getLogsByDate(date: String): List<ExerciseLogEntity>

    @Query("SELECT SUM(count) FROM exercise_logs WHERE date = :date AND unit = '回'")
    suspend fun getTotalWorkoutRepsByDate(date: String): Int?

    @Query("SELECT SUM(count) FROM exercise_logs WHERE date = :date AND unit = '秒'")
    suspend fun getTotalStretchSecondsByDate(date: String): Int?

    @Query("DELETE FROM exercise_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)


    // --- Daily Summaries ---
    @Query("SELECT * FROM daily_summaries WHERE date = :date LIMIT 1")
    suspend fun getDailySummary(date: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE date = :date LIMIT 1")
    fun getDailySummaryFlow(date: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSummariesInRangeFlow(startDate: String, endDate: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE date <= :endDate ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSummaries(endDate: String, limit: Int): List<DailySummaryEntity>

    @Upsert
    suspend fun upsertDailySummary(summary: DailySummaryEntity)


    // --- Transaction: 1-Tap Log + Update DailySummary ---
    @Transaction
    suspend fun logExerciseAndRecalculate(
        typeId: Long,
        typeName: String,
        count: Int,
        unit: String,
        date: String
    ) {
        // 1. Insert ExerciseLog
        insertLog(
            ExerciseLogEntity(
                exerciseTypeId = typeId,
                exerciseName = typeName,
                date = date,
                count = count,
                unit = unit
            )
        )

        // 2. Fetch existing daily summary or create new
        val existing = getDailySummary(date) ?: DailySummaryEntity(date = date)
        val totalReps = (getTotalWorkoutRepsByDate(date) ?: 0)
        val totalStretchSecs = (getTotalStretchSecondsByDate(date) ?: 0)

        // 3. Recalculate achievement level
        // Level 0: 0
        // Level 1: Any exercise (Reps > 0 or Stretch >= 30s) OR steps >= 2500
        // Level 2: Goal Met: (Reps >= 20 or Stretch >= 60s or steps >= 5000 or activeMinutes >= 15)
        // Level 3: steps >= 10000 OR (Reps >= 30 & steps >= 5000)
        // Level 4: steps >= 15000 OR (Reps >= 50 & steps >= 8000)
        val hasExercise = totalReps > 0 || totalStretchSecs >= 30
        val isGoalMet = hasExercise || existing.stepCount >= 5000 || existing.activeMinutes >= 15

        val level = when {
            existing.stepCount >= 15000 || (totalReps >= 50 && existing.stepCount >= 8000) -> 4
            existing.stepCount >= 10000 || (totalReps >= 30 && existing.stepCount >= 5000) -> 3
            isGoalMet && (totalReps >= 20 || totalStretchSecs >= 60 || existing.stepCount >= 5000) -> 2
            isGoalMet -> 1
            else -> 0
        }

        upsertDailySummary(
            existing.copy(
                workoutCount = totalReps,
                stretchSeconds = totalStretchSecs,
                isGoalMet = isGoalMet,
                achievementLevel = level,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
