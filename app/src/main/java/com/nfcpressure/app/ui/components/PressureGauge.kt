package com.nfcpressure.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcpressure.app.ui.theme.GaugeBackground
import com.nfcpressure.app.ui.theme.GaugeGreen
import com.nfcpressure.app.ui.theme.GaugeRed
import com.nfcpressure.app.ui.theme.GaugeYellow
import kotlin.math.cos
import kotlin.math.sin

/**
 * 圆形压力仪表盘组件（带刻度和指针）
 * 显示范围 0-100 mmHg
 * 绿色区域: 0-30 mmHg
 * 黄色区域: 30-60 mmHg
 * 红色区域: 60-100 mmHg
 */
@Composable
fun PressureGauge(
    pressure: Float,
    modifier: Modifier = Modifier,
    maxPressure: Float = 100f,
    showValue: Boolean = true
) {
    val animatedPressure by animateFloatAsState(
        targetValue = pressure.coerceIn(0f, maxPressure),
        animationSpec = tween(durationMillis = 500),
        label = "pressureAnimation"
    )
    val textMeasurer = rememberTextMeasurer()

    val gaugeColor = when {
        pressure < 0 -> GaugeBackground
        pressure <= 30 -> GaugeGreen
        pressure <= 60 -> GaugeYellow
        else -> GaugeRed
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val padding = 16.dp.toPx()

                // 半径定义（从外到内）
                val labelRadius = size.minDimension / 2 - padding
                val tickOuterRadius = labelRadius - 12.dp.toPx()
                val tickMajorLength = 12.dp.toPx()
                val tickMinorLength = 6.dp.toPx()
                val arcRadius = tickOuterRadius - tickMajorLength - 2.dp.toPx()
                val arcStrokeWidth = 12.dp.toPx()
                val needleLength = arcRadius - arcStrokeWidth / 2 - 8.dp.toPx()

                // ===== 1. 绘制刻度数字 =====
                val labelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)

                for (v in listOf(0, 30, 60, 100)) {
                    val canvasAngle = 135.0 + (v.toDouble() / maxPressure.toDouble()) * 270.0
                    val angleRad = Math.toRadians(canvasAngle)

                    val lx = center.x + labelRadius * cos(angleRad).toFloat()
                    val ly = center.y + labelRadius * sin(angleRad).toFloat()

                    val textColor = when {
                        v <= 30 -> GaugeGreen
                        v <= 60 -> GaugeYellow
                        else -> GaugeRed
                    }

                    val textLayout = textMeasurer.measure(
                        text = v.toString(),
                        style = labelStyle.copy(color = textColor)
                    )

                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            lx - textLayout.size.width / 2f,
                            ly - textLayout.size.height / 2f
                        )
                    )
                }

                // ===== 2. 绘制刻度线 =====
                for (v in 0..100 step 10) {
                    val isMajor = v == 0 || v == 30 || v == 60 || v == 100
                    val tickLen = if (isMajor) tickMajorLength else tickMinorLength
                    val tickW = if (isMajor) 2.5f.dp.toPx() else 1.2f.dp.toPx()

                    val canvasAngle = 135.0 + (v.toDouble() / maxPressure.toDouble()) * 270.0
                    val angleRad = Math.toRadians(canvasAngle)

                    val outerR = tickOuterRadius
                    val innerR = tickOuterRadius - tickLen

                    val tickColor = when {
                        v < 30 -> if (isMajor) GaugeGreen else Color(0xFF999999)
                        v == 30 -> GaugeYellow
                        v < 60 -> if (isMajor) GaugeYellow else Color(0xFF999999)
                        v == 60 -> GaugeRed
                        else -> if (isMajor) GaugeRed else Color(0xFF999999)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(
                            center.x + innerR * cos(angleRad).toFloat(),
                            center.y + innerR * sin(angleRad).toFloat()
                        ),
                        end = Offset(
                            center.x + outerR * cos(angleRad).toFloat(),
                            center.y + outerR * sin(angleRad).toFloat()
                        ),
                        strokeWidth = tickW,
                        cap = StrokeCap.Round
                    )
                }

                // ===== 3. 绘制弧线 =====
                // 背景弧
                drawArc(
                    color = GaugeBackground,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                )

                // 绿色区域 (0-30)
                val greenSweep = (30f / maxPressure) * 270f
                drawArc(
                    color = GaugeGreen,
                    startAngle = 135f,
                    sweepAngle = greenSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                )

                // 黄色区域 (30-60)
                val yellowSweep = ((60f - 30f) / maxPressure) * 270f
                drawArc(
                    color = GaugeYellow,
                    startAngle = 135f + greenSweep,
                    sweepAngle = yellowSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                )

                // 红色区域 (60-100)
                val redSweep = ((maxPressure - 60f) / maxPressure) * 270f
                drawArc(
                    color = GaugeRed,
                    startAngle = 135f + greenSweep + yellowSweep,
                    sweepAngle = redSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
                )

                // ===== 4. 绘制指针 =====
                val needleAngle = 135.0 + (animatedPressure.toDouble() / maxPressure.toDouble()) * 270.0
                val needleRad = Math.toRadians(needleAngle)

                // 指针尖端
                val tipX = center.x + needleLength * cos(needleRad).toFloat()
                val tipY = center.y + needleLength * sin(needleRad).toFloat()

                // 指针底部宽度（垂直于指针方向）
                val halfWidth = 5.dp.toPx()
                val perpRad = needleRad + Math.PI / 2

                val base1X = center.x + halfWidth * cos(perpRad).toFloat()
                val base1Y = center.y + halfWidth * sin(perpRad).toFloat()
                val base2X = center.x - halfWidth * cos(perpRad).toFloat()
                val base2Y = center.y - halfWidth * sin(perpRad).toFloat()

                // 指针尾部（反方向短尾）
                val tailLength = 14.dp.toPx()
                val tailX = center.x - tailLength * cos(needleRad).toFloat()
                val tailY = center.y - tailLength * sin(needleRad).toFloat()

                // 绘制指针（菱形：尖端→左侧→尾部→右侧）
                val needlePath = Path().apply {
                    moveTo(tipX, tipY)
                    lineTo(base1X, base1Y)
                    lineTo(tailX, tailY)
                    lineTo(base2X, base2Y)
                    close()
                }
                drawPath(
                    path = needlePath,
                    color = Color(0xFFE53935),
                    style = Fill
                )

                // 中心盖（外圈深色）
                drawCircle(
                    color = Color(0xFF424242),
                    radius = 10.dp.toPx(),
                    center = center
                )
                // 中心盖（内圈浅色）
                drawCircle(
                    color = Color(0xFF9E9E9E),
                    radius = 5.dp.toPx(),
                    center = center
                )
            }

            // 显示数值
            if (showValue) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 30.dp)
                ) {
                    Text(
                        text = if (pressure < 0) "--" else String.format("%.1f", pressure),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = gaugeColor
                    )
                    Text(
                        text = "mmHg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 简化版小仪表盘
 */
@Composable
fun MiniPressureGauge(
    pressure: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        pressure < 0 -> Color.Gray
        pressure <= 30 -> GaugeGreen
        pressure <= 60 -> GaugeYellow
        else -> GaugeRed
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            drawCircle(
                color = GaugeBackground,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            val sweepAngle = (pressure.coerceIn(0f, 100f) / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = if (pressure < 0) "--" else "${pressure.toInt()}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
