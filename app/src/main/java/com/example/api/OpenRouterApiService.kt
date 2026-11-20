package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    @Json(name = "model") val model: String = "openrouter/free",
    @Json(name = "messages") val messages: List<OpenRouterMessage>
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    @Json(name = "choices") val choices: List<OpenRouterChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessageContent? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessageContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null
)

interface OpenRouterApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"

        fun create(): OpenRouterApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(OpenRouterApiService::class.java)
        }
    }
}

data class OpenRouterTtsModel(
    val id: String,
    val name: String,
    val description: String? = null
)

val DEFAULT_OPENROUTER_TTS_MODELS = listOf(
    OpenRouterTtsModel("deepgram/flux-tts:free", "Deepgram: Flux TTS (free)", "Ultra-fast free text-to-speech"),
    OpenRouterTtsModel("google/gemini-3.8-flash-lite-tts", "Google: Gemini 3.8 Flash Lite TTS", "Fast multimodal speech generation"),
    OpenRouterTtsModel("google/gemini-3.8-flash-tts", "Google: Gemini 3.8 Flash TTS", "High-quality neural voice generation"),
    OpenRouterTtsModel("google/gemini-3.1-flash-tts-preview", "Google: Gemini 3.1 Flash TTS Preview", "Preview voice generation"),
    OpenRouterTtsModel("microsoft/mai-voice-2-flash", "Microsoft AI: MAI-Voice-2-Flash", "Natural multi-locale voice synthesis"),
    OpenRouterTtsModel("microsoft/mai-voice-2", "Microsoft AI: MAI-Voice-2", "Rich expressiveness and clarity"),
    OpenRouterTtsModel("hexgrad/kokoro-82m", "hexgrad: Kokoro 82M", "Lightweight high-quality TTS"),
    OpenRouterTtsModel("mistralai/voxtral-mini-tts-2603", "Mistral: Voxtral Mini TTS", "Accurate European voice synthesis"),
    OpenRouterTtsModel("fish-audio/s1", "Fish Audio: S1", "Zero-shot voice synthesis"),
    OpenRouterTtsModel("fish-audio/s2-pro", "Fish Audio: S2 Pro", "Professional studio speech"),
    OpenRouterTtsModel("fish-audio/s2.1-pro-free:free", "Fish Audio: S2.1 Pro Free (free)", "Free studio speech synthesis"),
    OpenRouterTtsModel("deepgram/aura-2", "Deepgram: Aura-2", "Conversational AI voice"),
    OpenRouterTtsModel("minimax/speech-2.8-hd", "MiniMax: Speech 2.8 HD", "High-definition expressive speech"),
    OpenRouterTtsModel("minimax/speech-2.8-turbo", "MiniMax: Speech 2.8 Turbo", "Low-latency expressive voice"),
    OpenRouterTtsModel("x-ai/grok-voice-tts-1.0", "SpaceXAI: Grok Voice TTS 1.0", "Multilingual voice with style control")
)

sealed class OpenRouterTtsResult {
    data class Success(val audioBytes: ByteArray) : OpenRouterTtsResult()
    data class Failure(val statusCode: Int?, val errorMessage: String) : OpenRouterTtsResult()
}

/**
 * Dedicated network call function for OpenRouter TTS synthesis.
 * Completely separate from text-generation and uses its own independently stored API key.
 */
fun fetchOpenRouterSpeechAudio(
    client: OkHttpClient,
    apiKey: String,
    modelId: String,
    text: String,
    voice: String = "alloy"
): OpenRouterTtsResult {
    if (apiKey.isBlank()) {
        return OpenRouterTtsResult.Failure(null, "No OpenRouter TTS API key configured")
    }

    val requestJsonString = try {
        val root = JSONObject()
        root.put("model", modelId.ifBlank { "deepgram/flux-tts:free" })
        root.put("input", text)
        root.put("voice", voice)
        root.toString()
    } catch (e: Exception) {
        return OpenRouterTtsResult.Failure(null, "JSON build error: ${e.message}")
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val request = Request.Builder()
        .url("https://openrouter.ai/api/v1/audio/speech")
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
        .header("HTTP-Referer", "https://vocabai.app")
        .header("X-Title", "VocabAI")
        .post(requestJsonString.toRequestBody(mediaType))
        .build()

    return try {
        client.newCall(request).execute().use { resp ->
            val statusCode = resp.code
            if (!resp.isSuccessful) {
                val responseBody = resp.body?.string() ?: ""
                val preview = responseBody.take(200).replace("\n", " ").trim()
                val cleanErrorMessage = try {
                    val json = JSONObject(responseBody)
                    val errorObj = json.optJSONObject("error")
                    val rawMsg = errorObj?.optString("message")?.takeIf { it.isNotBlank() }
                    if (rawMsg != null) "HTTP $statusCode $rawMsg" else "HTTP $statusCode $preview"
                } catch (ignored: Exception) {
                    "HTTP $statusCode $preview"
                }
                OpenRouterTtsResult.Failure(statusCode, cleanErrorMessage)
            } else {
                val bytes = resp.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    OpenRouterTtsResult.Success(bytes)
                } else {
                    OpenRouterTtsResult.Failure(statusCode, "Empty audio returned from OpenRouter")
                }
            }
        }
    } catch (e: Exception) {
        OpenRouterTtsResult.Failure(null, e.message ?: "Network error during OpenRouter TTS call")
    }
}

