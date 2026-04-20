package com.finanza.v4.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.v4.data.repository.ShoppingSnapshot
import com.finanza.v4.domain.ShoppingItem
import com.finanza.v4.domain.ShoppingList
import com.finanza.v4.ui.components.AddItemCard
import com.finanza.v4.ui.components.EmptyStateCard
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint

@Composable
fun ShoppingScreen(
    snapshot: ShoppingSnapshot,
    activeListId: String?,
    onAddItem: (String?) -> Unit,
    onEditItem: (ShoppingItem) -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    onDeleteItem: (String) -> Unit,
    onAddList: () -> Unit,
    onEditList: (ShoppingList) -> Unit,
    onDeleteList: (String) -> Unit
) {
    val pending = snapshot.items.count { !it.bought }
    val orderedLists = snapshot.lists.sortedWith(
        compareByDescending<ShoppingList> { activeListId != null && it.id == activeListId }
            .thenBy { it.position }
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "Lista",
                subtitle = "Compras sincronizadas",
                trailing = { MetricPill("$pending pendente(s)", FinanzaMint) }
            )
        }
        if (snapshot.items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Lista vazia",
                    subtitle = "Ao baixar da v3, seus itens aparecem aqui.",
                    icon = Icons.Rounded.ShoppingCart
                )
            }
        }
        orderedLists.forEach { list ->
            val items = snapshot.items.filter { it.listId == list.id }
            val isActive = activeListId != null && list.id == activeListId
            if (items.isNotEmpty()) {
                item {
                    FinanzaSection(
                        title = "${list.icon} ${list.name}",
                        subtitle = "${items.count { !it.bought }}/${items.size} pendentes${if (isActive) " • ativa" else ""}",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MetricPill("${items.count { !it.bought }}/${items.size}", FinanzaGreen)
                                IconButton(onClick = { onDeleteList(list.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir lista", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier.clickable { onEditList(list) }
                    ) {}
                }
                items(items, key = { it.id }) { item ->
                    ShoppingRow(
                        item = item,
                        onEdit = onEditItem,
                        onToggle = onToggleItem,
                        onDelete = onDeleteItem
                    )
                }
                item {
                    AddItemCard(text = "+ Item em ${list.name}", onClick = { onAddItem(list.id) })
                }
            } else {
                item {
                    FinanzaSection(
                        title = "${list.icon} ${list.name}",
                        subtitle = "Lista vazia${if (isActive) " • ativa" else ""}",
                        trailing = {
                            IconButton(onClick = { onDeleteList(list.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Excluir lista", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.clickable { onEditList(list) }
                    ) {
                        Text(
                            "Adicione o primeiro item.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                item {
                    AddItemCard(text = "+ Item em ${list.name}", onClick = { onAddItem(list.id) })
                }
            }
        }
        item {
            AddItemCard(text = "+ Nova lista", onClick = onAddList)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onEdit: (ShoppingItem) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    FinanzaListItem(
        emoji = if (item.bought) "✓" else "○",
        title = item.name,
        subtitle = item.category,
        amount = item.qty.ifBlank { null },
        amountColor = MaterialTheme.colorScheme.onSurfaceVariant,
        badge = if (item.bought) "comprado" else "pendente",
        badgeColor = if (item.bought) FinanzaGreen else FinanzaMint,
        iconColor = if (item.bought) FinanzaGreen else FinanzaMint,
        onClick = { onEdit(item) },
        trailing = {
            Text(
                if (item.bought) "↩" else "✓",
                color = if (item.bought) FinanzaGreen else FinanzaMint,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onToggle(item.id, !item.bought) }.padding(8.dp)
            )
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Excluir item", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}
