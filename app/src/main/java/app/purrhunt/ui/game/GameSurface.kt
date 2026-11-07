package app.purrhunt.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import app.purrhunt.game.engine.GameEngine
import app.purrhunt.game.render.FrameRenderer
import app.purrhunt.game.render.SceneRenderer

/**
 * Compose host of the game: a full-screen Canvas redrawn every display frame, multi-touch input forwarded
 * to the engine. Only the draw phase is invalidated per frame (the frame counter is read inside onDraw),
 * so there is no recomposition during play.
 */
@Composable
fun GameSurface(
    engine: GameEngine,
    renderer: SceneRenderer,
    onFrame: (Long) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    /** Below 1 shrinks the world (creatures, props) for small previews. */
    densityScale: Float = 1f,
) {
    val density = LocalDensity.current.density * densityScale
    val frame = remember { mutableLongStateOf(0L) }
    LaunchedEffect(engine) {
        engine.resetClock()
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                onFrame(t)
                frame.longValue = t
            }
        }
    }
    val input = if (interactive) {
        Modifier.pointerInput(engine) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        val id = change.id.value.toInt()
                        val p = change.position
                        when {
                            change.pressed && !change.previousPressed -> engine.pointerDown(id, p.x, p.y)
                            !change.pressed && change.previousPressed -> engine.pointerUp(id)
                            change.pressed -> engine.pointerMove(id, p.x, p.y)
                        }
                        change.consume()
                    }
                    if (event.type == PointerEventType.Exit) engine.pointersCancelled()
                }
            }
        }
    } else {
        Modifier
    }
    Canvas(
        modifier
            .fillMaxSize()
            .onSizeChanged { engine.resize(it.width.toFloat(), it.height.toFloat(), density) }
            .then(input)
            .testTag("game_surface"),
    ) {
        frame.longValue // read so every display frame invalidates the draw
        renderer.prepare(engine, density)
        with(renderer as FrameRenderer) { render(engine, engine.world.time.toFloat()) }
    }
}
