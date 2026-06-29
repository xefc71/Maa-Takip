package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeState {
    var isDark: Boolean by mutableStateOf(true)
}

// High Density Theme Color Palette (Beautiful Custom Cyber-Green Dark Theme)
val EmeraldGreen = Color(0xFF10B981) // Beautiful neon-mint green for active states, graphs, and successes
val DeepGreen = Color(0xFF064E3B) // Deep dark emerald for active container backgrounds and badges
val GoldenGold = Color(0xFF34D399) // High-contrast glowing mint/green for primary brand accents

val CharcoalNavy: Color
    get() = if (ThemeState.isDark) Color(0xFF080C10) else Color(0xFFF8FAFC) // Obsidian dark background vs Crisp light bg

val DarkSlate: Color
    get() = if (ThemeState.isDark) Color(0xFF111720) else Color(0xFFF3FAF6) // Deep space-gray vs 1/25th green-tinted White card

val LightSlate: Color
    get() = if (ThemeState.isDark) Color(0xFF1E293B) else Color(0xFF0B4637) // Sleek border slate-gray vs very dark green border

val TextWhite: Color
    get() = if (ThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A) // Crisp White vs slate-900

val TextGray: Color
    get() = if (ThemeState.isDark) Color(0xFF94A3B8) else Color(0xFF64748B) // Slate Gray vs slate-500

val AlertRed = Color(0xFFEF4444) // Vibrant glowing red for main work stop actions
val InfoTeal = Color(0xFF06B6D4) // Glowing high-tech cyan accent
val SoftYellow = Color(0xFFF59E0B) // Warn/Overtime gold accent

// Additional Theme Accents mapped to the cyber-green aesthetic
val HighDensityPrimary = Color(0xFF10B981)
val HighDensityPrimaryContainer = Color(0xFF064E3B)
val HighDensityOnPrimaryContainer = Color(0xFFD1FAE5)

val HighDensitySecondaryContainer: Color
    get() = if (ThemeState.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

val HighDensityOnSecondaryContainer: Color
    get() = if (ThemeState.isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)

val HighDensityBorder = Color(0xFF115E59)




