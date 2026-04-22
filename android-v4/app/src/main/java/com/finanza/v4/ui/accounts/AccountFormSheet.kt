package com.finanza.v4.ui.accounts

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
import com.finanza.v4.ui.home.AccountFormUiState
import com.finanza.v4.ui.home.FormMode
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaSurface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountFormSheet(
    state: AccountFormUiState,
    onDismiss: () -> Unit,
    onChange: ((AccountFormUiState) -> AccountFormUiState) -> Unit,
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
                title = if (state.mode == FormMode.Edit) "Editar conta" else "Nova conta",
                subtitle = "Defina nome, saldo inicial e o papel dessa conta na sua rotina",
                emoji = state.icon.ifBlank { "\uD83C\uDFE6" },
                color = FinanzaMint
            )
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.name,
                onValueChange = { value -> onChange { it.copy(name = value) } },
                label = "Nome da conta"
            )
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.icon,
                onValueChange = { value -> onChange { it.copy(icon = value) } },
                label = "Icone ou emoji"
            )
            FinanzaCard(radius = 22.dp, glowColor = FinanzaMint) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tipo de conta", style = MaterialTheme.typography.labelMedium, color = FinanzaMint)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.types.forEach { (id, label) ->
                            FinanzaChip(
                                text = label,
                                selected = state.type == id,
                                onClick = { onChange { it.copy(type = id) } },
                                color = FinanzaMint
                            )
                        }
                    }
                }
            }
            FinanzaTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.balance,
                onValueChange = { value -> onChange { it.copy(balance = value) } },
                label = "Saldo inicial",
                prefix = "R$",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (state.type == "investment") {
                FinanzaCard(radius = 22.dp, glowColor = FinanzaGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rendimento mensal", style = MaterialTheme.typography.labelMedium, color = FinanzaGreen)
                        FinanzaTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.yieldRate,
                            onValueChange = { value -> onChange { it.copy(yieldRate = value) } },
                            label = "Percentual estimado ao mes",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Text(
                            "Use a mesma logica da dashboard para projetar crescimento e reserva.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            FinanzaPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (state.saving) "Salvando..." else "Salvar conta agora",
                onClick = onSave,
                enabled = !state.saving
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
