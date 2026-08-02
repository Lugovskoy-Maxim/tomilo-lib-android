package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TomiloApi {
    // ── Auth / profile ──────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthPayload>

    @POST("auth/yandex-token")
    suspend fun loginYandexToken(@Body body: YandexTokenRequest): ApiResponse<AuthPayload>

    @POST("auth/vk-id")
    suspend fun loginVkId(@Body body: VkIdLoginRequest): ApiResponse<AuthPayload>

    @GET("users/profile")
    suspend fun profile(): ApiResponse<UserDto>

    @GET("users/{id}")
    suspend fun publicUser(@Path("id") id: String): ApiResponse<PublicUserDto>

    // ── Catalog ─────────────────────────────────────────────────
    @GET("titles")
    suspend fun catalogTitles(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("search") search: String? = null,
        @Query("genres") genres: String? = null,
        @Query("types") types: String? = null,
        @Query("status") status: String? = null,
        @Query("sortBy") sortBy: String = "updatedAt",
        @Query("sortOrder") sortOrder: String = "desc",
        @Query("releaseYears") releaseYears: String? = null,
        @Query("ageLimits") ageLimits: String? = null,
        @Query("includeAdult") includeAdult: Boolean? = null,
    ): ApiResponse<CatalogPageDto>

    @GET("titles/filters/options")
    suspend fun catalogFilterOptions(): ApiResponse<CatalogFilterOptionsDto>

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

    @GET("chapters/{id}/next")
    suspend fun chapterNext(@Path("id") id: String): ApiResponse<ChapterDto>

    @GET("chapters/{id}/prev")
    suspend fun chapterPrev(@Path("id") id: String): ApiResponse<ChapterDto>

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

    // ── Bookmarks ───────────────────────────────────────────────
    @GET("users/profile/bookmarks")
    suspend fun bookmarks(
        @Query("category") category: String? = null,
        @Query("grouped") grouped: Boolean? = null,
    ): ApiResponse<JsonElement>

    @GET("users/profile/bookmarks/{titleId}/status")
    suspend fun bookmarkStatus(@Path("titleId") titleId: String): ApiResponse<BookmarkStatusDto>

    @POST("users/profile/bookmarks/{titleId}")
    suspend fun addBookmark(
        @Path("titleId") titleId: String,
        @Query("category") category: String = "reading",
    ): ApiResponse<JsonElement>

    @PUT("users/profile/bookmarks/{titleId}")
    suspend fun updateBookmark(
        @Path("titleId") titleId: String,
        @Body body: UpdateBookmarkRequest,
    ): ApiResponse<JsonElement>

    @DELETE("users/profile/bookmarks/{titleId}")
    suspend fun removeBookmark(@Path("titleId") titleId: String): ApiResponse<JsonElement>

    // ── Comments ────────────────────────────────────────────────
    @GET("comments")
    suspend fun comments(
        @Query("entityType") entityType: String,
        @Query("entityId") entityId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("includeReplies") includeReplies: Boolean = true,
        @Query("sortOrder") sortOrder: String = "newest",
    ): ApiResponse<CommentsPageDto>

    @POST("comments")
    suspend fun createComment(@Body body: CreateCommentRequest): ApiResponse<CommentDto>

    @POST("comments/{id}/like")
    suspend fun likeComment(@Path("id") id: String): ApiResponse<JsonElement>

    // ── Messaging ───────────────────────────────────────────────
    @GET("conversations")
    suspend fun conversations(): ApiResponse<List<ConversationPreviewDto>>

    @GET("conversations/unread-count")
    suspend fun conversationsUnread(): ApiResponse<UnreadCountDto>

    @GET("conversations/support")
    suspend fun supportConversation(): ApiResponse<ConversationPreviewDto>

    @POST("conversations")
    suspend fun createConversation(@Body body: CreateConversationRequest): ApiResponse<ConversationPreviewDto>

    @GET("conversations/{id}/messages")
    suspend fun messages(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50,
    ): ApiResponse<MessagesPageDto>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: SendMessageRequest,
    ): ApiResponse<DirectMessageDto>

    @POST("conversations/{id}/read")
    suspend fun markConversationRead(@Path("id") id: String): ApiResponse<JsonElement>

    // ── Leaderboard ─────────────────────────────────────────────
    @GET("users/leaderboard")
    suspend fun leaderboard(
        @Query("category") category: String = "level",
        @Query("period") period: String = "all",
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1,
    ): ApiResponse<LeaderboardResponseDto>

    // ── Notifications (in-app «пуши») ───────────────────────────
    @GET("notifications")
    suspend fun notifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
    ): ApiResponse<JsonElement>

    @GET("notifications/unread-count")
    suspend fun notificationsUnread(): ApiResponse<UnreadCountDto>

    @POST("notifications/mark-all-read")
    suspend fun markAllNotificationsRead(): ApiResponse<JsonElement>

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): ApiResponse<JsonElement>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): ApiResponse<JsonElement>

    // ── Reading history ─────────────────────────────────────────
    @GET("users/profile/history")
    suspend fun readingHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40,
        @Query("light") light: Boolean = true,
    ): ApiResponse<JsonElement>

    @POST("users/profile/history/{titleId}/{chapterId}")
    suspend fun addHistory(
        @Path("titleId") titleId: String,
        @Path("chapterId") chapterId: String,
    ): ApiResponse<JsonElement>

    // ── Title rating ────────────────────────────────────────────
    @POST("titles/{id}/rating")
    suspend fun rateTitle(
        @Path("id") id: String,
        @Body body: RateTitleRequest,
    ): ApiResponse<JsonElement>

    @GET("titles/{id}/my-rating")
    suspend fun myTitleRating(@Path("id") id: String): ApiResponse<JsonElement>

    // ── Admin ───────────────────────────────────────────────────
    @GET("admin/dashboard")
    suspend fun adminDashboard(): ApiResponse<AdminDashboardDto>

    @GET("admin/activity")
    suspend fun adminActivity(
        @Query("limit") limit: Int = 30,
        @Query("page") page: Int = 1,
    ): ApiResponse<JsonElement>

    @GET("admin/system")
    suspend fun adminSystem(): ApiResponse<JsonElement>

    @POST("admin/cache/clear")
    suspend fun adminClearCache(): ApiResponse<JsonElement>

    @GET("admin/users")
    suspend fun adminUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("search") search: String? = null,
        @Query("role") role: String? = null,
    ): ApiResponse<AdminUsersPageDto>

    @POST("admin/users/{id}/ban")
    suspend fun adminBanUser(
        @Path("id") id: String,
        @Body body: AdminBanRequest,
    ): ApiResponse<JsonElement>

    @POST("admin/users/{id}/unban")
    suspend fun adminUnbanUser(@Path("id") id: String): ApiResponse<JsonElement>

    @PUT("admin/users/{id}/role")
    suspend fun adminSetRole(
        @Path("id") id: String,
        @Body body: AdminRoleRequest,
    ): ApiResponse<JsonElement>

    @GET("admin/comments")
    suspend fun adminComments(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("search") search: String? = null,
    ): ApiResponse<AdminCommentsPageDto>

    @PUT("admin/comments/{id}/visibility")
    suspend fun adminCommentVisibility(
        @Path("id") id: String,
        @Body body: AdminCommentVisibilityRequest,
    ): ApiResponse<JsonElement>

    @DELETE("admin/comments/{id}")
    suspend fun adminDeleteComment(@Path("id") id: String): ApiResponse<JsonElement>

    @GET("admin/titles")
    suspend fun adminTitles(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("search") search: String? = null,
    ): ApiResponse<AdminTitlesPageDto>

    @GET("admin/comments/stats")
    suspend fun adminCommentsStats(): ApiResponse<JsonElement>
}
