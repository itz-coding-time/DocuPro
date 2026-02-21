package com.example.docupro.utils

import android.content.Context
import com.example.docupro.models.Associate
import com.example.docupro.models.Incident
import com.example.docupro.models.SettingsData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StatementGenerator {

    fun generateCombinedStatement(context: Context, incidents: List<Incident>, associate: Associate, settings: SettingsData): String {
        // Read the template file from the app's assets folder
        val template = try {
            context.assets.open("statement_template.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return "Error loading statement template. Ensure 'statement_template.txt' is in the app/src/main/assets/ folder."
        }

        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val currentDate = LocalDateTime.now().format(dateFormatter)
        val sortedIncidents = incidents.sortedBy { LocalDateTime.parse(it.timestamp) }

        if (sortedIncidents.isEmpty()) return "No incidents recorded."

        // 1. WHAT HAPPENED
        val whatHappened = StringBuilder()
        sortedIncidents.forEachIndexed { index, incident ->
            whatHappened.appendLine("Incident ${index + 1} (${incident.type} Violation):")
            whatHappened.appendLine(incident.details).appendLine()
        }

        // 2. WHEN OCCURRED
        val whenOccurred = StringBuilder()
        whenOccurred.appendLine("Yes, this document details a history of progressive discipline during the shift:")
        sortedIncidents.forEachIndexed { index, incident ->
            val dt = LocalDateTime.parse(incident.timestamp)
            whenOccurred.appendLine("- Incident ${index + 1}: ${dt.format(dateFormatter)} at ${dt.format(timeFormatter)}")
        }

        // 3. WHERE OCCURRED
        val locations = sortedIncidents.map { "- ${it.location}" + if (it.cameraFriendlyName.isNotBlank()) " (Recorded on Camera: ${it.cameraFriendlyName})" else "" }.distinct()

        // 4. WITNESSES
        val allWitnesses = sortedIncidents.map { it.witnesses }.filter { it.isNotBlank() }.distinct()
        val witnessesStr = if (allWitnesses.isEmpty()) "No witnesses recorded." else allWitnesses.joinToString(", ")

        // 5. TOLD ANYONE
        val toldAnyone = if (sortedIncidents.any { it.managerNotified }) "Yes, Management was immediately notified at the time of the occurrence(s)." else "N/A - Management documented this directly."

        // 6. ADDITIONAL COMMENTS (Corrective Action Timeline)
        val additionalComments = StringBuilder()
        additionalComments.appendLine("CORRECTIVE ACTION TIMELINE:")
        sortedIncidents.forEachIndexed { index, incident ->
            additionalComments.append("Incident ${index + 1} Action: ${incident.action}")
            if (incident.actionDetails.isNotBlank()) additionalComments.append(" (${incident.actionDetails})")
            if (incident.action == "Warn") additionalComments.append(" | Complied: ${if (incident.complied == true) "Yes" else "No"}")
            else if (incident.action == "Dismissal from Work") additionalComments.append(" | Time Left Building: ${incident.timeLeftBuilding}")
            additionalComments.appendLine()
        }
        if (sortedIncidents.any { it.action == "Dismissal from Work" }) {
            additionalComments.appendLine("\nFINAL OUTCOME: Progressive discipline policy limits were reached, resulting in a Dismissal from Shift.")
        }

        // Inject data into the template placeholders
        return template
            .replace("{EMP_NAME}", associate.name)
            .replace("{EMP_ID}", associate.eeid)
            .replace("{STORE_NUM}", settings.storeNumber)
            .replace("{DATE}", currentDate)
            .replace("{WHAT_HAPPENED}", whatHappened.toString().trimEnd())
            .replace("{WHEN_OCCURRED}", whenOccurred.toString().trimEnd())
            .replace("{WHERE_OCCURRED}", locations.joinToString("\n"))
            .replace("{WITNESSES}", witnessesStr)
            .replace("{TOLD_ANYONE}", toldAnyone)
            .replace("{ADDITIONAL_COMMENTS}", additionalComments.toString().trimEnd())
    }
}