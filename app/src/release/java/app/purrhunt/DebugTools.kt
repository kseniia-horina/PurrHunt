package app.purrhunt

import androidx.compose.runtime.Composable

/** Release builds ship without the debug zoo. */
object DebugTools {
    const val zooAvailable = false

    @Composable
    fun Zoo(onBack: () -> Unit) = Unit
}
