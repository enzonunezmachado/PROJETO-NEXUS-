package com.nexus.app.data.db

import androidx.room.TypeConverter
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.EventCostType
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectStatus
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

class NexusTypeConverters {
    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toTaskPriority(raw: String?): TaskPriority = TaskPriority.fromStringOrDefault(raw)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(raw: String?): TaskStatus = TaskStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(raw: String?): ProjectStatus = ProjectStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromCategoryScope(value: CategoryScope): String = value.name

    @TypeConverter
    fun toCategoryScope(raw: String?): CategoryScope = CategoryScope.fromStringOrDefault(raw)

    @TypeConverter
    fun fromEventLifecycleStatus(value: EventLifecycleStatus): String = value.name

    @TypeConverter
    fun toEventLifecycleStatus(raw: String?): EventLifecycleStatus =
        EventLifecycleStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromTicketStatus(value: TicketStatus): String = value.name

    @TypeConverter
    fun toTicketStatus(raw: String?): TicketStatus = TicketStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(raw: String?): PaymentMethod = PaymentMethod.fromStringOrDefault(raw)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(raw: String?): TransactionType = TransactionType.fromStringOrDefault(raw)

    @TypeConverter
    fun fromFinancialStatus(value: FinancialStatus): String = value.name

    @TypeConverter
    fun toFinancialStatus(raw: String?): FinancialStatus = FinancialStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromEventCostType(value: EventCostType?): String? = value?.name

    @TypeConverter
    fun toEventCostType(raw: String?): EventCostType? =
        raw?.let { EventCostType.fromStringOrDefault(it) }

    @TypeConverter
    fun fromStatementStatus(value: StatementStatus): String = value.name

    @TypeConverter
    fun toStatementStatus(raw: String?): StatementStatus = StatementStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toRecurrenceFrequency(raw: String?): RecurrenceFrequency =
        RecurrenceFrequency.fromStringOrDefault(raw)

    @TypeConverter
    fun fromReminderSourceType(value: ReminderSourceType): String = value.name

    @TypeConverter
    fun toReminderSourceType(raw: String?): ReminderSourceType =
        ReminderSourceType.fromStringOrDefault(raw)

    @TypeConverter
    fun fromReminderCategory(value: ReminderCategory): String = value.name

    @TypeConverter
    fun toReminderCategory(raw: String?): ReminderCategory =
        ReminderCategory.fromStringOrDefault(raw)

    @TypeConverter
    fun fromReminderStatus(value: ReminderStatus): String = value.name

    @TypeConverter
    fun toReminderStatus(raw: String?): ReminderStatus =
        ReminderStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromBriefingType(value: BriefingType): String = value.name

    @TypeConverter
    fun toBriefingType(raw: String?): BriefingType =
        BriefingType.fromStringOrDefault(raw)

    @TypeConverter
    fun fromNexusDateFormat(value: NexusDateFormat): String = value.name

    @TypeConverter
    fun toNexusDateFormat(raw: String?): NexusDateFormat =
        NexusDateFormat.fromStringOrDefault(raw)

    @TypeConverter
    fun fromGoalStatus(value: GoalStatus): String = value.name

    @TypeConverter
    fun toGoalStatus(raw: String?): GoalStatus =
        GoalStatus.fromStringOrDefault(raw)

    @TypeConverter
    fun fromGoalProgressMode(value: GoalProgressMode): String = value.name

    @TypeConverter
    fun toGoalProgressMode(raw: String?): GoalProgressMode =
        GoalProgressMode.fromStringOrDefault(raw)

    @TypeConverter
    fun fromTaskRecurrenceFrequency(value: TaskRecurrenceFrequency): String = value.name

    @TypeConverter
    fun toTaskRecurrenceFrequency(raw: String?): TaskRecurrenceFrequency =
        TaskRecurrenceFrequency.fromStringOrDefault(raw)

    @TypeConverter
    fun fromRecurrenceExceptionType(value: RecurrenceExceptionType): String = value.name

    @TypeConverter
    fun toRecurrenceExceptionType(raw: String?): RecurrenceExceptionType =
        RecurrenceExceptionType.fromStringOrDefault(raw)
}
