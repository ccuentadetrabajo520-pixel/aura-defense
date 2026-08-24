package com.aura.defense.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.defense.tools.analyzeUrl
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

internal data class QrAnalysisResult(
    val rawText: String,
    val analyzedLink: String?,
    val verdict: String
)

internal fun processQrCode(rawText: String): QrAnalysisResult {
    val text = rawText.trim()
    if (!isUrl(text)) {
        return QrAnalysisResult(text, null, "Sospechoso: el código no contiene una URL válida.")
    }

    return QrAnalysisResult(text, text, analyzeUrl(text))
}

@Composable
internal fun QrScannerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { BarcodeScanning.getClient() }
    var hasCameraPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<QrAnalysisResult?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) scanMessage = "Se necesita permiso de cámara para escanear un código QR."
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedBitmap = bitmap
        result = null
        scanMessage = if (bitmap == null) "No se capturó ninguna imagen." else "Analizando código QR..."
        bitmap?.let { image ->
            scanner.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener { barcodes ->
                    val rawText = barcodes.firstOrNull()?.rawValue
                    if (rawText.isNullOrBlank()) {
                        scanMessage = "No se encontró un código QR en la imagen."
                    } else {
                        result = processQrCode(rawText)
                        scanMessage = null
                    }
                }
                .addOnFailureListener {
                    scanMessage = "No se pudo analizar la imagen."
                }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(scanner) {
        onDispose { scanner.close() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("QR Anti-Phishing", style = MaterialTheme.typography.headlineMedium)
        Text("Analiza el enlace antes de abrirlo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                if (hasCameraPermission) {
                    cameraLauncher.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Escanear código QR")
        }
        scanMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        result?.let { analysis ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resultado del análisis", color = MaterialTheme.colorScheme.primary)
                    Text("Enlace analizado", color = MaterialTheme.colorScheme.secondary)
                    Text(analysis.analyzedLink ?: analysis.rawText)
                    Text(analysis.verdict, color = verdictColor(analysis.verdict))
                }
            }
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

private fun isUrl(value: String): Boolean {
    val normalized = if (value.contains("://")) value else "https://$value"
    val uri = runCatching { Uri.parse(normalized) }.getOrNull()
    val uriString = uri?.toString() ?: return false
    val validatedUri = Uri.parse(uriString)
    return validatedUri.scheme in setOf("http", "https") && !validatedUri.host.isNullOrBlank()
}

private fun verdictColor(verdict: String) = when {
    verdict.startsWith("Peligroso") -> androidx.compose.ui.graphics.Color(0xFFFF5C5C)
    verdict.startsWith("Sospechoso") -> androidx.compose.ui.graphics.Color(0xFFFFD166)
    else -> androidx.compose.ui.graphics.Color(0xFFB8F72E)
}
