package com.example.docupro.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Associate
import com.example.docupro.models.SettingsData
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

    // State for General Settings
    var shiftStart by remember { mutableStateOf(settings.shiftStart) }
    var shiftEnd by remember { mutableStateOf(settings.shiftEnd) }
    var storeNum by remember { mutableStateOf(settings.storeNumber) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }

    // THE IMPORTER LAUNCHER
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Try importing from the file!
            val importedAssociates = ScheduleImporter.importFromExcel(context, it)
            if (importedAssociates.isNotEmpty()) {
                // Combine existing with newly imported, avoiding duplicate EEIDs
                val combined = (associates + importedAssociates).distinctBy { assoc -> assoc.eeid }
                onAssociatesSaved(combined)
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text("General Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = storeNum, onValueChange = { storeNum = it; onSettingsSaved(settings.copy(storeNumber = it)) }, label = { Text("Store Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = shiftStart, onValueChange = { shiftStart = it; onSettingsSaved(settings.copy(shiftStart = it)) }, label = { Text("Standard Shift Start (HH:mm)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = shiftEnd, onValueChange = { shiftEnd = it; onSettingsSaved(settings.copy(shiftEnd = it)) }, label = { Text("Standard Shift End (HH:mm)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            Text("App Theme Override", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { index, text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == index, onClick = { themeMode = index; onSettingsSaved(settings.copy(themeMode = index)) })
                        Text(text)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { associatesExpanded = !associatesExpanded }.padding(vertical = 8.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Associate Manifest", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand")
                }
            }
            if (associatesExpanded) {
                // THE IMPORT BUTTON
                Button(
                    onClick = { filePickerLauncher.launch("*/*") }, // Allows picking xlsx or csv
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Import Schedule (.xlsx / .csv)")
                }

                // List the associates below it
                associates.forEach { assoc ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(assoc.name, fontWeight = FontWeight.Bold)
                            Text("EEID: ${assoc.eeid}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onAssociatesSaved(associates.filter { it.id != assoc.id }) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}