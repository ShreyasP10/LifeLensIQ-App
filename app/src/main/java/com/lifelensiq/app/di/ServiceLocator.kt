package com.lifelensiq.app.di

import android.content.Context
import com.lifelensiq.app.data.local.AppDatabase
import com.lifelensiq.app.data.remote.FirebaseAuthRepository
import com.lifelensiq.app.data.remote.FirestoreEventSource
import com.lifelensiq.app.data.repository.EventRepositoryImpl
import com.lifelensiq.app.data.repository.TimetableRepositoryImpl
import com.lifelensiq.app.domain.repository.AuthRepository
import com.lifelensiq.app.domain.repository.EventRepository
import com.lifelensiq.app.domain.repository.TimetableRepository
import com.lifelensiq.app.export.ExportUseCase
import com.lifelensiq.app.timetable.TimetableImporter
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
        // warm up singletons lazily
        authRepository()
        eventRepository()
    }

    fun context(): Context = appContext

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

    private val _timetableRepo by lazy {
        TimetableRepositoryImpl(_db, _auth, _remote)
    }
    fun timetableRepository(): TimetableRepository = _timetableRepo

    private val _exportUseCase by lazy { ExportUseCase(_db, _auth, _timetableRepo) }
    fun exportUseCase(): ExportUseCase = _exportUseCase

    fun timetableImporter() = TimetableImporter()

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
