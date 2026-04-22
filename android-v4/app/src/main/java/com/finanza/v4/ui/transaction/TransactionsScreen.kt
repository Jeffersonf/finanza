package com.finanza.v4.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.v4.domain.Transaction
import com.finanza.v4.domain.TransactionType
import com.finanza.v4.ui.components.EmptyStateCard
import com.finanza.v4.ui.components.FinanzaChip
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.components.PeriodPillBar
import com.finanza.v4.ui.home.TransactionFilterUiState
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaRed
import com.finanza.v4.ui.theme.FinanzaSurface2
import com.finanza.v4.ui.theme.FinanzaText2
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    filters: TransactionFilterUiState,
    txView: String,
    categories: List<String>,
    onTxViewChange: (String) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onTypeChange: (TransactionType?) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onPaidChange: (String, Boolean) -> Unit
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
                title = "Transacoes",
                subtitle = viewLabel(txView),
                trailing = { MetricPill("${transactions.size}", FinanzaGreen) }
            )
        }
        item {
            FinanzaSection(title = "Filtros", subtitle = "Periodo, tipo e categoria") {
                PeriodPillBar(content = {
                    FinanzaChip(text = "Normal", selected = txView == "n", onClick = { onTxViewChange("n") }, color = FinanzaGreen)
                    FinanzaChip(text = "Compacto", selected = txView == "c", onClick = { onTxViewChange("c") }, color = FinanzaMint)
                    FinanzaChip(text = "Graficos", selected = txView == "chart", onClick = { onTxViewChange("chart") }, color = FinanzaAmber)
                })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FinanzaChip(text = "\u2039", selected = false, onClick = { onMonthChange(filters.month.minusMonths(1)) })
                    Text(
                        text = filters.month.month.getDisplayName(
                            TextStyle.FULL,
                            Locale.Builder().setLanguage("pt").setRegion("BR").build()
                        ).replaceFirstChar { it.uppercase() } + " ${filters.month.year}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FinanzaChip(text = "\u203A", selected = false, onClick = { onMonthChange(filters.month.plusMonths(1)) })
                }
                PeriodPillBar(content = {
                    FinanzaChip(text = "Tudo", selected = filters.type == null, onClick = { onTypeChange(null) })
                    FinanzaChip(text = "Despesas", selected = filters.type == TransactionType.Expense, onClick = { onTypeChange(TransactionType.Expense) }, color = FinanzaRed)
                    FinanzaChip(text = "Receitas", selected = filters.type == TransactionType.Income, onClick = { onTypeChange(TransactionType.Income) }, color = FinanzaGreen)
                })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filterCategories = listOf<String?>(null) + categories.ifEmpty { defaultCategories() }
                    filterCategories.distinct().forEach { category ->
                        FinanzaChip(
                            text = category ?: "Categorias",
                            selected = filters.category == category,
                            onClick = { onCategoryChange(category) }
                        )
                    }
                }
            }
        }
        item { TransactionSummary(transactions) }
        if (transactions.isNotEmpty()) {
            item {
                FinanzaSection(title = "Historico", subtitle = "${transactions.size} item(ns)") {}
            }
            when (txView) {
                "chart" -> item { CategoryChart(transactions) }
                "c" -> items(transactions, key = { it.id }) { tx ->
                    CompactTransactionRow(tx, onEdit, onDelete, onPaidChange)
                }
                else -> items(transactions, key = { it.id }) { tx ->
                    EditableTransactionRow(tx, onEdit, onDelete, onPaidChange)
                }
            }
        } else {
            item {
                EmptyStateCard(
                    title = "Nada encontrado",
                    subtitle = "Ajuste os filtros ou adicione um novo lancamento.",
                    icon = Icons.Rounded.SearchOff
                )
            }
        }
    }
}

