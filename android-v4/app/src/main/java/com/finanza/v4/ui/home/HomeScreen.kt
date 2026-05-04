package com.finanza.v4.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanza.v4.data.repository.AppPreferences
import com.finanza.v4.data.repository.BudgetUsage
import com.finanza.v4.data.repository.DashboardSnapshot
import com.finanza.v4.data.repository.FixedDashboardWidgetIds
import com.finanza.v4.data.repository.ShoppingSnapshot
import com.finanza.v4.domain.Goal
import com.finanza.v4.domain.Transaction
import com.finanza.v4.domain.TransactionType
import com.finanza.v4.ui.components.EmptyStateCard
import com.finanza.v4.ui.components.FinanzaCard
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.theme.DmSans
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaBorder
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaMuted
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaRed
import com.finanza.v4.ui.theme.FinanzaSurface2
import com.finanza.v4.ui.theme.FinanzaText
import com.finanza.v4.ui.theme.FinanzaText2
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun HomeScreen(
    snapshot: DashboardSnapshot,
    preferences: AppPreferences,
    goals: List<Goal>,
    shopping: ShoppingSnapshot,
    onNavigate: (AppScreen) -> Unit,
    onAddTransaction: () -> Unit,
    onAddAccount: () -> Unit,
    onAddBudget: () -> Unit,
    onAddGoal: () -> Unit,
    onAddShoppingItem: () -> Unit
) {
    val showIcons = preferences.showDashboardIcons
    val order = preferences.widgetOrder
        .ifEmpty { defaultHomeOrder() }
        .map(::normalizeHomeWidgetId)
        .distinct()
        .filter { it in homeWidgetIds }
        .ifEmpty { defaultHomeOrder() }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp)
    ) {
        order.forEach { widgetId ->
            when (widgetId) {
                "hero" -> item(key = "hero") {
                    Header(snapshot, preferences, onNavigate)
                }

                "quickadd" -> item(key = "quickadd") {
                    QuickAddSection(
                        onAddTransaction = onAddTransaction,
                        onAddAccount = onAddAccount,
                        onAddBudget = onAddBudget,
                        onAddGoal = onAddGoal,
                        onAddShoppingItem = onAddShoppingItem,
                        showIcons = showIcons
                    )
                }

                "cards" -> if (preferences.showWidget("cards")) {
                    item(key = "cards") {
                        SummarySection(snapshot, preferences, onNavigate, showIcons)
                    }
                }

                "insights" -> if (preferences.showWidget("insights")) {
                    item(key = "insights") {
                        InsightSection(snapshot, preferences, onNavigate, showIcons)
                    }
                }

                "budalerts" -> if (preferences.showWidget("budalerts")) {
                    item(key = "budalerts") {
                        BudgetAlertSection(snapshot.budgetUsage, onNavigate, showIcons)
                    }
                }

                "goals" -> if (preferences.showWidget("goals")) {
                    item(key = "goals") {
                        GoalsSection(goals, onNavigate, showIcons)
                    }
                }

                "shopping" -> if (preferences.showWidget("shopping")) {
                    item(key = "shopping") {
                        ShoppingSection(shopping, preferences.activeList, onNavigate, showIcons)
                    }
                }

                "accounts" -> if (preferences.showWidget("accounts")) {
                    item(key = "accounts") {
                        AccountsSection(snapshot, onNavigate, showIcons)
                    }
                }

                "budgets" -> if (preferences.showWidget("budgets")) {
                    item(key = "budgets") {
                        BudgetsSection(snapshot.budgetUsage, onNavigate, showIcons)
                    }
                }

                "recent" -> if (preferences.showWidget("recent")) {
                    item(key = "recent") {
                        RecentTransactionsSection(snapshot.recent, onNavigate, showIcons)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddSection(
    onAddTransaction: () -> Unit,
    onAddAccount: () -> Unit,
    onAddBudget: () -> Unit,
    onAddGoal: () -> Unit,
    onAddShoppingItem: () -> Unit,
    showIcons: Boolean
) {
    FinanzaSection(
        title = "Ações rápidas",
        subtitle = "Menos cliques para o que você faz toda hora",
        trailing = { MetricPill("ágil", FinanzaGreen) }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAddTile(
                modifier = Modifier.weight(1f),
                label = "Gasto",
                hint = "lançamento",
                icon = Icons.Rounded.Add,
                color = FinanzaGreen,
                showIcon = showIcons,
                onClick = onAddTransaction
            )
            QuickAddTile(
                modifier = Modifier.weight(1f),
                label = "Compra",
                hint = "lista ativa",
                icon = Icons.Rounded.ShoppingCart,
                color = FinanzaMint,
                showIcon = showIcons,
                onClick = onAddShoppingItem
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAddTile(
                modifier = Modifier.weight(1f),
                label = "Conta",
                hint = "saldo inicial",
                icon = Icons.Rounded.AccountBalanceWallet,
                color = FinanzaMint,
                showIcon = showIcons,
                onClick = onAddAccount
            )
            QuickAddTile(
                modifier = Modifier.weight(1f),
                label = "Limite",
                hint = "orçamento",
                icon = Icons.Rounded.PieChart,
                color = FinanzaAmber,
                showIcon = showIcons,
                onClick = onAddBudget
            )
            QuickAddTile(
                modifier = Modifier.weight(1f),
                label = "Meta",
                hint = "objetivo",
                icon = Icons.Rounded.Flag,
                color = FinanzaPurple,
                showIcon = showIcons,
                onClick = onAddGoal
            )
        }
    }
}

@Composable
private fun QuickAddTile(
    label: String,
    hint: String,
    icon: ImageVector,
    color: Color,
    showIcon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, color.copy(alpha = .18f), RoundedCornerShape(18.dp))
            .background(FinanzaSurface2.copy(alpha = .24f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (showIcon) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color.copy(alpha = .10f), RoundedCornerShape(11.dp))
                    .border(1.dp, FinanzaBorder, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
        }
        Column {
            Text(
                label,
                color = FinanzaText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Header(
    snapshot: DashboardSnapshot,
    preferences: AppPreferences,
    onNavigate: (AppScreen) -> Unit
) {
    val salary = preferences.monthlyIncomeCents
    val spent = snapshot.summary.expenseCents
    val remaining = salary - spent
    val today = LocalDate.now()
    val daysLeft = (YearMonth.from(today).lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)
    val safeDaily = if (salary > 0L) remaining / daysLeft else 0L
    val paceColor = when {
        salary <= 0L -> FinanzaAmber
        safeDaily >= 0L -> FinanzaGreen
        else -> FinanzaRed
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Dashboard",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "Centro do dia para lançar, revisar e decidir seus gastos",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            MetricPill(if (salary > 0L) "${daysLeft}d" else "renda", paceColor)
        }

        FinanzaCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .clickable { onNavigate(AppScreen.Settings) },
            radius = 30.dp,
            glowColor = paceColor
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricPill("Mês atual", FinanzaMint)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (salary > 0L) "ritmo diário" else "configure a renda",
                        color = FinanzaText2,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = if (salary > 0L) money(safeDaily) else "Defina sua renda",
                    color = paceColor,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = DmSans,
                        fontSize = 20.sp,
                        lineHeight = 24.sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (salary > 0L) {
                        "limite seguro por dia ate o fechamento do mes"
                    } else {
                        "toque para informar salário ou renda mensal"
                    },
                    color = FinanzaText2,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("Gasto ${money(spent)}", FinanzaRed, Modifier.weight(1f))
                    MetricPill("Sobra ${money(remaining)}", paceColor, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    snapshot: DashboardSnapshot,
    preferences: AppPreferences,
    onNavigate: (AppScreen) -> Unit,
    showIcons: Boolean
) {
    val salary = preferences.monthlyIncomeCents
    val spent = snapshot.summary.expenseCents
    val remaining = salary - spent

    FinanzaSection(
        title = "Resumo financeiro",
        subtitle = "Blocos rápidos do que entrou, saiu e falta pagar"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Salario",
                value = if (salary > 0L) money(salary) else "R$ 0,00",
                hint = "Renda base do mes",
                color = FinanzaGreen,
                emoji = "\uD83D\uDCB0",
                showIcon = showIcons,
                onClick = { onNavigate(AppScreen.Settings) }
            )
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Gasto",
                value = money(spent),
                hint = "Consumo do mês",
                color = FinanzaRed,
                emoji = "\u2B07",
                showIcon = showIcons,
                onClick = { onNavigate(AppScreen.Transactions) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Sobra",
                value = money(remaining),
                hint = "Salário menos gastos",
                color = if (remaining >= 0L) FinanzaMint else FinanzaRed,
                emoji = "\uD83D\uDCB9",
                showIcon = showIcons,
                onClick = { onNavigate(AppScreen.Transactions) }
            )
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "A pagar",
                value = money(snapshot.summary.futureCents),
                hint = "Vencimentos e contas",
                color = FinanzaPurple,
                emoji = "\uD83D\uDCCC",
                showIcon = showIcons,
                onClick = { onNavigate(AppScreen.Due) }
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    hint: String,
    color: Color,
    emoji: String,
    showIcon: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FinanzaCard(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        radius = 26.dp,
        padding = PaddingValues(15.dp),
        glowColor = color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showIcon) {
                    Text(emoji, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.weight(1f))
                MetricPill(label, color)
            }
            Text(
                value,
                color = color,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InsightSection(
    snapshot: DashboardSnapshot,
    preferences: AppPreferences,
    onNavigate: (AppScreen) -> Unit,
    showIcons: Boolean
) {
    val savings = snapshot.summary.incomeCents - snapshot.summary.expenseCents
    val savingsRate = if (snapshot.summary.incomeCents <= 0) 0 else {
        ((savings.toDouble() / snapshot.summary.incomeCents) * 100).toInt()
    }
    val projected = snapshot.summary.balanceCents + savings

    FinanzaSection(
        title = "Indicadores",
        subtitle = "Leitura rápida do desempenho do mês",
        trailing = { MetricPill("${savingsRate}%", if (savings >= 0) FinanzaGreen else FinanzaRed) },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Transactions) }
    ) {
        FinanzaListItem(
            emoji = "\uD83D\uDCC8",
            title = "Resultado do mes",
            subtitle = "Receitas menos despesas",
            amount = money(savings),
            amountColor = if (savings >= 0) FinanzaGreen else FinanzaRed,
            iconColor = if (savings >= 0) FinanzaGreen else FinanzaRed,
            showIcon = showIcons
        )
        FinanzaListItem(
            emoji = "\uD83D\uDCB9",
            title = "Taxa de economia",
            subtitle = "Quanto sobrou das entradas",
            amount = "${savingsRate}%",
            amountColor = if (savingsRate >= 0) FinanzaGreen else FinanzaRed,
            iconColor = FinanzaMint,
            showIcon = showIcons
        )
        FinanzaListItem(
            emoji = "\uD83D\uDD2D",
            title = "Projeção simples",
            subtitle = "Saldo atual somado ao resultado do mês",
            amount = money(projected),
            amountColor = if (projected >= snapshot.summary.balanceCents) FinanzaGreen else FinanzaAmber,
            iconColor = FinanzaPurple,
            showIcon = showIcons
        )
        if (preferences.monthlyIncomeCents <= 0L) {
            Text(
                "Defina uma renda mensal para o app calibrar melhor os indicadores.",
                color = FinanzaMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BudgetAlertSection(
    budgets: List<BudgetUsage>,
    onNavigate: (AppScreen) -> Unit,
    showIcons: Boolean
) {
    val criticalBudget = budgets.maxByOrNull { it.progress }
    val overBudget = criticalBudget?.takeIf { it.progress >= 1f }

    FinanzaSection(
        title = if (overBudget != null) "Alerta de limite" else "Limites do mês",
        subtitle = if (overBudget != null) "Categoria com gasto acima do previsto" else "Tudo sob controle por enquanto",
        trailing = {
            MetricPill(
                text = overBudget?.let { "${(it.progress * 100).toInt()}%" } ?: "ok",
                color = if (overBudget != null) FinanzaRed else FinanzaMint
            )
        },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Budgets) }
    ) {
        if (criticalBudget == null) {
            Text("Crie um orçamento para receber alertas e acompanhar limites.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@FinanzaSection
        }
        FinanzaListItem(
            emoji = if (overBudget != null) "\u26A0" else "\uD83C\uDFAF",
            title = criticalBudget.category,
            subtitle = if (overBudget != null) {
                "Excedido em ${money(criticalBudget.spentCents - criticalBudget.limitCents)}"
            } else {
                "Uso atual ${(criticalBudget.progress * 100).toInt()}%"
            },
            amount = "${money(criticalBudget.spentCents)} / ${money(criticalBudget.limitCents)}",
            amountColor = if (overBudget != null) FinanzaRed else FinanzaAmber,
            iconColor = if (overBudget != null) FinanzaRed else FinanzaAmber,
            showIcon = showIcons
        )
        LinearProgressIndicator(
            progress = { criticalBudget.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (overBudget != null) FinanzaRed else FinanzaAmber,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun GoalsSection(goals: List<Goal>, onNavigate: (AppScreen) -> Unit, showIcons: Boolean) {
    FinanzaSection(
        title = "Metas rápidas",
        subtitle = "${goals.size} meta(s)",
        trailing = { MetricPill("${goals.size}", FinanzaPurple) },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Goals) }
    ) {
        if (goals.isEmpty()) {
            Text("Suas metas aparecem aqui para acompanhamento rápido.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        goals.take(3).forEach { goal ->
            val progress = if (goal.targetCents <= 0) 0f else {
                (goal.currentCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
            }
            FinanzaListItem(
                emoji = goal.icon,
                title = goal.name,
                subtitle = "${(progress * 100).toInt()}% até ${goal.deadline}",
                amount = "${money(goal.currentCents)} / ${money(goal.targetCents)}",
                amountColor = FinanzaPurple,
                badge = "${(progress * 100).toInt()}%",
                badgeColor = FinanzaPurple,
                iconColor = FinanzaPurple,
                showIcon = showIcons
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = FinanzaPurple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ShoppingSection(
    snapshot: ShoppingSnapshot,
    activeListId: String?,
    onNavigate: (AppScreen) -> Unit,
    showIcons: Boolean
) {
    val list = snapshot.lists.firstOrNull { it.id == activeListId } ?: snapshot.lists.firstOrNull()
    val items = list?.let { selected ->
        snapshot.items.filter { it.listId == selected.id && !it.bought }
    }.orEmpty()

    FinanzaSection(
        title = "Lista de compras",
        subtitle = list?.let { "${it.icon} ${it.name}" } ?: "Nenhuma lista ativa",
        trailing = { MetricPill("${items.size}", FinanzaMint) },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Shopping) }
    ) {
        if (items.isEmpty()) {
            Text("Nada pendente na lista ativa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.take(4).forEach { item ->
            FinanzaListItem(
                emoji = "\u25CB",
                title = item.name,
                subtitle = item.category,
                amount = item.qty.ifBlank { null },
                amountColor = FinanzaMint,
                iconColor = FinanzaMint,
                showIcon = showIcons
            )
        }
    }
}

@Composable
private fun AccountsSection(snapshot: DashboardSnapshot, onNavigate: (AppScreen) -> Unit, showIcons: Boolean) {
    FinanzaSection(
        title = "Contas",
        subtitle = "Saldos principais",
        trailing = { MetricPill("${snapshot.accounts.size}", FinanzaMint) },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Accounts) }
    ) {
        if (snapshot.accounts.isEmpty()) {
            Text("As contas aparecerão aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        snapshot.accounts.forEach { account ->
            FinanzaListItem(
                emoji = account.icon,
                title = account.name,
                subtitle = account.type,
                amount = money(account.balanceCents),
                amountColor = FinanzaGreen,
                iconColor = FinanzaMint,
                showIcon = showIcons
            )
        }
    }
}

@Composable
private fun BudgetsSection(budgets: List<BudgetUsage>, onNavigate: (AppScreen) -> Unit, showIcons: Boolean) {
    FinanzaSection(
        title = "Orçamentos",
        subtitle = "Limites do mês",
        trailing = { MetricPill("${budgets.size}", FinanzaPurple) },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Budgets) }
    ) {
        if (budgets.isEmpty()) {
            Text("Os limites do mês aparecerão aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        budgets.forEach { budget ->
            FinanzaListItem(
                emoji = "\uD83C\uDFAF",
                title = budget.category,
                subtitle = "${(budget.progress * 100).toInt()}% usado",
                amount = "${money(budget.spentCents)} / ${money(budget.limitCents)}",
                amountColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                badge = "${(budget.progress * 100).toInt()}%",
                badgeColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                iconColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                showIcon = showIcons
            )
            LinearProgressIndicator(
                progress = { budget.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    recent: List<Transaction>,
    onNavigate: (AppScreen) -> Unit,
    showIcons: Boolean
) {
    if (recent.isEmpty()) {
        EmptyStateCard(
            title = "Nenhum lançamento",
            subtitle = "Use o botão + para começar o mês.",
            icon = Icons.Rounded.CreditCard
        )
        return
    }

    FinanzaSection(
        title = "Últimos lançamentos",
        subtitle = "${recent.size} movimentações recentes",
        trailing = { MetricPill("ver tudo", FinanzaGreen) },
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onNavigate(AppScreen.Transactions) }
    ) {
        recent.take(6).forEach { tx ->
            TransactionRow(tx, showIcons)
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, showIcons: Boolean) {
    val accent = if (transaction.type == TransactionType.Income) FinanzaGreen else FinanzaRed
    FinanzaListItem(
        emoji = if (transaction.type == TransactionType.Income) "\u2B06" else "\u2B07",
        title = transaction.description,
        subtitle = transaction.date,
        amount = "${if (transaction.type == TransactionType.Income) "+" else "-"}${money(transaction.amountCents)}",
        amountColor = accent,
        badge = transaction.category,
        badgeColor = accent,
        iconColor = accent,
        showIcon = showIcons
    )
}

private fun money(cents: Long): String {
    val format = NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("pt").setRegion("BR").build()
    )
    return format.format(cents / 100.0)
}

private fun AppPreferences.showWidget(id: String): Boolean {
    if (id in FixedDashboardWidgetIds) return true
    return widgetPrefs[id] ?: true
}

private fun defaultHomeOrder(): List<String> {
    return listOf(
        "hero",
        "quickadd",
        "cards",
        "insights",
        "budalerts",
        "goals",
        "shopping",
        "accounts",
        "budgets",
        "recent"
    )
}

private fun normalizeHomeWidgetId(id: String): String {
    return when (id) {
        "projection", "weekly", "anomaly", "ministats", "saverate", "charts", "compare", "barcats" -> "insights"
        else -> id
    }
}

private val homeWidgetIds = setOf(
    "hero",
    "quickadd",
    "cards",
    "insights",
    "budalerts",
    "goals",
    "shopping",
    "accounts",
    "budgets",
    "recent"
)
