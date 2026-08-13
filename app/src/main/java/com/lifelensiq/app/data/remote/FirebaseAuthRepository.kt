package com.lifelensiq.app.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.AuthState
import com.lifelensiq.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(context: Context) : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)

    init {
        _state.value = auth.currentUser?.let { AuthState.LoggedIn(it.toProfile()) } ?: AuthState.LoggedOut
        // Keep profile doc fresh on first login.
        auth.currentUser?.let { ensureProfileDoc(it) }
    }

    override val state: StateFlow<AuthState> = _state
    override val userId: String? get() = auth.currentUser?.uid

    override suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        auth.currentUser?.let { ensureProfileDoc(it) }
        _state.value = AuthState.LoggedIn(auth.currentUser!!.toProfile())
    }

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        auth.currentUser?.let { ensureProfileDoc(it) }
        _state.value = AuthState.LoggedIn(auth.currentUser!!.toProfile())
    }

    override suspend fun logout() {
        auth.signOut()
        _state.value = AuthState.LoggedOut
    }

    private fun FirebaseUser.toProfile() = UserProfile(uid, email, displayName)

    private fun ensureProfileDoc(user: FirebaseUser) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).set(
            mapOf(
                "userId" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "lastSeenAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        )
    }

    companion object {
        fun friendlyMessage(t: Throwable): String {
            val ex = t as? FirebaseAuthException
            return when (ex?.errorCode) {
                "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
                "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
                    "Invalid email or password."
                "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection."
                else -> t.message ?: "Authentication failed."
            }
        }
    }
}
