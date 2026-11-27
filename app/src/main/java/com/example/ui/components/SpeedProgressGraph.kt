package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionState
import com.example.model.SpeedSample
import com.example.model.VpnStats
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonEmerald

private const val GRAPH_WINDOW_CAPACITY = 30
private const val MIN_Y_SCALE_BPS = 16_384f // 128 Kbps baseline so small packet bursts remain clearly visible

@Composable
fun SpeedProgressGraphCard(
    connectionState: ConnectionState,
    stats: VpnStats,
    speedHistory: List<SpeedSample>,
    modifier: Modifier = Modifier
) {
    val isLive = connectionState == ConnectionState.CONNECTED

    val infiniteTransition = rememberInfiniteTransition(label = "live_dot")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLive) 0.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    val peakDownloadBps by remember(speedHistory) {
        derivedStateOf { speedHistory.maxOfOrNull { it.downloadBytesPerSec } ?: 0L }
    }
    val peakUploadBps by remember(speedHistory) {
        derivedStateOf { speedHistory.maxOfOrNull { it.uploadBytesPerSec } ?: 0L }
    }

    val targetMaxScaleBps = remember(peakDownloadBps, peakUploadBps) {
        val peak = maxOf(peakDownloadBps, peakUploadBps).toFloat()
        maxOf(peak * 1.25f, MIN_Y_SCALE_BPS)
    }

    val animatedMaxScaleBps by animateFloatAsState(
        targetValue = targetMaxScaleBps,
        animationSpec = tween(durationMillis = 350),
        label = "animatedMaxScale"
    )

    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("speed_progress_graph_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLive) CyanPrimary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: title + live status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .alpha(if (isLive) liveDotAlpha else 0.5f)
                            .clip(CircleShape)
                            .background(if (isLive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.live_graph_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    color = if (isLive) NeonEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isLive) BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.45f)) else null
                ) {
                    Text(
                        text = if (isLive) {
                            stringResource(R.string.live_graph_active)
                        } else {
                            stringResource(R.string.live_graph_standby)
                        },
                        color = if (isLive) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend row with real-time Download & Upload rates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LegendSpeedPill(
                    label = "↓ ${stringResource(R.string.speed_download)}",
                    speedText = if (isLive) stats.formattedDownloadSpeed() else "0 Kbps",
                    accentColor = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
                LegendSpeedPill(
                    label = "↑ ${stringResource(R.string.speed_upload)}",
                    speedText = if (isLive) stats.formattedUploadSpeed() else "0 Kbps",
                    accentColor = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time dual-curve progress graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = VpnStats.formatSpeed(animatedMaxScaleBps.toLong()),
                        fontSize = 9.sp,
                        color = labelColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = VpnStats.formatSpeed((animatedMaxScaleBps / 2f).toLong()),
                        fontSize = 9.sp,
                        color = labelColor.copy(alpha = 0.65f)
                    )
                    Text(
                        text = "0",
                        fontSize = 9.sp,
                        color = labelColor.copy(alpha = 0.65f)
                    )
                }

                // Graph Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 46.dp, top = 6.dp, bottom = 6.dp, end = 6.dp)
                        .testTag("speed_progress_canvas")
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    // Horizontal dashed grid lines (Top, Middle, Bottom)
                    listOf(0f, 0.5f, 1f).forEach { ratio ->
                        val y = chartHeight * ratio
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (ratio < 1f) dashEffect else null
                        )
                    }

                    if (speedHistory.isNotEmpty()) {
                        val stepX = chartWidth / (GRAPH_WINDOW_CAPACITY - 1).toFloat()

                        val downloadPoints = speedHistory.mapIndexed { index, sample ->
                            val x = index * stepX
                            val norm = (sample.downloadBytesPerSec.toFloat() / animatedMaxScaleBps)
                                .coerceIn(0f, 1f)
                            val y = chartHeight - (norm * chartHeight)
                            Offset(x, y)
                        }

                        val uploadPoints = speedHistory.mapIndexed { index, sample ->
                            val x = index * stepX
                            val norm = (sample.uploadBytesPerSec.toFloat() / animatedMaxScaleBps)
                                .coerceIn(0f, 1f)
                            val y = chartHeight - (norm * chartHeight)
                            Offset(x, y)
                        }

                        // Vertical live progress cursor at latest sample X
                        val cursorX = downloadPoints.last().x
                        drawLine(
                            color = Color.White.copy(alpha = 0.16f),
                            start = Offset(cursorX, 0f),
                            end = Offset(cursorX, chartHeight),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw Download curve + area fill
                        drawSpeedCurve(
                            points = downloadPoints,
                            chartHeight = chartHeight,
                            lineColor = NeonEmerald
                        )

                        // Draw Upload curve + area fill
                        drawSpeedCurve(
                            points = uploadPoints,
                            chartHeight = chartHeight,
                            lineColor = CyanPrimary
                        )

                        // Glowing endpoint markers when active
                        if (isLive) {
                            drawEndpointDot(downloadPoints.last(), NeonEmerald)
                            drawEndpointDot(uploadPoints.last(), CyanPrimary)
                        }
                    }
                }

                // Standby hint overlay when no samples yet
                if (!isLive && speedHistory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.live_graph_empty_hint),
                        fontSize = 11.sp,
                        color = labelColor.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer metrics: Peak & Total transferred
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.live_graph_peak)}: ↓ ${VpnStats.formatSpeed(peakDownloadBps)}  ↑ ${VpnStats.formatSpeed(peakUploadBps)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${speedHistory.size}/${GRAPH_WINDOW_CAPACITY}s",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLive) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendSpeedPill(
    label: String,
    speedText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = speedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun DrawScope.drawSpeedCurve(
    points: List<Offset>,
    chartHeight: Float,
    lineColor: Color
) {
    if (points.isEmpty()) return

    if (points.size == 1) {
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = points.first()
        )
        return
    }

    val strokePath = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val controlX = (current.x + next.x) / 2f
            cubicTo(
                controlX, current.y,
                controlX, next.y,
                next.x, next.y
            )
        }
    }

    val fillPath = Path().apply {
        addPath(strokePath)
        lineTo(points.last().x, chartHeight)
        lineTo(points.first().x, chartHeight)
        close()
    }

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                lineColor.copy(alpha = 0.28f),
                lineColor.copy(alpha = 0.0f)
            ),
            startY = 0f,
            endY = chartHeight
        )
    )

    drawPath(
        path = strokePath,
        color = lineColor,
        style = Stroke(
            width = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawEndpointDot(
    center: Offset,
    color: Color
) {
    drawCircle(
        color = color.copy(alpha = 0.28f),
        radius = 7.dp.toPx(),
        center = center
    )
    drawCircle(
        color = color,
        radius = 3.5.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 1.5.dp.toPx(),
        center = center
    )
}
