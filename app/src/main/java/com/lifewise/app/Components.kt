@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

// ---------- Layout helpers ----------

@Composable
fun ScreenList(padding: PaddingValues, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// ---------- Cards ----------

@Composable
fun HeroCard(label: String, big: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                big,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(sub, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

data class BarPart(val label: String, val value: Double, val color: Color)

@Composable
fun StackedBar(parts: List<BarPart>) {
    val shown = parts.filter { it.value > 0 }.sortedByDescending { it.value }
    val sum = shown.sumOf { it.value }
    if (sum <= 0) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            shown.forEach { part ->
                Box(
                    Modifier
                        .weight(part.value.toFloat())
                        .fillMaxHeight()
                        .background(part.color)
                )
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            shown.forEach { part ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(part.color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${part.label} ${(part.value / sum * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}

// ---------- Rows ----------

@Composable
fun Avatar(emoji: String, color: Color) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

@Composable
fun ItemRow(
    emoji: String,
    color: Color,
    title: String,
    subtitle: String = "",
    subtitleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val base = Modifier.clip(RoundedCornerShape(20.dp))
    ListItem(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = { Avatar(emoji, color) },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = if (subtitle.isNotBlank()) {
            { Text(subtitle, color = subtitleColor) }
        } else {
            null
        },
        trailingContent = trailing
    )
}

/** Today's medication doses with a tick box each. */
@Composable
fun DosesCard(meds: List<Medication>, taken: Set<String>, onToggle: (String) -> Unit) {
    val doses = meds
        .flatMap { m -> m.times.map { t -> Triple(m, t, doseKey(m.id, t)) } }
        .sortedBy { it.second }
    if (doses.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                "Medication today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            doses.forEach { (m, t, key) ->
                val done = key in taken
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(key) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = done, onCheckedChange = { onToggle(key) })
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${t.format(TimeFormat)} · ${m.name}",
                            textDecoration = if (done) TextDecoration.LineThrough else null
                        )
                        if (m.dose.isNotBlank()) {
                            Text(
                                m.dose,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Form pieces ----------

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun FormDialog(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = canSave) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${date.format(DateFormat)}")
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TimePicker(state = state)
            }
        }
    )
}

@Composable
fun TimeField(label: String, time: LocalTime, onChange: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${time.format(TimeFormat)}")
    }
    if (open) {
        TimeDialog(
            initial = time,
            onConfirm = { onChange(it); open = false },
            onDismiss = { open = false }
        )
    }
}
