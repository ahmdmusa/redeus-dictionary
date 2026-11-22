package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Resolution status of a word's local image asset.
 */
enum class LocalImageOrigin {
    SAVED_PERMANENT, // Downloaded once by the user & saved permanently in filesDir/word_images/
    BUNDLED_OFFLINE, // Pre-packaged inside assets/databases/word_images.db (3,600 curated illustrations)
    NONE
}

/**
 * Coil model key representing a word's image request.
 * Priority:
 *   1. User-saved permanent file: filesDir/word_images/<safe_word>.webp (never re-downloaded)
 *   2. Bundled APK database: assets/databases/word_images.db (3,600 WebP illustrations)
 *   3. Optional online URL (only when remoteUrl is non-blank & online; once fetched it is
 *      immediately persisted to Tier 1 so it becomes a permanent local offline asset forever!)
 */
data class WordImageRequest(
    val word: String,
    val remoteUrl: String = "",
    val version: Int = 0
)

/**
 * 3-Tier Permanent Offline-First Image Store for Redeus Dictionary.
 *
 * - Tier 1: `filesDir/word_images/<safe_word>.webp` — permanent user-downloaded assets
 * - Tier 2: `word_images.db` copied from `assets/databases/word_images.db` — 3,600 bundled WebP images
 * - Tier 3: On-demand download from `remoteUrl` -> compressed to WebP -> saved into Tier 1 forever
 */
