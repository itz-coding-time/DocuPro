package com.example.docupro.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LogsView(incidents: List<Incident>, associates: List<Associate>) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Incident Logs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            IconButton(onClick = {
                Toast.makeText(
                    context,
                    "Continuous recorder active. Clear App Cache in Android settings to wipe logs.",
                    Toast.LENGTH_LONG
                ).show()
            }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear Logs Info",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (incidents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No incidents logged yet.", color = Color.Gray)
            }
        } else {
            val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                val groupedIncidents = incidents.groupBy { it.associateId }

                groupedIncidents.forEach { (associateId, associateIncidents) ->
                    val associateName = associates.find { it.id == associateId }?.name ?: "Unknown Associate"
                    val isExpanded = expandedStates[associateId] == true

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { expandedStates[associateId] = !isExpanded }
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(associateName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("${associateIncidents.size} recorded incidents", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Expand",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        items(associateIncidents.sortedByDescending { it.timestamp }) { incident ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(incident.type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.weight(1f))

                                        val timeStr = try {
                                            val dt = LocalDateTime.parse(incident.timestamp)
                                            val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                                            dt.format(formatter)
                                        } catch (e: Exception) { incident.timestamp.take(10) }

                                        Text(timeStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(incident.details, style = MaterialTheme.typography.bodyMedium)

                                    if (incident.actionDetails.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Text("Notes: ${incident.actionDetails}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(incident.details))
                                                Toast.makeText(context, "Narrative Copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("COPY NARRATIVE", style = MaterialTheme.typography.labelSmall)
                                        }

                                        FilterChip(
                                            selected = true,
                                            onClick = { },
                                            label = { Text(incident.action, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (incident.action == "Dismissal from Work") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = if (incident.action == "Dismissal from Work") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
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
    }
}