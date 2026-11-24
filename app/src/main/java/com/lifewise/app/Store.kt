package com.lifewise.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Tiny local persistence: JSON strings in SharedPreferences. No database, no network. */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("lifewise", Context.MODE_PRIVATE)

    var currency: String
        get() = prefs.getString("currency", null) ?: Currencies.first()
        set(value) {
            prefs.edit().putString("currency", value).apply()
        }

    var foodBudget: Double
        get() = prefs.getString("food_budget", null)?.toDoubleOrNull() ?: 0.0
        set(value) {
            prefs.edit().putString("food_budget", value.toString()).apply()
        }

    /** Doses ticked off today. Automatically empty on a new day. */
    var taken: Set<String>
        get() {
            if (prefs.getString("taken_date", null) != LocalDate.now().toString()) return emptySet()
            return HashSet(prefs.getStringSet("taken", emptySet()) ?: emptySet())
        }
        set(value) {
            prefs.edit()
                .putString("taken_date", LocalDate.now().toString())
                .putStringSet("taken", HashSet(value))
                .apply()
        }

    // ----- generic helpers -----

    private fun <T> loadList(key: String, parse: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { parse(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun <T> saveList(key: String, list: List<T>, write: (T) -> JSONObject) {
        val arr = JSONArray()
        list.forEach { arr.put(write(it)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    // ----- payments -----

    fun loadPayments(): List<Payment> = loadList("payments") { o ->
        Payment(
            id = o.getLong("id"),
            name = o.getString("name"),
            amount = o.getDouble("amount"),
            cycle = Cycle.valueOf(o.getString("cycle")),
            category = PayCategory.valueOf(o.getString("category")),
            due = LocalDate.parse(o.getString("due")),
            autoPay = o.optBoolean("auto", false),
            paid = o.optBoolean("paid", false)
        )
    }

    fun savePayments(list: List<Payment>) = saveList("payments", list) { p ->
        JSONObject()
            .put("id", p.id)
            .put("name", p.name)
            .put("amount", p.amount)
            .put("cycle", p.cycle.name)
            .put("category", p.category.name)
            .put("due", p.due.toString())
            .put("auto", p.autoPay)
            .put("paid", p.paid)
    }

    // ----- appointments -----

    fun loadAppointments(): List<Appointment> = loadList("appointments") { o ->
        Appointment(
            id = o.getLong("id"),
            doctor = o.getString("doctor"),
            specialty = o.optString("specialty", ""),
            at = LocalDateTime.parse(o.getString("at")),
            place = o.optString("place", ""),
            note = o.optString("note", ""),
            fee = o.optDouble("fee", 0.0)
        )
    }

    fun saveAppointments(list: List<Appointment>) = saveList("appointments", list) { a ->
        JSONObject()
            .put("id", a.id)
            .put("doctor", a.doctor)
            .put("specialty", a.specialty)
            .put("at", a.at.toString())
            .put("place", a.place)
            .put("note", a.note)
            .put("fee", a.fee)
    }

    // ----- medications -----

    fun loadMedications(): List<Medication> = loadList("medications") { o ->
        val arr = o.getJSONArray("times")
        Medication(
            id = o.getLong("id"),
            name = o.getString("name"),
            dose = o.optString("dose", ""),
            times = (0 until arr.length()).map { LocalTime.parse(arr.getString(it)) }
        )
    }

    fun saveMedications(list: List<Medication>) = saveList("medications", list) { m ->
        val times = JSONArray()
        m.times.forEach { times.put(it.toString()) }
        JSONObject()
            .put("id", m.id)
            .put("name", m.name)
            .put("dose", m.dose)
            .put("times", times)
    }

    // ----- food -----

    fun loadExpenses(): List<FoodExpense> = loadList("expenses") { o ->
        FoodExpense(
            id = o.getLong("id"),
            title = o.getString("title"),
            amount = o.getDouble("amount"),
            type = FoodType.valueOf(o.getString("type")),
            date = LocalDate.parse(o.getString("date"))
        )
    }

    fun saveExpenses(list: List<FoodExpense>) = saveList("expenses", list) { e ->
        JSONObject()
            .put("id", e.id)
            .put("title", e.title)
            .put("amount", e.amount)
            .put("type", e.type.name)
            .put("date", e.date.toString())
    }

    fun loadShopping(): List<ShoppingItem> = loadList("shopping") { o ->
        ShoppingItem(
            id = o.getLong("id"),
            name = o.getString("name"),
            done = o.optBoolean("done", false)
        )
    }

    fun saveShopping(list: List<ShoppingItem>) = saveList("shopping", list) { s ->
        JSONObject()
            .put("id", s.id)
            .put("name", s.name)
            .put("done", s.done)
    }
}
