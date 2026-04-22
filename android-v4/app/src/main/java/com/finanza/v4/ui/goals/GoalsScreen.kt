package com.finanza.v4.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanza.v4.domain.Goal
import com.finanza.v4.ui.components.AddItemCard
import com.finanza.v4.ui.components.EmptyStateCard
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaPurple
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GoalsScreen(
    goals: List<Goal>,
    onAdd: () -> Unit,
    onEdit: (Goal) -> Unit,
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
                title = "Metas",
                subtitle = "Objetivos com prazo, valor alvo e ritmo de aporte",
                trailing = { MetricPill("${goals.size}", FinanzaPurple) }
            )
        }
        if (goals.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nenhuma meta",
                    subtitle = "Crie uma meta para acompanhar progresso, prazo e valor ja guardado.",
                    icon = Icons.Rounded.Flag
                )
            }
        }
        item { GoalsSummary(goals) }
        items(goals, key = { it.id }) { goal ->
            GoalRow(goal, onEdit = onEdit, onDelete = onDelete)
        }
        item { AddItemCard(text = "Nova meta", onClick = onAdd, color = FinanzaPurple) }
    }
}

@Composable
private fun GoalsSummary(goals: List<Goal>) {
    val target = goals.sumOf { it.targetCents }
    val current = goals.sumOf { it.currentCents }
    val monthly = goals.sumOf { it.monthlyCents }
    val progress = if (target <= 0L) 0 else ((current.toDouble() / target) * 100).toInt()

    FinanzaSection(
        title = "Seu plano de reserva",
        subtitle = "Metas sincronizadas e prontas para acompanhar",
        trailing = { MetricPill("${progress}%", FinanzaPurple) }
    ) {
        FinanzaListItem(
            emoji = "\uD83C\uDFC1",
            title = "Progresso geral",
            subtitle = "${goals.size} meta(s) cadastrada(s)",
            amount = "${money(current)} / ${money(target)}",
            amountColor = FinanzaGreen,
            iconColor = FinanzaPurple,
            stripColor = FinanzaPurple
        )
        FinanzaListItem(
            emoji = "\uD83D\uDCC5",
            title = "Aporte mensal previsto",
            subtitle = "Soma dos aportes planejados",
            amount = money(monthly),
            amountColor = FinanzaPurple,
            iconColor = FinanzaPurple,
            stripColor = FinanzaPurple
        )
    }
}

@Composable
private fun GoalRow(
    goal: Goal,
    onEdit: (Goal) -> Unit,
    onDelete: (String) -> Unit
) {
    val progress = if (goal.targetCents <= 0) 0f else (goal.currentCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
    FinanzaListItem(
        emoji = goal.icon,
        title = goal.name,
        subtitle = "Prazo final ${goal.deadline}",
        amount = "${money(goal.currentCents)} / ${money(goal.targetCents)}",
        amountColor = FinanzaGreen,
        badge = "${(progress * 100).toInt()}%",
        badgeColor = FinanzaPurple,
        iconColor = FinanzaPurple,
        stripColor = FinanzaPurple,
        onClick = { onEdit(goal) },
        trailing = {
            IconButton(onClick = { onDelete(goal.id) }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 52.dp)
            .height(5.dp),
        color = FinanzaGreen,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun money(cents: Long): String {
    return NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("pt").setRegion("BR").build()
    ).format(cents / 100.0)
}
