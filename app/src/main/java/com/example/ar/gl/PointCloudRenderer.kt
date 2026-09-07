package com.example.ar.gl

import android.opengl.GLES20
import com.google.ar.core.PointCloud

/**
 * Renders ARCore's sparse feature point cloud (the real "nube de puntos"
 * reconstructed from the phone camera + visual-inertial odometry) as
 * colored GL points, matching the reference stockpile-scanner visualization.
 */
class PointCloudRenderer {

    private var program = 0
    private var positionAttrib = 0
    private var mvpMatrixUniform = 0
    private var colorUniform = 0
    private var pointSizeUniform = 0

    fun createOnGlThread() {
        program = ShaderUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        mvpMatrixUniform = GLES20.glGetUniformLocation(program, "u_MvpMatrix")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
        pointSizeUniform = GLES20.glGetUniformLocation(program, "u_PointSize")
    }

    /**
     * @param pointCloud current frame's point cloud (xyzc quadruples, confidence in [3]).
     * @param mvpMatrix combined model-view-projection matrix (16 floats).
     */
    fun draw(pointCloud: PointCloud, mvpMatrix: FloatArray) {
        val numPoints = pointCloud.points.remaining() / 4
        if (numPoints == 0) return

        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(
            positionAttrib,
            4,
            GLES20.GL_FLOAT,
            false,
            16,
            pointCloud.points
        )
        GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0)
        GLES20.glUniform4f(colorUniform, 0.35f, 0.9f, 0.5f, 1.0f)
        GLES20.glUniform1f(pointSizeUniform, 8.0f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
        GLES20.glDisableVertexAttribArray(positionAttrib)
        ShaderUtil.checkGlError("PointCloudRenderer.draw")
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            uniform float u_PointSize;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MvpMatrix * vec4(a_Position.xyz, 1.0);
                gl_PointSize = u_PointSize;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """
    }
}
