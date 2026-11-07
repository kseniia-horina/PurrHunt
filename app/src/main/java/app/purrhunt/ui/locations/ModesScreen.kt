package app.purrhunt.ui.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.purrhunt.R
import app.purrhunt.domain.GameMode
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.descRes
import app.purrhunt.ui.components.nameRes

@Composable
fun ModesScreen(onBack: () -> Unit, onMode: (GameMode) -> Unit) {
    PurrScaffold(title = stringResource(R.string.modes_title), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameMode.entries.forEach { mode ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onMode(mode) }
                        .testTag("mode_${mode.name}"),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(mode.nameRes), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(mode.descRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
