package com.chisara.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chisara.app.notification.NotificationConfig
import com.chisara.app.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // The apps that can be toggled on/off in this MVP.
    val configurablePackages = rememberConfigurablePackages()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "App da tenere d'occhio") {
                Text(
                    "Solo le notifiche di queste app entrano nel gioco.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(8.dp))
                configurablePackages.forEachIndexed { index, pkg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(NotificationConfig.displayName(pkg))
                        Switch(
                            checked = pkg in settings.trackedPackages,
                            onCheckedChange = { viewModel.setPackageTracked(pkg, it) }
                        )
                    }
                    if (index < configurablePackages.lastIndex) HorizontalDivider()
                }
            }

            SectionCard(title = "Penalità per risposta sbagliata") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Togli punti se sbagli")
                    Switch(
                        checked = settings.penaltyEnabled,
                        onCheckedChange = { viewModel.setPenaltyEnabled(it) }
                    )
                }
                if (settings.penaltyEnabled) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Penalità: −${settings.wrongAnswerPenalty} punti",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = settings.wrongAnswerPenalty.toFloat(),
                        onValueChange = { viewModel.setPenaltyValue(it.toInt()) },
                        valueRange = 0f..50f,
                        steps = 9
                    )
                } else {
                    Text(
                        "Attualmente una risposta sbagliata vale 0 punti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberConfigurablePackages(): List<String> = listOf(
    NotificationConfig.PKG_WHATSAPP,
    NotificationConfig.PKG_TELEGRAM,
    NotificationConfig.PKG_INSTAGRAM
)

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(8.dp))
            content()
        }
    }
}
