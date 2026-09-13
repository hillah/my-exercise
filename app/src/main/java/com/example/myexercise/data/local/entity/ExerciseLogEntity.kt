package com.example.myexercise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_logs",
    indices = [Index(value = ["date"]), Index(value = ["exerciseTypeId"])]
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseTypeId: Long,
    val exerciseName: String,
    val date: String, // Format: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val count: Int,
    val unit: String,
    val note: String? = null
)
