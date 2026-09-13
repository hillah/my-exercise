package com.example.myexercise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String,
    val defaultCount: Int,
    val unit: String,
    val colorHex: String,
    val displayOrder: Int = 0,
    val isPreset: Boolean = true
)
