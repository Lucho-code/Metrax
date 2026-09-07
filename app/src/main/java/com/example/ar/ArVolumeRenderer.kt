package com.example.ar

import android.opengl.GLES20
import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import com.example.ar.gl.BackgroundRenderer
import com.example.ar.gl.PointCloudRenderer
import com.example.data.model.ArVolumeResult
import com.example.data.model.WorldPoint
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.PointCloud
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

/**
 * Drives the live ARCore session on the GL thread: draws the camera feed and
 * sparse point cloud, converts screen taps into world-anchored toe/boundary
 * points via depth-aware hit-testing, and runs the grid-mesh volume scan
 * (see [VolumeCalculator]) on demand.
 *
 * All ARCore/GL access happens on the GL thread. External callers only ever
 * queue commands ([postTap], [postUndo], [postReset], [postComputeVolume]);
 * results are published back via the constructor callbacks, which are
 * invoked on the GL thread — callers must hop to the main thread themselves
 * if needed (a [kotlinx.coroutines.flow.MutableStateFlow] update is safe as-is).
 */
class ArVolumeRenderer(
    private val getDisplayRotation: () -> Int,
    private val onUiState: (ArFrameUiState) -> Unit,
    private val onToePointsChanged: () -> Unit,
    private val onVolumeResult: (ArVolumeResult) -> Unit
) : android.opengl.GLSurfaceView.Renderer {

    @Volatile
    var session: Session? = null

    private val backgroundRenderer = BackgroundRenderer()
    private val pointCloudRenderer = PointCloudRenderer()

    private var viewportWidth = 1
    private var viewportHeight = 1
    @Volatile
    private var viewportChanged = true

    private val commandLock = Any()
    private val pendingCommands = ArrayDeque<(Frame) -> Unit>()

    private val toeAnchors = mutableListOf<com.google.ar.core.Anchor>()

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var pendingMessage: String? = null

    fun postTap(x: Float, y: Float) {
        synchronized(commandLock) { pendingCommands.addLast { frame -> handleTap(frame, x, y) } }
    }

    fun postUndo() {
        synchronized(commandLock) { pendingCommands.addLast { handleUndo() } }
    }

    fun postReset() {
        synchronized(commandLock) { pendingCommands.addLast { handleReset() } }
    }

    fun postComputeVolume(gridResolution: Int) {
        val clamped = VolumeCalculator.clampGridResolution(gridResolution)
        synchronized(commandLock) { pendingCommands.addLast { frame -> handleComputeVolume(frame, clamped) } }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        backgroundRenderer.createOnGlThread()
        pointCloudRenderer.createOnGlThread()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = this.session ?: return

        if (viewportChanged) {
            runCatching { session.setDisplayGeometry(getDisplayRotation(), viewportWidth, viewportHeight) }
            viewportChanged = false
        }

        try {
            session.setCameraTextureName(backgroundRenderer.textureId)
            val frame = session.update()
            val camera = frame.camera

            drainCommandQueue(frame)

            backgroundRenderer.updateDisplayGeometry(frame)
            backgroundRenderer.draw()

            var uiPointCloud: List<Offset> = emptyList()
            var uiToePoints: List<Offset> = emptyList()
            var planeDetected = false

            if (camera.trackingState == TrackingState.TRACKING) {
                camera.getViewMatrix(viewMatrix, 0)
                camera.getProjectionMatrix(projMatrix, 0, Z_NEAR, Z_FAR)
                Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

                var pointCloud: PointCloud? = null
                try {
                    pointCloud = frame.acquirePointCloud()
                    pointCloudRenderer.draw(pointCloud, vpMatrix)
                    uiPointCloud = sampleForOverlay(pointCloud)
                } catch (_: Exception) {
                    // No point cloud resolved for this frame yet; skip.
                } finally {
                    pointCloud?.close()
                }

                uiToePoints = toeAnchors.mapNotNull { anchor ->
                    if (anchor.trackingState != TrackingState.TRACKING) return@mapNotNull null
                    projectToScreen(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
                        ?.let { (sx, sy) -> Offset(sx, sy) }
                }

                planeDetected = session.getAllTrackables(Plane::class.java)
                    .any { it.trackingState == TrackingState.TRACKING }
            }

            val status = when (camera.trackingState) {
                TrackingState.TRACKING -> ArTrackingStatus.TRACKING
                TrackingState.PAUSED -> ArTrackingStatus.INITIALIZING
                else -> ArTrackingStatus.LOST
            }

            onUiState(
                ArFrameUiState(
                    trackingStatus = status,
                    planeDetected = planeDetected,
                    pointCloudScreenPoints = uiPointCloud,
                    toePointsScreen = uiToePoints,
                    statusMessage = pendingMessage ?: defaultMessage(status, planeDetected, toeAnchors.size)
                )
            )
            pendingMessage = null
        } catch (_: CameraNotAvailableException) {
            onUiState(ArFrameUiState(statusMessage = "Cámara no disponible. Cerrá otras apps que la estén usando."))
        } catch (_: Exception) {
            // Transient error (e.g. session paused mid-frame); skip this frame.
        }
    }

    private fun defaultMessage(status: ArTrackingStatus, planeDetected: Boolean, toeCount: Int): String = when {
        status != ArTrackingStatus.TRACKING -> "Moové el celular lentamente para que el sistema detecte el entorno."
        !planeDetected -> "Buscando el plano del suelo… apuntá hacia la base del material."
        toeCount < 3 -> "Tocá el contorno de la base del material (mínimo 3 puntos), rodeándolo."
        else -> "Contorno listo. Alejate para ver todo el material y presioná \"Calcular volumen\"."
    }

    private fun drainCommandQueue(frame: Frame) {
        val commands: List<(Frame) -> Unit>
        synchronized(commandLock) {
            if (pendingCommands.isEmpty()) return
            commands = pendingCommands.toList()
            pendingCommands.clear()
        }
        commands.forEach { it(frame) }
    }

    private fun handleTap(frame: Frame, x: Float, y: Float) {
        if (frame.camera.trackingState != TrackingState.TRACKING) return
        val hit = firstValidHit(frame.hitTest(x, y))
        if (hit == null) {
            pendingMessage = "No se detectó superficie en ese punto. Probá tocar sobre el contorno visible del material."
            return
        }
        toeAnchors.add(hit.createAnchor())
        onToePointsChanged()
    }

    private fun handleUndo(@Suppress("UNUSED_PARAMETER") frame: Frame? = null) {
        if (toeAnchors.isNotEmpty()) {
            toeAnchors.removeAt(toeAnchors.size - 1).detach()
            onToePointsChanged()
        }
    }

    private fun handleReset(@Suppress("UNUSED_PARAMETER") frame: Frame? = null) {
        if (toeAnchors.isNotEmpty()) {
            toeAnchors.forEach { it.detach() }
            toeAnchors.clear()
            onToePointsChanged()
        }
    }

    private fun handleComputeVolume(frame: Frame, gridResolution: Int) {
        if (toeAnchors.size < 3) {
            pendingMessage = "Marcá al menos 3 puntos en el contorno de la base antes de calcular el volumen."
            return
        }

        val toeWorldPoints = toeAnchors.map { it.pose.toWorldPoint() }
        val toeScreenPoints = toeAnchors.mapNotNull { anchor ->
            projectToScreen(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
        }

        if (toeScreenPoints.size < 3) {
            pendingMessage = "Alejate hasta que se vean todos los puntos del contorno en pantalla."
            return
        }

        val minX = toeScreenPoints.minOf { it.first }
        val maxX = toeScreenPoints.maxOf { it.first }
        val minY = toeScreenPoints.minOf { it.second }
        val maxY = toeScreenPoints.maxOf { it.second }
        val stepX = (maxX - minX) / gridResolution
        val stepY = (maxY - minY) / gridResolution
        if (stepX <= 0f || stepY <= 0f) {
            pendingMessage = "No se pudo medir el área: el contorno se ve demasiado pequeño en pantalla."
            return
        }

        val mesh: MutableList<MutableList<WorldPoint?>> = MutableList(gridResolution + 1) {
            MutableList<WorldPoint?>(gridResolution + 1) { null }
        }

        for (row in 0..gridResolution) {
            val py = minY + stepY * row
            for (col in 0..gridResolution) {
                val px = minX + stepX * col
                if (!VolumeCalculator.isInsidePolygon(px, py, toeScreenPoints)) continue
                val hit = firstValidHit(frame.hitTest(px, py)) ?: continue
                mesh[row][col] = hit.hitPose.toWorldPoint()
            }
        }

        val trackingRatio = toeAnchors.count { it.trackingState == TrackingState.TRACKING }.toFloat() / toeAnchors.size
        val result = VolumeCalculator.buildResult(toeWorldPoints, mesh, trackingRatio, gridResolution)
        if (result == null) {
            pendingMessage = "No se pudo reconstruir suficiente superficie. Probá acercarte o mejorar la iluminación."
            return
        }
        onVolumeResult(result)
    }

    private fun firstValidHit(hits: List<HitResult>): HitResult? {
        return hits.firstOrNull { hit ->
            when (val trackable = hit.trackable) {
                is Plane -> trackable.isPoseInPolygon(hit.hitPose) && trackable.trackingState == TrackingState.TRACKING
                is DepthPoint -> true
                is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                else -> false
            }
        }
    }

    private fun projectToScreen(worldX: Float, worldY: Float, worldZ: Float): Pair<Float, Float>? {
        val world = floatArrayOf(worldX, worldY, worldZ, 1f)
        val clip = FloatArray(4)
        Matrix.multiplyMV(clip, 0, vpMatrix, 0, world, 0)
        if (clip[3] <= 0.0001f) return null
        val ndcX = clip[0] / clip[3]
        val ndcY = clip[1] / clip[3]
        if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) return null
        val screenX = (ndcX * 0.5f + 0.5f) * viewportWidth
        val screenY = (1f - (ndcY * 0.5f + 0.5f)) * viewportHeight
        return screenX to screenY
    }

    private fun sampleForOverlay(pointCloud: PointCloud): List<Offset> {
        val buffer = pointCloud.points
        val totalPoints = buffer.remaining() / 4
        if (totalPoints == 0) return emptyList()
        val duplicate = buffer.duplicate()
        val stride = max(1, totalPoints / MAX_OVERLAY_POINTS)
        val result = ArrayList<Offset>(min(totalPoints, MAX_OVERLAY_POINTS))
        var index = 0
        while (index < totalPoints) {
            val base = index * 4
            val x = duplicate.get(base)
            val y = duplicate.get(base + 1)
            val z = duplicate.get(base + 2)
            projectToScreen(x, y, z)?.let { (sx, sy) -> result.add(Offset(sx, sy)) }
            index += stride
        }
        return result
    }

    private fun Pose.toWorldPoint() = WorldPoint(tx(), ty(), tz())

    companion object {
        private const val Z_NEAR = 0.05f
        private const val Z_FAR = 50f
        private const val MAX_OVERLAY_POINTS = 300
    }
}
