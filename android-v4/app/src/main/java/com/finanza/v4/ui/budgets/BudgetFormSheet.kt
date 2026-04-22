package com.finanza.v4.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.finanza.v4.ui.components.FinanzaCard
import com.finanza.v4.ui.components.FinanzaChip
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaSheetHeader
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.home.BudgetFormUiState
import com.finanza.v4.ui.home.FormMode
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaSurface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetFormSheet(
    state: BudgetFormUiState,
    categories: List<String>,
    onDismiss: () -> Unit,
    onChange: ((BudgetFormUiState) -> BudgetFormUiState) -> Unit,
    onSave: () -> Unit
) {
    if (!state.visible) return

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FinanzaSheetHeader(
                title = if (state.mode == FormMode.Edit) "Editar limite" else "Novo limite",
                subtitle = "Escolha a categoria, o teto do mes e acompanhe o consumo",
                emoji = "\uD83C\uDFAF",
                color = FinanzaAmber
            )
            FinanzaCard(radius = 22.dp, glowColor = FinanzaAmber) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Categoria principal", style = MaterialTheme.typography.labelMedium, color = FinanzaAmber)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val availableCategories = categories.ifEmpty { state.categories }
                        availableCategories.forEach { category ->
                            FinanzaChip(
                                text = category,
                                selected = state.category == category,
                                onClick = { onChange { it.copy(category = category) } },
                                color = FinanzaAmber
                            )
                        }
                    }
                }
            }
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.limit,
                onValueChange = { value -> onChange { it.copy(limit = value) } },
                label = "Valor maximo do mes",
                prefix = "R$",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.month,
                onValueChange = { value -> onChange { it.copy(month = value) } },
                label = "Mes de referencia",
                supportingText = "Formato AAAA-MM",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            FinanzaPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (state.saving) "Salvando..." else "Salvar limite mensal",
                onClick = onSave,
                enabled = !state.saving
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
