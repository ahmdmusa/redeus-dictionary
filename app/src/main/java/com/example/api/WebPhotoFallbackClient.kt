package com.example.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 100% Free, Zero-API-Key Web Photo Fallback using Wikipedia REST Summary + Wikimedia Commons.
 *
 * Used automatically whenever the user has not configured a Pexels API key (or when Pexels returns
 * no results), so web photo downloads work out-of-the-box for every vocabulary word and are then
 * compressed & saved permanently offline inside LocalWordImageStore.
 */
object WebPhotoFallbackClient {
    private const val TAG = "WebPhotoFallback"
    private const val USER_AGENT = "SadaEnglishApp/1.5 (https://github.com/ahmdmusa/redeus-dictionary)"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(14, TimeUnit.SECONDS)
            .build()
    }

    private val REJECTED_NAME_TOKENS = listOf(
        "logo", "icon", "flag_of", "coat_of_arms", "emblem", "seal_of",
        "signature", "autograph", "map_of", "locator_map", "blank_map",
        "diagram", "chart", "graph", "wordmark", "badge", "symbol", "button"
    )

    private fun isCleanPhotoUrl(url: String, titleOrFilename: String = ""): Boolean {
        val lowerUrl = url.lowercase()
        val lowerTitle = titleOrFilename.lowercase()
        if (!lowerUrl.startsWith("https://")) return false
        if (lowerUrl.contains(".svg") || lowerUrl.contains(".pdf") || lowerUrl.contains(".ogg") ||
            lowerUrl.contains(".webm") || lowerUrl.contains(".djvu")
        ) return false
        if (REJECTED_NAME_TOKENS.any { lowerUrl.contains(it) || lowerTitle.contains(it) }) return false
        return true
    }

    /**
     * Fetches a real web photo URL for [query] at 1-based [page] without requiring any API key.
     */
    fun fetchWebPhotoUrl(query: String, page: Int = 1): String? {
        val clean = query.trim()
        if (clean.isBlank()) return null

        val candidates = LinkedHashSet<String>()

        // 1. Wikipedia REST Summary thumbnail (best encyclopedic photo match on page 1)
        if (page <= 1) {
            fetchWikipediaSummaryPhoto(clean)?.let { candidates.add(it) }
        }

        // 2. Wikimedia Commons real bitmap photo search (supports cycling across multiple photos!)
        fetchWikimediaCommonsPhotos(clean, limit = 12).forEach { candidates.add(it) }

        if (candidates.isEmpty() && clean.contains(" ")) {
            val firstWord = clean.substringBefore(" ").trim()
            if (firstWord.length >= 3) {
                fetchWikipediaSummaryPhoto(firstWord)?.let { candidates.add(it) }
                fetchWikimediaCommonsPhotos(firstWord, limit = 8).forEach { candidates.add(it) }
            }
        }

        if (candidates.isEmpty()) return null
        val list = candidates.toList()
        val idx = ((page - 1).coerceAtLeast(0)) % list.size
        return list[idx]
    }

    private fun fetchWikipediaSummaryPhoto(term: String): String? {
        return try {
            val slug = URLEncoder.encode(term.replace(" ", "_"), "UTF-8")
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$slug"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val thumb = json.optJSONObject("thumbnail")?.optString("source")?.trim()
                if (!thumb.isNullOrBlank() && isCleanPhotoUrl(thumb)) thumb else null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Wikipedia summary lookup skipped for '$term': ${e.message}")
            null
        }
    }

    private fun fetchWikimediaCommonsPhotos(term: String, limit: Int): List<String> {
        return try {
            val encoded = URLEncoder.encode("$term filetype:bitmap", "UTF-8")
            val url = "https://commons.wikimedia.org/w/api.php" +
                "?action=query&generator=search&gsrnamespace=6&gsrsearch=$encoded" +
                "&gsrlimit=$limit&prop=imageinfo&iiprop=url|mime&iiurlwidth=640&format=json"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val pagesObj = json.optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
                val out = mutableListOf<String>()
                val keys = pagesObj.keys()
                while (keys.hasNext()) {
                    val pageKey = keys.next()
                    val pageObj = pagesObj.optJSONObject(pageKey) ?: continue
                    val title = pageObj.optString("title", "")
                    val infoArr = pageObj.optJSONArray("imageinfo") ?: continue
                    val info = infoArr.optJSONObject(0) ?: continue
                    val mime = info.optString("mime", "").lowercase()
                    if (!mime.startsWith("image/jpeg") && !mime.startsWith("image/png") && !mime.startsWith("image/webp")) {
                        continue
                    }
                    val thumbUrl = info.optString("thumburl", "").ifBlank { info.optString("url", "") }.trim()
                    if (thumbUrl.isNotBlank() && isCleanPhotoUrl(thumbUrl, title)) {
                        out.add(thumbUrl)
                    }
                }
                out
            }
        } catch (e: Exception) {
            Log.d(TAG, "Wikimedia Commons search skipped for '$term': ${e.message}")
            emptyList()
        }
    }
}
