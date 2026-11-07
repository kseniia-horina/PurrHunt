package app.purrhunt.ui.game

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Keeps the screen on and hides the system bars (immersive sticky) while the game is shown. */
@Composable
fun ImmersiveGameWindow() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

object ScreenPinning {
    fun start(activity: Activity) {
        if (!isPinned(activity)) runCatching { activity.startLockTask() }
    }

    fun stop(activity: Activity) {
        if (isPinned(activity)) runCatching { activity.stopLockTask() }
    }

    fun isPinned(context: Context): Boolean =
        context.getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
}
