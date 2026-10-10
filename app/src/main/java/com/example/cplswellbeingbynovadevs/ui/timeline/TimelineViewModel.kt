package com.example.cplswellbeingbynovadevs.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.UsageSession
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimelineViewModel(val repository: WellbeingRepository) : ViewModel() {
    private val _sessions = MutableStateFlow<List<UsageSession>>(emptyList())
    val sessions: StateFlow<List<UsageSession>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.selectedPartner.collect {
                loadTimeline()
            }
        }
    }

    fun loadTimeline() {
        val partner = repository.selectedPartner.value
        val targetId = partner?.partnerId ?: SupabaseClient.currentUserId ?: ""
        if (targetId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            val list = repository.getUsageSessionsForTarget(targetId, daysAgo = 0)
            _sessions.value = list.sortedByDescending { it.startedAt }
            _isLoading.value = false
        }
    }
}
