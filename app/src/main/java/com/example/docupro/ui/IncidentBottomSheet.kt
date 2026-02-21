package com.example.docupro.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import com.example.docupro.models.SettingsData
import com.example.docupro.utils.ShiftUtils
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentBottomSheet(
    associates: List<Associate>,
    settings: SettingsData,
    existingIncidents: List<Incident>,
    onDismiss: () -> Unit,
    onSave: (Incident) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Evaluate Shift Time constraints
    val inShift = remember(settings) { ShiftUtils.isCurrentlyInShift(settings.shiftStart, settings.shiftEnd) }
    val canReport = inShift || settings.debugBypassShiftTime

    // --- ENHANCED GHOST PROTOCOL ---
    val activeAssociates = remember(existingIncidents, associates, settings) {
        val shiftStart = ShiftUtils.getShiftStart(settings.shiftStart)
        val shiftEnd = ShiftUtils.getShiftEnd(settings.shiftStart, settings.shiftEnd)

        val terminatedIds = existingIncidents.filter {
            it.action == "Dismissal from Work" &&
                    LocalDateTime.parse(it.timestamp).isAfter(shiftStart) &&
                    LocalDateTime.parse(it.timestamp).isBefore(shiftEnd)
        }.map { it.associateId }.toSet()

        associates.filter { it.name.isNotBlank() && it.id !in terminatedIds }
    }

    var selectedAssociateId by remember { mutableStateOf("") }
    var violationType by remember { mutableStateOf("OSHA") }

    // --- NARRATIVE BUILDER STATES ---
    var reportMode by remember { mutableStateOf("Reported") } // Default to Reported as requested
    var manualDetails by remember { mutableStateOf("") }
    var reporterName by remember { mutableStateOf("") }
    var actionObserved by remember { mutableStateOf("") }
    var postAction by remember { mutableStateOf("") }
    var correctionGiven by remember { mutableStateOf("") }

    var location by remember { mutableStateOf("") }
    var cameraName by remember { mutableStateOf("") }
    var witnesses by remember { mutableStateOf("") }
    var actionDetails by remember { mutableStateOf("") }
    var warnComplied by remember { mutableStateOf<Boolean?>(null) }
    var managerNotified by remember { mutableStateOf(false) }
    var timeLeftBuilding by remember { mutableStateOf("") }

    val selectedName = activeAssociates.find { it.id == selectedAssociateId }?.name ?: "[Associate]"

    // --- PROGRESSIVE DISCIPLINE CALCULATIONS ---
    val historyCount = remember(selectedAssociateId, violationType, existingIncidents) {
        if (selectedAssociateId.isEmpty()) 0
        else existingIncidents.count {
            // Only count previous incidents that were actually acted upon (Warn/Dismiss), not just unwitnessed logs
            it.associateId == selectedAssociateId && it.type == violationType && it.action != "Logged"
        }
    }

    val actionTaken = remember(historyCount, violationType, selectedAssociateId, reportMode) {
        // If it's unwitnessed ("Reported"), we cannot issue a corrective action.
        if (reportMode == "Reported") "Logged"
        else if (selectedAssociateId.isEmpty()) "Warn"
        else when (violationType) {
            "OSHA" -> if (historyCount == 0) "Warn" else "Dismissal from Work"
            "Hostility" -> if (historyCount < 2) "Warn" else "Dismissal from Work"
            else -> "Warn"
        }
    }

    // Dynamic generated string based on selected Report Type
    val generatedDetails = remember(reportMode, selectedName, reporterName, actionObserved, postAction, correctionGiven, manualDetails) {
        val rep = reporterName.ifBlank { "[Reporter]" }
        val act = actionObserved.ifBlank { "[Action]" }
        val postAct = postAction.ifBlank { "[Post Action]" }
        val corr = correctionGiven.ifBlank { "[Correction]" }

        when (reportMode) {
            "Manual" -> manualDetails
            "Witnessed" -> "MOD witnessed Associate $selectedName $act. MOD Corrected Associate $selectedName, \"$corr\" Logged."
            "Reported" -> "Associate $rep reported to MOD that Associate $selectedName was $act. When MOD went to check, Associate $selectedName was $postAct. Cannot Correct, but logged."
            "Both" -> "Associate $rep reported to MOD that Associate $selectedName was $act. When MOD went to check, Associate $selectedName was still $act. MOD corrected Associate $selectedName \"$corr\". Logged."
            else -> manualDetails
        }
    }

    val chipColor = remember { Animatable(Color.Gray) }
    LaunchedEffect(selectedAssociateId, violationType, historyCount) {
        if (selectedAssociateId.isEmpty()) {
            chipColor.snapTo(Color.Gray)
            return@LaunchedEffect
        }
        when (violationType) {
            "OSHA" -> {
                if (historyCount == 0) chipColor.animateTo(Color(0xFFFF0000), tween(300))
                else {
                    for (i in 0..2) { chipColor.animateTo(Color(0xFFFF0000), tween(150)); chipColor.animateTo(Color.LightGray, tween(150)) }
                    chipColor.animateTo(Color.Gray, tween(300))
                }
            }
            "Hostility" -> {
                if (historyCount == 0) chipColor.animateTo(Color(0xFFEAB308), tween(300))
                else if (historyCount == 1) chipColor.animateTo(Color(0xFFFF0000), tween(300))
                else {
                    for (i in 0..2) { chipColor.animateTo(Color(0xFFFF0000), tween(150)); chipColor.animateTo(Color.LightGray, tween(150)) }
                    chipColor.animateTo(Color.Gray, tween(300))
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (!canReport) {
            Column(Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("Out of Shift Hours", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("DocuPro utilizes Active Reporting. You cannot make reports against associates outside of your scheduled shift parameters (${settings.shiftStart} - ${settings.shiftEnd}).", textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss) { Text("Acknowledge") }
                Spacer(Modifier.height(32.dp))
            }
        } else {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Log Incident", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                var expandedAssoc by remember { mutableStateOf(false) }
                val displaySelectedName = activeAssociates.find { it.id == selectedAssociateId }?.name ?: "Select Associate"

                ExposedDropdownMenuBox(expanded = expandedAssoc, onExpandedChange = { expandedAssoc = !expandedAssoc }) {
                    OutlinedTextField(
                        value = displaySelectedName, onValueChange = {}, readOnly = true,
                        label = { Text("Associate") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAssoc) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedAssoc, onDismissRequest = { expandedAssoc = false }) {
                        if (activeAssociates.isEmpty()) {
                            DropdownMenuItem(text = { Text("No active associates remaining.") }, onClick = { expandedAssoc = false })
                        } else {
                            activeAssociates.forEach { assoc ->
                                DropdownMenuItem(text = { Text(assoc.name) }, onClick = { selectedAssociateId = assoc.id; expandedAssoc = false })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = violationType == "OSHA", onClick = { violationType = "OSHA" })
                    Text("OSHA")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = violationType == "Hostility", onClick = { violationType = "Hostility" })
                    Text("Hostility")
                }
                Spacer(Modifier.height(8.dp))

                // --- NARRATIVE BUILDER UI ---
                Text("Report Builder", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Manual", "Witnessed", "Reported", "Both").forEach { mode ->
                        FilterChip(
                            selected = reportMode == mode,
                            onClick = { reportMode = mode },
                            label = { Text(if (mode == "Both") "Reported & Witnessed" else mode) }
                        )
                    }
                }

                if (reportMode == "Manual") {
                    OutlinedTextField(value = manualDetails, onValueChange = { manualDetails = it }, label = { Text("Details (What happened?)") }, modifier = Modifier.fillMaxWidth())
                } else {
                    if (reportMode == "Reported" || reportMode == "Both") {
                        OutlinedTextField(value = reporterName, onValueChange = { reporterName = it }, label = { Text("Reporting Associate Name") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(value = actionObserved, onValueChange = { actionObserved = it }, label = { Text("Action (e.g. climbing on the equipment)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    if (reportMode == "Reported") {
                        OutlinedTextField(value = postAction, onValueChange = { postAction = it }, label = { Text("Status upon checking (e.g. off of the equipment)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }

                    if (reportMode == "Witnessed" || reportMode == "Both") {
                        OutlinedTextField(value = correctionGiven, onValueChange = { correctionGiven = it }, label = { Text("Correction Given") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Generated Narrative Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(generatedDetails, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Literal Location (e.g. Sales Floor)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                var expandedCam by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedCam, onExpandedChange = { expandedCam = !expandedCam }) {
                    OutlinedTextField(
                        value = cameraName, onValueChange = { cameraName = it },
                        label = { Text("Camera Selection") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCam) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (settings.cameraPresets.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expandedCam, onDismissRequest = { expandedCam = false }) {
                            settings.cameraPresets.forEach { cam -> DropdownMenuItem(text = { Text(cam.friendlyName) }, onClick = { cameraName = cam.friendlyName; expandedCam = false }) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = witnesses, onValueChange = { witnesses = it }, label = { Text("Witnesses (if any)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (actionTaken == "Dismissal from Work") {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Required Action:", fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (reportMode == "Reported") {
                                // Locked state for unwitnessed reports
                                FilterChip(
                                    selected = true, onClick = { }, label = { Text("Logged Only (Unwitnessed)") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                )
                            } else {
                                // Normal corrective actions for Witnessed/Both/Manual
                                val textColor = if (chipColor.value.luminance() > 0.5f) Color.Black else Color.White
                                FilterChip(
                                    selected = actionTaken == "Warn", onClick = { }, label = { Text("Warn") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (actionTaken == "Warn") chipColor.value else MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = if (actionTaken == "Warn") textColor else MaterialTheme.colorScheme.onSecondaryContainer)
                                )
                                FilterChip(
                                    selected = actionTaken == "Dismissal from Work", onClick = { }, label = { Text("Dismiss") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (actionTaken == "Dismissal from Work") chipColor.value else MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = if (actionTaken == "Dismissal from Work") textColor else MaterialTheme.colorScheme.onSecondaryContainer)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = actionDetails, onValueChange = { actionDetails = it }, label = { Text("Action Notes (What was said?)") }, modifier = Modifier.fillMaxWidth())

                // These dynamic questions are naturally hidden when reportMode is "Reported" because actionTaken evaluates to "Logged"
                if (actionTaken == "Warn") {
                    Text("Did they comply?", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = warnComplied == true, onClick = { warnComplied = true })
                        Text("Yes")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = warnComplied == false, onClick = { warnComplied = false })
                        Text("No")
                    }
                } else if (actionTaken == "Dismissal from Work") {
                    OutlinedTextField(value = timeLeftBuilding, onValueChange = { timeLeftBuilding = it }, label = { Text("What time did they actually leave?") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = managerNotified, onCheckedChange = { managerNotified = it })
                    Text("Manager Notified")
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (selectedAssociateId.isNotEmpty() && generatedDetails.isNotBlank()) {
                            onSave(Incident(
                                associateId = selectedAssociateId, type = violationType, details = generatedDetails, timestamp = LocalDateTime.now().toString(),
                                location = location, action = actionTaken, actionDetails = actionDetails, cameraFriendlyName = cameraName,
                                witnesses = witnesses, complied = if (actionTaken == "Warn") warnComplied else null,
                                timeLeftBuilding = if (actionTaken == "Dismissal from Work") timeLeftBuilding else "", managerNotified = managerNotified
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Incident") }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}