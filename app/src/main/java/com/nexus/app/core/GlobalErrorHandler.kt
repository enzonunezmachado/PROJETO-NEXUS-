package com.nexus.app.core

import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.entity.ErrorLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.UUID

class GlobalErrorHandler(
    private val databaseProvider: () -> NexusDatabase,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            runBlocking(Dispatchers.IO) {
                val now = DateTimeUtils.nowEpochMillis()
                databaseProvider().errorLogDao().insert(
                    ErrorLogEntity(
                        id = UUID.randomUUID().toString(),
                        source = "Global (${thread.name})",
                        message = throwable.message ?: throwable::class.simpleName ?: "Exceção não tratada",
                        stackTrace = throwable.stackTraceToString().take(4000),
                        occurredAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } catch (_: Throwable) {
            // Evita loop recursivo caso a gravacao no banco falhe durante erro fatal
        } finally {
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
