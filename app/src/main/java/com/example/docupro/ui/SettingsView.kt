//DocuPro: Documentation for the modern Sheetz Supervisor.
//Dreamt up by Brandon Case,
//Brought to life by Google's Gemini.
//Thank you Google for enabling this project of mine.

package com.example.docupro.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.Camera
import com.example.docupro.models.SettingsData
import com.example.docupro.utils.ConfigManager
import com.example.docupro.utils.ScheduleImporter

@Composable
fun SettingsView(
    settings: SettingsData,
    associates: List<Associate>,
    onSettingsSaved: (SettingsData) -> Unit,
    onAssociatesSaved: (List<Associate>) -> Unit
) {
    val context = LocalContext.current
    var associatesExpanded by remember { mutableStateOf(false) }
    var camerasExpanded by remember { mutableStateOf(false) }
    var locationsExpanded by remember { mutableStateOf(false) }

    var newCameraName by remember { mutableStateOf("") }
    var newLocationName by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualEEID by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uriContent ->
            val importedAssociates = try {
                ScheduleImporter.importFromExcel(context, uriContent)
            } catch (_: Exception) {
                emptyList()
            }

            if (importedAssociates.isNotEmpty()) {
                val combined = (associates + importedAssociates).distinctBy { it.eeid }
                onAssociatesSaved(combined)
            }
        }
    }

    // --- JSON CONFIG EXPORT LAUNCHER ---
    val exportConfigLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            val success = ConfigManager.exportConfig(context, it, settings, associates)
            if (success) {
                Toast.makeText(context, "Config Exported Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Export Failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- JSON CONFIG IMPORT LAUNCHER ---
    val importConfigLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val importedData = ConfigManager.importConfig(context, it)
            if (importedData != null) {
                // Reactive UI: When we call these, MainActivity automatically saves them to SharedPreferences!
                onSettingsSaved(importedData.settings)
                onAssociatesSaved(importedData.associates)
                Toast.makeText(context, "Config Imported Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Import Failed or Invalid File.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // --- THEME SETTINGS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Force Dark Mode", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = settings.themeMode == 2,
                    onCheckedChange = { isDark ->
                        onSettingsSaved(settings.copy(themeMode = if (isDark) 2 else 1))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // We wrap the rest in the LazyColumn and give it weight(1f) to prevent UI overlap
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Text("General Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.storeNumber,
                    onValueChange = { onSettingsSaved(settings.copy(storeNumber = it)) },
                    label = { Text("Store Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // --- SHIFT TIME SCHEDULER ---
                Text("Shift Schedule (24h Time)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val parts = settings.shiftStart.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            android.app.TimePickerDialog(context, { _, hh, mm ->
                                onSettingsSaved(settings.copy(shiftStart = String.format("%02d:%02d", hh, mm)))
                            }, h, m, true).show() // true = 24h format
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start: ${settings.shiftStart}") }

                    OutlinedButton(
                        onClick = {
                            val parts = settings.shiftEnd.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
                            android.app.TimePickerDialog(context, { _, hh, mm ->
                                onSettingsSaved(settings.copy(shiftEnd = String.format("%02d:%02d", hh, mm)))
                            }, h, m, true).show() // true = 24h format
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("End: ${settings.shiftEnd}") }
                }

                // --- DEBUG BYPASS ---
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Checkbox(
                        checked = settings.debugBypassShiftTime,
                        onCheckedChange = { onSettingsSaved(settings.copy(debugBypassShiftTime = it)) }
                    )
                    Text("Bypass Shift Time Restrictions (Debug Mode)", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }

            // --- CAMERA MANIFEST ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { camerasExpanded = !camerasExpanded }.padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Camera Manifest", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (camerasExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown, null)
                    }
                }
            }

            if (camerasExpanded) {
                item {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCameraName,
                            onValueChange = { newCameraName = it },
                            label = { Text("Add Camera (e.g. Sales Floor 1)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (newCameraName.isNotBlank()) {
                                val newCam = Camera(friendlyName = newCameraName.trim())
                                onSettingsSaved(settings.copy(cameraPresets = settings.cameraPresets + newCam))
                                newCameraName = ""
                            }
                        }) { Icon(Icons.Default.Add, null) }
                    }
                }
                items(settings.cameraPresets) { camera ->
                    ListItem(
                        headlineContent = { Text(camera.friendlyName) },
                        trailingContent = {
                            IconButton(onClick = {
                                onSettingsSaved(settings.copy(cameraPresets = settings.cameraPresets.filter { item -> item.id != camera.id }))
                            }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            // --- LOCATION MANIFEST ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { locationsExpanded = !locationsExpanded }.padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Location Tags", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (locationsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown, null)
                    }
                }
            }

            if (locationsExpanded) {
                item {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newLocationName,
                            onValueChange = { newLocationName = it },
                            label = { Text("Add Spot (e.g. Bread rack)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (newLocationName.isNotBlank()) {
                                onSettingsSaved(settings.copy(locationPresets = settings.locationPresets + newLocationName.trim()))
                                newLocationName = ""
                            }
                        }) { Icon(Icons.Default.Add, null) }
                    }
                }
                items(settings.locationPresets) { loc ->
                    ListItem(
                        headlineContent = { Text(loc) },
                        trailingContent = {
                            IconButton(onClick = {
                                onSettingsSaved(settings.copy(locationPresets = settings.locationPresets.filter { it != loc }))
                            }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            // --- ASSOCIATE MANIFEST ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { associatesExpanded = !associatesExpanded }.padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Associate Manifest", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (associatesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown, null)
                    }
                }
            }

            if (associatesExpanded) {
                item {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Manual Add", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(value = manualEEID, onValueChange = { manualEEID = it }, label = { Text("EEID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            }
                            IconButton(onClick = {
                                if (manualName.isNotBlank() && manualEEID.isNotBlank()) {
                                    onAssociatesSaved(associates + Associate(name = manualName.trim(), eeid = manualEEID.trim()))
                                    manualName = ""
                                    manualEEID = ""
                                }
                            }) { Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import Schedule (XLSX)")
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    }
                }
                items(associates) { assoc ->
                    ListItem(
                        headlineContent = { Text(assoc.name) },
                        supportingContent = { Text("ID: ${assoc.eeid}") },
                        trailingContent = {
                            IconButton(onClick = { onAssociatesSaved(associates.filter { item -> item.id != assoc.id }) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            // --- CONFIGURATION BACKUP & RESTORE ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Text("Backup & Restore Config", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Save a 'Golden Config' (Associates, Locations, Cameras) to your phone, or load an existing one. Incident Logs are not affected by configuration changes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportConfigLauncher.launch("DocuPro_Config.json") },
                                modifier = Modifier.weight(1f)
                            ) { Text("Export Config") }

                            Button(
                                onClick = { importConfigLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Import Config") }
                        }
                    }
                }
            }
        }
    }
}