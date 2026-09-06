package com.summitokr.android.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive

/** 兼容 List<String> 与 JSON 字符串两种形态（后端 goal-groups 树内 motivations 为字符串） */
object StringListCompatSerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder): List<String> {
        if (decoder !is JsonDecoder) return emptyList()
        return when (val el = decoder.decodeJsonElement()) {
            is JsonArray -> el.mapNotNull { (it as? JsonNull)?.let { null } ?: it.jsonPrimitive.content }
            is JsonNull -> emptyList()
            else -> {
                val s = el.jsonPrimitive.content
                if (s.isBlank()) emptyList()
                else runCatching { Json.decodeFromString<List<String>>(s) }.getOrElse { listOf(s) }
            }
        }
    }
    override fun serialize(encoder: Encoder, value: List<String>) {
        if (encoder is JsonEncoder) encoder.encodeJsonElement(Json.encodeToJsonElement(delegate, value))
        else encoder.encodeString(Json.encodeToString(delegate, value))
    }
}

typealias StrList = @Serializable(with = StringListCompatSerializer::class) List<String>

@Serializable
data class ApiResp<T>(val code: Int = 0, val message: String? = null, val data: T? = null)

@Serializable object NoBody

// ============ Auth / User ============
@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val avatar: String? = null,
    val bio: String? = null,
    val preferredTheme: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Double = 0.0,
    val user: User = User(),
)

@Serializable
data class AuthTokens(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Double = 0.0,
)

@Serializable data class LoginReq(val email: String, val password: String)
@Serializable data class RegisterReq(val email: String, val username: String, val password: String)
@Serializable data class RefreshReq(val refreshToken: String)

// ============ Objective ============
@Serializable
data class Objective(
    val id: String = "",
    val goalGroupId: String = "",
    val title: String = "",
    val color: String = "#409EFF",
    val startAt: String? = null,
    val endAt: String? = null,
    @Serializable(with = StringListCompatSerializer::class) val motivations: List<String> = emptyList(),
    @Serializable(with = StringListCompatSerializer::class) val feasibilities: List<String> = emptyList(),
    val status: String = "unplanned",
    val weight: Double = 100.0,
    val currentProgress: Double? = null,
    val expectedProgress: Double? = null,
    val isLagging: Boolean? = null,
    val keyResultCount: Int? = null,
    val goalGroup: GoalGroupLite? = null,
    val keyResults: List<KeyResult>? = null,
)

@Serializable
data class GoalGroupLite(
    val id: String = "",
    val name: String = "",
    val color: String = "#409EFF",
    val visionId: String? = null,
)

@Serializable
data class ObjectiveListData(val list: List<Objective> = emptyList(), val total: Int = 0)

@Serializable
data class CreateObjectiveReq(
    val goalGroupId: String,
    val title: String,
    val color: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val motivations: List<String>? = null,
    val feasibilities: List<String>? = null,
)

@Serializable
data class UpdateObjectiveReq(
    val title: String? = null,
    val color: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val motivations: List<String>? = null,
    val feasibilities: List<String>? = null,
)

// ============ KeyResult ============
@Serializable
data class KeyResult(
    val id: String = "",
    val objectiveId: String = "",
    val title: String = "",
    val emoji: String = "🎯",
    val initialValue: Double = 0.0,
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val calculationType: String = "sum",
    val weight: Double = 100.0,
    val minRecordCount: Int = 0,
    val confidence: String? = null,
    val sortOrder: Int = 0,
    val currentProgress: Double? = null,
    val records: List<KrRecord>? = null,
)

@Serializable
data class CreateKeyResultReq(
    val objectiveId: String,
    val title: String,
    val emoji: String? = null,
    val initialValue: Double,
    val targetValue: Double,
    val calculationType: String? = null,
    val weight: Double? = null,
)

@Serializable
data class UpdateKeyResultReq(
    val title: String? = null,
    val confidence: String? = null,
    val weight: Double? = null,
)

@Serializable
data class KrRecord(
    val id: String = "",
    val keyResultId: String = "",
    val value: Double = 0.0,
    val note: String? = null,
    val recordedAt: String = "",
)

