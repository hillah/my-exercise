package com.example.myexercise.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myexercise.data.health.HealthConnectManager
import com.example.myexercise.data.local.AppDatabase
import com.example.myexercise.data.repository.ExerciseRepository
import java.util.concurrent.TimeUnit

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "HealthSyncWorker started background sync...")
        val healthConnectManager = HealthConnectManager(applicationContext)

        if (!healthConnectManager.isAvailable()) {
            Log.w(TAG, "Health Connect is not available on this device.")
            return Result.success()
        }

        if (!healthConnectManager.hasAllPermissions()) {
            Log.w(TAG, "Health Connect permissions are not granted yet.")
            return Result.success()
        }

        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repository = ExerciseRepository(db.exerciseDao())

            val syncResult = repository.syncRecentHealthData(healthConnectManager, days = 3)
            if (syncResult.isFailure) {
                Log.w(
                    TAG,
                    "Health sync for recent 3 days failed in background: ${syncResult.exceptionOrNull()?.message}"
                )
                return Result.retry()
            }

            Log.d(TAG, "Successfully updated DailySummary for recent 3 days from background sync.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during HealthSyncWorker execution", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HealthSyncWorker"
        const val PERIODIC_WORK_NAME = "health_connect_periodic_sync"
        const val ONE_TIME_WORK_NAME = "health_connect_one_time_sync"

        /**
         * Schedules periodic background sync (runs every 1 hour).
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder().build()

            val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
            Log.d(TAG, "Periodic health sync scheduled every 1 hour.")
        }

        /**
         * Triggers an immediate one-time sync in background.
         */
        fun triggerImmediateSync(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Immediate one-time health sync enqueued.")
        }
    }
}
