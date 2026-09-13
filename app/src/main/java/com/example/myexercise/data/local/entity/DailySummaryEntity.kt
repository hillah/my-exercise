package com.example.myexercise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val stepCount: Int = 0,
    val activeMinutes: Int = 0,
    val workoutCount: Int = 0, // 単位が「回」の自重トレーニング合計（スクワット・腕立て・腹筋）
    val stretchSeconds: Int = 0, // 単位が「秒」のストレッチ合計秒数
    val isGoalMet: Boolean = false,
    val achievementLevel: Int = 0, // 0..4 (GitHub 草の濃淡に対応)
    val updatedAt: Long = System.currentTimeMillis()
)
