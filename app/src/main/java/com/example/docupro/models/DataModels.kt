package com.example.docupro.models

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Associate(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val eeid: String = ""
)

data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val associateId: String = "",
    val type: String = "", // OSHA, Hostility
    val details: String = "",
    val timestamp: String = "", // ISO string
    val location: String = "",
    val action: String = "", // Warn, Dismissal from Work
    val actionDetails: String = "",
    val cameraFriendlyName: String = "",
    val witnesses: String = "",
    val complied: Boolean? = null,
    val timeLeftBuilding: String = "",
    val managerNotified: Boolean = false
)

data class Camera(
    val id: String = UUID.randomUUID().toString(),
    val friendlyName: String = ""
)

data class SettingsData(
    val themeMode: Int = 1, // 1 = light/system, 2 = dark
    val storeNumber: String = "",
    val cameraPresets: List<Camera> = emptyList(),
    // Default shift times to current time formatting
    val shiftStart: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val shiftEnd: String = LocalTime.now().plusHours(8).format(DateTimeFormatter.ofPattern("HH:mm")),
    val debugBypassShiftTime: Boolean = false // Toggle for active reporting constraints
)