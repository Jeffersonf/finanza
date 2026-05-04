package com.finanza.v4.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item {
            PageHeader(
                title = "Limites",
                subtitle = "Veja onde o mês está no controle e onde já pede atenção",
                trailing = { MetricPill("${budgets.size}", FinanzaGreen) }
            )
        }
        if (budgets.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nenhum limite criado",
                    subtitle = "Defina tetos por categoria para acompanhar o ritmo do mes.",
                    icon = Icons.Rounded.Savings
                )
            }
        }
        item { BudgetSummary(budgets) }
        item {
            FinanzaSection(
                title = "Categorias monitoradas",
                subtitle = "Cada bloco segue o mesmo padrão visual de risco da versão web",
                trailing = { MetricPill("mensal", FinanzaAmber) }
            ) {}
        }
        items(budgets, key = { it.category }) { budget ->
            val remaining = budget.limitCents - budget.spentCents
            val alertColor = if (remaining < 0) FinanzaRed else FinanzaGreen
            FinanzaListItem(
                modifier = Modifier.clickable { onEdit(budget) },
                emoji = "\uD83C\uDFAF",
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
        item { AddItemCard(text = "Novo limite mensal", onClick = onAdd, color = FinanzaAmber) }
    }
}

@Composable
private fun BudgetSummary(budgets: List<BudgetUsage>) {
    val limit = budgets.sumOf { it.limitCents }
    val spent = budgets.sumOf { it.spentCents }
    val remaining = limit - spent
    val progress = if (limit <= 0L) 0 else ((spent.toDouble() / limit) * 100).toInt()
    val color = when {
        limit == 0L -> FinanzaAmber
        remaining < 0L -> FinanzaRed
        else -> FinanzaGreen
    }

    FinanzaSection(
        title = "Panorama do mês",
        subtitle = "Leitura rápida do consumo por categoria",
        trailing = { MetricPill("${progress}%", color) }
    ) {
        FinanzaListItem(
            emoji = "\uD83D\uDCCA",
            title = "Total planejado",
            subtitle = "${budgets.size} limite(s) ativos",
            amount = money(limit),
            amountColor = FinanzaGreen,
            iconColor = FinanzaGreen,
            stripColor = FinanzaGreen
        )
        FinanzaListItem(
            emoji = if (remaining >= 0L) "\uD83D\uDFE2" else "\uD83D\uDD34",
            title = if (remaining >= 0L) "Ainda disponível" else "Acima do limite",
            subtitle = "Gasto até agora: ${money(spent)}",
            amount = money(kotlin.math.abs(remaining)),
            amountColor = color,
            iconColor = color,
            stripColor = color
        )
    }
}

private fun money(cents: Long): String {
    return NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("pt").setRegion("BR").build()
    ).format(cents / 100.0)
}
