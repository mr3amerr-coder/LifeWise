@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class Section(val label: String, val emoji: String) {
    TODAY("Today", "🏠"),
    PAY("Payments", "💳"),
    HEALTH("Health", "🩺"),
    FOOD("Food", "🍽️")
}

/** Which add/edit dialog is open (null item = adding something new). */
sealed interface Dlg {
    data class Pay(val item: Payment?) : Dlg
    data class Appt(val item: Appointment?) : Dlg
    data class Med(val item: Medication?) : Dlg
    data class Exp(val item: FoodExpense?) : Dlg
    data object Budget : Dlg
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeWiseApp(vm: MainViewModel) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    val section = Section.entries[tabIndex]

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LifeWise", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { vm.cycleCurrency() }) { Text(vm.currency) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Section.entries.forEachIndexed { i, s ->
                    NavigationBarItem(
                        selected = tabIndex == i,
                        onClick = { tabIndex = i },
                        icon = { Text(s.emoji, fontSize = 20.sp) },
                        label = { Text(s.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            when (section) {
                Section.TODAY -> Unit
                Section.PAY -> ExtendedFloatingActionButton(
                    onClick = { dlg = Dlg.Pay(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add payment") }
                )
                Section.FOOD -> ExtendedFloatingActionButton(
                    onClick = { dlg = Dlg.Exp(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add expense") }
                )
                Section.HEALTH -> {
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        ExtendedFloatingActionButton(
                            onClick = { menu = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Add") }
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("🩺  Appointment") },
                                onClick = { menu = false; dlg = Dlg.Appt(null) }
                            )
                            DropdownMenuItem(
                                text = { Text("💊  Medication") },
                                onClick = { menu = false; dlg = Dlg.Med(null) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (section) {
            Section.TODAY -> TodayScreen(vm, padding)
            Section.PAY -> PaymentsScreen(
                payments = vm.payments,
                currency = vm.currency,
                padding = padding,
                onOpen = { dlg = Dlg.Pay(it) },
                onPaid = { vm.markPaid(it) }
            )
            Section.HEALTH -> HealthScreen(
                appointments = vm.appointments,
                medications = vm.medications,
                taken = vm.taken,
                currency = vm.currency,
                padding = padding,
                onAppointment = { dlg = Dlg.Appt(it) },
                onMedication = { dlg = Dlg.Med(it) },
                onToggleDose = { vm.toggleDose(it) }
            )
            Section.FOOD -> FoodScreen(
                vm = vm,
                padding = padding,
                onEdit = { dlg = Dlg.Exp(it) },
                onBudget = { dlg = Dlg.Budget }
            )
        }
    }

    when (val d = dlg) {
        null -> Unit
        is Dlg.Pay -> {
            val item = d.item
            PaymentDialog(
                initial = item,
                currency = vm.currency,
                onSave = { vm.savePayment(it); dlg = null },
                onDelete = if (item == null) null else ({ vm.deletePayment(item.id); dlg = null }),
                onDismiss = { dlg = null }
            )
        }
        is Dlg.Appt -> {
            val item = d.item
            AppointmentDialog(
                initial = item,
                currency = vm.currency,
                onSave = { vm.saveAppointment(it); dlg = null },
                onDelete = if (item == null) null else ({ vm.deleteAppointment(item.id); dlg = null }),
                onDismiss = { dlg = null }
            )
        }
        is Dlg.Med -> {
            val item = d.item
            MedicationDialog(
                initial = item,
                onSave = { vm.saveMedication(it); dlg = null },
                onDelete = if (item == null) null else ({ vm.deleteMedication(item.id); dlg = null }),
                onDismiss = { dlg = null }
            )
        }
        is Dlg.Exp -> {
            val item = d.item
            ExpenseDialog(
                initial = item,
                currency = vm.currency,
                onSave = { vm.saveExpense(it); dlg = null },
                onDelete = if (item == null) null else ({ vm.deleteExpense(item.id); dlg = null }),
                onDismiss = { dlg = null }
            )
        }
        Dlg.Budget -> BudgetDialog(
            initial = vm.foodBudget,
            currency = vm.currency,
            onSave = { vm.setBudget(it); dlg = null },
            onDismiss = { dlg = null }
        )
    }
}
