package com.nexus.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.BriefingHistoryEntity
import com.nexus.app.data.db.entity.BudgetEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.ErrorLogEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.GoalMilestoneEntity
import com.nexus.app.data.db.entity.GoalProgressLogEntity
import com.nexus.app.data.db.entity.InstallmentPurchaseEntity
import com.nexus.app.data.db.entity.LoanEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.QuoteEntity
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: String = SettingsEntity.SINGLETON_ID): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    suspend fun getSettings(id: String = SettingsEntity.SINGLETON_ID): SettingsEntity?

    @Query("SELECT * FROM settings")
    suspend fun getAllOnce(): List<SettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SettingsEntity>)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM settings")
    fun observeCount(): Flow<Int>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM categories")
    fun observeCount(): Flow<Int>
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Query(
        """
        SELECT * FROM projects
        WHERE name LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun search(query: String): Flow<List<ProjectEntity>>

    @Query("UPDATE projects SET goalId = :goalId, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateGoalLink(projectId: String, goalId: String?, updatedAt: Long)

    @Query("UPDATE projects SET goalId = NULL, updatedAt = :updatedAt WHERE goalId = :goalId")
    suspend fun unlinkGoal(goalId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM projects")
    fun observeCount(): Flow<Int>
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startAt ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startAt ASC")
    suspend fun getAllOnce(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Query(
        """
        SELECT * FROM events
        WHERE name LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR locationName LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
        ORDER BY startAt ASC
        """
    )
    fun search(query: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE recurrenceId = :recurrenceId AND occurrenceDateIso = :occurrenceDateIso LIMIT 1")
    suspend fun getByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): EventEntity?

    @Query("SELECT * FROM events WHERE recurrenceId = :recurrenceId ORDER BY startAt ASC, createdAt ASC")
    suspend fun getByRecurrenceId(recurrenceId: String): List<EventEntity>

    @Query("UPDATE events SET recurrenceId = NULL, updatedAt = :updatedAt WHERE recurrenceId = :recurrenceId")
    suspend fun unlinkRecurrence(recurrenceId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events WHERE recurrenceId = :recurrenceId AND eventStatus != 'ATTENDED'")
    suspend fun deleteNonAttendedByRecurrence(recurrenceId: String)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM events")
    fun observeCount(): Flow<Int>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAt IS NOT NULL ORDER BY dueAt ASC")
    fun observeTasksWithDueDate(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    suspend fun countByProject(projectId: String): Int

    @Query("UPDATE tasks SET projectId = NULL, updatedAt = :updatedAt WHERE projectId = :projectId")
    suspend fun unlinkProject(projectId: String, updatedAt: Long)

    @Query("UPDATE tasks SET eventId = NULL, updatedAt = :updatedAt WHERE eventId = :eventId")
    suspend fun unlinkEvent(eventId: String, updatedAt: Long)

    @Query("UPDATE tasks SET goalId = :goalId, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateGoalLink(taskId: String, goalId: String?, updatedAt: Long)

    @Query("UPDATE tasks SET goalId = NULL, updatedAt = :updatedAt WHERE goalId = :goalId")
    suspend fun unlinkGoal(goalId: String, updatedAt: Long)

    @Query(
        """
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun search(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE recurrenceId = :recurrenceId AND occurrenceDateIso = :occurrenceDateIso LIMIT 1")
    suspend fun getByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE recurrenceId = :recurrenceId ORDER BY dueAt ASC, createdAt ASC")
    suspend fun getByRecurrenceId(recurrenceId: String): List<TaskEntity>

    @Query("UPDATE tasks SET recurrenceId = NULL, updatedAt = :updatedAt WHERE recurrenceId = :recurrenceId")
    suspend fun unlinkRecurrence(recurrenceId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks WHERE recurrenceId = :recurrenceId AND status != 'COMPLETED'")
    suspend fun deleteNonCompletedByRecurrence(recurrenceId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tasks")
    fun observeCount(): Flow<Int>
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByTaskId(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllOnce(): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubtaskEntity?

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getByTaskId(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM subtasks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM subtasks")
    fun observeCount(): Flow<Int>
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY startAt ASC")
    fun observeAll(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments ORDER BY startAt ASC")
    suspend fun getAllOnce(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AppointmentEntity?

    @Query(
        """
        SELECT * FROM appointments
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR location LIKE '%' || :query || '%'
        ORDER BY startAt ASC
        """
    )
    fun search(query: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE recurrenceId = :recurrenceId AND occurrenceDateIso = :occurrenceDateIso LIMIT 1")
    suspend fun getByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE recurrenceId = :recurrenceId ORDER BY startAt ASC, createdAt ASC")
    suspend fun getByRecurrenceId(recurrenceId: String): List<AppointmentEntity>

    @Query("UPDATE appointments SET recurrenceId = NULL, updatedAt = :updatedAt WHERE recurrenceId = :recurrenceId")
    suspend fun unlinkRecurrence(recurrenceId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(appointment: AppointmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AppointmentEntity>)

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM appointments WHERE recurrenceId = :recurrenceId")
    suspend fun deleteByRecurrenceId(recurrenceId: String)

    @Query("DELETE FROM appointments")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM appointments WHERE eventId = :eventId")
    suspend fun countByEvent(eventId: String): Int

    @Query("UPDATE appointments SET eventId = NULL, updatedAt = :updatedAt WHERE eventId = :eventId")
    suspend fun unlinkEvent(eventId: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM appointments")
    fun observeCount(): Flow<Int>
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CreditCardEntity?

    @Query(
        """
        SELECT * FROM credit_cards
        WHERE name LIKE '%' || :query || '%'
           OR institution LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<CreditCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: CreditCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CreditCardEntity>)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM credit_cards")
    fun observeCount(): Flow<Int>
}

@Dao
interface CreditCardStatementDao {
    @Query("SELECT * FROM credit_card_statements ORDER BY dueAt DESC")
    fun observeAll(): Flow<List<CreditCardStatementEntity>>

    @Query("SELECT * FROM credit_card_statements ORDER BY dueAt DESC")
    suspend fun getAllOnce(): List<CreditCardStatementEntity>

    @Query("SELECT * FROM credit_card_statements WHERE creditCardId = :cardId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getByCardAndMonth(cardId: String, yearMonth: String): CreditCardStatementEntity?

    @Query("SELECT * FROM credit_card_statements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CreditCardStatementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(statement: CreditCardStatementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CreditCardStatementEntity>)

    @Query("DELETE FROM credit_card_statements")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM credit_card_statements")
    fun observeCount(): Flow<Int>
}

@Dao
interface InstallmentPurchaseDao {
    @Query("SELECT * FROM installment_purchases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InstallmentPurchaseEntity>>

    @Query("SELECT * FROM installment_purchases ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<InstallmentPurchaseEntity>

    @Query("SELECT * FROM installment_purchases WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InstallmentPurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(purchase: InstallmentPurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InstallmentPurchaseEntity>)

    @Query("DELETE FROM installment_purchases WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM installment_purchases")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM installment_purchases")
    fun observeCount(): Flow<Int>
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY startAt ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules ORDER BY startAt ASC")
    suspend fun getAllOnce(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecurringRuleEntity>)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recurring_rules")
    fun observeCount(): Flow<Int>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY COALESCE(dueAt, occurredAt) DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE eventId = :eventId AND isTicketObligation = 1 LIMIT 1")
    suspend fun getTicketObligationByEvent(eventId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE eventId = :eventId AND isTicketObligation = 1 ORDER BY installmentNumber ASC, dueAt ASC")
    suspend fun getTicketObligationsByEvent(eventId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE recurringRuleId = :ruleId AND occurrenceIndex = :index LIMIT 1")
    suspend fun getRecurringOccurrence(ruleId: String, index: Int): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE statementId = :statementId")
    suspend fun getByStatementId(statementId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE installmentPurchaseId = :purchaseId ORDER BY installmentNumber ASC")
    suspend fun getByInstallmentPurchaseId(purchaseId: String): List<TransactionEntity>

    @Query("UPDATE transactions SET projectId = NULL, updatedAt = :updatedAt WHERE projectId = :projectId")
    suspend fun unlinkProject(projectId: String, updatedAt: Long)

    @Query("UPDATE transactions SET eventId = NULL, updatedAt = :updatedAt WHERE eventId = :eventId")
    suspend fun unlinkEvent(eventId: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE taskId = :taskId")
    suspend fun countByTask(taskId: String): Int

    @Query("UPDATE transactions SET taskId = NULL, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun unlinkTask(taskId: String, updatedAt: Long)

    @Query("DELETE FROM transactions WHERE eventId = :eventId AND isTicketObligation = 1")
    suspend fun deleteTicketObligationForEvent(eventId: String)

    @Query("DELETE FROM transactions WHERE installmentPurchaseId = :purchaseId AND status != 'SETTLED'")
    suspend fun deleteNonSettledByInstallmentPurchase(purchaseId: String)

    @Query("UPDATE transactions SET installmentPurchaseId = NULL, updatedAt = :updatedAt WHERE installmentPurchaseId = :purchaseId")
    suspend fun unlinkInstallmentPurchase(purchaseId: String, updatedAt: Long)

    @Query("DELETE FROM transactions WHERE recurringRuleId = :ruleId AND status != 'SETTLED'")
    suspend fun deleteNonSettledByRecurringRule(ruleId: String)

    @Query("UPDATE transactions SET recurringRuleId = NULL, occurrenceIndex = NULL, updatedAt = :updatedAt WHERE recurringRuleId = :ruleId")
    suspend fun unlinkRecurringRule(ruleId: String, updatedAt: Long)

    @Query(
        """
        SELECT * FROM transactions
        WHERE description LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY COALESCE(dueAt, occurredAt) DESC
        """
    )
    fun search(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY COALESCE(nextDueAt, createdAt) ASC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LoanEntity>)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM loans")
    fun observeCount(): Flow<Int>
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY yearMonth DESC")
    suspend fun getAllOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getByCategoryAndMonth(categoryId: String, yearMonth: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM budgets")
    fun observeCount(): Flow<Int>
}

@Dao
interface FinancialGoalDao {
    @Query("SELECT * FROM financial_goals ORDER BY COALESCE(deadlineAt, createdAt) ASC")
    fun observeAll(): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<FinancialGoalEntity>

    @Query("SELECT * FROM financial_goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FinancialGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: FinancialGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FinancialGoalEntity>)

    @Query("DELETE FROM financial_goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM financial_goals")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM financial_goals")
    fun observeCount(): Flow<Int>
}

@Dao
interface AuditHistoryDao {
    @Query("SELECT * FROM audit_history ORDER BY occurredAt DESC LIMIT 100")
    fun observeLatest(): Flow<List<AuditHistoryEntity>>

    @Query("SELECT * FROM audit_history WHERE entityType = :entityType ORDER BY occurredAt DESC LIMIT :limit")
    fun observeLatestByEntityType(entityType: String, limit: Int = 50): Flow<List<AuditHistoryEntity>>

    @Query("SELECT * FROM audit_history ORDER BY occurredAt DESC")
    suspend fun getAllOnce(): List<AuditHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AuditHistoryEntity>)

    @Query("DELETE FROM audit_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM audit_history")
    fun observeCount(): Flow<Int>
}

@Dao
interface ErrorLogDao {
    @Query("SELECT * FROM error_logs ORDER BY occurredAt DESC LIMIT 50")
    fun observeLatest50(): Flow<List<ErrorLogEntity>>

    @Query("SELECT * FROM error_logs ORDER BY occurredAt DESC")
    suspend fun getAllOnce(): List<ErrorLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ErrorLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ErrorLogEntity>)

    @Query("DELETE FROM error_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM error_logs")
    fun observeCount(): Flow<Int>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
    suspend fun getAllOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE status = :status ORDER BY triggerAt ASC")
    suspend fun getByStatus(status: ReminderStatus = ReminderStatus.ACTIVE): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query(
        """
        SELECT * FROM reminders
        WHERE sourceType = :sourceType AND sourceId = :sourceId AND category = :category
        LIMIT 1
        """
    )
    suspend fun getBySourceAndCategory(
        sourceType: ReminderSourceType,
        sourceId: String,
        category: ReminderCategory
    ): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun getBySource(sourceType: ReminderSourceType, sourceId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReminderEntity>)

    @Query(
        """
        UPDATE reminders
        SET status = :newStatus, updatedAt = :updatedAt
        WHERE sourceType = :sourceType AND sourceId = :sourceId AND category = :category
        """
    )
    suspend fun updateStatusForSourceCategory(
        sourceType: ReminderSourceType,
        sourceId: String,
        category: ReminderCategory,
        newStatus: ReminderStatus,
        updatedAt: Long
    )

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM reminders")
    fun observeCount(): Flow<Int>
}

@Dao
interface RoutineAssistantSettingsDao {
    @Query("SELECT * FROM routine_assistant_settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: String = RoutineAssistantSettingsEntity.SINGLETON_ID): Flow<RoutineAssistantSettingsEntity?>

    @Query("SELECT * FROM routine_assistant_settings WHERE id = :id LIMIT 1")
    suspend fun getSettings(id: String = RoutineAssistantSettingsEntity.SINGLETON_ID): RoutineAssistantSettingsEntity?

    @Query("SELECT * FROM routine_assistant_settings")
    suspend fun getAllOnce(): List<RoutineAssistantSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: RoutineAssistantSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RoutineAssistantSettingsEntity>)

    @Query("DELETE FROM routine_assistant_settings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM routine_assistant_settings")
    fun observeCount(): Flow<Int>
}

@Dao
interface BriefingHistoryDao {
    @Query("SELECT * FROM briefing_history ORDER BY dateIso DESC, updatedAt DESC LIMIT 60")
    fun observeLatest(): Flow<List<BriefingHistoryEntity>>

    @Query("SELECT * FROM briefing_history ORDER BY dateIso DESC")
    suspend fun getAllOnce(): List<BriefingHistoryEntity>

    @Query("SELECT * FROM briefing_history WHERE briefingType = :type AND dateIso = :dateIso LIMIT 1")
    suspend fun getByTypeAndDate(type: BriefingType, dateIso: String): BriefingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BriefingHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BriefingHistoryEntity>)

    @Query("DELETE FROM briefing_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM briefing_history")
    fun observeCount(): Flow<Int>
}

@Dao
interface PersonalizationSettingsDao {
    @Query("SELECT * FROM personalization_settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: String = PersonalizationSettingsEntity.SINGLETON_ID): Flow<PersonalizationSettingsEntity?>

    @Query("SELECT * FROM personalization_settings WHERE id = :id LIMIT 1")
    suspend fun getSettings(id: String = PersonalizationSettingsEntity.SINGLETON_ID): PersonalizationSettingsEntity?

    @Query("SELECT * FROM personalization_settings")
    suspend fun getAllOnce(): List<PersonalizationSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: PersonalizationSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PersonalizationSettingsEntity>)

    @Query("DELETE FROM personalization_settings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM personalization_settings")
    fun observeCount(): Flow<Int>
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<QuoteEntity>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuoteEntity>)

    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM quotes")
    fun observeCount(): Flow<Int>
}

@Dao
interface PersonalGoalDao {
    @Query("SELECT * FROM personal_goals ORDER BY CASE WHEN targetDateAt IS NULL THEN 1 ELSE 0 END, targetDateAt ASC, updatedAt DESC")
    fun observeAll(): Flow<List<PersonalGoalEntity>>

    @Query("SELECT * FROM personal_goals ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<PersonalGoalEntity>

    @Query("SELECT * FROM personal_goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PersonalGoalEntity?

    @Query(
        """
        SELECT * FROM personal_goals
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun search(query: String): Flow<List<PersonalGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: PersonalGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PersonalGoalEntity>)

    @Query("DELETE FROM personal_goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM personal_goals")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM personal_goals")
    fun observeCount(): Flow<Int>
}

@Dao
interface GoalMilestoneDao {
    @Query("SELECT * FROM goal_milestones ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<GoalMilestoneEntity>>

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getByGoalOnce(goalId: String): List<GoalMilestoneEntity>

    @Query("SELECT * FROM goal_milestones ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllOnce(): List<GoalMilestoneEntity>

    @Query("SELECT * FROM goal_milestones WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalMilestoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(milestone: GoalMilestoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GoalMilestoneEntity>)

    @Query("DELETE FROM goal_milestones WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM goal_milestones WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: String)

    @Query("DELETE FROM goal_milestones")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM goal_milestones")
    fun observeCount(): Flow<Int>
}

@Dao
interface GoalProgressLogDao {
    @Query("SELECT * FROM goal_progress_logs ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<GoalProgressLogEntity>>

    @Query("SELECT * FROM goal_progress_logs WHERE goalId = :goalId ORDER BY recordedAt DESC")
    suspend fun getByGoalOnce(goalId: String): List<GoalProgressLogEntity>

    @Query("SELECT * FROM goal_progress_logs ORDER BY recordedAt DESC")
    suspend fun getAllOnce(): List<GoalProgressLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: GoalProgressLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GoalProgressLogEntity>)

    @Query("DELETE FROM goal_progress_logs WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: String)

    @Query("DELETE FROM goal_progress_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM goal_progress_logs")
    fun observeCount(): Flow<Int>
}

@Dao
interface TaskRecurrenceDao {
    @Query("SELECT * FROM recurrences ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskRecurrenceEntity>>

    @Query("SELECT * FROM recurrences ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<TaskRecurrenceEntity>

    @Query("SELECT * FROM recurrences WHERE isActive = 1 ORDER BY createdAt ASC")
    suspend fun getActiveOnce(): List<TaskRecurrenceEntity>

    @Query("SELECT * FROM recurrences WHERE domain = :domain ORDER BY createdAt ASC")
    suspend fun getByDomainOnce(domain: RecurrenceDomain): List<TaskRecurrenceEntity>

    @Query("SELECT * FROM recurrences WHERE domain = :domain AND isActive = 1 ORDER BY createdAt ASC")
    suspend fun getActiveByDomainOnce(domain: RecurrenceDomain): List<TaskRecurrenceEntity>

    @Query("SELECT * FROM recurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskRecurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recurrence: TaskRecurrenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TaskRecurrenceEntity>)

    @Query("DELETE FROM recurrences WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurrences")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recurrences")
    fun observeCount(): Flow<Int>
}

@Dao
interface RecurrenceExceptionDao {
    @Query("SELECT * FROM recurrence_exceptions ORDER BY occurrenceDateIso ASC")
    fun observeAll(): Flow<List<RecurrenceExceptionEntity>>

    @Query("SELECT * FROM recurrence_exceptions ORDER BY occurrenceDateIso ASC")
    suspend fun getAllOnce(): List<RecurrenceExceptionEntity>

    @Query("SELECT * FROM recurrence_exceptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurrenceExceptionEntity?

    @Query("SELECT * FROM recurrence_exceptions WHERE recurrenceId = :recurrenceId ORDER BY occurrenceDateIso ASC")
    suspend fun getByRecurrenceId(recurrenceId: String): List<RecurrenceExceptionEntity>

    @Query("SELECT * FROM recurrence_exceptions WHERE recurrenceId = :recurrenceId AND occurrenceDateIso = :occurrenceDateIso LIMIT 1")
    suspend fun getByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): RecurrenceExceptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exception: RecurrenceExceptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecurrenceExceptionEntity>)

    @Query("DELETE FROM recurrence_exceptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurrence_exceptions WHERE recurrenceId = :recurrenceId")
    suspend fun deleteByRecurrenceId(recurrenceId: String)

    @Query("DELETE FROM recurrence_exceptions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recurrence_exceptions")
    fun observeCount(): Flow<Int>
}

@Dao
interface ScheduleChecklistItemDao {
    @Query("SELECT * FROM schedule_checklist_items ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<ScheduleChecklistItemEntity>>

    @Query("SELECT * FROM schedule_checklist_items WHERE appointmentId = :appointmentId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByAppointmentId(appointmentId: String): Flow<List<ScheduleChecklistItemEntity>>

    @Query("SELECT * FROM schedule_checklist_items WHERE eventId = :eventId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByEventId(eventId: String): Flow<List<ScheduleChecklistItemEntity>>

    @Query("SELECT * FROM schedule_checklist_items ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllOnce(): List<ScheduleChecklistItemEntity>

    @Query("SELECT * FROM schedule_checklist_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduleChecklistItemEntity?

    @Query("SELECT * FROM schedule_checklist_items WHERE appointmentId = :appointmentId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getByAppointmentId(appointmentId: String): List<ScheduleChecklistItemEntity>

    @Query("SELECT * FROM schedule_checklist_items WHERE eventId = :eventId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getByEventId(eventId: String): List<ScheduleChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ScheduleChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: ScheduleChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleChecklistItemEntity>)

    @Update
    suspend fun update(item: ScheduleChecklistItemEntity)

    @Query("DELETE FROM schedule_checklist_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM schedule_checklist_items WHERE appointmentId = :appointmentId")
    suspend fun deleteByAppointmentId(appointmentId: String)

    @Query("DELETE FROM schedule_checklist_items WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM schedule_checklist_items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM schedule_checklist_items")
    fun observeCount(): Flow<Int>
}




