@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDateTime

@Composable
fun TodayScreen(vm: MainViewModel, padding: PaddingValues) {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val currency = vm.currency

    val recurring = vm.payments.sumOf { it.monthlyCost }
    val food = foodSpentThisMonth(vm.expenses, today)
    val insights = buildInsights(
        vm.payments, vm.appointments, vm.expenses, vm.foodBudget, currency, now
    )
    val agenda = buildAgenda(
        vm.payments, vm.appointments, vm.medications, vm.taken, currency, now
    )

    ScreenList(padding) {
        item {
            HeroCard(
                label = "Estimated spend this month",
                big = money(recurring + food, currency),
                sub = "Payments ${money(recurring, currency)} · Food ${money(food, currency)}"
            )
        }
        item { InfoCard("Smart insights", insights) }
        if (vm.medications.isNotEmpty()) {
            item { DosesCard(vm.medications, vm.taken, vm::toggleDose) }
        }
        item { SectionTitle("Next 7 days") }
        if (agenda.isEmpty()) {
            item { EmptyHint("Nothing coming up. Add payments, appointments or medication from the other tabs.") }
        } else {
            items(agenda) { a ->
                ItemRow(
                    emoji = a.emoji,
                    color = if (a.urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    title = a.title,
                    subtitle = a.subtitle,
                    trailing = {
                        Text(
                            whenText(a.at, a.hasTime, today),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (a.urgent) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                )
            }
        }
    }
}
