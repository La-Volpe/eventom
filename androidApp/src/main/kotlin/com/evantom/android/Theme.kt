package com.evantom.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Eventim-flavoured palette (M1 design notes): bright background,
 * ticket-blue accent. A static scheme instead of the wallpaper-derived
 * dynamic one — dynamic color cannot be overridden per-screen and would
 * override the brand feel the design notes ask for.
 */
val TicketBlue = Color(0xFF1C6EE8)
private val TicketBlueDark = Color(0xFF155BC5)
private val BrightBackground = Color(0xFFFBFBFC)

private val EventimLightColors =
    lightColorScheme(
        primary = TicketBlue,
        onPrimary = Color.White,
        secondary = TicketBlueDark,
        background = BrightBackground,
        surface = Color.White,
    )

private val EventimDarkColors =
    darkColorScheme(
        primary = TicketBlue,
        onPrimary = Color.White,
        secondary = Color(0xFF8FB7F3),
        background = Color(0xFF111318),
        surface = Color(0xFF1B1D23),
    )

/**
 * Thin Material3 wrapper with the fixed brand palette.
 */
@Composable
fun EvenTomTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) EventimDarkColors else EventimLightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/**
 * The EVENTOM wordmark — one component so splash, login and the events
 * header render the identical face. [compact] is the appbar-appropriate
 * variant (smaller size/tracking); the full variant matches the splash.
 */
@Composable
fun EventomWordmark(modifier: Modifier = Modifier, compact: Boolean = false) {
    val base = if (compact) {
        MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 2.sp)
    } else {
        MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, letterSpacing = 4.sp)
    }
    Text(
        "EVENTOM",
        style = base.copy(fontWeight = FontWeight.ExtraBold),
        color = TicketBlue,
        modifier = modifier,
    )
}