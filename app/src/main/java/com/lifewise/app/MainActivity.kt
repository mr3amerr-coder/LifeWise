package com.lifewise.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    var currency by mutableStateOf(store.currency)
        private set
    var foodBudget by mutableStateOf(store.foodBudget)
        private set
    var payments by mutableStateOf(store.loadPayments())
        private set
    var appointments by mutableStateOf(store.loadAppointments())
        private set
    var medications by mutableStateOf(store.loadMedications())
        private set
    var expenses by mutableStateOf(store.loadExpenses())
        private set
    var shopping by mutableStateOf(store.loadShopping())
        private set
    var taken by mutableStateOf(store.taken)
        private set

    private fun <T> List<T>.upsert(item: T, id: (T) -> Long): List<T> =
        if (any { id(it) == id(item) }) map { if (id(it) == id(item)) item else it } else this + item

    private fun changed() = Reminders.rescheduleAll(app)

    // ----- payments -----

    fun savePayment(p: Payment) {
        payments = payments.upsert(p) { it.id }
        store.savePayments(payments)
        changed()
    }

    fun deletePayment(id: Long) {
        payments = payments.filterNot { it.id == id }
        store.savePayments(payments)
        changed()
    }

    fun markPaid(p: Payment) = savePayment(p.markPaid())

    // ----- health -----

    fun saveAppointment(a: Appointment) {
        appointments = appointments.upsert(a) { it.id }
        store.saveAppointments(appointments)
        changed()
    }

    fun deleteAppointment(id: Long) {
        appointments = appointments.filterNot { it.id == id }
        store.saveAppointments(appointments)
        changed()
    }

    fun saveMedication(m: Medication) {
        medications = medications.upsert(m) { it.id }
        store.saveMedications(medications)
        changed()
    }

    fun deleteMedication(id: Long) {
        medications = medications.filterNot { it.id == id }
        store.saveMedications(medications)
        changed()
    }

    fun toggleDose(key: String) {
        taken = if (key in taken) taken - key else taken + key
        store.taken = taken
        changed()
    }

    /** Called when the app returns to the foreground, so a new day clears yesterday's ticks. */
    fun syncDay() {
        taken = store.taken
    }

    // ----- food -----

    fun saveExpense(e: FoodExpense) {
        expenses = expenses.upsert(e) { it.id }
        store.saveExpenses(expenses)
    }

    fun deleteExpense(id: Long) {
        expenses = expenses.filterNot { it.id == id }
        store.saveExpenses(expenses)
    }

    fun setBudget(value: Double) {
        foodBudget = value
        store.foodBudget = value
    }

    fun addShopping(name: String) {
        shopping = shopping + ShoppingItem(System.currentTimeMillis(), name, false)
        store.saveShopping(shopping)
    }

    fun toggleShopping(id: Long) {
        shopping = shopping.map { if (it.id == id) it.copy(done = !it.done) else it }
        store.saveShopping(shopping)
    }

    fun deleteShopping(id: Long) {
        shopping = shopping.filterNot { it.id == id }
        store.saveShopping(shopping)
    }

    fun clearDoneShopping() {
        shopping = shopping.filterNot { it.done }
        store.saveShopping(shopping)
    }

    // ----- settings -----

    fun cycleCurrency() {
        val next = Currencies[(Currencies.indexOf(currency) + 1) % Currencies.size]
        currency = next
        store.currency = next
        changed()
    }
}

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Reminders.ensureChannel(this)
        Reminders.rescheduleAll(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LifeWiseTheme {
                LifeWiseApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.syncDay()
    }
}
