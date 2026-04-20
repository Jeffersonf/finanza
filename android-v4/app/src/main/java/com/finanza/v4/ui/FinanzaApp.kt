package com.finanza.v4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanza.v4.ui.accounts.AccountFormSheet
import com.finanza.v4.ui.accounts.AccountsScreen
import com.finanza.v4.ui.budgets.BudgetFormSheet
import com.finanza.v4.ui.budgets.BudgetsScreen
import com.finanza.v4.ui.due.DueScreen
import com.finanza.v4.ui.components.FinanzaBackground
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
import com.finanza.v4.ui.transaction.AddTransactionSheet
import com.finanza.v4.ui.transaction.TransactionsScreen

@Composable
fun FinanzaApp(
    viewModel: HomeViewModel,
    onEnableReminders: () -> Unit
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

    FinanzaTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::openAddTransaction,
                    containerColor = FinanzaGreen,
                    contentColor = FinanzaBg,
                    shape = RoundedCornerShape(999.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp, pressedElevation = 5.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Novo lançamento")
                }
            },
            bottomBar = {
                FinanzaNavigationBar(
                    currentScreen = currentScreen,
                    onScreenChange = viewModel::setScreen
                )
            },
            containerColor = FinanzaBg
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                FinanzaBackground {
                    when (currentScreen) {
                        AppScreen.Home -> HomeScreen(
                            snapshot = state,
                            preferences = appPreferences,
                            goals = goals,
                            shopping = shopping,
                            onNavigate = viewModel::setScreen
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
private fun FinanzaNavigationBar(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit
) {
    val items = listOf(
        NavItem("Início", "📊", AppScreen.Home),
        NavItem("Gastos", "💸", AppScreen.Transactions),
        NavItem("Venc.", "📌", AppScreen.Due),
        NavItem("Orçam.", "🎯", AppScreen.Budgets),
        NavItem("Lista", "🛒", AppScreen.Shopping),
        NavItem("Metas", "🏆", AppScreen.Goals),
        NavItem("Contas", "🏦", AppScreen.Accounts),
        NavItem("Config", "⚙️", AppScreen.Settings)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, ambientColor = Color.Black.copy(alpha = .36f), spotColor = Color.Black.copy(alpha = .36f))
            .background(FinanzaSurface.copy(alpha = .90f))
            .border(1.dp, FinanzaBorder.copy(alpha = .80f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 4.dp, vertical = 5.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentScreen == item.screen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clickable { onScreenChange(item.screen) }
                        .padding(top = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = item.icon,
                        fontSize = if (selected) 25.sp else 22.sp,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = item.label,
                        color = if (selected) FinanzaGreen else FinanzaMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(if (selected) 4.dp else 0.dp)
                            .background(FinanzaGreen, RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: String,
    val screen: AppScreen
)
