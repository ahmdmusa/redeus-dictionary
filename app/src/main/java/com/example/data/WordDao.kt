package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY timestamp DESC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE isBookmarked = 1")
    suspend fun getBookmarkedWordsList(): List<WordEntity>

    @Query("SELECT * FROM words")
    suspend fun getAllWordsList(): List<WordEntity>

    @Query("SELECT * FROM words WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getWord(word: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Query("UPDATE words SET isBookmarked = :isBookmarked WHERE LOWER(word) = LOWER(:word)")
    suspend fun updateBookmarkStatus(word: String, isBookmarked: Boolean)

    @Query("UPDATE words SET tags = :tags WHERE LOWER(word) = LOWER(:word)")
    suspend fun updateWordTags(word: String, tags: String)

    @Query("UPDATE words SET arabicMeaning = :arabicMeaning WHERE LOWER(word) = LOWER(:word)")
    suspend fun updateArabicMeaning(word: String, arabicMeaning: String)

    @Query("UPDATE words SET imagePrompt = :imagePrompt WHERE LOWER(word) = LOWER(:word)")
    suspend fun updateWordImagePrompt(word: String, imagePrompt: String)

    @Query("UPDATE words SET lastUsedInReadingDate = :date WHERE LOWER(word) = LOWER(:word)")
    suspend fun updateLastUsedInReadingDate(word: String, date: Long)

    @Query("UPDATE words SET audioPath = :path WHERE LOWER(word) = LOWER(:word)")
    suspend fun updateAudioPath(word: String, path: String?)

    @Query("UPDATE words SET isLeech = 0 WHERE LOWER(word) = LOWER(:word)")
    suspend fun clearLeechFlag(word: String)

    @Query("UPDATE words SET easeFactor = 2.5, interval = 0, repetitions = 0, lapses = 0, isLeech = 0, distinctReviewDays = 0, lastSuccessfulReviewDay = NULL, dueDate = :today, lastReviewed = NULL, fsrsStability = NULL, fsrsDifficulty = NULL, fsrsDue = NULL, fsrsState = NULL, fsrsStep = NULL, fsrsLastReview = NULL WHERE LOWER(word) = LOWER(:word)")
    suspend fun resetWordProgress(word: String, today: Long)

    @Query("UPDATE words SET easeFactor = 2.5, interval = 0, repetitions = 0, lapses = 0, isLeech = 0, distinctReviewDays = 0, lastSuccessfulReviewDay = NULL, dueDate = :today, lastReviewed = NULL, fsrsStability = NULL, fsrsDifficulty = NULL, fsrsDue = NULL, fsrsState = NULL, fsrsStep = NULL, fsrsLastReview = NULL")
    suspend fun resetAllWordsProgress(today: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: ReviewLogEntity)

    @Query("SELECT * FROM review_logs ORDER BY timestamp DESC")
    suspend fun getAllReviewLogs(): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs WHERE LOWER(word) = LOWER(:word) ORDER BY timestamp DESC")
    fun getReviewLogsForWord(word: String): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs WHERE LOWER(word) = LOWER(:word) ORDER BY timestamp DESC")
    suspend fun getReviewLogsForWordList(word: String): List<ReviewLogEntity>

    @Query("DELETE FROM review_logs")
    suspend fun clearReviewLogs()

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("UPDATE words SET isBookmarked = 0")
    suspend fun clearBookmarks()

    @Query("DELETE FROM words")
    suspend fun clearHistory()
}
