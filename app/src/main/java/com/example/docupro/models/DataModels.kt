package com.example.docupro.models

import java.util.UUID

data class Associate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val eeid: String
)

data class Camera(
    val id: String = UUID.randomUUID().toString(),
    var friendlyName: String,
    var literalPosition: String = "Literalposition",
    var cameraViewName: String = "CameraViewName"
)

data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val associateId: String,
    val type: String,
    val details: String,
    val timestamp: String,
    val location: String,
    val action: String,
    val actionDetails: String = "",       // Added for CoPilot formatting
    val cameraFriendlyName: String = "",  // Added for CoPilot formatting
    val complied: Boolean? = null,        // Tracks if they complied
    val timeLeftBuilding: String = "",    // Tracks when they left the building
    val managerNotified: Boolean = false  // Tracks if manager was notified
)

data class SettingsData(
    val shiftStart: String = "21:00",
    val shiftEnd: String = "07:30",
    val storeNumber: String = "318",
    val themeMode: Int = 0 // 0: System, 1: Light, 2: Dark
)