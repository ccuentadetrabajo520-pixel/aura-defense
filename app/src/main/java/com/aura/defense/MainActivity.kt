package com.aura.defense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraDefenseTheme {
                AuraDefenseApp()
            }
        }
    }
}

@Composable
private fun AuraDefenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF050A0C),
            surface = Color(0xFF0B1518),
            primary = Color(0xFF25F4D0),
            secondary = Color(0xFFB8F72E),
            onBackground = Color(0xFFE8FFFA)
        ),
        content = content
    )
}

@Composable
private fun AuraDefenseApp() {
    val navController = rememberNavController()
    val tabs = listOf(
        BottomTab("inicio", "Inicio", Icons.Filled.Home),
        BottomTab("auras", "Auras", Icons.Filled.Radar),
        BottomTab("defensa", "Defensa", Icons.Filled.Shield),
        BottomTab("apps", "Apps", Icons.Filled.GridView)
    )

    NavHost(navController = navController, startDestination = "inicio") {
        composable("centro_aura") { AuraCenterScreen() }
        tabs.forEach { tab ->
            composable(tab.route) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF091215)) {
                            tabs.forEach { item ->
                                NavigationBarItem(
                                    selected = item.route == tab.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    if (tab.route == "inicio") {
                        HomeScreen(Modifier.padding(paddingValues)) { navController.navigate("centro_aura") }
                    } else {
                        PlaceholderTab(tab.label, Modifier.padding(paddingValues))
                    }
                }
            }
        }
    }
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit) {
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
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF031311))
        ) { Text("ESCANEAR AHORA", fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
    }
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

@Composable
private fun AuraCenterScreen() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Centro Aura", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        Text("Centro Aura (Próximamente)")
        Spacer(Modifier.height(32.dp))
        Text("ID de usuario", color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun PlaceholderTab(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 28.sp) }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    AuraDefenseTheme {
        HomeScreen(onSettingsClick = {})
    }
}
