//DocuPro: Documentation for the modern Sheetz Supervisor.
//Dreamt up by Brandon Case,
//Brought to life by Google's Gemini.
//Thank you Google for enabling this project of mine.

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
    var cameraViewName: String = "CameraViewName",
    var cameraPresets: List<String> = listOf("Sales Floor 1", "Kitchen", "Gas Island 4")
)

data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val associateId: String,
    val type: String,
    val details: String,
    val timestamp: String,
    val location: String,
    val action: String,
    val actionDetails: String = "",
    val cameraFriendlyName: String = "",
    val witnesses: String = "",           // Added to resolve the type error in StatementGenerator
    val complied: Boolean? = null,
    val timeLeftBuilding: String = "",
    val managerNotified: Boolean = false
)

data class SettingsData(
    val shiftStart: String = "21:00",
    val shiftEnd: String = "07:30",
    val storeNumber: String = "318",
    val themeMode: Int = 0,
    val cameraPresets: List<Camera> = emptyList()
)
