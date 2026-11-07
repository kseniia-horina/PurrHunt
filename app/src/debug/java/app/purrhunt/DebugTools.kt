package app.purrhunt

import androidx.compose.runtime.Composable
import app.purrhunt.zoo.ZooScreen

/** Debug-only tools: the creature zoo for visual checks of every animation. */
object DebugTools {
    const val zooAvailable = true

    @Composable
    fun Zoo(onBack: () -> Unit) = ZooScreen(onBack)
}
