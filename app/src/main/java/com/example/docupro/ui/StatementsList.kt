package com.example.docupro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import com.example.docupro.models.SettingsData
import com.example.docupro.utils.StatementGenerator
import java.time.LocalDateTime

@Composable
fun StatementsList(incidents: List<Incident>, associates: List<Associate>, settings: SettingsData, onExport: (String, String) -> Unit) {
    val context = LocalContext.current
    val associatesWithIncidents = associates.filter { assoc -> incidents.any { it.associateId == assoc.id } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(associatesWithIncidents) { assoc ->
            val assocIncidents = incidents.filter { it.associateId == assoc.id }
            val isDismissed = assocIncidents.any { it.action == "Dismissal from Work" }

            // Calculate Day-to-Day vs Lifetime
            val today = LocalDateTime.now().toLocalDate()
            val todayIncidents = assocIncidents.filter {
                try { LocalDateTime.parse(it.timestamp).toLocalDate() == today } catch(e: Exception) { false }
            }

            var expanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(assoc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Total Incidents: ${assocIncidents.size} | Today: ${todayIncidents.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            if (isDismissed) {
                                Text("(Includes Dismissal from Work)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(Modifier.padding(bottom = 16.dp))

                            Button(
                                onClick = {
                                    val txt = StatementGenerator.generateCombinedStatement(context, todayIncidents, assoc, settings)
                                    onExport(txt, "Daily_Statement_${assoc.name.replace(" ", "_")}_$today.txt")
                                },
                                enabled = todayIncidents.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Email, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Daily Statement (${todayIncidents.size})")
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val txt = StatementGenerator.generateCombinedStatement(context, assocIncidents, assoc, settings)
                                    onExport(txt, "Lifetime_Statement_${assoc.name.replace(" ", "_")}.txt")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Lifetime Statement (${assocIncidents.size})")
                            }
                        }
                    }
                }
            }
        }
    }
}