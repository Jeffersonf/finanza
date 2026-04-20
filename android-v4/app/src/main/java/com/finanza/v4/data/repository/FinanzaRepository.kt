package com.finanza.v4.data.repository

import com.finanza.v4.data.local.AccountDao
import com.finanza.v4.data.local.AccountEntity
import com.finanza.v4.data.local.AppSettingsDao
import com.finanza.v4.data.local.AppSettingsEntity
import com.finanza.v4.data.local.BudgetDao
import com.finanza.v4.data.local.BudgetEntity
import com.finanza.v4.data.local.CategoryDao
import com.finanza.v4.data.local.CategoryEntity
import com.finanza.v4.data.local.GoalDao
import com.finanza.v4.data.local.GoalEntity
import com.finanza.v4.data.local.ShoppingItemDao
import com.finanza.v4.data.local.ShoppingItemEntity
import com.finanza.v4.data.local.ShoppingListDao
import com.finanza.v4.data.local.ShoppingListEntity
import com.finanza.v4.data.local.TransactionDao
import com.finanza.v4.data.local.TransactionEntity
import com.finanza.v4.domain.Account
import com.finanza.v4.domain.AccountDraft
import com.finanza.v4.domain.Budget
import com.finanza.v4.domain.BudgetDraft
import com.finanza.v4.domain.Goal
import com.finanza.v4.domain.GoalDraft
import com.finanza.v4.domain.MoneySummary
import com.finanza.v4.domain.ShoppingItem
import com.finanza.v4.domain.ShoppingItemDraft
import com.finanza.v4.domain.ShoppingList
import com.finanza.v4.domain.ShoppingListDraft
import com.finanza.v4.domain.Transaction
import com.finanza.v4.domain.TransactionDraft
import com.finanza.v4.domain.TransactionType
import com.finanza.v4.data.sync.FinanzaApiClient
import com.finanza.v4.data.sync.SyncConfig
import com.finanza.v4.data.sync.SyncPreferences
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class FinanzaRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val categoryDao: CategoryDao,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    private val appSettingsDao: AppSettingsDao,
    private val syncPreferences: SyncPreferences,
    private val apiClient: FinanzaApiClient
) {
    val syncConfig: Flow<SyncConfig> = syncPreferences.config

    fun observeDashboard(month: YearMonth): Flow<DashboardSnapshot> {
        val monthKey = month.toString()
        return combine(
            accountDao.observeAccounts(),
            transactionDao.observeMonth(monthKey),
            transactionDao.observeRecent(8),
            budgetDao.observeMonth(monthKey)
        ) { accounts, monthTransactions, recent, budgets ->
            DashboardSnapshot(
                summary = summarize(accounts, monthTransactions),
                accounts = accounts.map { it.toDomain() },
                recent = recent.map { it.toDomain() },
                budgetUsage = budgetUsage(monthTransactions, budgets)
            )
        }
    }

    fun observeNeedsSeed(): Flow<Boolean> = accountDao.observeAccounts().map { it.isEmpty() }

    fun observeAccounts(): Flow<List<Account>> {
        return accountDao.observeAccounts().map { accounts -> accounts.map { it.toDomain() } }
    }

    fun observeBudgets(month: YearMonth): Flow<List<Budget>> {
        return budgetDao.observeMonth(month.toString()).map { budgets -> budgets.map { it.toDomain() } }
    }

    fun observeGoals(): Flow<List<Goal>> {
        return goalDao.observeGoals().map { goals -> goals.map { it.toDomain() } }
    }

    fun observeCategoryNames(): Flow<List<String>> {
        return categoryDao.observeCategories().map { categories ->
            categories.map { it.name }.filter { it.isNotBlank() }.ifEmpty { defaultCategories() }
        }
    }

    fun observeShopping(): Flow<ShoppingSnapshot> {
        return combine(
            shoppingListDao.observeLists(),
            shoppingItemDao.observeItems()
        ) { lists, items ->
            ShoppingSnapshot(
                lists = lists.map { it.toDomain() },
                items = items.map { it.toDomain() }
            )
        }
    }

    fun observeAppPreferences(): Flow<AppPreferences> {
        return appSettingsDao.observe().map { settings -> settings?.toPreferences() ?: AppPreferences() }
    }

    fun observeTransactions(filters: TransactionFilters): Flow<List<Transaction>> {
        return transactionDao
            .observeFiltered(
                month = filters.month.toString(),
                category = filters.category,
                type = filters.type?.toStorage()
            )
            .map { transactions -> transactions.map { it.toDomain() } }
    }

    suspend fun saveTransaction(draft: TransactionDraft) {
        val existing = draft.id?.let { transactionDao.getById(it) }
        val saved = TransactionEntity(
            id = draft.id ?: UUID.randomUUID().toString(),
            accountId = draft.accountId,
            type = draft.type.toStorage(),
            description = draft.description.trim(),
            category = draft.category.trim(),
            amountCents = draft.amountCents,
            date = draft.date,
            note = draft.note.trim(),
            paid = existing?.paid ?: false,
            pending = draft.pending,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        existing?.let { reverseBalanceImpact(it) }
        transactionDao.upsert(saved)
        applyBalanceImpact(saved)
    }

    suspend fun setTransactionPaid(id: String, paid: Boolean) {
        val existing = transactionDao.getById(id) ?: return
        if (existing.pending && paid) {
            applyBalanceImpact(existing.copy(pending = false, paid = true))
        }
        transactionDao.setPaid(id, paid)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.getById(id)?.let { reverseBalanceImpact(it) }
        transactionDao.deleteById(id)
    }

    suspend fun saveAppPreferences(preferences: AppPreferences) {
        appSettingsDao.upsert(preferences.toEntity())
    }

    suspend fun saveAccount(draft: AccountDraft) {
        val existing = draft.id?.let { accountDao.getById(it) }
        accountDao.upsert(
            AccountEntity(
                id = draft.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim(),
                icon = draft.icon.trim().ifBlank { "ðŸ¦" },
                type = draft.type,
                balanceCents = draft.balanceCents,
                yieldRate = draft.yieldRate.takeIf { draft.type == "investment" } ?: existing?.yieldRate ?: 0.0
            )
        )
    }

    suspend fun deleteAccount(id: String): DeleteResult {
        if (transactionDao.countByAccount(id) > 0) {
            return DeleteResult.Blocked("Esta conta possui lanÃ§amentos vinculados.")
        }
        accountDao.deleteById(id)
        return DeleteResult.Deleted
    }

    suspend fun saveBudget(draft: BudgetDraft) {
        budgetDao.upsert(
            BudgetEntity(
                id = draft.id ?: UUID.randomUUID().toString(),
                category = draft.category.trim(),
                limitCents = draft.limitCents,
                month = draft.month
            )
        )
    }

    suspend fun deleteBudget(id: String) {
        budgetDao.deleteById(id)
    }

    suspend fun saveGoal(draft: GoalDraft) {
        goalDao.upsert(
            GoalEntity(
                id = draft.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim(),
                icon = draft.icon.trim().ifBlank { "ðŸŽ¯" },
                targetCents = draft.targetCents,
                currentCents = draft.currentCents,
                deadline = draft.deadline,
                description = draft.description.trim(),
                monthlyCents = draft.monthlyCents
            )
        )
    }

    suspend fun deleteGoal(id: String) {
        goalDao.deleteById(id)
    }

    suspend fun saveShoppingList(draft: ShoppingListDraft) {
        shoppingListDao.upsert(
            ShoppingListEntity(
                id = draft.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim(),
                icon = draft.icon.trim().ifBlank { "ðŸ›’" },
                position = draft.position
            )
        )
    }

    suspend fun deleteShoppingList(id: String) {
        shoppingItemDao.deleteByListId(id)
        shoppingListDao.deleteById(id)
    }

    suspend fun saveShoppingItem(draft: ShoppingItemDraft) {
        shoppingItemDao.upsert(
            ShoppingItemEntity(
                id = draft.id ?: UUID.randomUUID().toString(),
                listId = draft.listId,
                name = draft.name.trim(),
                qty = draft.qty.trim(),
                category = draft.category.trim().ifBlank { "ðŸ›’ Geral" },
                bought = draft.bought
            )
        )
    }

    suspend fun setShoppingItemBought(id: String, bought: Boolean) {
        shoppingItemDao.setBought(id, bought)
    }

    suspend fun deleteShoppingItem(id: String) {
        shoppingItemDao.deleteById(id)
    }

    suspend fun importLegacyBackup(json: String): ImportResult {
        val root = JSONObject(json)
        val transactions = root.optJSONArray("transactions")
            ?: return ImportResult.Failed("Arquivo invÃ¡lido: transaÃ§Ãµes nÃ£o encontradas.")
        val accounts = root.optJSONArray("accounts") ?: JSONArray()
        val budgets = root.optJSONArray("budgets") ?: JSONArray()
        val goals = root.optJSONArray("goals") ?: JSONArray()
        val categories = root.optJSONArray("categories") ?: root.optJSONArray("customCategories") ?: JSONArray()
        val shopping = root.optJSONObject("shopping")
        val shoppingLists = shopping?.optJSONArray("lists") ?: root.optJSONArray("shoppingLists") ?: JSONArray()
        val shoppingItems = shopping?.optJSONArray("items") ?: root.optJSONArray("shoppingItems") ?: JSONArray()
        val settings = root.optJSONObject("settings")

        val importedAccounts = parseAccounts(accounts)
        val fallbackAccountId = importedAccounts.firstOrNull()?.id ?: UUID.randomUUID().toString()
        val safeAccounts = importedAccounts.ifEmpty {
            listOf(AccountEntity(fallbackAccountId, "Principal", "ðŸ¦", "checking", 0))
        }
        val importedTransactions = parseTransactions(transactions, fallbackAccountId)
        val importedBudgets = parseBudgets(budgets)
        val importedGoals = parseGoals(goals)
        val importedCategories = parseCategories(categories)
        val importedShoppingLists = parseShoppingLists(shoppingLists)
        val importedShoppingItems = parseShoppingItems(shoppingItems)

        transactionDao.deleteAll()
        budgetDao.deleteAll()
        goalDao.deleteAll()
        categoryDao.deleteAll()
        shoppingItemDao.deleteAll()
        shoppingListDao.deleteAll()
        appSettingsDao.deleteAll()
        accountDao.deleteAll()
        accountDao.upsertAll(safeAccounts)
        transactionDao.upsertAll(importedTransactions)
        budgetDao.upsertAll(importedBudgets)
        goalDao.upsertAll(importedGoals)
        categoryDao.upsertAll(importedCategories)
        shoppingListDao.upsertAll(importedShoppingLists.ifEmpty { listOf(defaultShoppingList()) })
        shoppingItemDao.upsertAll(importedShoppingItems)
        settings?.let { appSettingsDao.upsert(parseSettings(it)) }

        return ImportResult.Imported(
            accounts = safeAccounts.size,
            transactions = importedTransactions.size,
            budgets = importedBudgets.size,
            goals = importedGoals.size,
            categories = importedCategories.size,
            shoppingLists = importedShoppingLists.size,
            shoppingItems = importedShoppingItems.size
        )
    }

    suspend fun login(baseUrl: String, username: String, password: String): SyncConfig {
        val config = apiClient.login(baseUrl, username, password)
        syncPreferences.save(config)
        return config
    }

    suspend fun pushLocalToRemote(config: SyncConfig) {
        require(config.connected) { "Conecte na API primeiro." }
        apiClient.putImport(config, buildBackupJson())
    }

    suspend fun exportBackupJson(): String {
        return buildBackupJson().toString(2)
    }

    suspend fun disconnectSync() {
        syncPreferences.clear()
    }

    suspend fun pullRemoteToLocal(config: SyncConfig): ImportResult {
        require(config.connected) { "Conecte na API primeiro." }
        val state = apiClient.getState(config)
        val txResponse = apiClient.getTransactions(config)
        val budgetsText = apiClient.getBudgets(config)
        val goalsText = apiClient.getGoals(config)
        val merged = JSONObject()
            .put("accounts", state.optJSONArray("accounts") ?: JSONArray())
            .put("transactions", txResponse.optJSONArray("data") ?: JSONArray())
            .put("budgets", JSONArray(budgetsText))
            .put("goals", JSONArray(goalsText))
            .put("categories", state.optJSONArray("categories") ?: JSONArray())
            .put("shopping", state.optJSONObject("shopping") ?: JSONObject())
            .put("settings", state.optJSONObject("settings") ?: JSONObject())
        return importLegacyBackup(merged.toString())
    }

    suspend fun seedIfNeeded() {
        if (accountDao.count() > 0) return
        val accountId = UUID.randomUUID().toString()
        val savingsId = UUID.randomUUID().toString()
        accountDao.upsertAll(
            listOf(
                AccountEntity(accountId, "Principal", "ðŸ¦", "checking", 150_000),
                AccountEntity(savingsId, "Reserva", "ðŸ’Ž", "savings", 820_000, yieldRate = 0.55)
            )
        )
        listOf(
            TransactionEntity(UUID.randomUUID().toString(), accountId, "income", "SalÃ¡rio", "SalÃ¡rio", 520_000, "2026-04-05"),
            TransactionEntity(UUID.randomUUID().toString(), accountId, "expense", "Mercado", "AlimentaÃ§Ã£o", 86_900, "2026-04-08"),
            TransactionEntity(UUID.randomUUID().toString(), accountId, "expense", "Academia", "SaÃºde", 12_990, "2026-04-10"),
            TransactionEntity(UUID.randomUUID().toString(), savingsId, "income", "Rendimento", "Investimentos", 4_500, "2026-04-15"),
            TransactionEntity(UUID.randomUUID().toString(), accountId, "expense", "Internet", "Casa", 11_990, "2026-04-20", pending = true)
        ).forEach { transaction ->
            transactionDao.upsert(transaction)
            applyBalanceImpact(transaction)
        }
        budgetDao.upsertAll(
            listOf(
                BudgetEntity(UUID.randomUUID().toString(), "AlimentaÃ§Ã£o", 120_000, "2026-04"),
                BudgetEntity(UUID.randomUUID().toString(), "SaÃºde", 35_000, "2026-04"),
                BudgetEntity(UUID.randomUUID().toString(), "Casa", 80_000, "2026-04")
            )
        )
    }

    private fun summarize(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): MoneySummary {
        val income = transactions.filter { it.type == "income" }.sumOf { it.amountCents }
        val expense = transactions.filter { it.type == "expense" && !it.pending }.sumOf { it.amountCents }
        val future = transactions.filter { it.pending }.sumOf { it.amountCents }
        return MoneySummary(
            balanceCents = accounts.sumOf { it.balanceCents },
            incomeCents = income,
            expenseCents = expense,
            futureCents = future
        )
    }

    private suspend fun applyBalanceImpact(transaction: TransactionEntity) {
        val delta = balanceDelta(transaction)
        if (delta != 0L) {
            accountDao.adjustBalance(transaction.accountId, delta)
        }
    }

    private suspend fun reverseBalanceImpact(transaction: TransactionEntity) {
        val delta = balanceDelta(transaction)
        if (delta != 0L) {
            accountDao.adjustBalance(transaction.accountId, -delta)
        }
    }

    private fun balanceDelta(transaction: TransactionEntity): Long {
        if (transaction.pending) return 0L
        return if (transaction.type == "income") transaction.amountCents else -transaction.amountCents
    }

    private fun budgetUsage(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ): List<BudgetUsage> {
        return budgets.map { budget ->
            val spent = transactions
                .filter { it.type == "expense" && it.category == budget.category }
                .sumOf { it.amountCents }
            BudgetUsage(
                id = budget.id,
                category = budget.category,
                spentCents = spent,
                limitCents = budget.limitCents
            )
        }
    }

    private suspend fun buildBackupJson(): JSONObject {
        val accounts = JSONArray().also { array ->
            accountDao.listAll().forEach { account ->
                array.put(
                    JSONObject()
                        .put("id", account.id)
                        .put("name", account.name)
                        .put("icon", account.icon)
                        .put("type", account.type)
                        .put("balance", account.balanceCents / 100.0)
                        .put("yieldRate", account.yieldRate)
                )
            }
        }
        val transactions = JSONArray().also { array ->
            transactionDao.listAll().forEach { tx ->
                array.put(
                    JSONObject()
                        .put("id", tx.id)
                        .put("accountId", tx.accountId)
                        .put("type", tx.type)
                        .put("desc", tx.description)
                        .put("description", tx.description)
                        .put("category", tx.category)
                        .put("amount", tx.amountCents / 100.0)
                        .put("date", tx.date)
                        .put("note", tx.note)
                        .put("paid", tx.paid)
                        .put("pending", tx.pending)
                )
            }
        }
        val budgets = JSONArray().also { array ->
            budgetDao.listAll().forEach { budget ->
                array.put(
                    JSONObject()
                        .put("id", budget.id)
                        .put("category", budget.category)
                        .put("limit", budget.limitCents / 100.0)
                        .put("month", budget.month)
                )
            }
        }
        val goals = JSONArray().also { array ->
            goalDao.listAll().forEach { goal ->
                array.put(
                    JSONObject()
                        .put("id", goal.id)
                        .put("name", goal.name)
                        .put("icon", goal.icon)
                        .put("target", goal.targetCents / 100.0)
                        .put("current", goal.currentCents / 100.0)
                        .put("deadline", goal.deadline)
                        .put("description", goal.description)
                        .put("monthly", goal.monthlyCents / 100.0)
                )
            }
        }
        val categories = JSONArray().also { array ->
            categoryDao.listAll().forEach { category ->
                array.put(
                    JSONObject()
                        .put("id", category.id)
                        .put("icon", category.icon)
                        .put("ico", category.icon)
                        .put("name", category.name)
                        .put("color", category.color)
                        .put("col", category.color)
                )
            }
        }
        val shoppingLists = JSONArray().also { array ->
            shoppingListDao.listAll().forEach { list ->
                array.put(
                    JSONObject()
                        .put("id", list.id)
                        .put("name", list.name)
                        .put("icon", list.icon)
                        .put("ico", list.icon)
                        .put("position", list.position)
                )
            }
        }
        val shoppingItems = JSONArray().also { array ->
            shoppingItemDao.listAll().forEach { item ->
                array.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("listId", item.listId)
                        .put("list_id", item.listId)
                        .put("name", item.name)
                        .put("qty", item.qty)
                        .put("category", item.category)
                        .put("cat", item.category)
                        .put("bought", item.bought)
                        .put("createdAt", item.createdMs)
                        .put("created_ms", item.createdMs)
                )
            }
        }
        val settings = appSettingsDao.get()?.toJson() ?: JSONObject().put("theme", "dark")
        return JSONObject()
            .put("app", "Finanza")
            .put("version", "4.0.0-alpha.1")
            .put("exported_at", java.time.Instant.now().toString())
            .put("accounts", accounts)
            .put("transactions", transactions)
            .put("budgets", budgets)
            .put("goals", goals)
            .put("categories", categories)
            .put("shopping", JSONObject().put("lists", shoppingLists).put("items", shoppingItems))
            .put("settings", settings)
    }
}