class LocalWordImageStore private constructor(
    private val context: Context
) {
    private val dbLock = Any()
    private val savedDir: File = File(context.filesDir, SAVED_IMAGES_DIR_NAME).apply { mkdirs() }

    @Volatile
    private var imagesDb: SQLiteDatabase? = null

    @Volatile
    private var dbUnavailable: Boolean = false

    @Volatile
    private var bundledWordKeysCache: Set<String>? = null

    private val savedWordFileNamesCache: MutableSet<String> =
        java.util.Collections.synchronizedSet(HashSet())

    @Volatile
    private var savedCacheLoaded: Boolean = false

    @Volatile
    private var warmUpStarted: Boolean = false

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Pre-copies word_images.db (28MB) and loads all ~3,586 bundled word keys + saved filenames
     * into lightweight RAM HashSets (~150KB) on a background I/O thread.
     * Eliminates main-thread SQLite / filesystem jank during card flips and dictionary detail views.
     */
    fun warmUpSync() {
        ensureSavedCacheLoaded()
        ensureBundledKeysLoaded()
    }

    private fun ensureSavedCacheLoaded() {
        if (savedCacheLoaded) return
        synchronized(savedWordFileNamesCache) {
            if (savedCacheLoaded) return
            try {
                val files = savedDir.listFiles { f -> f.isFile && f.name.endsWith(".webp") && f.length() > 64L }
                savedWordFileNamesCache.clear()
                files?.forEach { savedWordFileNamesCache.add(it.name) }
            } catch (_: Throwable) {
            }
            savedCacheLoaded = true
        }
    }

    private fun ensureBundledKeysLoaded(): Set<String>? {
        bundledWordKeysCache?.let { return it }
        val db = openImagesDb() ?: return null
        return try {
            val keys = HashSet<String>(4096)
            db.rawQuery("SELECT word_lower FROM images", null).use { c ->
                while (c.moveToNext()) {
                    keys.add(c.getString(0))
                }
            }
            bundledWordKeysCache = keys
            keys
        } catch (e: Exception) {
            Log.w(TAG, "Failed to preload bundled image keys", e)
            null
        }
    }

    fun isWarmedUp(): Boolean = savedCacheLoaded && (bundledWordKeysCache != null || dbUnavailable)

    fun normalizedKey(word: String): String {
        return word.trim().lowercase(Locale.ROOT)
    }

    fun safeFileName(word: String): String {
        val norm = normalizedKey(word)
        val cleaned = norm.replace(Regex("[^a-z0-9_-]"), "_").trim('_')
        val prefix = cleaned.ifBlank { "word" }.take(64)
        return "${prefix}_${norm.hashCode().toUInt().toString(16)}.webp"
    }

    fun savedImageFile(word: String): File {
        if (!savedDir.exists()) savedDir.mkdirs()
        return File(savedDir, safeFileName(word))
    }

    fun hasSavedPermanentImage(word: String): Boolean {
        if (savedCacheLoaded) {
            return savedWordFileNamesCache.contains(safeFileName(word))
        }
        val f = savedImageFile(word)
        return f.exists() && f.isFile && f.length() > 64L
    }

    fun hasBundledImage(word: String): Boolean {
        val key = normalizedKey(word)
        if (key.isBlank()) return false
        bundledWordKeysCache?.let { return it.contains(key) }
        val db = openImagesDb() ?: return false
        return try {
            db.rawQuery("SELECT 1 FROM images WHERE word_lower = ? LIMIT 1", arrayOf(key)).use { c ->
                c.moveToFirst()
            }
        } catch (e: Exception) {
            Log.w(TAG, "hasBundledImage failed for '$key'", e)
            false
        }
    }

    /**
     * Fast non-blocking RAM-only lookup for Compose UI thread.
     * Returns null if background warm-up hasn't finished yet (caller resolves on Dispatchers.IO).
     */
    fun getLocalImageOriginIfCached(word: String): LocalImageOrigin? {
        if (!isWarmedUp()) return null
        val key = normalizedKey(word)
        if (key.isBlank()) return LocalImageOrigin.NONE
        if (savedWordFileNamesCache.contains(safeFileName(word))) {
            return LocalImageOrigin.SAVED_PERMANENT
        }
        if (bundledWordKeysCache?.contains(key) == true) {
            return LocalImageOrigin.BUNDLED_OFFLINE
        }
        return LocalImageOrigin.NONE
    }

    fun getLocalImageOrigin(word: String): LocalImageOrigin {
        if (hasSavedPermanentImage(word)) return LocalImageOrigin.SAVED_PERMANENT
        if (hasBundledImage(word)) return LocalImageOrigin.BUNDLED_OFFLINE
        return LocalImageOrigin.NONE
    }

    fun hasAnyLocalImage(word: String): Boolean = getLocalImageOrigin(word) != LocalImageOrigin.NONE

    fun getBundledImageBytes(word: String): ByteArray? {
        val key = normalizedKey(word)
        if (key.isBlank()) return null
        val db = openImagesDb() ?: return null
        return try {
            db.rawQuery("SELECT webp FROM images WHERE word_lower = ? LIMIT 1", arrayOf(key)).use { c ->
                if (c.moveToFirst()) c.getBlob(0) else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getBundledImageBytes failed for '$key'", e)
            null
        }
    }

    fun getBundledImageCount(): Int {
        bundledWordKeysCache?.let { return it.size }
        val db = openImagesDb() ?: return 0
        return try {
            db.rawQuery("SELECT COUNT(*) FROM images", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    fun getSavedPermanentImageCount(): Int {
        if (savedCacheLoaded) return savedWordFileNamesCache.size
        return savedDir.listFiles { f -> f.isFile && f.name.endsWith(".webp") && f.length() > 64L }?.size ?: 0
    }

    /**
     * Permanently saves raw image bytes (JPEG/PNG/WebP) as an optimized WebP file under
     * `filesDir/word_images/` so this word never needs to download its image again.
     */
    fun saveImageBytesPermanently(word: String, rawBytes: ByteArray): File? {
        if (word.isBlank() || rawBytes.size < 64) return null
        return try {
            val outFile = savedImageFile(word)
            val tmpFile = File(savedDir, "${outFile.name}.tmp")
            val finalBytes = transcodeToWebpIfPossible(rawBytes) ?: rawBytes
            FileOutputStream(tmpFile).use { fos ->
                fos.write(finalBytes)
                fos.flush()
            }
            if (outFile.exists()) outFile.delete()
            if (!tmpFile.renameTo(outFile)) {
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }
            savedWordFileNamesCache.add(outFile.name)
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save permanent image for '$word'", e)
            null
        }
    }

    /**
     * Downloads an image URL once and stores it permanently in Tier 1 (`filesDir/word_images/`).
     * Returns the saved local file on success, or null on network/decode error.
     */
    suspend fun downloadAndSavePermanently(word: String, remoteUrl: String): File? = withContext(Dispatchers.IO) {
        val url = remoteUrl.trim()
        if (word.isBlank() || !(url.startsWith("http://") || url.startsWith("https://"))) {
            return@withContext null
        }
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SadaEnglishApp/1.5 (https://github.com/ahmdmusa/redeus-dictionary)")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Remote image HTTP ${resp.code} for '$word'")
                    return@withContext null
                }
                val bytes = resp.body?.bytes() ?: return@withContext null
                if (bytes.size < 128) return@withContext null
                saveImageBytesPermanently(word, bytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download remote image for '$word': ${e.message}")
            null
        }
    }

    fun deleteSavedPermanentImage(word: String): Boolean {
        val f = savedImageFile(word)
        savedWordFileNamesCache.remove(f.name)
        return if (f.exists()) f.delete() else false
    }

    @Suppress("DEPRECATION")
    private fun transcodeToWebpIfPossible(rawBytes: ByteArray): ByteArray? {
        return try {
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null
            val maxDim = 640
            val scaled = if (decoded.width > maxDim) {
                val h = (decoded.height.toFloat() * maxDim / decoded.width.toFloat()).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, maxDim, h, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }
            val out = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            val ok = scaled.compress(format, 75, out)
            scaled.recycle()
            if (ok && out.size() > 64) out.toByteArray() else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun openImagesDb(): SQLiteDatabase? {
        imagesDb?.let { if (it.isOpen) return it }
        if (dbUnavailable) return null

        synchronized(dbLock) {
            imagesDb?.let { if (it.isOpen) return it }
            if (dbUnavailable) return null
            return try {
                val dbFile = ensureImagesDbCopied()
                if (dbFile == null || !dbFile.exists()) {
                    dbUnavailable = true
                    null
                } else {
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    ).also { imagesDb = it }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open word_images.db", e)
                dbUnavailable = true
                null
            }
        }
    }

    private fun ensureImagesDbCopied(): File? {
        val targetFile = context.getDatabasePath(IMAGES_DB_NAME)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val installedVersion = prefs.getInt(KEY_IMAGES_DB_VERSION, 0)
        if (installedVersion == CURRENT_IMAGES_DB_VERSION && targetFile.exists() && targetFile.length() > 100_000L) {
            return targetFile
        }
        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.parentFile, "$IMAGES_DB_NAME.tmp")
        return try {
            context.assets.open(IMAGES_ASSET_PATH).use { input ->
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
            prefs.edit().putInt(KEY_IMAGES_DB_VERSION, CURRENT_IMAGES_DB_VERSION).apply()
            targetFile
        } catch (e: Exception) {
            Log.w(TAG, "Offline images asset '$IMAGES_ASSET_PATH' not available: ${e.message}")
            tempFile.delete()
            null
        }
    }

    /**
     * Custom Coil Fetcher that resolves a [WordImageRequest] through the 3-tier permanent store.
     */
    inner class WordImageFetcher(
        private val request: WordImageRequest,
        private val options: Options
    ) : Fetcher {
        override suspend fun fetch(): FetchResult {
            // Tier 1: User-saved permanent file on internal storage
            val savedFile = savedImageFile(request.word)
            if (savedFile.exists() && savedFile.length() > 64L) {
                val bytes = withContext(Dispatchers.IO) { savedFile.readBytes() }
                return SourceResult(
                    source = ImageSource(Buffer().write(bytes), options.context),
                    mimeType = "image/webp",
                    dataSource = DataSource.DISK
                )
            }

            // Tier 2: Bundled 3,600 WebP database (word_images.db)
            val bundledBytes = withContext(Dispatchers.IO) { getBundledImageBytes(request.word) }
            if (bundledBytes != null && bundledBytes.size > 64) {
                return SourceResult(
                    source = ImageSource(Buffer().write(bundledBytes), options.context),
                    mimeType = "image/webp",
                    dataSource = DataSource.DISK
                )
            }

            // Tier 3: Optional remote fetch -> save permanently to Tier 1 so it never downloads again
            val remoteUrl = request.remoteUrl.trim()
            if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                val downloaded = downloadAndSavePermanently(request.word, remoteUrl)
                if (downloaded != null && downloaded.exists()) {
                    val bytes = withContext(Dispatchers.IO) { downloaded.readBytes() }
                    return SourceResult(
                        source = ImageSource(Buffer().write(bytes), options.context),
                        mimeType = "image/webp",
                        dataSource = DataSource.NETWORK
                    )
                }
            }

            throw IllegalStateException("No local or reachable image for '${request.word}'")
        }
    }

    fun createImageLoader(): ImageLoader {
        return ImageLoader.Builder(context.applicationContext)
            .okHttpClient(httpClient)
            .components {
                add(Fetcher.Factory<WordImageRequest> { data, options, _ ->
                    WordImageFetcher(data, options)
                })
            }
            .crossfade(true)
            .build()
    }

    companion object {
        private const val TAG = "LocalWordImageStore"
        private const val PREFS_NAME = "offline_assets_prefs"
        private const val KEY_IMAGES_DB_VERSION = "word_images_db_version"
        private const val CURRENT_IMAGES_DB_VERSION = 1
        private const val IMAGES_DB_NAME = "word_images.db"
        private const val IMAGES_ASSET_PATH = "databases/word_images.db"
        private const val SAVED_IMAGES_DIR_NAME = "word_images"

        @Volatile
        private var INSTANCE: LocalWordImageStore? = null

        @Volatile
        private var SHARED_LOADER: ImageLoader? = null

        fun getInstance(context: Context): LocalWordImageStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalWordImageStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getSharedImageLoader(context: Context): ImageLoader {
            return SHARED_LOADER ?: synchronized(this) {
                SHARED_LOADER ?: getInstance(context).createImageLoader().also { SHARED_LOADER = it }
            }
        }
    }
}
