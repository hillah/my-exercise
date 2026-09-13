package com.example.myexercise.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun hasWritePermission(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
    }

    suspend fun writeExerciseSession(
        title: String,
        iconKey: String,
        durationSeconds: Int = 60,
        endTime: Instant = Instant.now()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(
            IllegalStateException("Health Connect is not available")
        )
        try {
            val startTime = endTime.minusSeconds(durationSeconds.toLong().coerceAtLeast(10L))
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(endTime)

            val exerciseType = when (iconKey) {
                "squat", "pushup", "situp" -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
                "stretch" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
                else -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            }

            val record = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                exerciseType = exerciseType,
                title = title,
                notes = "Recorded via My Exercise 1-Tap"
            )

            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readDailySteps(date: LocalDate = LocalDate.now()): Int {
        val client = healthConnectClient ?: return 0
        return try {
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = client.aggregate(
                androidx.health.connect.client.request.AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun readDailyActiveMinutes(date: LocalDate = LocalDate.now()): Int {
        val client = healthConnectClient ?: return 0
        return try {
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = client.aggregate(
                androidx.health.connect.client.request.AggregateRequest(
                    metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val duration = response[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
            duration?.toMinutes()?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
