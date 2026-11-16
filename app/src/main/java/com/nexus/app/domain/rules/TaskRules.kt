package com.nexus.app.domain.rules

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.domain.model.TaskWithDetails
import java.time.LocalDate

enum class DeadlineCondition(val label: String) {
    ON_TIME("No prazo"),
    OVERDUE("Atrasada"),
    NO_DEADLINE("Sem prazo")
}

enum class TaskScheduleGroup(val label: String) {
    OVERDUE("Atrasadas"),
    TODAY("Hoje"),
    THIS_WEEK("Esta semana"),
    UPCOMING("Próximas"),
    NO_DEADLINE("Sem prazo"),
    TERMINAL("Concluídas e canceladas")
}

data class TaskGroupSection(
    val group: TaskScheduleGroup,
    val items: List<TaskWithDetails>
)

object TaskRules {

    fun deadlineCondition(
        task: TaskEntity,
        nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): DeadlineCondition {
        if (task.dueAt == null) return DeadlineCondition.NO_DEADLINE
        return if (AgendaEngine.isTaskOverdue(task, nowEpoch, today)) {
            DeadlineCondition.OVERDUE
        } else {
            DeadlineCondition.ON_TIME
        }
    }

    fun groupTasks(
        tasks: List<TaskWithDetails>,
        nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): List<TaskGroupSection> {
        val weekEnd = today.plusDays(7)
        val buckets = linkedMapOf<TaskScheduleGroup, MutableList<TaskWithDetails>>(
            TaskScheduleGroup.OVERDUE to mutableListOf(),
            TaskScheduleGroup.TODAY to mutableListOf(),
            TaskScheduleGroup.THIS_WEEK to mutableListOf(),
            TaskScheduleGroup.UPCOMING to mutableListOf(),
            TaskScheduleGroup.NO_DEADLINE to mutableListOf(),
            TaskScheduleGroup.TERMINAL to mutableListOf()
        )

        tasks.forEach { item ->
            val t = item.task
            val bucket = when {
                t.status.isTerminal -> TaskScheduleGroup.TERMINAL
                t.dueAt == null -> TaskScheduleGroup.NO_DEADLINE
                AgendaEngine.isTaskOverdue(t, nowEpoch, today) -> TaskScheduleGroup.OVERDUE
                else -> {
                    val dueDate = DateTimeUtils.toLocalDate(t.dueAt)
                    when {
                        dueDate == today -> TaskScheduleGroup.TODAY
                        !dueDate.isAfter(weekEnd) -> TaskScheduleGroup.THIS_WEEK
                        else -> TaskScheduleGroup.UPCOMING
                    }
                }
            }
            buckets.getValue(bucket).add(item)
        }

        val activeComparator = compareBy<TaskWithDetails>(
            { it.task.dueAt ?: Long.MAX_VALUE },
            { -it.task.priority.sortRank },
            { it.task.title.lowercase() }
        )
        val terminalComparator = compareByDescending<TaskWithDetails> {
            it.task.completedAt ?: it.task.updatedAt
        }

        return buckets.mapNotNull { (group, list) ->
            if (list.isEmpty()) return@mapNotNull null
            val sorted = if (group == TaskScheduleGroup.TERMINAL) {
                list.sortedWith(terminalComparator)
            } else {
                list.sortedWith(activeComparator)
            }
            TaskGroupSection(group = group, items = sorted)
        }
    }
}
