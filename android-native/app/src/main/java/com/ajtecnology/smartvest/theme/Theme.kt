package com.ajtecnology.smartvest.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Outline = Color(0xFFE2E8F0)
val BrandAccent = Color(0xFFEA580C)

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
    surfaceVariant = Inset,
    onSurfaceVariant = BodyText,
    outline = Outline,
    error = SosRed,
    onError = Color.White
)

private val SmartVestTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun SmartVestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartVestColors,
        typography = SmartVestTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp)),
        content = content
    )
}
