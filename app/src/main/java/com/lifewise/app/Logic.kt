package com.lifewise.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
val ShortDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")

fun money(value: Double, currency: String): String =
    String.format(Locale.getDefault(), "%s %,.2f", currency, value)

fun trimNumber(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

// ---------- Payments ----------

fun advance(anchor: LocalDate, cycle: Cycle, k: Long): LocalDate = when (cycle) {
    Cycle.WEEKLY -> anchor.plusWeeks(k)
    Cycle.MONTHLY -> anchor.plusMonths(k)
    Cycle.QUARTERLY -> anchor.plusMonths(3 * k)
    Cycle.YEARLY -> anchor.plusYears(k)
    Cycle.ONCE -> anchor
}

/** Next date this payment is due, or null when a one-time payment has been paid. */
fun Payment.nextDue(today: LocalDate): LocalDate? {
    if (cycle == Cycle.ONCE) return if (paid) null else due
    if (!autoPay) return due
    var k = 0L
    var d = due
    while (d.isBefore(today)) {
        k++
        d = advance(due, cycle, k)
    }
    return d
}

fun Payment.isOverdue(today: LocalDate): Boolean = nextDue(today)?.isBefore(today) == true

/** One-time: becomes paid. Recurring: moves to the next cycle. */
fun Payment.markPaid(): Payment =
    if (cycle == Cycle.ONCE) copy(paid = true) else copy(due = advance(due, cycle, 1))

fun dueText(due: LocalDate, today: LocalDate): String {
    val d = ChronoUnit.DAYS.between(today, due)
    return when {
        d < -1L -> "Overdue by ${-d} days"
        d == -1L -> "Overdue by 1 day"
        d == 0L -> "Due today"
        d == 1L -> "Due tomorrow"
        d <= 30L -> "In $d days"
        else -> due.format(DateFormat)
    }
}

// ---------- Agenda ----------

data class AgendaItem(
    val at: LocalDateTime,
    val hasTime: Boolean,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val urgent: Boolean
)

fun whenText(at: LocalDateTime, hasTime: Boolean, today: LocalDate): String {
    val d = ChronoUnit.DAYS.between(today, at.toLocalDate())
    val day = when (d) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> at.toLocalDate().format(ShortDateFormat)
    }
    return if (hasTime) "$day · ${at.format(TimeFormat)}" else day
}

fun doseKey(medId: Long, time: java.time.LocalTime): String = "$medId|$time"

/** Everything happening in the next [days] days: payments, appointments and today's untaken doses. */
fun buildAgenda(
    payments: List<Payment>,
    appointments: List<Appointment>,
    meds: List<Medication>,
    taken: Set<String>,
    currency: String,
    now: LocalDateTime,
    days: Long = 7
): List<AgendaItem> {
    val today = now.toLocalDate()
    val horizon = today.plusDays(days)
    val out = mutableListOf<AgendaItem>()

    for (p in payments) {
        val due = p.nextDue(today) ?: continue
        if (due.isAfter(horizon)) continue
        val d = ChronoUnit.DAYS.between(today, due)
        out += AgendaItem(
            at = due.atStartOfDay(),
            hasTime = false,
            emoji = p.category.emoji,
            title = p.name,
            subtitle = "${dueText(due, today)} · ${money(p.amount, currency)}",
            urgent = d <= 1L
        )
    }

    for (a in appointments) {
        if (a.at.isBefore(now) || a.at.toLocalDate().isAfter(horizon)) continue
        out += AgendaItem(
            at = a.at,
            hasTime = true,
            emoji = "🩺",
            title = a.doctor,
            subtitle = listOf(a.specialty, a.place).filter { it.isNotBlank() }.joinToString(" · "),
            urgent = ChronoUnit.HOURS.between(now, a.at) < 24L
        )
    }

    for (m in meds) {
        for (t in m.times) {
            if (doseKey(m.id, t) in taken) continue
            val at = today.atTime(t)
            out += AgendaItem(
                at = at,
                hasTime = true,
                emoji = "💊",
                title = m.name,
                subtitle = m.dose.ifBlank { "Medication" },
                urgent = at.isBefore(now)
            )
        }
    }

    return out.sortedBy { it.at }
}

// ---------- Insights ----------

fun foodSpentThisMonth(expenses: List<FoodExpense>, today: LocalDate): Double {
    val month = YearMonth.from(today)
    return expenses.filter { YearMonth.from(it.date) == month }.sumOf { it.amount }
}

fun buildInsights(
    payments: List<Payment>,
    appointments: List<Appointment>,
    expenses: List<FoodExpense>,
    foodBudget: Double,
    currency: String,
    now: LocalDateTime
): List<String> {
    val today = now.toLocalDate()
    val out = mutableListOf<String>()

    val overdue = payments.filter { it.isOverdue(today) }
    if (overdue.isNotEmpty()) {
        out += "${overdue.size} overdue payment(s) worth ${money(overdue.sumOf { it.amount }, currency)}. Pay these first."
    }

    val soon = payments.filter { p ->
        val d = p.nextDue(today)
        d != null && !d.isBefore(today) && !d.isAfter(today.plusDays(7))
    }
    if (soon.isNotEmpty()) {
        out += "${soon.size} payment(s) due this week, ${money(soon.sumOf { it.amount }, currency)} in total."
    }

    val next = appointments.filter { it.at.isAfter(now) }.minByOrNull { it.at }
    if (next != null && ChronoUnit.DAYS.between(today, next.at.toLocalDate()) <= 7L) {
        out += "Next appointment: ${next.doctor}, ${next.at.format(DateTimeFormat)}."
    }

    val spent = foodSpentThisMonth(expenses, today)
    if (foodBudget > 0) {
        val pct = (spent / foodBudget * 100).roundToInt()
        val left = foodBudget - spent
        val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
        out += if (left <= 0) {
            "Food budget exceeded ($pct% used)."
        } else {
            "Food: $pct% of budget used. About ${money(left / daysLeft, currency)} per day left this month."
        }
    } else if (spent > 0) {
        out += "You spent ${money(spent, currency)} on food this month. Set a budget in the Food tab."
    }

    val recurring = payments.filter { it.monthlyCost > 0 }
    val total = recurring.sumOf { it.monthlyCost }
    val top = recurring.maxByOrNull { it.monthlyCost }
    if (top != null && total > 0 && recurring.size >= 2) {
        out += "${top.name} is your biggest recurring cost: ${(top.monthlyCost / total * 100).roundToInt()}% of monthly payments."
    }

    if (out.isEmpty()) {
        out += if (payments.isEmpty() && appointments.isEmpty() && expenses.isEmpty()) {
            "Add payments, doctor visits, medication or meals and your insights will show up here."
        } else {
            "Nothing urgent. You're all caught up."
        }
    }
    return out.take(4)
}
