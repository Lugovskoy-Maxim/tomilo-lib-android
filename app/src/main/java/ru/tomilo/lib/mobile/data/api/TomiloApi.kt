package ru.tomilo.lib.mobile.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TomiloApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthPayload>

    @GET("users/profile")
    suspend fun profile(): ApiResponse<UserDto>

    @GET("titles/latest-updates")
    suspend fun latestUpdates(
        @Query("limit") limit: Int = 24,
        @Query("page") page: Int = 1,
    ): ApiResponse<List<CatalogTitleDto>>

    @GET("titles/popular")
    suspend fun popular(
        @Query("limit") limit: Int = 24,
    ): ApiResponse<List<CatalogTitleDto>>

    @GET("titles/{id}")
    suspend fun titleById(@Path("id") id: String): ApiResponse<TitleDetailDto>

    @GET("titles/slug/{slug}")
    suspend fun titleBySlug(@Path("slug") slug: String): ApiResponse<TitleDetailDto>

    @GET("chapters/title/{titleId}")
    suspend fun chaptersByTitle(
        @Path("titleId") titleId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("sortOrder") sortOrder: String = "asc",
    ): ApiResponse<ChaptersPageDto>

    @GET("chapters/{id}")
    suspend fun chapterById(@Path("id") id: String): ApiResponse<ChapterDto>

    @GET("search/autocomplete")
    suspend fun searchAutocomplete(
        @Query("q") q: String,
        @Query("limit") limit: Int = 20,
        @Query("type") type: String = "titles",
    ): ApiResponse<List<SearchHitDto>>

    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("limit") limit: Int = 20,
        @Query("type") type: String = "titles",
    ): ApiResponse<List<SearchHitDto>>
}
