package app.purrhunt.ui.game.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Front camera for the game screen. Takes a silent still whenever [requests] emits, and optionally shows
 * a small live preview so that a screen recording contains the cat's face picture-in-picture.
 */
@Composable
fun CatCamera(
    requests: Flow<String>,
    showPreview: Boolean,
    onSaved: (path: String, tag: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val capture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val previewView = remember { if (showPreview) PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } else null }

    DisposableEffect(owner) {
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            provider = runCatching { future.get() }.getOrNull()
            val p = provider ?: return@addListener
            val useCases = buildList {
                add(capture)
                previewView?.let { add(Preview.Builder().build().also { pr -> pr.surfaceProvider = it.surfaceProvider }) }
            }
            runCatching {
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, *useCases.toTypedArray())
            }.onFailure { Log.w(TAG, "front camera unavailable", it) }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    LaunchedEffect(requests) {
        requests.collect { tag -> takePhoto(context, capture, tag, onSaved) }
    }

    if (previewView != null) {
        AndroidView(
            factory = { previewView },
            modifier = modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(16.dp)).alpha(0.9f),
        )
    }
}

private fun takePhoto(context: Context, capture: ImageCapture, tag: String, onSaved: (String, String) -> Unit) {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    val file = File(dir, "catch_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file)
        .setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = true })
        .build()
    capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved(file.absolutePath, tag)
        override fun onError(exception: ImageCaptureException) {
            Log.w(TAG, "photo failed", exception)
        }
    })
}

private const val TAG = "PurrHuntCamera"
