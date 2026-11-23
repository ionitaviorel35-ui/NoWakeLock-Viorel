package com.js.nowakelock.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.js.nowakelock.data.repository.preferences.UserPreferencesRepository

private fun backendLabel(p: UserPreferencesRepository.BackendPreference): String =
    p.name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun PrivilegedBackendSettings(
    backendPreference: UserPreferencesRepository.BackendPreference,
    onShowBackendDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowBackendDialog() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Privileged backend", style = MaterialTheme.typography.titleMedium)
        Text(
            backendLabel(backendPreference),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BackendSelectionDialog(
    currentPreference: UserPreferencesRepository.BackendPreference,
    onPreferenceSelected: (UserPreferencesRepository.BackendPreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privileged backend") },
        text = {
            Column {
                UserPreferencesRepository.BackendPreference.values().forEach { pref ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPreferenceSelected(pref)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = pref == currentPreference, onClick = null)
                        Text(backendLabel(pref), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
