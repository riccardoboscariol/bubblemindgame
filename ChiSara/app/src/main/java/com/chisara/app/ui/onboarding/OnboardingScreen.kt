package com.chisara.app.ui.onboarding

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chisara.app.permissions.PermissionUtils
import com.chisara.app.ui.theme.Mint
import com.chisara.app.viewmodel.GameViewModel

@Composable
fun OnboardingScreen(
    viewModel: GameViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()

    // Re-check permissions whenever we come back from a Settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Chi Sarà? 🕵️", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Quando ti arriva un messaggio su WhatsApp, Telegram o Instagram, l'app lo nasconde " +
                "e ti mostra una notifica \"cieca\". Tocca a te indovinare CHI te l'ha mandato. " +
                "Più la persona scrive di rado, più punti vali se ci azzecchi!",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            "Per funzionare servono due permessi speciali",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        PermissionCard(
            granted = permissions.notificationAccess,
            title = "1 · Accesso alle notifiche",
            description = "Permette all'app di intercettare e sostituire le notifiche dei messaggi " +
                "prima che tu le legga.",
            buttonText = "Apri impostazioni notifiche",
            onClick = { context.startActivity(PermissionUtils.notificationAccessIntent(context)) }
        )

        PermissionCard(
            granted = permissions.usageAccess,
            title = "2 · Accesso all'utilizzo",
            description = "Serve a capire se hai \"sbirciato\" aprendo l'app originale prima di " +
                "rispondere. In quel caso il messaggio non conta per il gioco.",
            buttonText = "Apri accesso all'utilizzo",
            onClick = { context.startActivity(PermissionUtils.usageAccessIntent(context)) }
        )

        if (!permissions.postNotifications) {
            PermissionCard(
                granted = false,
                title = "3 · Invio notifiche",
                description = "Su Android 13+ concedi all'app di mostrarti le notifiche cieche. " +
                    "Trovi l'interruttore nelle impostazioni dell'app.",
                buttonText = "Apri impostazioni app",
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        ).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )
        }

        PreviewInstructionsCard()

        Spacer(Modifier.size(4.dp))
        Button(
            onClick = onDone,
            enabled = permissions.allGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (permissions.allGranted) "Iniziamo a giocare!" else "Concedi i permessi per continuare")
        }
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun PermissionCard(
    granted: Boolean,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (granted) Mint else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (!granted) {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                    Text(buttonText)
                }
            } else {
                Text("Concesso ✓", color = Mint, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PreviewInstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "⚠️ Disattiva le anteprime dei messaggi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8A5300)
            )
            Text(
                "Se le anteprime sono attive, il testo del messaggio è già visibile nella tendina " +
                    "e il gioco non ha senso. Disattivale così:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B4300)
            )
            Text(
                "• WhatsApp: Impostazioni ▸ Notifiche ▸ disattiva \"Mostra anteprima\".\n" +
                    "• Telegram: Impostazioni ▸ Notifiche e suoni ▸ disattiva \"Anteprima messaggio\".\n" +
                    "• Instagram: Impostazioni ▸ Notifiche (o nascondi il contenuto dalle impostazioni " +
                    "notifiche del sistema per Instagram).",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B4300)
            )
        }
    }
}
