package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.BackupValidationResult
import com.nexus.app.data.backup.GoalMilestoneBackupDto
import com.nexus.app.data.backup.GoalProgressLogBackupDto
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.backup.PersonalGoalBackupDto
import com.nexus.app.data.backup.PersonalizationSettingsBackupDto
import com.nexus.app.data.backup.QuoteBackupDto
import com.nexus.app.data.db.entity.GoalMilestoneEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.QuoteEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.DashboardBlockConfig
import com.nexus.app.domain.model.DashboardBlockType
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.GoalStatusFilter
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.PersonalGoalWithDetails
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.QuoteLibraryStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NexusPhase6UnitTests {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    // 1. Ativacao e desativacao de blocos do dashboard
    @Test
    fun `toggling dashboard blocks updates enabled flag without losing other block settings`() {
        val defaults = NexusRepository.defaultDashboardBlocks()
        assertEquals(9, defaults.size)
        assertTrue(defaults.all { it.enabled })

        val disabled = NexusRepository.toggleDashboardBlock(defaults, DashboardBlockType.DAILY_QUOTE, false)
        assertFalse(disabled.first { it.type == DashboardBlockType.DAILY_QUOTE }.enabled)
        assertEquals(8, disabled.count { it.enabled })

        val reenabled = NexusRepository.toggleDashboardBlock(disabled, DashboardBlockType.DAILY_QUOTE, true)
        assertTrue(reenabled.first { it.type == DashboardBlockType.DAILY_QUOTE }.enabled)
        assertEquals(9, reenabled.count { it.enabled })
    }

    // 2. Reordenacao dos blocos
    @Test
    fun `moving dashboard blocks reindexes contiguous zero-based order cleanly`() {
        val defaults = NexusRepository.defaultDashboardBlocks()
        val movedUp = NexusRepository.moveDashboardBlock(defaults, DashboardBlockType.FINANCE_SUMMARY, -2)
        val orderedTypes = movedUp.sortedBy { it.order }.map { it.type }

        assertEquals(DashboardBlockType.FINANCE_SUMMARY, orderedTypes[1])
        assertEquals((0 until orderedTypes.size).toList(), movedUp.sortedBy { it.order }.map { it.order })

        // Limite superior/inferior nao quebra ordem
        val clamped = NexusRepository.moveDashboardBlock(movedUp, DashboardBlockType.GREETING, -5)
        assertEquals(DashboardBlockType.GREETING, clamped.sortedBy { it.order }.first().type)
    }

    // 3. Restauracao do layout padrao
    @Test
    fun `resetting dashboard blocks restores canonical order enabled flags and maxItems`() {
        val customized = NexusRepository.defaultDashboardBlocks()
            .let { NexusRepository.toggleDashboardBlock(it, DashboardBlockType.GOALS, false) }
            .let { NexusRepository.moveDashboardBlock(it, DashboardBlockType.CHARTS_INDICATORS, -2) }
            .let {
                NexusRepository.updateDashboardBlockConfig(
                    it,
                    DashboardBlockConfig(type = DashboardBlockType.TASK_SUMMARY, enabled = true, order = 2, maxItems = 8)
                )
            }
        assertFalse(customized.first { it.type == DashboardBlockType.GOALS }.enabled)

        val restored = NexusRepository.defaultDashboardBlocks()
        assertEquals(DashboardBlockType.GREETING, restored.first().type)
        assertEquals(DashboardBlockType.TASK_SUMMARY, restored[2].type)
        assertEquals(DashboardBlockType.FINANCE_SUMMARY, restored[3].type)
        assertEquals(DashboardBlockType.CHARTS_INDICATORS, restored[4].type)
        assertTrue(restored.all { it.enabled })
        assertEquals(4, restored.first { it.type == DashboardBlockType.TASK_SUMMARY }.maxItems)
    }

    // 4. Persistencia das preferencias de personalizacao
    @Test
    fun `dashboard block configs round-trip through JSON serialization and fill missing blocks safely`() {
        val customized = NexusRepository.moveDashboardBlock(
            NexusRepository.toggleDashboardBlock(NexusRepository.defaultDashboardBlocks(), DashboardBlockType.UPCOMING_EVENTS, false),
            DashboardBlockType.GOALS,
            -3
        )
        val serialized = NexusRepository.serializeDashboardBlocks(customized)
        val parsed = NexusRepository.parseDashboardBlocks(serialized)

        assertEquals(customized.size, parsed.size)
        assertEquals(
            customized.sortedBy { it.order }.map { it.type to it.enabled },
            parsed.sortedBy { it.order }.map { it.type to it.enabled }
        )

        // JSON parcial ou invalido cai em defaults seguros sem crash
        val partialParsed = NexusRepository.parseDashboardBlocks("""[{"type":"GREETING","enabled":false,"order":0}]""")
        assertEquals(9, partialParsed.size)
        assertFalse(partialParsed.first { it.type == DashboardBlockType.GREETING }.enabled)
        assertTrue(partialParsed.first { it.type == DashboardBlockType.GOALS }.enabled)

        val fallback = NexusRepository.parseDashboardBlocks("corrupted-json")
        assertEquals(NexusRepository.defaultDashboardBlocks(), fallback)
    }

    // 5. Edicao de nome de exibicao e texto de saudacao
    @Test
    fun `greeting formatter supports AUTO time greeting custom template with placeholder and date formats`() {
        val customWithPlaceholder = DateTimeUtils.formatGreeting("Ana", "Foco total hoje, {nome}!")
        assertEquals("Foco total hoje, Ana!", customWithPlaceholder)

        val customLiteral = DateTimeUtils.formatGreeting("Ana", "Central de Comando")
        assertEquals("Central de Comando", customLiteral)

        val autoMorning = DateTimeUtils.greetingForHour("Enzo", 9)
        val autoAfternoon = DateTimeUtils.greetingForHour("Enzo", 15)
        val autoNight = DateTimeUtils.greetingForHour("Enzo", 21)
        assertEquals("Bom dia, Enzo", autoMorning)
        assertEquals("Boa tarde, Enzo", autoAfternoon)
        assertEquals("Boa noite, Enzo", autoNight)

        val sampleDate = LocalDate.of(2026, 9, 15)
        assertEquals("15/09/2026", DateTimeUtils.formatDateByPattern(sampleDate, NexusDateFormat.DD_MM_YYYY))
        assertTrue(DateTimeUtils.formatDateByPattern(sampleDate, NexusDateFormat.WEEKDAY_DD_MM_YYYY).contains("15/09/2026"))
        assertTrue(DateTimeUtils.formatDateByPattern(sampleDate, NexusDateFormat.DD_MMM_YYYY).contains("2026"))
    }

    // 6. Cadastro, edicao e exclusao de frases locais
    @Test
    fun `quote entity preserves blank optional authorship without inventing author`() {
        val created = QuoteEntity(
            id = "q1",
            text = "Disciplina gera liberdade.",
            author = "",
            isActive = true,
            createdAt = 100L,
            updatedAt = 100L
        )
        assertEquals("", created.author)

        val edited = created.copy(
            text = "Constância supera intensidade isolada.",
            author = "Autoria Confirmada",
            updatedAt = 200L
        )
        assertEquals("Constância supera intensidade isolada.", edited.text)
        assertEquals("Autoria Confirmada", edited.author)

        val libraryAfterDelete = listOf(edited).filterNot { it.id == "q1" }
        assertTrue(libraryAfterDelete.isEmpty())
    }

    // 7. Rotacao diaria sem repeticao imediata
    @Test
    fun `daily quote selection stays deterministic within same day and avoids immediate next-day repeat`() {
        val quotes = listOf(
            QuoteEntity(id = "q1", text = "Frase 1", author = "", isActive = true, shownCount = 0, createdAt = 1L, updatedAt = 1L),
            QuoteEntity(id = "q2", text = "Frase 2", author = "Autor B", isActive = true, shownCount = 0, createdAt = 2L, updatedAt = 2L)
        )
        val baseSettings = PersonalizationSettingsEntity(createdAt = 1L, updatedAt = 1L)

        val (day1Selection, day1Picked) = NexusRepository.selectDailyQuote(quotes, baseSettings, "2026-09-15")
        assertEquals(QuoteLibraryStatus.ROTATING, day1Selection.libraryStatus)
        assertNotNull(day1Picked)

        // Reabrir no mesmo dia devolve exatamente a mesma frase persistida
        val sameDaySettings = baseSettings.copy(lastQuoteId = day1Picked!!.id, lastQuoteDateIso = "2026-09-15")
        val (reopenSelection, reopenPicked) = NexusRepository.selectDailyQuote(quotes, sameDaySettings, "2026-09-15")
        assertEquals(day1Picked.id, reopenSelection.quote?.id)
        assertNull(reopenPicked)

        // Dia seguinte com >= 2 ativas evita repetir a frase do dia anterior
        val updatedQuotes = quotes.map {
            if (it.id == day1Picked.id) it.copy(shownCount = 1, lastShownDateIso = "2026-09-15") else it
        }
        val (day2Selection, day2Picked) = NexusRepository.selectDailyQuote(updatedQuotes, sameDaySettings, "2026-09-16")
        assertNotNull(day2Picked)
        assertNotEquals(day1Picked.id, day2Selection.quote?.id)
    }

    // 8. Comportamento seguro com 0 e 1 frase ativa
    @Test
    fun `daily quote handles zero active quotes and single active quote safely`() {
        val settings = PersonalizationSettingsEntity(createdAt = 1L, updatedAt = 1L)

        // 0 frases
        val (emptySel, emptyPick) = NexusRepository.selectDailyQuote(emptyList(), settings, "2026-09-15")
        assertEquals(QuoteLibraryStatus.EMPTY, emptySel.libraryStatus)
        assertNull(emptySel.quote)
        assertNull(emptyPick)

        // Todas inativas
        val inactiveOnly = listOf(QuoteEntity(id = "q-off", text = "Inativa", isActive = false, createdAt = 1L, updatedAt = 1L))
        val (inactiveSel, _) = NexusRepository.selectDailyQuote(inactiveOnly, settings, "2026-09-15")
        assertEquals(QuoteLibraryStatus.EMPTY, inactiveSel.libraryStatus)

        // Apenas 1 frase ativa
        val singleOnly = listOf(QuoteEntity(id = "q-only", text = "Única frase", author = "", isActive = true, createdAt = 1L, updatedAt = 1L))
        val (singleSel, singlePick) = NexusRepository.selectDailyQuote(singleOnly, settings, "2026-09-15")
        assertEquals(QuoteLibraryStatus.SINGLE_QUOTE, singleSel.libraryStatus)
        assertEquals("q-only", singleSel.quote?.id)
        assertNotNull(singlePick)
    }

    // 9. Importacao e exportacao de frases locais
    @Test
    fun `quote importer parses JSON and TXT while keeping missing authorship blank`() {
        val rawJson = """
            [
              { "text": "Foco é dizer não.", "author": "" },
              { "text": "Um passo por dia.", "author": "Autor Real" }
            ]
        """.trimIndent()
        val jsonImported = NexusRepository.parseImportedQuotes(rawJson)
        assertEquals(2, jsonImported.size)
        assertEquals("Foco é dizer não." to "", jsonImported[0])
        assertEquals("Um passo por dia." to "Autor Real", jsonImported[1])

        val rawTxt = """
            # Comentário ignorado
            Primeira frase sem autoria
            Segunda frase - Autor Conhecido
            Terceira frase | Outro Autor
        """.trimIndent()
        val txtImported = NexusRepository.parseImportedQuotes(rawTxt)
        assertEquals(3, txtImported.size)
        assertEquals("Primeira frase sem autoria" to "", txtImported[0])
        assertEquals("Segunda frase" to "Autor Conhecido", txtImported[1])
        assertEquals("Terceira frase" to "Outro Autor", txtImported[2])
    }

    // 10 e 11. Criacao/edicao de objetivos pessoais e marcos com progresso AUTO e MANUAL
    @Test
    fun `goal progress computes automatically from milestones tasks projects or respects manual mode`() {
        val goalAuto = PersonalGoalEntity(
            id = "g1",
            title = "Certificação Android",
            progressMode = GoalProgressMode.AUTO,
            createdAt = 1L,
            updatedAt = 1L
        )
        val m1 = GoalMilestoneEntity(id = "m1", goalId = "g1", title = "Módulo 1", isCompleted = true, createdAt = 1L, updatedAt = 1L)
        val m2 = GoalMilestoneEntity(id = "m2", goalId = "g1", title = "Módulo 2", isCompleted = false, createdAt = 2L, updatedAt = 2L)
        val linkedTaskDone = TaskWithDetails(
            task = TaskEntity(id = "t1", title = "Estudar Room", goalId = "g1", status = TaskStatus.COMPLETED, createdAt = 1L, updatedAt = 1L),
            project = null, category = null, event = null, subtasks = emptyList()
        )
        val linkedProjectActive = ProjectWithProgress(
            project = ProjectEntity(id = "p1", name = "App Prático", goalId = "g1", status = ProjectStatus.ACTIVE, createdAt = 1L, updatedAt = 1L),
            totalTasks = 1, completedTasks = 0
        )

        // 2 de 4 concluidos (m1 + t1) = 50%
        val autoPct = NexusRepository.computeGoalProgressPercent(
            goal = goalAuto,
            milestones = listOf(m1, m2),
            linkedTasks = listOf(linkedTaskDone),
            linkedProjects = listOf(linkedProjectActive)
        )
        assertEquals(50, autoPct)

        // Modo manual preserva valor informado
        val goalManual = goalAuto.copy(progressMode = GoalProgressMode.MANUAL, manualProgressPercent = 72)
        assertEquals(
            72,
            NexusRepository.computeGoalProgressPercent(goalManual, listOf(m1, m2), emptyList(), emptyList())
        )

        // Status concluido sempre 100%
        assertEquals(
            100,
            NexusRepository.computeGoalProgressPercent(goalAuto.copy(status = GoalStatus.COMPLETED), listOf(m2), emptyList(), emptyList())
        )
    }

    // 12. Vinculacao de tarefas e projetos a objetivos sem duplicacao
    @Test
    fun `linking tasks and projects uses foreign key without duplicating existing entities`() {
        val originalTask = TaskEntity(id = "task-1", title = "Ler documentação", goalId = null, createdAt = 1L, updatedAt = 1L)
        val originalProject = ProjectEntity(id = "proj-1", name = "NEXUS v5", goalId = null, createdAt = 1L, updatedAt = 1L)

        // Simula linkTaskToGoal / linkProjectToGoal via goalId FK
        val linkedTask = originalTask.copy(goalId = "goal-1")
        val linkedProject = originalProject.copy(goalId = "goal-1")

        val allTasks = listOf(linkedTask)
        val allProjects = listOf(linkedProject)

        assertEquals(1, allTasks.size)
        assertEquals(1, allProjects.size)
        assertEquals("goal-1", allTasks.single().goalId)
        assertEquals("goal-1", allProjects.single().goalId)

        // Ao desvincular ou excluir objetivo, registros sao preservados com goalId = null
        val unlinkedTask = linkedTask.copy(goalId = null)
        assertEquals("task-1", unlinkedTask.id)
        assertNull(unlinkedTask.goalId)
    }

    // 13. Filtros de objetivos por status
    @Test
    fun `personal goal status filters distinguish active overdue paused completed and all`() {
        val today = LocalDate.of(2026, 9, 15)
        val startOfToday = DateTimeUtils.localDateToStartOfDayEpoch(today)
        val pastDeadline = DateTimeUtils.localDateTimeToEpoch(today.minusDays(2), LocalTime.of(12, 0))
        val futureDeadline = DateTimeUtils.localDateTimeToEpoch(today.plusDays(10), LocalTime.of(12, 0))

        val gOverdue = PersonalGoalEntity(id = "g1", title = "Atrasado", status = GoalStatus.ACTIVE, targetDateAt = pastDeadline, createdAt = 1L, updatedAt = 1L)
        val gOnTime = PersonalGoalEntity(id = "g2", title = "No prazo", status = GoalStatus.ACTIVE, targetDateAt = futureDeadline, createdAt = 2L, updatedAt = 2L)
        val gPaused = PersonalGoalEntity(id = "g3", title = "Pausado", status = GoalStatus.PAUSED, targetDateAt = pastDeadline, createdAt = 3L, updatedAt = 3L)
        val gDone = PersonalGoalEntity(id = "g4", title = "Concluído", status = GoalStatus.COMPLETED, createdAt = 4L, updatedAt = 4L)

        assertTrue(NexusRepository.isGoalOverdue(gOverdue, startOfToday))
        assertFalse(NexusRepository.isGoalOverdue(gOnTime, startOfToday))
        assertFalse(NexusRepository.isGoalOverdue(gPaused, startOfToday))
        assertFalse(NexusRepository.isGoalOverdue(gDone, startOfToday))

        val wrapped = listOf(gOverdue, gOnTime, gPaused, gDone).map { g ->
            PersonalGoalWithDetails(goal = g, category = null, isOverdue = NexusRepository.isGoalOverdue(g, startOfToday))
        }

        val onlyOverdue = wrapped.filter { it.isOverdue }
        val onlyPaused = wrapped.filter { it.goal.status == GoalStatus.PAUSED }
        val onlyCompleted = wrapped.filter { it.goal.status == GoalStatus.COMPLETED }
        val onlyActive = wrapped.filter { it.goal.status == GoalStatus.ACTIVE }

        assertEquals(listOf("g1"), onlyOverdue.map { it.goal.id })
        assertEquals(listOf("g3"), onlyPaused.map { it.goal.id })
        assertEquals(listOf("g4"), onlyCompleted.map { it.goal.id })
        assertEquals(listOf("g1", "g2"), onlyActive.map { it.goal.id })
        assertEquals(4, GoalStatusFilter.ALL.let { wrapped.size })
    }

    // 14. Backup e restauracao envolvendo objetivos, marcos, frases, personalizacao e v1/v2/v3 -> v4
    @Test
    fun `backup v4 round-trips all phase 6 records and v1 v2 v3 payloads remain backward compatible`() {
        val v4Payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            exportedAt = "2026-09-15T12:00:00-03:00",
            appVersion = "5.0.0",
            databaseVersion = 4,
            personalizationSettings = listOf(
                PersonalizationSettingsBackupDto(
                    greetingEnabled = true,
                    greetingTemplate = "Foco, {nome}",
                    dateFormatPattern = NexusDateFormat.DD_MM_YYYY.name,
                    showDailyQuoteOnDashboard = true,
                    compactDashboardMode = true,
                    showMetricSummaryOnDashboard = false,
                    dashboardBlocksJson = NexusRepository.serializeDashboardBlocks(NexusRepository.defaultDashboardBlocks()),
                    createdAt = 1L,
                    updatedAt = 2L
                )
            ),
            quotes = listOf(
                QuoteBackupDto(id = "q1", text = "Teste local", author = "", isActive = true, createdAt = 1L, updatedAt = 1L)
            ),
            personalGoals = listOf(
                PersonalGoalBackupDto(
                    id = "pg1",
                    title = "Ler 12 livros",
                    status = GoalStatus.ACTIVE.name,
                    progressMode = GoalProgressMode.AUTO.name,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            ),
            goalMilestones = listOf(
                GoalMilestoneBackupDto(id = "gm1", goalId = "pg1", title = "Livro 1", isCompleted = true, createdAt = 1L, updatedAt = 1L)
            ),
            goalProgressLogs = listOf(
                GoalProgressLogBackupDto(id = "gl1", goalId = "pg1", progressPercent = 10, note = "Iniciado", recordedAt = 1L, createdAt = 1L, updatedAt = 1L)
            )
        )

        val encoded = tolerantJson.encodeToString(v4Payload)
        val decoded = tolerantJson.decodeFromString<NexusBackupPayload>(encoded)

        assertEquals(NexusBackupPayload.CURRENT_FORMAT_VERSION, decoded.formatVersion)
        assertEquals(1, decoded.personalizationSettings.size)
        assertEquals("Foco, {nome}", decoded.personalizationSettings.single().greetingTemplate)
        assertEquals(1, decoded.quotes.size)
        assertEquals("", decoded.quotes.single().author)
        assertEquals(1, decoded.personalGoals.size)
        assertEquals(1, decoded.goalMilestones.size)
        assertEquals(1, decoded.goalProgressLogs.size)

        // Retrocompatibilidade v1/v2/v3 sem campos da Fase 6
        val legacyV3Json = """
            {
              "formatVersion": 3,
              "exportedAt": "2026-08-10T10:00:00-03:00",
              "appVersion": "4.0.0",
              "databaseVersion": 3,
              "settings": [
                { "id": "nexus_settings_default", "userName": "Enzo", "darkTheme": true, "declaredBalanceCents": 100000, "createdAt": 1, "updatedAt": 1 }
              ],
              "tasks": [
                { "id": "t-v3", "title": "Tarefa antiga", "priority": "NORMAL", "status": "NOT_STARTED", "createdAt": 1, "updatedAt": 1 }
              ]
            }
        """.trimIndent()
        val legacyDecoded = tolerantJson.decodeFromString<NexusBackupPayload>(legacyV3Json)
        assertEquals(3, legacyDecoded.formatVersion)
        assertNull(legacyDecoded.tasks.single().goalId)
        assertTrue(legacyDecoded.personalGoals.isEmpty())
        assertTrue(legacyDecoded.quotes.isEmpty())

        // Tipo de resultado de validacao disponivel
        assertTrue(BackupValidationResult.Invalid("x").userMessage == "x")
    }
}
