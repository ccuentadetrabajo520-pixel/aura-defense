package com.aura.defense.tools

import kotlin.math.log2

fun evaluatePassword(password: String): String {
    if (password.isEmpty()) return "Riesgo alto: introduce una contraseña para evaluarla."

    val issues = mutableListOf<String>()
    val length = password.length
    val variety = listOf(
        password.any(Char::isLowerCase),
        password.any(Char::isUpperCase),
        password.any(Char::isDigit),
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    val alphabetSize = when (variety) {
        4 -> 95
        3 -> 62
        2 -> 52
        else -> 26
    }
    val entropy = length * log2(alphabetSize.toDouble())

    if (length < 12) issues += "menos de 12 caracteres"
    if (variety < 3) issues += "poca variedad de caracteres"
    if (hasRepetition(password)) issues += "repeticiones evidentes"
    if (hasSequence(password)) issues += "secuencia predecible"
    if (commonWords.any { password.lowercase().contains(it) }) issues += "palabra común"
    if (entropy < 50) issues += "entropía aproximada baja"

    return when {
        issues.size >= 3 || entropy < 35 -> "Riesgo alto: ${issues.joinToString(", ")}."
        issues.isNotEmpty() -> "Mejorable: ${issues.joinToString(", ")}."
        else -> "Fuerte: longitud, variedad y entropía aproximada adecuadas."
    }
}

private val commonWords = setOf("password", "contraseña", "qwerty", "admin", "welcome", "letmein", "dragon", "monkey")

private fun hasRepetition(password: String): Boolean {
    return password.windowed(3).any { it.all { character -> character == it.first() } } ||
        password.zipWithNext().count { (first, second) -> first == second } >= 2
}

private fun hasSequence(password: String): Boolean {
    return password.windowed(3).any { window ->
        val codes = window.map(Char::code)
        codes[1] - codes[0] == 1 && codes[2] - codes[1] == 1 ||
            codes[1] - codes[0] == -1 && codes[2] - codes[1] == -1
    }
}
