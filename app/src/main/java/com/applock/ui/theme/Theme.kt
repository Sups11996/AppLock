package com.applock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Colour palette ──────────────────────────────────────────────────────────
// A restrained dark palette — deep surface with teal/cyan accents.
private val DarkColorScheme = darkColorScheme(
    primary        = Color(0xFF80CBC4),   // muted teal
    onPrimary      = Color(0xFF003733),
    primaryContainer   = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFA7F3EC),

    secondary      = Color(0xFF90CAF9),   // soft blue
    onSecondary    = Color(0xFF0D2B4D),
    secondaryContainer = Color(0xFF1A3D6D),
    onSecondaryContainer = Color(0xFFD6EAFF),

    background     = Color(0xFF0F0F0F),
    onBackground   = Color(0xFFE4E4E4),

    surface        = Color(0xFF1A1A1A),
    onSurface      = Color(0xFFE4E4E4),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBDBDBD),

    error          = Color(0xFFCF6679),
    onError        = Color(0xFF370B1E),
)

// ── Typography ──────────────────────────────────────────────────────────────
// Using default Material3 type scale for now — can swap font family later.
private val AppTypography = Typography()

// ── Theme entry-point ───────────────────────────────────────────────────────
@Composable
fun AppLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