@Composable
private fun TransactionSummary(transactions: List<Transaction>) {
    val income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amountCents }
    val expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amountCents }
    val pending = transactions.filter { it.pending }.sumOf { it.amountCents }
    FinanzaSection(title = "Resumo do periodo", subtitle = "Totais filtrados") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricPill("Receitas ${money(income)}", FinanzaGreen, Modifier.weight(1f))
            MetricPill("Despesas ${money(expense)}", FinanzaRed, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricPill("Saldo ${money(income - expense)}", if (income >= expense) FinanzaMint else FinanzaRed, Modifier.weight(1f))
            MetricPill("A pagar ${money(pending)}", FinanzaPurple, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EditableTransactionRow(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onPaidChange: (String, Boolean) -> Unit
) {
    val accent = if (transaction.type == TransactionType.Income) FinanzaGreen else FinanzaRed
    val glow = when {
        transaction.pending -> FinanzaPurple
        !transaction.paid && transaction.type == TransactionType.Expense -> FinanzaAmber
        else -> accent
    }
    FinanzaListItem(
        emoji = if (transaction.type == TransactionType.Income) "\u2B06" else "\u2B07",
        title = transaction.description,
        subtitle = "${transaction.date}${if (transaction.pending) " \u2022 a pagar" else ""}${if (transaction.paid) " \u2022 pago" else ""}",
        amount = "${if (transaction.type == TransactionType.Income) "+" else "-"}${money(transaction.amountCents)}",
        amountColor = accent,
        badge = transaction.category,
        badgeColor = glow,
        iconColor = glow,
        stripColor = glow,
        onClick = { onEdit(transaction) },
        trailing = {
            IconButton(onClick = { onEdit(transaction) }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar")
            }
            if (transaction.type == TransactionType.Expense) {
                IconButton(onClick = { onPaidChange(transaction.id, !transaction.paid) }) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Marcar pago",
                        tint = if (transaction.paid) FinanzaGreen else FinanzaPurple
                    )
                }
            }
            IconButton(onClick = { onDelete(transaction.id) }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = FinanzaRed)
            }
        }
    )
}

@Composable
private fun CompactTransactionRow(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onPaidChange: (String, Boolean) -> Unit
) {
    val accent = if (transaction.type == TransactionType.Income) FinanzaGreen else FinanzaRed
    FinanzaListItem(
        emoji = if (transaction.type == TransactionType.Income) "\u2B06" else "\u2B07",
        title = transaction.description,
        subtitle = "${transaction.category} \u2022 ${transaction.date}",
        amount = "${if (transaction.type == TransactionType.Income) "+" else "-"}${money(transaction.amountCents)}",
        amountColor = accent,
        iconColor = accent,
        stripColor = accent,
        onClick = { onEdit(transaction) },
        trailing = {
            if (transaction.type == TransactionType.Expense) {
                Text(
                    if (transaction.paid) "pago" else "pagar",
                    color = if (transaction.paid) FinanzaGreen else FinanzaPurple,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onPaidChange(transaction.id, !transaction.paid) }
                        .padding(8.dp)
                )
            }
            IconButton(onClick = { onDelete(transaction.id) }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = FinanzaRed)
            }
        }
    )
}

@Composable
private fun CategoryChart(transactions: List<Transaction>) {
    val expenses = transactions
        .filter { it.type == TransactionType.Expense }
        .groupBy { it.category.ifBlank { "Outros" } }
        .mapValues { entry -> entry.value.sumOf { it.amountCents } }
        .toList()
        .sortedByDescending { it.second }
        .take(8)
    val max = expenses.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1L
    FinanzaSection(
        title = "Gastos por categoria",
        subtitle = "Ranking visual do mes",
        trailing = { MetricPill("${expenses.size}", FinanzaPurple) }
    ) {
        if (expenses.isEmpty()) {
            Text("Sem despesas para montar grafico.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        expenses.forEachIndexed { index, (category, cents) ->
            val color = listOf(FinanzaRed, FinanzaAmber, FinanzaPurple, FinanzaMint, FinanzaGreen)[index % 5]
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category, modifier = Modifier.weight(1f), color = FinanzaText2, style = MaterialTheme.typography.bodyMedium)
                    Text(money(cents), color = color, style = MaterialTheme.typography.labelMedium)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(FinanzaSurface2.copy(alpha = .72f), RoundedCornerShape(999.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((cents.toFloat() / max.toFloat()).coerceIn(.04f, 1f))
                            .height(10.dp)
                            .background(color.copy(alpha = .82f), RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}

private fun viewLabel(txView: String): String {
    return when (txView) {
        "c" -> "Visual compacto"
        "chart" -> "Graficos do mes"
        else -> "Historico completo"
    }
}

private fun defaultCategories(): List<String> {
    return listOf("Alimentacao", "Casa", "Transporte", "Saude", "Educacao", "Lazer", "Salario", "Investimentos", "Outros")
}

private fun money(cents: Long): String {
    return NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("pt").setRegion("BR").build()
    ).format(cents / 100.0)
}
