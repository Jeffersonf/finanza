package com.finanza.v4.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanza.v4.domain.Account
import com.finanza.v4.domain.TransactionType
import com.finanza.v4.ui.components.FinanzaCard
import com.finanza.v4.ui.components.FinanzaChip
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaSheetHeader
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.components.FinanzaToggleRow
import com.finanza.v4.ui.home.AddTransactionMode
import com.finanza.v4.ui.home.AddTransactionUiState
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaRed
import com.finanza.v4.ui.theme.FinanzaSurface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    state: AddTransactionUiState,
    accounts: List<Account>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onChange: ((AddTransactionUiState) -> AddTransactionUiState) -> Unit,
    onParseQuickText: () -> Unit,
    onSave: () -> Unit
) {
    if (!state.visible) return

    val accent = if (state.type == TransactionType.Income) FinanzaGreen else FinanzaRed
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FinanzaSurface.copy(alpha = .98f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FinanzaSheetHeader(
                title = if (state.mode == AddTransactionMode.Edit) "Editar lancamento" else "Novo lancamento",
                subtitle = "Defina valor, categoria, conta e se esse gasto ainda esta pendente",
                emoji = if (state.type == TransactionType.Income) "\u2191" else "\u2193",
                color = accent
            )

            if (state.mode == AddTransactionMode.Create) {
                FinanzaCard(radius = 22.dp, glowColor = FinanzaMint) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Preenchimento rapido", style = MaterialTheme.typography.labelMedium, color = FinanzaMint)
                        FinanzaTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.quickText,
                            onValueChange = { value -> onChange { it.copy(quickText = value) } },
                            label = "Descreva do seu jeito",
                            placeholder = "mercado 38,90 hoje"
                        )
                        FinanzaPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Interpretar texto",
                            onClick = onParseQuickText,
                            enabled = state.quickText.isNotBlank()
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanzaChip(
                    modifier = Modifier.weight(1f),
                    text = "\u2193 Despesa",
                    selected = state.type == TransactionType.Expense,
                    onClick = { onChange { it.withType(TransactionType.Expense) } },
                    color = FinanzaRed
                )
                FinanzaChip(
                    modifier = Modifier.weight(1f),
                    text = "\u2191 Receita",
                    selected = state.type == TransactionType.Income,
                    onClick = { onChange { it.withType(TransactionType.Income) } },
                    color = FinanzaGreen
                )
            }

            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.amount,
                onValueChange = { amount -> onChange { it.copy(amount = amount) } },
                label = "Valor",
                prefix = "R$",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.description,
                onValueChange = { description -> onChange { it.copy(description = description) } },
                label = "Descricao"
            )

            FieldGroup(title = "Categoria", color = accent) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val availableCategories = categories.ifEmpty { state.categories }
                    availableCategories.forEach { category ->
                        FinanzaChip(
                            text = category,
                            selected = state.category == category,
                            onClick = { onChange { it.copy(category = category) } },
                            color = accent
                        )
                    }
                }
            }

            FieldGroup(title = "Conta de origem", color = FinanzaGreen) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        FinanzaChip(
                            text = "${account.icon} ${account.name}",
                            selected = state.accountId == account.id,
                            onClick = { onChange { it.copy(accountId = account.id) } },
                            color = FinanzaGreen
                        )
                    }
                }
            }

            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.date,
                onValueChange = { date -> onChange { it.copy(date = date) } },
                label = "Data do lancamento",
                supportingText = "Formato AAAA-MM-DD",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (state.type == TransactionType.Expense) {
                FinanzaToggleRow(
                    title = "Marcar como pendente",
                    subtitle = "Mantem o item no planejamento ate o pagamento",
                    checked = state.pending,
                    onCheckedChange = { pending -> onChange { it.copy(pending = pending) } },
                    color = FinanzaPurple
                )
            }

            state.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            FinanzaPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = when {
                    state.saving -> "Salvando..."
                    state.mode == AddTransactionMode.Edit -> "Salvar mudancas"
                    else -> "Salvar lancamento"
                },
                onClick = onSave,
                enabled = !state.saving
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun FieldGroup(
    title: String,
    color: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    FinanzaCard(radius = 22.dp, glowColor = color) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            content()
        }
    }
}