data class ImportResult(
    val accounts: Int,
    val transactions: Int,
    val budgets: Int,
    val goals: Int = 0,
    val categories: Int = 0,
    val shoppingLists: Int = 0,
    val shoppingItems: Int = 0,
    val error: String? = null
) {
    companion object {
        fun Imported(
            accounts: Int,
            transactions: Int,
            budgets: Int,
            goals: Int = 0,
            categories: Int = 0,
            shoppingLists: Int = 0,
            shoppingItems: Int = 0
        ) = ImportResult(accounts, transactions, budgets, goals, categories, shoppingLists, shoppingItems)
        fun Failed(error: String) = ImportResult(0, 0, 0, error = error)
    }
}

data class DashboardSnapshot(
    val summary: MoneySummary = MoneySummary(0, 0, 0, 0),
    val accounts: List<Account> = emptyList(),
    val recent: List<Transaction> = emptyList(),
    val budgetUsage: List<BudgetUsage> = emptyList()
)

data class TransactionFilters(
    val month: YearMonth = YearMonth.now(),
    val category: String? = null,
    val type: TransactionType? = null
)

data class BudgetUsage(
    val id: String,
    val category: String,
    val spentCents: Long,
    val limitCents: Long
) {
    val progress: Float = if (limitCents <= 0) 0f else (spentCents.toFloat() / limitCents).coerceIn(0f, 1.2f)
}

