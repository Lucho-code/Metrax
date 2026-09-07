package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ar.ArAvailability
import com.example.ar.ArFrameUiState
import com.example.ar.ArVolumeRenderer
import com.example.data.model.ArVolumeResult
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.delay

/**
 * Hosts a live ARCore session: real camera feed + sparse point cloud +
 * plane/depth tracking, rendered via [ArVolumeRenderer] on a [GLSurfaceView].
 * Handles camera permission, the ARCore Play Services install flow, and
 * ties the ARCore [Session] and GL thread to the host lifecycle.
 */
@Composable
fun ArCameraView(
    modifier: Modifier = Modifier,
    onUiState: (ArFrameUiState) -> Unit,
    onToePointsChanged: () -> Unit,
    onVolumeResult: (ArVolumeResult) -> Unit,
    onAvailabilityChanged: (ArAvailability) -> Unit,
    onRendererReady: (ArVolumeRenderer) -> Unit,
    onGlSurfaceViewReady: (GLSurfaceView) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) onAvailabilityChanged(ArAvailability.NeedsCameraPermission)
    }

    var installRequested by remember { mutableStateOf(false) }
    var resumeTrigger by remember { mutableStateOf(0) }
    var session by remember { mutableStateOf<Session?>(null) }
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }

    val renderer = remember {
        ArVolumeRenderer(
            getDisplayRotation = {
                @Suppress("DEPRECATION")
                (context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager)
                    .defaultDisplay.rotation
            },
            onUiState = onUiState,
            onToePointsChanged = onToePointsChanged,
            onVolumeResult = onVolumeResult
        )
    }
    LaunchedEffect(renderer) { onRendererReady(renderer) }

    // Availability check + ARCore Play Services install flow + Session creation.
    // Re-keyed on resumeTrigger so returning from the Play Store install flow retries.
    LaunchedEffect(hasCameraPermission, resumeTrigger) {
        if (!hasCameraPermission || activity == null || session != null) return@LaunchedEffect
        onAvailabilityChanged(ArAvailability.Checking)

        while (true) {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            if (availability.isTransient) {
                delay(200)
                continue
            }
            if (!availability.isSupported) {
                onAvailabilityChanged(
                    ArAvailability.Unsupported("Este dispositivo no es compatible con mediciones AR de nube de puntos.")
                )
                return@LaunchedEffect
            }
            try {
                val installStatus = ArCoreApk.getInstance().requestInstall(activity, !installRequested)
                if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    installRequested = true
                    onAvailabilityChanged(ArAvailability.NeedsInstall)
                    return@LaunchedEffect
                }
                val newSession = Session(context)
                newSession.configure(
                    Config(newSession).apply {
                        focusMode = Config.FocusMode.AUTO
                        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                        depthMode = if (newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                            Config.DepthMode.AUTOMATIC
                        } else {
                            Config.DepthMode.DISABLED
                        }
                    }
                )
                newSession.resume()
                renderer.session = newSession
                session = newSession
                onAvailabilityChanged(ArAvailability.Ready)
            } catch (e: UnavailableUserDeclinedInstallationException) {
                onAvailabilityChanged(ArAvailability.Unsupported("Instalación de ARCore rechazada."))
            } catch (e: UnavailableArcoreNotInstalledException) {
                onAvailabilityChanged(ArAvailability.NeedsInstall)
            } catch (e: UnavailableApkTooOldException) {
                onAvailabilityChanged(ArAvailability.NeedsInstall)
            } catch (e: UnavailableSdkTooOldException) {
                onAvailabilityChanged(ArAvailability.Unsupported("Actualizá la app para usar mediciones AR."))
            } catch (e: UnavailableDeviceNotCompatibleException) {
                onAvailabilityChanged(ArAvailability.Unsupported("Este dispositivo no es compatible con ARCore."))
            } catch (e: CameraNotAvailableException) {
                onAvailabilityChanged(ArAvailability.Unsupported("La cámara está en uso por otra app."))
            } catch (e: Exception) {
                onAvailabilityChanged(ArAvailability.Unsupported("No se pudo iniciar AR: ${e.message}"))
            }
            return@LaunchedEffect
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val s = session
                    if (s != null) {
                        try {
                            s.resume()
                            renderer.session = s
                            glSurfaceView?.onResume()
                        } catch (_: CameraNotAvailableException) {
                            onAvailabilityChanged(ArAvailability.Unsupported("La cámara está en uso por otra app."))
                        }
                    } else {
                        // May be returning from the ARCore/Play Store install flow; retry setup.
                        resumeTrigger++
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceView?.onPause()
                    session?.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            session?.close()
        }
    }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        if (hasCameraPermission && session != null) {
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(2)
                        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        val gestureDetector = GestureDetector(
                            ctx,
                            object : GestureDetector.SimpleOnGestureListener() {
                                override fun onSingleTapUp(e: MotionEvent): Boolean {
                                    renderer.postTap(e.x, e.y)
                                    return true
                                }
                            }
                        )
                        setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
                        glSurfaceView = this
                        onGlSurfaceViewReady(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
