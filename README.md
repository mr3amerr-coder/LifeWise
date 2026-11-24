# LifeWise

One private Android app for the admin of everyday life: **payments, doctor and medication, and food**.
No account, no ads, no internet permission. Everything is stored on the phone.

## Tabs

**Today**: estimated spend this month, smart insights, today's medication doses (tick them off) and a
7-day agenda that merges payments, appointments and doses.

**Payments**: bills, subscriptions, rent, installments, insurance, school fees, one-time payments.
- Weekly, monthly, quarterly, yearly or one-time, normalized into a real monthly cost.
- Auto-pay items roll forward by themselves; manual bills stay due, turn red when overdue, and have a
  "Mark paid" button that moves them to the next cycle.
- Category breakdown bar, quick-add presets (Electricity, Water, Internet, Rent, Netflix, ...).

**Health**: doctor appointments (doctor, specialty, date and time, place, fee, note) and daily medication
with any number of times per day. Today's doses can be ticked off; the tick resets every day.

**Food**: monthly food budget with a progress bar and "you can spend about X per day" advice,
expense log (groceries, restaurant, delivery, coffee), spending breakdown, and a shopping list.

Tap the currency in the top bar to cycle EGP, $, EUR, GBP, SAR, AED.

## Reminders

Local notifications, no server needed:
- Payments: 09:00 the day before and on the due day; every morning while an unpaid payment is overdue.
- Appointments: 18:00 the day before and 2 hours before.
- Medication: every day at each chosen time (skipped once the dose is ticked).

Alarms are rebuilt from the stored data whenever something changes, when an alarm fires, and after a
reboot, so they never go stale. They are inexact (no special permission, better battery life), so a
reminder can arrive a few minutes late. Android 13+ asks for notification permission on first launch.

## Build and run

1. Install Android Studio (Ladybug or newer) with JDK 17.
2. File > Open the `LifeWise` folder and let Gradle sync (needs internet once).
3. Run on an emulator or a phone with USB debugging.

Command line: `./gradlew assembleDebug` (needs ANDROID_HOME pointing at an Android SDK).

## Code map

| File | Purpose |
|---|---|
| `Model.kt` | Data types: payments, appointments, medication, food |
| `Logic.kt` | Due-date math, agenda, insights, formatting |
| `Store.kt` | Local storage (JSON in SharedPreferences) |
| `Reminders.kt` | Alarms, notification receiver, boot receiver |
| `MainActivity.kt` | Activity and ViewModel |
| `App.kt` | Navigation, floating buttons, dialog routing |
| `TodayScreen.kt`, `PaymentsScreen.kt`, `HealthScreen.kt`, `FoodScreen.kt` | The four tabs |
| `Dialogs.kt`, `Components.kt`, `Theme.kt` | Forms, shared UI pieces, Material 3 theme |

## Ideas for the next version

- Backup and restore to a file; CSV export
- Home screen widget with the next payment or dose
- Premium unlock: unlimited items, encrypted backup, family sharing for medication