data class ShoppingSnapshot(
    val lists: List<ShoppingList> = emptyList(),
    val items: List<ShoppingItem> = emptyList()
)

data class AppPreferences(
    val theme: String = "dark",
    val cdi: Double = 10.40,
    val selic: Double = 10.50,
    val monthlyIncomeCents: Long = 0L,
    val dueItems: List<DueItem> = emptyList(),
    val txView: String = "n",
    val activeList: String? = null,
    val widgetPrefs: Map<String, Boolean> = defaultWidgetPrefs(),
    val widgetOrder: List<String> = defaultWidgetOrder()
)

data class DueItem(
    val id: String,
    val name: String,
    val amountCents: Long,
    val category: String,
    val recurrence: String = "monthly",
    val nextDueDate: String,
    val dueDay: Int,
    val paymentMethod: String = "pix",
    val paymentPlace: String = "",
    val accountId: String? = null,
    val notes: String = "",
    val active: Boolean = true,
    val paidKeys: List<String> = emptyList()
)

data class DashboardWidget(
    val id: String,
    val emoji: String,
    val label: String,
    val enabled: Boolean
)

val DashboardWidgets = listOf(
    DashboardWidget("cards", "💳", "Resumo do dia a dia", true),
    DashboardWidget("charts", "ðŸ“Š", "GrÃ¡ficos", true),
    DashboardWidget("compare", "ðŸ“…", "Comparativo mensal", true),
    DashboardWidget("projection", "🔭", "Dica de projeção", true),
    DashboardWidget("weekly", "📆", "Dica da semana", true),
    DashboardWidget("anomaly", "💡", "Dica fora da curva", true),
    DashboardWidget("budalerts", "âš ï¸", "Alertas de orÃ§amento", true),
    DashboardWidget("goals", "ðŸ†", "Metas rÃ¡pidas", true),
    DashboardWidget("budgets", "ðŸŽ¯", "OrÃ§amentos rÃ¡pidos", false),
    DashboardWidget("recent", "ðŸ’¸", "Ãšltimas transaÃ§Ãµes", true),
    DashboardWidget("ministats", "ðŸ“ˆ", "Mini estatÃ­sticas", false),
    DashboardWidget("accounts", "ðŸ¦", "Saldos das contas", false),
    DashboardWidget("shopping", "ðŸ›’", "Lista de compras", true),
    DashboardWidget("barcats", "ðŸ“‰", "Ranking de gastos", false),
    DashboardWidget("saverate", "ðŸ’¹", "Taxa de economia", false)
)

