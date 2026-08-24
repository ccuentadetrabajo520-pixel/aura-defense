package com.aura.defense.tools

import android.net.Uri
import java.net.InetAddress
import java.util.Locale

fun analyzeUrl(url: String): String {
    val candidate = url.trim()
    if (candidate.isBlank()) return "Sospechoso: introduce una URL para analizarla."

    val normalized = if (candidate.contains("://")) candidate else "https://$candidate"
    val uri = runCatching { Uri.parse(normalized) }.getOrNull()
    val host = uri?.host?.lowercase(Locale.US).orEmpty()
    val lowerUrl = normalized.lowercase(Locale.US)
    val reasons = mutableListOf<String>()

    if (uri == null || host.isBlank()) reasons += "formato de URL inválido"
    if (host.startsWith("xn--") || host.split('.').any { it.startsWith("xn--") }) reasons += "dominio Punycode"
    if (host.any { it.code > 127 } || containsHomoglyph(host)) reasons += "caracteres Unicode confusos"
    if (isDirectIp(host)) reasons += "dirección IP directa"
    if (uri?.scheme.equals("http", ignoreCase = true)) reasons += "conexión sin TLS"
    if (host in shorteners) reasons += "acortador de enlaces"
    if (host.split('.').size > 4) reasons += "exceso de subdominios"
    if (host.substringAfterLast('.', "").lowercase(Locale.US) in suspiciousTlds) reasons += "TLD sospechoso"
    if (sensitiveWords.any { lowerUrl.contains(it) }) reasons += "palabras asociadas a fraude"
    if (containsBrandImpersonation(host)) reasons += "posible suplantación de marca"

    return when {
        uri == null || host.isBlank() -> "Sospechoso: formato de URL inválido."
        reasons.size >= 2 || reasons.any { it == "dirección IP directa" || it == "posible suplantación de marca" } ->
            "Peligroso: ${reasons.joinToString(", ")}."
        reasons.isNotEmpty() -> "Sospechoso: ${reasons.joinToString(", ")}."
        else -> "Seguro: no se detectaron señales locales de riesgo."
    }
}

private val shorteners = setOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rebrand.ly")
private val suspiciousTlds = setOf("zip", "mov", "top", "xyz", "click", "gq", "tk", "ml", "ga", "cf")
private val sensitiveWords = setOf("login", "verify", "wallet", "bank", "reset", "prize")
private val knownBrands = setOf("paypal", "microsoft", "google", "apple", "amazon", "netflix", "facebook", "instagram", "whatsapp")

private fun isDirectIp(host: String): Boolean {
    val ipv4 = host.split('.')
    val isIpv4 = ipv4.size == 4 && ipv4.all { it.toIntOrNull()?.let { value -> value in 0..255 } == true }
    val isIpv6 = host.contains(':') && runCatching { InetAddress.getByName(host) }.isSuccess
    return isIpv4 || isIpv6
}

private fun containsHomoglyph(host: String): Boolean {
    return host.any { it in "а-яΑ-ΩοеіΙӏ" }
}

private fun containsBrandImpersonation(host: String): Boolean {
    val labels = host.split('.')
    return knownBrands.any { brand ->
        labels.any { label ->
            label.contains(brand) && label != brand && !label.endsWith(".$brand")
        }
    }
}
