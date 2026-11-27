package com.nexus.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.EventCostType
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val userName: String = "Enzo",
    val darkTheme: Boolean = true,
    val declaredBalanceCents: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val SINGLETON_ID = "nexus_settings_default"
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val scope: CategoryScope = CategoryScope.GENERAL,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = PersonalGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["goalId"])
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val deadlineAt: Long? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val goalId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskRecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["creditCardId"]),
        Index(value = ["startAt"]),
        Index(value = ["recurrenceId"]),
        Index(value = ["recurrenceId", "occurrenceDateIso"], unique = true)
    ]
)
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val categoryId: String? = null,
    val startAt: Long,
    val endAt: Long,
    val isAllDay: Boolean = false,
    val officialLink: String = "",
    val ticketLink: String = "",
    val locationName: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val notes: String = "",
    val eventStatus: EventLifecycleStatus = EventLifecycleStatus.PLANNED,
    val ticketStatus: TicketStatus = TicketStatus.FREE,
    val ticketPriceCents: Long = 0L,
    val ticketQuantity: Int = 1,
    val ticketFeesCents: Long = 0L,
    val ticketTotalCents: Long = 0L,
    val ticketPurchaseDate: Long? = null,
    val ticketDueDate: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val creditCardId: String? = null,
    val installmentsCount: Int = 1,
    val recurrenceId: String? = null,
    val occurrenceDateIso: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PersonalGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskRecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["categoryId"]),
        Index(value = ["eventId"]),
        Index(value = ["goalId"]),
        Index(value = ["status"]),
        Index(value = ["dueAt"]),
        Index(value = ["recurrenceId"]),
        Index(value = ["recurrenceId", "occurrenceDateIso"], unique = true)
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val projectId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val goalId: String? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val dueAt: Long? = null,
    val hasDueTime: Boolean = false,
    val notes: String = "",
    val completedAt: Long? = null,
    val recurrenceId: String? = null,
    val occurrenceDateIso: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"])
    ]
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskRecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["eventId"]),
        Index(value = ["startAt"]),
        Index(value = ["endAt"]),
        Index(value = ["recurrenceId"]),
        Index(value = ["recurrenceId", "occurrenceDateIso"], unique = true)
    ]
)
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String? = null,
    val eventId: String? = null,
    val startAt: Long,
    val endAt: Long,
    val isAllDay: Boolean = false,
    val location: String = "",
    val link: String = "",
    val recurrenceId: String? = null,
    val occurrenceDateIso: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String = "",
    val totalLimitCents: Long = 0L,
    val availableLimitCents: Long = 0L,
    val closingDay: Int = 10,
    val dueDay: Int = 17,
    val colorHex: String = "#9A9A9A",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "credit_card_statements",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["creditCardId"]),
        Index(value = ["creditCardId", "yearMonth"], unique = true),
        Index(value = ["dueAt"])
    ]
)
data class CreditCardStatementEntity(
    @PrimaryKey val id: String,
    val creditCardId: String,
    val yearMonth: String,
    val closingAt: Long,
    val dueAt: Long,
    val status: StatementStatus = StatementStatus.OPEN,
    val paidAt: Long? = null,
    val paidAmountCents: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "installment_purchases",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["creditCardId"]),
        Index(value = ["categoryId"]),
        Index(value = ["eventId"]),
        Index(value = ["projectId"])
    ]
)
data class InstallmentPurchaseEntity(
    @PrimaryKey val id: String,
    val description: String,
    val totalAmountCents: Long,
    val installmentsCount: Int,
    val installmentAmountCents: Long,
    val firstDueAt: Long,
    val creditCardId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val projectId: String? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["creditCardId"])
    ]
)
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountCents: Long,
    val categoryId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val creditCardId: String? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val customIntervalDays: Int = 30,
    val startAt: Long,
    val endAt: Long? = null,
    val maxOccurrences: Int? = null,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CreditCardStatementEntity::class,
            parentColumns = ["id"],
            childColumns = ["statementId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = InstallmentPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentPurchaseId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RecurringRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringRuleId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["projectId"]),
        Index(value = ["eventId"]),
        Index(value = ["taskId"]),
        Index(value = ["creditCardId"]),
        Index(value = ["statementId"]),
        Index(value = ["installmentPurchaseId"]),
        Index(value = ["recurringRuleId"]),
        Index(value = ["recurringRuleId", "occurrenceIndex"], unique = true),
        Index(value = ["dueAt"]),
        Index(value = ["occurredAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val description: String,
    val amountCents: Long,
    val categoryId: String? = null,
    val status: FinancialStatus = FinancialStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val occurredAt: Long,
    val dueAt: Long? = null,
    val settledAt: Long? = null,
    val projectId: String? = null,
    val eventId: String? = null,
    val taskId: String? = null,
    val eventCostType: EventCostType? = null,
    val isTicketObligation: Boolean = false,
    val creditCardId: String? = null,
    val statementId: String? = null,
    val installmentPurchaseId: String? = null,
    val installmentNumber: Int? = null,
    val installmentsTotal: Int? = null,
    val recurringRuleId: String? = null,
    val occurrenceIndex: Int? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String,
    val creditor: String,
    val originalAmountCents: Long,
    val contractedAmountCents: Long,
    val installmentsTotal: Int,
    val installmentAmountCents: Long,
    val paidInstallments: Int = 0,
    val nextDueAt: Long? = null,
    val remainingBalanceCents: Long,
    val finalDateAt: Long? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["categoryId", "yearMonth"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val yearMonth: String,
    val limitCents: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "financial_goals",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class FinancialGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long = 0L,
    val deadlineAt: Long? = null,
    val categoryId: String? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "audit_history",
    indices = [Index(value = ["occurredAt"])]
)
data class AuditHistoryEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val summary: String,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "error_logs",
    indices = [Index(value = ["occurredAt"])]
)
data class ErrorLogEntity(
    @PrimaryKey val id: String,
    val source: String,
    val message: String,
    val stackTrace: String,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["sourceType", "sourceId", "category"], unique = true),
        Index(value = ["status"]),
        Index(value = ["triggerAt"])
    ]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val sourceType: ReminderSourceType,
    val sourceId: String,
    val category: ReminderCategory,
    val title: String,
    val message: String,
    val triggerAt: Long,
    val originalDueAt: Long? = null,
    val minutesBefore: Int = 0,
    val channelId: String,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val snoozedUntil: Long? = null,
    val isExactScheduled: Boolean = false,
    val firedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "routine_assistant_settings")
data class RoutineAssistantSettingsEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val notificationsEnabled: Boolean = true,
    val scheduleChannelEnabled: Boolean = true,
    val financeChannelEnabled: Boolean = true,
    val eventsChannelEnabled: Boolean = true,
    val routineChannelEnabled: Boolean = true,
    val morningBriefingEnabled: Boolean = true,
    val morningBriefingHour: Int = 8,
    val morningBriefingMinute: Int = 0,
    val nightReviewEnabled: Boolean = true,
    val nightReviewHour: Int = 21,
    val nightReviewMinute: Int = 0,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    val defaultTaskReminderMinutes: Int = 30,
    val defaultAppointmentReminderMinutes: Int = 30,
    val defaultEventReminderMinutes: Int = 60,
    val defaultFinanceReminderMinutes: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val SINGLETON_ID = "nexus_routine_default"
    }
}

@Entity(
    tableName = "briefing_history",
    indices = [
        Index(value = ["briefingType", "dateIso"], unique = true),
        Index(value = ["dateIso"])
    ]
)
data class BriefingHistoryEntity(
    @PrimaryKey val id: String,
    val briefingType: BriefingType,
    val dateIso: String,
    val summaryText: String,
    val tasksCount: Int = 0,
    val appointmentsCount: Int = 0,
    val billsDueCount: Int = 0,
    val overdueCount: Int = 0,
    val reviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "personalization_settings")
