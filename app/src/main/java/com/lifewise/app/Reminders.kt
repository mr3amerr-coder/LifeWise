package com.lifewise.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local reminders for payments, appointments and medication.
 *
 * Every time data changes (or an alarm fires, or the phone reboots) all alarms are cancelled and
 * rebuilt from the stored data, so reminders can never go stale. Alarms are inexact on purpose:
 * no special permission and kinder to the battery, so they can arrive a few minutes late.
 */
object Reminders {

    const val CHANNEL = "reminders"
    private const val KEYS = "keys"

    private data class Planned(
        val key: String,
        val at: LocalDateTime,
        val title: String,
        val text: String
    )

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Payments, appointments and medication"
        nm.createNotificationChannel(channel)
    }

    fun rescheduleAll(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val state = app.getSharedPreferences("alarms", Context.MODE_PRIVATE)

        state.getStringSet(KEYS, emptySet())?.forEach { key ->
            am.cancel(pending(app, key, "", ""))
        }

        val plan = buildPlan(Store(app), LocalDateTime.now())
        val zone = ZoneId.systemDefault()
        plan.forEach { p ->
            val millis = p.at.atZone(zone).toInstant().toEpochMilli()
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(app, p.key, p.title, p.text))
        }
        state.edit().putStringSet(KEYS, plan.map { it.key }.toSet()).apply()
    }

    private fun buildPlan(store: Store, now: LocalDateTime): List<Planned> {
        val today = now.toLocalDate()
        val currency = store.currency
        val plan = mutableListOf<Planned>()

        // Payments: 9:00 the day before and on the due day. Unpaid and late: every morning.
        for (p in store.loadPayments()) {
            val due = p.nextDue(today) ?: continue
            val amount = money(p.amount, currency)
            val candidates = listOf(
                due.minusDays(1).atTime(9, 0) to "${p.name} is due tomorrow ($amount)",
                due.atTime(9, 0) to "${p.name} is due today ($amount)"
            )
            val pick = candidates.firstOrNull { it.first.isAfter(now) }
                ?: (today.plusDays(1).atTime(9, 0) to "${p.name} is overdue ($amount)")
            plan += Planned("pay:${p.id}", pick.first, "Payment reminder", pick.second)
        }

        // Appointments: 18:00 the day before and 2 hours before.
        for (a in store.loadAppointments()) {
            val dayBefore = a.at.toLocalDate().minusDays(1).atTime(18, 0)
            val twoHours = a.at.minusHours(2)
            val place = if (a.place.isBlank()) "" else " · ${a.place}"
            if (dayBefore.isAfter(now)) {
                plan += Planned(
                    "appt:${a.id}:d", dayBefore, "Appointment tomorrow",
                    "${a.doctor} at ${a.at.format(TimeFormat)}$place"
                )
            }
            if (twoHours.isAfter(now)) {
                plan += Planned(
                    "appt:${a.id}:h", twoHours, "Appointment in 2 hours",
                    "${a.doctor} at ${a.at.format(TimeFormat)}$place"
                )
            }
        }

        // Medication: the next occurrence of every daily time. Skips a dose already ticked today.
        val taken = store.taken
        for (m in store.loadMedications()) {
            for (t in m.times) {
                var at = today.atTime(t)
                if (!at.isAfter(now) || doseKey(m.id, t) in taken) at = today.plusDays(1).atTime(t)
                plan += Planned(
                    "med:${m.id}:$t", at, "Time for ${m.name}",
                    m.dose.ifBlank { "Take your medication" }
                )
            }
        }
        return plan
    }

    private fun pending(context: Context, key: String, title: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("title", title)
            .putExtra("text", text)
            .putExtra("nid", key.hashCode())
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        if (!title.isNullOrBlank()) {
            val text = intent.getStringExtra("text") ?: ""
            val nid = intent.getIntExtra("nid", 0)
            Reminders.ensureChannel(context)

            val allowed = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (allowed) {
                val open = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, Reminders.CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(nid, notification)
            }
        }
        // Roll everything forward (next day's dose, next payment reminder, ...).
        Reminders.rescheduleAll(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.rescheduleAll(context)
    }
}
