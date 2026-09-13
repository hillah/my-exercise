package com.example.myexercise.ui.main

import com.example.myexercise.data.local.entity.DailySummaryEntity
import com.example.myexercise.data.local.entity.ExerciseLogEntity
import com.example.myexercise.data.local.entity.ExerciseTypeEntity

data class MainUiState(
    val isLoading: Boolean = true,
    val streakDays: Int = 0,
    val isTodayCompleted: Boolean = false,
    val exerciseTypes: List<ExerciseTypeEntity> = emptyList(),
    val todaySummary: DailySummaryEntity? = null,
    val heatmapSummaries: List<DailySummaryEntity> = emptyList(),
    val todayLogs: List<ExerciseLogEntity> = emptyList(),
    val isHealthConnectAvailable: Boolean = false,
    val hasHealthPermissions: Boolean = false,
    val snackBarMessage: String? = null
)
