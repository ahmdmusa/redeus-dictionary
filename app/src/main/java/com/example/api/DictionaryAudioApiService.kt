package com.example.api

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "DictionaryAudio"

@JsonClass(generateAdapter = true)
data class DictionaryApiEntry(
    @Json(name = "word") val word: String? = null,
    @Json(name = "phonetic") val phonetic: String? = null,
    @Json(name = "phonetics") val phonetics: List<DictionaryApiPhonetic>? = null
)

@JsonClass(generateAdapter = true)
data class DictionaryApiPhonetic(
    @Json(name = "text") val text: String? = null,
    @Json(name = "audio") val audio: String? = null,
    @Json(name = "sourceUrl") val sourceUrl: String? = null
)

/**
 * Normalizes a raw audio URL string:
 * - Trims whitespace
 * - Discards empty strings / blank strings
 * - Prepends "https:" if protocol-relative (starts with "//")
 * - Upgrades "http://" to "https://"
 */
fun normalizeAudioUrl(rawUrl: String?): String? {
    if (rawUrl == null) return null
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return null

    return when {
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.startsWith("http://") -> "https://${trimmed.removePrefix("http://")}"
        trimmed.startsWith("https://") -> trimmed
        else -> "https://$trimmed"
    }
}

/**
 * Chooses an audio pronunciation URL from the list of candidates.
 * 1. First tries to find an audio URL matching the user's selected accent preference (UK vs US).
 * 2. If no match for the preferred accent is found, falls back to ANY available non-empty audio URL.
 * 3. Returns null ONLY if literally every entry has an empty/blank audio URL.
 */
fun selectAudioUrl(urls: List<String>, preferUk: Boolean): String? {
    if (urls.isEmpty()) return null

    val normalizedUrls = urls.mapNotNull { normalizeAudioUrl(it) }

    if (normalizedUrls.isEmpty()) return null

    // 1. Try to find an audio URL matching preferred accent
    val preferredMatch = if (preferUk) {
        normalizedUrls.firstOrNull { url ->
            val lower = url.lowercase(Locale.ROOT)
            lower.contains("-uk") || lower.contains("_uk") || lower.contains("/uk") ||
            lower.contains("-gb") || lower.contains("_gb") || lower.contains("/gb") ||
            lower.contains("british") || lower.contains("/en_uk") || lower.contains("/en_gb")
        }
    } else {
        normalizedUrls.firstOrNull { url ->
            val lower = url.lowercase(Locale.ROOT)
            lower.contains("-us") || lower.contains("_us") || lower.contains("/us") ||
            lower.contains("american") || lower.contains("/en_us")
        }
    }

    if (preferredMatch != null) {
        return preferredMatch
    }

    // 2. If no match for preferred accent, try secondary common accent if available
    val secondaryMatch = if (preferUk) {
        normalizedUrls.firstOrNull { url ->
            val lower = url.lowercase(Locale.ROOT)
            lower.contains("-us") || lower.contains("_us") || lower.contains("/us") ||
            lower.contains("american") || lower.contains("/en_us")
        }
    } else {
        normalizedUrls.firstOrNull { url ->
            val lower = url.lowercase(Locale.ROOT)
            lower.contains("-uk") || lower.contains("_uk") || lower.contains("/uk") ||
            lower.contains("-gb") || lower.contains("_gb") || lower.contains("/gb") ||
            lower.contains("british") || lower.contains("/en_uk") || lower.contains("/en_gb")
        }
    }

    // 3. Fall back to ANY non-empty audio URL from the list regardless of accent
    return secondaryMatch ?: normalizedUrls.firstOrNull()
}

class DictionaryAudioApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
) {
    private val listType = Types.newParameterizedType(List::class.java, DictionaryApiEntry::class.java)
    private val adapter = moshi.adapter<List<DictionaryApiEntry>>(listType)

    /**
     * Sanitizes the input word (trim, lowercase, URL-encode), executes the HTTP GET request,
     * logs the status code and raw response JSON body via Log.d(), and safely parses the result.
     */
    suspend fun getWordEntries(rawWord: String): List<DictionaryApiEntry> {
        val sanitizedWord = rawWord.trim().lowercase(Locale.ROOT)
        if (sanitizedWord.isBlank()) {
            Log.d(TAG, "getWordEntries called with empty/blank word")
            return emptyList()
        }

        val encodedWord = try {
            URLEncoder.encode(sanitizedWord, "UTF-8").replace("+", "%20")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to URL-encode word '$sanitizedWord'", e)
            sanitizedWord
        }

        val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$encodedWord"
        Log.d(TAG, "Fetching audio pronunciation from URL: $url (sanitized word: '$sanitizedWord')")

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "RedeusDictionaryApp/1.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val statusCode = response.code
                    val responseBody = response.body?.string() ?: ""

                    Log.d(TAG, "HTTP Response Code: $statusCode for '$sanitizedWord'")
                    Log.d(TAG, "HTTP Response Body: $responseBody")

                    if (!response.isSuccessful) {
                        Log.w(TAG, "API returned non-successful HTTP $statusCode for '$sanitizedWord'. Response: $responseBody")
                        return@withContext emptyList()
                    }

                    if (responseBody.isBlank()) {
                        Log.w(TAG, "Empty response body received for '$sanitizedWord'")
                        return@withContext emptyList()
                    }

                    try {
                        val entries = adapter.fromJson(responseBody) ?: emptyList()
                        Log.d(TAG, "Successfully parsed ${entries.size} entries for '$sanitizedWord'")
                        entries
                    } catch (parseEx: Exception) {
                        Log.e(TAG, "JSON Parsing Error for word '$sanitizedWord'. Raw JSON was: $responseBody", parseEx)
                        emptyList()
                    }
                }
            } catch (networkEx: Exception) {
                Log.e(TAG, "Network/I/O Error while fetching pronunciation for '$sanitizedWord' from $url", networkEx)
                emptyList()
            }
        }
    }

    companion object {
        fun create(): DictionaryAudioApiService {
            return DictionaryAudioApiService()
        }
    }
}
