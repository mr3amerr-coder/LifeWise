@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun PaymentsScreen(
    payments: List<Payment>,
    currency: String,
    padding: PaddingValues,
    onOpen: (Payment) -> Unit,
    onPaid: (Payment) -> Unit
) {
    val today = LocalDate.now()
    val active = payments.filter { it.nextDue(today) != null }
    val overdue = active.filter { it.isOverdue(today) }.sortedBy { it.nextDue(today) ?: today }
    val upcoming = active.filterNot { it.isOverdue(today) }.sortedBy { it.nextDue(today) ?: today }
    val paidOnce = payments.filter { it.cycle == Cycle.ONCE && it.paid }

    val monthly = payments.sumOf { it.monthlyCost }
    val next30 = active
        .filter { p -> p.nextDue(today)?.isAfter(today.plusDays(30)) == false }
        .sumOf { it.amount }
    val overdueTotal = overdue.sumOf { it.amount }

    ScreenList(padding) {
        item {
            HeroCard(
                label = "Monthly payments",
                big = money(monthly, currency),
                sub = buildString {
                    append("${money(next30, currency)} due in the next 30 days")
                    if (overdueTotal > 0) append(" · ${money(overdueTotal, currency)} overdue")
                }
            )
        }

        if (payments.isEmpty()) {
            item { EmptyHint("No payments yet. Tap Add to track a bill, subscription, rent or installment.") }
        } else {
            item {
                StackedBar(
                    PayCategory.entries.map { cat ->
                        BarPart(
                            cat.label,
                            payments.filter { it.category == cat }.sumOf { it.monthlyCost },
                            cat.color
                        )
                    }
                )
            }
        }

        if (overdue.isNotEmpty()) {
            item { SectionTitle("Overdue") }
            items(overdue) { p -> PaymentRow(p, currency, today, onOpen, onPaid) }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Upcoming") }
            items(upcoming) { p -> PaymentRow(p, currency, today, onOpen, onPaid) }
        }
        if (paidOnce.isNotEmpty()) {
            item { SectionTitle("Paid") }
            items(paidOnce) { p -> PaymentRow(p, currency, today, onOpen, onPaid) }
        }
    }
}

@Composable
private fun PaymentRow(
    p: Payment,
    currency: String,
    today: LocalDate,
    onOpen: (Payment) -> Unit,
    onPaid: (Payment) -> Unit
) {
    val due = p.nextDue(today)
    val late = p.isOverdue(today)
    val subtitle = if (due == null) {
        "Paid"
    } else {
        "${dueText(due, today)} · ${p.cycle.label}"
    }

    ItemRow(
        emoji = p.category.emoji,
        color = p.category.color,
        title = p.name,
        subtitle = subtitle,
        subtitleColor = if (late) MaterialTheme.colorScheme.error else Color.Unspecified,
        onClick = { onOpen(p) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(money(p.amount, currency), fontWeight = FontWeight.SemiBold)
                when {
                    p.cycle == Cycle.ONCE && p.paid ->
                        Text("Paid ✓", style = MaterialTheme.typography.bodySmall)
                    p.autoPay && p.cycle != Cycle.ONCE ->
                        Text("Auto-pay", style = MaterialTheme.typography.bodySmall)
                    else -> TextButton(
                        onClick = { onPaid(p) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp
                        )
                    ) { Text("Mark paid") }
                }
            }
        }
    )
}
