package com.trail2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.R

data class ReportData(
    val reason: String,
    val description: String?
)

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (ReportData) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    val reasons = listOf(
        "spam" to stringResource(R.string.report_reason_spam),
        "harassment" to stringResource(R.string.report_reason_harassment),
        "inappropriate" to stringResource(R.string.report_reason_inappropriate),
        "misinformation" to stringResource(R.string.report_reason_misinformation),
        "other" to stringResource(R.string.report_reason_other)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.report_select_reason), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                reasons.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = key }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.report_description)) },
                    placeholder = { Text(stringResource(R.string.report_description_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedReason?.let { reason ->
                        onSubmit(ReportData(reason, description.ifBlank { null }))
                    }
                },
                enabled = selectedReason != null
            ) {
                Text(stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
