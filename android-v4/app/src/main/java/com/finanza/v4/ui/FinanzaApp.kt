package com.finanza.v4.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanza.v4.ui.accounts.AccountFormSheet
import com.finanza.v4.ui.accounts.AccountsScreen
import com.finanza.v4.ui.budgets.BudgetFormSheet
import com.finanza.v4.ui.budgets.BudgetsScreen
import com.finanza.v4.ui.components.FinanzaBackground
import com.finanza.v4.ui.components.FinanzaCard
import com.finanza.v4.ui.components.FinanzaChip
import com.finanza.v4.ui.components.FinanzaGhostButton
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.components.CenteredIconBubble
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.due.DueScreen
import com.finanza.v4.ui.goals.GoalFormSheet
import com.finanza.v4.ui.goals.GoalsScreen
import com.finanza.v4.ui.home.AppScreen
import com.finanza.v4.ui.home.AppLockUiState
import com.finanza.v4.ui.home.HomeScreen
import com.finanza.v4.ui.home.HomeViewModel
import com.finanza.v4.ui.settings.SettingsScreen
import com.finanza.v4.ui.shopping.ShoppingFormSheet
import com.finanza.v4.ui.shopping.ShoppingScreen
import com.finanza.v4.ui.theme.FinanzaBg
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaBorder
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaMuted
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaRed
import com.finanza.v4.ui.theme.FinanzaSurface
import com.finanza.v4.ui.theme.FinanzaTheme
import com.finanza.v4.ui.theme.FinanzaText
import com.finanza.v4.ui.theme.Syne
import com.finanza.v4.ui.transaction.AddTransactionSheet
import com.finanza.v4.ui.transaction.TransactionsScreen
import kotlinx.coroutines.launch

