package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.SpeedTestResult
import com.example.model.VpnServer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonEmerald
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedTestScreen(
    speedTestResult: SpeedTestResult,
    selectedServer: VpnServer,
    connectionState: ConnectionState,
    onStartTest: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Determine current speed to show on gauge dial
    val displaySpeed = when {
        speedTestResult.isRunning && speedTestResult.progress < 0.65f -> speedTestResult.downloadMbps
        speedTestResult.isRunning -> speedTestResult.uploadMbps
        speedTestResult.downloadMbps > 0 -> speedTestResult.downloadMbps
        else -> 0.0
    }

    val maxGaugeSpeed = 200.0
    val targetFraction = (displaySpeed / maxGaugeSpeed).coerceIn(0.0, 1.0).toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 200),
        label = "gaugeSweep"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active server indicator
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selectedServer.flagEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Testing Server",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedServer.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Surface(
                    color = if (connectionState == ConnectionState.CONNECTED) NeonEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTED) "TUNNEL ACTIVE" else "DIRECT NET",
                        color = if (connectionState == ConnectionState.CONNECTED) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Cyber Speedometer Gauge Dial
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val primaryCyan = CyanPrimary
            val emerald = NeonEmerald
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val arcSize = Size(diameter, diameter)

                // Background track arc (from 140° to 400° -> 260° sweep)
                drawArc(
                    color = trackColor,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active speed gradient arc
                if (animatedFraction > 0.01f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryCyan, emerald, primaryCyan)
                        ),
                        startAngle = 140f,
                        sweepAngle = 260f * animatedFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Needle tip dot
                val currentAngle = 140f + (260f * animatedFraction)
                val angleRad = Math.toRadians(currentAngle.toDouble())
                val radius = diameter / 2
                val centerX = size.width / 2
                val centerY = size.height / 2
                val dotX = centerX + (radius * cos(angleRad)).toFloat()
                val dotY = centerY + (radius * sin(angleRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = primaryCyan,
                    radius = 4.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // Digital readout in center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", displaySpeed),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Mbps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = speedTestResult.status,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (speedTestResult.isRunning) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { speedTestResult.progress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyanPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Metric Tiles Grid (Ping, Jitter, Download, Upload)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ping
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_ping_tile"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Ping", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (speedTestResult.pingMs > 0) "${speedTestResult.pingMs} ms" else "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Jitter
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_jitter_tile"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Jitter", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (speedTestResult.jitterMs > 0) "${speedTestResult.jitterMs} ms" else "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Download
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_download_tile"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Download", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (speedTestResult.downloadMbps > 0) String.format(Locale.US, "%.1f Mbps", speedTestResult.downloadMbps) else "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Upload
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_upload_tile"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Upload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (speedTestResult.uploadMbps > 0) String.format(Locale.US, "%.1f Mbps", speedTestResult.uploadMbps) else "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Performance Capability Badges
        if (speedTestResult.downloadMbps > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Network Capability Rating",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CapabilityTag("4K UHD Streaming", true)
                        CapabilityTag("Online Gaming", speedTestResult.pingMs < 60)
                        CapabilityTag("Fast Torrenting", true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Action Button: Start Speed Test
        Button(
            onClick = onStartTest,
            enabled = !speedTestResult.isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_speed_test_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = Color(0xFF070B19)
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (speedTestResult.isRunning) "TESTING IN PROGRESS…" else "START SPEED TEST",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CapabilityTag(label: String, active: Boolean) {
    Surface(
        color = if (active) NeonEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = (if (active) "✓ " else "• ") + label,
            fontSize = 11.sp,
            color = if (active) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
