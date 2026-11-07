package app.purrhunt

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.purrhunt.ui.navigation.PurrNavHost
import app.purrhunt.ui.navigation.StartViewModel
import app.purrhunt.ui.theme.PurrHuntTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val startViewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { startViewModel.startDestination.value == null }
        enableEdgeToEdge()
        setContent {
            PurrHuntTheme {
                PurrNavHost(startViewModel)
            }
        }
    }
}
