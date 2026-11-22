package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestoreSummary(
    val wordsCount: Int,
    val sentencesCount: Int,
    val readingSessionsCount: Int,
    val reviewLogsCount: Int
)

object BackupManager {
    private const val BACKUP_VERSION = 1

    suspend fun exportBackupJson(context: Context, uri: Uri): Result<BackupRestoreSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val db = AppDatabase.getDatabase(context)
                val words = db.wordDao().getAllWordsList()
                val sentences = db.minedSentenceDao().getAllSentencesList()
                val sessions = db.readingSessionDao().getAllSessionsList()
                val logs = db.wordDao().getAllReviewLogs()

                val root = JSONObject().apply {
                    put("app", "Redeus Dictionary")
                    put("version", BACKUP_VERSION)
                    put("exportedAt", System.currentTimeMillis())

                    put("words", JSONArray().apply {
                        words.forEach { w ->
                            put(JSONObject().apply {
                                put("word", w.word)
                                put("phonetic", w.phonetic)
                                put("partOfSpeech", w.partOfSpeech)
                                put("definition", w.definition)
                                put("synonyms", w.synonyms)
                                put("antonyms", w.antonyms)
                                put("example", w.example)
                                put("mnemonic", w.mnemonic)
                                put("imagePrompt", w.imagePrompt)
                                put("isBookmarked", w.isBookmarked)
                                put("timestamp", w.timestamp)
                                put("easeFactor", w.easeFactor.toDouble())
                                put("interval", w.interval)
                                put("repetitions", w.repetitions)
                                put("dueDate", w.dueDate)
                                if (w.lastReviewed != null) put("lastReviewed", w.lastReviewed)
                                if (w.lastUsedInReadingDate != null) put("lastUsedInReadingDate", w.lastUsedInReadingDate)
                                put("lapses", w.lapses)
                                put("isLeech", w.isLeech)
                                put("distinctReviewDays", w.distinctReviewDays)
                                if (w.lastSuccessfulReviewDay != null) put("lastSuccessfulReviewDay", w.lastSuccessfulReviewDay)
                                if (w.fsrsStability != null) put("fsrsStability", w.fsrsStability)
                                if (w.fsrsDifficulty != null) put("fsrsDifficulty", w.fsrsDifficulty)
                                if (w.fsrsDue != null) put("fsrsDue", w.fsrsDue)
                                if (w.fsrsState != null) put("fsrsState", w.fsrsState)
                                if (w.fsrsStep != null) put("fsrsStep", w.fsrsStep)
                                if (w.fsrsLastReview != null) put("fsrsLastReview", w.fsrsLastReview)
                                put("tags", w.tags)
                                put("arabicMeaning", w.arabicMeaning)
                            })
                        }
                    })

                    put("minedSentences", JSONArray().apply {
                        sentences.forEach { s ->
                            put(JSONObject().apply {
                                put("sentence", s.sentence)
                                put("targetWord", s.targetWord)
                                put("meaning", s.meaning)
                                put("category", s.category)
                                put("notes", s.notes)
                                put("isFavorite", s.isFavorite)
                                put("practiceCount", s.practiceCount)
                                if (s.lastPracticed != null) put("lastPracticed", s.lastPracticed)
                                put("timestamp", s.timestamp)
                            })
                        }
                    })

                    put("readingSessions", JSONArray().apply {
                        sessions.forEach { r ->
                            put(JSONObject().apply {
                                put("date", r.date)
                                put("score", r.score)
                                put("total", r.total)
                                put("levelBefore", r.levelBefore)
                                put("levelAfter", r.levelAfter)
                            })
                        }
                    })

                    put("reviewLogs", JSONArray().apply {
                        logs.forEach { l ->
                            put(JSONObject().apply {
                                put("word", l.word)
                                put("rating", l.rating)
                                put("timestamp", l.timestamp)
                            })
                        }
                    })
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(root.toString(2).toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open output file")

                BackupRestoreSummary(
                    wordsCount = words.size,
                    sentencesCount = sentences.size,
                    readingSessionsCount = sessions.size,
                    reviewLogsCount = logs.size
                )
            }
        }