val FixedDashboardWidgetIds = listOf("cards", "projection", "weekly", "anomaly")

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data class Blocked(val reason: String) : DeleteResult
}

private fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    icon = icon,
    type = type,
    balanceCents = balanceCents,
    yieldRate = yieldRate
)

private fun BudgetEntity.toDomain() = Budget(
    id = id,
    category = category,
    limitCents = limitCents,
    month = month
)

private fun GoalEntity.toDomain() = Goal(
    id = id,
    name = name,
    icon = icon,
    targetCents = targetCents,
    currentCents = currentCents,
    deadline = deadline,
    description = description,
    monthlyCents = monthlyCents
)

private fun ShoppingListEntity.toDomain() = ShoppingList(
    id = id,
    name = name,
    icon = icon,
    position = position
)

private fun ShoppingItemEntity.toDomain() = ShoppingItem(
    id = id,
    listId = listId,
    name = name,
    qty = qty,
    category = category,
    bought = bought,
    createdMs = createdMs
)

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    type = if (type == "income") TransactionType.Income else TransactionType.Expense,
    description = description,
    category = category,
    amountCents = amountCents,
    date = date,
    paid = paid,
    pending = pending
)

private fun TransactionType.toStorage(): String {
    return when (this) {
        TransactionType.Income -> "income"
        TransactionType.Expense -> "expense"
    }
}

