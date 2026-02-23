package com.example.docupro.utils

import android.content.Context
import android.net.Uri
import com.example.docupro.models.AppConfigExport
import com.example.docupro.models.Associate
import com.example.docupro.models.SettingsData
import com.google.gson.Gson
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object ConfigManager {

    fun exportConfig(context: Context, uri: Uri, settings: SettingsData, associates: List<Associate>): Boolean {
        return try {
            val exportData = AppConfigExport(settings, associates)
            val jsonString = Gson().toJson(exportData)

            // Open the URI provided by the Android file picker and write the JSON
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importConfig(context: Context, uri: Uri): AppConfigExport? {
        return try {
            // Read the JSON file the user selected
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val jsonString = reader.readText()
                    // Convert back from JSON to our Kotlin Data Class
                    Gson().fromJson(jsonString, AppConfigExport::class.java)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}