package com.chisara.app.ui.guess

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chisara.app.ui.Format
import com.chisara.app.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessScreen(
    viewModel: GameViewModel,
    eventId: Long,
    onRevealReady: () -> Unit,
    onBack: () -> Unit
) {
    val guessState by viewModel.guessScreen.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) { viewModel.loadEventForGuess(eventId) }

    // As soon as a guess is scored, move to the reveal screen.
    LaunchedEffect(outcome) { if (outcome != null) onRevealReady() }

    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi sarà?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        val state = guessState
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val filtered = remember(state.recentContacts, query) {
            if (query.isBlank()) state.recentContacts
            else state.recentContacts.filter { it.displayName.contains(query, ignoreCase = true) }
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📩 Messaggio misterioso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Arrivato alle ${Format.time(state.arrivalTs)} da ${state.appName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.groupName != null) {
                    Text(
                        "Nel gruppo \"${state.groupName}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Chi ha scritto nel gruppo? Scegli un contatto o scrivi un nome.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "Chi te l'ha mandato? Scegli un contatto o scrivi un nome.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Cerca o scrivi un nome…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(Modifier.size(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Free-text guess when what the user typed isn't in the list.
                if (query.isNotBlank() && filtered.none { it.displayName.equals(query, ignoreCase = true) }) {
                    item {
                        ListItem(
                            headlineContent = { Text("Rispondi: \"$query\"") },
                            supportingContent = { Text("Usa il nome che hai scritto") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableRow { viewModel.submitGuess(eventId, query.trim()) }
                        )
                    }
                }

                items(filtered, key = { it.id }) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.displayName) },
                        supportingContent = {
                            Text("Messaggi ricevuti: ${contact.historicalMessageCount}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableRow { viewModel.submitGuess(eventId, contact.displayName) }
                    )
                }

                if (filtered.isEmpty() && query.isBlank()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text(
                                "Non ci sono ancora contatti noti per questa app. " +
                                    "Scrivi un nome qui sopra per rispondere.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.size(12.dp))
                            Button(
                                onClick = { if (query.isNotBlank()) viewModel.submitGuess(eventId, query.trim()) },
                                enabled = query.isNotBlank()
                            ) { Text("Rispondi") }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
