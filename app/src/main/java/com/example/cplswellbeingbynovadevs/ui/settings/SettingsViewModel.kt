package com.example.cplswellbeingbynovadevs.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.UpdateManifest
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(val repository: WellbeingRepository) : ViewModel() {
    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile
    val isPauseSharing: StateFlow<Boolean> = repository.isPauseSharing

    private val _updateManifest = MutableStateFlow<UpdateManifest?>(null)
    val updateManifest: StateFlow<UpdateManifest?> = _updateManifest.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showChangeEmailDialog = MutableStateFlow(false)
    val showChangeEmailDialog: StateFlow<Boolean> = _showChangeEmailDialog.asStateFlow()

    private val _showChangePasswordDialog = MutableStateFlow(false)
    val showChangePasswordDialog: StateFlow<Boolean> = _showChangePasswordDialog.asStateFlow()

    private val _isUpdatingAccount = MutableStateFlow(false)
    val isUpdatingAccount: StateFlow<Boolean> = _isUpdatingAccount.asStateFlow()

    private val _accountMessage = MutableStateFlow<String?>(null)
    val accountMessage: StateFlow<String?> = _accountMessage.asStateFlow()

    fun openChangeEmailDialog() {
        _showChangeEmailDialog.value = true
        _accountMessage.value = null
    }

    fun closeChangeEmailDialog() {
        _showChangeEmailDialog.value = false
    }

    fun openChangePasswordDialog() {
        _showChangePasswordDialog.value = true
        _accountMessage.value = null
    }

    fun closeChangePasswordDialog() {
        _showChangePasswordDialog.value = false
    }

    fun submitNewEmail(newEmail: String, onSuccess: () -> Unit) {
        if (newEmail.isBlank()) {
            _accountMessage.value = "Please enter a valid email address"
            return
        }
        viewModelScope.launch {
            _isUpdatingAccount.value = true
            _accountMessage.value = null
            val res = repository.updateEmail(newEmail.trim())
            _isUpdatingAccount.value = false
            res.fold(
                onSuccess = {
                    closeChangeEmailDialog()
                    onSuccess()
                },
                onFailure = { err ->
                    _accountMessage.value = err.message ?: "Failed to update email"
                }
            )
        }
    }

    fun submitNewPassword(newPassword: String, onSuccess: () -> Unit) {
        if (newPassword.isBlank() || newPassword.length < 6) {
            _accountMessage.value = "Password must be at least 6 characters"
            return
        }
        viewModelScope.launch {
            _isUpdatingAccount.value = true
            _accountMessage.value = null
            val res = repository.updatePassword(newPassword.trim())
            _isUpdatingAccount.value = false
            res.fold(
                onSuccess = {
                    closeChangePasswordDialog()
                    onSuccess()
                },
                onFailure = { err ->
                    _accountMessage.value = err.message ?: "Failed to update password"
                }
            )
        }
    }

    fun togglePauseSharing() {
        repository.togglePauseSharing()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val manifest = repository.checkForAppUpdate()
            _isCheckingUpdate.value = false
            if (manifest != null && manifest.latestVersionCode > 1) {
                _updateManifest.value = manifest
                _showUpdateDialog.value = true
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun signOut() {
        repository.signOut()
    }
}
