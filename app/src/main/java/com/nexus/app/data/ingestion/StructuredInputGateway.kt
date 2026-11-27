package com.nexus.app.data.ingestion

import androidx.room.withTransaction
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import java.util.UUID

interface StructuredInputGateway {
    suspend fun <T> inTransaction(block: suspend () -> T): T

    suspend fun findRecordedOperationState(operationId: String): StructuredProcessingState?
    suspend fun recordOperationOutcome(
        operationId: String,
        state: StructuredProcessingState,
        detail: String,
        occurredAt: Long
    )

    suspend fun categoryExists(id: String): Boolean
    suspend fun projectExists(id: String): Boolean
    suspend fun personalGoalExists(id: String): Boolean
    suspend fun eventExists(id: String): Boolean
    suspend fun taskExists(id: String): Boolean
    suspend fun subtaskExists(id: String): Boolean
    suspend fun appointmentExists(id: String): Boolean
    suspend fun transactionExists(id: String): Boolean
    suspend fun taskRecurrenceExists(id: String): Boolean
    suspend fun creditCardExists(id: String): Boolean
    suspend fun installmentPurchaseExists(id: String): Boolean
    suspend fun recurringRuleExists(id: String): Boolean

    suspend fun getTaskById(id: String): TaskEntity?
    suspend fun getSubtaskById(id: String): SubtaskEntity?
    suspend fun getSubtasksByTaskId(taskId: String): List<SubtaskEntity>
    suspend fun getAppointmentById(id: String): AppointmentEntity?
    suspend fun getEventById(id: String): EventEntity?
    suspend fun getTaskRecurrenceById(id: String): TaskRecurrenceEntity?
    suspend fun getScheduleChecklistItemsByAppointmentId(appointmentId: String): List<ScheduleChecklistItemEntity>
    suspend fun getScheduleChecklistItemsByEventId(eventId: String): List<ScheduleChecklistItemEntity>
    suspend fun getProjectById(id: String): ProjectEntity?
    suspend fun getPersonalGoalById(id: String): PersonalGoalEntity?
    suspend fun getTransactionById(id: String): TransactionEntity?

    suspend fun getTaskByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): TaskEntity?
    suspend fun getStatementById(id: String): CreditCardStatementEntity?
    suspend fun getAllStatementsOnce(): List<CreditCardStatementEntity>
    suspend fun getTransactionByRecurringOccurrence(ruleId: String, occurrenceIndex: Int): TransactionEntity?
    suspend fun getTransactionsByInstallmentPurchase(purchaseId: String): List<TransactionEntity>

    suspend fun insertTask(entity: TaskEntity)
    suspend fun insertSubtask(entity: SubtaskEntity)
    suspend fun insertSubtasks(entities: List<SubtaskEntity>)
    suspend fun insertAppointment(entity: AppointmentEntity)
    suspend fun insertEvent(entity: EventEntity)
    suspend fun insertTaskRecurrence(entity: TaskRecurrenceEntity)
    suspend fun insertScheduleChecklistItems(entities: List<ScheduleChecklistItemEntity>)
    suspend fun insertProject(entity: ProjectEntity)
    suspend fun insertPersonalGoal(entity: PersonalGoalEntity)
    suspend fun insertTransaction(entity: TransactionEntity)
}

