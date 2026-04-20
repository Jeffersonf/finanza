package com.finanza.v4.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanza.v4.data.repository.AppPreferences
import com.finanza.v4.data.repository.DashboardWidgets
import com.finanza.v4.data.repository.FixedDashboardWidgetIds
import com.finanza.v4.domain.ShoppingList
import com.finanza.v4.ui.components.FinanzaChip
import com.finanza.v4.ui.components.FinanzaGhostButton
import com.finanza.v4.ui.components.FinanzaPrimaryButton
import com.finanza.v4.ui.components.FinanzaSection
import com.finanza.v4.ui.components.FinanzaTextField
import com.finanza.v4.ui.components.MetricPill
import com.finanza.v4.ui.components.PageHeader
import com.finanza.v4.ui.home.SettingsUiState
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    preferences: AppPreferences,
    shoppingLists: List<ShoppingList>,
    onChange: ((SettingsUiState) -> SettingsUiState) -> Unit,
    onPreferencesChange: ((AppPreferences) -> AppPreferences) -> Unit,
    onLogin: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onDisconnect: () -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String) -> Unit,
    onEnableReminders: () -> Unit
) {
    val context = LocalContext.current
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var cdiText by remember(preferences.cdi) { mutableStateOf(formatRate(preferences.cdi)) }
    var selicText by remember(preferences.selic) { mutableStateOf(formatRate(preferences.selic)) }
    var monthlyIncomeText by remember(preferences.monthlyIncomeCents) { mutableStateOf(formatMoneyInput(preferences.monthlyIncomeCents)) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (json != null) onImportBackup(json)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "Ajustes",
                subtitle = "Conta, backup e sincronizacao",
                trailing = { MetricPill(if (state.connected) "online" else "local", if (state.connected) FinanzaGreen else FinanzaMint) }
            )
        }
        item {
            FinanzaSection("Preferencias da v3", "Tema, taxas, visualizacao e lista ativa sincronizados com a web.") {
                Text("Aparencia", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanzaChip(
                        text = "Glass escuro",
                        selected = preferences.theme == "dark",
                        onClick = { onPreferencesChange { it.copy(theme = "dark") } }
                    )
                    FinanzaChip(
                        text = "Claro",
                        selected = preferences.theme == "light",
                        onClick = { onPreferencesChange { it.copy(theme = "light") } },
                        color = FinanzaMint
                    )
                }
                FinanzaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = monthlyIncomeText,
                    onValueChange = { monthlyIncomeText = it },
                    label = "Salario/renda mensal base",
                    prefix = "R$",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                FinanzaGhostButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Salvar renda mensal",
                    onClick = {
                        val income = parseMoneyToCents(monthlyIncomeText).coerceAtLeast(0L)
                        onPreferencesChange { it.copy(monthlyIncomeCents = income) }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    FinanzaTextField(
                        modifier = Modifier.weight(1f),
                        value = cdiText,
                        onValueChange = { cdiText = it },
                        label = "CDI (%)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    FinanzaTextField(
                        modifier = Modifier.weight(1f),
                        value = selicText,
                        onValueChange = { selicText = it },
                        label = "Selic (%)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                FinanzaGhostButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Salvar taxas",
                    onClick = {
                        val cdi = parseRate(cdiText) ?: preferences.cdi
                        val selic = parseRate(selicText) ?: preferences.selic
                        onPreferencesChange { it.copy(cdi = cdi, selic = selic) }
                    }
                )
                Text("Visualizacao dos lancamentos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "n" to "Normal",
                        "c" to "Compacta",
                        "chart" to "Graficos"
                    ).forEach { (key, label) ->
                        FinanzaChip(
                            text = label,
                            selected = preferences.txView == key,
                            onClick = { onPreferencesChange { it.copy(txView = key) } }
                        )
                    }
                }
                Text("Lista ativa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanzaChip(
                        text = "Nenhuma",
                        selected = preferences.activeList.isNullOrBlank(),
                        onClick = { onPreferencesChange { it.copy(activeList = null) } },
                        color = FinanzaMint
                    )
                    shoppingLists.forEach { list ->
                        FinanzaChip(
                            text = "${list.icon} ${list.name}",
                            selected = preferences.activeList == list.id,
                            onClick = { onPreferencesChange { it.copy(activeList = list.id) } }
                        )
                    }
                }
            }
        }
        item {
            FinanzaSection("Widgets do dashboard", "Mesmos blocos configuraveis do app/site v3.") {
                val orderedWidgets = orderedWidgets(preferences)
                orderedWidgets.forEachIndexed { index, widget ->
                    val fixed = widget.id in FixedDashboardWidgetIds
                    val enabled = fixed || (preferences.widgetPrefs[widget.id] ?: widget.enabled)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FinanzaGhostButton(
                            modifier = Modifier.weight(1f),
                            text = "${if (fixed) "Fixo" else if (enabled) "Ativo" else "Oculto"}  ${widget.emoji} ${widget.label}",
                            onClick = {
                                if (!fixed) {
                                    onPreferencesChange {
                                        it.copy(widgetPrefs = it.widgetPrefs + (widget.id to !enabled))
                                    }
                                }
                            },
                            color = if (enabled) FinanzaGreen else FinanzaMint
                        )
                        FinanzaGhostButton(
                            modifier = Modifier.weight(.34f),
                            text = "Subir",
                            onClick = { onPreferencesChange { it.copy(widgetOrder = moveWidget(orderedWidgets.map { widget -> widget.id }, index, -1)) } },
                            enabled = !fixed && index > FixedDashboardWidgetIds.lastIndex
                        )
                        FinanzaGhostButton(
                            modifier = Modifier.weight(.38f),
                            text = "Descer",
                            onClick = { onPreferencesChange { it.copy(widgetOrder = moveWidget(orderedWidgets.map { widget -> widget.id }, index, 1)) } },
                            enabled = !fixed && index < orderedWidgets.lastIndex
                        )
                    }
                }
            }
        }
        item {
            FinanzaSection("Sincronizacao", "Conecte com a API existente do Finanza para enviar ou baixar dados.") {
                FinanzaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.baseUrl,
                    onValueChange = { value -> onChange { it.copy(baseUrl = value, message = null) } },
                    label = "URL da API",
                    placeholder = "https://finanza-api.onrender.com"
                )
                FinanzaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.username,
                    onValueChange = { value -> onChange { it.copy(username = value, message = null) } },
                    label = "Usuario"
                )
                FinanzaTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.password,
                    onValueChange = { value -> onChange { it.copy(password = value, message = null) } },
                    label = "Senha",
                    visualTransformation = PasswordVisualTransformation()
                )
                FinanzaPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = if (state.syncing) "Sincronizando..." else "Conectar",
                    onClick = onLogin,
                    enabled = !state.syncing && state.baseUrl.isNotBlank() && state.username.isNotBlank() && state.password.isNotBlank()
                )
                if (state.connected) {
                    Text("Conectado${state.userName.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}", color = MaterialTheme.colorScheme.primary)
                    FinanzaGhostButton(modifier = Modifier.fillMaxWidth(), text = "Enviar local para nuvem", onClick = onPush, enabled = !state.syncing)
                    FinanzaGhostButton(modifier = Modifier.fillMaxWidth(), text = "Baixar nuvem para este aparelho", onClick = onPull, enabled = !state.syncing)
                    FinanzaGhostButton(modifier = Modifier.fillMaxWidth(), text = "Desconectar", onClick = onDisconnect, enabled = !state.syncing, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            FinanzaSection("Backup local", "Exporte um JSON da v4 ou importe o backup gerado pela versao 3.9.1.") {
                FinanzaGhostButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Exportar backup v4",
                    onClick = {
                        onExportBackup { json ->
                            pendingExportJson = json
                            exportLauncher.launch("finanza-v4-backup.json")
                        }
                    },
                    enabled = !state.importing
                )
                FinanzaPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = if (state.importing) "Processando..." else "Importar backup 3.9.1",
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    enabled = !state.importing
                )
                state.message?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            FinanzaSection("Notificacoes", "Ative um lembrete diario para despesas pendentes e proximas do vencimento.") {
                FinanzaGhostButton(modifier = Modifier.fillMaxWidth(), text = "Ativar lembretes", onClick = onEnableReminders)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun parseRate(value: String): Double? {
    return value.trim().replace(",", ".").toDoubleOrNull()
}

private fun formatRate(value: Double): String {
    return String.format(Locale.US, "%.2f", value).replace(".", ",")
}

private fun parseMoneyToCents(value: String): Long {
    val raw = value.trim()
    val normalized = when {
        raw.contains(".") && raw.contains(",") -> raw.replace(".", "").replace(",", ".")
        raw.contains(",") -> raw.replace(".", "").replace(",", ".")
        raw.contains(".") -> {
            val decimalDigits = raw.substringAfterLast(".").length
            if (decimalDigits in 1..2) raw else raw.replace(".", "")
        }
        else -> raw
    }
    return normalized.toBigDecimalOrNull()
        ?.movePointRight(2)
        ?.setScale(0, java.math.RoundingMode.HALF_UP)
        ?.toLong()
        ?: 0L
}

private fun formatMoneyInput(cents: Long): String {
    if (cents <= 0L) return ""
    return String.format(Locale.US, "%.2f", cents / 100.0).replace(".", ",")
}

private fun orderedWidgets(preferences: AppPreferences) = preferences.widgetOrder
    .mapNotNull { id -> DashboardWidgets.firstOrNull { it.id == id } }
    .plus(DashboardWidgets.filter { widget -> widget.id !in preferences.widgetOrder })
    .let { widgets -> FixedDashboardWidgetIds.mapNotNull { id -> widgets.firstOrNull { it.id == id } } + widgets.filterNot { it.id in FixedDashboardWidgetIds } }

private fun moveWidget(order: List<String>, index: Int, delta: Int): List<String> {
    if (index <= FixedDashboardWidgetIds.lastIndex) return order
    val target = (index + delta).coerceIn(0, order.lastIndex)
    if (target <= FixedDashboardWidgetIds.lastIndex) return order
    if (target == index) return order
    return order.toMutableList().also { list ->
        val item = list.removeAt(index)
        list.add(target, item)
    }
}

