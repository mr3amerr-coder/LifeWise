@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private fun parseAmount(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

// ---------- Payment ----------

private data class PaymentPreset(val name: String, val category: PayCategory, val auto: Boolean)

private val PaymentPresets = listOf(
    PaymentPreset("Electricity", PayCategory.BILLS, false),
    PaymentPreset("Water", PayCategory.BILLS, false),
    PaymentPreset("Internet", PayCategory.BILLS, false),
    PaymentPreset("Mobile", PayCategory.BILLS, false),
    PaymentPreset("Rent", PayCategory.RENT, false),
    PaymentPreset("Netflix", PayCategory.SUBSCRIPTION, true),
    PaymentPreset("Spotify", PayCategory.SUBSCRIPTION, true),
    PaymentPreset("Installment", PayCategory.INSTALLMENT, false),
    PaymentPreset("Insurance", PayCategory.INSURANCE, false),
    PaymentPreset("School fees", PayCategory.EDUCATION, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    initial: Payment?,
    currency: String,
    onSave: (Payment) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.let { trimNumber(it) } ?: "") }
    var cycle by remember { mutableStateOf(initial?.cycle ?: Cycle.MONTHLY) }
    var category by remember { mutableStateOf(initial?.category ?: PayCategory.BILLS) }
    var due by remember { mutableStateOf(initial?.due ?: LocalDate.now().plusDays(7)) }
    var autoPay by remember { mutableStateOf(initial?.autoPay ?: false) }
    val amount = parseAmount(amountText)

    FormDialog(
        title = if (initial == null) "New payment" else "Edit payment",
        canSave = name.isNotBlank() && amount != null && amount >= 0,
        onSave = {
            onSave(
                Payment(
                    id = initial?.id ?: System.currentTimeMillis(),
                    name = name.trim(),
                    amount = amount ?: 0.0,
                    cycle = cycle,
                    category = category,
                    due = due,
                    autoPay = autoPay && cycle != Cycle.ONCE,
                    paid = initial?.paid == true && cycle == Cycle.ONCE
                )
            )
        },
        onDismiss = onDismiss,
        onDelete = onDelete
    ) {
        if (initial == null) {
            ChipRow {
                PaymentPresets.forEach { preset ->
                    AssistChip(
                        onClick = {
                            name = preset.name
                            category = preset.category
                            autoPay = preset.auto
                        },
                        label = { Text(preset.name) }
                    )
                }
            }
        }
        Field("Name", name, { name = it })
        Field("Amount ($currency)", amountText, { amountText = it }, KeyboardType.Decimal)
        Text("Repeats", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            Cycle.entries.forEach { c ->
                FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.label) })
            }
        }
        Text("Type", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            PayCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text("${c.emoji} ${c.label}") }
                )
            }
        }
        DateField(if (cycle == Cycle.ONCE) "Due date" else "Next due", due) { due = it }
        if (cycle != Cycle.ONCE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = autoPay, onCheckedChange = { autoPay = it })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Charged automatically")
                    Text(
                        "Auto-pay items roll forward by themselves. Others stay due until you mark them paid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------- Appointment ----------

private val Specialties = listOf("General", "Dentist", "Eyes", "Skin", "Heart", "Kids", "Bones", "Lab tests")

@Composable
fun AppointmentDialog(
    initial: Appointment?,
    currency: String,
    onSave: (Appointment) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var doctor by remember { mutableStateOf(initial?.doctor ?: "") }
    var specialty by remember { mutableStateOf(initial?.specialty ?: "") }
    var date by remember { mutableStateOf(initial?.at?.toLocalDate() ?: LocalDate.now().plusDays(1)) }
    var time by remember { mutableStateOf(initial?.at?.toLocalTime() ?: LocalTime.of(17, 0)) }
    var place by remember { mutableStateOf(initial?.place ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var feeText by remember { mutableStateOf(initial?.fee?.takeIf { it > 0 }?.let { trimNumber(it) } ?: "") }
    val fee = if (feeText.isBlank()) 0.0 else parseAmount(feeText)

    FormDialog(
        title = if (initial == null) "New appointment" else "Edit appointment",
        canSave = doctor.isNotBlank() && fee != null && fee >= 0,
        onSave = {
            onSave(
                Appointment(
                    id = initial?.id ?: System.currentTimeMillis(),
                    doctor = doctor.trim(),
                    specialty = specialty.trim(),
                    at = LocalDateTime.of(date, time),
                    place = place.trim(),
                    note = note.trim(),
                    fee = fee ?: 0.0
                )
            )
        },
        onDismiss = onDismiss,
        onDelete = onDelete
    ) {
        Field("Doctor or clinic", doctor, { doctor = it })
        ChipRow {
            Specialties.forEach { s ->
                FilterChip(selected = specialty == s, onClick = { specialty = s }, label = { Text(s) })
            }
        }
        Field("Specialty", specialty, { specialty = it })
        DateField("Date", date) { date = it }
        TimeField("Time", time) { time = it }
        Field("Place", place, { place = it })
        Field("Fee ($currency, optional)", feeText, { feeText = it }, KeyboardType.Decimal)
        Field("Note", note, { note = it })
    }
}

// ---------- Medication ----------

@Composable
fun MedicationDialog(
    initial: Medication?,
    onSave: (Medication) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var dose by remember { mutableStateOf(initial?.dose ?: "") }
    var times by remember { mutableStateOf(initial?.times ?: listOf(LocalTime.of(8, 0))) }
    var addingTime by remember { mutableStateOf(false) }

    FormDialog(
        title = if (initial == null) "New medication" else "Edit medication",
        canSave = name.isNotBlank() && times.isNotEmpty(),
        onSave = {
            onSave(
                Medication(
                    id = initial?.id ?: System.currentTimeMillis(),
                    name = name.trim(),
                    dose = dose.trim(),
                    times = times.distinct().sorted()
                )
            )
        },
        onDismiss = onDismiss,
        onDelete = onDelete
    ) {
        Field("Medication name", name, { name = it })
        Field("Dose (e.g. 1 tablet after food)", dose, { dose = it })
        Text("Daily times (tap one to remove it)", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            times.forEach { t ->
                AssistChip(
                    onClick = { times = times - t },
                    label = { Text("${t.format(TimeFormat)}  ✕") }
                )
            }
        }
        OutlinedButton(onClick = { addingTime = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add a time")
        }
    }

    if (addingTime) {
        TimeDialog(
            initial = LocalTime.of(12, 0),
            onConfirm = {
                times = (times + it).distinct().sorted()
                addingTime = false
            },
            onDismiss = { addingTime = false }
        )
    }
}

// ---------- Food ----------

private data class ExpensePreset(val title: String, val type: FoodType)

private val ExpensePresets = listOf(
    ExpensePreset("Groceries", FoodType.GROCERIES),
    ExpensePreset("Lunch", FoodType.RESTAURANT),
    ExpensePreset("Dinner out", FoodType.RESTAURANT),
    ExpensePreset("Delivery", FoodType.DELIVERY),
    ExpensePreset("Coffee", FoodType.COFFEE)
)

@Composable
fun ExpenseDialog(
    initial: FoodExpense?,
    currency: String,
    onSave: (FoodExpense) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.let { trimNumber(it) } ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: FoodType.GROCERIES) }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    val amount = parseAmount(amountText)

    FormDialog(
        title = if (initial == null) "New food expense" else "Edit food expense",
        canSave = title.isNotBlank() && amount != null && amount >= 0,
        onSave = {
            onSave(
                FoodExpense(
                    id = initial?.id ?: System.currentTimeMillis(),
                    title = title.trim(),
                    amount = amount ?: 0.0,
                    type = type,
                    date = date
                )
            )
        },
        onDismiss = onDismiss,
        onDelete = onDelete
    ) {
        if (initial == null) {
            ChipRow {
                ExpensePresets.forEach { p ->
                    AssistChip(
                        onClick = { title = p.title; type = p.type },
                        label = { Text(p.title) }
                    )
                }
            }
        }
        Field("What did you buy?", title, { title = it })
        Field("Amount ($currency)", amountText, { amountText = it }, KeyboardType.Decimal)
        ChipRow {
            FoodType.entries.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text("${t.emoji} ${t.label}") }
                )
            }
        }
        DateField("Date", date) { date = it }
    }
}

@Composable
fun BudgetDialog(
    initial: Double,
    currency: String,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(if (initial > 0) trimNumber(initial) else "") }
    val value = if (text.isBlank()) 0.0 else parseAmount(text)

    FormDialog(
        title = "Monthly food budget",
        canSave = value != null && value >= 0,
        onSave = { onSave(value ?: 0.0) },
        onDismiss = onDismiss
    ) {
        Field("Budget ($currency)", text, { text = it }, KeyboardType.Decimal)
        Text(
            "Leave empty to remove the budget.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
