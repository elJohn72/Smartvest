package com.ajtecnology.smartvest.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF0F172A)
val Signal = Color(0xFF1E40AF)
val Pulse = Color(0xFF3B82F6)
val Paper = Color(0xFFF8FAFC)
val CardBg = Color(0xFFFFFFFF)
val Inset = Color(0xFFEEF3F8)
val BodyText = Color(0xFF334155)
val MetaText = Color(0xFF64748B)

val ClearGreen = Color(0xFF166534)
val ClearSoft = Color(0xFFDCFCE7)
val CautionYellow = Color(0xFFA16207)
val CautionSoft = Color(0xFFFEF3C7)
val WarnOrange = Color(0xFFC2410C)
val WarnSoft = Color(0xFFFFEDD5)
val SosRed = Color(0xFFB91C1C)
val SosSoft = Color(0xFFFEE2E2)

private val SmartVestColors = lightColorScheme(
    primary = Signal,
    onPrimary = Color.White,
    secondary = Pulse,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Night,
    surface = CardBg,
    onSurface = BodyText,
    error = SosRed,
    onError = Color.White
)

@Composable
fun SmartVestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartVestColors,
        content = content
    )
}