data class PersonalizationSettingsEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
    val greetingEnabled: Boolean = true,
    val greetingTemplate: String = "AUTO",
    val dateFormatPattern: NexusDateFormat = NexusDateFormat.WEEKDAY_DD_MM_YYYY,
    val showDailyQuoteOnDashboard: Boolean = true,
    val compactDashboardMode: Boolean = false,
    val showMetricSummaryOnDashboard: Boolean = true,
    val dashboardBlocksJson: String = "",
    val lastQuoteDateIso: String? = null,
    val lastQuoteId: String? = null,
    val previousQuoteId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val SINGLETON_ID = "nexus_personalization_default"
    }
}

@Entity(
    tableName = "quotes",
    indices = [Index(value = ["isActive"])]
)
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String = "",
    val isActive: Boolean = true,
    val lastShownDateIso: String? = null,
    val shownCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "personal_goals",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["status"]),
        Index(value = ["targetDateAt"])
    ]
)
data class PersonalGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String? = null,
    val targetDateAt: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val progressMode: GoalProgressMode = GoalProgressMode.AUTO,
    val manualProgressPercent: Int = 0,
    val completedAt: Long? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "goal_milestones",
    foreignKeys = [
        ForeignKey(
            entity = PersonalGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class GoalMilestoneEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val title: String,
    val description: String = "",
    val dueAt: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "goal_progress_logs",
    foreignKeys = [
        ForeignKey(
            entity = PersonalGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["recordedAt"])
    ]
)
data class GoalProgressLogEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val progressPercent: Int,
    val note: String = "",
    val recordedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "recurrences",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PersonalGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["categoryId"]),
        Index(value = ["eventId"]),
        Index(value = ["goalId"]),
        Index(value = ["isActive"]),
        Index(value = ["domain"])
    ]
)
data class TaskRecurrenceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val projectId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val goalId: String? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val notes: String = "",
    val hasDueTime: Boolean = false,
    val dueTimeMinutesOfDay: Int? = null,
    val reminderMinutesBefore: Int? = null,
    val frequency: TaskRecurrenceFrequency = TaskRecurrenceFrequency.WEEKLY,
    val customIntervalDays: Int = 1,
    val weekdaysMask: Int = 0,
    val startDateIso: String,
    val endDateIso: String? = null,
    val maxOccurrences: Int? = null,
    val isActive: Boolean = true,
    val domain: RecurrenceDomain = RecurrenceDomain.TASK,
    val isAllDay: Boolean = false,
    val durationMillis: Long = 3_600_000L,
    val location: String = "",
    val link: String = "",
    val officialLink: String = "",
    val ticketLink: String = "",
    val locationName: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val eventStatus: EventLifecycleStatus = EventLifecycleStatus.PLANNED,
    val ticketStatus: TicketStatus = TicketStatus.FREE,
    val ticketPriceCents: Long = 0L,
    val ticketQuantity: Int = 1,
    val ticketFeesCents: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val creditCardId: String? = null,
    val installmentsCount: Int = 1,
    val templateSubtasksJson: String = "[]",
    val checklistTemplateVersion: Int = 1,
    val templateHistoryJson: String = "[]",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "recurrence_exceptions",
    foreignKeys = [
        ForeignKey(
            entity = TaskRecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recurrenceId"]),
        Index(value = ["recurrenceId", "occurrenceDateIso"], unique = true)
    ]
)
data class RecurrenceExceptionEntity(
    @PrimaryKey val id: String,
    val recurrenceId: String,
    val occurrenceDateIso: String,
    val exceptionType: RecurrenceExceptionType = RecurrenceExceptionType.SKIP,
    val overrideTitle: String? = null,
    val overrideDueAt: Long? = null,
    val overrideEndAt: Long? = null,
    val overrideDescription: String? = null,
    val overrideLocation: String? = null,
    val overrideLink: String? = null,
    val overrideIsAllDay: Boolean? = null,
    val taskId: String? = null,
    val appointmentId: String? = null,
    val eventId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "schedule_checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = AppointmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["appointmentId"]),
        Index(value = ["appointmentId", "sortOrder"]),
        Index(value = ["eventId"]),
        Index(value = ["eventId", "sortOrder"])
    ]
)
data class ScheduleChecklistItemEntity(
    @PrimaryKey val id: String,
    val appointmentId: String? = null,
    val eventId: String? = null,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val templateVersion: Int = 1,
    val createdAt: Long,
    val updatedAt: Long
)



