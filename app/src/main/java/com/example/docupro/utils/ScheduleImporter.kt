package com.example.docupro.utils

import android.content.Context
import android.net.Uri
import com.example.docupro.models.Associate
import com.example.docupro.models.SettingsData
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader

object ScheduleImporter {

    // --- XLSX IMPORTER ---
    fun importFromExcel(context: Context, uri: Uri): List<Associate> {
        val newAssociates = mutableListOf<Associate>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0) // Grabs the first tab of the spreadsheet

                // Starts at row 1 (skipping row 0 which is usually headers like "Name", "ID")
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue

                    // ASSUMPTION: Column 0 is Name, Column 1 is EEID.
                    // (You may need to change 0 and 1 if your specific spreadsheet is different!)
                    val nameCell = row.getCell(0)?.toString() ?: ""
                    val eeidCell = row.getCell(1)?.toString() ?: ""

                    // Only add if it actually found data
                    if (nameCell.isNotBlank() && eeidCell.isNotBlank()) {
                        // Replace any .0 at the end of EEID if Excel formatted it as a decimal
                        val cleanEeid = eeidCell.removeSuffix(".0")
                        newAssociates.add(Associate(name = nameCell.trim(), eeid = cleanEeid.trim()))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If it fails (sometimes Android struggles with giant Excel files), fallback to CSV
            return importFromCsv(context, uri)
        }
        return newAssociates
    }

    // --- CSV FALLBACK ---
    fun importFromCsv(context: Context, uri: Uri): List<Associate> {
        val newAssociates = mutableListOf<Associate>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.readLine() // Skip header line

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 2) {
                        newAssociates.add(Associate(name = tokens[0].trim(), eeid = tokens[1].trim()))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newAssociates
    }

    fun importFromExcel(context: Context, uri: Uri, settings: SettingsData) {}
}