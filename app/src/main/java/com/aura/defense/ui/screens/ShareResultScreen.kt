package com.aura.defense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ShareResultScreen(text: String, verdict: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Resultado del análisis", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Texto recibido", color = MaterialTheme.colorScheme.secondary)
                Text(text, color = MaterialTheme.colorScheme.onSurface)
                Text(verdict, color = verdictColor(verdict), fontSize = 18.sp)
            }
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

private fun verdictColor(verdict: String) = when {
    verdict.startsWith("Peligroso") -> androidx.compose.ui.graphics.Color(0xFFFF5C5C)
    verdict.startsWith("Sospechoso") -> androidx.compose.ui.graphics.Color(0xFFFFD166)
    else -> androidx.compose.ui.graphics.Color(0xFFB8F72E)
}
