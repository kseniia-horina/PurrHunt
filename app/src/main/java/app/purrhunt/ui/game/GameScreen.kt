package app.purrhunt.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.purrhunt.R
import app.purrhunt.domain.GameMode

@Composable
fun GameScreen(
    onFinished: (Long) -> Unit,
    onChangeLocation: (GameMode) -> Unit,
    onChangeMode: () -> Unit,
    onExitToMenu: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.pause_title))
        Button(onClick = { onChangeLocation(GameMode.HUNT) }) { Text(stringResource(R.string.pause_change_location)) }
        Button(onClick = onChangeMode) { Text(stringResource(R.string.pause_change_mode)) }
        Button(onClick = onExitToMenu) { Text(stringResource(R.string.pause_to_menu)) }
    }
}
