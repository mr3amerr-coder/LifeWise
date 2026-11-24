package com.lifewise.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class Cycle(val label: String, val perMonth: Double) {
    MONTHLY("Monthly", 1.0),
    WEEKLY("Weekly", 52.0 / 12.0),
    QUARTERLY("Quarterly", 1.0 / 3.0),
    YEARLY("Yearly", 1.0 / 12.0),
    ONCE("One-time", 0.0)
}

enum class PayCategory(val label: String, val emoji: String) {
    SUBSCRIPTION("Subscription", "📺"),
    BILLS("Bills", "💡"),
    RENT("Rent", "🏠"),
    INSTALLMENT("Installment", "🧾"),
    INSURANCE("Insurance", "🛡️"),
    EDUCATION("Education", "🎓"),
    HEALTH("Health", "💊"),
    OTHER("Other", "📌")
}

/**
 * A payment you owe. [due] is the next unpaid due date.
 * Auto-pay items roll forward by themselves; manual items stay due (and become overdue)
 * until they are marked as paid.
 */
data class Payment(
    val id: Long,
    val name: String,
    val amount: Double,
    val cycle: Cycle,
    val category: PayCategory,
    val due: LocalDate,
    val autoPay: Boolean,
    val paid: Boolean = false
) {
    val monthlyCost: Double get() = amount * cycle.perMonth
}

data class Appointment(
    val id: Long,
    val doctor: String,
    val specialty: String,
    val at: LocalDateTime,
    val place: String,
    val note: String,
    val fee: Double
)

/** A medication taken every day at the given [times]. */
data class Medication(
    val id: Long,
    val name: String,
    val dose: String,
    val times: List<LocalTime>
)

enum class FoodType(val label: String, val emoji: String) {
    GROCERIES("Groceries", "🛒"),
    RESTAURANT("Restaurant", "🍽️"),
    DELIVERY("Delivery", "🛵"),
    COFFEE("Coffee & snacks", "☕"),
    OTHER("Other", "🥘")
}

data class FoodExpense(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: FoodType,
    val date: LocalDate
)

data class ShoppingItem(val id: Long, val name: String, val done: Boolean)

val Currencies = listOf("EGP", "$", "€", "£", "SAR", "AED")
