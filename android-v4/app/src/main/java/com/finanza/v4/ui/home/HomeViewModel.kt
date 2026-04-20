package com.finanza.v4.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.finanza.v4.data.repository.DashboardSnapshot
import com.finanza.v4.data.repository.DeleteResult
import com.finanza.v4.data.repository.FinanzaRepository
import com.finanza.v4.data.repository.ShoppingSnapshot
import com.finanza.v4.data.repository.TransactionFilters
import com.finanza.v4.data.repository.BudgetUsage
import com.finanza.v4.data.repository.AppPreferences
import com.finanza.v4.data.repository.DueItem
import com.finanza.v4.data.sync.SyncConfig
import com.finanza.v4.domain.Account
import com.finanza.v4.domain.AccountDraft
import com.finanza.v4.domain.BudgetDraft
import com.finanza.v4.domain.Goal
import com.finanza.v4.domain.GoalDraft
import com.finanza.v4.domain.ShoppingItem
import com.finanza.v4.domain.ShoppingItemDraft
import com.finanza.v4.domain.ShoppingList
import com.finanza.v4.domain.ShoppingListDraft
import com.finanza.v4.domain.Transaction
import com.finanza.v4.domain.TransactionDraft
import com.finanza.v4.domain.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: FinanzaRepository
) : ViewModel() {
    val state: StateFlow<DashboardSnapshot> = repository
        .observeDashboard(YearMonth.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSnapshot())

    private val _addTransactionState = MutableStateFlow(AddTransactionUiState())
    val addTransactionState: StateFlow<AddTransactionUiState> = _addTransactionState.asStateFlow()

    private val _accountFormState = MutableStateFlow(AccountFormUiState())
    val accountFormState: StateFlow<AccountFormUiState> = _accountFormState.asStateFlow()

    private val _budgetFormState = MutableStateFlow(BudgetFormUiState())
    val budgetFormState: StateFlow<BudgetFormUiState> = _budgetFormState.asStateFlow()

    private val _goalFormState = MutableStateFlow(GoalFormUiState())
    val goalFormState: StateFlow<GoalFormUiState> = _goalFormState.asStateFlow()

    private val _shoppingFormState = MutableStateFlow(ShoppingFormUiState())
    val shoppingFormState: StateFlow<ShoppingFormUiState> = _shoppingFormState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = repository.syncConfig
        .mapToSettingsState(_settingsState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _currentScreen = MutableStateFlow(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _transactionFilters = MutableStateFlow(TransactionFilterUiState())
    val transactionFilters: StateFlow<TransactionFilterUiState> = _transactionFilters.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = _transactionFilters
        .flatMapLatest { filters ->
            repository.observeTransactions(
                TransactionFilters(
                    month = filters.month,
                    category = filters.category,
                    type = filters.type
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals = repository
        .observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoryNames = repository
        .observeCategoryNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shopping = repository
        .observeShopping()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingSnapshot())

    val appPreferences = repository
        .observeAppPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
        }
    }

    fun openAddTransaction() {
        val firstAccount = state.value.accounts.firstOrNull()?.id.orEmpty()
        _addTransactionState.value = AddTransactionUiState(
            visible = true,
            mode = AddTransactionMode.Create,
            accountId = firstAccount,
            date = LocalDate.now().toString()
        )
    }

    fun openEditTransaction(transaction: Transaction) {
        _addTransactionState.value = AddTransactionUiState(
            visible = true,
            mode = AddTransactionMode.Edit,
            id = transaction.id,
            type = transaction.type,
            amount = formatInputAmount(transaction.amountCents),
            description = transaction.description,
            category = transaction.category,
            accountId = transaction.accountId,
            date = transaction.date,
            pending = transaction.pending
        )
    }

    fun closeAddTransaction() {
        _addTransactionState.update { it.copy(visible = false, error = null, saving = false) }
    }

    fun updateAddTransaction(transform: (AddTransactionUiState) -> AddTransactionUiState) {
        _addTransactionState.update { transform(it).copy(error = null) }
    }

    fun parseQuickTransaction() {
        val current = _addTransactionState.value
        val parsed = parseTransactionText(current.quickText)
        if (parsed == null) {
            _addTransactionState.update { it.copy(error = "Nao entendi. Tente: mercado 38,90 hoje") }
            return
        }
        _addTransactionState.update {
            it.copy(
                type = parsed.type,
                amount = formatInputAmount(parsed.amountCents),
                description = parsed.description,
                category = parsed.category,
                date = parsed.date,
                pending = parsed.pending,
                error = null
            )
        }
    }

    fun saveAddTransaction() {
        val current = _addTransactionState.value
        val amountCents = parseCents(current.amount)
        val accountId = current.accountId.ifBlank { state.value.accounts.firstOrNull()?.id.orEmpty() }
        val description = current.description.trim()
        val category = current.category.trim()
        val error = when {
            accountId.isBlank() -> "Crie ou selecione uma conta."
            amountCents <= 0 -> "Informe um valor vÃ¡lido."
            description.isBlank() -> "Informe uma descriÃ§Ã£o."
            category.isBlank() -> "Escolha uma categoria."
            !isValidIsoDate(current.date) -> "Use uma data no formato AAAA-MM-DD."
            else -> null
        }
        if (error != null) {
            _addTransactionState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _addTransactionState.update { it.copy(saving = true, error = null) }
            repository.saveTransaction(
                TransactionDraft(
                    id = current.id,
                    accountId = accountId,
                    type = current.type,
                    description = description,
                    category = category,
                    amountCents = amountCents,
                    date = current.date,
                    pending = current.pending && current.type == TransactionType.Expense
                )
            )
            _addTransactionState.value = AddTransactionUiState(visible = false)
            autoPushLocalChange()
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setTransactionMonth(month: YearMonth) {
        _transactionFilters.update { it.copy(month = month) }
    }

    fun setTransactionCategory(category: String?) {
        _transactionFilters.update { it.copy(category = category) }
    }

    fun setTransactionType(type: TransactionType?) {
        _transactionFilters.update { it.copy(type = type) }
    }

    fun markTransactionPaid(id: String, paid: Boolean) {
        viewModelScope.launch {
            repository.setTransactionPaid(id, paid)
            autoPushLocalChange()
        }
    }

    fun deleteTransaction(id: String) {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Excluir lanÃ§amento?", "Esta aÃ§Ã£o nÃ£o pode ser desfeita.") {
                viewModelScope.launch {
                    repository.deleteTransaction(id)
                    autoPushLocalChange()
                }
            })
        }
    }

    fun openAddAccount() {
        _accountFormState.value = AccountFormUiState(visible = true)
    }

    fun openEditAccount(account: Account) {
        _accountFormState.value = AccountFormUiState(
            visible = true,
            mode = FormMode.Edit,
            id = account.id,
            name = account.name,
            icon = account.icon,
            type = account.type,
            balance = formatInputAmount(account.balanceCents),
            yieldRate = account.yieldRate.takeIf { it > 0.0 }?.toString().orEmpty()
        )
    }

    fun closeAccountForm() {
        _accountFormState.update { it.copy(visible = false, saving = false, error = null) }
    }

    fun updateAccountForm(transform: (AccountFormUiState) -> AccountFormUiState) {
        _accountFormState.update { transform(it).copy(error = null) }
    }

    fun saveAccount() {
        val current = _accountFormState.value
        val balanceCents = parseCents(current.balance)
        val yieldRate = current.yieldRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        val error = when {
            current.name.trim().isBlank() -> "Informe o nome da conta."
            current.icon.trim().isBlank() -> "Escolha um Ã­cone."
            current.type.isBlank() -> "Escolha o tipo da conta."
            else -> null
        }
        if (error != null) {
            _accountFormState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _accountFormState.update { it.copy(saving = true, error = null) }
            repository.saveAccount(
                AccountDraft(
                    id = current.id,
                    name = current.name,
                    icon = current.icon,
                    type = current.type,
                    balanceCents = balanceCents,
                    yieldRate = yieldRate
                )
            )
            _accountFormState.value = AccountFormUiState(visible = false)
            autoPushLocalChange()
        }
    }

    fun deleteAccount(id: String) {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Excluir conta?", "SÃ³ Ã© possÃ­vel excluir contas sem lanÃ§amentos vinculados.") {
                viewModelScope.launch {
                    when (val result = repository.deleteAccount(id)) {
                        DeleteResult.Deleted -> autoPushLocalChange()
                        is DeleteResult.Blocked -> _accountFormState.update {
                            it.copy(visible = true, error = result.reason, saving = false)
                        }
                    }
                }
            })
        }
    }

    fun openAddBudget() {
        _budgetFormState.value = BudgetFormUiState(visible = true, month = YearMonth.now().toString())
    }

    fun openEditBudget(budget: BudgetUsage) {
        _budgetFormState.value = BudgetFormUiState(
            visible = true,
            mode = FormMode.Edit,
            id = budget.id,
            category = budget.category,
            limit = formatInputAmount(budget.limitCents),
            month = YearMonth.now().toString()
        )
    }

    fun closeBudgetForm() {
        _budgetFormState.update { it.copy(visible = false, saving = false, error = null) }
    }

    fun updateBudgetForm(transform: (BudgetFormUiState) -> BudgetFormUiState) {
        _budgetFormState.update { transform(it).copy(error = null) }
    }

    fun saveBudget() {
        val current = _budgetFormState.value
        val limitCents = parseCents(current.limit)
        val error = when {
            current.category.trim().isBlank() -> "Escolha uma categoria."
            limitCents <= 0 -> "Informe um limite vÃ¡lido."
            runCatching { YearMonth.parse(current.month) }.isFailure -> "Use um mÃªs no formato AAAA-MM."
            else -> null
        }
        if (error != null) {
            _budgetFormState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _budgetFormState.update { it.copy(saving = true, error = null) }
            repository.saveBudget(
                BudgetDraft(
                    id = current.id,
                    category = current.category,
                    limitCents = limitCents,
                    month = current.month
                )
            )
            _budgetFormState.value = BudgetFormUiState(visible = false)
            autoPushLocalChange()
        }
    }

    fun deleteBudget(id: String) {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Excluir orÃ§amento?", "O limite mensal desta categoria serÃ¡ removido.") {
                viewModelScope.launch {
                    repository.deleteBudget(id)
                    autoPushLocalChange()
                }
            })
        }
    }

    fun openAddGoal() {
        _goalFormState.value = GoalFormUiState(visible = true, deadline = LocalDate.now().plusMonths(6).toString())
    }

    fun openEditGoal(goal: Goal) {
        _goalFormState.value = GoalFormUiState(
            visible = true,
            mode = FormMode.Edit,
            id = goal.id,
            name = goal.name,
            icon = goal.icon,
            target = formatInputAmount(goal.targetCents),
            current = formatInputAmount(goal.currentCents),
            deadline = goal.deadline,
            description = goal.description,
            monthly = formatInputAmount(goal.monthlyCents)
        )
    }

    fun closeGoalForm() {
        _goalFormState.update { it.copy(visible = false, saving = false, error = null) }
    }

    fun updateGoalForm(transform: (GoalFormUiState) -> GoalFormUiState) {
        _goalFormState.update { transform(it).copy(error = null) }
    }

    fun saveGoal() {
        val current = _goalFormState.value
        val target = parseCents(current.target)
        val present = parseCents(current.current)
        val monthly = parseCents(current.monthly)
        val error = when {
            current.name.trim().isBlank() -> "Informe o nome da meta."
            target <= 0 -> "Informe o valor alvo."
            !isValidIsoDate(current.deadline) -> "Use uma data no formato AAAA-MM-DD."
            else -> null
        }
        if (error != null) {
            _goalFormState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _goalFormState.update { it.copy(saving = true, error = null) }
            repository.saveGoal(
                GoalDraft(
                    id = current.id,
                    name = current.name,
                    icon = current.icon,
                    targetCents = target,
                    currentCents = present,
                    deadline = current.deadline,
                    description = current.description,
                    monthlyCents = monthly
                )
            )
            _goalFormState.value = GoalFormUiState(visible = false)
            autoPushLocalChange()
        }
    }

    fun deleteGoal(id: String) {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Excluir meta?", "Esta meta serÃ¡ removida da v4 e do prÃ³ximo backup enviado.") {
                viewModelScope.launch {
                    repository.deleteGoal(id)
                    autoPushLocalChange()
                }
            })
        }
    }

    fun openAddShoppingItem(listId: String? = null) {
        val targetListId = listId ?: shopping.value.lists.firstOrNull()?.id.orEmpty()
        if (targetListId.isBlank()) {
            openAddShoppingList()
            return
        }
        _shoppingFormState.value = ShoppingFormUiState(visible = true, listId = targetListId)
    }

    fun openEditShoppingItem(item: ShoppingItem) {
        _shoppingFormState.value = ShoppingFormUiState(
            visible = true,
            mode = ShoppingFormMode.ItemEdit,
            id = item.id,
            listId = item.listId,
            name = item.name,
            qty = item.qty,
            category = item.category,
            bought = item.bought
        )
    }

    fun openAddShoppingList() {
        _shoppingFormState.value = ShoppingFormUiState(visible = true, mode = ShoppingFormMode.ListCreate, icon = "ðŸ›’")
    }

    fun openEditShoppingList(list: ShoppingList) {
        _shoppingFormState.value = ShoppingFormUiState(
            visible = true,
            mode = ShoppingFormMode.ListEdit,
            id = list.id,
            name = list.name,
            icon = list.icon,
            position = list.position
        )
    }

    fun closeShoppingForm() {
        _shoppingFormState.update { it.copy(visible = false, saving = false, error = null) }
    }

    fun updateShoppingForm(transform: (ShoppingFormUiState) -> ShoppingFormUiState) {
        _shoppingFormState.update { transform(it).copy(error = null) }
    }

    fun saveShoppingForm() {
        val current = _shoppingFormState.value
        val isList = current.mode == ShoppingFormMode.ListCreate || current.mode == ShoppingFormMode.ListEdit
        val error = when {
            current.name.trim().isBlank() -> if (isList) "Informe o nome da lista." else "Informe o nome do item."
            !isList && current.listId.isBlank() -> "Escolha uma lista."
            else -> null
        }
        if (error != null) {
            _shoppingFormState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _shoppingFormState.update { it.copy(saving = true, error = null) }
            if (isList) {
                repository.saveShoppingList(
                    ShoppingListDraft(
                        id = current.id,
                        name = current.name,
                        icon = current.icon,
                        position = current.position
                    )
                )
            } else {
                repository.saveShoppingItem(
                    ShoppingItemDraft(
                        id = current.id,
                        listId = current.listId,
                        name = current.name,
                        qty = current.qty,
                        category = current.category,
                        bought = current.bought
                    )
                )
            }
            _shoppingFormState.value = ShoppingFormUiState(visible = false)
            autoPushLocalChange()
        }
    }

    fun setShoppingItemBought(id: String, bought: Boolean) {
        viewModelScope.launch {
            repository.setShoppingItemBought(id, bought)
            autoPushLocalChange()
        }
    }

    fun deleteShoppingItem(id: String) {
        viewModelScope.launch {
            repository.deleteShoppingItem(id)
            autoPushLocalChange()
        }
    }

    fun deleteShoppingList(id: String) {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Excluir lista?", "A lista e seus itens serÃ£o removidos.") {
                viewModelScope.launch {
                    repository.deleteShoppingList(id)
                    autoPushLocalChange()
                }
            })
        }
    }

    fun importLegacyBackup(json: String) {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Importar backup?", "Os dados locais da v4 serÃ£o substituÃ­dos pelo arquivo selecionado.") {
                runImportLegacyBackup(json)
            })
        }
    }

    fun loginSync() {
        val current = _settingsState.value
        viewModelScope.launch {
            _settingsState.update { it.copy(syncing = true, message = null) }
            val result = runCatching {
                val config = repository.login(current.baseUrl, current.username, current.password)
                val imported = repository.pullRemoteToLocal(config)
                config to imported
            }
            _settingsState.update {
                result.fold(
                    onSuccess = { (config, imported) ->
                        it.copy(
                            syncing = false,
                            password = "",
                            apiKey = config.apiKey,
                            userName = config.userName,
                            message = "Conectado como ${config.userName}. Baixado: ${imported.transactions} lanÃ§amentos, ${imported.accounts} contas, ${imported.budgets} orÃ§amentos, ${imported.goals} metas."
                        )
                    },
                    onFailure = { error -> it.copy(syncing = false, message = "Falha no login: ${error.message}") }
                )
            }
        }
    }

    fun pushLocalToRemote() {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Enviar dados locais?", "A nuvem serÃ¡ substituÃ­da pelo conteÃºdo local da v4.") {
                viewModelScope.launch {
                    val current = _settingsState.value
                    _settingsState.update { s -> s.copy(syncing = true, message = null) }
                    val result = runCatching {
                        repository.pushLocalToRemote(current.toSyncConfig())
                    }
                    _settingsState.update { s ->
                        result.fold(
                            onSuccess = { s.copy(syncing = false, message = "Dados locais enviados para a API.") },
                            onFailure = { e -> s.copy(syncing = false, message = "Falha ao enviar: ${e.message}") }
                        )
                    }
                }
            })
        }
    }

    fun pullRemoteToLocal() {
        _settingsState.update {
            it.copy(confirm = ConfirmAction("Baixar dados da nuvem?", "Os dados locais da v4 serÃ£o substituÃ­dos pelo servidor.") {
                viewModelScope.launch {
                    val current = _settingsState.value
                    _settingsState.update { s -> s.copy(syncing = true, message = null) }
                    val result = runCatching {
                        repository.pullRemoteToLocal(current.toSyncConfig())
                    }
                    _settingsState.update { s ->
                        result.fold(
                            onSuccess = { imported ->
                                if (imported.error != null) s.copy(syncing = false, message = imported.error)
                                else s.copy(syncing = false, message = "Baixado: ${imported.transactions} lanÃ§amentos, ${imported.accounts} contas, ${imported.budgets} orÃ§amentos, ${imported.goals} metas, ${imported.shoppingItems} itens.")
                            },
                            onFailure = { e -> s.copy(syncing = false, message = "Falha ao baixar: ${e.message}") }
                        )
                    }
                }
            })
        }
    }

    fun disconnectSync() {
        viewModelScope.launch {
            repository.disconnectSync()
            _settingsState.update {
                it.copy(apiKey = "", userName = "", password = "", message = "Conta desconectada deste aparelho.")
            }
        }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _settingsState.update { it.copy(importing = true, message = null) }
            val result = runCatching { repository.exportBackupJson() }
            _settingsState.update { state ->
                result.fold(
                    onSuccess = { state.copy(importing = false, message = "Backup v4 pronto para salvar.") },
                    onFailure = { state.copy(importing = false, message = "Falha ao exportar: ${it.message}") }
                )
            }
            result.getOrNull()?.let(onReady)
        }
    }

    private suspend fun autoPushLocalChange() {
        val config = _settingsState.value.toSyncConfig()
        if (!config.connected) return
        _settingsState.update { it.copy(syncing = true, message = "Sincronizando alteraÃ§Ãµes...") }
        val result = runCatching { repository.pushLocalToRemote(config) }
        _settingsState.update { state ->
            result.fold(
                onSuccess = { state.copy(syncing = false, message = "AlteraÃ§Ãµes sincronizadas.") },
                onFailure = { error -> state.copy(syncing = false, message = "AlteraÃ§Ã£o salva localmente. Sync falhou: ${error.message}") }
            )
        }
    }

    fun updateSettings(transform: (SettingsUiState) -> SettingsUiState) {
        _settingsState.update { transform(it) }
    }

    fun updateAppPreferences(transform: (AppPreferences) -> AppPreferences) {
        viewModelScope.launch {
            repository.saveAppPreferences(transform(appPreferences.value))
            autoPushLocalChange()
        }
    }

    fun payDueItem(item: DueItem, date: String) {
        viewModelScope.launch {
            val accountId = item.accountId?.takeIf { it.isNotBlank() } ?: state.value.accounts.firstOrNull()?.id ?: return@launch
            repository.saveTransaction(
                TransactionDraft(
                    accountId = accountId,
                    type = TransactionType.Expense,
                    description = item.name,
                    category = item.category,
                    amountCents = item.amountCents,
                    date = date,
                    note = listOf(methodLabel(item.paymentMethod), item.paymentPlace).filter { it.isNotBlank() }.joinToString(" • "),
                    pending = false
                )
            )
            val key = date.take(7)
            repository.saveAppPreferences(
                appPreferences.value.copy(
                    dueItems = appPreferences.value.dueItems.map {
                        if (it.id == item.id && key !in it.paidKeys) it.copy(paidKeys = it.paidKeys + key) else it
                    }
                )
            )
            autoPushLocalChange()
        }
    }

    fun dismissConfirm() {
        _settingsState.update { it.copy(confirm = null) }
    }

    fun runConfirm() {
        val action = _settingsState.value.confirm ?: return
        _settingsState.update { it.copy(confirm = null) }
        action.onConfirm()
    }

    private fun runImportLegacyBackup(json: String) {
        viewModelScope.launch {
            _settingsState.update { it.copy(importing = true, message = null) }
            val result = runCatching { repository.importLegacyBackup(json) }.getOrElse { error ->
                _settingsState.update { it.copy(importing = false, message = "Falha ao importar: ${error.message}") }
                return@launch
            }
            _settingsState.update {
                if (result.error != null) it.copy(importing = false, message = result.error)
                else it.copy(importing = false, message = "Importado: ${result.transactions} lanÃ§amentos, ${result.accounts} contas, ${result.budgets} orÃ§amentos, ${result.goals} metas, ${result.shoppingItems} itens.")
            }
        }
    }

    private fun parseCents(value: String): Long {
        val raw = value.trim()
        val normalized = when {
            raw.contains(".") && raw.contains(",") -> raw.replace(".", "").replace(",", ".")
            raw.contains(",") -> raw.replace(".", "").replace(",", ".")
            raw.contains(".") -> {
                val decimalDigits = raw.substringAfterLast(".").length
                if (decimalDigits in 1..2) raw else raw.replace(".", "")
            }
            else -> raw
        }
        return normalized.toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, java.math.RoundingMode.HALF_UP)
            ?.toLong()
            ?: 0L
    }

    private fun methodLabel(method: String): String {
        return when (method) {
            "pix" -> "Pix"
            "boleto" -> "Boleto"
            "credit" -> "Cartao principal"
            "store_card" -> "Cartao proprio"
            "debit" -> "Debito automatico"
            "financing" -> "Crediario"
            else -> method
        }
    }

    private fun isValidIsoDate(value: String): Boolean {
        return runCatching { LocalDate.parse(value) }.isSuccess
    }

    private fun formatInputAmount(cents: Long): String {
        return "%.2f".format(cents / 100.0).replace(".", ",")
    }

    private fun parseTransactionText(text: String): ParsedTransaction? {
        val raw = text.trim()
        if (raw.isBlank()) return null
        val amountMatch = Regex("""\d+(?:[.,]\d{1,2})?""").find(raw) ?: return null
        val amountCents = parseCents(amountMatch.value)
        if (amountCents <= 0) return null
        val lower = raw.lowercase()
        val type = if (listOf("recebi", "receita", "salario", "salÃ¡rio", "freela", "ganhei", "entrada").any { it in lower }) {
            TransactionType.Income
        } else {
            TransactionType.Expense
        }
        val date = when {
            "ontem" in lower -> LocalDate.now().minusDays(1).toString()
            "amanha" in lower || "amanhÃ£" in lower -> LocalDate.now().plusDays(1).toString()
            else -> LocalDate.now().toString()
        }
        val pending = type == TransactionType.Expense && listOf("vence", "venc", "pagar", "a pagar", "amanha", "amanhÃ£").any { it in lower }
        val categories = categoryNames.value.ifEmpty {
            if (type == TransactionType.Income) listOf("Salario", "Freelance", "Investimentos", "Outros")
            else listOf("Alimentacao", "Casa", "Transporte", "Saude", "Educacao", "Lazer", "Outros")
        }
        val category = inferCategory(lower, type, categories)
        val description = raw
            .replace(amountMatch.value, "")
            .replace(Regex("""(?i)\b(gastei|paguei|comprei|recebi|hoje|ontem|amanh[Ã£a]|no|na|em|de|r\$)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { category }
        return ParsedTransaction(type, amountCents, description.replaceFirstChar { it.uppercase() }, category, date, pending)
    }

    private fun inferCategory(text: String, type: TransactionType, categories: List<String>): String {
        fun pick(vararg names: String): String? = categories.firstOrNull { category ->
            names.any { name -> category.lowercase().contains(name) }
        }
        if (type == TransactionType.Income) {
            return pick("sal", "freel", "invest") ?: categories.first()
        }
        val fallback = categories.firstOrNull { it.lowercase().contains("outro") } ?: categories.first()
        return when {
            listOf("mercado", "ifood", "restaurante", "lanche", "padaria", "comida", "delivery").any { it in text } -> pick("aliment", "mercado", "comida")
            listOf("uber", "99", "taxi", "gasolina", "combustivel", "Ã´nibus", "onibus").any { it in text } -> pick("transporte")
            listOf("aluguel", "luz", "agua", "internet", "condominio", "casa").any { it in text } -> pick("casa", "moradia")
            listOf("farmacia", "remedio", "medico", "saude", "saÃºde").any { it in text } -> pick("saude", "saÃºde")
            listOf("curso", "livro", "faculdade", "educacao", "educaÃ§Ã£o").any { it in text } -> pick("educ")
            listOf("cinema", "bar", "jogo", "show", "lazer").any { it in text } -> pick("lazer")
            else -> fallback
        } ?: fallback
    }
}

private data class ParsedTransaction(
    val type: TransactionType,
    val amountCents: Long,
    val description: String,
    val category: String,
    val date: String,
    val pending: Boolean
)

data class AddTransactionUiState(
    val visible: Boolean = false,
    val mode: AddTransactionMode = AddTransactionMode.Create,
    val id: String? = null,
    val type: TransactionType = TransactionType.Expense,
    val amount: String = "",
    val description: String = "",
    val category: String = "AlimentaÃ§Ã£o",
    val accountId: String = "",
    val date: String = LocalDate.now().toString(),
    val pending: Boolean = false,
    val quickText: String = "",
    val saving: Boolean = false,
    val error: String? = null
) {
    val categories: List<String> = when (type) {
        TransactionType.Income -> listOf("SalÃ¡rio", "Freelance", "Investimentos", "Outros")
        TransactionType.Expense -> listOf("AlimentaÃ§Ã£o", "Casa", "Transporte", "SaÃºde", "EducaÃ§Ã£o", "Lazer", "Outros")
    }

    fun withType(nextType: TransactionType): AddTransactionUiState {
        val nextCategory = when {
            category in categoriesFor(nextType) -> category
            else -> categoriesFor(nextType).first()
        }
        return copy(type = nextType, category = nextCategory, pending = pending && nextType == TransactionType.Expense)
    }

    private fun categoriesFor(type: TransactionType): List<String> {
        return when (type) {
            TransactionType.Income -> listOf("SalÃ¡rio", "Freelance", "Investimentos", "Outros")
            TransactionType.Expense -> listOf("AlimentaÃ§Ã£o", "Casa", "Transporte", "SaÃºde", "EducaÃ§Ã£o", "Lazer", "Outros")
        }
    }
}

enum class AddTransactionMode {
    Create,
    Edit
}

enum class AppScreen {
    Home,
    Transactions,
    Due,
    Accounts,
    Budgets,
    Goals,
    Shopping,
    Settings
}

data class TransactionFilterUiState(
    val month: YearMonth = YearMonth.now(),
    val category: String? = null,
    val type: TransactionType? = null
)

enum class FormMode {
    Create,
    Edit
}

data class AccountFormUiState(
    val visible: Boolean = false,
    val mode: FormMode = FormMode.Create,
    val id: String? = null,
    val name: String = "",
    val icon: String = "ðŸ¦",
    val type: String = "checking",
    val balance: String = "",
    val yieldRate: String = "",
    val saving: Boolean = false,
    val error: String? = null
) {
    val types: List<Pair<String, String>> = listOf(
        "checking" to "Corrente",
        "savings" to "PoupanÃ§a",
        "wallet" to "Carteira",
        "investment" to "Investimento"
    )
}

data class BudgetFormUiState(
    val visible: Boolean = false,
    val mode: FormMode = FormMode.Create,
    val id: String? = null,
    val category: String = "AlimentaÃ§Ã£o",
    val limit: String = "",
    val month: String = YearMonth.now().toString(),
    val saving: Boolean = false,
    val error: String? = null
) {
    val categories: List<String> = listOf("AlimentaÃ§Ã£o", "Casa", "Transporte", "SaÃºde", "EducaÃ§Ã£o", "Lazer", "Outros")
}

data class GoalFormUiState(
    val visible: Boolean = false,
    val mode: FormMode = FormMode.Create,
    val id: String? = null,
    val name: String = "",
    val icon: String = "ðŸŽ¯",
    val target: String = "",
    val current: String = "",
    val deadline: String = LocalDate.now().plusMonths(6).toString(),
    val description: String = "",
    val monthly: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

enum class ShoppingFormMode {
    ItemCreate,
    ItemEdit,
    ListCreate,
    ListEdit
}

data class ShoppingFormUiState(
    val visible: Boolean = false,
    val mode: ShoppingFormMode = ShoppingFormMode.ItemCreate,
    val id: String? = null,
    val listId: String = "",
    val name: String = "",
    val icon: String = "ðŸ›’",
    val qty: String = "",
    val category: String = "ðŸ›’ Geral",
    val bought: Boolean = false,
    val position: Int = 0,
    val saving: Boolean = false,
    val error: String? = null
)

data class SettingsUiState(
    val importing: Boolean = false,
    val syncing: Boolean = false,
    val baseUrl: String = DEFAULT_API_URL,
    val username: String = "",
    val password: String = "",
    val apiKey: String = "",
    val userName: String = "",
    val message: String? = null,
    val confirm: ConfirmAction? = null
) {
    val connected: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}

data class ConfirmAction(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

private fun Flow<SyncConfig>.mapToSettingsState(
    draft: StateFlow<SettingsUiState>
): Flow<SettingsUiState> {
    return combine(draft) { config, current ->
        current.copy(
            baseUrl = current.baseUrl.ifBlank { config.baseUrl },
            apiKey = config.apiKey.ifBlank { current.apiKey },
            userName = config.userName.ifBlank { current.userName }
        )
    }
}

private fun SettingsUiState.toSyncConfig(): SyncConfig {
    return SyncConfig(baseUrl = baseUrl, apiKey = apiKey, userName = userName)
}

private const val DEFAULT_API_URL = "https://finanza-api.onrender.com"

class HomeViewModelFactory(
    private val repository: FinanzaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository) as T
    }
}

