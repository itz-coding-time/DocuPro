package com.example.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import com.example.docupro.models.SettingsData
import com.example.docupro.utils.ShiftUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    // --- GHOST PROTOCOL ---
    // Recalculates who is available based on Shift boundaries and Action Taken
    val activeAssociates = remember(existingIncidents, associates, settings) {
        val shiftStart = ShiftUtils.getShiftStart(settings.shiftStart)
        val shiftEnd = ShiftUtils.getShiftEnd(settings.shiftStart, settings.shiftEnd)

        val terminatedIds = existingIncidents.filter {
            (it.action == "Dismissal from Work" || it.action == "Send Home") &&
                    LocalDateTime.parse(it.timestamp).isAfter(shiftStart) &&
                    LocalDateTime.parse(it.timestamp).isBefore(shiftEnd)
        }.map { it.associateId }.toSet()

        associates.filter { it.id !in terminatedIds }
    }

    var selectedAssociateId by remember { mutableStateOf("") }
    var violationType by remember { mutableStateOf("OSHA") }
    var details by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var cameraName by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("Warn") }
    var actionDetails by remember { mutableStateOf("") }
    var warnComplied by remember { mutableStateOf<Boolean?>(null) }
    var managerNotified by remember { mutableStateOf(false) }
    var timeLeftBuilding by remember { mutableStateOf("") }

    val systemTime = remember { LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Log Incident", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // --- GHOST PROTOCOL DROPDOWN ---
            var expanded by remember { mutableStateOf(false) }
            val selectedName = activeAssociates.find { it.id == selectedAssociateId }?.name ?: "Select Associate"

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedName, onValueChange = {}, readOnly = true,
                    label = { Text("Who?") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (activeAssociates.isEmpty()) {
                        DropdownMenuItem(text = { Text("No active associates remaining on shift.") }, onClick = { expanded = false })
                    } else {
                        activeAssociates.forEach { assoc ->
                            DropdownMenuItem(text = { Text(assoc.name) }, onClick = { selectedAssociateId = assoc.id; expanded = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Violation Type
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = violationType == "OSHA", onClick = { violationType = "OSHA" })
                Text("OSHA")
                Spacer(Modifier.width(16.dp))
                RadioButton(selected = violationType == "Hostility", onClick = { violationType = "Hostility" })
                Text("Hostility")
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details (What happened?)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Literal Location (e.g. Sales Floor)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = cameraName, onValueChange = { cameraName = it }, label = { Text("Camera Name (e.g. Camera 1)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            // Actions
            Text("Action Taken:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = actionTaken == "Warn", onClick = { actionTaken = "Warn" }, label = { Text("Warn") })
                FilterChip(selected = actionTaken == "Send Home", onClick = { actionTaken = "Send Home" }, label = { Text("Send Home") })
                FilterChip(selected = actionTaken == "Dismissal from Work", onClick = { actionTaken = "Dismissal from Work" }, label = { Text("Dismiss") })
            }

            OutlinedTextField(value = actionDetails, onValueChange = { actionDetails = it }, label = { Text("Action Notes (What was said?)") }, modifier = Modifier.fillMaxWidth())

            // Dynamic extra fields based on action
            if (actionTaken == "Warn") {
                Text("Did they comply?", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = warnComplied == true, onClick = { warnComplied = true })
                    Text("Yes")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = warnComplied == false, onClick = { warnComplied = false })
                    Text("No")
                }
            } else if (actionTaken == "Send Home") {
                OutlinedTextField(
                    value = timeLeftBuilding,
                    onValueChange = { timeLeftBuilding = it },
                    label = { Text("What time did they actually leave?") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Manager Notification Tracker
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Checkbox(checked = managerNotified, onCheckedChange = { managerNotified = it })
                Text("Manager Notified")
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (selectedAssociateId.isNotEmpty() && details.isNotEmpty()) {
                        onSave(Incident(
                            associateId = selectedAssociateId,
                            type = violationType,
                            details = details,
                            timestamp = LocalDateTime.now().toString(),
                            location = location,
                            action = actionTaken,
                            actionDetails = actionDetails,
                            cameraFriendlyName = cameraName,
                            complied = if (actionTaken == "Warn") warnComplied else null,
                            timeLeftBuilding = if (actionTaken == "Send Home") timeLeftBuilding else "",
                            managerNotified = managerNotified
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Incident") }
            Spacer(Modifier.height(32.dp))
        }
    }
}