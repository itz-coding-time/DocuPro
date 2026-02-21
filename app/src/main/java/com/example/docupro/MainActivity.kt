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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// --- DATA MODELS ---
data class Associate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val eeid: String
)

data class Camera(
    val id: String = UUID.randomUUID().toString(),
    val friendlyName: String = "Camera",
    val literalPosition: String = "Literalposition",
    val cameraViewName: String = "CameraViewName"
)

/**
 * Incident model updated:
 * - Removed 'location' (Camera FriendlyName will serve as location)
 * - Removed send-home fields (Send Home removed)
 * - action values: "Coached" (OSHA coached), "Warn (Hostility)", "Warn (Hostility) - MGR Notified", "Dismissal from Work"
 * - added managerNotified flag for Hostility second warn
 */
data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val associateId: String,
    val type: String, // "OSHA" or "Hostility"
    val details: String,
    val timestamp: String,
    val action: String = "Coached", // default to coached for OSHA semantics
    val cameraFriendlyName: String = "None",
    val actionDetails: String = "",
    val didComply: Boolean? = null,
    val managerNotified: Boolean = false
)

data class SettingsData(
    val shiftStart: String = "21:00",
    val shiftEnd: String = "07:30",
    val storeNumber: String = "318",
    val themeMode: Int = 0, // 0: System, 1: Light, 2: Dark
    val cameras: List<Camera> = emptyList()
)

