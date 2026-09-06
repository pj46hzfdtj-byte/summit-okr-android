package com.summitokr.android.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 通用异步 UI 状态 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/** 共享加载作用域：StateFlow 线程安全，fire-and-forget 加载不绑定 VM 生命周期 */
private val LoadScope = kotlinx.coroutines.CoroutineScope(
    kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate,
)

fun <T> MutableStateFlow<UiState<T>>.load(block: suspend () -> T) {
    value = UiState.Loading
    LoadScope.launch {
        try {
            value = UiState.Success(block())
        } catch (e: ApiException) {
            value = UiState.Error(e.message ?: "请求失败")
        } catch (e: Exception) {
            value = UiState.Error("网络错误，请检查后端服务")
        }
    }
}

class AuthVM : ViewModel() {
    private val _state = MutableStateFlow<UiState<User?>>(UiState.Loading)
    val state: StateFlow<UiState<User?>> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { restore() }

    fun restore() {
        if (TokenStore.accessToken == null) {
            _state.value = UiState.Success(null)
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                _state.value = UiState.Success(NetClient.api.profile().unwrap())
            } catch (e: Exception) {
                TokenStore.clear()
                _state.value = UiState.Success(null)
            }
        }
    }

    fun login(email: String, password: String) {
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val resp = NetClient.api.login(LoginReq(email, password)).unwrap()
                TokenStore.setTokens(resp.accessToken, resp.refreshToken)
                _state.value = UiState.Success(resp.user)
            } catch (e: Exception) {
                _error.value = e.message ?: "登录失败"
            } finally {
                _busy.value = false
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val resp = NetClient.api.register(RegisterReq(email, username, password)).unwrap()
                TokenStore.setTokens(resp.accessToken, resp.refreshToken)
                _state.value = UiState.Success(resp.user)
            } catch (e: Exception) {
                _error.value = e.message ?: "注册失败"
            } finally {
                _busy.value = false
            }
        }
    }

    fun logout() {
        TokenStore.clear()
        _state.value = UiState.Success(null)
    }
}

class SummaryVM : ViewModel() {
    private val _summary = MutableStateFlow<UiState<SummaryData>>(UiState.Loading)
    val summary: StateFlow<UiState<SummaryData>> = _summary.asStateFlow()
    private val _checkin = MutableStateFlow<UiState<CheckInStatus>>(UiState.Loading)
    val checkin: StateFlow<UiState<CheckInStatus>> = _checkin.asStateFlow()

    init { refresh() }

    fun refresh() {
        _summary.load { NetClient.api.summary().unwrap() }
        _checkin.load { NetClient.api.checkinStatus().unwrap() }
    }

    fun checkinNow() {
        viewModelScope.launch {
            try {
                NetClient.api.checkin(CheckInReq(tzOffsetMin = -(java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000) * -1)).unwrapOrNull()
                _checkin.load { NetClient.api.checkinStatus().unwrap() }
                _summary.load { NetClient.api.summary().unwrap() }
            } catch (_: Exception) {
            }
        }
    }

    fun toggleTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                NetClient.api.completeTask(taskId, CompleteTaskReq(completed)).unwrapOrNull()
                _summary.load { NetClient.api.summary().unwrap() }
            } catch (_: Exception) {
            }
        }
    }
}

class GoalsVM : ViewModel() {
    private val _tree = MutableStateFlow<UiState<List<GoalGroup>>>(UiState.Loading)
    val tree: StateFlow<UiState<List<GoalGroup>>> = _tree.asStateFlow()

    init { refresh() }

    fun refresh() {
        _tree.load { NetClient.api.goalTree(true).unwrap() }
    }
}

data class GoalDetailData(val objective: Objective, val keyResults: List<KeyResult>)

class GoalDetailVM(val objectiveId: String) : ViewModel() {
    private val _detail = MutableStateFlow<UiState<GoalDetailData>>(UiState.Loading)
    val detail: StateFlow<UiState<GoalDetailData>> = _detail.asStateFlow()
    private val _trends = MutableStateFlow<Map<String, List<TrendPoint>>>(emptyMap())
    val trends: StateFlow<Map<String, List<TrendPoint>>> = _trends.asStateFlow()

    init { refresh() }

    fun refresh() {
        _detail.load {
            val obj = NetClient.api.objective(objectiveId).unwrap()
            val krs = obj.keyResults ?: NetClient.api.keyResults(objectiveId).unwrap()
            GoalDetailData(obj, krs)
        }
        viewModelScope.launch {
            try {
                val krs = _detail.value.let { if (it is UiState.Success) it.data.keyResults else emptyList() }
                val map = HashMap(_trends.value)
                for (kr in krs) {
                    map[kr.id] = NetClient.api.trend(kr.id).unwrap()
                }
                _trends.value = map
            } catch (_: Exception) {
            }
        }
    }

