package com.example.docupro.models

data class Associate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val eeid: String
)

data class Camera(
    val id: String = java.util.UUID.randomUUID().toString(),
    val friendlyName: String
)

data class SettingsData(
    val storeNumber: String = "",
    val shiftStart: String = "22:00",
    val shiftEnd: String = "06:30",
    val themeMode: Int = 1, // 1 = Auto/Light, 2 = Dark
    val debugBypassShiftTime: Boolean = false,
    val cameraPresets: List<Camera> = emptyList(),
    val locationPresets: List<String> = emptyList() // NEW: Custom tagged spots
)

data class Incident(
    val id: String = java.util.UUID.randomUUID().toString(),
    val associateId: String,
    val type: String, // "OSHA" or "Hostility"
    val details: String,
    val timestamp: String,
    val location: String,
    val action: String, // "Warn", "Dismissal from Work", etc
    val actionDetails: String,
    val cameraFriendlyName: String,
    val witnesses: String,
    val complied: Boolean? = null,
    val timeLeftBuilding: String = "",
    val managerNotified: Boolean = false,
    // NEW: Network Mapping fields (Defaulted so old JSON saves don't break)
    val reporterId: String? = null,
    val witnessIds: List<String> = emptyList()
)

// Wrapper for exporting/importing your full configuration
data class AppConfigExport(
    val settings: SettingsData,
    val associates: List<Associate>
)