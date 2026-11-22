package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class OfflineBook(
    val bookNum: Int,
    val title: String,
    val shortTag: String,
    val cefr: String,
    val category: String, // "essential" or "ielts"
    val unitCount: Int,
    val wordCount: Int
)

data class OfflineUnit(
    val id: String,
    val bookNum: Int,
    val unitNum: Int,
    val title: String,
    val wordCount: Int
)

data class OfflineTopic(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val wordCount: Int
)

data class OfflineCurriculumWord(
    val id: String,
    val word: String,
    val ipa: String,
    val pos: String,
    val definitionEn: String,
    val translationAr: String,
    val exampleEn: String,
    val bookNum: Int,
    val unitNum: Int,
    val topicId: String,
    val wordOrder: Int,
    val cefr: String,
    val category: String,
    val bookShortTag: String = "",
    val unitTitle: String = ""
)

data class OfflineSearchSuggestion(
    val word: String,
    val translationAr: String,
    val previewDef: String,
    val badge: String,
    val isCurriculum: Boolean
)

/**
 * Read-only SQLite wrapper over the pre-packaged offline dictionary database
 * (6 Essential English Books + 3 IELTS Books = 9,591 curriculum words,
 * 26 Life Topics, and 87,422 English-Arabic general dictionary entries).
 */
