package com.sajatpenzugyek.app.core.theme

import androidx.compose.ui.graphics.Color
import com.sajatpenzugyek.app.domain.model.Category

object CategoryColorProvider {

    val PRESET_PALETTE = listOf(
        "#10B981", // Emerald
        "#F59E0B", // Amber
        "#3B82F6", // Blue
        "#8B5CF6", // Purple
        "#0284C7", // Sky
        "#EC4899", // Pink
        "#14B8A6", // Teal
        "#F97316", // Orange
        "#F43F5E", // Rose
        "#6366F1", // Indigo
        "#6B7280"  // Slate / Gray
    )

    private val DEFAULT_COLORS = mapOf(
        "food" to Color(0xFF10B981),
        "dining" to Color(0xFFF59E0B),
        "transport" to Color(0xFF3B82F6),
        "subscriptions" to Color(0xFF8B5CF6),
        "housing" to Color(0xFF0284C7),
        "entertainment" to Color(0xFFEC4899),
        "savings" to Color(0xFF14B8A6),
        "income" to Color(0xFF059669),
        "health" to Color(0xFFF43F5E),
        "shopping" to Color(0xFFF97316),
        "transfers" to Color(0xFF6366F1),
        "other" to Color(0xFF6B7280)
    )

    fun getColor(categoryId: String?, categoryColorHex: String? = null): Color {
        if (!categoryColorHex.isNullOrBlank()) {
            try {
                val hex = categoryColorHex.removePrefix("#")
                val longVal = hex.toLong(16)
                val argb = if (hex.length == 6) 0xFF000000L or longVal else longVal
                return Color(argb)
            } catch (_: Exception) {}
        }

        if (categoryId != null && DEFAULT_COLORS.containsKey(categoryId)) {
            return DEFAULT_COLORS[categoryId]!!
        }

        // Deterministic hash fallback
        if (!categoryId.isNullOrBlank()) {
            val idx = Math.abs(categoryId.hashCode()) % PRESET_PALETTE.size
            return getColor(null, PRESET_PALETTE[idx])
        }

        return Color(0xFF6B7280)
    }

    fun getColor(category: Category?): Color {
        return getColor(category?.id, category?.color)
    }
}
