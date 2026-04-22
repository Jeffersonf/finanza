package com.finanza.v4.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaSheetHeader
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.components.FinanzaToggleRow
import com.finanza.v4.ui.home.ShoppingFormMode
import com.finanza.v4.ui.home.ShoppingFormUiState
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingFormSheet(
    state: ShoppingFormUiState,
    onDismiss: () -> Unit,
    onChange: ((ShoppingFormUiState) -> ShoppingFormUiState) -> Unit,
    onSave: () -> Unit
) {
    if (!state.visible) return
    val isList = state.mode == ShoppingFormMode.ListCreate || state.mode == ShoppingFormMode.ListEdit
    val title = when (state.mode) {
        ShoppingFormMode.ItemCreate -> "Novo item"
        ShoppingFormMode.ItemEdit -> "Editar item"
        ShoppingFormMode.ListCreate -> "Nova lista"
        ShoppingFormMode.ListEdit -> "Editar lista"
    }

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
                title = title,
                subtitle = if (isList) "Organize listas por rotina, lugar ou tipo de compra" else "Defina nome, quantidade e status do item",
                emoji = if (isList) state.icon.ifBlank { "\uD83D\uDED2" } else "\u25CB",
                color = if (isList) FinanzaGreen else FinanzaMint
            )
            if (isList) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinanzaTextField(
                        modifier = Modifier.weight(.42f),
                        value = state.icon,
                        onValueChange = { value -> onChange { it.copy(icon = value) } },
                        label = "Icone"
                    )
                    FinanzaTextField(
                        modifier = Modifier.weight(1f),
                        value = state.name,
                        onValueChange = { value -> onChange { it.copy(name = value) } },
                        label = "Nome da lista"
                    )
                }
            } else {
                FinanzaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = "Nome do item"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinanzaTextField(
                        modifier = Modifier.weight(.70f),
                        value = state.qty,
                        onValueChange = { value -> onChange { it.copy(qty = value) } },
                        label = "Quantidade"
                    )
                    FinanzaTextField(
                        modifier = Modifier.weight(1f),
                        value = state.category,
                        onValueChange = { value -> onChange { it.copy(category = value) } },
                        label = "Categoria"
                    )
                }
                FinanzaToggleRow(
                    title = "Comprado",
                    subtitle = "Marca o item como concluido e tira da pendencia",
                    checked = state.bought,
                    onCheckedChange = { bought -> onChange { it.copy(bought = bought) } },
                    color = FinanzaGreen
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FinanzaPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (state.saving) "Salvando..." else if (isList) "Salvar lista" else "Salvar item",
                onClick = onSave,
                enabled = !state.saving
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
