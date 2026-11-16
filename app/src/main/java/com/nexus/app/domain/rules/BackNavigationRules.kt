package com.nexus.app.domain.rules

enum class BackSubRouteKind {
    NONE,
    TOP_LEVEL_SUBROUTE,
    MORE_CHILD_SUBROUTE
}

enum class BackTabKind {
    HOME,
    OTHER
}

data class BackNavigationState(
    val hasOpenEditor: Boolean,
    val editorHasUnsavedChanges: Boolean,
    val subRouteKind: BackSubRouteKind,
    val currentTab: BackTabKind
)

enum class BackAction {
    CLOSE_EDITOR,
    CONFIRM_DISCARD_EDITOR,
    RETURN_TO_MORE_MENU,
    RETURN_FROM_SUBROUTE,
    RETURN_TO_HOME_TAB,
    ALLOW_SYSTEM_EXIT
}

/**
 * Regra pura de resolução do botão Voltar do sistema.
 * Prioridade fixa:
 * 1. Editor/modal aberto -> confirma se houver alterações não salvas, senão fecha.
 * 2. Subtela profunda (Menus filhos de Mais) -> volta ao menu Mais.
 * 3. Subtela de topo (Mais, Busca, Assistente de Rotina, Personalizar Início) -> volta à aba atual.
 * 4. Aba diferente de Início -> volta para Início.
 * 5. Início sem nada aberto -> deixa o sistema sair do app.
 */
fun resolveBackAction(state: BackNavigationState): BackAction {
    return when {
        state.hasOpenEditor -> {
            if (state.editorHasUnsavedChanges) {
                BackAction.CONFIRM_DISCARD_EDITOR
            } else {
                BackAction.CLOSE_EDITOR
            }
        }
        state.subRouteKind == BackSubRouteKind.MORE_CHILD_SUBROUTE -> BackAction.RETURN_TO_MORE_MENU
        state.subRouteKind == BackSubRouteKind.TOP_LEVEL_SUBROUTE -> BackAction.RETURN_FROM_SUBROUTE
        state.currentTab != BackTabKind.HOME -> BackAction.RETURN_TO_HOME_TAB
        else -> BackAction.ALLOW_SYSTEM_EXIT
    }
}
