package com.example.docupro

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.core.content.edit
import com.example.docupro.models.*
import com.example.docupro.ui.*
import com.example.docupro.utils.StatementGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private val gson = Gson()

    private var exportContent: String = ""
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let {
            contentResolver.openOutputStream(it)?.use { out -> out.write(exportContent.toByteArray()) }
            Toast.makeText(this, "Statement Exported Successfully", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = getSharedPreferences("DocuProPrefs", MODE_PRIVATE)

        setContent {
            var settings by remember { mutableStateOf(loadSettings()) }
            var associates by remember { mutableStateOf(loadAssociates()) }
            var incidents by remember { mutableStateOf(loadIncidents()) }
            var currentScreen by remember { mutableStateOf("Home") }
            var showIncidentForm by remember { mutableStateOf(false) }

            MaterialTheme(
                colorScheme = if (settings.themeMode == 2) darkColorScheme() else lightColorScheme()
            ) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(24.dp))
                            Text("DocuPro", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            HorizontalDivider()
                            NavigationDrawerItem(label = { Text("Home") }, selected = currentScreen == "Home", onClick = { currentScreen = "Home"; scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.Home, null) })
                            NavigationDrawerItem(label = { Text("Logs") }, selected = currentScreen == "Logs", onClick = { currentScreen = "Logs"; scope.launch { drawerState.close() } }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) })

                            // NEW: Route to Network Map
                            NavigationDrawerItem(label = { Text("Network Map") }, selected = currentScreen == "Network", onClick = { currentScreen = "Network"; scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.Share, null) })

                            NavigationDrawerItem(label = { Text("Statements") }, selected = currentScreen == "Statements", onClick = { currentScreen = "Statements"; scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.Email, null) })
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            NavigationDrawerItem(label = { Text("Settings") }, selected = currentScreen == "Settings", onClick = { currentScreen = "Settings"; scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.Settings, null) })
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(currentScreen) },
                                navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null) } },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        },
                        floatingActionButton = {
                            if (currentScreen == "Home") {
                                FloatingActionButton(onClick = { showIncidentForm = true }) { Icon(Icons.Default.Add, null) }
                            }
                        }
                    ) { p ->
                        Box(Modifier.padding(p).fillMaxSize()) {
                            when (currentScreen) {
                                "Home" -> HomeDashboard(incidents)
                                "Logs" -> LogsView (incidents, associates)
                                "Network" -> NetworkMapScreen(incidents, associates)
                                "Statements" -> StatementsList(incidents, associates, settings) { txt, name ->
                                    exportContent = txt
                                    createDocumentLauncher.launch(name)
                                }
                                "Settings" -> SettingsView(
                                    settings = settings,
                                    associates = associates,
                                    onSettingsSaved = { settings = it; saveSettings(it) },
                                    onAssociatesSaved = { associates = it; saveAssociates(it) }
                                )
                            }
                        }

                        if (showIncidentForm) {
                            IncidentBottomSheet(
                                associates = associates,
                                settings = settings,
                                existingIncidents = incidents,
                                onDismiss = { showIncidentForm = false },
                                onSave = { inc ->
                                    incidents = incidents + inc
                                    saveIncidents(incidents)
                                    showIncidentForm = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadSettings() = gson.fromJson(sharedPrefs.getString("settings", "{}"), SettingsData::class.java) ?: SettingsData()
    private fun saveSettings(s: SettingsData) = sharedPrefs.edit { putString("settings", gson.toJson(s)) }
    private fun loadAssociates(): List<Associate> = gson.fromJson(sharedPrefs.getString("associates", "[]"), object : TypeToken<List<Associate>>() {}.type)
    private fun saveAssociates(a: List<Associate>) = sharedPrefs.edit { putString("associates", gson.toJson(a)) }
    private fun loadIncidents(): List<Incident> = gson.fromJson(sharedPrefs.getString("incidents", "[]"), object : TypeToken<List<Incident>>() {}.type)
    private fun saveIncidents(i: List<Incident>) = sharedPrefs.edit { putString("incidents", gson.toJson(i)) }
}

@Composable
fun HomeDashboard(incidents: List<Incident>) {
    val today = LocalDateTime.now().toLocalDate()
    val count = incidents.count { LocalDateTime.parse(it.timestamp).toLocalDate() == today }

    Column(Modifier.padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Night Shift Overview", color = Color.Gray)

        Card(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("INCIDENTS TONIGHT", style = MaterialTheme.typography.labelSmall)
                Text("$count", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LogsView(incidents: List<Incident>, associates: List<Associate>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Incident Logs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            IconButton(onClick = {
                android.widget.Toast.makeText(
                    context,
                    "Continuous recorder active. Clear App Cache in Android settings to wipe logs.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Clear Logs Info",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (incidents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No incidents logged yet.", color = androidx.compose.ui.graphics.Color.Gray)
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
                                Icon(androidx.compose.material.icons.Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
                                            val dt = java.time.LocalDateTime.parse(incident.timestamp)
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                                            dt.format(formatter)
                                        } catch (e: Exception) { incident.timestamp.take(10) }

                                        Text(timeStr, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(incident.details, style = MaterialTheme.typography.bodyMedium)

                                    if (incident.actionDetails.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Text("Notes: ${incident.actionDetails}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(incident.details))
                                                android.widget.Toast.makeText(context, "Narrative Copied!", android.widget.Toast.LENGTH_SHORT).show()
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

@Composable
fun StatementsList(incidents: List<Incident>, associates: List<Associate>, settings: SettingsData, onExport: (String, String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val associatesWithIncidents = associates.filter { assoc -> incidents.any { it.associateId == assoc.id } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(associatesWithIncidents) { assoc ->
            val assocIncidents = incidents.filter { it.associateId == assoc.id }
            val isDismissed = assocIncidents.any { it.action == "Dismissal from Work" }

            // Calculate Day-to-Day vs Lifetime
            val today = java.time.LocalDateTime.now().toLocalDate()
            val todayIncidents = assocIncidents.filter {
                try { java.time.LocalDateTime.parse(it.timestamp).toLocalDate() == today } catch(e: Exception) { false }
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