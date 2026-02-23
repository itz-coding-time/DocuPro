package com.example.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docupro.models.Incident
import java.time.LocalDateTime

@Composable
fun HomeDashboard(incidents: List<Incident>) {
    val today = LocalDateTime.now().toLocalDate()
    val count = incidents.count {
        try {
            LocalDateTime.parse(it.timestamp).toLocalDate() == today
        } catch(e: Exception) {
            false
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Night Shift Overview", color = Color.Gray)

        Card(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("INCIDENTS TONIGHT", style = MaterialTheme.typography.labelSmall)
                Text("$count", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}