internal class RoomStructuredInputGateway(
    private val database: NexusDatabase
) : StructuredInputGateway {

    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }

    override suspend fun findRecordedOperationState(operationId: String): StructuredProcessingState? {
        val committed = database.auditHistoryDao().getAllOnce().firstOrNull { entry ->
            entry.entityType == AUDIT_ENTITY_TYPE &&
                entry.entityId == operationId &&
                (entry.action == StructuredProcessingState.APPLIED.name ||
                    entry.action == StructuredProcessingState.UPDATED.name ||
                    entry.action == StructuredProcessingState.CONFLICT_PRESERVED_LOCAL.name)
        } ?: return null
        return runCatching { StructuredProcessingState.valueOf(committed.action) }.getOrNull()
    }

    override suspend fun recordOperationOutcome(
        operationId: String,
        state: StructuredProcessingState,
        detail: String,
        occurredAt: Long
    ) {
        if (state != StructuredProcessingState.APPLIED &&
            state != StructuredProcessingState.UPDATED &&
            state != StructuredProcessingState.CONFLICT_PRESERVED_LOCAL
        ) {
            return
        }
        database.auditHistoryDao().insert(
            AuditHistoryEntity(
                id = "structured-op-$operationId-${UUID.randomUUID()}",
                entityType = AUDIT_ENTITY_TYPE,
                entityId = operationId,
                action = state.name,
                summary = detail,
                occurredAt = occurredAt,
                createdAt = occurredAt,
                updatedAt = occurredAt
            )
        )
    }

    override suspend fun categoryExists(id: String): Boolean =
        database.categoryDao().getById(id) != null

    override suspend fun projectExists(id: String): Boolean =
        database.projectDao().getById(id) != null

    override suspend fun personalGoalExists(id: String): Boolean =
        database.personalGoalDao().getById(id) != null

    override suspend fun eventExists(id: String): Boolean =
        database.eventDao().getById(id) != null

    override suspend fun taskExists(id: String): Boolean =
        database.taskDao().getById(id) != null

    override suspend fun subtaskExists(id: String): Boolean =
        database.subtaskDao().getById(id) != null

    override suspend fun appointmentExists(id: String): Boolean =
        database.appointmentDao().getById(id) != null

    override suspend fun transactionExists(id: String): Boolean =
        database.transactionDao().getById(id) != null

    override suspend fun taskRecurrenceExists(id: String): Boolean =
        database.taskRecurrenceDao().getById(id) != null

    override suspend fun creditCardExists(id: String): Boolean =
        database.creditCardDao().getById(id) != null

    override suspend fun installmentPurchaseExists(id: String): Boolean =
        database.installmentPurchaseDao().getById(id) != null

    override suspend fun recurringRuleExists(id: String): Boolean =
        database.recurringRuleDao().getById(id) != null

    override suspend fun getTaskById(id: String): TaskEntity? =
        database.taskDao().getById(id)

    override suspend fun getSubtaskById(id: String): SubtaskEntity? =
        database.subtaskDao().getById(id)

    override suspend fun getSubtasksByTaskId(taskId: String): List<SubtaskEntity> =
        database.subtaskDao().getByTaskId(taskId)

    override suspend fun getAppointmentById(id: String): AppointmentEntity? =
        database.appointmentDao().getById(id)

    override suspend fun getEventById(id: String): EventEntity? =
        database.eventDao().getById(id)

    override suspend fun getTaskRecurrenceById(id: String): TaskRecurrenceEntity? =
        database.taskRecurrenceDao().getById(id)

    override suspend fun getScheduleChecklistItemsByAppointmentId(appointmentId: String): List<ScheduleChecklistItemEntity> =
        database.scheduleChecklistItemDao().getByAppointmentId(appointmentId)

    override suspend fun getScheduleChecklistItemsByEventId(eventId: String): List<ScheduleChecklistItemEntity> =
        database.scheduleChecklistItemDao().getByEventId(eventId)

    override suspend fun getProjectById(id: String): ProjectEntity? =
        database.projectDao().getById(id)

    override suspend fun getPersonalGoalById(id: String): PersonalGoalEntity? =
        database.personalGoalDao().getById(id)

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        database.transactionDao().getById(id)

    override suspend fun getTaskByRecurrenceAndDate(
        recurrenceId: String,
        occurrenceDateIso: String
    ): TaskEntity? = database.taskDao().getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)

    override suspend fun getStatementById(id: String): CreditCardStatementEntity? =
        database.creditCardStatementDao().getById(id)

    override suspend fun getAllStatementsOnce(): List<CreditCardStatementEntity> =
        database.creditCardStatementDao().getAllOnce()

    override suspend fun getTransactionByRecurringOccurrence(
        ruleId: String,
        occurrenceIndex: Int
    ): TransactionEntity? = database.transactionDao().getRecurringOccurrence(ruleId, occurrenceIndex)

    override suspend fun getTransactionsByInstallmentPurchase(purchaseId: String): List<TransactionEntity> =
        database.transactionDao().getByInstallmentPurchaseId(purchaseId)

    override suspend fun insertTask(entity: TaskEntity) {
        database.taskDao().upsert(entity)
    }

    override suspend fun insertSubtask(entity: SubtaskEntity) {
        database.subtaskDao().upsert(entity)
    }

    override suspend fun insertSubtasks(entities: List<SubtaskEntity>) {
        if (entities.isNotEmpty()) {
            database.subtaskDao().insertAll(entities)
        }
    }

    override suspend fun insertAppointment(entity: AppointmentEntity) {
        database.appointmentDao().upsert(entity)
    }

    override suspend fun insertEvent(entity: EventEntity) {
        database.eventDao().upsert(entity)
    }

    override suspend fun insertTaskRecurrence(entity: TaskRecurrenceEntity) {
        database.taskRecurrenceDao().upsert(entity)
    }

    override suspend fun insertScheduleChecklistItems(entities: List<ScheduleChecklistItemEntity>) {
        if (entities.isNotEmpty()) {
            database.scheduleChecklistItemDao().insertAll(entities)
        }
    }

    override suspend fun insertProject(entity: ProjectEntity) {
        database.projectDao().upsert(entity)
    }

    override suspend fun insertPersonalGoal(entity: PersonalGoalEntity) {
        database.personalGoalDao().upsert(entity)
    }

    override suspend fun insertTransaction(entity: TransactionEntity) {
        database.transactionDao().upsert(entity)
    }

    companion object {
        const val AUDIT_ENTITY_TYPE = "STRUCTURED_INPUT_OP"
    }
}
