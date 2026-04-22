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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanza.v4.ui.accounts.AccountFormSheet
import com.finanza.v4.ui.accounts.AccountsScreen
import com.finanza.v4.ui.budgets.BudgetFormSheet
import com.finanza.v4.ui.budgets.BudgetsScreen
import com.finanza.v4.ui.components.FinanzaBackground
import com.finanza.v4.ui.components.FinanzaCard
import com.finanza.v4.ui.due.DueScreen
import com.finanza.v4.ui.goals.GoalFormSheet
import com.finanza.v4.ui.goals.GoalsScreen
import com.finanza.v4.ui.home.AppScreen
import com.finanza.v4.ui.home.HomeScreen
import com.finanza.v4.ui.home.HomeViewModel
import com.finanza.v4.ui.settings.SettingsScreen
import com.finanza.v4.ui.shopping.ShoppingFormSheet
import com.finanza.v4.ui.shopping.ShoppingScreen
import com.finanza.v4.ui.theme.FinanzaBg
import com.finanza.v4.ui.theme.FinanzaBorder
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMuted
import com.finanza.v4.ui.theme.FinanzaSurface
import com.finanza.v4.ui.theme.FinanzaTheme
import com.finanza.v4.ui.theme.FinanzaText
import com.finanza.v4.ui.transaction.AddTransactionSheet
import com.finanza.v4.ui.transaction.TransactionsScreen
import kotlinx.coroutines.launch

@Composable
fun FinanzaApp(
    viewModel: HomeViewModel,
    onEnableReminders: () -> Unit,
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
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val categoryNames by viewModel.categoryNames.collectAsStateWithLifecycle()
    val shopping by viewModel.shopping.collectAsStateWithLifecycle()
    val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
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
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinanzaSidebar(
                        currentScreen = currentScreen,
                        expanded = navExpanded,
                        onToggle = { navExpanded = !navExpanded },
                        onScreenChange = { screen ->
                            viewModel.setScreen(screen)
                            if (!wideLayout) navExpanded = false
                        }
                    )

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
                                onChange = viewModel::updateSettings,
                                onPreferencesChange = viewModel::updateAppPreferences,
                                onLogin = viewModel::loginSync,
                                onPush = viewModel::pushLocalToRemote,
                                onPull = viewModel::pullRemoteToLocal,
                                onDisconnect = viewModel::disconnectSync,
                                onExportBackup = viewModel::exportBackup,
                                onImportBackup = viewModel::importLegacyBackup,
                                onEnableReminders = onEnableReminders
                            )
                        }
                    }
                }
            }
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
    }
}

@Composable
private fun FinanzaSidebar(
    currentScreen: AppScreen,
    expanded: Boolean,
    onToggle: () -> Unit,
    onScreenChange: (AppScreen) -> Unit
) {
    val items = listOf(
        NavItem("Dashboard", Icons.Rounded.Home, AppScreen.Home),
        NavItem("Gastos", Icons.Rounded.CreditCard, AppScreen.Transactions),
        NavItem("Vencimentos", Icons.Rounded.CalendarMonth, AppScreen.Due),
        NavItem("Limites", Icons.Rounded.PieChart, AppScreen.Budgets),
        NavItem("Compras", Icons.Rounded.ShoppingCart, AppScreen.Shopping),
        NavItem("Metas", Icons.Rounded.EmojiEvents, AppScreen.Goals),
        NavItem("Contas", Icons.Rounded.AccountBalanceWallet, AppScreen.Accounts),
        NavItem("Ajustes", Icons.Rounded.Settings, AppScreen.Settings)
    )
    val targetWidth by animateDpAsState(if (expanded) 220.dp else 78.dp, label = "sidebar-width")

    FinanzaCard(
        modifier = Modifier
            .width(targetWidth)
            .animateContentSize(),
        radius = 28.dp,
        padding = androidx.compose.foundation.layout.PaddingValues(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SidebarHeader(expanded = expanded, onToggle = onToggle)
            items.forEach { item ->
                SidebarItem(
                    item = item,
                    selected = currentScreen == item.screen,
                    expanded = expanded,
                    onClick = { onScreenChange(item.screen) }
                )
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
                Text("Finanza", color = FinanzaText, fontWeight = FontWeight.Bold)
                Text("Barra lateral", color = FinanzaMuted, style = MaterialTheme.typography.bodySmall)
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
                color = if (selected) FinanzaGreen.copy(alpha = .14f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) FinanzaGreen.copy(alpha = .18f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(20.dp),
            tint = if (selected) FinanzaGreen else FinanzaMuted
        )
        AnimatedVisibility(expanded) {
            Text(
                text = item.label,
                color = if (selected) FinanzaText else FinanzaMuted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: AppScreen
)
