package com.finanza.v4.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.finanza.v4.ui.theme.FinanzaBg
import com.finanza.v4.ui.theme.FinanzaBg2
import com.finanza.v4.ui.theme.FinanzaBorder
import com.finanza.v4.ui.theme.FinanzaGreen
import com.finanza.v4.ui.theme.FinanzaMint
import com.finanza.v4.ui.theme.FinanzaMuted
import com.finanza.v4.ui.theme.FinanzaText
import com.finanza.v4.ui.theme.FinanzaText2
import com.finanza.v4.ui.theme.FinanzaSurface
import com.finanza.v4.ui.theme.FinanzaSurface2

@Composable
fun FinanzaBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanzaBg)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(FinanzaGreen.copy(alpha = .105f), Color.Transparent),
                        center = Offset(540f, -130f),
                        radius = 900f
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(FinanzaMint.copy(alpha = .075f), Color.Transparent),
                        center = Offset(980f, 1480f),
                        radius = 760f
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFA78BFA).copy(alpha = .072f), Color.Transparent),
                        center = Offset(-80f, 1260f),
                        radius = 620f
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            FinanzaBg2.copy(alpha = .70f),
                            FinanzaBg.copy(alpha = .94f),
                            FinanzaBg
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun FinanzaCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    glowColor: Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(radius), ambientColor = Color.Black.copy(alpha = .24f), spotColor = Color.Black.copy(alpha = .32f)),
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = FinanzaSurface.copy(alpha = .86f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .09f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = .07f), Color.Transparent, FinanzaSurface2.copy(alpha = .10f))
                    )
                )
                .padding(padding)
        ) {
            if (glowColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = .20f), Color.Transparent),
                                center = Offset(300f, -70f),
                                radius = 250f
                            )
                        )
                )
            }
            content()
        }
    }
}

@Composable
fun FinanzaPanel(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FinanzaCard(modifier = modifier.fillMaxWidth(), radius = 24.dp) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
fun FinanzaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .shadow(14.dp, RoundedCornerShape(14.dp), ambientColor = FinanzaGreen.copy(alpha = .14f), spotColor = FinanzaGreen.copy(alpha = .22f))
            .background(
                if (enabled) Brush.linearGradient(listOf(FinanzaGreen, Color(0xFFA8E040)))
                else Brush.linearGradient(listOf(FinanzaSurface2, FinanzaSurface2)),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (enabled) FinanzaBg else FinanzaMuted)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, color = if (enabled) FinanzaBg else FinanzaMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FinanzaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = FinanzaGreen
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .border(1.dp, color.copy(alpha = if (enabled) .24f else .10f), RoundedCornerShape(14.dp))
            .background(FinanzaSurface2.copy(alpha = .74f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (enabled) color else FinanzaMuted)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, color = if (enabled) color else FinanzaMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FinanzaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        Text(label, color = FinanzaMuted, style = MaterialTheme.typography.labelMedium)
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FinanzaBorder, RoundedCornerShape(14.dp)),
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, color = FinanzaMuted) } },
            prefix = prefix?.let { { Text(it, color = FinanzaMuted, fontWeight = FontWeight.Bold) } },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FinanzaSurface2.copy(alpha = .78f),
                unfocusedContainerColor = FinanzaSurface2.copy(alpha = .58f),
                disabledContainerColor = FinanzaSurface2.copy(alpha = .38f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = FinanzaGreen,
                focusedTextColor = FinanzaText,
                unfocusedTextColor = FinanzaText,
                focusedLabelColor = FinanzaGreen,
                unfocusedLabelColor = FinanzaMuted
            )
        )
        if (supportingText != null) {
            Text(supportingText, color = FinanzaMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FinanzaChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FinanzaGreen
) {
    Text(
        text = text,
        modifier = modifier
            .border(1.dp, if (selected) color.copy(alpha = .32f) else FinanzaBorder, RoundedCornerShape(999.dp))
            .background(if (selected) color.copy(alpha = .17f) else FinanzaSurface2.copy(alpha = .64f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (selected) color else FinanzaMuted,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanzaChipRow(
    items: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FinanzaGreen
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FinanzaChip(text = item, selected = selected == item, onClick = { onSelect(item) }, color = color)
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    FinanzaCard(modifier = modifier.fillMaxWidth(), radius = 22.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CenteredIconBubble(FinanzaMint) {
                Icon(icon, contentDescription = null, tint = FinanzaMint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AddItemCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, FinanzaGreen.copy(alpha = .24f), RoundedCornerShape(18.dp))
            .background(FinanzaGreen.copy(alpha = .07f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = FinanzaGreen, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .border(1.dp, color.copy(alpha = .28f), RoundedCornerShape(999.dp))
            .background(color.copy(alpha = .10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun CenteredIconBubble(
    color: Color,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, color.copy(alpha = .18f), RoundedCornerShape(12.dp))
            .background(color.copy(alpha = .12f), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        content = content
    )
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displaySmall, color = FinanzaText)
            Text(subtitle, color = FinanzaMuted, style = MaterialTheme.typography.bodyMedium)
        }
        trailing?.invoke()
    }
}

@Composable
fun FinanzaSection(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    FinanzaCard(
        modifier = modifier.fillMaxWidth(),
        radius = 28.dp,
        padding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = FinanzaText)
                    if (subtitle != null) {
                        Text(subtitle, color = FinanzaMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
fun FinanzaSheetHeader(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color = FinanzaGreen,
    modifier: Modifier = Modifier
) {
    FinanzaCard(
        modifier = modifier.fillMaxWidth(),
        radius = 28.dp,
        padding = PaddingValues(16.dp),
        glowColor = color
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBubble(emoji = emoji, color = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = FinanzaText)
                Text(subtitle, color = FinanzaMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FinanzaToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FinanzaGreen
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = .055f), RoundedCornerShape(16.dp))
            .background(FinanzaSurface2.copy(alpha = .42f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = FinanzaText, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FinanzaMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FinanzaBg,
                checkedTrackColor = color,
                uncheckedThumbColor = FinanzaMuted,
                uncheckedTrackColor = FinanzaSurface2,
                uncheckedBorderColor = FinanzaBorder
            )
        )
    }
}

@Composable
fun EmojiBubble(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .border(1.dp, color.copy(alpha = .16f), RoundedCornerShape(12.dp))
            .background(color.copy(alpha = .12f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun FinanzaListItem(
    emoji: String,
    title: String,
    subtitle: String,
    amount: String? = null,
    amountColor: Color = FinanzaText,
    badge: String? = null,
    badgeColor: Color = FinanzaGreen,
    iconColor: Color = FinanzaGreen,
    stripColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = .045f), RoundedCornerShape(16.dp))
            .background(FinanzaSurface2.copy(alpha = .24f), RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (stripColor != null) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(42.dp)
                    .background(stripColor.copy(alpha = .72f), RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.width(9.dp))
        }
        EmojiBubble(emoji = emoji, color = iconColor)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = FinanzaText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge != null) {
                    MetricPill(text = badge, color = badgeColor)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    subtitle,
                    color = FinanzaMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (amount != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = amountColor, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(Modifier.width(6.dp))
        }
        trailing?.invoke()
    }
}

@Composable
fun PeriodPillBar(
    content: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, FinanzaBorder, RoundedCornerShape(999.dp))
            .background(FinanzaSurface.copy(alpha = .74f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        content = content
    )
}
