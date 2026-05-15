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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nfcpressure.app.ui.theme.GaugeBackground
import com.nfcpressure.app.ui.theme.GaugeGreen
import com.nfcpressure.app.ui.theme.GaugeRed
import com.nfcpressure.app.ui.theme.GaugeYellow
import kotlin.math.cos
import kotlin.math.sin

/**
 * 圆形压力仪表盘组件
 * 显示范围 0-120 mmHg
 * 绿色区域: 0-30 mmHg
 * 黄色区域: 30-50 mmHg
 * 红色区域: 50+ mmHg
 */
@Composable
fun PressureGauge(
    pressure: Float,
    modifier: Modifier = Modifier,
    maxPressure: Float = 120f,
    showValue: Boolean = true
) {
    // 动画化压力值
    val animatedPressure by animateFloatAsState(
        targetValue = pressure.coerceIn(0f, maxPressure),
        animationSpec = tween(durationMillis = 500),
        label = "pressureAnimation"
    )
    
    // 计算指针角度 (从-225°到45°，共270°范围)
    val angle = (animatedPressure / maxPressure) * 270f - 225f
    
    // 确定颜色
    val gaugeColor = when {
        pressure < 0 -> GaugeBackground
        pressure <= 30 -> GaugeGreen
        pressure <= 50 -> GaugeYellow
        else -> GaugeRed
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                // 绘制背景弧
                drawArc(
                    color = GaugeBackground,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // 绘制绿色区域 (0-30)
                val greenSweep = (30f / maxPressure) * 270f
                drawArc(
                    color = GaugeGreen,
                    startAngle = 135f,
                    sweepAngle = greenSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // 绘制黄色区域 (30-50)
                val yellowSweep = ((50f - 30f) / maxPressure) * 270f
                drawArc(
                    color = GaugeYellow,
                    startAngle = 135f + greenSweep,
                    sweepAngle = yellowSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // 绘制红色区域 (50-120)
                val redSweep = ((maxPressure - 50f) / maxPressure) * 270f
                drawArc(
                    color = GaugeRed,
                    startAngle = 135f + greenSweep + yellowSweep,
                    sweepAngle = redSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // 绘制指针
                if (pressure >= 0) {
                    val pointerLength = radius - 30.dp.toPx()
                    val pointerAngleRad = Math.toRadians(angle.toDouble())
                    val pointerEnd = Offset(
                        (center.x + pointerLength * cos(pointerAngleRad)).toFloat(),
                        (center.y + pointerLength * sin(pointerAngleRad)).toFloat()
                    )
                    
                    // 指针线
                    drawLine(
                        color = gaugeColor,
                        start = center,
                        end = pointerEnd,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // 指针圆点
                    drawCircle(
                        color = gaugeColor,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                }
            }
            
            // 显示数值
            if (showValue) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 20.dp)
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
        
        // 刻度标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", fontSize = 12.sp, color = Color.Gray)
            Text("30", fontSize = 12.sp, color = GaugeGreen)
            Text("50", fontSize = 12.sp, color = GaugeYellow)
            Text("80", fontSize = 12.sp, color = GaugeRed)
            Text("120", fontSize = 12.sp, color = GaugeRed)
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
        pressure <= 50 -> GaugeYellow
        else -> GaugeRed
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // 背景
            drawCircle(
                color = GaugeBackground,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // 进度
            val sweepAngle = (pressure.coerceIn(0f, 120f) / 120f) * 360f
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
