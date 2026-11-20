package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_logs")
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val rating: String,
    val timestamp: Long = System.currentTimeMillis()
)
