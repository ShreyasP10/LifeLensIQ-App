package com.lifelensiq.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String? = null
)

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: UserProfile) : AuthState
    data class Error(val message: String) : AuthState
}

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    val userId: String?
}
