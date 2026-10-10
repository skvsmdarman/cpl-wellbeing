package com.example.cplswellbeingbynovadevs.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(val repository: WellbeingRepository) : ViewModel() {
    private val _usersList = MutableStateFlow<List<UserProfile>>(emptyList())
    val usersList: StateFlow<List<UserProfile>> = _usersList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedUserForEdit = MutableStateFlow<UserProfile?>(null)
    val selectedUserForEdit: StateFlow<UserProfile?> = _selectedUserForEdit.asStateFlow()

    private val _showCreateUserDialog = MutableStateFlow(false)
    val showCreateUserDialog: StateFlow<Boolean> = _showCreateUserDialog.asStateFlow()

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _usersList.value = repository.adminFetchAllUsers()
            _isLoading.value = false
        }
    }

    fun openCreateUserDialog() {
        _showCreateUserDialog.value = true
        _statusMessage.value = null
    }

    fun closeCreateUserDialog() {
        _showCreateUserDialog.value = false
    }

    fun selectUserForEdit(user: UserProfile?) {
        _selectedUserForEdit.value = user
        _statusMessage.value = null
    }

    fun createNewUser(email: String, pass: String, displayName: String, role: String, onSuccess: () -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            _statusMessage.value = "Please fill email and password"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val res = repository.adminCreateUser(email, pass, displayName, role)
            _isLoading.value = false
            res.fold(
                onSuccess = {
                    closeCreateUserDialog()
                    loadAllUsers()
                    onSuccess()
                },
                onFailure = { err ->
                    _statusMessage.value = err.message ?: "Failed to create user"
                }
            )
        }
    }

    fun updateUserRole(user: UserProfile, newRole: String) {
        val updated = user.copy(role = newRole)
        viewModelScope.launch {
            _isLoading.value = true
            repository.adminUpdateUserProfile(updated)
            loadAllUsers()
            selectUserForEdit(null)
            _isLoading.value = false
        }
    }

    fun toggleUserAccountStatus(user: UserProfile) {
        val newStatus = if (user.accountStatus == UserProfile.STATUS_ACTIVE) UserProfile.STATUS_DISABLED else UserProfile.STATUS_ACTIVE
        val updated = user.copy(accountStatus = newStatus)
        viewModelScope.launch {
            _isLoading.value = true
            repository.adminUpdateUserProfile(updated)
            loadAllUsers()
            selectUserForEdit(null)
            _isLoading.value = false
        }
    }
}
