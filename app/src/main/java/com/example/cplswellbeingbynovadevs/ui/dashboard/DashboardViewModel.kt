package com.example.cplswellbeingbynovadevs.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.ConnectedPartner
import com.example.cplswellbeingbynovadevs.data.model.UsageDaily
import com.example.cplswellbeingbynovadevs.data.model.UsageSession
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import com.example.cplswellbeingbynovadevs.service.BatteryHelper
import com.example.cplswellbeingbynovadevs.service.BatteryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(val repository: WellbeingRepository) : ViewModel() {
    val connectedPartners: StateFlow<List<ConnectedPartner>> = repository.connectedPartners
    val selectedPartner: StateFlow<ConnectedPartner?> = repository.selectedPartner

    private val _isViewingMyself = MutableStateFlow(true)
    val isViewingMyself: StateFlow<Boolean> = _isViewingMyself.asStateFlow()

    private val _myBattery = MutableStateFlow<BatteryInfo?>(null)
    val myBattery: StateFlow<BatteryInfo?> = _myBattery.asStateFlow()

    private val _todayUsageDaily = MutableStateFlow<List<UsageDaily>>(emptyList())
    val todayUsageDaily: StateFlow<List<UsageDaily>> = _todayUsageDaily.asStateFlow()

    private val _todaySessions = MutableStateFlow<List<UsageSession>>(emptyList())
    val todaySessions: StateFlow<List<UsageSession>> = _todaySessions.asStateFlow()

    private val _sevenDaysTotalSeconds = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val sevenDaysTotalSeconds: StateFlow<List<Pair<String, Long>>> = _sevenDaysTotalSeconds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.selectedPartner.collect { partner ->
                if (partner != null) {
                    _isViewingMyself.value = false
                }
                loadTargetWellbeingData()
            }
        }
        loadData()
    }

    fun updateMyBattery(context: Context) {
        _myBattery.value = BatteryHelper.getBatteryInfo(context)
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshPartners()
            loadTargetWellbeingData()
            _isLoading.value = false
        }
    }

    fun selectMyself() {
        _isViewingMyself.value = true
        repository.selectPartner(null)
        viewModelScope.launch { loadTargetWellbeingData() }
    }

    fun selectPartner(partner: ConnectedPartner) {
        _isViewingMyself.value = false
        repository.selectPartner(partner)
        viewModelScope.launch { loadTargetWellbeingData() }
    }

    private suspend fun loadTargetWellbeingData() {
        val targetId = if (_isViewingMyself.value) {
            SupabaseClient.currentUserId ?: ""
        } else {
            selectedPartner.value?.partnerId ?: SupabaseClient.currentUserId ?: ""
        }

        if (targetId.isEmpty()) return

        val dailyList = repository.getUsageDailyForTarget(targetId, daysAgo = 0)
        val sessionList = repository.getUsageSessionsForTarget(targetId, daysAgo = 0)

        _todayUsageDaily.value = dailyList
        _todaySessions.value = sessionList

        val sevenDays = mutableListOf<Pair<String, Long>>()
        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 6 downTo 0) {
            val daysAgoCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayOfWeekIndex = daysAgoCal.get(Calendar.DAY_OF_WEEK) - 1
            val dayLabel = dayLabels.getOrElse(dayOfWeekIndex) { "Day" }

            val list = repository.getUsageDailyForTarget(targetId, daysAgo = i)
            val totalSec = list.sumOf { it.totalSeconds }
            sevenDays.add(Pair(dayLabel, totalSec))
        }
        _sevenDaysTotalSeconds.value = sevenDays
    }
}