private fun parseAccounts(accounts: JSONArray): List<AccountEntity> {
    return (0 until accounts.length()).mapNotNull { index ->
        val item = accounts.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
        val name = item.optString("name").ifBlank { "Conta" }
        AccountEntity(
            id = id,
            name = name,
            icon = item.optString("icon").ifBlank { "ðŸ¦" },
            type = item.optString("type").ifBlank { "checking" },
            balanceCents = moneyToCents(item, "balance"),
            yieldRate = item.optDouble("yieldRate", item.optDouble("yield_rate", 0.0))
        )
    }
}

private fun parseTransactions(transactions: JSONArray, fallbackAccountId: String): List<TransactionEntity> {
    return (0 until transactions.length()).mapNotNull { index ->
        val item = transactions.optJSONObject(index) ?: return@mapNotNull null
        val amountCents = moneyToCents(item, "amount")
        if (amountCents <= 0L) return@mapNotNull null
        val type = item.optString("type").ifBlank { "expense" }
        val date = item.optString("date").ifBlank { return@mapNotNull null }
        TransactionEntity(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            accountId = item.optString("accountId").ifBlank { item.optString("account_id").ifBlank { fallbackAccountId } },
            type = if (type == "income") "income" else "expense",
            description = item.optString("desc").ifBlank { item.optString("description").ifBlank { "LanÃ§amento" } },
            category = item.optString("category").ifBlank { "Outros" },
            amountCents = amountCents,
            date = date,
            note = item.optString("note"),
            paid = item.optBoolean("paid", false),
            pending = item.optBoolean("pending", false),
            createdAt = System.currentTimeMillis() - index
        )
    }
}