class OfflineDictionaryDatabase private constructor(
    private val context: Context
) {
    private val lock = Any()

    @Volatile
    private var database: SQLiteDatabase? = null

    @Volatile
    private var isUnavailable: Boolean = false

    private fun openDb(): SQLiteDatabase? {
        database?.let { if (it.isOpen) return it }
        if (isUnavailable) return null

        synchronized(lock) {
            database?.let { if (it.isOpen) return it }
            if (isUnavailable) return null
            return try {
                val dbFile = ensureDatabaseCopied()
                if (dbFile == null || !dbFile.exists()) {
                    isUnavailable = true
                    null
                } else {
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    ).also { database = it }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open offline dictionary database", e)
                isUnavailable = true
                null
            }
        }
    }

    private fun ensureDatabaseCopied(): File? {
        val targetFile = context.getDatabasePath(DB_FILE_NAME)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val installedVersion = prefs.getInt(KEY_DB_VERSION, 0)
        if (installedVersion == CURRENT_DB_VERSION && targetFile.exists() && targetFile.length() > 100_000L) {
            return targetFile
        }
        targetFile.parentFile?.mkdirs()
        // Remove legacy v1 database copy if present to reclaim storage
        val legacyV1 = context.getDatabasePath("sara_dictionary_v1.db")
        if (legacyV1.exists() && legacyV1.absolutePath != targetFile.absolutePath) {
            legacyV1.delete()
        }
        val tempFile = File(targetFile.parentFile, "$DB_FILE_NAME.tmp")
        return try {
            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            prefs.edit().putInt(KEY_DB_VERSION, CURRENT_DB_VERSION).apply()
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy offline dictionary asset '$ASSET_PATH'", e)
            tempFile.delete()
            null
        }
    }

    fun isAvailable(): Boolean = openDb() != null

    fun getAllBooks(): List<OfflineBook> {
        val db = openDb() ?: return emptyList()
        val result = mutableListOf<OfflineBook>()
        db.rawQuery(
            """
            SELECT book_num, title, short_tag, cefr, category, unit_count, word_count
            FROM books
            ORDER BY book_num ASC
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                result.add(
                    OfflineBook(
                        bookNum = c.getInt(0),
                        title = c.getString(1),
                        shortTag = c.getString(2),
                        cefr = c.getString(3),
                        category = c.getString(4),
                        unitCount = c.getInt(5),
                        wordCount = c.getInt(6)
                    )
                )
            }
        }
        return result
    }

    fun getUnitsForBook(bookNum: Int): List<OfflineUnit> {
        val db = openDb() ?: return emptyList()
        val result = mutableListOf<OfflineUnit>()
        db.rawQuery(
            """
            SELECT id, book_num, unit_num, title, word_count
            FROM units
            WHERE book_num = ?
            ORDER BY unit_num ASC
            """.trimIndent(),
            arrayOf(bookNum.toString())
        ).use { c ->
            while (c.moveToNext()) {
                result.add(
                    OfflineUnit(
                        id = c.getString(0),
                        bookNum = c.getInt(1),
                        unitNum = c.getInt(2),
                        title = c.getString(3),
                        wordCount = c.getInt(4)
                    )
                )
            }
        }
        return result
    }

    fun getWordsForUnit(bookNum: Int, unitNum: Int): List<OfflineCurriculumWord> {
        val db = openDb() ?: return emptyList()
        val result = mutableListOf<OfflineCurriculumWord>()
        db.rawQuery(
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE cw.book_num = ? AND cw.unit_num = ?
            ORDER BY cw.word_order ASC
            """.trimIndent(),
            arrayOf(bookNum.toString(), unitNum.toString())
        ).use { c ->
            while (c.moveToNext()) {
                result.add(readCurriculumWord(c))
            }
        }
        return result
    }

    fun getAllTopics(): List<OfflineTopic> {
        val db = openDb() ?: return emptyList()
        val result = mutableListOf<OfflineTopic>()
        db.rawQuery(
            """
            SELECT id, name_en, name_ar, word_count
            FROM topics
            WHERE word_count > 0
            ORDER BY word_count DESC, name_en ASC
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                result.add(
                    OfflineTopic(
                        id = c.getString(0),
                        nameEn = c.getString(1),
                        nameAr = c.getString(2),
                        wordCount = c.getInt(3)
                    )
                )
            }
        }
        return result
    }

    fun getWordsForTopic(topicId: String): List<OfflineCurriculumWord> {
        val db = openDb() ?: return emptyList()
        val result = mutableListOf<OfflineCurriculumWord>()
        db.rawQuery(
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE cw.topic_id = ?
            ORDER BY cw.book_num ASC, cw.unit_num ASC, cw.word_order ASC
            """.trimIndent(),
            arrayOf(topicId)
        ).use { c ->
            while (c.moveToNext()) {
                result.add(readCurriculumWord(c))
            }
        }
        return result
    }

    fun getBookWordLowersMap(): Map<Int, Set<String>> {
        val db = openDb() ?: return emptyMap()
        val map = mutableMapOf<Int, MutableSet<String>>()
        db.rawQuery("SELECT book_num, word_lower FROM curriculum_words", null).use { c ->
            while (c.moveToNext()) {
                val bn = c.getInt(0)
                val wl = c.getString(1)
                map.getOrPut(bn) { HashSet() }.add(wl)
            }
        }
        return map
    }

    fun getUnitWordLowersMap(bookNum: Int): Map<Int, Set<String>> {
        val db = openDb() ?: return emptyMap()
        val map = mutableMapOf<Int, MutableSet<String>>()
        db.rawQuery(
            "SELECT unit_num, word_lower FROM curriculum_words WHERE book_num = ?",
            arrayOf(bookNum.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val un = c.getInt(0)
                val wl = c.getString(1)
                map.getOrPut(un) { HashSet() }.add(wl)
            }
        }
        return map
    }

    fun getTopicWordLowersMap(): Map<String, Set<String>> {
        val db = openDb() ?: return emptyMap()
        val map = mutableMapOf<String, MutableSet<String>>()
        db.rawQuery(
            "SELECT topic_id, word_lower FROM curriculum_words WHERE topic_id != ''",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val tid = c.getString(0)
                val wl = c.getString(1)
                map.getOrPut(tid) { HashSet() }.add(wl)
            }
        }
        return map
    }

    /**
     * Finds all curriculum occurrences of a word, ordered with richest entries first
     * (Books 1-6 Essential -> Book 7 Barron's IELTS -> Book 9 IELTS Bank -> Book 8 IELTS Listening).
     */
    fun findAllCurriculumOccurrences(query: String): List<OfflineCurriculumWord> {
        val db = openDb() ?: return emptyList()
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        val result = mutableListOf<OfflineCurriculumWord>()
        db.rawQuery(
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE cw.word_lower = ?
            ORDER BY
                CASE
                    WHEN cw.book_num <= 6 THEN 1
                    WHEN cw.book_num = 7 THEN 2
                    WHEN cw.book_num = 9 THEN 3
                    ELSE 4
                END ASC,
                cw.book_num ASC,
                cw.unit_num ASC
            """.trimIndent(),
            arrayOf(clean)
        ).use { c ->
            while (c.moveToNext()) {
                result.add(readCurriculumWord(c))
            }
        }
        return result
    }

    fun findBestCurriculumWord(query: String): OfflineCurriculumWord? {
        return findAllCurriculumOccurrences(query).firstOrNull()
    }

    fun getGeneralArabicTranslation(word: String): String? {
        val db = openDb() ?: return null
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return null

        // 1. Prefer curated translation from curriculum_words (Books 1-6 + IELTS)
        db.rawQuery(
            """
            SELECT translation_ar
            FROM curriculum_words
            WHERE word_lower = ? AND translation_ar != ''
            ORDER BY
                CASE WHEN book_num <= 6 THEN 1 WHEN book_num = 7 THEN 2 ELSE 3 END ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(clean)
        ).use { c ->
            if (c.moveToFirst()) {
                val ar = c.getString(0)?.trim()
                if (!ar.isNullOrBlank()) return ar
            }
        }

        // 2. Fallback to the 87,422-word general English-Arabic dictionary
        db.rawQuery(
            "SELECT translation_ar FROM general_dict WHERE word_lower = ? LIMIT 1",
            arrayOf(clean)
        ).use { c ->
            if (c.moveToFirst()) {
                val ar = c.getString(0)?.trim()
                if (!ar.isNullOrBlank()) return ar
            }
        }

        return null
    }

    /**
     * Instant offline autocomplete suggestions supporting both English prefix/contains
     * and Arabic meaning search.
     */
    fun findAllCurriculumOccurrencesWithStem(query: String): List<OfflineCurriculumWord> {
        val exact = findAllCurriculumOccurrences(query)
        if (exact.isNotEmpty()) return exact
        for (stem in OfflineQueryMatcher.generateEnglishStemCandidates(query)) {
            val hits = findAllCurriculumOccurrences(stem)
            if (hits.isNotEmpty()) return hits
        }
        return emptyList()
    }

    fun getWordOfTheDay(epochDay: Long): OfflineCurriculumWord? {
        val db = openDb() ?: return null
        val total = db.rawQuery(
            "SELECT COUNT(*) FROM curriculum_words WHERE book_num <= 6",
            null
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        if (total <= 0) return null
        // Deterministic pseudo-random spread across all 3,600 Essential Book words
        val offset = ((kotlin.math.abs(epochDay * 2654435761L) % total.toLong()).toInt()).coerceIn(0, total - 1)
        return db.rawQuery(
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE cw.book_num <= 6
            ORDER BY cw.book_num ASC, cw.unit_num ASC, cw.word_order ASC
            LIMIT 1 OFFSET ?
            """.trimIndent(),
            arrayOf(offset.toString())
        ).use { c ->
            if (c.moveToNext()) readCurriculumWord(c) else null
        }
    }

    fun getWordsByCefr(cefrFilter: String, limit: Int = 40): List<OfflineCurriculumWord> {
        val db = openDb() ?: return emptyList()
        val clean = cefrFilter.trim().uppercase()
        if (clean.isEmpty()) return emptyList()
        val result = mutableListOf<OfflineCurriculumWord>()
        val isIelts = clean == "IELTS"
        val sql = if (isIelts) {
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE cw.category = 'ielts'
            GROUP BY cw.word_lower
            ORDER BY cw.book_num ASC, cw.unit_num ASC, cw.word_order ASC
            LIMIT ?
            """.trimIndent()
        } else {
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE UPPER(cw.cefr) LIKE ?
            GROUP BY cw.word_lower
            ORDER BY cw.book_num ASC, cw.unit_num ASC, cw.word_order ASC
            LIMIT ?
            """.trimIndent()
        }
        val args = if (isIelts) arrayOf(limit.toString()) else arrayOf("%$clean%", limit.toString())
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                result.add(readCurriculumWord(c))
            }
        }
        return result
    }

    fun findFuzzySuggestions(query: String, limit: Int = 5): List<OfflineSearchSuggestion> {
        val db = openDb() ?: return emptyList()
        val clean = query.trim().lowercase()
        if (clean.length < 3 || ArabicNormalizer.containsArabic(clean)) return emptyList()

        val firstChar = clean[0]
        val nextChar = (firstChar.code + 1).toChar()
        val minLen = (clean.length - 2).coerceAtLeast(2)
        val maxLen = clean.length + 2
        val pool = LinkedHashMap<String, OfflineSearchSuggestion>()

        // Gather same-initial curriculum words within ±2 length for fast Levenshtein ranking
        db.rawQuery(
            """
            SELECT cw.word, cw.translation_ar, cw.definition_en, b.short_tag
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            WHERE cw.word_lower >= ? AND cw.word_lower < ?
              AND LENGTH(cw.word_lower) BETWEEN CAST(? AS INTEGER) AND CAST(? AS INTEGER)
            GROUP BY cw.word_lower
            LIMIT 800
            """.trimIndent(),
            arrayOf(firstChar.toString(), nextChar.toString(), minLen.toString(), maxLen.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val w = c.getString(0)
                val key = w.lowercase()
                if (!pool.containsKey(key)) {
                    pool[key] = OfflineSearchSuggestion(
                        word = w,
                        translationAr = c.getString(1),
                        previewDef = c.getString(2),
                        badge = c.getString(3),
                        isCurriculum = true
                    )
                }
            }
        }

        // Also check 2-char prefix in general_dict if pool is small
        if (pool.size < 120) {
            val p2 = clean.take(2)
            val p2End = p2.dropLast(1) + (p2.last() + 1)
            db.rawQuery(
                """
                SELECT word_lower, translation_ar
                FROM general_dict
                WHERE word_lower >= ? AND word_lower < ?
                  AND LENGTH(word_lower) BETWEEN CAST(? AS INTEGER) AND CAST(? AS INTEGER)
                LIMIT 300
                """.trimIndent(),
                arrayOf(p2, p2End, minLen.toString(), maxLen.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    val w = c.getString(0)
                    val key = w.lowercase()
                    if (!pool.containsKey(key)) {
                        pool[key] = OfflineSearchSuggestion(
                            word = w,
                            translationAr = c.getString(1),
                            previewDef = "",
                            badge = "Dictionary",
                            isCurriculum = false
                        )
                    }
                }
            }
        }

        val ranked = OfflineQueryMatcher.rankFuzzyMatches(clean, pool.keys, limit)
        return ranked.mapNotNull { pool[it.lowercase()] }
    }

    fun searchSuggestions(
        query: String,
        limit: Int = 10,
        booksOnly: Boolean = false
    ): List<OfflineSearchSuggestion> {
        val db = openDb() ?: return emptyList()
        val clean = query.trim().lowercase()
        if (clean.length < 2) return emptyList()

        val seenWords = LinkedHashSet<String>()
        val results = mutableListOf<OfflineSearchSuggestion>()
        val isArabicQuery = ArabicNormalizer.containsArabic(clean)

        if (isArabicQuery) {
            val normQuery = ArabicNormalizer.normalize(clean)
            if (normQuery.isBlank()) return emptyList()
            val likePattern = "%$normQuery%"
            db.rawQuery(
                """
                SELECT cw.word, cw.translation_ar, cw.definition_en, b.short_tag
                FROM curriculum_words cw
                JOIN books b ON b.book_num = cw.book_num
                WHERE cw.translation_ar_norm LIKE ?
                ORDER BY
                    CASE WHEN cw.translation_ar_norm = ? THEN 0
                         WHEN cw.translation_ar_norm LIKE ? THEN 1
                         ELSE 2 END ASC,
                    cw.book_num ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf(likePattern, normQuery, "$normQuery%", (limit * 2).toString())
            ).use { c ->
                while (c.moveToNext() && results.size < limit) {
                    val w = c.getString(0)
                    if (seenWords.add(w.lowercase())) {
                        results.add(
                            OfflineSearchSuggestion(
                                word = w,
                                translationAr = c.getString(1),
                                previewDef = c.getString(2),
                                badge = c.getString(3),
                                isCurriculum = true
                            )
                        )
                    }
                }
            }
            if (!booksOnly && results.size < limit) {
                val remaining = limit - results.size
                db.rawQuery(
                    """
                    SELECT word_lower, translation_ar
                    FROM general_dict
                    WHERE translation_ar_norm LIKE ?
                    ORDER BY
                        CASE WHEN translation_ar_norm = ? THEN 0
                             WHEN translation_ar_norm LIKE ? THEN 1
                             ELSE 2 END ASC,
                        LENGTH(word_lower) ASC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(likePattern, normQuery, "$normQuery%", (remaining * 2).toString())
                ).use { c ->
                    while (c.moveToNext() && results.size < limit) {
                        val w = c.getString(0)
                        if (seenWords.add(w.lowercase())) {
                            results.add(
                                OfflineSearchSuggestion(
                                    word = w,
                                    translationAr = c.getString(1),
                                    previewDef = "",
                                    badge = "Dictionary",
                                    isCurriculum = false
                                )
                            )
                        }
                    }
                }
            }
        } else {
            val prefixEnd = clean.dropLast(1) + (clean.last() + 1)
            // 1. Curriculum prefix matches first
            db.rawQuery(
                """
                SELECT cw.word, cw.translation_ar, cw.definition_en, b.short_tag
                FROM curriculum_words cw
                JOIN books b ON b.book_num = cw.book_num
                WHERE cw.word_lower >= ? AND cw.word_lower < ?
                ORDER BY
                    CASE WHEN cw.word_lower = ? THEN 0 ELSE 1 END ASC,
                    LENGTH(cw.word_lower) ASC,
                    cw.book_num ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf(clean, prefixEnd, clean, (limit * 3).toString())
            ).use { c ->
                while (c.moveToNext() && results.size < limit) {
                    val w = c.getString(0)
                    if (seenWords.add(w.lowercase())) {
                        results.add(
                            OfflineSearchSuggestion(
                                word = w,
                                translationAr = c.getString(1),
                                previewDef = c.getString(2),
                                badge = c.getString(3),
                                isCurriculum = true
                            )
                        )
                    }
                }
            }

            // 1b. English inflected stem fallback (e.g., "abandoned" -> "abandon", "children" -> "child")
            if (results.none { it.word.equals(clean, ignoreCase = true) } && results.size < limit) {
                val stems = OfflineQueryMatcher.generateEnglishStemCandidates(clean)
                for (stem in stems) {
                    if (results.size >= limit) break
                    if (seenWords.contains(stem)) continue
                    db.rawQuery(
                        """
                        SELECT cw.word, cw.translation_ar, cw.definition_en, b.short_tag
                        FROM curriculum_words cw
                        JOIN books b ON b.book_num = cw.book_num
                        WHERE cw.word_lower = ?
                        ORDER BY cw.book_num ASC
                        LIMIT 1
                        """.trimIndent(),
                        arrayOf(stem)
                    ).use { c ->
                        if (c.moveToNext()) {
                            val w = c.getString(0)
                            if (seenWords.add(w.lowercase())) {
                                results.add(
                                    0,
                                    OfflineSearchSuggestion(
                                        word = w,
                                        translationAr = c.getString(1),
                                        previewDef = c.getString(2),
                                        badge = "${c.getString(3)} · Stem",
                                        isCurriculum = true
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. General 87K dictionary prefix matches
            if (!booksOnly && results.size < limit) {
                val remaining = limit - results.size
                db.rawQuery(
                    """
                    SELECT word_lower, translation_ar
                    FROM general_dict
                    WHERE word_lower >= ? AND word_lower < ?
                    ORDER BY LENGTH(word_lower) ASC, word_lower ASC
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(clean, prefixEnd, (remaining * 3).toString())
                ).use { c ->
                    while (c.moveToNext() && results.size < limit) {
                        val w = c.getString(0)
                        if (seenWords.add(w.lowercase())) {
                            results.add(
                                OfflineSearchSuggestion(
                                    word = w,
                                    translationAr = c.getString(1),
                                    previewDef = "",
                                    badge = "Dictionary",
                                    isCurriculum = false
                                )
                            )
                        }
                    }
                }
            }
        }

        return results.take(limit)
    }

    fun searchCurriculumWords(
        query: String,
        category: String? = null,
        limit: Int = 60
    ): List<OfflineCurriculumWord> {
        val db = openDb() ?: return emptyList()
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        val result = mutableListOf<OfflineCurriculumWord>()
        val isArabic = ArabicNormalizer.containsArabic(clean)
        val normAr = if (isArabic) ArabicNormalizer.normalize(clean) else clean
        val like = "%$clean%"
        val arLike = "%$normAr%"
        val categoryClause = if (category != null) "AND cw.category = ?" else ""
        val args = mutableListOf(like, arLike, like)
        if (category != null) args.add(category)
        args.add(limit.toString())

        db.rawQuery(
            """
            SELECT cw.id, cw.word, cw.ipa, cw.pos, cw.definition_en, cw.translation_ar,
                   cw.example_en, cw.book_num, cw.unit_num, cw.topic_id, cw.word_order,
                   cw.cefr, cw.category, b.short_tag, u.title
            FROM curriculum_words cw
            JOIN books b ON b.book_num = cw.book_num
            JOIN units u ON u.book_num = cw.book_num AND u.unit_num = cw.unit_num
            WHERE (cw.word_lower LIKE ? OR cw.translation_ar_norm LIKE ? OR LOWER(cw.definition_en) LIKE ?)
            $categoryClause
            ORDER BY
                CASE WHEN cw.word_lower = '${clean.replace("'", "''")}' THEN 0
                     WHEN cw.word_lower LIKE '${clean.replace("'", "''")}%' THEN 1
                     ELSE 2 END ASC,
                cw.book_num ASC, cw.unit_num ASC
            LIMIT ?
            """.trimIndent(),
            args.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                result.add(readCurriculumWord(c))
            }
        }
        return result
    }

    private fun readCurriculumWord(c: android.database.Cursor): OfflineCurriculumWord {
        return OfflineCurriculumWord(
            id = c.getString(0),
            word = c.getString(1),
            ipa = c.getString(2),
            pos = c.getString(3),
            definitionEn = c.getString(4),
            translationAr = c.getString(5),
            exampleEn = c.getString(6),
            bookNum = c.getInt(7),
            unitNum = c.getInt(8),
            topicId = c.getString(9),
            wordOrder = c.getInt(10),
            cefr = c.getString(11),
            category = c.getString(12),
            bookShortTag = c.getString(13),
            unitTitle = c.getString(14)
        )
    }

    companion object {
        private const val TAG = "OfflineDictionaryDB"
        private const val PREFS_NAME = "offline_assets_prefs"
        private const val KEY_DB_VERSION = "offline_dict_db_version"
        private const val CURRENT_DB_VERSION = 2
        private const val ASSET_PATH = "databases/sara_dictionary.db"
        private const val DB_FILE_NAME = "sara_dictionary_v2.db"

        @Volatile
        private var INSTANCE: OfflineDictionaryDatabase? = null

        fun getInstance(context: Context): OfflineDictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineDictionaryDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        fun containsArabic(text: String): Boolean {
            return ArabicNormalizer.containsArabic(text)
        }

        fun buildDeckTagForUnit(bookShortTag: String, unitNum: Int): String {
            return "$bookShortTag - U$unitNum"
        }

        fun buildTagsForCurriculumWord(
            item: OfflineCurriculumWord,
            allOccurrences: List<OfflineCurriculumWord> = listOf(item),
            extraTags: List<String> = emptyList()
        ): String {
            val tags = LinkedHashSet<String>()
            val sourceList = if (allOccurrences.isNotEmpty()) allOccurrences else listOf(item)
            sourceList.forEach { occ ->
                if (occ.category == "ielts") {
                    tags.add("IELTS")
                } else {
                    tags.add("Essential 6 Books")
                }
                if (occ.bookShortTag.isNotBlank()) {
                    tags.add(occ.bookShortTag)
                    tags.add(buildDeckTagForUnit(occ.bookShortTag, occ.unitNum))
                }
                if (occ.cefr.isNotBlank()) {
                    tags.add(occ.cefr)
                }
            }
            extraTags.map { it.trim() }.filter { it.isNotBlank() }.forEach { tags.add(it) }
            return tags.joinToString(", ")
        }

        fun mergeTags(existingTags: String, newTags: String): String {
            val merged = LinkedHashSet<String>()
            existingTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { merged.add(it) }
            newTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { merged.add(it) }
            return merged.joinToString(", ")
        }

        fun toWordEntity(
            item: OfflineCurriculumWord,
            allOccurrences: List<OfflineCurriculumWord> = listOf(item),
            isBookmarked: Boolean = false,
            extraTags: List<String> = emptyList()
        ): WordEntity {
            val bestTranslation = item.translationAr.ifBlank {
                allOccurrences.firstOrNull { it.translationAr.isNotBlank() }?.translationAr ?: ""
            }
            val bestIpa = item.ipa.ifBlank {
                allOccurrences.firstOrNull { it.ipa.isNotBlank() }?.ipa ?: ""
            }
            val bestPos = item.pos.ifBlank {
                allOccurrences.firstOrNull { it.pos.isNotBlank() }?.pos ?: ""
            }
            return WordEntity(
                word = item.word.trim().lowercase(),
                phonetic = bestIpa,
                partOfSpeech = bestPos,
                definition = item.definitionEn,
                synonyms = "",
                antonyms = "",
                example = item.exampleEn,
                mnemonic = "",
                imagePrompt = "",
                isBookmarked = isBookmarked,
                timestamp = System.currentTimeMillis(),
                dueDate = java.time.LocalDate.now().toEpochDay(),
                tags = buildTagsForCurriculumWord(item, allOccurrences, extraTags),
                arabicMeaning = bestTranslation
            )
        }
    }
}
