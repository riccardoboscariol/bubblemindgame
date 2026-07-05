package com.chisara.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chisara.app.data.db.dao.ContactAccuracy
import com.chisara.app.notification.NotificationConfig
import com.chisara.app.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val trend by viewModel.scoreTrend.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiche") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Metric("${stats.totalScore}", "Punti totali")
                        Metric("${stats.accuracyPercent}%", "Precisione")
                        Metric("${stats.correct}/${stats.attempts}", "Indovinati")
                    }
                }
            }

            if (trend.size >= 2) {
                item {
                    SectionTitle("Andamento del punteggio")
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Punteggio cumulato su ${trend.size} risposte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.size(8.dp))
                            TrendChart(
                                values = trend,
                                lineColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("Su chi sei più bravo a indovinare")
            }

            val ranked = stats.accuracyByContact
            if (ranked.isEmpty()) {
                item {
                    Text(
                        "Ancora nessuna risposta valida. Gioca qualche round e qui vedrai la classifica " +
                            "dei contatti che azzecchi di più (e di meno).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(ranked.take(5), key = { it.displayName + it.sourcePackage }) { row ->
                    ContactAccuracyRow(row)
                }

                if (ranked.size > 5) {
                    item {
                        Text(
                            "…e quelli su cui sbagli di più",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(ranked.takeLast(5).reversed(), key = { "worst_" + it.displayName + it.sourcePackage }) { row ->
                        ContactAccuracyRow(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

/** Minimal line chart of a monotonically-growing cumulative score. */
@Composable
private fun TrendChart(
    values: List<Int>,
    lineColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, v ->
            val x = stepX * index
            val y = size.height - (v.toFloat() / maxValue) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6f)
        )
        // End marker.
        val lastX = size.width
        val lastY = size.height - (values.last().toFloat() / maxValue) * size.height
        drawCircle(color = lineColor, radius = 8f, center = Offset(lastX, lastY))
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ContactAccuracyRow(row: ContactAccuracy) {
    val pct = if (row.attempts == 0) 0f else row.correct.toFloat() / row.attempts
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.displayName, fontWeight = FontWeight.SemiBold)
                Text("${(pct * 100).toInt()}%")
            }
            Text(
                "${NotificationConfig.displayName(row.sourcePackage)} · ${row.correct}/${row.attempts} indovinati",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(2.dp))
        }
    }
}
