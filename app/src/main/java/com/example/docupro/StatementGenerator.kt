package com.example.docupro

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StatementGenerator {

    /**
     * Generates a statement matching the provided DOCX schema.
     * - Accepts a list of incidents (one or more) for the same associate and combines them.
     * - Leaves fields blank when the app cannot supply values (Position, Witnesses, Feelings, etc.)
     * - Designed for copy/paste into the Word form.
     */
    fun generateForIncidents(
        incidents: List<Incident>,
        associate: Associate,
        settings: SettingsData
    ): String {
        val sorted = incidents.sortedBy { LocalDateTime.parse(it.timestamp) }
        val first = sorted.firstOrNull()
        val dt = first?.let { LocalDateTime.parse(it.timestamp) } ?: LocalDateTime.now()
        val dateStr = dt.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

        // WHAT happened: combined chronological entries
        val whatBuilder = StringBuilder()
        sorted.forEachIndexed { idx, inc ->
            val t = LocalDateTime.parse(inc.timestamp).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))
            whatBuilder.append("[${t}] (${inc.type}) ${inc.details}")
            if (inc.actionDetails.isNotBlank()) whatBuilder.append(" — Notes: ${inc.actionDetails}")
            if (idx < sorted.lastIndex) whatBuilder.append("\n\n")
        }

        // WHEN: list timestamps
        val whenBuilder = StringBuilder()
        sorted.forEach { inc ->
            val t = LocalDateTime.parse(inc.timestamp).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))
            whenBuilder.append("${t}\n")
        }

        // WHERE: camera friendly names if present
        val locationLine = sorted.mapNotNull { inc ->
            inc.cameraFriendlyName.takeIf { it.isNotBlank() && it != "None" }
        }.distinct().joinToString("; ")

        // Fields left blank for manual entry
        val witnesses = ""
        val toldAnyone = ""
        val feelings = ""
        val resolution = ""
        val additional = ""

        // Action summary
        val actionsSummary = sorted.joinToString("; ") {
            it.action + (it.actionDetails.takeIf { d -> d.isNotBlank() }?.let { d -> ": $d" } ?: "")
        }

        return """
Employee Name: ${associate.name}
Employee ID: ${associate.eeid}
Store #: ${settings.storeNumber}
Date: $dateStr
Position: 

Describe in detail WHAT happened (stick to facts only and what you experienced or witnessed first-hand).
${whatBuilder.toString()}

Describe in detail WHEN the incident(s) occurred (please list dates and times). Has this happened before or is there a history of this?
${whenBuilder.toString()}

Describe in detail WHERE the incident(s) occurred (be very descriptive of the location).
${if (locationLine.isNotBlank()) locationLine else ""}

Please provide the names of any WITNESS(ES).
${witnesses}

Have you told anyone else about this? If yes, who and when? And what exactly did you tell them?
${toldAnyone}

How did this make you feel?
${feelings}

What would a satisfactory resolution of this situation look/feel like to you?
${resolution}

Any additional comments or information you think Sheetz needs to know about this situation?
${additional}

Action Summary: ${actionsSummary}

Signature: _______________________________ Date: ____________________
""".trimIndent()
    }

    /**
     * Convenience wrapper for single-incident statements.
     */
    fun generateForIncident(
        incident: Incident,
        associate: Associate,
        settings: SettingsData
    ): String = generateForIncidents(listOf(incident), associate, settings)
}