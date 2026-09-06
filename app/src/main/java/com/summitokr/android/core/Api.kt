package com.summitokr.android.core

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Api {
    // Auth
    @POST("auth/login") suspend fun login(@Body body: LoginReq): ApiResp<AuthResponse>
    @POST("auth/register") suspend fun register(@Body body: RegisterReq): ApiResp<AuthResponse>
    @POST("auth/refresh") fun refresh(@Body body: RefreshReq): Call<ApiResp<AuthTokens>>
    @GET("auth/profile") suspend fun profile(): ApiResp<User>

    // Summary / Gantt
    @GET("summary") suspend fun summary(): ApiResp<SummaryData>
    @GET("gantt") suspend fun gantt(@Query("scope") scope: String? = null): ApiResp<GanttData>

    // Goal groups
    @GET("goal-groups") suspend fun goalTree(@Query("includeObjectives") include: Boolean = true): ApiResp<List<GoalGroup>>
    @POST("goal-groups") suspend fun createGroup(@Body body: CreateGoalGroupReq): ApiResp<GoalGroup>
    @PATCH("goal-groups/{id}") suspend fun updateGroup(@Path("id") id: String, @Body body: UpdateGoalGroupReq): ApiResp<GoalGroup>
    @DELETE("goal-groups/{id}") suspend fun deleteGroup(@Path("id") id: String): ApiResp<NoBody>

    // Objectives
    @GET("objectives") suspend fun objectives(
        @Query("goalGroupId") groupId: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
    ): ApiResp<ObjectiveListData>
    @GET("objectives/{id}") suspend fun objective(@Path("id") id: String): ApiResp<Objective>
    @POST("objectives") suspend fun createObjective(@Body body: CreateObjectiveReq): ApiResp<Objective>
    @PATCH("objectives/{id}") suspend fun updateObjective(@Path("id") id: String, @Body body: UpdateObjectiveReq): ApiResp<Objective>
    @DELETE("objectives/{id}") suspend fun deleteObjective(@Path("id") id: String): ApiResp<NoBody>

    // Key results
    @GET("key-results/by-objective/{objectiveId}") suspend fun keyResults(@Path("objectiveId") objectiveId: String): ApiResp<List<KeyResult>>
    @POST("key-results") suspend fun createKeyResult(@Body body: CreateKeyResultReq): ApiResp<KeyResult>
    @PATCH("key-results/{id}") suspend fun updateKeyResult(@Path("id") id: String, @Body body: UpdateKeyResultReq): ApiResp<KeyResult>
    @DELETE("key-results/{id}") suspend fun deleteKeyResult(@Path("id") id: String): ApiResp<NoBody>

    // Records
    @GET("records/trend/{keyResultId}") suspend fun trend(@Path("keyResultId") keyResultId: String): ApiResp<List<TrendPoint>>
    @POST("records") suspend fun createRecord(@Body body: CreateRecordReq): ApiResp<KrRecord>
    @DELETE("records/{id}") suspend fun deleteRecord(@Path("id") id: String): ApiResp<NoBody>

    // Memos
    @GET("memos") suspend fun memos(@Query("ownerType") ownerType: String, @Query("ownerId") ownerId: String): ApiResp<List<Memo>>
    @POST("memos") suspend fun createMemo(@Body body: CreateMemoReq): ApiResp<Memo>
    @DELETE("memos/{id}") suspend fun deleteMemo(@Path("id") id: String): ApiResp<NoBody>

    // Tasks
    @GET("tasks") suspend fun tasks(@Query("status") status: String? = null, @Query("date") date: String? = null): ApiResp<List<Task>>
    @POST("tasks") suspend fun createTask(@Body body: CreateTaskReq): ApiResp<Task>
    @PATCH("tasks/{id}") suspend fun updateTask(@Path("id") id: String, @Body body: UpdateTaskReq): ApiResp<Task>
    @POST("tasks/{id}/complete") suspend fun completeTask(@Path("id") id: String, @Body body: CompleteTaskReq): ApiResp<Task>
    @DELETE("tasks/{id}") suspend fun deleteTask(@Path("id") id: String): ApiResp<NoBody>
    @POST("tasks/delete-overdue") suspend fun deleteOverdue(): ApiResp<CountResp>

    // Visions
    @GET("visions") suspend fun visions(): ApiResp<List<Vision>>
    @POST("visions") suspend fun createVision(@Body body: CreateVisionReq): ApiResp<Vision>
    @PATCH("visions/{id}") suspend fun updateVision(@Path("id") id: String, @Body body: UpdateVisionReq): ApiResp<Vision>
    @POST("visions/{id}/achieve") suspend fun achieveVision(@Path("id") id: String): ApiResp<Vision>
    @POST("visions/{id}/reset-status") suspend fun resetVision(@Path("id") id: String): ApiResp<Vision>
    @DELETE("visions/{id}") suspend fun deleteVision(@Path("id") id: String): ApiResp<NoBody>

    // Focus cycles
    @GET("focus-cycles/active") suspend fun activeCycle(): ApiResp<FocusCycle?>
    @POST("focus-cycles") suspend fun createCycle(@Body body: CreateFocusCycleReq): ApiResp<FocusCycle>
    @PATCH("focus-cycles/{cycleId}/objectives/{objectiveId}/weight") suspend fun updateWeight(
        @Path("cycleId") cycleId: String, @Path("objectiveId") objectiveId: String, @Body body: WeightReq,
    ): ApiResp<NoBody>
    @POST("focus-cycles/{cycleId}/end") suspend fun endCycle(@Path("cycleId") cycleId: String): ApiResp<NoBody>

    // Reviews
    @GET("reviews") suspend fun reviews(@Query("type") type: String? = null): ApiResp<List<Review>>
    @GET("reviews/by-objective/{objectiveId}") suspend fun reviewsByObjective(@Path("objectiveId") objectiveId: String): ApiResp<List<Review>>
    @POST("reviews") suspend fun createReview(@Body body: CreateReviewReq): ApiResp<Review>
    @DELETE("reviews/{id}") suspend fun deleteReview(@Path("id") id: String): ApiResp<NoBody>

    // Check-in
    @GET("checkins/status") suspend fun checkinStatus(): ApiResp<CheckInStatus>
    @PUT("checkins/this-week") suspend fun checkin(@Body body: CheckInReq): ApiResp<NoBody>

    // Notifications
    @GET("notifications") suspend fun notifications(): ApiResp<NotifListData>
    @POST("notifications/{id}/read") suspend fun markRead(@Path("id") id: String): ApiResp<NoBody>
    @POST("notifications/read-all") suspend fun markAllRead(): ApiResp<NoBody>

    // Recycle
    @GET("recycle") suspend fun recycle(): ApiResp<List<RecycleItem>>
    @POST("recycle/restore") suspend fun restore(@Body body: RecycleRefReq): ApiResp<NoBody>
    @POST("recycle/destroy") suspend fun destroy(@Body body: RecycleRefReq): ApiResp<NoBody>
    @DELETE("recycle/empty") suspend fun emptyRecycle(): ApiResp<CountResp>

    // AI
    @POST("ai/plan-goal") suspend fun aiPlanGoal(@Body body: AiPlanGoalReq): ApiResp<AiPlanGoalResult>
    @POST("ai/plan-tasks") suspend fun aiPlanTasks(@Body body: AiPlanTasksReq): ApiResp<AiPlanTaskResult>
    @POST("ai/suggest-score") suspend fun aiSuggestScore(@Body body: AiSuggestScoreReq): ApiResp<AiSuggestScoreResult>
    @POST("ai/suggest-motivations") suspend fun aiMotivations(@Body body: AiMotivationsReq): ApiResp<AiMotivationsResult>
    @POST("ai/weekly-report") suspend fun aiWeeklyReport(): ApiResp<AiWeeklyReportResult>
    @GET("ai/usage") suspend fun aiUsage(): ApiResp<AiUsageStat>

    // Feedback
    @POST("feedback") suspend fun feedback(@Body body: FeedbackReq): ApiResp<NoBody>
}