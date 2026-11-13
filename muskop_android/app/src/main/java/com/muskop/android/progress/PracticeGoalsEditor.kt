package com.muskop.android.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.muskop.session.PracticeGoals

@Composable
internal fun PracticeGoalsEditor(
    goals: PracticeGoals,
    onDismiss: () -> Unit,
    onSave: (PracticeGoals) -> Unit,
) {
    var weekly by remember(goals) { mutableStateOf(goals.weekly.toString()) }
    var monthly by remember(goals) { mutableStateOf(goals.monthly.toString()) }
    var yearly by remember(goals) { mutableStateOf(goals.yearly.toString()) }
    fun parsed() = listOf(weekly, monthly, yearly).map { it.toIntOrNull()?.takeIf { value -> value >= 0 } }
    val values = parsed()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Practice goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter minutes for each period. Use 0 to leave a goal unset.")
                GoalField("Weekly minutes", weekly, { weekly = it })
                GoalField("Monthly minutes", monthly, { monthly = it })
                GoalField("Yearly minutes", yearly, { yearly = it })
                if (values.any { it == null }) Text("Enter a whole number of minutes (0 or more).")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (values.all { it != null }) onSave(PracticeGoals(values[0]!!, values[1]!!, values[2]!!))
            }, enabled = values.all { it != null }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GoalField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= 9 && input.all(Char::isDigit)) onChange(input) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
