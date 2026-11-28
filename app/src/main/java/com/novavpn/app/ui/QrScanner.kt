package com.novavpn.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.novavpn.app.ui.components.AlbumIcon
import com.novavpn.app.ui.components.CloseIcon
import com.novavpn.app.ui.components.FlashIcon
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen QR scanner in the app's own style: live camera, Cancel (top left),
 * and Flash / Album buttons at the bottom. [onResult] gets the raw text of the first QR code found,
 * from the camera or from a photo picked in the album.
 * Drawn as an overlay inside the app window (not a Dialog) so status/navigation bar insets work.
 */
@Composable
fun QrScannerScreen(onResult: (String) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    ScannerContent(onResult, onClose)
}

@Composable
private fun ScannerContent(onResult: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) {
        if (!hasCamera && !askedOnce) { askedOnce = true; permissionLauncher.launch(Manifest.permission.CAMERA) }
    }

    // Coming back from the settings page: pick up a permission granted there.
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // One scanner for everything; the flag makes sure we report a code only once.
    val done = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown(); scanner.close() }
    }
    fun report(text: String) {
        if (done.compareAndSet(false, true)) onResult(text)
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
    LaunchedEffect(camera, torchOn) { camera?.cameraControl?.enableTorch(torchOn && hasFlash) }

    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
        if (image == null) {
            Toast.makeText(context, "Couldn't open this image", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scanner.process(image)
            .addOnSuccessListener { codes ->
                val text = codes.firstNotNullOfOrNull { it.rawValue }
                if (text != null) report(text)
                else Toast.makeText(context, "No QR code found in this image", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { Toast.makeText(context, "Couldn't read this image", Toast.LENGTH_SHORT).show() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Nova.Black)
            .pointerInput(Unit) { detectTapGestures { } }, // block taps from reaching the screen underneath
    ) {
        if (hasCamera) {
            CameraPreview(
                scanner = scanner,
                executor = analysisExecutor,
                lifecycleOwner = lifecycleOwner,
                onCamera = { camera = it },
                onCode = ::report,
            )
            FrameOverlay()
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Camera access is off", color = Nova.White, style = NovaType.Title)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow the camera to scan a QR code, or pick a photo from your album.",
                    color = Nova.TextMuted, style = NovaType.Body, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                ScannerButton(
                    label = "Allow camera",
                    onClick = {
                        // Once the system has stopped asking, the only way back is the app's settings page.
                        runCatching {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(android.net.Uri.fromParts("package", context.packageName, null)),
                            )
                        }
                    },
                )
            }
        }

        // Cancel, top left.
        ScannerButton(
            label = "Cancel",
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 12.dp),
        ) { CloseIcon(Nova.White, Modifier.size(20.dp)) }

        // Flash + Album, footer.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (hasCamera) {
                Text("Scan a server QR code", color = Nova.White, style = NovaType.BodyStrong)
                Spacer(Modifier.height(4.dp))
                Text("Fit the code inside the frame", color = Nova.TextMuted, style = NovaType.Caption)
                Spacer(Modifier.height(20.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasCamera) {
                    ScannerButton(
                        label = if (torchOn) "Flash on" else "Flash",
                        selected = torchOn,
                        enabled = hasFlash,
                        onClick = { torchOn = !torchOn },
                    ) { FlashIcon(if (torchOn) Nova.Lime else Nova.White, filled = torchOn, modifier = Modifier.size(20.dp)) }
                }
                ScannerButton(
                    label = "Album",
                    onClick = { album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ) { AlbumIcon(Nova.White, Modifier.size(20.dp)) }
            }
        }
    }
}

/** Pill button used on the scanner; same look as the Home menu pill and region chips. */
@Composable
private fun ScannerButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val shape = NovaShapes.Pill
    Row(
        modifier
            .height(48.dp)
            // pressedScale = 1f: no size change on press, the colour change shows the state
            .pressable(shape, 1f, enabled = enabled, label = label, onClick = onClick)
            .chipSurface(shape, selected)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.invoke()
        Text(
            label,
            color = when {
                !enabled -> Nova.Gray
                selected -> Nova.Lime
                else -> Nova.White
            },
            style = NovaType.Secondary,
            modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp),
        )
    }
}

@Composable
private fun CameraPreview(
    scanner: BarcodeScanner,
    executor: java.util.concurrent.Executor,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCamera: (Camera?) -> Unit,
    onCode: (String) -> Unit,
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            val p = runCatching { future.get() }.getOrNull() ?: return@addListener
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
                val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(input)
                    .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onCode) }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                p.unbindAll()
                onCamera(p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis))
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { onCamera(null); runCatching { provider?.unbindAll() } }
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}

/** Dimmed screen with a clear rounded square in the middle and lime corner brackets. */
@Composable
private fun FrameOverlay() {
    Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val side = minOf(size.width * 0.7f, 300.dp.toPx())
        val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f - 24.dp.toPx())
        val radius = 28.dp.toPx()
        drawRect(Color.Black.copy(alpha = 0.6f))
        drawRoundRect(Color.Black, topLeft, Size(side, side), CornerRadius(radius), blendMode = BlendMode.Clear)

        val arm = 34.dp.toPx()
        val sw = 4.dp.toPx()
        val l = topLeft.x
        val t = topLeft.y
        val r = l + side
        val b = t + side
        fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
            val path = Path().apply {
                moveTo(cx, cy + dy * arm)
                lineTo(cx, cy + dy * radius)
                quadraticBezierTo(cx, cy, cx + dx * radius, cy)
                lineTo(cx + dx * arm, cy)
            }
            drawPath(path, Nova.Lime, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        corner(l, t, 1f, 1f)
        corner(r, t, -1f, 1f)
        corner(l, b, 1f, -1f)
        corner(r, b, -1f, -1f)
    }
}