    suspend fun importBackupJson(context: Context, uri: Uri): Result<BackupRestoreSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: error("Unable to read backup file")

                val root = JSONObject(raw)
                val db = AppDatabase.getDatabase(context)
                val wordDao = db.wordDao()
                val sentenceDao = db.minedSentenceDao()
                val readingDao = db.readingSessionDao()

                var wordsImported = 0
                val wordsArr = root.optJSONArray("words")
                if (wordsArr != null) {
                    for (i in 0 until wordsArr.length()) {
                        val obj = wordsArr.optJSONObject(i) ?: continue
                        val wordText = obj.optString("word", "").trim()
                        if (wordText.isBlank()) continue
                        val entity = WordEntity(
                            word = wordText,
                            phonetic = obj.optString("phonetic", ""),
                            partOfSpeech = obj.optString("partOfSpeech", ""),
                            definition = obj.optString("definition", ""),
                            synonyms = obj.optString("synonyms", ""),
                            antonyms = obj.optString("antonyms", ""),
                            example = obj.optString("example", ""),
                            mnemonic = obj.optString("mnemonic", ""),
                            imagePrompt = obj.optString("imagePrompt", ""),
                            isBookmarked = obj.optBoolean("isBookmarked", false),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                            interval = obj.optInt("interval", 0),
                            repetitions = obj.optInt("repetitions", 0),
                            dueDate = obj.optLong("dueDate", java.time.LocalDate.now().toEpochDay()),
                            lastReviewed = if (obj.has("lastReviewed") && !obj.isNull("lastReviewed")) obj.optLong("lastReviewed") else null,
                            lastUsedInReadingDate = if (obj.has("lastUsedInReadingDate") && !obj.isNull("lastUsedInReadingDate")) obj.optLong("lastUsedInReadingDate") else null,
                            audioPath = null,
                            lapses = obj.optInt("lapses", 0),
                            isLeech = obj.optBoolean("isLeech", false),
                            distinctReviewDays = obj.optInt("distinctReviewDays", 0),
                            lastSuccessfulReviewDay = if (obj.has("lastSuccessfulReviewDay") && !obj.isNull("lastSuccessfulReviewDay")) obj.optLong("lastSuccessfulReviewDay") else null,
                            fsrsStability = if (obj.has("fsrsStability") && !obj.isNull("fsrsStability")) obj.optDouble("fsrsStability") else null,
                            fsrsDifficulty = if (obj.has("fsrsDifficulty") && !obj.isNull("fsrsDifficulty")) obj.optDouble("fsrsDifficulty") else null,
                            fsrsDue = if (obj.has("fsrsDue") && !obj.isNull("fsrsDue")) obj.optLong("fsrsDue") else null,
                            fsrsState = if (obj.has("fsrsState") && !obj.isNull("fsrsState")) obj.optString("fsrsState") else null,
                            fsrsStep = if (obj.has("fsrsStep") && !obj.isNull("fsrsStep")) obj.optInt("fsrsStep") else null,
                            fsrsLastReview = if (obj.has("fsrsLastReview") && !obj.isNull("fsrsLastReview")) obj.optLong("fsrsLastReview") else null,
                            tags = obj.optString("tags", ""),
                            arabicMeaning = obj.optString("arabicMeaning", "")
                        )
                        wordDao.insertWord(entity)
                        wordsImported++
                    }
                }

