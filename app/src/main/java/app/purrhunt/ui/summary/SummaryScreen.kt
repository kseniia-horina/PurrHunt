package app.purrhunt.ui.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.purrhunt.R
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.Location
import app.purrhunt.ui.components.PurrScaffold

@Composable
fun SummaryScreen(onPlayAgain: (Location, Boolean, GameMode) -> Unit, onMenu: () -> Unit) {
    PurrScaffold(title = stringResource(R.string.summary_title), onBack = null) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Button(onClick = onMenu) { Text(stringResource(R.string.summary_to_menu)) }
        }
    }
}
