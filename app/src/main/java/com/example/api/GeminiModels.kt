package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// The structure we want to get back for the word details
@JsonClass(generateAdapter = true)
data class WordApiResponse(
    @Json(name = "word") val word: String,
    @Json(name = "phonetic") val phonetic: String,
    @Json(name = "partOfSpeech") val partOfSpeech: String,
    @Json(name = "definition") val definition: String,
    @Json(name = "synonyms") val synonyms: List<String>,
    @Json(name = "antonyms") val antonyms: List<String>,
    @Json(name = "example") val example: String,
    @Json(name = "mnemonic") val mnemonic: String,
    @Json(name = "imagePrompt") val imagePrompt: String
)

@JsonClass(generateAdapter = true)
data class ReadingPassageResponse(
    @Json(name = "passage") val passage: String,
    @Json(name = "questions") val questions: List<ReadingQuestion>,
    @Json(name = "grammarQuestions") val grammarQuestions: List<ReadingQuestion> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReadingQuestion(
    @Json(name = "question") val question: String,
    @Json(name = "options") val options: List<String>,
    @Json(name = "correctAnswer") val correctAnswer: String,
    @Json(name = "explanation") val explanation: String,
    @Json(name = "type") val type: String = "comprehension"
)