sealed class OpenRouterModelsResult {
    data class Success(val models: List<OpenRouterTtsModel>) : OpenRouterModelsResult()
    data class Failure(val errorMessage: String) : OpenRouterModelsResult()
}

/**
 * Fetches models live from OpenRouter's /api/v1/models endpoint and filters exclusively
 * to models with audio/speech output capabilities.
 */
fun fetchOpenRouterTtsModels(client: OkHttpClient, apiKey: String? = null): OpenRouterModelsResult {
    try {
        val requestBuilder = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .header("User-Agent", "VocabAI")
            .header("HTTP-Referer", "https://vocabai.app")
            .header("X-Title", "VocabAI")

        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
        }

        val request = requestBuilder.get().build()

        client.newCall(request).execute().use { resp ->
            val statusCode = resp.code
            val body = resp.body?.string() ?: ""

            if (!resp.isSuccessful) {
                val errorMsg = if (statusCode == 401 || statusCode == 403) {
                    "Could not load models — check your API key"
                } else {
                    "Could not load models (HTTP $statusCode) — check your connection or key"
                }
                Log.w("OpenRouterTTS", "Failed to fetch OpenRouter models: HTTP $statusCode $body")
                return OpenRouterModelsResult.Failure(errorMsg)
            }

            val json = JSONObject(body)
            val data = json.optJSONArray("data")
            if (data == null || data.length() == 0) {
                return OpenRouterModelsResult.Failure("Could not load models — empty response from OpenRouter")
            }

            val parsed = mutableListOf<OpenRouterTtsModel>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("name", id)
                val desc = item.optString("description", "")

                // Check architecture and modalities for audio/speech capability
                var isTtsCapable = false

                val arch = item.optJSONObject("architecture")
                if (arch != null) {
                    val modality = arch.optString("modality", "").lowercase(java.util.Locale.ROOT)
                    if (modality.contains("->audio") || modality.contains("->speech") || modality.contains("text->audio") || modality == "audio") {
                        isTtsCapable = true
                    }

                    val outModalities = arch.optJSONArray("output_modalities")
                    if (outModalities != null) {
                        for (m in 0 until outModalities.length()) {
                            val mod = outModalities.optString(m).lowercase(java.util.Locale.ROOT)
                            if (mod == "audio" || mod == "speech") {
                                isTtsCapable = true
                                break
                            }
                        }
                    }
                }

                val topOutModalities = item.optJSONArray("output_modalities")
                if (topOutModalities != null) {
                    for (m in 0 until topOutModalities.length()) {
                        val mod = topOutModalities.optString(m).lowercase(java.util.Locale.ROOT)
                        if (mod == "audio" || mod == "speech") {
                            isTtsCapable = true
                            break
                        }
                    }
                }

                // Also check known naming patterns for speech/TTS models
                val idLower = id.lowercase(java.util.Locale.ROOT)
                val nameLower = name.lowercase(java.util.Locale.ROOT)
                if (idLower.contains("tts") || idLower.contains("speech") || idLower.contains("voice") ||
                    idLower.contains("flux-tts") || idLower.contains("kokoro") || idLower.contains("voxtral") ||
                    idLower.contains("aura") || idLower.contains("fish-audio") ||
                    nameLower.contains("tts") || nameLower.contains("text-to-speech") || nameLower.contains("speech")) {
                    isTtsCapable = true
                }

                if (isTtsCapable && id.isNotBlank()) {
                    parsed.add(OpenRouterTtsModel(id, name, desc.takeIf { it.isNotBlank() }))
                }
            }

            if (parsed.isNotEmpty()) {
                return OpenRouterModelsResult.Success(parsed)
            } else {
                return OpenRouterModelsResult.Failure("No audio/TTS models found for this account")
            }
        }
    } catch (e: Exception) {
        Log.w("OpenRouterTTS", "Failed to fetch OpenRouter models dynamically: ${e.message}")
        return OpenRouterModelsResult.Failure("Could not load models — check your internet connection or API key")
    }
}

