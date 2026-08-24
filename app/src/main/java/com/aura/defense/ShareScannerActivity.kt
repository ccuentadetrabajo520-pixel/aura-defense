package com.aura.defense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aura.defense.tools.ShareScannerHandler
import com.aura.defense.ui.screens.ShareResultScreen

class ShareScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val handler = ShareScannerHandler()
        val text = handler.extractText(intent)
        val verdict = handler.analyzeSharedText(intent)
            ?: "Sospechoso: no se recibió texto para analizar."

        setContent {
            ShareResultScreen(
                text = text ?: "Sin texto recibido",
                verdict = verdict,
                onClose = ::finish
            )
        }
    }
}
