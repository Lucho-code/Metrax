package com.example.ar.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Draws the live ARCore camera feed as a full-screen textured quad using an
 * external OES texture, following the standard ARCore background-rendering
 * idiom. UV coordinates are re-derived every frame via
 * [Frame.transformCoordinates2d] since the camera-to-view transform changes
 * with device rotation and display geometry.
 */
class BackgroundRenderer {

    var textureId: Int = -1
        private set

    private var quadProgram = 0
    private var quadPositionAttrib = 0
    private var quadTexCoordAttrib = 0
    private var quadTextureUniform = 0

    // Full-screen quad corners in normalized device coordinates (2 floats/vertex).
    private val quadPositions: FloatBuffer = directFloatBuffer(8).apply {
        put(floatArrayOf(-1f, -1f, -1f, +1f, +1f, -1f, +1f, +1f))
        position(0)
    }

    // Re-derived every frame from quadPositions via transformCoordinates2d.
    private val quadTexCoords: FloatBuffer = directFloatBuffer(8)

    fun createOnGlThread() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        quadProgram = ShaderUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        quadPositionAttrib = GLES20.glGetAttribLocation(quadProgram, "a_Position")
        quadTexCoordAttrib = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord")
        quadTextureUniform = GLES20.glGetUniformLocation(quadProgram, "u_Texture")
    }

    /** Recomputes the display-space UV mapping for the current frame's geometry. */
    fun updateDisplayGeometry(frame: Frame) {
        if (frame.hasDisplayGeometryChanged()) {
            quadPositions.position(0)
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                quadPositions,
                Coordinates2d.TEXTURE_NORMALIZED,
                quadTexCoords
            )
            quadPositions.position(0)
            quadTexCoords.position(0)
        }
    }

    fun draw() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(quadProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(quadTextureUniform, 0)

        quadPositions.position(0)
        GLES20.glVertexAttribPointer(quadPositionAttrib, 2, GLES20.GL_FLOAT, false, 0, quadPositions)
        quadTexCoords.position(0)
        GLES20.glVertexAttribPointer(quadTexCoordAttrib, 2, GLES20.GL_FLOAT, false, 0, quadTexCoords)

        GLES20.glEnableVertexAttribArray(quadPositionAttrib)
        GLES20.glEnableVertexAttribArray(quadTexCoordAttrib)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(quadPositionAttrib)
        GLES20.glDisableVertexAttribArray(quadTexCoordAttrib)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        ShaderUtil.checkGlError("BackgroundRenderer.draw")
    }

    companion object {
        private fun directFloatBuffer(capacity: Int): FloatBuffer =
            ByteBuffer.allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
    }
}
