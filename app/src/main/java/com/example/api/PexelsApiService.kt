package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PexelsSearchResponse(
    @Json(name = "photos") val photos: List<PexelsPhoto>? = null,
    @Json(name = "total_results") val totalResults: Int? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "per_page") val perPage: Int? = null
)

@JsonClass(generateAdapter = true)
data class PexelsPhoto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "photographer") val photographer: String? = null,
    @Json(name = "src") val src: PexelsPhotoSrc? = null
)

@JsonClass(generateAdapter = true)
data class PexelsPhotoSrc(
    @Json(name = "original") val original: String? = null,
    @Json(name = "large2x") val large2x: String? = null,
    @Json(name = "large") val large: String? = null,
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "small") val small: String? = null,
    @Json(name = "portrait") val portrait: String? = null,
    @Json(name = "landscape") val landscape: String? = null,
    @Json(name = "tiny") val tiny: String? = null
)

interface PexelsApiService {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("page") page: Int = 1
    ): PexelsSearchResponse

    companion object {
        private const val BASE_URL = "https://api.pexels.com/"

        fun create(): PexelsApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
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

            return retrofit.create(PexelsApiService::class.java)
        }
    }
}
