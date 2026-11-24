@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifewise.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FoodScreen(
    vm: MainViewModel,
    padding: PaddingValues,
    onEdit: (FoodExpense) -> Unit,
    onBudget: () -> Unit
) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val currency = vm.currency
    val budget = vm.foodBudget
    val monthExpenses = vm.expenses
        .filter { YearMonth.from(it.date) == month }
        .sortedWith(compareByDescending<FoodExpense> { it.date }.thenByDescending { it.id })
    val spent = monthExpenses.sumOf { it.amount }

    ScreenList(padding) {
        item { BudgetCard(spent, budget, currency, onBudget) }

        if (monthExpenses.isNotEmpty()) {
            item {
                StackedBar(
                    FoodType.entries.map { t ->
                        BarPart(t.label, monthExpenses.filter { it.type == t }.sumOf { it.amount }, t.color)
                    }
                )
            }
        }

        item { SectionTitle("Shopping list") }
        item { ShoppingInput(vm::addShopping) }
        items(vm.shopping, key = { "shop-${it.id}" }) { s ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = s.done, onCheckedChange = { vm.toggleShopping(s.id) })
                Text(
                    s.name,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (s.done) TextDecoration.LineThrough else null
                )
                IconButton(onClick = { vm.deleteShopping(s.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove ${s.name}")
                }
            }
        }
        if (vm.shopping.any { it.done }) {
            item {
                TextButton(onClick = vm::clearDoneShopping) { Text("Clear checked items") }
            }
        }

        item { SectionTitle("This month") }
        if (monthExpenses.isEmpty()) {
            item { EmptyHint("No food expenses this month. Tap Add to log groceries, restaurants or delivery.") }
        } else {
            items(monthExpenses) { e ->
                ItemRow(
                    emoji = e.type.emoji,
                    color = e.type.color,
                    title = e.title,
                    subtitle = "${e.type.label} · ${e.date.format(DateFormat)}",
                    onClick = { onEdit(e) },
                    trailing = { Text(money(e.amount, currency), fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(spent: Double, budget: Double, currency: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Food this month", style = MaterialTheme.typography.labelLarge)
            Text(
                money(spent, currency),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (budget > 0) {
                val fraction = (spent / budget).toFloat().coerceIn(0f, 1f)
                val over = spent > budget
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (over) {
                        "Over budget by ${money(spent - budget, currency)}"
                    } else {
                        "${money(budget - spent, currency)} left of ${money(budget, currency)}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Tap to set a monthly food budget", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ShoppingInput(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text.trim())
            text = ""
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Add to shopping list") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        IconButton(onClick = { submit() }) {
            Icon(Icons.Default.Add, contentDescription = "Add item")
        }
    }
}
