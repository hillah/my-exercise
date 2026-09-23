package com.example.myexercise.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myexercise.data.health.HealthConnectManager
import com.example.myexercise.data.local.AppDatabase
import com.example.myexercise.data.local.entity.ExerciseTypeEntity
import com.example.myexercise.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: ExerciseRepository,
    private val healthConnectManager: HealthConnectManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultTypes()
            checkHealthConnectStatus()
            refreshStreak()
            syncHealthConnect(quietly = true)
        }

        // Collect Exercise Types
        viewModelScope.launch {
            repository.getAllExerciseTypes().collect { types ->
                _uiState.update { it.copy(exerciseTypes = types) }
            }
        }

        // Collect Today Summary
        viewModelScope.launch {
            repository.getTodaySummary().collect { summary ->
                _uiState.update {
                    it.copy(
                        todaySummary = summary,
                        isTodayCompleted = summary?.isGoalMet ?: false
                    )
                }
                refreshStreak()
            }
        }

        // Collect Heatmap Summaries
        viewModelScope.launch {
            repository.getHeatmapSummaries(16).collect { summaries ->
                _uiState.update { it.copy(heatmapSummaries = summaries, isLoading = false) }
            }
        }

        // Collect Today's Workout Logs
        viewModelScope.launch {
            repository.getTodayLogs().collect { logs ->
                _uiState.update { it.copy(todayLogs = logs) }
            }
        }
    }

    fun logQuickExercise(type: ExerciseTypeEntity) {
        viewModelScope.launch {
            repository.logQuickExercise(type)
            _uiState.update {
                it.copy(snackBarMessage = "${type.name} +${type.defaultCount}${type.unit} を記録しました！🔥")
            }
            refreshStreak()

            // Also write to Health Connect if available and permitted
            if (healthConnectManager.isAvailable() && healthConnectManager.hasWritePermission()) {
                val duration = if (type.unit == "秒") type.defaultCount else 60
                healthConnectManager.writeExerciseSession(
                    title = "${type.name} +${type.defaultCount}${type.unit}",
                    iconKey = type.iconKey,
                    durationSeconds = duration
                )
            }
        }
    }

    fun syncHealthConnect(quietly: Boolean = false, days: Int = 3) {
        viewModelScope.launch {
            if (!healthConnectManager.isAvailable()) {
                if (!quietly) _uiState.update { it.copy(snackBarMessage = "ヘルスコネクトが利用できません") }
                return@launch
            }
            if (!healthConnectManager.hasAllPermissions()) {
                if (!quietly) _uiState.update { it.copy(snackBarMessage = "ヘルスコネクトの読み取り権限が必要です") }
                return@launch
            }

            try {
                val result = repository.syncRecentHealthData(healthConnectManager, days = days)
                result.onSuccess { todaySteps ->
                    val displaySteps = if (todaySteps > 0) todaySteps else _uiState.value.todaySummary?.stepCount ?: 0
                    if (!quietly) {
                        _uiState.update {
                            it.copy(snackBarMessage = "直近${days}日分のGarmin / ヘルスコネクトデータを同期しました（本日: ${displaySteps}歩）")
                        }
                    }
                    refreshStreak()
                }.onFailure { error ->
                    if (!quietly) {
                        _uiState.update { it.copy(snackBarMessage = "同期に失敗しました: ${error.localizedMessage}") }
                    }
                }
            } catch (e: Exception) {
                if (!quietly) {
                    _uiState.update { it.copy(snackBarMessage = "同期に失敗しました: ${e.localizedMessage}") }
                }
            }
        }
    }

    private fun checkHealthConnectStatus() {
        viewModelScope.launch {
            val available = healthConnectManager.isAvailable()
            val hasPerms = if (available) healthConnectManager.hasAllPermissions() else false
            _uiState.update {
                it.copy(
                    isHealthConnectAvailable = available,
                    hasHealthPermissions = hasPerms
                )
            }
        }
    }

    private suspend fun refreshStreak() {
        val streak = repository.calculateCurrentStreak()
        _uiState.update { it.copy(streakDays = streak) }
    }

    fun clearSnackBarMessage() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(application)
            val repository = ExerciseRepository(db.exerciseDao())
            val healthConnect = HealthConnectManager(application)
            return MainViewModel(application, repository, healthConnect) as T
        }
    }
}
