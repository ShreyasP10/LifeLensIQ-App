package com.lifelensiq.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelensiq.app.data.remote.FirebaseAuthRepository
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(email: String, password: String) = authenticate { auth.register(email, password) }

    fun login(email: String, password: String) = authenticate { auth.login(email, password) }

    private fun authenticate(block: suspend () -> Result<Unit>) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = block()
            _uiState.value = result.fold(
                onSuccess = { AuthUiState() },
                onFailure = { AuthUiState(error = FirebaseAuthRepository.friendlyMessage(it)) }
            )
        }
    }
}

val AuthRepository.isLoggedIn: Boolean
    get() = state.value is AuthState.LoggedIn
