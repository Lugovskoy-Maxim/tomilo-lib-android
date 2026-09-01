package ru.tomilo.lib.mobile.data.api

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @POST("users/daily-bonus")
    suspend fun claimDailyBonus(): ApiResponse<DailyBonusResultDto>

    @GET("users/daily-quests")
    suspend fun dailyQuests(): ApiResponse<DailyQuestsDto>

    @POST("users/daily-quests/claim")
    suspend fun claimDailyQuest(@Body body: QuestClaimRequest): ApiResponse<QuestClaimResultDto>

    @POST("users/daily-quests/claim-all")
    suspend fun claimAllDailyQuests(): ApiResponse<QuestClaimResultDto>

    @GET("users/profile/wheel")
    suspend fun wheel(): ApiResponse<WheelDto>

    @POST("users/profile/wheel/spin")
    suspend fun spinWheel(@Body body: WheelSpinRequest): ApiResponse<WheelSpinResultDto>

    @GET("wheel/recent-wins")
    suspend fun wheelRecentWins(): ApiResponse<WheelRecentWinsDto>

    // ── Games hub ───────────────────────────────────────────────
    @GET("users/profile/inventory")
    suspend fun gameInventory(): ApiResponse<List<GameInventoryItemDto>>

    @GET("users/profile/disciples")
    suspend fun gameDisciples(): ApiResponse<GameDisciplesDto>

    @POST("users/profile/disciples/train")
    suspend fun gameTrainDisciple(@Body body: GameCharacterRequest): ApiResponse<GameTrainResultDto>

    @POST("users/profile/disciples/primary")
    suspend fun gameSetPrimaryDisciple(@Body body: GameCharacterRequest): ApiResponse<GamePrimaryResultDto>

    @POST("users/profile/disciples/warehouse")
    suspend fun gameSetDiscipleWarehouse(@Body body: GameWarehouseRequest): ApiResponse<GameSimpleResultDto>

    @PUT("users/profile/disciples/battle-squad")
    suspend fun gameSetBattleSquad(@Body body: GameBattleSquadRequest): ApiResponse<GameBattleSquadResultDto>

    @GET("users/profile/disciples/battle-match")
    suspend fun gameBattleMatch(): ApiResponse<GameBattleMatchDto?>

    @POST("users/profile/disciples/battle")
    suspend fun gameBattle(@Body body: GameBattleRequest): ApiResponse<GameBattleResultDto>

    @GET("users/profile/cards")
    suspend fun gameCards(): ApiResponse<GameCardsDto>

    @GET("users/profile/alchemy/status")
    suspend fun gameAlchemyStatus(): ApiResponse<GameAlchemyStatusDto>

    // ── Tomilo shop ─────────────────────────────────────────────
    @GET("shop/decorations/{type}")
    suspend fun shopDecorations(@Path("type") type: String): ApiResponse<List<ShopDecorationDto>>

    @GET("shop/profile/decorations")
    suspend fun ownedDecorations(): ApiResponse<List<ShopDecorationDto>>

    @POST("shop/purchase/{type}/{id}")
    suspend fun purchaseDecoration(
        @Path("type") type: String,
        @Path("id") id: String,
    ): ApiResponse<JsonElement>

    @PUT("shop/equip/{type}/{id}")
    suspend fun equipDecoration(
        @Path("type") type: String,
        @Path("id") id: String,
    ): ApiResponse<JsonElement>

    @DELETE("shop/equip/{type}")
    suspend fun unequipDecoration(@Path("type") type: String): ApiResponse<JsonElement>

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

    @GET("titles/random")
    suspend fun randomTitles(
        @Query("limit") limit: Int = 1,
        @Query("includeAdult") includeAdult: Boolean? = null,
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
    suspend fun chapterNext(
        @Path("id") id: String,
        @Query("currentChapter") currentChapter: Double? = null,
    ): ApiResponse<ChapterDto>

    @GET("chapters/{id}/prev")
    suspend fun chapterPrev(
        @Path("id") id: String,
        @Query("currentChapter") currentChapter: Double? = null,
    ): ApiResponse<ChapterDto>

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

    @GET("comments/reactions/emojis")
    suspend fun commentReactionEmojis(): ApiResponse<CommentReactionEmojisDto>

    @POST("comments/{id}/reactions")
    suspend fun toggleCommentReaction(
        @Path("id") id: String,
        @Body body: CommentReactionRequest,
    ): ApiResponse<JsonElement>

    // ── Messaging ───────────────────────────────────────────────
    @GET("conversations")
    suspend fun conversations(): ApiResponse<JsonElement>

    @GET("conversations/unread-count")
    suspend fun conversationsUnread(): ApiResponse<JsonElement>

    @GET("conversations/support")
    suspend fun supportConversation(): ApiResponse<JsonElement>

    /** Admin-only: all support threads */
    @GET("conversations/support/inbox")
    suspend fun supportInbox(): ApiResponse<JsonElement>

    @POST("conversations")
    suspend fun createConversation(@Body body: CreateConversationRequest): ApiResponse<JsonElement>

    @GET("conversations/{id}/messages")
    suspend fun messages(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50,
    ): ApiResponse<JsonElement>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: SendMessageRequest,
    ): ApiResponse<JsonElement>

    @POST("conversations/{id}/read")
    suspend fun markConversationRead(@Path("id") id: String): ApiResponse<JsonElement>

    // ── Friends ─────────────────────────────────────────────────
    @GET("friends")
    suspend fun friends(): ApiResponse<List<FriendEntryDto>>

    @GET("friends/requests")
    suspend fun friendRequests(): ApiResponse<FriendRequestsDto>

    @GET("friends/search")
    suspend fun searchFriends(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<FriendSearchResultDto>>

    @GET("friends/status/{userId}")
    suspend fun friendStatus(@Path("userId") userId: String): ApiResponse<FriendStatusDto>

    @POST("friends/request")
    suspend fun sendFriendRequest(@Body body: SendFriendRequestDto): ApiResponse<JsonElement>

    @POST("friends/accept/{requestId}")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: String): ApiResponse<JsonElement>

    @POST("friends/reject/{requestId}")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: String): ApiResponse<JsonElement>

    @DELETE("friends/{userId}")
    suspend fun removeFriend(@Path("userId") userId: String): ApiResponse<JsonElement>

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
        @Query("sort") sort: String = "createdAt",
        @Query("order") order: String = "desc",
    ): ApiResponse<JsonElement>

    @GET("notifications/unread-count")
    suspend fun notificationsUnread(): ApiResponse<UnreadCountDto>

    @POST("notifications/mark-all-read")
    suspend fun markAllNotificationsRead(): ApiResponse<JsonElement>

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): ApiResponse<JsonElement>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): ApiResponse<JsonElement>

    // ── Push (FCM device token) ─────────────────────────────────
    @POST("users/profile/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): ApiResponse<JsonElement>

    @DELETE("users/profile/device-token")
    suspend fun unregisterDeviceToken(@Body body: DeviceTokenUnregisterRequest): ApiResponse<JsonElement>

    // ── Reading history / progress ──────────────────────────────
    @GET("users/profile/history")
    suspend fun readingHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40,
        @Query("light") light: Boolean = true,
    ): ApiResponse<JsonElement>

    /** Прогресс: chaptersRead / totalChapters / progressPercent */
    @GET("users/profile/progress/{titleId}")
    suspend fun readingProgress(
        @Path("titleId") titleId: String,
    ): ApiResponse<ReadingProgressDto>

    @GET("users/profile/history/{titleId}/read-ids")
    suspend fun historyReadIds(@Path("titleId") titleId: String): ApiResponse<ReadIdsDto>

    @GET("users/profile/history/{titleId}")
    suspend fun historyByTitle(@Path("titleId") titleId: String): ApiResponse<JsonElement>

    @POST("users/profile/history/{titleId}/{chapterId}")
    suspend fun addHistory(
        @Path("titleId") titleId: String,
        @Path("chapterId") chapterId: String,
    ): ApiResponse<JsonElement>

    @DELETE("users/profile/history/{titleId}")
    suspend fun deleteTitleHistory(@Path("titleId") titleId: String): ApiResponse<JsonElement>

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

    @GET("admin/settings")
    suspend fun adminSettings(): ApiResponse<AdminSiteSettingsDto>

    @PATCH("admin/settings")
    suspend fun adminUpdateSettings(
        @Body body: AdminSiteSettingsUpdate,
    ): ApiResponse<AdminSiteSettingsDto>

    @GET("admin/health")
    suspend fun adminHealth(): ApiResponse<JsonElement>

    @GET("reports")
    suspend fun adminReports(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("isResolved") isResolved: String? = null,
        @Query("reportType") reportType: String? = null,
        @Query("entityType") entityType: String? = null,
    ): ApiResponse<AdminReportsPageDto>

    @PUT("reports/{id}/status")
    suspend fun adminUpdateReportStatus(
        @Path("id") id: String,
        @Body body: AdminReportStatusRequest,
    ): ApiResponse<JsonElement>

    @DELETE("reports/{id}")
    suspend fun adminDeleteReport(@Path("id") id: String): ApiResponse<JsonElement>

    @GET("users/admin/{id}")
    suspend fun adminUserDetails(@Path("id") id: String): ApiResponse<AdminUserDto>

    @PUT("users/admin/{id}")
    suspend fun adminUpdateUser(
        @Path("id") id: String,
        @Body body: JsonObject,
    ): ApiResponse<AdminUserDto>

    @PATCH("users/admin/{id}/balance")
    suspend fun adminUpdateUserBalance(
        @Path("id") id: String,
        @Body body: AdminBalanceRequest,
    ): ApiResponse<JsonElement>

    @PUT("titles/{id}")
    suspend fun adminUpdateTitle(
        @Path("id") id: String,
        @Body body: AdminTitleUpdateRequest,
    ): ApiResponse<JsonElement>

    @DELETE("titles/{id}")
    suspend fun adminDeleteTitle(@Path("id") id: String): ApiResponse<JsonElement>

    @GET("auto-parsing")
    suspend fun autoParsingJobsRaw(): JsonElement

    @POST("auto-parsing")
    suspend fun createAutoParsingJob(
        @Body body: AutoParseCreateRequest,
    ): JsonElement

    @PATCH("auto-parsing/{id}")
    suspend fun updateAutoParsingJob(
        @Path("id") id: String,
        @Body body: AutoParseUpdateRequest,
    ): JsonElement

    @DELETE("auto-parsing/{id}")
    suspend fun deleteAutoParsingJob(@Path("id") id: String): JsonElement

    @POST("auto-parsing/{id}/check")
    suspend fun checkAutoParsingJob(@Path("id") id: String): JsonElement

    @POST("manga-parser/search-sources")
    suspend fun searchMangaSources(
        @Body body: SearchSourcesRequest,
    ): JsonElement

    @POST("manga-parser/parse-title")
    suspend fun parseMangaTitle(
        @Body body: ParseTitleRequest,
    ): JsonElement

    @GET("manga-parser/supported-sites")
    suspend fun mangaParserSites(): JsonElement

    // ── Payments / T-Bank ───────────────────────────────────────
    @POST("payments/checkout")
    suspend fun createTbankPayment(
        @Body body: CreateTbankPaymentRequest,
    ): ApiResponse<RobokassaPaymentFormDto>

    @POST("payments/robokassa/admin/test")
    suspend fun createAdminRobokassaTestPayment(): ApiResponse<RobokassaPaymentFormDto>

    @GET("payments/robokassa/status/{invId}")
    suspend fun robokassaPaymentStatus(
        @Path("invId") invId: String,
    ): ApiResponse<RobokassaPaymentStatusDto>

    @GET("payments/history")
    suspend fun paymentHistory(): ApiResponse<List<PremiumPaymentHistoryItemDto>>

    @POST("payments/premium/coins")
    suspend fun purchasePremiumWithCoins(
        @Body body: CoinPremiumPurchaseRequest,
    ): ApiResponse<CoinPremiumPurchaseResultDto>
}