                var sentencesImported = 0
                val sentencesArr = root.optJSONArray("minedSentences")
                if (sentencesArr != null) {
                    for (i in 0 until sentencesArr.length()) {
                        val obj = sentencesArr.optJSONObject(i) ?: continue
                        val sentenceText = obj.optString("sentence", "").trim()
                        val targetWord = obj.optString("targetWord", "").trim()
                        if (sentenceText.isBlank()) continue
                        val existing = sentenceDao.findBySentenceAndWord(sentenceText, targetWord)
                        if (existing == null) {
                            sentenceDao.insertSentence(
                                MinedSentenceEntity(
                                    sentence = sentenceText,
                                    targetWord = targetWord,
                                    meaning = obj.optString("meaning", ""),
                                    category = obj.optString("category", "Casual"),
                                    notes = obj.optString("notes", ""),
                                    isFavorite = obj.optBoolean("isFavorite", false),
                                    practiceCount = obj.optInt("practiceCount", 0),
                                    lastPracticed = if (obj.has("lastPracticed") && !obj.isNull("lastPracticed")) obj.optLong("lastPracticed") else null,
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                )
                            )
                            sentencesImported++
                        }
                    }
                }

                var sessionsImported = 0
                val existingSessions = readingDao.getAllSessionsList()
                    .map { "${it.date}_${it.score}_${it.total}" }
                    .toMutableSet()
                val sessionsArr = root.optJSONArray("readingSessions")
                if (sessionsArr != null) {
                    for (i in 0 until sessionsArr.length()) {
                        val obj = sessionsArr.optJSONObject(i) ?: continue
                        val date = obj.optLong("date", 0L)
                        val score = obj.optInt("score", 0)
                        val total = obj.optInt("total", 0)
                        val key = "${date}_${score}_${total}"
                        if (date > 0L && existingSessions.add(key)) {
                            readingDao.insertSession(
                                ReadingSessionEntity(
                                    date = date,
                                    score = score,
                                    total = total,
                                    levelBefore = obj.optInt("levelBefore", 1),
                                    levelAfter = obj.optInt("levelAfter", 1)
                                )
                            )
                            sessionsImported++
                        }
                    }
                }

                var logsImported = 0
                val existingLogs = wordDao.getAllReviewLogs()
                    .map { "${it.word.lowercase()}_${it.rating}_${it.timestamp}" }
                    .toMutableSet()
                val logsArr = root.optJSONArray("reviewLogs")
                if (logsArr != null) {
                    for (i in 0 until logsArr.length()) {
                        val obj = logsArr.optJSONObject(i) ?: continue
                        val w = obj.optString("word", "").trim()
                        val rating = obj.optString("rating", "").trim()
                        val ts = obj.optLong("timestamp", 0L)
                        val key = "${w.lowercase()}_${rating}_${ts}"
                        if (w.isNotBlank() && rating.isNotBlank() && existingLogs.add(key)) {
                            wordDao.insertReviewLog(
                                ReviewLogEntity(word = w, rating = rating, timestamp = ts)
                            )
                            logsImported++
                        }
                    }
                }

                BackupRestoreSummary(
                    wordsCount = wordsImported,
                    sentencesCount = sentencesImported,
                    readingSessionsCount = sessionsImported,
                    reviewLogsCount = logsImported
                )
            }
        }

    suspend fun exportAnkiCsv(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val db = AppDatabase.getDatabase(context)
                val bookmarked = db.wordDao().getBookmarkedWordsList()
                val wordsToExport = bookmarked.ifEmpty { db.wordDao().getAllWordsList() }

                val sb = StringBuilder()
                sb.append("Word,Phonetic,PartOfSpeech,Definition,Example,Mnemonic,Tags\n")
                wordsToExport.forEach { w ->
                    sb.append(csvEscape(w.word)).append(',')
                        .append(csvEscape(w.phonetic)).append(',')
                        .append(csvEscape(w.partOfSpeech)).append(',')
                        .append(csvEscape(w.definition)).append(',')
                        .append(csvEscape(w.example)).append(',')
                        .append(csvEscape(w.mnemonic)).append(',')
                        .append(csvEscape(w.tags))
                        .append('\n')
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(sb.toString().toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open CSV output file")

                wordsToExport.size
            }
        }

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\r\n", " ").replace("\n", " ")
        return if (cleaned.contains(',') || cleaned.contains('"')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }
}
