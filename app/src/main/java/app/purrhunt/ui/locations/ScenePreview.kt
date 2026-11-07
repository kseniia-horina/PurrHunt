package app.purrhunt.ui.locations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.Location
import app.purrhunt.domain.SceneSpec
import app.purrhunt.domain.Season
import app.purrhunt.game.engine.EngineSpec
import app.purrhunt.game.engine.GameEngine
import app.purrhunt.game.engine.GameEvents
import app.purrhunt.game.engine.GameRandom
import app.purrhunt.game.render.SceneRenderer
import app.purrhunt.game.scenes.SceneLook
import app.purrhunt.ui.game.GameSurface
import java.time.LocalDate

/** A live, silent, non-interactive piece of the scene with two creatures, shown on location cards. */
@Composable
fun ScenePreview(location: Location, night: Boolean, settings: GameSettings, modifier: Modifier = Modifier) {
    val season = if (settings.seasons) Season.ofMonth(LocalDate.now().monthValue) else Season.SUMMER
    val engine = remember(location, night, season) {
        GameEngine(
            EngineSpec(SceneSpec(location, night, season), GameMode.HUNT, creatureCount = 2, adaptive = false),
            GameRandom(location.ordinal * 31L + if (night) 7 else 0),
            GameEvents.None,
            wallStart = 0L,
        )
    }
    val renderer = remember(location, night, season) { SceneRenderer(SceneLook(night, season), catContrast = false) }
    GameSurface(engine, renderer, onFrame = engine::frame, modifier = modifier, interactive = false, densityScale = 0.6f)
}
