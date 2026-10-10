package com.example.cplswellbeingbynovadevs.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import com.example.cplswellbeingbynovadevs.data.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: WellbeingRepository) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onEmailChanged(value: String) { _email.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }
    fun onDisplayNameChanged(value: String) { _displayName.value = value }
    fun toggleSignUpMode() {
        _isSignUpMode.value = !_isSignUpMode.value
        _errorMessage.value = null
    }

    fun submit(onSuccess: (UserProfile) -> Unit) {
        val mail = _email.value.trim()
        val pass = _password.value.trim()

        if (mail.isEmpty() || pass.isEmpty()) {
            _errorMessage.value = "Please enter email and password"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            if (_isSignUpMode.value) {
                val name = _displayName.value.trim().ifEmpty { mail.substringBefore("@") }
                val role = if (mail.equals("mda446747@gmail.com", ignoreCase = true)) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER
                val result = repository.signUp(mail, pass, name, role)
                _isLoading.value = false
                result.fold(
                    onSuccess = { profile -> onSuccess(profile) },
                    onFailure = { err -> _errorMessage.value = err.message ?: "Sign up failed" }
                )
            } else {
                val result = repository.signIn(mail, pass)
                _isLoading.value = false
                result.fold(
                    onSuccess = { profile -> onSuccess(profile) },
                    onFailure = { err -> _errorMessage.value = err.message ?: "Sign in failed" }
                )
            }
        }
    }
}
