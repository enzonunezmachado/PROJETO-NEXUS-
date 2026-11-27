package com.nexus.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.dao.AppointmentDao
import com.nexus.app.data.db.dao.AuditHistoryDao
import com.nexus.app.data.db.dao.BriefingHistoryDao
import com.nexus.app.data.db.dao.BudgetDao
import com.nexus.app.data.db.dao.CategoryDao
import com.nexus.app.data.db.dao.CreditCardDao
import com.nexus.app.data.db.dao.CreditCardStatementDao
import com.nexus.app.data.db.dao.ErrorLogDao
import com.nexus.app.data.db.dao.EventDao
import com.nexus.app.data.db.dao.FinancialGoalDao
import com.nexus.app.data.db.dao.GoalMilestoneDao
import com.nexus.app.data.db.dao.GoalProgressLogDao
import com.nexus.app.data.db.dao.InstallmentPurchaseDao
import com.nexus.app.data.db.dao.LoanDao
import com.nexus.app.data.db.dao.PersonalGoalDao
import com.nexus.app.data.db.dao.PersonalizationSettingsDao
import com.nexus.app.data.db.dao.ProjectDao
import com.nexus.app.data.db.dao.QuoteDao
import com.nexus.app.data.db.dao.RecurrenceExceptionDao
import com.nexus.app.data.db.dao.RecurringRuleDao
import com.nexus.app.data.db.dao.ReminderDao
import com.nexus.app.data.db.dao.RoutineAssistantSettingsDao
import com.nexus.app.data.db.dao.ScheduleChecklistItemDao
import com.nexus.app.data.db.dao.SettingsDao
import com.nexus.app.data.db.dao.SubtaskDao
import com.nexus.app.data.db.dao.TaskDao
import com.nexus.app.data.db.dao.TaskRecurrenceDao
import com.nexus.app.data.db.dao.TransactionDao
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
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.CategoryScope

@Database(
    entities = [
        SettingsEntity::class,
        CategoryEntity::class,
        ProjectEntity::class,
        EventEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        AppointmentEntity::class,
        CreditCardEntity::class,
        CreditCardStatementEntity::class,
        InstallmentPurchaseEntity::class,
        RecurringRuleEntity::class,
        TransactionEntity::class,
        LoanEntity::class,
        BudgetEntity::class,
        FinancialGoalEntity::class,
        AuditHistoryEntity::class,
        ErrorLogEntity::class,
        ReminderEntity::class,
        RoutineAssistantSettingsEntity::class,
        BriefingHistoryEntity::class,
        PersonalizationSettingsEntity::class,
        QuoteEntity::class,
        PersonalGoalEntity::class,
        GoalMilestoneEntity::class,
        GoalProgressLogEntity::class,
        TaskRecurrenceEntity::class,
        RecurrenceExceptionEntity::class,
        ScheduleChecklistItemEntity::class
    ],
    version = NexusDatabase.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(NexusTypeConverters::class)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun projectDao(): ProjectDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardStatementDao(): CreditCardStatementDao
    abstract fun installmentPurchaseDao(): InstallmentPurchaseDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun budgetDao(): BudgetDao
    abstract fun financialGoalDao(): FinancialGoalDao
    abstract fun auditHistoryDao(): AuditHistoryDao
    abstract fun errorLogDao(): ErrorLogDao
    abstract fun reminderDao(): ReminderDao
    abstract fun routineAssistantSettingsDao(): RoutineAssistantSettingsDao
    abstract fun briefingHistoryDao(): BriefingHistoryDao
    abstract fun personalizationSettingsDao(): PersonalizationSettingsDao
    abstract fun quoteDao(): QuoteDao
    abstract fun personalGoalDao(): PersonalGoalDao
    abstract fun goalMilestoneDao(): GoalMilestoneDao
    abstract fun goalProgressLogDao(): GoalProgressLogDao
    abstract fun taskRecurrenceDao(): TaskRecurrenceDao
    abstract fun recurrenceExceptionDao(): RecurrenceExceptionDao
    abstract fun scheduleChecklistItemDao(): ScheduleChecklistItemDao

    companion object {
        const val DATABASE_NAME = "nexus_local.db"
        const val DATABASE_VERSION = 8

        fun build(context: Context): NexusDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NexusDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*NexusMigrations.ALL_MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedInitialDefaults(db)
                    }
                })
                .build()
        }

        /**
         * Insere apenas configuracoes iniciais (usuario "Enzo", tema escuro) e categorias
         * base editaveis. Nunca insere tarefas, eventos, transacoes ou compromissos ficticios.
         */
        internal fun seedInitialDefaults(db: SupportSQLiteDatabase) {
            val now = DateTimeUtils.nowEpochMillis()
            db.execSQL(
                """
                INSERT OR IGNORE INTO settings (id, userName, darkTheme, declaredBalanceCents, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(SettingsEntity.SINGLETON_ID, "Enzo", 1, 0L, now, now)
            )

            val defaultCategories = listOf(
                Triple("cat-default-pessoal", "Pessoal", "#4C8BF5"),
                Triple("cat-default-trabalho", "Trabalho", "#9A9A9A"),
                Triple("cat-default-saude", "Saúde", "#34C759"),
                Triple("cat-default-estudos", "Estudos", "#E5A000")
            )
            defaultCategories.forEach { (id, name, color) ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (id, name, colorHex, scope, isDefault, createdAt, updatedAt)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(id, name, color, CategoryScope.GENERAL.name, 1, now, now)
                )
            }

            NexusMigrations.seedDefaultFinancialCategories(db)
            NexusMigrations.seedDefaultRoutineSettings(db)
            NexusMigrations.seedDefaultPersonalizationSettings(db)
        }
    }
}
