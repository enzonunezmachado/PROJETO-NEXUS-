package com.nexus.app.domain.model

enum class TaskPriority(val label: String, val sortRank: Int) {
    LOW("Baixa", 1),
    NORMAL("Normal", 2),
    HIGH("Alta", 3),
    URGENT("Urgente", 4);

    companion object {
        fun fromStringOrDefault(raw: String?): TaskPriority =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NORMAL
    }
}

enum class TaskStatus(val label: String, val isTerminal: Boolean) {
    NOT_STARTED("Não iniciada", false),
    IN_PROGRESS("Em andamento", false),
    WAITING("Aguardando", false),
    BLOCKED("Bloqueada", false),
    COMPLETED("Concluída", true),
    CANCELLED("Cancelada", true);

    companion object {
        fun fromStringOrDefault(raw: String?): TaskStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NOT_STARTED
    }
}

enum class ProjectStatus(val label: String) {
    PLANNED("Planejado"),
    ACTIVE("Em andamento"),
    ON_HOLD("Pausado"),
    COMPLETED("Concluído"),
    ARCHIVED("Arquivado");

    companion object {
        fun fromStringOrDefault(raw: String?): ProjectStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

enum class CategoryScope(val label: String) {
    GENERAL("Geral"),
    APPOINTMENT("Agenda"),
    TASK("Tarefa"),
    FINANCE("Financeiro"),
    EVENT("Evento");

    companion object {
        fun fromStringOrDefault(raw: String?): CategoryScope =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GENERAL
    }
}

enum class EventLifecycleStatus(val label: String) {
    PLANNED("Planejado"),
    CONFIRMED("Confirmado"),
    ATTENDED("Realizado"),
    CANCELLED("Cancelado");

    companion object {
        fun fromStringOrDefault(raw: String?): EventLifecycleStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PLANNED
    }
}

enum class TicketStatus(val label: String) {
    FREE("Gratuito"),
    NOT_BOUGHT("Não comprado"),
    PENDING("Pendente"),
    PARTIALLY_PAID("Parcialmente pago"),
    BOUGHT("Comprado"),
    PAID("Pago"),
    CANCELLED("Cancelado");

    val isPaidOrBought: Boolean get() = this == BOUGHT || this == PAID
    val requiresFinancialObligation: Boolean get() = this != FREE && this != CANCELLED

    companion object {
        fun fromStringOrDefault(raw: String?): TicketStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FREE
    }
}

enum class PaymentMethod(val label: String) {
    PIX("Pix"),
    DEBIT("Débito"),
    CREDIT_CARD("Cartão de crédito"),
    CASH("Dinheiro"),
    BANK_TRANSFER("Transferência"),
    BOLETO("Boleto"),
    OTHER("Outro");

    companion object {
        fun fromStringOrDefault(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PIX
    }
}

enum class TransactionType(val label: String) {
    INCOME("Receita"),
    EXPENSE("Despesa");

    companion object {
        fun fromStringOrDefault(raw: String?): TransactionType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EXPENSE
    }
}

enum class FinancialStatus(val label: String) {
    PLANNED("Previsto"),
    PENDING("Pendente"),
    SETTLED("Realizado"),
    OVERDUE("Atrasado"),
    CANCELLED("Cancelado");

    companion object {
        fun fromStringOrDefault(raw: String?): FinancialStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

enum class EventCostType(val label: String) {
    TICKET("Ingresso"),
    TRANSPORT("Transporte"),
    FOOD("Alimentação"),
    LODGING("Hospedagem"),
    PARKING("Estacionamento"),
    SHOPPING("Compras"),
    OTHER("Outros");

    companion object {
        fun fromStringOrDefault(raw: String?): EventCostType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

enum class StatementStatus(val label: String) {
    OPEN("Aberta"),
    CLOSED("Fechada"),
    PENDING("Pendente"),
    PAID("Paga"),
    OVERDUE("Atrasada");

    companion object {
        fun fromStringOrDefault(raw: String?): StatementStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OPEN
    }
}

enum class RecurrenceFrequency(val label: String) {
    DAILY("Diária"),
    WEEKLY("Semanal"),
    BIWEEKLY("Quinzenal"),
    MONTHLY("Mensal"),
    BIMONTHLY("Bimestral"),
    QUARTERLY("Trimestral"),
    SEMIANNUAL("Semestral"),
    ANNUAL("Anual"),
    CUSTOM_DAYS("Personalizada (dias)");

    companion object {
        fun fromStringOrDefault(raw: String?): RecurrenceFrequency =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MONTHLY
    }
}

enum class ReminderSourceType(val label: String) {
    TASK("Tarefa"),
    APPOINTMENT("Compromisso"),
    EVENT("Evento"),
    TRANSACTION("Lançamento"),
    STATEMENT("Fatura"),
    ROUTINE_MORNING("Briefing Diário"),
    ROUTINE_NIGHT("Revisão Noturna");

    companion object {
        fun fromStringOrDefault(raw: String?): ReminderSourceType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TASK
    }
}

enum class ReminderCategory(val label: String) {
    SCHEDULE("Agenda"),
    FINANCIAL("Financeiro"),
    ROUTINE("Rotina");

    companion object {
        fun fromStringOrDefault(raw: String?): ReminderCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SCHEDULE
    }
}

enum class ReminderStatus(val label: String) {
    ACTIVE("Agendado"),
    FIRED("Disparado"),
    DISMISSED("Dispensado"),
    CANCELLED("Cancelado");

    companion object {
        fun fromStringOrDefault(raw: String?): ReminderStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

enum class BriefingType(val label: String) {
    MORNING("Briefing Diário"),
    NIGHT_REVIEW("Revisão Noturna");

    companion object {
        fun fromStringOrDefault(raw: String?): BriefingType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MORNING
    }
}

enum class NexusDateFormat(val label: String, val example: String) {
    WEEKDAY_DD_MM_YYYY("Dia da semana, DD/MM/AAAA", "Sexta-feira, 15/05/2026"),
    DD_MM_YYYY("DD/MM/AAAA", "15/05/2026"),
    DD_MMM_YYYY("DD de mês de AAAA", "15 de maio de 2026"),
    DD_MM_WEEKDAY("DD/MM · Dia da semana", "15/05 · Sexta-feira");

    companion object {
        fun fromStringOrDefault(raw: String?): NexusDateFormat =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: WEEKDAY_DD_MM_YYYY
    }
}

enum class DashboardBlockType(val defaultTitle: String, val description: String) {
    GREETING("Saudação", "Boas-vindas personalizadas e data formatada"),
    DAILY_QUOTE("Frase diária", "Frase da sua biblioteca local offline"),
    TASK_SUMMARY("Resumo de tarefas", "Contadores e tarefas prioritárias/atrasadas"),
    TODAY_AGENDA("Agenda do dia", "Próximo compromisso e compromissos de hoje"),
    FINANCE_SUMMARY("Resumo financeiro", "Saldo declarado, receitas e despesas do mês"),
    UPCOMING_EVENTS("Próximos eventos", "Eventos confirmados ou planejados"),
    UPCOMING_PAYMENTS("Próximos pagamentos", "Contas a pagar e faturas abertas"),
    GOALS("Metas", "Objetivos pessoais e metas financeiras em andamento"),
    CHARTS_INDICATORS("Gráficos e indicadores", "Fluxo mensal e gastos por categoria");

    companion object {
        fun fromStringOrDefault(raw: String?): DashboardBlockType? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

enum class GoalStatus(val label: String) {
    ACTIVE("Em andamento"),
    PAUSED("Pausado"),
    COMPLETED("Concluído"),
    CANCELLED("Cancelado");

    companion object {
        fun fromStringOrDefault(raw: String?): GoalStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

enum class GoalProgressMode(val label: String) {
    AUTO("Automático (marcos, tarefas e projetos)"),
    MANUAL("Manual (%)");

    companion object {
        fun fromStringOrDefault(raw: String?): GoalProgressMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AUTO
    }
}

enum class QuoteLibraryStatus {
    EMPTY,
    SINGLE_QUOTE,
    ROTATING
}

enum class TaskRecurrenceFrequency(val label: String) {
    DAILY("Diariamente"),
    SPECIFIC_WEEKDAYS("Dias específicos da semana"),
    WEEKLY("Semanalmente"),
    BIWEEKLY("Quinzenalmente"),
    MONTHLY("Mensalmente"),
    ANNUAL("Anualmente"),
    CUSTOM_DAYS("Intervalo personalizado");

    companion object {
        fun fromStringOrDefault(raw: String?): TaskRecurrenceFrequency =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: WEEKLY
    }
}

enum class RecurrenceDomain(val label: String) {
    TASK("Tarefa"),
    APPOINTMENT("Compromisso"),
    EVENT("Evento");

    companion object {
        fun fromStringOrDefault(raw: String?): RecurrenceDomain =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TASK
    }
}

enum class RecurrenceExceptionType(val label: String) {
    SKIP("Ignorada"),
    OVERRIDE("Alterada");

    companion object {
        fun fromStringOrDefault(raw: String?): RecurrenceExceptionType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SKIP
    }
}


