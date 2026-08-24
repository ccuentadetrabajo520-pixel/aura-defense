package com.aura.defense.data

import android.content.Context
import org.json.JSONArray

object ThreatRepository {
    fun loadBlockedDomains(context: Context): List<String> {
        return runCatching {
            val json = context.assets.open("threats.json").bufferedReader().use { it.readText() }
            val threats = JSONArray(json)
            buildList {
                for (index in 0 until threats.length()) {
                    threats.optJSONObject(index)?.optString("domain")
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    fun isBlockedDomain(domain: String, blockedDomains: Collection<String>): Boolean {
        val normalized = domain.trim().trimEnd('.').lowercase()
        return blockedDomains.any { blocked ->
            val entry = blocked.trim().trimEnd('.').lowercase()
            normalized == entry || normalized.endsWith(".$entry")
        }
    }
}
