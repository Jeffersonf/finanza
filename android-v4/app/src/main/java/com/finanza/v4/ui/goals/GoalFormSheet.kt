package com.finanza.v4.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.finanza.v4.ui.components.FinanzaCard
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaSheetHeader
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.home.FormMode
import com.finanza.v4.ui.home.GoalFormUiState
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormSheet(
    state: GoalFormUiState,
    onDismiss: () -> Unit,
    onChange: ((GoalFormUiState) -> GoalFormUiState) -> Unit,
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
                title = if (state.mode == FormMode.Edit) "Editar meta" else "Nova meta",
                subtitle = "Defina valor alvo, prazo e o aporte que cabe no seu mes",
                emoji = state.icon.ifBlank { "\uD83C\uDFC6" },
                color = FinanzaPurple
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanzaTextField(
                    modifier = Modifier.weight(1f),
                    value = state.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = "Nome da meta"
                )
                FinanzaTextField(
                    modifier = Modifier.weight(.42f),
                    value = state.icon,
                    onValueChange = { value -> onChange { it.copy(icon = value) } },
                    label = "Icone"
                )
            }
            FinanzaCard(radius = 22.dp, glowColor = FinanzaPurple) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Planejamento financeiro", style = MaterialTheme.typography.labelMedium, color = FinanzaPurple)
                    FinanzaTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.target,
                        onValueChange = { value -> onChange { it.copy(target = value) } },
                        label = "Valor alvo",
                        prefix = "R$",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    FinanzaTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.current,
                        onValueChange = { value -> onChange { it.copy(current = value) } },
                        label = "Valor ja guardado",
                        prefix = "R$",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    FinanzaTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.monthly,
                        onValueChange = { value -> onChange { it.copy(monthly = value) } },
                        label = "Aporte mensal planejado",
                        prefix = "R$",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.deadline,
                onValueChange = { value -> onChange { it.copy(deadline = value) } },
                label = "Prazo final",
                supportingText = "Formato AAAA-MM-DD",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.description,
                onValueChange = { value -> onChange { it.copy(description = value) } },
                label = "Descricao da meta",
                singleLine = false
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FinanzaPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (state.saving) "Salvando..." else "Salvar meta agora",
                onClick = onSave,
                enabled = !state.saving
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
