package com.finanza.v4.ui.due

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanza.v4.data.repository.AppPreferences
import com.finanza.v4.data.repository.DueItem
import com.finanza.v4.domain.Account
import com.finanza.v4.ui.components.FinanzaGhostButton
import com.finanza.v4.ui.components.FinanzaListItem
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.theme.FinanzaAmber
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaPurple
import com.finanza.v4.ui.theme.FinanzaRed
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID

@Composable
fun DueScreen(
    preferences: AppPreferences,
    accounts: List<Account>,
    categories: List<String>,
    onPreferencesChange: ((AppPreferences) -> AppPreferences) -> Unit,
    onPay: (DueItem, String) -> Unit
) {
    var editing by remember { mutableStateOf<DueItem?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val occurrences = remember(preferences.dueItems) { dueOccurrences(preferences.dueItems) }
    val total = occurrences.sumOf { it.item.amountCents }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "Vencimentos",
                subtitle = "O que pagar, quando vence e onde resolver",
                trailing = { MetricPill("${occurrences.size}", FinanzaPurple) }
            )
        }
        item {
            FinanzaSection(
                title = "Proximos compromissos",
                subtitle = "${money(total)} nos proximos 45 dias",
                trailing = { FinanzaGhostButton(text = "+ Novo", onClick = { editing = null; showForm = true }, color = FinanzaPurple) }
            ) {
                if (occurrences.isEmpty()) {
                    Text("Nenhum vencimento cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(occurrences, key = { "${it.item.id}-${it.date}" }) { occurrence ->
            DueRow(
                occurrence = occurrence,
                account = accounts.find { it.id == occurrence.item.accountId },
                onEdit = { editing = occurrence.item; showForm = true },
                onDelete = {
                    onPreferencesChange { prefs -> prefs.copy(dueItems = prefs.dueItems.filterNot { it.id == occurrence.item.id }) }
                },
                onPay = { onPay(occurrence.item, occurrence.date) }
            )
        }
        if (showForm) {
            item {
                DueForm(
                    item = editing,
                    accounts = accounts,
                    categories = categories,
                    onCancel = { showForm = false },
                    onSave = { item ->
                        onPreferencesChange { prefs ->
                            val exists = prefs.dueItems.any { it.id == item.id }
                            prefs.copy(dueItems = if (exists) prefs.dueItems.map { if (it.id == item.id) item else it } else prefs.dueItems + item)
                        }
                        showForm = false
                    }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DueRow(
    occurrence: DueOccurrence,
    account: Account?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPay: () -> Unit
) {
    val item = occurrence.item
    val days = LocalDate.parse(occurrence.date).toEpochDay() - LocalDate.now().toEpochDay()
    val color = when {
        days < 0 -> FinanzaRed
        days <= 3 -> FinanzaAmber
        else -> FinanzaPurple
    }
    FinanzaListItem(
        emoji = "📌",
        title = item.name,
        subtitle = "${formatDate(occurrence.date)} • ${methodLabel(item.paymentMethod)}${item.paymentPlace.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}${account?.let { " • ${it.icon} ${it.name}" } ?: ""}",
        amount = money(item.amountCents),
        amountColor = color,
        badge = if (days < 0) "atrasado" else if (days == 0L) "hoje" else "em ${days}d",
        badgeColor = color,
        stripColor = color,
        onClick = onEdit,
        trailing = {
            Row {
                IconButton(onClick = onPay) { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = FinanzaGreen) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = null, tint = FinanzaRed) }
            }
        }
    )
}

@Composable
private fun DueForm(
    item: DueItem?,
    accounts: List<Account>,
    categories: List<String>,
    onCancel: () -> Unit,
    onSave: (DueItem) -> Unit
) {
    var name by remember(item?.id) { mutableStateOf(item?.name ?: "") }
    var amount by remember(item?.id) { mutableStateOf(item?.amountCents?.let { centsInput(it) } ?: "") }
    var date by remember(item?.id) { mutableStateOf(item?.nextDueDate ?: LocalDate.now().toString()) }
    var method by remember(item?.id) { mutableStateOf(item?.paymentMethod ?: "pix") }
    var place by remember(item?.id) { mutableStateOf(item?.paymentPlace ?: "") }
    var category by remember(item?.id) { mutableStateOf(item?.category ?: categories.firstOrNull().orEmpty().ifBlank { "A classificar" }) }
    var accountId by remember(item?.id) { mutableStateOf(item?.accountId ?: "") }
    var notes by remember(item?.id) { mutableStateOf(item?.notes ?: "") }
    FinanzaSection(title = if (item == null) "Novo vencimento" else "Editar vencimento", subtitle = "Data, forma e local de pagamento") {
        FinanzaTextField(value = name, onValueChange = { name = it }, label = "Nome", placeholder = "Internet, luz, crediario...")
        FinanzaTextField(value = amount, onValueChange = { amount = it }, label = "Valor previsto", prefix = "R$ ", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
        FinanzaTextField(value = date, onValueChange = { date = it }, label = "Proximo vencimento", placeholder = "2026-04-20")
        FinanzaTextField(value = place, onValueChange = { place = it }, label = "Onde pagar", placeholder = "App, site, loja, banco...")
        FinanzaTextField(value = notes, onValueChange = { notes = it }, label = "Observacao", placeholder = "Contrato, login, codigo...")
        ChipRow("Forma", listOf("pix" to "Pix", "boleto" to "Boleto", "credit" to "Cartao", "store_card" to "Loja", "debit" to "Debito", "financing" to "Crediario"), method) { method = it }
        ChipRow("Categoria", categories.ifEmpty { listOf("A classificar") }.map { it to it }, category) { category = it }
        ChipRow("Conta/cartao", listOf("" to "Sem conta") + accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanzaGhostButton(text = "Cancelar", onClick = onCancel, modifier = Modifier.weight(1f))
            FinanzaPrimaryButton(text = "Salvar", onClick = {
                val dueDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                onSave(
                    DueItem(
                        id = item?.id ?: UUID.randomUUID().toString(),
                        name = name.ifBlank { "Vencimento" },
                        amountCents = parseCents(amount),
                        category = category.ifBlank { "A classificar" },
                        nextDueDate = dueDate.toString(),
                        dueDay = dueDate.dayOfMonth,
                        paymentMethod = method,
                        paymentPlace = place,
                        accountId = accountId.ifBlank { null },
                        notes = notes,
                        paidKeys = item?.paidKeys ?: emptyList()
                    )
                )
            }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChipRow(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, text) ->
                MetricPill(text, if (selected == key) FinanzaGreen else Color.Gray, Modifier.clickable { onSelect(key) })
            }
        }
    }
}

data class DueOccurrence(val item: DueItem, val date: String)

private fun dueOccurrences(items: List<DueItem>): List<DueOccurrence> {
    val today = LocalDate.now()
    val until = today.plusDays(45)
    return items.filter { it.active }.flatMap { item ->
        if (item.recurrence == "once") {
            val date = runCatching { LocalDate.parse(item.nextDueDate) }.getOrDefault(today)
            val key = YearMonth.from(date).toString()
            if (date in today..until && key !in item.paidKeys) listOf(DueOccurrence(item, date.toString())) else emptyList()
        } else {
            (0..2).mapNotNull { offset ->
                val ym = YearMonth.from(today).plusMonths(offset.toLong())
                val date = ym.atDay(item.dueDay.coerceAtMost(ym.lengthOfMonth()))
                if (date in today..until && ym.toString() !in item.paidKeys) DueOccurrence(item, date.toString()) else null
            }
        }
    }.sortedBy { it.date }
}

private fun methodLabel(method: String) = when (method) {
    "pix" -> "Pix"
    "boleto" -> "Boleto"
    "credit" -> "Cartao principal"
    "store_card" -> "Cartao proprio"
    "debit" -> "Debito automatico"
    "financing" -> "Crediario"
    else -> method
}

private fun parseCents(value: String): Long {
    val normalized = value.replace(".", "").replace(",", ".")
    return normalized.toBigDecimalOrNull()?.movePointRight(2)?.toLong() ?: 0L
}

private fun centsInput(cents: Long) = String.format(Locale.US, "%.2f", cents / 100.0).replace(".", ",")

private fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("pt").setRegion("BR").build()).format(cents / 100.0)

private fun formatDate(date: String): String {
    return runCatching {
        val d = LocalDate.parse(date)
        "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)
    }.getOrDefault(date)
}
