package com.aura.defense

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit) {
    var auraId by remember { mutableStateOf("AURA-001") }
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF102126), modifier = Modifier.weight(1f)) {
                TextField(
                    value = auraId,
                    onValueChange = { auraId = it },
                    singleLine = true,
                    label = { Text("ID Aura") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Centro Aura", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(34.dp))
        Text("AURA DEFENSE", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text("SIN ESCANEO", color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
        Spacer(Modifier.height(28.dp))
        RadarPlaceholder(Modifier.size(280.dp))
        Spacer(Modifier.weight(1f))
        HomeActionButton("ESCANEAR AHORA")
        Spacer(Modifier.height(10.dp))
        HomeActionButton("ACTIVAR DEFENSA VPN")
        Spacer(Modifier.height(10.dp))
        HomeActionButton("MODO EMERGENCIA")
    }
}

@Composable
private fun HomeActionButton(label: String) {
    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF031311))
    ) { Text(label, fontSize = 16.sp) }
}

@Composable
private fun RadarPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "radarRotation")
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 8.dp.toPx()
        for (scale in listOf(1f, .72f, .44f)) {
            drawCircle(Color(0xFF1B4D4C), radius * scale, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
        drawLine(Color(0xFF25F4D0), center, Offset(center.x, center.y - radius), 3.dp.toPx(), StrokeCap.Round)
        rotate(rotation, center) { drawLine(Color(0x9925F4D0), center, Offset(center.x, center.y - radius), 8.dp.toPx(), StrokeCap.Round) }
        drawCircle(Color(0xFFB8F72E), 5.dp.toPx(), center)
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    AuraDefenseTheme {
        HomeScreen(onSettingsClick = {})
    }
}
