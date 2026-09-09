package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConfidenceLevel
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryOrange
import kotlin.math.cos
import kotlin.math.sin

/**
 * Semicircular "speedometer" gauge for a 0f..1f confidence ratio, matching
 * the Toe/Surface Coverage Confidence dials on SR Measure's measurement
 * detail screen. Band boundaries match [ConfidenceLevel.fromRatio] exactly,
 * so the needle position always agrees with the Baja/Media/Alta label below it.
 */
@Composable
fun ConfidenceGaugeDial(
    label: String,
    ratio: Float,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceLevel.fromRatio(ratio)
    val levelColor = when (level) {
        ConfidenceLevel.HIGH -> AccentEmerald
        ConfidenceLevel.MEDIUM -> PrimaryOrange
        ConfidenceLevel.LOW -> AccentRose
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(120.dp)
                .height(66.dp)
        ) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height * 2 - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Three bands whose boundaries mirror ConfidenceLevel's own thresholds (0.55, 0.85).
            drawArc(
                color = AccentRose,
                startAngle = 180f,
                sweepAngle = 180f * 0.55f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            drawArc(
                color = PrimaryOrange,
                startAngle = 180f + 180f * 0.55f,
                sweepAngle = 180f * (0.85f - 0.55f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            drawArc(
                color = AccentEmerald,
                startAngle = 180f + 180f * 0.85f,
                sweepAngle = 180f * (1f - 0.85f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            // Needle, pivoted at the bottom-center of the arc's bounding box.
            val center = Offset(size.width / 2f, size.height)
            val radius = (size.width - strokeWidth) / 2f
            val angleRad = Math.toRadians((180f + ratio.coerceIn(0f, 1f) * 180f).toDouble())
            val needleEnd = Offset(
                x = center.x + (radius * 0.75f) * cos(angleRad).toFloat(),
                y = center.y + (radius * 0.75f) * sin(angleRad).toFloat()
            )
            drawLine(color = Color.White, start = center, end = needleEnd, strokeWidth = 4f, cap = StrokeCap.Round)
            drawCircle(color = Color.White, radius = 5f, center = center)
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(shape = RoundedCornerShape(8.dp), color = levelColor.copy(alpha = 0.18f)) {
            Text(
                text = level.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = levelColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}