private fun parseBudgets(budgets: JSONArray): List<BudgetEntity> {
    val currentMonth = YearMonth.now().toString()
    return (0 until budgets.length()).mapNotNull { index ->
        val item = budgets.optJSONObject(index) ?: return@mapNotNull null
        val limitCents = moneyToCents(item, "limit")
        val category = item.optString("category")
        if (category.isBlank() || limitCents <= 0L) return@mapNotNull null
        BudgetEntity(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            category = category,
            limitCents = limitCents,
            month = item.optString("month").ifBlank { currentMonth }
        )
    }
}

private fun parseGoals(goals: JSONArray): List<GoalEntity> {
    return (0 until goals.length()).mapNotNull { index ->
        val item = goals.optJSONObject(index) ?: return@mapNotNull null
        val target = moneyToCents(item, "target")
        val name = item.optString("name")
        val deadline = item.optString("deadline").take(10)
        if (name.isBlank() || target <= 0L || deadline.isBlank()) return@mapNotNull null
        GoalEntity(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = name,
            icon = item.optString("icon").ifBlank { "ðŸŽ¯" },
            targetCents = target,
            currentCents = moneyToCents(item, "current"),
            deadline = deadline,
            description = item.optString("description").ifBlank { item.optString("desc") },
            monthlyCents = moneyToCents(item, "monthly")
        )
    }
}

private fun parseCategories(categories: JSONArray): List<CategoryEntity> {
    return (0 until categories.length()).mapNotNull { index ->
        val item = categories.optJSONObject(index) ?: return@mapNotNull null
        val name = item.optString("name")
        if (name.isBlank()) return@mapNotNull null
        CategoryEntity(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            icon = item.optString("icon").ifBlank { item.optString("ico").ifBlank { "ðŸ·ï¸" } },
            name = name,
            color = item.optString("color").ifBlank { item.optString("col").ifBlank { "#888" } }
        )
    }
}

