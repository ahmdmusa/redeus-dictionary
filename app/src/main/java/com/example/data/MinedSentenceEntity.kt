package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mined_sentences")
data class MinedSentenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sentence: String,
    val targetWord: String,
    val meaning: String,
    val category: String, // "Casual", "Workplace", "Dining", "Idioms", "Travel"
    val notes: String = "",
    val isFavorite: Boolean = false,
    val practiceCount: Int = 0,
    val lastPracticed: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
