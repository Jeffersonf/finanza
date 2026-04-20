package com.finanza.v4.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanza.v4.data.repository.BudgetUsage
import com.finanza.v4.ui.components.AddItemCard
import com.finanza.v4.ui.components.EmptyStateCard
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetsScreen(
    budgets: List<BudgetUsage>,
    onAdd: () -> Unit,
    onEdit: (BudgetUsage) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "Orçamentos",
                subtitle = "Limites mensais por categoria",
                trailing = { MetricPill("${budgets.size}", FinanzaGreen) }
            )
        }
        if (budgets.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nenhum orçamento",
                    subtitle = "Crie limites por categoria para acompanhar o mês.",
                    icon = Icons.Rounded.Savings
                )
            }
        }
        item { FinanzaSection(title = "Limites", subtitle = "Leitura rápida de consumo") {} }
        items(budgets, key = { it.category }) { budget ->
            val remaining = budget.limitCents - budget.spentCents
            val alertColor = if (remaining < 0) FinanzaRed else FinanzaGreen
            FinanzaListItem(
                modifier = Modifier.clickable { onEdit(budget) },
                emoji = "🎯",
                title = budget.category,
                subtitle = if (remaining >= 0) "Restam ${money(remaining)}" else "Excedido em ${money(kotlin.math.abs(remaining))}",
                amount = "${money(budget.spentCents)} / ${money(budget.limitCents)}",
                amountColor = alertColor,
                badge = "${(budget.progress * 100).toInt()}%",
                badgeColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                iconColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                stripColor = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                onClick = { onEdit(budget) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onDelete(budget.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
            LinearProgressIndicator(
                progress = { budget.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 52.dp)
                    .height(5.dp),
                color = if (budget.progress >= 1f) FinanzaRed else FinanzaAmber,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        item {
            AddItemCard(text = "+ Novo orçamento", onClick = onAdd)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun money(cents: Long): String {
    return NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("pt").setRegion("BR").build()
    ).format(cents / 100.0)
}