@Serializable data class CreateRecordReq(val keyResultId: String, val value: Double, val note: String? = null)

@Serializable
data class TrendPoint(val recordedAt: String = "", val value: Double = 0.0, val cumulativeValue: Double = 0.0)

// ============ Memo ============
@Serializable
data class Memo(
    val id: String = "",
    val ownerType: String = "",
    val ownerId: String = "",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable data class CreateMemoReq(val ownerType: String, val ownerId: String, val content: String)

// ============ Task ============
@Serializable
data class Task(
    val id: String = "",
    val objectiveId: String? = null,
    val title: String = "",
    val description: String? = null,
    val status: String = "pending",
    val completedAt: String? = null,
    val scheduledAt: String? = null,
    val repeatRule: String = "none",
    val contribution: String? = null,
)

@Serializable
data class CreateTaskReq(
    val title: String,
    val objectiveId: String? = null,
    val description: String? = null,
    val scheduledAt: String? = null,
    val repeatRule: String? = null,
    val contribution: String? = null,
)

@Serializable data class UpdateTaskReq(val title: String? = null, val scheduledAt: String? = null, val repeatRule: String? = null)
@Serializable data class CompleteTaskReq(val completed: Boolean)
@Serializable data class IdsReq(val ids: List<String>)

// ============ Vision ============
@Serializable
data class Vision(
    val id: String = "",
    val content: String = "",
    val startAge: Int? = null,
    val endAge: Int? = null,
    val status: String = "upcoming",
    val progress: Double? = null,
)

@Serializable data class CreateVisionReq(val content: String, val startAge: Int? = null, val endAge: Int? = null)
@Serializable data class UpdateVisionReq(val content: String? = null, val startAge: Int? = null, val endAge: Int? = null)

// ============ GoalGroup ============
@Serializable
data class GoalGroup(
    val id: String = "",
    val parentId: String? = null,
    val visionId: String? = null,
    val name: String = "",
    val color: String = "#409EFF",
    val sortOrder: Int = 0,
    val children: List<GoalGroup> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val vision: Vision? = null,
)

@Serializable data class CreateGoalGroupReq(val name: String, val parentId: String? = null, val color: String? = null, val visionId: String? = null)
@Serializable data class UpdateGoalGroupReq(val name: String? = null, val color: String? = null)

// ============ FocusCycle ============
@Serializable
data class FocusCycleObjectiveRef(val objectiveId: String, val weight: Double = 100.0)

@Serializable
data class FocusCycle(
    val id: String = "",
    val name: String = "",
    val startAt: String = "",
    val endAt: String = "",
    val isActive: Boolean = false,
    val cycleScore: Double? = null,
    val objectives: List<FocusCycleMember> = emptyList(),
)

@Serializable
data class FocusCycleMember(
    val objectiveId: String = "",
    val weight: Double = 100.0,
    val objective: Objective? = null,
)

@Serializable
data class CreateFocusCycleReq(
    val name: String,
    val startAt: String? = null,
    val endAt: String? = null,
    val objectives: List<FocusCycleObjectiveRef>,
)

@Serializable data class WeightReq(val weight: Double)

// ============ Review ============
@Serializable data class KrScore(val keyResultId: String, val score: Double = 0.0, val note: String? = null)

@Serializable
data class Review(
    val id: String = "",
    val objectiveId: String = "",
    val type: String = "midterm",
    val version: Int = 1,
    val krScores: List<KrScore> = emptyList(),
    val selfRating: Double = 0.0,
    val objectiveScore: Double? = null,
    val problems: String? = null,
    val solutions: String? = null,
    val thoughts: String? = null,
    val createdAt: String = "",
)

@Serializable
data class CreateReviewReq(
    val objectiveId: String,
    val type: String,
    val krScores: List<KrScore>,
    val selfRating: Double,
    val problems: String? = null,
    val solutions: String? = null,
    val thoughts: String? = null,
)

// ============ Summary / Gantt ============
@Serializable
data class SummaryData(
    val activeFocusCycle: FocusCycle? = null,
    val totalObjectives: Int = 0,
    val inProgressObjectives: Int = 0,
    val completedObjectives: Int = 0,
    val laggingObjectives: List<Objective> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val randomMotivation: String? = null,
)

@Serializable
data class GanttItem(
    val id: String = "",
    val title: String = "",
    val color: String = "#409EFF",
    val startAt: String = "",
    val endAt: String = "",
    val currentProgress: Double = 0.0,
    val expectedProgress: Double = 0.0,
    val isLagging: Boolean = false,
    val status: String = "",
    val worstConfidence: String? = null,
)

@Serializable
data class GanttData(
    val items: List<GanttItem> = emptyList(),
    val todayLine: String = "",
    val rangeStart: String = "",
    val rangeEnd: String = "",
)

// ============ CheckIn ============
@Serializable
data class CheckInStatus(
    val weekStart: String = "",
    val weekEnd: String = "",
    val done: Boolean = false,
    val krUpdatedCount: Int = 0,
    val totalActiveKrCount: Int = 0,
    val streak: Int = 0,
)

@Serializable data class CheckInReq(val note: String? = null, val tzOffsetMin: Int = 480)

// ============ Notification ============
@Serializable
data class AppNotif(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String? = null,
    val link: String? = null,
    val read: Boolean = false,
    val createdAt: String = "",
)

@Serializable data class NotifListData(val list: List<AppNotif> = emptyList(), val unread: Int = 0)

// ============ Recycle ============
@Serializable
data class RecycleItem(
    val id: String = "",
    val entityType: String = "",
    val title: String = "",
    val meta: String? = null,
    val deletedAt: String = "",
)

@Serializable data class RecycleRefReq(val entityType: String, val id: String)
@Serializable data class CountResp(val count: Int = 0)

// ============ AI ============
@Serializable data class AiPlanGoalReq(val goal: String, val context: String? = null, val goalGroupId: String? = null)
@Serializable data class AiObjectivePlan(val title: String = "", val motivations: StrList = emptyList(), val feasibilities: StrList = emptyList())
@Serializable
data class AiPlannedKr(
    val title: String = "",
    val initialValue: Double = 0.0,
    val targetValue: Double = 0.0,
    val calculationType: String = "sum",
    val emoji: String? = null,
    val weight: Double? = null,
)
@Serializable data class AiPlanGoalResult(val objective: AiObjectivePlan = AiObjectivePlan(), val keyResults: List<AiPlannedKr> = emptyList())

@Serializable data class AiPlanTasksReq(val objectiveId: String? = null, val keyResultId: String? = null, val context: String? = null)
@Serializable
data class AiPlannedTask(
    val title: String = "",
    val description: String? = null,
    val scheduledAt: String? = null,
    val repeatRule: String? = null,
    val contribution: String? = null,
)
@Serializable data class AiPlanTaskResult(val tasks: List<AiPlannedTask> = emptyList())

@Serializable data class AiSuggestScoreReq(val objectiveId: String)
@Serializable data class AiSuggestScoreResult(val krScores: List<KrScore> = emptyList(), val selfRating: Double = 0.0, val reasoning: String? = null)

@Serializable data class AiMotivationsReq(val objectiveTitle: String, val context: String? = null)
@Serializable data class AiMotivationsResult(val motivations: StrList = emptyList())

@Serializable data class AiWeeklyReportResult(val summary: String = "", val highlights: StrList = emptyList(), val risks: StrList = emptyList(), val nextWeek: StrList = emptyList())

@Serializable data class AiUsageStat(val used: Int = 0, val limit: Int = 0, val resetAt: String? = null)

// ============ Feedback ============
@Serializable data class FeedbackReq(val type: String, val content: String, val contact: String? = null)

/** 进度值归一：后端 0-1 小数，防御性处理 0-100 */
fun normProgress(v: Double?): Double = when {
    v == null -> 0.0
    v > 1.0 -> (v / 100.0).coerceIn(0.0, 1.0)
    else -> v.coerceIn(0.0, 1.0)
}

fun JsonElement?.orNull(): JsonElement? = this