private fun parseShoppingLists(lists: JSONArray): List<ShoppingListEntity> {
    return (0 until lists.length()).mapNotNull { index ->
        val item = lists.optJSONObject(index) ?: return@mapNotNull null
        val name = item.optString("name")
        if (name.isBlank()) return@mapNotNull null
        ShoppingListEntity(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = name,
            icon = item.optString("icon").ifBlank { item.optString("ico").ifBlank { "ðŸ›’" } },
            position = item.optInt("position", index)
        )
    }
}

private fun parseShoppingItems(items: JSONArray): List<ShoppingItemEntity> {
    return (0 until items.length()).mapNotNull { index ->
        val item = items.optJSONObject(index) ?: return@mapNotNull null
        val name = item.optString("name")
        val listId = item.optString("listId").ifBlank { item.optString("list_id") }
        if (name.isBlank() || listId.isBlank()) return@mapNotNull null
        ShoppingItemEntity(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            listId = listId,
            name = name,
            qty = item.optString("qty"),
            category = item.optString("category").ifBlank { item.optString("cat") },
            bought = item.optBoolean("bought", false),
            createdMs = item.optLong("createdAt", item.optLong("created_ms", System.currentTimeMillis() - index))
        )
    }
}

private fun parseSettings(settings: JSONObject): AppSettingsEntity {
    return AppSettingsEntity(
        theme = settings.optString("theme").ifBlank { "dark" },
        ratesJson = (settings.optJSONObject("rates") ?: JSONObject()).toString(),
        widgetPrefsJson = (settings.optJSONObject("widgetPrefs") ?: settings.optJSONObject("widget_prefs") ?: JSONObject()).toString(),
        widgetOrderJson = (settings.optJSONArray("widgetOrder") ?: settings.optJSONArray("widget_order") ?: JSONArray()).toString(),
        txView = normalizeTxView(settings.optString("txView").ifBlank { settings.optString("tx_view").ifBlank { "n" } }),
        activeList = settings.optString("activeList").ifBlank { settings.optString("active_list").ifBlank { null } }
    )
}

private fun defaultShoppingList(): ShoppingListEntity {
    return ShoppingListEntity(
        id = UUID.randomUUID().toString(),
        name = "Mercado",
        icon = "ðŸ›’",
        position = 0
    )
}

private fun AppSettingsEntity.toJson(): JSONObject {
    return JSONObject()
        .put("theme", theme)
        .put("rates", JSONObject(ratesJson.ifBlank { "{}" }))
        .put("widgetPrefs", JSONObject(widgetPrefsJson.ifBlank { "{}" }))
        .put("widget_prefs", JSONObject(widgetPrefsJson.ifBlank { "{}" }))
        .put("widgetOrder", JSONArray(widgetOrderJson.ifBlank { "[]" }))
        .put("widget_order", JSONArray(widgetOrderJson.ifBlank { "[]" }))
        .put("txView", txView)
        .put("tx_view", txView)
        .put("activeList", activeList)
        .put("active_list", activeList)
}

private fun AppSettingsEntity.toPreferences(): AppPreferences {
    val rates = jsonObjectOrEmpty(ratesJson)
    val widgetPrefsJson = jsonObjectOrEmpty(widgetPrefsJson)
    val widgetOrderJson = jsonArrayOrEmpty(widgetOrderJson)
    val prefs = defaultWidgetPrefs().toMutableMap()
    DashboardWidgets.forEach { widget ->
        if (widgetPrefsJson.has(widget.id)) prefs[widget.id] = widgetPrefsJson.optBoolean(widget.id, widget.enabled)
    }
    val order = (0 until widgetOrderJson.length()).mapNotNull { index ->
        widgetOrderJson.optString(index).takeIf { it.isNotBlank() }
    }.ifEmpty { defaultWidgetOrder() }
    return AppPreferences(
        theme = theme.ifBlank { "dark" },
        cdi = rates.optDouble("cdi", 10.40),
        selic = rates.optDouble("selic", 10.50),
        monthlyIncomeCents = rates.optLong("monthlyIncomeCents", rates.optLong("monthly_income_cents", 0L)),
        dueItems = parseDueItems(rates.optJSONArray("dueItems") ?: rates.optJSONArray("due_items") ?: JSONArray()),
        txView = normalizeTxView(txView),
        activeList = activeList?.takeIf { it.isNotBlank() },
        widgetPrefs = prefs,
        widgetOrder = order
    )
}

