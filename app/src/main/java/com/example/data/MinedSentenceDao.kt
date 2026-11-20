package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MinedSentenceDao {
    @Query("SELECT * FROM mined_sentences ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllSentences(): Flow<List<MinedSentenceEntity>>

    @Query("SELECT * FROM mined_sentences ORDER BY timestamp DESC")
    suspend fun getAllSentencesList(): List<MinedSentenceEntity>

    @Query("SELECT * FROM mined_sentences WHERE LOWER(sentence) = LOWER(:sentence) AND LOWER(targetWord) = LOWER(:targetWord) LIMIT 1")
    suspend fun findBySentenceAndWord(sentence: String, targetWord: String): MinedSentenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(sentence: MinedSentenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<MinedSentenceEntity>)

    @Query("UPDATE mined_sentences SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE mined_sentences SET practiceCount = :practiceCount, lastPracticed = :lastPracticed WHERE id = :id")
    suspend fun updatePractice(id: Long, practiceCount: Int, lastPracticed: Long)

    @Delete
    suspend fun deleteSentence(sentence: MinedSentenceEntity)

    @Query("DELETE FROM mined_sentences")
    suspend fun clearAllSentences()
}
