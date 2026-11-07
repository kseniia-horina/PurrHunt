package app.purrhunt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.purrhunt.DebugTools
import app.purrhunt.data.SettingsRepository
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.Location
import app.purrhunt.ui.achievements.AchievementsScreen
import app.purrhunt.ui.album.AlbumScreen
import app.purrhunt.ui.cats.CatEditScreen
import app.purrhunt.ui.cats.CatsScreen
import app.purrhunt.ui.game.GameScreen
import app.purrhunt.ui.journal.JournalScreen
import app.purrhunt.ui.locations.LocationsScreen
import app.purrhunt.ui.locations.ModesScreen
import app.purrhunt.ui.menu.MenuScreen
import app.purrhunt.ui.onboarding.OnboardingScreen
import app.purrhunt.ui.settings.SettingsScreen
import app.purrhunt.ui.summary.SummaryScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

object Routes {
    const val ONBOARDING = "onboarding"
    const val MENU = "menu"
    const val CATS = "cats"
    const val CAT_EDIT = "cat/{id}"
    const val MODES = "modes"
    const val LOCATIONS = "locations/{mode}"
    const val GAME = "game/{location}/{night}/{mode}"
    const val SUMMARY = "summary/{sessionId}"
    const val JOURNAL = "journal"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"
    const val ALBUM = "album"
    const val ZOO = "zoo"

    fun catEdit(id: Long) = "cat/$id"
    fun locations(mode: GameMode) = "locations/${mode.name}"
    fun game(location: Location, night: Boolean, mode: GameMode) = "game/${location.name}/$night/${mode.name}"
    fun summary(sessionId: Long) = "summary/$sessionId"
}

@HiltViewModel
class StartViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    /** Null while DataStore is loading; the splash screen stays visible until then. */
    val startDestination: StateFlow<String?> = settings.appState
        .take(1)
        .map { if (it.onboardingDone) Routes.MENU else Routes.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun PurrNavHost(startViewModel: StartViewModel, openZoo: Boolean = false, nav: NavHostController = rememberNavController()) {
    val start by startViewModel.startDestination.collectAsStateWithLifecycle()
    val startRoute = start ?: return
    if (openZoo && DebugTools.zooAvailable) {
        LaunchedEffect(Unit) { nav.navigate(Routes.ZOO) }
    }

    NavHost(navController = nav, startDestination = startRoute) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.MENU) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.MENU) {
            MenuScreen(
                onPlay = { nav.navigate(Routes.locations(GameMode.HUNT)) },
                onModes = { nav.navigate(Routes.MODES) },
                onJournal = { nav.navigate(Routes.JOURNAL) },
                onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onCats = { nav.navigate(Routes.CATS) },
                onAddCat = { nav.navigate(Routes.catEdit(0)) },
                onAlbum = { nav.navigate(Routes.ALBUM) },
                onZoo = if (DebugTools.zooAvailable) ({ nav.navigate(Routes.ZOO) }) else null,
            )
        }
        composable(Routes.CATS) {
            CatsScreen(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.catEdit(it)) },
            )
        }
        composable(Routes.CAT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            CatEditScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.MODES) {
            ModesScreen(
                onBack = { nav.popBackStack() },
                onMode = { mode ->
                    if (mode == GameMode.HUNT) nav.navigate(Routes.locations(mode))
                    else nav.navigate(Routes.game(Location.MEADOW, false, mode))
                },
            )
        }
        composable(Routes.LOCATIONS, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
            val mode = GameMode.valueOf(entry.arguments?.getString("mode") ?: GameMode.HUNT.name)
            LocationsScreen(
                onBack = { nav.popBackStack() },
                onLocation = { location, night -> nav.navigate(Routes.game(location, night, mode)) },
            )
        }
        composable(
            Routes.GAME,
            arguments = listOf(
                navArgument("location") { type = NavType.StringType },
                navArgument("night") { type = NavType.BoolType },
                navArgument("mode") { type = NavType.StringType },
            ),
        ) {
            GameScreen(
                onFinished = { sessionId ->
                    nav.navigate(Routes.summary(sessionId)) { popUpTo(Routes.MENU) }
                },
                onChangeLocation = { mode -> nav.navigate(Routes.locations(mode)) { popUpTo(Routes.MENU) } },
                onChangeMode = { nav.navigate(Routes.MODES) { popUpTo(Routes.MENU) } },
                onExitToMenu = { nav.popBackStack(Routes.MENU, inclusive = false) },
            )
        }
        composable(Routes.SUMMARY, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) {
            SummaryScreen(
                onPlayAgain = { location, night, mode ->
                    nav.navigate(Routes.game(location, night, mode)) { popUpTo(Routes.MENU) }
                },
                onMenu = { nav.popBackStack(Routes.MENU, inclusive = false) },
            )
        }
        composable(Routes.JOURNAL) {
            JournalScreen(
                onBack = { nav.popBackStack() },
                onPlayLocation = { location -> nav.navigate(Routes.game(location, false, GameMode.HUNT)) },
            )
        }
        composable(Routes.ACHIEVEMENTS) { AchievementsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ALBUM) { AlbumScreen(onBack = { nav.popBackStack() }) }
        if (DebugTools.zooAvailable) {
            composable(Routes.ZOO) { DebugTools.Zoo(onBack = { nav.popBackStack() }) }
        }
    }
}
