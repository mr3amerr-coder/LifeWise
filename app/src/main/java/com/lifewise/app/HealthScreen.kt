@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDateTime

@Composable
fun HealthScreen(
    appointments: List<Appointment>,
    medications: List<Medication>,
    taken: Set<String>,
    currency: String,
    padding: PaddingValues,
    onAppointment: (Appointment) -> Unit,
    onMedication: (Medication) -> Unit,
    onToggleDose: (String) -> Unit
) {
    val now = LocalDateTime.now()
    val upcoming = appointments.filter { !it.at.isBefore(now) }.sortedBy { it.at }
    val past = appointments.filter { it.at.isBefore(now) }.sortedByDescending { it.at }.take(5)

    ScreenList(padding) {
        if (medications.isNotEmpty()) {
            item { DosesCard(medications, taken, onToggleDose) }
        }

        item { SectionTitle("Upcoming appointments") }
        if (upcoming.isEmpty()) {
            item { EmptyHint("No upcoming visits. Tap Add to schedule one and get a reminder the day before.") }
        } else {
            items(upcoming) { a -> AppointmentRow(a, currency) { onAppointment(a) } }
        }

        item { SectionTitle("Medications") }
        if (medications.isEmpty()) {
            item { EmptyHint("No medication yet. Tap Add to set daily reminders.") }
        } else {
            items(medications) { m ->
                ItemRow(
                    emoji = "💊",
                    color = MedColor,
                    title = m.name,
                    subtitle = listOf(m.dose, m.times.joinToString(", ") { it.format(TimeFormat) })
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    onClick = { onMedication(m) }
                )
            }
        }

        if (past.isNotEmpty()) {
            item { SectionTitle("Past visits") }
            items(past) { a -> AppointmentRow(a, currency) { onAppointment(a) } }
        }
    }
}

@Composable
private fun AppointmentRow(a: Appointment, currency: String, onClick: () -> Unit) {
    ItemRow(
        emoji = "🩺",
        color = HealthColor,
        title = a.doctor,
        subtitle = listOf(a.specialty, a.at.format(DateTimeFormat), a.place)
            .filter { it.isNotBlank() }
            .joinToString(" · "),
        onClick = onClick,
        trailing = if (a.fee > 0) {
            { Text(money(a.fee, currency), fontWeight = FontWeight.SemiBold) }
        } else {
            null
        }
    )
}
