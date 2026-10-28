package ar.trenar.app.ui.cercanas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ar.trenar.app.R
import ar.trenar.app.data.model.ManualLocation
import ar.trenar.app.ui.common.StationMiniMap

/**
 * Lets the user type an address to search Cercanas around instead of the GPS fix.
 * As they type, [preview] (resolved by the ViewModel after a pause) is shown as a pin on a map.
 */
@Composable
fun ManualLocationDialog(
    initialAddress: String,
    preview: ManualLocation?,
    geocoding: Boolean,
    geocodeError: Boolean,
    onAddressChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var address by remember { mutableStateOf(initialAddress) }
    val shape = RoundedCornerShape(14.dp)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.ubicacion_manual_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.ubicacion_manual_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        if (it.length <= 200) {
                            address = it
                            onAddressChange(it)
                        }
                    },
                    label = { Text(stringResource(R.string.ubicacion_manual_hint)) },
                    singleLine = true,
                    isError = geocodeError,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (geocodeError) {
                    Text(
                        stringResource(R.string.ubicacion_manual_no_encontrada),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // Fixed-height map area so the dialog doesn't jump when the pin appears.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) {
                        StationMiniMap(
                            lat = preview.lat,
                            lng = preview.lng,
                            label = preview.address,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            stringResource(R.string.ubicacion_manual_mapa_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    if (geocoding) {
                        CircularProgressIndicator(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancelar))
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { onConfirm(address) },
                        enabled = !geocoding && address.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.ubicacion_manual_confirmar))
                    }
                }
            }
        }
    }
}
