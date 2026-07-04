package com.chisara.app.ui.reveal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chisara.app.data.repository.GameRepository
import com.chisara.app.ui.theme.Coral
import com.chisara.app.ui.theme.Mint
import com.chisara.app.viewmodel.GameViewModel
import kotlin.math.roundToInt

@Composable
fun RevealScreen(
    viewModel: GameViewModel,
    onDone: () -> Unit
) {
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val o = outcome) {
            is GameRepository.GuessOutcome.Revealed -> RevealedContent(o)
            is GameRepository.GuessOutcome.Invalidated -> InvalidatedContent(o)
            GameRepository.GuessOutcome.Unavailable, null -> {
                Text(
                    "Questo messaggio non è più disponibile.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Continua")
        }
    }
}

@Composable
private fun RevealedContent(o: GameRepository.GuessOutcome.Revealed) {
    val banner = if (o.correct) "🎉 Indovinato!" else "❌ Sbagliato"
    val color = if (o.correct) Mint else Coral

    Text(banner, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Il mittente era", style = MaterialTheme.typography.labelLarge)
            Text(o.realSenderName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!o.correct) {
                Text(
                    "Tu avevi risposto: ${o.guessedName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("+${o.score} punti", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val pct = (o.priorProbability * 100).roundToInt()
            Text(
                "Questo contatto rappresenta circa il $pct% dei messaggi di quell'app " +
                    "(${"%.1f".format(o.infoBits)} bit di sorpresa). " +
                    if (o.correct) "Più è raro, più vale!" else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InvalidatedContent(o: GameRepository.GuessOutcome.Invalidated) {
    Text("🙈 Hai sbirciato!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF8A5300))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(o.reason, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF6B4300))
            Text(
                "Questo messaggio non conta per il gioco. Il mittente era ${o.realSenderName}.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B4300)
            )
        }
    }
}
