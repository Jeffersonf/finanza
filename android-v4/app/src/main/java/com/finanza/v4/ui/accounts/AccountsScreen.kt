package com.finanza.v4.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanza.v4.domain.Account
import com.finanza.v4.ui.components.AddItemCard
import com.finanza.v4.ui.components.EmptyStateCard
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
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
                title = "Contas",
                subtitle = "Corrente, poupança, carteira e investimentos",
                trailing = { MetricPill("${accounts.size}", FinanzaGreen) }
            )
        }
        if (accounts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nenhuma conta",
                    subtitle = "Crie uma conta para organizar seus saldos.",
                    icon = Icons.Rounded.AccountBalance
                )
            }
        }
        item { AccountSummary(accounts) }
        items(accounts, key = { it.id }) { account ->
            FinanzaListItem(
                emoji = account.icon,
                title = account.name,
                subtitle = account.typeLabel(),
                amount = money(account.balanceCents),
                amountColor = FinanzaGreen,
                badge = account.typeLabel(),
                badgeColor = FinanzaMint,
                iconColor = FinanzaMint,
                stripColor = FinanzaMint,
                onClick = { onEdit(account) },
                trailing = {
                    IconButton(onClick = { onDelete(account.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
        item { AddItemCard(text = "+ Nova conta", onClick = onAdd) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AccountSummary(accounts: List<Account>) {
    val total = accounts.sumOf { it.balanceCents }
    val investments = accounts.filter { it.type == "investment" }.sumOf { it.balanceCents }
    FinanzaSection(
        title = "Saldos",
        subtitle = "Base sincronizada com a v3",
        trailing = { MetricPill(money(total), FinanzaGreen) }
    ) {
        FinanzaListItem(
            emoji = "💰",
            title = "Patrimônio total",
            subtitle = "${accounts.size} conta(s) cadastrada(s)",
            amount = money(total),
            amountColor = FinanzaGreen,
            iconColor = FinanzaGreen,
            stripColor = FinanzaGreen
        )
        FinanzaListItem(
            emoji = "📈",
            title = "Investimentos",
            subtitle = "Contas com rendimento",
            amount = money(investments),
            amountColor = FinanzaMint,
            iconColor = FinanzaMint,
            stripColor = FinanzaMint
        )
    }
}

private fun Account.typeLabel(): String {
    return when (type) {
        "checking" -> "Corrente"
        "savings" -> "Poupança"
        "wallet" -> "Carteira"
        "investment" -> "Investimento"
        else -> "Conta ativa"
    }
}

private fun money(cents: Long): String {
    return NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("pt").setRegion("BR").build()
    ).format(cents / 100.0)
}
