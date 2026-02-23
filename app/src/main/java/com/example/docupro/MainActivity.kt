package com.example.docupro


import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

// ... existing code ...
// ... existing code ...
@Composable
fun LogsView(incidents: List<Incident>, associates: List<Associate>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Incident Logs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            // Dummy Clear Logs Button
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
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                // QOL: Bundle reports by Associate!
                val groupedIncidents = incidents.groupBy { it.associateId }

                groupedIncidents.forEach { (associateId, associateIncidents) ->
                    val associateName = associates.find { it.id == associateId }?.name ?: "Unknown Associate"

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(androidx.compose.material.icons.Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(associateName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.weight(1f))
                                Badge { Text("${associateIncidents.size}") }
                            }
                        }
                    }

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

                                    // Format timestamp nicely
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
                                    // QOL: Copy to Clipboard for easy pasting into standard systems!
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
// ... existing code ...
@Composable
fun StatementsList(incidents: List<Incident>, associates: List<Associate>, settings: SettingsData, onExport: (String, String) -> Unit) {
    // Grab the context so we can read the asset file
    val context = androidx.compose.ui.platform.LocalContext.current

    // Group incidents by active associates
    val associatesWithIncidents = associates.filter { assoc -> incidents.any { it.associateId == assoc.id } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(associatesWithIncidents) { assoc ->
            val assocIncidents = incidents.filter { it.associateId == assoc.id }
            val isDismissed = assocIncidents.any { it.action == "Dismissal from Work" }

            Card(Modifier.fillMaxWidth().clickable {
                // Pass the Context, the List of Incidents, the Associate, and Settings
                val txt = StatementGenerator.generateCombinedStatement(context, assocIncidents, assoc, settings)
                onExport(txt, "Combined_Statement_${assoc.name.replace(" ", "_")}.txt")
            }) {
                ListItem(
                    headlineContent = { Text("Generate Statement for ${assoc.name}") },
                    supportingContent = { Text("Contains ${assocIncidents.size} incidents ${if (isDismissed) "(Including Dismissal)" else ""}") },
                    trailingContent = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}