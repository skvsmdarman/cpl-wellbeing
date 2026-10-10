package com.example.cplswellbeingbynovadevs.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.UsageDaily
import com.example.cplswellbeingbynovadevs.data.model.UsageSession
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AnalyticsViewModel(val repository: WellbeingRepository) : ViewModel() {
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _usageDailyList = MutableStateFlow<List<UsageDaily>>(emptyList())
    val usageDailyList: StateFlow<List<UsageDaily>> = _usageDailyList.asStateFlow()

    private val _selectedAppDetail = MutableStateFlow<UsageDaily?>(null)
    val selectedAppDetail: StateFlow<UsageDaily?> = _selectedAppDetail.asStateFlow()

    private val _appSessions = MutableStateFlow<List<UsageSession>>(emptyList())
    val appSessions: StateFlow<List<UsageSession>> = _appSessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.selectedPartner.collect {
                loadAnalyticsData()
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        loadAnalyticsData()
    }

    fun selectAppForDetail(app: UsageDaily?) {
        _selectedAppDetail.value = app
        if (app != null) {
            loadSessionsForApp(app.appId)
        }
    }

    fun loadAnalyticsData() {
        val partner = repository.selectedPartner.value
        val targetId = partner?.partnerId ?: SupabaseClient.currentUserId ?: ""
        if (targetId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            when (_selectedTab.value) {
                0 -> {
                    _usageDailyList.value = repository.getUsageDailyForTarget(targetId, daysAgo = 0)
                }
                1 -> {
                    _usageDailyList.value = repository.getUsageDailyForTarget(targetId, daysAgo = 1)
                }
                2 -> {
                    val combinedList = mutableListOf<UsageDaily>()
                    for (daysAgo in 0..6) {
                        combinedList.addAll(repository.getUsageDailyForTarget(targetId, daysAgo))
                    }
                    val aggregated = combinedList.groupBy { it.appId }.map { (appId, list) ->
                        val appName = list.firstOrNull()?.appName ?: appId
                        val totalSec = list.sumOf { it.totalSeconds }
                        val totalSessions = list.sumOf { it.sessionCount }
                        UsageDaily(
                            id = UUID.randomUUID().toString(),
                            userId = targetId,
                            appId = appId,
                            appName = appName,
                            usageDate = "7 Days",
                            totalSeconds = totalSec,
                            sessionCount = totalSessions
                        )
                    }.sortedByDescending { it.totalSeconds }
                    _usageDailyList.value = aggregated
                }
            }
            _isLoading.value = false
        }
    }

    private fun loadSessionsForApp(appId: String) {
        val partner = repository.selectedPartner.value
        val targetId = partner?.partnerId ?: SupabaseClient.currentUserId ?: ""
        if (targetId.isEmpty()) return

        viewModelScope.launch {
            val daysAgo = if (_selectedTab.value == 1) 1 else 0
            val allSessions = repository.getUsageSessionsForTarget(targetId, daysAgo = daysAgo)
            _appSessions.value = allSessions.filter { it.appId == appId }
        }
    }
}