@Composable
fun FinanzaApp(
    viewModel: HomeViewModel,
    onEnableReminders: () -> Unit,
    onRequestBiometricUnlock: () -> Unit,
    biometricAvailable: Boolean,
    onExitApp: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addTransactionState by viewModel.addTransactionState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val transactionFilters by viewModel.transactionFilters.collectAsStateWithLifecycle()
    val accountFormState by viewModel.accountFormState.collectAsStateWithLifecycle()
    val budgetFormState by viewModel.budgetFormState.collectAsStateWithLifecycle()
    val goalFormState by viewModel.goalFormState.collectAsStateWithLifecycle()
    val shoppingFormState by viewModel.shoppingFormState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val appLockState by viewModel.appLockState.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val categoryNames by viewModel.categoryNames.collectAsStateWithLifecycle()
    val shopping by viewModel.shopping.collectAsStateWithLifecycle()
    val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val navItems = remember {
        listOf(
            NavItem("Dashboard", "📊", AppScreen.Home, FinanzaGreen, "Visao geral"),
            NavItem("Gastos", "💸", AppScreen.Transactions, FinanzaRed, "Dia a dia"),
            NavItem("Vencimentos", "📌", AppScreen.Due, FinanzaPurple, "Contas futuras"),
            NavItem("Limites", "🎯", AppScreen.Budgets, FinanzaAmber, "Orcamentos"),
            NavItem("Compras", "🛒", AppScreen.Shopping, FinanzaMint, "Lista ativa"),
            NavItem("Metas", "🏆", AppScreen.Goals, FinanzaPurple, "Objetivos"),
            NavItem("Contas", "🏦", AppScreen.Accounts, FinanzaMint, "Saldo e rendimento"),
            NavItem("Ajustes", "⚙️", AppScreen.Settings, FinanzaGreen, "Preferencias")
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wideLayout = configuration.screenWidthDp >= 900
    var navExpanded by rememberSaveable { mutableStateOf(wideLayout) }
    var lastBackPressAt by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(wideLayout) {
        if (wideLayout) navExpanded = true
    }

    BackHandler {
        when {
            appLockState.locked -> Unit
            addTransactionState.visible -> viewModel.closeAddTransaction()
            accountFormState.visible -> viewModel.closeAccountForm()
            budgetFormState.visible -> viewModel.closeBudgetForm()
            goalFormState.visible -> viewModel.closeGoalForm()
            shoppingFormState.visible -> viewModel.closeShoppingForm()
            settingsState.confirm != null -> viewModel.dismissConfirm()
            currentScreen != AppScreen.Home -> {
                viewModel.setScreen(AppScreen.Home)
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Voltando para a dashboard",
                        duration = SnackbarDuration.Short
                    )
                }
            }

            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressAt < 2200L) {
                    onExitApp()
                } else {
                    lastBackPressAt = now
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = "Toque em voltar novamente para sair",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    FinanzaTheme(darkTheme = appPreferences.theme != "light") {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::openAddTransaction,
                    containerColor = FinanzaGreen,
                    contentColor = FinanzaBg,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.navigationBarsPadding(),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 10.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Novo lancamento")
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            containerColor = FinanzaBg
        ) { padding ->
            FinanzaBackground {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (wideLayout) {
                        FinanzaSidebar(
                            currentScreen = currentScreen,
                            items = navItems,
                            expanded = navExpanded,
                            onToggle = { navExpanded = !navExpanded },
                            onScreenChange = viewModel::setScreen,
                            onPrimaryAction = viewModel::openAddTransaction,
                            onSecondaryAction = viewModel::exportBackup
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (currentScreen) {
                            AppScreen.Home -> HomeScreen(
                                snapshot = state,
                                preferences = appPreferences,
                                goals = goals,
                                shopping = shopping,
                                onNavigate = viewModel::setScreen,
                                onAddTransaction = viewModel::openAddTransaction,
                                onAddAccount = viewModel::openAddAccount,
                                onAddBudget = viewModel::openAddBudget,
                                onAddGoal = viewModel::openAddGoal,
                                onAddShoppingItem = { viewModel.openAddShoppingItem() }
                            )

                            AppScreen.Transactions -> TransactionsScreen(
                                transactions = transactions,
                                filters = transactionFilters,
                                txView = appPreferences.txView,
                                categories = categoryNames,
                                onTxViewChange = { view -> viewModel.updateAppPreferences { it.copy(txView = view) } },
                                onMonthChange = viewModel::setTransactionMonth,
                                onCategoryChange = viewModel::setTransactionCategory,
                                onTypeChange = viewModel::setTransactionType,
                                onEdit = viewModel::openEditTransaction,
                                onDelete = viewModel::deleteTransaction,
                                onPaidChange = viewModel::markTransactionPaid
                            )

                            AppScreen.Due -> DueScreen(
                                preferences = appPreferences,
                                accounts = state.accounts,
                                categories = categoryNames,
                                onPreferencesChange = viewModel::updateAppPreferences,
                                onPay = viewModel::payDueItem
                            )

                            AppScreen.Accounts -> AccountsScreen(
                                accounts = state.accounts,
                                onAdd = viewModel::openAddAccount,
                                onEdit = viewModel::openEditAccount,
                                onDelete = viewModel::deleteAccount
                            )

                            AppScreen.Budgets -> BudgetsScreen(
                                budgets = state.budgetUsage,
                                onAdd = viewModel::openAddBudget,
                                onEdit = viewModel::openEditBudget,
                                onDelete = viewModel::deleteBudget
                            )

                            AppScreen.Goals -> GoalsScreen(
                                goals = goals,
                                onAdd = viewModel::openAddGoal,
                                onEdit = viewModel::openEditGoal,
                                onDelete = viewModel::deleteGoal
                            )

                            AppScreen.Shopping -> ShoppingScreen(
                                snapshot = shopping,
                                activeListId = appPreferences.activeList,
                                onAddItem = viewModel::openAddShoppingItem,
                                onEditItem = viewModel::openEditShoppingItem,
                                onToggleItem = viewModel::setShoppingItemBought,
                                onDeleteItem = viewModel::deleteShoppingItem,
                                onAddList = viewModel::openAddShoppingList,
                                onEditList = viewModel::openEditShoppingList,
                                onDeleteList = viewModel::deleteShoppingList
                            )

                            AppScreen.Settings -> SettingsScreen(
                                state = settingsState,
                                preferences = appPreferences,
                                shoppingLists = shopping.lists,
                                biometricAvailable = biometricAvailable,
                                onChange = viewModel::updateSettings,
                                onPreferencesChange = viewModel::updateAppPreferences,
                                onLogin = viewModel::loginSync,
                                onPush = viewModel::pushLocalToRemote,
                                onPull = viewModel::pullRemoteToLocal,
                                onDisconnect = viewModel::disconnectSync,
                                onAutoSyncChange = viewModel::setAutoSyncEnabled,
                                onExportBackup = viewModel::exportBackup,
                                onImportBackup = viewModel::importLegacyBackup,
                                onAppLockChange = viewModel::setAppLockEnabled,
                                onBiometricsChange = viewModel::setBiometricsEnabled,
                                onSavePin = viewModel::saveAppPin,
                                onClearPin = viewModel::clearAppPin,
                                onEnableReminders = onEnableReminders
                            )
                        }
                    }
                }
            }
        }

        if (!wideLayout) {
            FinanzaBottomNav(
                currentScreen = currentScreen,
                items = navItems,
                onScreenChange = viewModel::setScreen
            )
        }

        AddTransactionSheet(
            state = addTransactionState,
            accounts = state.accounts,
            categories = categoryNames,
            onDismiss = viewModel::closeAddTransaction,
            onChange = viewModel::updateAddTransaction,
            onParseQuickText = viewModel::parseQuickTransaction,
            onSave = viewModel::saveAddTransaction
        )
        AccountFormSheet(
            state = accountFormState,
            onDismiss = viewModel::closeAccountForm,
            onChange = viewModel::updateAccountForm,
            onSave = viewModel::saveAccount
        )
        BudgetFormSheet(
            state = budgetFormState,
            categories = categoryNames,
            onDismiss = viewModel::closeBudgetForm,
            onChange = viewModel::updateBudgetForm,
            onSave = viewModel::saveBudget
        )
        GoalFormSheet(
            state = goalFormState,
            onDismiss = viewModel::closeGoalForm,
            onChange = viewModel::updateGoalForm,
            onSave = viewModel::saveGoal
        )
        ShoppingFormSheet(
            state = shoppingFormState,
            onDismiss = viewModel::closeShoppingForm,
            onChange = viewModel::updateShoppingForm,
            onSave = viewModel::saveShoppingForm
        )

        settingsState.confirm?.let { confirm ->
            AlertDialog(
                onDismissRequest = viewModel::dismissConfirm,
                containerColor = FinanzaSurface,
                titleContentColor = FinanzaGreen,
                textContentColor = FinanzaMuted,
                title = { Text(confirm.title) },
                text = { Text(confirm.message) },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::runConfirm,
                        colors = ButtonDefaults.textButtonColors(contentColor = FinanzaGreen)
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::dismissConfirm,
                        colors = ButtonDefaults.textButtonColors(contentColor = FinanzaMuted)
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (appLockState.locked) {
            AppLockGate(
                state = appLockState,
                biometricAvailable = biometricAvailable,
                onUnlockWithPin = viewModel::unlockWithPin,
                onRequestBiometricUnlock = onRequestBiometricUnlock
            )
        }
    }
}

@Composable
private fun AppLockGate(
    state: AppLockUiState,
    biometricAvailable: Boolean,
    onUnlockWithPin: (String) -> Unit,
    onRequestBiometricUnlock: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanzaBg.copy(alpha = .96f)),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            FinanzaCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 32.dp,
                glowColor = FinanzaMint
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CenteredIconBubble(FinanzaMint) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = FinanzaMint
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        MetricPill("modo privado", FinanzaMint)
                    }
                    Text("Finanza bloqueado", color = FinanzaText, style = MaterialTheme.typography.displaySmall)
                    Text(
                        "Desbloqueie com biometria ou PIN local para acessar seus dados neste aparelho.",
                        color = FinanzaMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.hasPin) {
                            FinanzaChip(
                                text = "PIN local",
                                selected = true,
                                onClick = {},
                                color = FinanzaGreen
                            )
                        }
                        if (state.biometricsEnabled && biometricAvailable) {
                            FinanzaChip(
                                text = "Biometria",
                                selected = true,
                                onClick = {},
                                color = FinanzaPurple
                            )
                        }
                    }
                    if (state.hasPin) {
                        FinanzaTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = pin,
                            onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                            label = "PIN do app",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        FinanzaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Desbloquear com PIN",
                            onClick = { onUnlockWithPin(pin) },
                            enabled = pin.length >= 4
                        )
                    }
                    if (state.biometricsEnabled && biometricAvailable) {
                        FinanzaGhostButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Usar biometria do aparelho",
                            onClick = onRequestBiometricUnlock,
                            icon = Icons.Rounded.Fingerprint,
                            color = FinanzaMint
                        )
                    }
                    state.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun FinanzaSidebar(
    currentScreen: AppScreen,
    items: List<NavItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onScreenChange: (AppScreen) -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: ((String) -> Unit) -> Unit
) {
    val targetWidth by animateDpAsState(if (expanded) 220.dp else 78.dp, label = "sidebar-width")

    FinanzaCard(
        modifier = Modifier
            .width(targetWidth)
            .fillMaxHeight()
            .animateContentSize(),
        radius = 28.dp,
        padding = PaddingValues(10.dp),
        glowColor = FinanzaMint
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SidebarHeader(expanded = expanded, onToggle = onToggle)
            AnimatedVisibility(expanded) {
                Text(
                    text = "Menu",
                    color = FinanzaMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            items.forEach { item ->
                SidebarItem(
                    item = item,
                    selected = currentScreen == item.screen,
                    expanded = expanded,
                    onClick = { onScreenChange(item.screen) }
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinanzaCard(
                        radius = 22.dp,
                        padding = PaddingValues(14.dp),
                        glowColor = FinanzaGreen
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Atalhos fixos",
                                color = FinanzaMuted,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Use o menu como na versao web e mantenha o fluxo rapido aqui tambem.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FinanzaGhostButton(
                                    text = "+ Nova",
                                    onClick = onPrimaryAction,
                                    modifier = Modifier.weight(1f)
                                )
                                FinanzaGhostButton(
                                    text = "Backup",
                                    onClick = { onSecondaryAction {} },
                                    modifier = Modifier.weight(1f),
                                    color = FinanzaMint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarHeader(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, FinanzaBorder, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .16f), RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FinanzaGreen.copy(alpha = .12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.Menu,
                contentDescription = null,
                tint = FinanzaGreen
            )
        }
        AnimatedVisibility(expanded) {
            Column {
                Text(
                    "Finanza",
                    color = FinanzaText,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = Syne),
                    fontWeight = FontWeight.Bold
                )
                Text("Navegacao principal", color = FinanzaMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    item: NavItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = if (selected) item.accent.copy(alpha = .14f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) item.accent.copy(alpha = .22f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) item.accent.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .20f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.emoji,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.scale(if (selected) 1.05f else 1f)
            )
        }
        AnimatedVisibility(expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.label,
                        color = if (selected) FinanzaText else FinanzaMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = item.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (selected) item.accent else FinanzaMuted.copy(alpha = .6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val emoji: String,
    val screen: AppScreen,
    val accent: Color,
    val subtitle: String
)

@Composable
private fun FinanzaBottomNav(
    currentScreen: AppScreen,
    items: List<NavItem>,
    onScreenChange: (AppScreen) -> Unit
) {
    val compactItems = remember(items) {
        items.filter { it.screen in setOf(AppScreen.Home, AppScreen.Transactions, AppScreen.Due, AppScreen.Accounts, AppScreen.Settings) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        FinanzaCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            radius = 28.dp,
            padding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            glowColor = FinanzaMint
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                compactItems.forEach { item ->
                    val selected = currentScreen == item.screen
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onScreenChange(item.screen) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = item.emoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.scale(if (selected) 1.12f else 1f)
                        )
                        Text(
                            text = item.label,
                            color = if (selected) item.accent else FinanzaMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .width(if (selected) 18.dp else 4.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) item.accent else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
