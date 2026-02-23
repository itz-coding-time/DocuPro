package com.example.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.Incident

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkMapScreen(incidents: List<Incident>, associates: List<Associate>) {
    val associateMap = associates.associateBy { it.id }

    // Tally relationship counts
    val reportsAgainst = mutableMapOf<String, MutableMap<String, Int>>() // Target ID -> (Reporter ID -> Count)
    val reportedBy = mutableMapOf<String, MutableMap<String, Int>>()     // Reporter ID -> (Target ID -> Count)

    incidents.forEach { incident ->
        val targetId = incident.associateId
        val reporterId = incident.reporterId

        if (reporterId != null && reporterId != targetId) {
            val againstTarget = reportsAgainst.getOrPut(targetId) { mutableMapOf() }
            againstTarget[reporterId] = againstTarget.getOrDefault(reporterId, 0) + 1

            val byReporter = reportedBy.getOrPut(reporterId) { mutableMapOf() }
            byReporter[targetId] = byReporter.getOrDefault(targetId, 0) + 1
        }
    }

    // Filter to only show associates with network activity
    val activeNetworkIds = (reportsAgainst.keys + reportedBy.keys).distinct()

    if (activeNetworkIds.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reporting relationships found yet.", color = Color.Gray)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Incident Network Map", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Visualize who is reporting whom to identify patterns.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        }

        items(activeNetworkIds) { assocId ->
            val assocName = associateMap[assocId]?.name ?: "Unknown/Deleted Associate"
            val reportsMade = reportedBy[assocId] ?: emptyMap()
            val reportsReceived = reportsAgainst[assocId] ?: emptyMap()

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(assocName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    if (reportsMade.isNotEmpty()) {
                        Text("Reported Others:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            reportsMade.forEach { (targetId, count) ->
                                val targetName = associateMap[targetId]?.name ?: "Unknown"
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("$targetName ($count)") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (reportsReceived.isNotEmpty()) {
                        Text("Reported By:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            reportsReceived.forEach { (reporterId, count) ->
                                val reporterName = associateMap[reporterId]?.name ?: "Unknown"
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("$reporterName ($count)") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}