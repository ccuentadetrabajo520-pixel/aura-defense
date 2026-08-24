package com.aura.defense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aura.defense.tools.evaluatePassword

@Composable
internal fun PasswordAuditorScreen(onClose: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            password = ""
            verdict = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Auditor de Contraseñas")
        Text("La evaluación es local y la contraseña no se guarda.")
        TextField(
            value = password,
            onValueChange = { password = it; verdict = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = { verdict = evaluatePassword(password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Evaluar")
        }
        verdict?.let { Text(it) }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}
