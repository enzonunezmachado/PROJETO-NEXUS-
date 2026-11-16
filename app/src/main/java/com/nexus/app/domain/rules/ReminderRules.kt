package com.nexus.app.domain.rules

import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.domain.model.ReminderStatus

enum class ReminderSyncAction {
    PRESERVE_UNTOUCHED,
    RESCHEDULE,
    DISABLE
}

object ReminderRules {

    /**
     * Considera cancelado qualquer lembrete com [ReminderStatus.CANCELLED] ou registros
     * legados que tenham gravado `minutesBefore < 0`.
     */
    fun isEffectivelyCancelled(reminder: ReminderEntity): Boolean =
        reminder.status == ReminderStatus.CANCELLED || reminder.minutesBefore < 0

    /**
     * Lembretes cancelados (ou com `minutesBefore < 0`) nunca devem ser exibidos na fila.
     */
    fun isQueueVisible(reminder: ReminderEntity): Boolean =
        !isEffectivelyCancelled(reminder)

    /**
     * Lembretes cancelados, com `minutesBefore < 0` ou dispensados nunca devem ser agendados/disparados.
     */
    fun isSchedulableOrFirable(reminder: ReminderEntity): Boolean =
        !isEffectivelyCancelled(reminder) && reminder.status != ReminderStatus.DISMISSED

    /**
     * Ao desativar um lembrete, preserva o `minutesBefore` original (sem gravar `-1`).
     */
    fun resolveDisabledMinutesBefore(
        existing: ReminderEntity?,
        fallbackDefaultMinutes: Int
    ): Int = existing?.minutesBefore?.takeIf { it >= 0 } ?: fallbackDefaultMinutes.coerceAtLeast(0)

    /**
     * Resolve o valor para o seletor do editor de Tarefa, Compromisso, Evento ou Transação:
     * - `null`: sem registro prévio (exibe "Padrão")
     * - `-1`: sentinela de UI para "Sem lembrete" quando o lembrete está cancelado
     * - `>= 0`: antecedência real salva no banco (ex.: 0, 15, 30, 60, 120, etc.)
     */
    fun resolveEditorReminderMinutes(existing: ReminderEntity?): Int? {
        if (existing == null) return null
        return if (isEffectivelyCancelled(existing)) {
            -1
        } else {
            existing.minutesBefore.coerceAtLeast(0)
        }
    }

    /**
     * Decide como sincronizar o lembrete ao salvar o registro:
     * - Se `overrideMinutesBefore < 0` -> desativa explicitamente (`DISABLE`), mantendo `minutesBefore` original e `status = CANCELLED`.
     * - Se `overrideMinutesBefore == null` (usuário não mexeu no seletor):
     *   - Se já existe um lembrete cancelado (ou legado com `minutesBefore < 0`) -> `PRESERVE_UNTOUCHED`
     *     (não reativa automaticamente ao salvar sem mexer no lembrete).
     *   - Se já existe um lembrete e o prazo não mudou -> `PRESERVE_UNTOUCHED`
     *     (não apaga, não cancela soneca/disparo, não altera `minutesBefore`).
     *   - Caso contrário -> `RESCHEDULE` usando o `minutesBefore` existente (ou padrão se não houver).
     * - Se `overrideMinutesBefore >= 0` -> `RESCHEDULE` com a nova antecedência informada.
     */
    fun resolveReminderSyncAction(
        existing: ReminderEntity?,
        currentDueAt: Long?,
        overrideMinutesBefore: Int?
    ): ReminderSyncAction {
        if (overrideMinutesBefore != null && overrideMinutesBefore < 0) {
            return ReminderSyncAction.DISABLE
        }
        if (overrideMinutesBefore == null && existing != null) {
            if (isEffectivelyCancelled(existing)) {
                return ReminderSyncAction.PRESERVE_UNTOUCHED
            }
            if (existing.originalDueAt == currentDueAt) {
                return ReminderSyncAction.PRESERVE_UNTOUCHED
            }
        }
        return ReminderSyncAction.RESCHEDULE
    }
}
