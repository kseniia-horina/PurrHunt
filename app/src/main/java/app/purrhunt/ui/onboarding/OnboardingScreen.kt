package app.purrhunt.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.SettingsRepository
import app.purrhunt.ui.components.drawPaw
import app.purrhunt.ui.theme.PurrColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    fun finish(then: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingDone()
            then()
        }
    }
}

private data class Slide(val title: Int, val text: Int)

private val slides = listOf(
    Slide(R.string.onb_title_1, R.string.onb_text_1),
    Slide(R.string.onb_title_2, R.string.onb_text_2),
    Slide(R.string.onb_title_3, R.string.onb_text_3),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val finish = { viewModel.finish(onDone) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = finish, modifier = Modifier.testTag("onboarding_skip")) { Text(stringResource(R.string.onb_skip)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).testTag("onboarding_pager")) { page ->
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Illustration(page, Modifier.size(200.dp))
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(slides[page].title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(slides[page].text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            }
        }
        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(slides.size) { i ->
                Box(
                    Modifier
                        .size(if (i == pager.currentPage) 12.dp else 8.dp)
                        .background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                )
            }
        }
        val last = pager.currentPage == slides.lastIndex
        Button(
            onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).height(56.dp).testTag("onboarding_next"),
        ) {
            Text(stringResource(if (last) R.string.onb_start else R.string.onb_next))
        }
    }
}

@Composable
private fun Illustration(page: Int, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (page) {
            0 -> {
                // A phone with a hold ring in its top right corner.
                drawRoundRect(PurrColors.NightSurfaceHigh, Offset(w * 0.22f, h * 0.05f), Size(w * 0.56f, h * 0.9f), androidx.compose.ui.geometry.CornerRadius(24f))
                val c = Offset(w * 0.68f, h * 0.15f)
                drawCircle(PurrColors.Outline, radius = w * 0.08f, center = c, style = Stroke(6f))
                drawArc(PurrColors.Amber, -90f, 250f, false, Offset(c.x - w * 0.08f, c.y - w * 0.08f), Size(w * 0.16f, w * 0.16f), style = Stroke(6f, cap = StrokeCap.Round))
                drawCircle(PurrColors.Cream.copy(alpha = 0.8f), radius = w * 0.04f, center = c)
            }
            1 -> {
                // A shield over a screen.
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.84f, h * 0.2f)
                    cubicTo(w * 0.84f, h * 0.6f, w * 0.7f, h * 0.8f, w * 0.5f, h * 0.92f)
                    cubicTo(w * 0.3f, h * 0.8f, w * 0.16f, h * 0.6f, w * 0.16f, h * 0.2f)
                    close()
                }
                drawPath(p, PurrColors.Sky.copy(alpha = 0.25f))
                drawPath(p, PurrColors.Sky, style = Stroke(6f))
                drawPaw(PurrColors.Amber, Offset(w * 0.5f, h * 0.5f), w * 0.36f)
            }
            else -> {
                // A fish-shaped treat next to a paw.
                val body = Path().apply {
                    moveTo(w * 0.18f, h * 0.5f)
                    cubicTo(w * 0.3f, h * 0.3f, w * 0.55f, h * 0.3f, w * 0.68f, h * 0.5f)
                    cubicTo(w * 0.55f, h * 0.7f, w * 0.3f, h * 0.7f, w * 0.18f, h * 0.5f)
                    close()
                    moveTo(w * 0.66f, h * 0.5f)
                    lineTo(w * 0.84f, h * 0.36f)
                    lineTo(w * 0.84f, h * 0.64f)
                    close()
                }
                drawPath(body, PurrColors.Amber)
                drawCircle(Color(0xFF2B1E00), radius = w * 0.025f, center = Offset(w * 0.3f, h * 0.47f))
                drawPaw(PurrColors.Cream.copy(alpha = 0.6f), Offset(w * 0.5f, h * 0.86f), w * 0.2f)
            }
        }
    }
}
