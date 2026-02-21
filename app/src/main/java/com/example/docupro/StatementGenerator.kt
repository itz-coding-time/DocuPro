//DocuPro: Documentation for the modern Sheetz Supervisor.
//Dreamt up by Brandon Case,
//Brought to life by Google's Gemini.
//Thank you Google for enabling this project of mine.

package com.example.docupro

import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import com.example.docupro.models.SettingsData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StatementGenerator {

    fun generateForIncidents(
        incidents: List<Incident>,
        associate: Associate,
        settings: SettingsData
    ): String {
        val sorted = incidents.sortedBy { LocalDateTime.parse(it.timestamp) }
        val first = sorted.firstOrNull() ?: return ""
        val dt = LocalDateTime.parse(first.timestamp)
        val dateStr = dt.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

        val whatBuilder = StringBuilder()
        sorted.forEach { inc ->
            val time = LocalDateTime.parse(inc.timestamp).format(DateTimeFormatter.ofPattern("HH:mm"))
            whatBuilder.append("[$time] ${inc.details}")
            if (inc.managerNotified) whatBuilder.append(" (Manager notified immediately).")
            whatBuilder.append("\n")
        }

        val witnesses = sorted.map { it.witnesses }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        val cameras = sorted.map { it.cameraFriendlyName }.filter { it.isNotBlank() }.distinct().joinToString("; ")

        return """
Employee Name: ${associate.name}
Employee ID: ${associate.eeid}
Store #: ${settings.storeNumber}
Date: $dateStr

WHAT happened:
${whatBuilder.toString()}

WHERE it occurred:
${if (cameras.isNotBlank()) "Recorded on: $cameras" else "Store Floor"}

WITNESSES:
${if (witnesses.isNotBlank()) witnesses else "None recorded."}

Action Summary: 
${sorted.joinToString("; ") { it.action }}

Signature: _______________________________ Date: ____________________
""".trimIndent()
    }

    fun generateForIncident(incident: Incident, associate: Associate, settings: SettingsData) =
        generateForIncidents(listOf(incident), associate, settings)
}