    fun addRecord(keyResultId: String, value: Double, note: String?) {
        viewModelScope.launch {
            try {
                NetClient.api.createRecord(CreateRecordReq(keyResultId, value, note)).unwrapOrNull()
                _trends.value = _trends.value + (keyResultId to NetClient.api.trend(keyResultId).unwrap())
                refresh()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteKeyResult(keyResultId: String) {
        viewModelScope.launch {
            try { NetClient.api.deleteKeyResult(keyResultId).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }
}

class TasksVM : ViewModel() {
    private val _tasks = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val tasks: StateFlow<UiState<List<Task>>> = _tasks.asStateFlow()

    init { refresh() }

    fun refresh() {
        _tasks.load { NetClient.api.tasks().unwrap() }
    }

    fun create(title: String, scheduledAt: String?, repeatRule: String) {
        viewModelScope.launch {
            try {
                NetClient.api.createTask(CreateTaskReq(title, scheduledAt = scheduledAt, repeatRule = repeatRule)).unwrapOrNull()
                refresh()
            } catch (_: Exception) {
            }
        }
    }

    fun toggle(task: Task) {
        viewModelScope.launch {
            try {
                NetClient.api.completeTask(task.id, CompleteTaskReq(task.status != "completed")).unwrapOrNull()
                refresh()
            } catch (_: Exception) {
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            try {
                NetClient.api.deleteTask(id).unwrapOrNull()
                refresh()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteOverdue(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val n = NetClient.api.deleteOverdue().unwrap().count
                refresh()
                onDone(n)
            } catch (_: Exception) {
            }
        }
    }
}

class VisionVM : ViewModel() {
    private val _visions = MutableStateFlow<UiState<List<Vision>>>(UiState.Loading)
    val visions: StateFlow<UiState<List<Vision>>> = _visions.asStateFlow()

    init { refresh() }

    fun refresh() {
        _visions.load { NetClient.api.visions().unwrap() }
    }

    fun create(content: String, startAge: Int?, endAge: Int?) {
        viewModelScope.launch {
            try { NetClient.api.createVision(CreateVisionReq(content, startAge, endAge)).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun update(id: String, content: String, startAge: Int?, endAge: Int?) {
        viewModelScope.launch {
            try { NetClient.api.updateVision(id, UpdateVisionReq(content, startAge, endAge)).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun achieve(id: String) {
        viewModelScope.launch {
            try { NetClient.api.achieveVision(id).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun reset(id: String) {
        viewModelScope.launch {
            try { NetClient.api.resetVision(id).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            try { NetClient.api.deleteVision(id).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }
}

class FocusVM : ViewModel() {
    private val _cycle = MutableStateFlow<UiState<FocusCycle?>>(UiState.Loading)
    val cycle: StateFlow<UiState<FocusCycle?>> = _cycle.asStateFlow()

    init { refresh() }

    fun refresh() {
        _cycle.load { NetClient.api.activeCycle().unwrapOrNull() }
    }

    fun create(name: String, objectiveIds: List<String>) {
        viewModelScope.launch {
            try {
                val now = java.time.Instant.now()
                val end = now.plusSeconds(30L * 24 * 3600)
                NetClient.api.createCycle(
                    CreateFocusCycleReq(
                        name = name,
                        startAt = now.toString(),
                        endAt = end.toString(),
                        objectives = objectiveIds.map { FocusCycleObjectiveRef(it, 100.0) },
                    )
                ).unwrapOrNull()
                refresh()
            } catch (_: Exception) {
            }
        }
    }

    fun setWeight(cycleId: String, objectiveId: String, weight: Double) {
        viewModelScope.launch {
            try { NetClient.api.updateWeight(cycleId, objectiveId, WeightReq(weight)).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun endCycle(id: String) {
        viewModelScope.launch {
            try { NetClient.api.endCycle(id).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }
}

class ReviewsVM : ViewModel() {
    private val _reviews = MutableStateFlow<UiState<List<Review>>>(UiState.Loading)
    val reviews: StateFlow<UiState<List<Review>>> = _reviews.asStateFlow()

    init { refresh() }

    fun refresh() {
        _reviews.load { NetClient.api.reviews().unwrap() }
    }

    fun create(req: CreateReviewReq) {
        viewModelScope.launch {
            try { NetClient.api.createReview(req).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }
}

class GanttVM : ViewModel() {
    private val _data = MutableStateFlow<UiState<GanttData>>(UiState.Loading)
    val data: StateFlow<UiState<GanttData>> = _data.asStateFlow()

    init { refresh() }

    fun refresh() {
        _data.load { NetClient.api.gantt().unwrap() }
    }
}

class RecycleVM : ViewModel() {
    private val _items = MutableStateFlow<UiState<List<RecycleItem>>>(UiState.Loading)
    val items: StateFlow<UiState<List<RecycleItem>>> = _items.asStateFlow()

    init { refresh() }

    fun refresh() {
        _items.load { NetClient.api.recycle().unwrap() }
    }

    fun restore(item: RecycleItem) {
        viewModelScope.launch {
            try { NetClient.api.restore(RecycleRefReq(item.entityType, item.id)).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun destroy(item: RecycleItem) {
        viewModelScope.launch {
            try { NetClient.api.destroy(RecycleRefReq(item.entityType, item.id)).unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }

    fun empty() {
        viewModelScope.launch {
            try { NetClient.api.emptyRecycle().unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }
}

class NotifVM : ViewModel() {
    private val _data = MutableStateFlow<UiState<NotifListData>>(UiState.Loading)
    val data: StateFlow<UiState<NotifListData>> = _data.asStateFlow()

    init { refresh() }

    fun refresh() {
        _data.load { NetClient.api.notifications().unwrap() }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try { NetClient.api.markAllRead().unwrapOrNull(); refresh() } catch (_: Exception) {}
        }
    }
}

class AiVM : ViewModel() {
    private val _usage = MutableStateFlow<AiUsageStat?>(null)
    val usage: StateFlow<AiUsageStat?> = _usage.asStateFlow()

    init {
        viewModelScope.launch {
            try { _usage.value = NetClient.api.aiUsage().unwrapOrNull() } catch (_: Exception) {}
        }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            try { _usage.value = NetClient.api.aiUsage().unwrapOrNull() } catch (_: Exception) {}
        }
    }
}
