package com.lifelensiq.app.di

import android.content.Context
import com.lifelensiq.app.data.local.AppDatabase
import com.lifelensiq.app.data.remote.FirebaseAuthRepository
import com.lifelensiq.app.data.remote.FirestoreEventSource
import com.lifelensiq.app.data.repository.EventRepositoryImpl
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.export.ExportUseCase
import com.lifelensiq.app.tracking.EventEmitter
import com.lifelensiq.app.tracking.WakeDetector
import com.lifelensiq.app.util.DeviceIdProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual DI — simple and explicit for a prototype. */
object ServiceLocator {

    private lateinit var appContext: Context
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        appContext = context.applicationContext
        // Warm up singletons, but never let a failure here crash the whole
        // process (e.g. Firebase unavailable on the device). Local tracking
        // must keep working regardless.
        try {
            eventRepository()
        } catch (e: Throwable) {
            android.util.Log.e("ServiceLocator", "eventRepository init failed", e)
        }
        try {
            authRepository()
        } catch (e: Throwable) {
            android.util.Log.e("ServiceLocator", "authRepository init failed", e)
        }
    }

    fun context(): Context {
        return if (::appContext.isInitialized) appContext
        else throw IllegalStateException("ServiceLocator not initialized. Call ServiceLocator.init(context) in Application.onCreate()")
    }

    private val _db by lazy { AppDatabase.get(appContext) }
    fun db() = _db

    private val _auth by lazy { FirebaseAuthRepository(appContext) }
    fun authRepository(): AuthRepository = _auth

    private val _remote by lazy { FirestoreEventSource() }
    fun firestoreSource() = _remote

    private val _eventRepo by lazy {
        EventRepositoryImpl(_db, _auth, _remote, DeviceIdProvider.get(appContext))
    }
    fun eventRepository(): EventRepository = _eventRepo

    private val _exportUseCase by lazy { ExportUseCase(_db, _auth) }
    fun exportUseCase(): ExportUseCase = _exportUseCase

    private val _wakeDetector by lazy { WakeDetector() }
    fun wakeDetector(): WakeDetector? = _wakeDetector

    /** Emits through the repository (write-through local). */
    fun eventEmitter(): EventEmitter = object : EventEmitter {
        override suspend fun emit(eventType: String, payload: Map<String, Any?>): String =
            _eventRepo.emit(eventType, payload)
    }

    /** Fire-and-forget emit helper for non-suspending contexts. */
    fun emitAsync(eventType: String, payload: Map<String, Any?>) {
        appScope.launch { eventEmitter().emit(eventType, payload) }
    }
}
