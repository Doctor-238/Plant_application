package com.Plant_application.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WikimediaApiService {
    @GET("w/api.php")
    suspend fun getPageImage(
        @Query("action") action: String = "query",
        @Query("titles") titles: String,
        @Query("prop") prop: String = "pageimages",
        @Query("format") format: String = "json",
        @Query("pithumbsize") pithumbsize: Int = 800,
        @Query("redirects") redirects: String = "true"
    ): Response<WikimediaResponse>

    @GET("w/api.php")
    suspend fun searchPages(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("srsearch") srsearch: String,
        @Query("srlimit") srlimit: Int = 5,
        @Query("format") format: String = "json",
        @Query("redirects") redirects: String = "true"
    ): Response<WikimediaSearchResponse>

    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String
    ): Response<PageSummaryResponse>

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        fun create(language: String = "ko"): WikimediaApiService {
            val userAgentInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", "PlantApplication/1.0 (Android; +contact:dev@plantapp.local)")
                    .build()
                chain.proceed(requestWithUserAgent)
            }
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .build()
            return Retrofit.Builder()
                .baseUrl("https://$language.wikipedia.org/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WikimediaApiService::class.java)
        }
    }
}

@Serializable
data class WikimediaResponse(
    @SerialName("query")
    val query: WikimediaQuery? = null
)

@Serializable
data class WikimediaQuery(
    @SerialName("pages")
    val pages: Map<String, Page>? = null
)

@Serializable
data class Page(
    @SerialName("pageid")
    val pageid: Int? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("thumbnail")
    val thumbnail: Thumbnail? = null,
    @SerialName("missing")
    val missing: String? = null
)

@Serializable
data class Thumbnail(
    @SerialName("source")
    val source: String
)

@Serializable
data class WikimediaSearchResponse(
    @SerialName("query")
    val query: SearchQuery? = null
)

@Serializable
data class SearchQuery(
    @SerialName("search")
    val search: List<SearchItem>? = null
)

@Serializable
data class SearchItem(
    @SerialName("title")
    val title: String? = null,
    @SerialName("pageid")
    val pageid: Int? = null
)

@Serializable
data class PageSummaryResponse(
    @SerialName("title")
    val title: String? = null,
    @SerialName("thumbnail")
    val thumbnail: SummaryImage? = null,
    @SerialName("originalimage")
    val originalimage: SummaryImage? = null
)

@Serializable
data class SummaryImage(
    @SerialName("source")
    val source: String? = null,
    @SerialName("width")
    val width: Int? = null,
    @SerialName("height")
    val height: Int? = null
)