package com.example.docupro.utils

import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import com.example.docupro.models.SettingsData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StatementGenerator_BAK
{

    fun generateCombinedStatement(incidents: List<Incident>, associate: Associate, settings: SettingsData): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        val date = formatter.format(LocalDateTime.now())
        val supervisorName = "Brandon Case" // Replace with actual supervisor name from settings if available

        val statementHeader = "Statement of Corrective Action\n\n" +
                "Date: $date\n" +
                "Associate Name: ${associate.name}\n" +
                "Associate ID: ${associate.eeid}\n" +
                "Store Number: ${settings.storeNumber}\n" +
                "Supervisor: $supervisorName\n\n" +
                "This document serves as a formal record of all corrective actions and documented incidents involving the above-named associate during their employment at Sheetz.\n\n" +
                "--------------------------------------------------\n\n"

        val incidentsText = incidents.joinToString("\n\n--------------------------------------------------\n\n") { incident ->
            "Date of Incident: ${LocalDateTime.parse(incident.timestamp).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))}\n" +
            "Violation Type: ${incident.type}\n" +
            "Location of Incident: ${incident.location}\n" +
            "Camera (if applicable): ${incident.cameraFriendlyName}\n\n" +
            "Details:\n${incident.details}\n\n" +
            "Corrective Action Taken: ${incident.action}\n" +
            "Notes on Action: ${incident.actionDetails}\n" +
            (if (incident.complied != null) "Complied with Directive: ${if (incident.complied) "Yes" else "No"}\n" else "") +
            (if (incident.timeLeftBuilding.isNotBlank()) "Time Left Building: ${incident.timeLeftBuilding}\n" else "") +
            "Manager Notified: ${if (incident.managerNotified) "Yes" else "No"}"
        }

        val closingStatement = if (incidents.any { it.action == "Dismissal from Work" }) {
            "\n\n--------------------------------------------------\n\n" +
            "CONCLUSION:\n\n" +
            "Due to the repeated nature of these violations and a demonstrated inability to adhere to company policy and safety standards, a final corrective action of DISMISSAL FROM WORK has been taken, effective immediately.\n\n" +
            "This decision was made after careful review of all documented incidents and is considered final.\n\n"
        } else {
            "\n\n--------------------------------------------------\n\n" +
            "All incidents have been logged and will be kept on record. Further violations may lead to additional corrective action, up to and including dismissal from work.\n"
        }

        return statementHeader + incidentsText + closingStatement
    }
}