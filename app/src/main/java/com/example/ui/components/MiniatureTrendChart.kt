package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.theme.MutedText
import com.example.ui.theme.PolarCyan

@OptIn(ExperimentalTextApi::class)
@Composable
fun MiniatureTrendChart(
    temperatures: List<Double>,
    times: List<String>,
    modifier: Modifier = Modifier
) {
    if (temperatures.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val strokeColor = PolarCyan
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            PolarCyan.copy(alpha = 0.35f),
            PolarCyan.copy(alpha = 0.0f)
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        
        val paddingLeft = 30f
        val paddingRight = 30f
        val paddingTop = 40f
        val paddingBottom = 40f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val minTemp = temperatures.minOrNull() ?: 0.0
        val maxTemp = temperatures.maxOrNull() ?: 20.0
        val tempRange = if (maxTemp == minTemp) 1.0 else maxTemp - minTemp

        val points = mutableListOf<Offset>()
        val stepX = chartWidth / (temperatures.size - 1).coerceAtLeast(1)

        for (i in temperatures.indices) {
            val x = paddingLeft + i * stepX
            val ratio = (temperatures[i] - minTemp) / tempRange
            // Invert Y coordinate so higher temps are represented at the top of the canvas
            val y = paddingTop + chartHeight - (ratio * chartHeight).toFloat()
            points.add(Offset(x, y))
        }

        // Draw background area gradient
        if (points.size > 1) {
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                for (point in points) {
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }
            drawPath(path = fillPath, brush = gradientBrush)
        }

        // Draw trend curve
        if (points.size > 1) {
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    // Smooth quadratic/cubic Bezier curves between timestamps
                    val controlX = (p0.x + p1.x) / 2
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }
            drawPath(
                path = strokePath,
                color = strokeColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Draw circles & temperature labels above points
        for (i in points.indices) {
            val point = points[i]
            
            // Only draw a subset of circles/labels if many points exist to keep dashboard design minimalist
            if (i % 2 == 0) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = PolarCyan,
                    radius = 2.dp.toPx(),
                    center = point
                )

                // Render Temperature text
                val tempText = "${temperatures[i].toInt()}°"
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(tempText),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        point.x - textLayoutResult.size.width / 2,
                        point.y - textLayoutResult.size.height - 4f
                    )
                )

                // Render Time text below the bottom axis line
                if (i < times.size) {
                    val timeStr = times[i]
                    val timeTextLayout = textMeasurer.measure(
                        text = AnnotatedString(timeStr),
                        style = TextStyle(
                            color = MutedText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    drawText(
                        textLayoutResult = timeTextLayout,
                        topLeft = Offset(
                            point.x - timeTextLayout.size.width / 2,
                            paddingTop + chartHeight + 8f
                        )
                    )
                }
            }
        }
    }
}
