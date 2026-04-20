package com.finanza.v4.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaRed
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
    onNavigate: (AppScreen) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header(snapshot, preferences, onNavigate) }
        val order = preferences.widgetOrder.ifEmpty { defaultHomeOrder() }
        var insightsRendered = false
        order.forEach { widgetId ->
            when (widgetId) {
                "cards" -> if (preferences.showWidget("cards")) {
                    item { SummarySection(snapshot, preferences, onNavigate) }
                }
                "ministats", "saverate", "projection" -> if (!insightsRendered && preferences.showAnyWidget("ministats", "saverate", "projection")) {
                    insightsRendered = true
                    item(key = "insights") { InsightSection(snapshot, preferences, onNavigate) }
                }
                "budalerts" -> snapshot.budgetUsage.firstOrNull { it.progress >= 1f && preferences.showWidget("budalerts") }?.let { budget ->
                    item { BudgetAlertCard(budget) }
                }
                "goals" -> if (preferences.showWidget("goals")) {
                    item { GoalsSection(goals, onNavigate) }
                }
                "shopping" -> if (preferences.showWidget("shopping")) {
                    item { ShoppingSection(shopping, preferences.activeList, onNavigate) }
                }
                "accounts" -> if (preferences.showWidget("accounts")) {
                    item { AccountsSection(snapshot, onNavigate) }
                }
                "budgets" -> if (preferences.showWidget("budgets")) {
                    item { BudgetsSection(snapshot.budgetUsage, onNavigate) }
                }
                "recent" -> if (preferences.showWidget("recent")) {
                    item {
                        FinanzaSection(
                            title = "Ultimas transacoes",
                            subtitle = "${snapshot.recent.size} recentes",
                            modifier = Modifier.clickable { onNavigate(AppScreen.Transactions) }
                        ) {}
                    }
                    if (snapshot.recent.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Nenhum lancamento",
                                subtitle = "Use o botao + para comecar o mes.",
                                icon = Icons.Rounded.CreditCard
                            )
                        }
                    }
                    items(snapshot.recent, key = { it.id }) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(snapshot: DashboardSnapshot, preferences: AppPreferences, onNavigate: (AppScreen) -> Unit) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row {
                    Text(text = "fin", color = FinanzaGreen, style = MaterialTheme.typography.displaySmall)
                    Text(text = "anza", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displaySmall)
                }
                Text(
                    text = "Seu salario contra o gasto do dia a dia",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            MetricPill(if (salary > 0L) "${daysLeft}d" else "definir renda", paceColor)
        }
        FinanzaCard(
            modifier = Modifier.fillMaxWidth().clickable { onNavigate(AppScreen.Settings) },
            radius = 28.dp,
            glowColor = paceColor
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (salary > 0L) money(safeDaily) else "Defina sua renda",
                    color = paceColor,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (salary > 0L) "limite seguro por dia ate fechar o mes" else "toque para informar salario/renda mensal",
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
private fun SummarySection(snapshot: DashboardSnapshot, preferences: AppPreferences, onNavigate: (AppScreen) -> Unit) {
    val salary = preferences.monthlyIncomeCents
    val spent = snapshot.summary.expenseCents
    val remaining = salary - spent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Salario",
                value = if (salary > 0L) money(salary) else "R$ 0,00",
                hint = "Renda base do mes",
                color = FinanzaGreen,
                emoji = "💰",
                onClick = { onNavigate(AppScreen.Settings) }
            )
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Gasto",
                value = money(spent),
                hint = "Consumo do mes",
                color = FinanzaRed,
                emoji = "⬇",
                onClick = { onNavigate(AppScreen.Transactions) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "Sobra",
                value = money(remaining),
                hint = "Salario menos gastos",
                color = if (remaining >= 0L) FinanzaMint else FinanzaRed,
                emoji = "💹",
                onClick = { onNavigate(AppScreen.Transactions) }
            )
            SummaryTile(
                modifier = Modifier.weight(1f),
                label = "A pagar",
                value = money(snapshot.summary.futureCents),
                hint = "Vencimentos e contas",
                color = FinanzaPurple,
                emoji = "📌",
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
    color: androidx.compose.ui.graphics.Color,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FinanzaCard(
        modifier = modifier.clickable(onClick = onClick),
        radius = 28.dp,
        padding = PaddingValues(15.dp),
        glowColor = color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                MetricPill(label, color)
            }
            Text(
                value,
                color = color,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
private fun InsightSection(snapshot: DashboardSnapshot, preferences: AppPreferences, onNavigate: (AppScreen) -> Unit) {
    val savings = snapshot.summary.incomeCents - snapshot.summary.expenseCents
    val savingsRate = if (snapshot.summary.incomeCents <= 0) 0 else ((savings.toDouble() / snapshot.summary.incomeCents) * 100).toInt()
    val projected = snapshot.summary.balanceCents + savings
    FinanzaSection(
        title = "Indicadores",
        subtitle = "Leitura rapida do mes",
        trailing = { MetricPill("${savingsRate}%", if (savings >= 0) FinanzaGreen else FinanzaRed) }
        ,
        modifier = Modifier.clickable { onNavigate(AppScreen.Transactions) }
    ) {
        if (preferences.showWidget("ministats")) {
            FinanzaListItem(
                emoji = "ðŸ“ˆ",
                title = "Resultado do mes",
                subtitle = "Receitas menos despesas",
                amount = money(savings),
                amountColor = if (savings >= 0) FinanzaGreen else FinanzaRed,
                iconColor = if (savings >= 0) FinanzaGreen else FinanzaRed
            )
        }
        if (preferences.showWidget("saverate")) {
            FinanzaListItem(
                emoji = "ðŸ’¹",
                title = "Taxa de economia",
                subtitle = "Quanto sobrou das entradas",
                amount = "${savingsRate}%",
                amountColor = if (savingsRate >= 0) FinanzaGreen else FinanzaRed,
                iconColor = FinanzaMint
            )
        }
        if (preferences.showWidget("projection")) {
            FinanzaListItem(
                emoji = "ðŸ”­",
                title = "Projecao simples",
                subtitle = "Saldo atual + resultado do mes",
                amount = money(projected),
                amountColor = if (projected >= snapshot.summary.balanceCents) FinanzaGreen else FinanzaAmber,
                iconColor = FinanzaPurple
            )
        }
    }
}

@Composable
private fun BudgetAlertCard(budget: BudgetUsage) {
    FinanzaCard(radius = 24.dp, glowColor = FinanzaRed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("âš ", style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text("Orcamento estourado", fontWeight = FontWeight.Bold)
                Text(
                    "${budget.category}: excedido em ${money(budget.spentCents - budget.limitCents)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            MetricPill("${(budget.progress * 100).toInt()}%", FinanzaRed)
        }
    }
}

@Composable
private fun GoalsSection(goals: List<Goal>, onNavigate: (AppScreen) -> Unit) {
    FinanzaSection(
        title = "Metas rapidas",
        subtitle = "${goals.size} meta(s)",
        trailing = { MetricPill("${goals.size}", FinanzaPurple) },
        modifier = Modifier.clickable { onNavigate(AppScreen.Goals) }
    ) {
        if (goals.isEmpty()) {
            Text("Suas metas sincronizadas aparecem aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        goals.take(3).forEach { goal ->
            val progress = if (goal.targetCents <= 0) 0f else (goal.currentCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
            FinanzaListItem(
                emoji = goal.icon,
                title = goal.name,
                subtitle = "${(progress * 100).toInt()}% ate ${goal.deadline}",
                amount = "${money(goal.currentCents)} / ${money(goal.targetCents)}",
                amountColor = FinanzaPurple,
                badge = "${(progress * 100).toInt()}%",
                badgeColor = FinanzaPurple,
                iconColor = FinanzaPurple
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = FinanzaPurple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ShoppingSection(snapshot: ShoppingSnapshot, activeListId: String?, onNavigate: (AppScreen) -> Unit) {
    val list = snapshot.lists.firstOrNull { it.id == activeListId } ?: snapshot.lists.firstOrNull()
    val items = list?.let { selected -> snapshot.items.filter { it.listId == selected.id && !it.bought } }.orEmpty()
    FinanzaSection(
        title = "Lista de compras",
        subtitle = list?.let { "${it.icon} ${it.name}" } ?: "Nenhuma lista",
        trailing = { MetricPill("${items.size}", FinanzaMint) },
        modifier = Modifier.clickable { onNavigate(AppScreen.Shopping) }
    ) {
        if (items.isEmpty()) {
            Text("Nada pendente na lista ativa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.take(4).forEach { item ->
            FinanzaListItem(
                emoji = "â—‹",
                title = item.name,
                subtitle = item.category,
                amount = item.qty.ifBlank { null },
                amountColor = FinanzaMint,
                iconColor = FinanzaMint
            )
        }
    }
}

@Composable
private fun AccountsSection(snapshot: DashboardSnapshot, onNavigate: (AppScreen) -> Unit) {
    FinanzaSection(
        title = "Contas",
        subtitle = "Saldos principais",
        trailing = { MetricPill("${snapshot.accounts.size}", FinanzaMint) },
        modifier = Modifier.clickable { onNavigate(AppScreen.Accounts) }
    ) {
        if (snapshot.accounts.isEmpty()) {
            Text("As contas aparecerao aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        snapshot.accounts.forEach { account ->
            FinanzaListItem(
                emoji = account.icon,
                title = account.name,
                subtitle = account.type,
                amount = money(account.balanceCents),
                amountColor = FinanzaGreen,
                iconColor = FinanzaMint
            )
        }
    }
}

@Composable
private fun BudgetsSection(budgets: List<BudgetUsage>, onNavigate: (AppScreen) -> Unit) {
    FinanzaSection(
        title = "Orcamentos",
        subtitle = "Limites do mes",
        trailing = { MetricPill("${budgets.size}", FinanzaPurple) },
        modifier = Modifier.clickable { onNavigate(AppScreen.Budgets) }
    ) {
        if (budgets.isEmpty()) {
            Text("Os limites do mes aparecerao aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        budgets.forEach { budget ->
            FinanzaListItem(
                emoji = "ðŸŽ¯",
                title = budget.category,
                subtitle = "${(budget.progress * 100).toInt()}% usado",
                amount = "${money(budget.spentCents)} / ${money(budget.limitCents)}",
                amountColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                badge = "${(budget.progress * 100).toInt()}%",
                badgeColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                iconColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber
            )
            LinearProgressIndicator(
                progress = { budget.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val accent = if (transaction.type == TransactionType.Income) FinanzaGreen else FinanzaRed
    FinanzaListItem(
        emoji = if (transaction.type == TransactionType.Income) "â¬†" else "â¬‡",
        title = transaction.description,
        subtitle = transaction.date,
        amount = "${if (transaction.type == TransactionType.Income) "+" else "-"}${money(transaction.amountCents)}",
        amountColor = accent,
        badge = transaction.category,
        badgeColor = accent,
        iconColor = accent
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

private fun AppPreferences.showAnyWidget(vararg ids: String): Boolean {
    return ids.any { showWidget(it) }
}

private fun defaultHomeOrder(): List<String> {
    return FixedDashboardWidgetIds + listOf("ministats", "budalerts", "goals", "shopping", "accounts", "budgets", "recent")
}

