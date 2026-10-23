package ar.trenar.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ar.trenar.app.R
import ar.trenar.app.data.FeedbackClient
import ar.trenar.app.ui.theme.OnTimeGreen
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("feature") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!sending) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (done) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnTimeGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.feedback_ok),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.cerrar))
                    }
                } else {
                    Text(
                        stringResource(R.string.feedback_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "feature",
                            onClick = { type = "feature" },
                            label = { Text(stringResource(R.string.feedback_type_feature)) },
                        )
                        FilterChip(
                            selected = type == "bug",
                            onClick = { type = "bug" },
                            label = { Text(stringResource(R.string.feedback_type_bug)) },
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        label = { Text(stringResource(R.string.feedback_title_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 2000) description = it },
                        label = { Text(stringResource(R.string.feedback_desc_label)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { if (it.length <= 120) contact = it },
                        label = { Text(stringResource(R.string.feedback_contact_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error) {
                        Text(
                            stringResource(R.string.feedback_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (sending) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                        }
                        TextButton(onClick = onDismiss, enabled = !sending) {
                            Text(stringResource(R.string.cancelar))
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = {
                                error = false
                                sending = true
                                scope.launch {
                                    val ok = FeedbackClient.send(type, title.trim(), description.trim(), contact.trim())
                                    sending = false
                                    if (ok) done = true else error = true
                                }
                            },
                            enabled = !sending && title.isNotBlank() && description.isNotBlank(),
                        ) {
                            Text(stringResource(if (sending) R.string.feedback_sending else R.string.feedback_send))
                        }
                    }
                }
            }
        }
    }
}
