package com.example.cplswellbeingbynovadevs.ui.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.ConnectedPartner
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel(val repository: WellbeingRepository) : ViewModel() {
    val connectedPartners: StateFlow<List<ConnectedPartner>> = repository.connectedPartners

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode.asStateFlow()

    private val _inputCode = MutableStateFlow("")
    val inputCode: StateFlow<String> = _inputCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _showCodeDialog = MutableStateFlow(false)
    val showCodeDialog: StateFlow<Boolean> = _showCodeDialog.asStateFlow()

    private val _showEnterCodeDialog = MutableStateFlow(false)
    val showEnterCodeDialog: StateFlow<Boolean> = _showEnterCodeDialog.asStateFlow()

    fun onInputCodeChanged(value: String) {
        _inputCode.value = value
    }

    fun openGenerateDialog() {
        _showCodeDialog.value = true
        _statusMessage.value = null
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.generateConnectionCode()
            _isLoading.value = false
            res.fold(
                onSuccess = { code -> _generatedCode.value = code },
                onFailure = { err -> _statusMessage.value = err.message ?: "Failed to generate code" }
            )
        }
    }

    fun closeGenerateDialog() {
        _showCodeDialog.value = false
        _generatedCode.value = null
    }

    fun openEnterCodeDialog() {
        _showEnterCodeDialog.value = true
        _inputCode.value = ""
        _statusMessage.value = null
    }

    fun closeEnterCodeDialog() {
        _showEnterCodeDialog.value = false
    }

    fun submitConnectionCode(onSuccess: () -> Unit) {
        val code = _inputCode.value.trim()
        if (code.isEmpty()) {
            _statusMessage.value = "Please enter a valid connection code"
            return
        }

        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val res = repository.acceptConnectionCode(code)
            _isLoading.value = false
            res.fold(
                onSuccess = {
                    closeEnterCodeDialog()
                    onSuccess()
                },
                onFailure = { err ->
                    _statusMessage.value = err.message ?: "Failed to connect using code"
                }
            )
        }
    }

    fun disconnect(connectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.disconnectPartner(connectionId)
            _isLoading.value = false
        }
    }
}
