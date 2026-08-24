package com.aura.defense.tools

import android.content.Intent

class ShareScannerHandler {
    fun extractText(intent: Intent): String? {
        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.toString()?.trim()

        return text?.takeIf { it.isNotEmpty() }
    }

    fun analyzeSharedText(intent: Intent): String? {
        return extractText(intent)?.let(::analyzeUrl)
    }
}
