package app.purrhunt.ui.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.purrhunt.R
import app.purrhunt.ui.components.PurrScaffold

@Composable
fun AlbumScreen(onBack: () -> Unit) {
    PurrScaffold(title = stringResource(R.string.album_title), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.album_empty))
        }
    }
}
