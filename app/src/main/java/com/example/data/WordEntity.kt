package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val word: String,
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val definition: String = "",
    val synonyms: String = "", // Comma-separated list
    val antonyms: String = "",  // Comma-separated list
    val example: String = "",
    val mnemonic: String = "",
    val imagePrompt: String = "",
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long = java.time.LocalDate.now().toEpochDay(),
    val lastReviewed: Long? = null,
    val lastUsedInReadingDate: Long? = null,
    val audioPath: String? = null,
    val lapses: Int = 0,
    val isLeech: Boolean = false,
    val distinctReviewDays: Int = 0,
    val lastSuccessfulReviewDay: Long? = null,
    val fsrsStability: Double? = null,
    val fsrsDifficulty: Double? = null,
    val fsrsDue: Long? = null,
    val fsrsState: String? = null,
    val fsrsStep: Int? = null,
    val fsrsLastReview: Long? = null,
    val tags: String = ""
)