// Manager to notify on second Hostility warn
private const val MANAGER_NAME = "Jeff"

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private val gson = Gson()

    // File Picker Launcher for Document Export
    private var exportContent: String = ""
    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(exportContent.toByteArray())
                    Toast.makeText(this, "Statement Exported Successfully", Toast.LENGTH_LONG).show()
                }
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = getSharedPreferences("DocuProPrefs", Context.MODE_PRIVATE)

        setContent {
            // --- STATE MANAGEMENT ---
            var settings by remember { mutableStateOf(loadSettings()) }
            var associates by remember { mutableStateOf(loadAssociates()) }
            var incidents by remember { mutableStateOf(loadIncidents()) }

            // Theme override logic
            val isDarkTheme = when (settings.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf("Home") }
                var showIncidentPage by remember { mutableStateOf(false) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "DocuPro",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                            Divider()
                            NavigationDrawerItem(
                                label = { Text("Home") },
                                selected = currentScreen == "Home",
                                onClick = {
                                    currentScreen = "Home"
                                    showIncidentPage = false
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Home, null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("Logs") },
                                selected = currentScreen == "Logs",
                                onClick = {
                                    currentScreen = "Logs"
                                    showIncidentPage = false
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.List, null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("Generated Statements") },
                                selected = currentScreen == "Statements",
                                onClick = {
                                    currentScreen = "Statements"
                                    showIncidentPage = false
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Email, null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            Divider(Modifier.padding(vertical = 8.dp))
                            NavigationDrawerItem(
                                label = { Text("Settings") },
                                selected = currentScreen == "Settings",
                                onClick = {
                                    currentScreen = "Settings"
                                    showIncidentPage = false
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Settings, null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        when {
                                            showIncidentPage -> "Log Incident"
                                            else -> currentScreen
                                        }
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        if (showIncidentPage) {
                                            showIncidentPage = false
                                        } else {
                                            scope.launch { drawerState.open() }
                                        }
                                    }) {
                                        Icon(
                                            if (showIncidentPage) Icons.Default.ArrowBack else Icons.Default.Menu,
                                            contentDescription = "Menu"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        },
                        floatingActionButton = {
                            if (currentScreen == "Home" && !showIncidentPage) {
                                FloatingActionButton(onClick = { showIncidentPage = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Log Incident")
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                        ) {
                            if (showIncidentPage) {
                                IncidentPage(
                                    associates = associates,
                                    settings = settings,
                                    existingIncidents = incidents,
                                    onDismiss = { showIncidentPage = false },
                                    onSave = { newIncident ->
                                        val updated = incidents + newIncident
                                        incidents = updated
                                        saveIncidents(updated)
                                        showIncidentPage = false
                                    }
                                )
                            } else {
                                when (currentScreen) {
                                    "Home" -> HomeView(incidents)
                                    "Logs" -> LogsView(incidents, associates)
                                    "Statements" -> StatementsView(
                                        incidents,
                                        associates,
                                        settings
                                    ) { content, fileName ->
                                        exportContent = content
                                        createDocumentLauncher.launch(fileName)
                                    }

                                    "Settings" -> SettingsView(
                                        settings,
                                        associates,
                                        onSettingsSaved = { newSettings ->
                                            settings = newSettings
                                            saveSettings(newSettings)
                                        },
                                        onAssociatesSaved = { newAssociates ->
                                            associates = newAssociates
                                            saveAssociates(newAssociates)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SHARED PREFERENCES HELPER FUNCTIONS ---
    private fun loadAssociates(): List<Associate> {
        val json = sharedPrefs.getString("associates", "[]")
        return gson.fromJson(json, object : TypeToken<List<Associate>>() {}.type)
    }

    private fun saveAssociates(list: List<Associate>) =
        sharedPrefs.edit().putString("associates", gson.toJson(list)).apply()

    private fun loadIncidents(): List<Incident> {
        val json = sharedPrefs.getString("incidents", "[]")
        return gson.fromJson(json, object : TypeToken<List<Incident>>() {}.type)
    }

    private fun saveIncidents(list: List<Incident>) =
        sharedPrefs.edit().putString("incidents", gson.toJson(list)).apply()

    private fun loadSettings(): SettingsData {
        val json = sharedPrefs.getString("settings", gson.toJson(SettingsData()))
        return gson.fromJson(json, SettingsData::class.java)
    }

    private fun saveSettings(data: SettingsData) =
        sharedPrefs.edit().putString("settings", gson.toJson(data)).apply()
}

// --- VIEWS ---

@Composable
fun HomeView(incidents: List<Incident>) {
    val today = LocalDateTime.now().toLocalDate()
    val todayIncidents =
        incidents.filter { LocalDateTime.parse(it.timestamp).toLocalDate() == today }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Press the + button to log a new incident.",
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("INCIDENTS TODAY", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${todayIncidents.size}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun LogsView(incidents: List<Incident>, associates: List<Associate>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(incidents.reversed()) { incident ->
            val associate = associates.find { it.id == incident.associateId }?.name ?: "Unknown"
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(associate, fontWeight = FontWeight.Bold)
                        Text(
                            LocalDateTime.parse(incident.timestamp)
                                .format(DateTimeFormatter.ofPattern("MM/dd HH:mm")),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(
                            containerColor = if (incident.type == "OSHA") Color(0xFFEAB308) else Color(
                                0xFFEF4444
                            )
                        ) { Text(incident.type, modifier = Modifier.padding(4.dp)) }
                        Badge(
                            containerColor = when {
                                incident.action.startsWith("Coached") -> Color(0xFF3B82F6)
                                incident.action.contains("Warn") -> Color(0xFFF59E0B)
                                incident.action == "Dismissal from Work" -> Color.DarkGray
                                else -> Color.DarkGray
                            }
                        ) { Text(incident.action, modifier = Modifier.padding(4.dp)) }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Location is represented by camera friendly name
                    if (incident.cameraFriendlyName.isNotBlank() && incident.cameraFriendlyName != "None") {
                        Text("Camera: ${incident.cameraFriendlyName}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        incident.details,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                    if (incident.actionDetails.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Notes: ${incident.actionDetails}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StatementsView(
    incidents: List<Incident>,
    associates: List<Associate>,
    settings: SettingsData,
    onExport: (String, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(incidents.reversed()) { incident ->
            val associate = associates.find { it.id == incident.associateId }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (associate != null) {
                            val statementText = generateStatementText(incident, associate, settings)
                            val fileName =
                                "Statement_${associate.name.replace(" ", "_")}_${incident.timestamp.take(10)}.txt"
                            onExport(statementText, fileName)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Generate Statement for ${associate?.name ?: "Unknown"}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            LocalDateTime.parse(incident.timestamp)
                                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm")),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsView(
    settings: SettingsData,
    associates: List<Associate>,
    onSettingsSaved: (SettingsData) -> Unit,
    onAssociatesSaved: (List<Associate>) -> Unit
) {
    var shiftStart by remember { mutableStateOf(settings.shiftStart) }
    var shiftEnd by remember { mutableStateOf(settings.shiftEnd) }
    var storeNum by remember { mutableStateOf(settings.storeNumber) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var newAssocName by remember { mutableStateOf("") }
    var newAssocEeid by remember { mutableStateOf("") }

    var cameras by remember { mutableStateOf(settings.cameras.toMutableList()) }
    var newCameraFriendly by remember { mutableStateOf("") }
    var newCameraLiteral by remember { mutableStateOf("") }
    var newCameraView by remember { mutableStateOf("") }

    var associatesExpanded by remember { mutableStateOf(true) }
    var camerasExpanded by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                "General Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = storeNum,
                onValueChange = {
                    storeNum = it
                    onSettingsSaved(settings.copy(storeNumber = it, cameras = cameras))
                },
                label = { Text("Store Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = shiftStart,
                onValueChange = {
                    shiftStart = it
                    onSettingsSaved(settings.copy(shiftStart = it, cameras = cameras))
                },
                label = { Text("Standard Shift Start (HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = shiftEnd,
                onValueChange = {
                    shiftEnd = it
                    onSettingsSaved(settings.copy(shiftEnd = it, cameras = cameras))
                },
                label = { Text("Standard Shift End (HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text("App Theme Override", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                listOf("System", "Light", "Dark").forEachIndexed { index, text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeMode == index,
                            onClick = {
                                themeMode = index
                                onSettingsSaved(settings.copy(themeMode = index, cameras = cameras))
                            }
                        )
                        Text(text)
                    }
                }
            }
            Divider(Modifier.padding(vertical = 16.dp))
        }

        // Associates collapsible (above cameras)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { associatesExpanded = !associatesExpanded }
            ) {
                Text(
                    "Define Associates",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (associatesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            Spacer(Modifier.height(8.dp))
            if (associatesExpanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newAssocName,
                        onValueChange = { newAssocName = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newAssocEeid,
                        onValueChange = { newAssocEeid = it },
                        label = { Text("EEID") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (newAssocName.isNotBlank() && newAssocEeid.isNotBlank()) {
                            onAssociatesSaved(
                                associates + Associate(
                                    name = newAssocName,
                                    eeid = newAssocEeid
                                )
                            )
                            newAssocName = ""
                            newAssocEeid = ""
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Add") }
                }
                Spacer(Modifier.height(16.dp))

                associates.forEach { assoc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(assoc.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "EEID: ${assoc.eeid}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                onAssociatesSaved(associates.filter { it.id != assoc.id })
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Cameras collapsible
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { camerasExpanded = !camerasExpanded }
            ) {
                Text(
                    "Cameras",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (camerasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            Spacer(Modifier.height(8.dp))
            if (camerasExpanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCameraFriendly,
                        onValueChange = { newCameraFriendly = it },
                        label = { Text("Friendly Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newCameraLiteral,
                        onValueChange = { newCameraLiteral = it },
                        label = { Text("Literal Position") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newCameraView,
                    onValueChange = { newCameraView = it },
                    label = { Text("Camera View Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        val friendly =
                            if (newCameraFriendly.isBlank()) "Camera" else newCameraFriendly
                        val literal =
                            if (newCameraLiteral.isBlank()) "Literalposition" else newCameraLiteral
                        val view =
                            if (newCameraView.isBlank()) "CameraViewName" else newCameraView
                        cameras = (cameras + Camera(
                            friendlyName = friendly,
                            literalPosition = literal,
                            cameraViewName = view
                        )).toMutableList()
                        onSettingsSaved(settings.copy(cameras = cameras))
                        newCameraFriendly = ""
                        newCameraLiteral = ""
                        newCameraView = ""
                    }) {
                        Text("Add Camera")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        cameras = mutableListOf()
                        onSettingsSaved(settings.copy(cameras = cameras))
                    }) {
                        Text("Clear Cameras")
                    }
                }
                Spacer(Modifier.height(16.dp))

                cameras.forEach { cam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cam.friendlyName, fontWeight = FontWeight.Bold)
                                Text(
                                    "Position: ${cam.literalPosition} • View: ${cam.cameraViewName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                cameras = cameras.filter { it.id != cam.id }.toMutableList()
                                onSettingsSaved(settings.copy(cameras = cameras))
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// --- INCIDENT PAGE (FULL PAGE) ---
// Implements the flow rules:
// - OSHA: action label "Coached". If prior OSHA in shift exists -> Coached (Warn) disabled and Dismissal required.
// - Hostility: first warn allowed, second warn triggers manager notification (managerNotified=true), third occurrence -> Dismissal from Work.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentPage(
    associates: List<Associate>,
    settings: SettingsData,
    existingIncidents: List<Incident>,
    onDismiss: () -> Unit,
    onSave: (Incident) -> Unit
) {
    val context = LocalContext.current

    var selectedAssociateId by remember { mutableStateOf("") }
    var violationType by remember { mutableStateOf("OSHA") }
    var details by remember { mutableStateOf("") }

    var selectedCameraId by remember { mutableStateOf<String?>(null) }
    var cameraFriendlyName by remember { mutableStateOf("None") }

    var whatDidYouSay by remember { mutableStateOf("") }
    var didComply by remember { mutableStateOf<Boolean?>(null) }

    val now = remember { LocalDateTime.now() }
    val systemTime = remember {
        now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    // Determine counts for the current shift for this associate and violation type
    fun occurrencesThisShiftForType(associateId: String, type: String): Int {
        if (associateId.isEmpty()) return 0
        val shiftStart = getShiftStart(settings.shiftStart)
        val shiftEnd = getShiftEnd(settings.shiftStart, settings.shiftEnd)
        return existingIncidents.count {
            it.associateId == associateId &&
                    it.type == type &&
                    LocalDateTime.parse(it.timestamp).isAfter(shiftStart) &&
                    LocalDateTime.parse(it.timestamp).isBefore(shiftEnd)
        }
    }

    // isCoachedEnabled logic:
    // - For OSHA: if any prior OSHA in shift -> Coached disabled (no second chance)
    // - For Hostility: allow up to 2 warns; after 2 prior Hostility warns -> Coached disabled (Dismissal required)
    val isCoachedEnabled = remember(selectedAssociateId, violationType, didComply) {
        if (selectedAssociateId.isEmpty()) return@remember true
        val count = occurrencesThisShiftForType(selectedAssociateId, violationType)
        when (violationType) {
            "OSHA" -> count == 0 // if any prior OSHA, coached disabled
            "Hostility" -> count < 2 // allow up to 2 warns; third occurrence requires dismissal
            else -> true
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Log Incident",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Who
            var expanded by remember { mutableStateOf(false) }
            val selectedName =
                associates.find { it.id == selectedAssociateId }?.name ?: "Select Associate"
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Who?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    associates.forEach { assoc ->
                        DropdownMenuItem(
                            text = { Text(assoc.name) },
                            onClick = {
                                selectedAssociateId = assoc.id
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Camera selection (FriendlyName only)
            if (settings.cameras.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Camera:", modifier = Modifier.weight(0.3f))
                    Text(
                        "No cameras defined",
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        Toast.makeText(
                            context,
                            "Add cameras in Settings (FriendlyName, Literalposition, CameraViewName).",
                            Toast.LENGTH_LONG
                        ).show()
                    }) {
                        Text("Add A Camera? +")
                    }
                }
            } else {
                var camExpanded by remember { mutableStateOf(false) }
                val selectedCamLabel =
                    settings.cameras.find { it.id == selectedCameraId }?.friendlyName
                        ?: "Select Camera"
                ExposedDropdownMenuBox(
                    expanded = camExpanded,
                    onExpandedChange = { camExpanded = !camExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCamLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Camera (Friendly Name)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = camExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = camExpanded,
                        onDismissRequest = { camExpanded = false }
                    ) {
                        settings.cameras.forEach { cam ->
                            DropdownMenuItem(
                                text = { Text(cam.friendlyName) },
                                onClick = {
                                    selectedCameraId = cam.id
                                    cameraFriendlyName = cam.friendlyName
                                    camExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Add A Camera? +") },
                            onClick = {
                                camExpanded = false
                                Toast.makeText(
                                    context,
                                    "Add cameras in Settings (FriendlyName, Literalposition, CameraViewName).",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Violation Type
            Text("What Violation was committed?", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = violationType == "OSHA",
                    onClick = { violationType = "OSHA" }
                )
                Text("OSHA")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = violationType == "Hostility",
                    onClick = { violationType = "Hostility" }
                )
                Text("Hostility")
            }
            Spacer(Modifier.height(8.dp))

            // Details
            val detailLabel =
                if (violationType == "OSHA") "Which violation? Be specific in wording."
                else "What was said and to whom?"
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text(detailLabel) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Time Recorded (system)
            OutlinedTextField(
                value = systemTime,
                onValueChange = {},
                label = { Text("Time Recorded (System Auto)") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Action flow: Coached (OSHA) / Warn (Hostility) / escalation rules
            Text("What action did you take?", style = MaterialTheme.typography.labelMedium)
            Column {
                // Label text differs for OSHA vs Hostility
                val coachedLabel = if (violationType == "OSHA") "Coached (OSHA)" else "Warn (Hostility)"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = true && (/* we'll set action on save */ false),
                        onClick = { /* no-op: selection handled on save via flow */ },
                        enabled = false
                    )
                    Text("Action will be determined by flow rules below", color = Color.Gray)
                }

                Spacer(Modifier.height(8.dp))
                // Show current flow state and options for user to provide follow-ups
                if (violationType == "OSHA") {
                    // OSHA: single coached -> if prior OSHA exists in shift, coached disabled and Dismissal required
                    val priorCount = occurrencesThisShiftForTypeLocal(existingIncidents, selectedAssociateId, "OSHA", settings)
                    if (priorCount > 0) {
                        Text(
                            "OSHA violation already recorded this shift. Coached is disabled; Dismissal from Work required.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            "OSHA flow: Coached (one chance). If coached fails, escalate to Dismissal immediately.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = whatDidYouSay,
                        onValueChange = { whatDidYouSay = it },
                        label = { Text("What did you say to the associate? (Coaching language)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Did associate comply?", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = didComply == true, onClick = { didComply = true })
                        Text("Yes")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = didComply == false, onClick = { didComply = false })
                        Text("No")
                    }
                } else {
                    // Hostility flow: Warn -> Warn (Mgr notified) -> Dismissal
                    val priorCount = occurrencesThisShiftForTypeLocal(existingIncidents, selectedAssociateId, "Hostility", settings)
                    when (priorCount) {
                        0 -> {
                            Text("Hostility flow: First Warn allowed.", style = MaterialTheme.typography.bodySmall)
                        }
                        1 -> {
                            Text("Hostility flow: Second Warn will notify manager (${MANAGER_NAME}).", style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {
                            Text("Hostility flow: Limit reached this shift — Dismissal from Work required.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = whatDidYouSay,
                        onValueChange = { whatDidYouSay = it },
                        label = { Text("What did you say to the associate?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Did associate comply?", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = didComply == true, onClick = { didComply = true })
                        Text("Yes")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = didComply == false, onClick = { didComply = false })
                        Text("No")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    // Validation
                    if (selectedAssociateId.isEmpty() || details.isBlank()) {
                        Toast.makeText(context, "Please select an associate and enter details", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    // Determine prior counts
                    val priorCount = occurrencesThisShiftForTypeLocal(existingIncidents, selectedAssociateId, violationType, settings)

                    // Build actionDetails
                    var actionDetails = whatDidYouSay
                    if (didComply == false) {
                        actionDetails = (actionDetails.takeIf { it.isNotBlank() }?.plus(" ") ?: "") +
                                "Associate did not comply."
                    }

                    // Flow logic
                    val finalAction: String
                    var managerNotified = false

                    if (violationType == "OSHA") {
                        // If prior OSHA exists -> Dismissal required
                        finalAction = if (priorCount > 0 || didComply == false) {
                            "Dismissal from Work"
                        } else {
                            "Coached"
                        }
                    } else {
                        // Hostility flow
                        when (priorCount) {
                            0 -> {
                                // First warn -> record as "Warn (Hostility)" (we'll label as "Warn (Hostility)")
                                finalAction = "Warn (Hostility)"
                            }
                            1 -> {
                                // Second warn -> manager notified
                                finalAction = "Warn (Hostility) - MGR Notified"
                                managerNotified = true
                                actionDetails = (actionDetails.takeIf { it.isNotBlank() }?.plus(" ") ?: "") +
                                        "Manager notified: $MANAGER_NAME."
                            }
                            else -> {
                                // 2 or more prior -> Dismissal
                                finalAction = "Dismissal from Work"
                            }
                        }
                    }

                    val incident = Incident(
                        associateId = selectedAssociateId,
                        type = violationType,
                        details = details,
                        timestamp = LocalDateTime.now().toString(),
                        action = finalAction,
                        cameraFriendlyName = cameraFriendlyName,
                        actionDetails = actionDetails,
                        didComply = didComply,
                        managerNotified = managerNotified
                    )
                    onSave(incident)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Generate Statement")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// Helper local function used inside composable to avoid recomputing external closures
private fun occurrencesThisShiftForTypeLocal(
    existingIncidents: List<Incident>,
    associateId: String,
    type: String,
    settings: SettingsData
): Int {
    if (associateId.isEmpty()) return 0
    val shiftStart = getShiftStart(settings.shiftStart)
    val shiftEnd = getShiftEnd(settings.shiftStart, settings.shiftEnd)
    return existingIncidents.count {
        it.associateId == associateId &&
                it.type == type &&
                LocalDateTime.parse(it.timestamp).isAfter(shiftStart) &&
                LocalDateTime.parse(it.timestamp).isBefore(shiftEnd)
    }
}

// --- UTILITIES ---

fun getShiftStart(shiftStartTime: String): LocalDateTime {
    val now = LocalDateTime.now()
    val time = LocalTime.parse(shiftStartTime)
    var start = now.with(time)
    if (now.toLocalTime().isBefore(time) && time.hour > 12) {
        start = start.minusDays(1)
    }
    return start
}

fun getShiftEnd(shiftStartTime: String, shiftEndTime: String): LocalDateTime {
    val start = getShiftStart(shiftStartTime)
    val end = LocalTime.parse(shiftEndTime)
    var endDateTime = start.with(end)
    if (end.isBefore(LocalTime.parse(shiftStartTime))) {
        endDateTime = endDateTime.plusDays(1)
    }
    return endDateTime
}

fun generateStatementText(
    incident: Incident,
    associate: Associate,
    settings: SettingsData
): String {
    val dt = LocalDateTime.parse(incident.timestamp)
    val dateStr = dt.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    val timeStr = dt.format(DateTimeFormatter.ofPattern("HH:mm"))

    // Use camera friendly name as location descriptor
    val locationLine =
        if (incident.cameraFriendlyName.isNotBlank() && incident.cameraFriendlyName != "None")
            "Location (Camera): ${incident.cameraFriendlyName}\n"
        else ""

    val actionDetailsLine =
        if (incident.actionDetails.isNotBlank())
            "Action Details: ${incident.actionDetails}\n"
        else ""

    val complianceLine = when (incident.didComply) {
        true -> "Associate complied with instructions.\n"
        false -> "Associate did not comply with instructions.\n"
        else -> ""
    }

    val managerLine = if (incident.managerNotified) "Manager Notified: $MANAGER_NAME\n" else ""

    return """
Employee Name: ${associate.name}
Employee ID: ${associate.eeid}
Store #: ${settings.storeNumber}
Date: $dateStr
Position: Associate

Describe in detail WHAT happened (stick to facts only and what you experienced or witnessed first-hand).
On $dateStr at $timeStr, an incident was observed regarding a violation of workplace conduct (${incident.type}). 
Specifically: ${incident.details}

Describe in detail WHEN the incident(s) occurred (please list dates and times). Has this happened before or is there a history of this?
The incident occurred on $dateStr at exactly $timeStr during the current shift.

Describe in detail WHERE the incident(s) occurred (be very descriptive of the location).
$locationLine

Describe the Action Taken to resolve this situation:
The resulting disciplinary action taken was: ${incident.action}.
$actionDetailsLine$complianceLine$managerLine
Signature: _______________________________            Date: ____________________
    """.trimIndent()
}