private fun AppPreferences.toEntity(): AppSettingsEntity {
    val rates = JSONObject()
        .put("cdi", cdi)
        .put("selic", selic)
        .put("monthlyIncomeCents", monthlyIncomeCents)
        .put("monthly_income_cents", monthlyIncomeCents)
        .put("dueItems", dueItemsToJson(dueItems))
        .put("due_items", dueItemsToJson(dueItems))
    val prefs = JSONObject()
    DashboardWidgets.forEach { widget ->
        prefs.put(widget.id, widgetPrefs[widget.id] ?: widget.enabled)
    }
    val order = JSONArray()
    val knownIds = DashboardWidgets.map { it.id }.toSet()
    widgetOrder.filter { it in knownIds }.ifEmpty { defaultWidgetOrder() }.forEach { order.put(it) }
    return AppSettingsEntity(
        theme = theme.ifBlank { "dark" },
        ratesJson = rates.toString(),
        widgetPrefsJson = prefs.toString(),
        widgetOrderJson = order.toString(),
        txView = normalizeTxView(txView),
        activeList = activeList?.takeIf { it.isNotBlank() }
    )
}

private fun normalizeTxView(value: String): String {
    return when (value) {
        "c", "chart" -> value
        else -> "n"
    }
}

private fun parseDueItems(array: JSONArray): List<DueItem> {
    return (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val amountCents = when {
            item.has("amountCents") -> item.optLong("amountCents")
            item.has("amount_cents") -> item.optLong("amount_cents")
            else -> (item.optDouble("amount", 0.0) * 100).toLong()
        }
        val date = item.optString("nextDueDate").ifBlank { item.optString("next_due_date").ifBlank { java.time.LocalDate.now().toString() } }
        DueItem(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = item.optString("name").ifBlank { "Vencimento" },
            amountCents = amountCents,
            category = item.optString("category").ifBlank { "A classificar" },
            recurrence = item.optString("recurrence").ifBlank { "monthly" },
            nextDueDate = date,
            dueDay = item.optInt("dueDay", item.optInt("due_day", date.takeLast(2).toIntOrNull() ?: 1)).coerceIn(1, 31),
            paymentMethod = item.optString("paymentMethod").ifBlank { item.optString("payment_method").ifBlank { "pix" } },
            paymentPlace = item.optString("paymentPlace").ifBlank { item.optString("payment_place") },
            accountId = item.optString("accountId").ifBlank { item.optString("account_id").ifBlank { null } },
            notes = item.optString("notes").ifBlank { item.optString("note") },
            active = item.optBoolean("active", true),
            paidKeys = jsonArrayToStrings(item.optJSONArray("paidKeys") ?: item.optJSONArray("paid_keys") ?: JSONArray())
        )
    }
}

private fun dueItemsToJson(items: List<DueItem>): JSONArray {
    val array = JSONArray()
    items.forEach { item ->
        array.put(JSONObject()
            .put("id", item.id)
            .put("name", item.name)
            .put("amount", item.amountCents / 100.0)
            .put("amountCents", item.amountCents)
            .put("category", item.category)
            .put("recurrence", item.recurrence)
            .put("nextDueDate", item.nextDueDate)
            .put("dueDay", item.dueDay)
            .put("paymentMethod", item.paymentMethod)
            .put("paymentPlace", item.paymentPlace)
            .put("accountId", item.accountId)
            .put("notes", item.notes)
            .put("active", item.active)
            .put("paidKeys", JSONArray(item.paidKeys)))
    }
    return array
}

private fun jsonArrayToStrings(array: JSONArray): List<String> {
    return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
}

private fun jsonObjectOrEmpty(raw: String): JSONObject {
    return runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrDefault(JSONObject())
}

private fun jsonArrayOrEmpty(raw: String): JSONArray {
    return runCatching { JSONArray(raw.ifBlank { "[]" }) }.getOrDefault(JSONArray())
}

private fun defaultWidgetPrefs(): Map<String, Boolean> {
    return DashboardWidgets.associate { it.id to (it.enabled || it.id in FixedDashboardWidgetIds) }
}

private fun defaultWidgetOrder(): List<String> {
    return FixedDashboardWidgetIds + DashboardWidgets.map { it.id }.filterNot { it in FixedDashboardWidgetIds }
}

private fun defaultCategories(): List<String> {
    return listOf("Alimentacao", "Casa", "Transporte", "Saude", "Educacao", "Lazer", "Salario", "Investimentos", "Outros")
}

private fun moneyToCents(item: JSONObject, key: String): Long {
    val raw = item.opt(key) ?: return 0L
    return when (raw) {
        is Number -> (raw.toDouble() * 100).toLong()
        is String -> normalizeMoney(raw).toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, java.math.RoundingMode.HALF_UP)
            ?.toLong() ?: 0L
        else -> 0L
    }
}

private fun normalizeMoney(value: String): String {
    val raw = value.trim()
    return when {
        raw.contains(".") && raw.contains(",") -> raw.replace(".", "").replace(",", ".")
        raw.contains(",") -> raw.replace(".", "").replace(",", ".")
        raw.contains(".") -> {
            val decimalDigits = raw.substringAfterLast(".").length
            if (decimalDigits in 1..2) raw else raw.replace(".", "")
        }
        else -> raw
    }
}
