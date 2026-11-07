package app.purrhunt.ui.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.Location

@Composable
fun ScenePreview(location: Location, night: Boolean, settings: GameSettings, modifier: Modifier = Modifier) {
    val colors = when {
        night -> listOf(Color(0xFF0B1022), Color(0xFF1B2440))
        location == Location.BASEMENT -> listOf(Color(0xFF4A3A33), Color(0xFF2B2420))
        location == Location.GARDEN -> listOf(Color(0xFF55705A), Color(0xFF3A4D3C))
        location == Location.MEADOW -> listOf(Color(0xFF6D7F55), Color(0xFF465538))
        location == Location.POND -> listOf(Color(0xFF2F5568), Color(0xFF1C3442))
        else -> listOf(Color(0xFF4F6656), Color(0xFF2C4250))
    }
    Box(modifier.background(Brush.verticalGradient(colors)))
}
