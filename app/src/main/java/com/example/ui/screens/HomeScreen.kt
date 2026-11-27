package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionState
import com.example.model.IpInfo
import com.example.model.VpnProtocol
import com.example.model.VpnServer
import com.example.model.VpnStats
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonEmerald

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    stats: VpnStats,
    server: VpnServer,
    servers: List<VpnServer>,
    protocol: VpnProtocol,
    ipInfo: IpInfo,
    primaryDns: String,
    secondaryDns: String,
    onPrimaryDnsChange: (String) -> Unit,
    onSecondaryDnsChange: (String) -> Unit,
    onSelectServer: (VpnServer) -> Unit,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onToggleConnection: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isServerDropdownExpanded by remember { mutableStateOf(false) }

    // Pulse animation for connection button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Hero graphic banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.vpn_hero),
                contentDescription = "Global Network Mesh",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            // 100% Free Badge in Top Corner
            Surface(
                color = NeonEmerald.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "100% FREE • NO MONEY",
                        color = NeonEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Protocol tag
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = protocol.displayName,
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Ultimate World VPN",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (connectionState == ConnectionState.CONNECTED)
                        "Protected & Encrypted • Virtual IP Active"
                    else
                        "Select any world server & connect to change IP",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Center VPN Connect Button with Radar Rings
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(190.dp)
                .testTag("vpn_power_button_container")
        ) {
            if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (connectionState == ConnectionState.CONNECTED) EmeraldGlow else CyanGlow)
                )
            }

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        when (connectionState) {
                            ConnectionState.CONNECTED -> NeonEmerald.copy(alpha = 0.15f)
                            ConnectionState.CONNECTING -> AmberWarning.copy(alpha = 0.15f)
                            ConnectionState.DISCONNECTING -> CoralDanger.copy(alpha = 0.15f)
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = when (connectionState) {
                            ConnectionState.CONNECTED -> NeonEmerald
                            ConnectionState.CONNECTING -> AmberWarning
                            ConnectionState.DISCONNECTING -> CoralDanger
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )

            Surface(
                onClick = onToggleConnection,
                shape = CircleShape,
                color = when (connectionState) {
                    ConnectionState.CONNECTED -> NeonEmerald
                    ConnectionState.CONNECTING -> AmberWarning
                    ConnectionState.DISCONNECTING -> CoralDanger
                    ConnectionState.DISCONNECTED -> CyanPrimary
                },
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(115.dp)
                    .testTag("vpn_toggle_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            ConnectionState.CONNECTED -> Icons.Default.Lock
                            ConnectionState.CONNECTING -> Icons.Default.PowerSettingsNew
                            ConnectionState.DISCONNECTING -> Icons.Default.PowerSettingsNew
                            ConnectionState.DISCONNECTED -> Icons.Default.LockOpen
                        },
                        contentDescription = "VPN Power",
                        tint = Color(0xFF070B19),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "CONNECTED"
                            ConnectionState.CONNECTING -> "CONNECTING"
                            ConnectionState.DISCONNECTING -> "CLOSING"
                            ConnectionState.DISCONNECTED -> "CONNECT"
                        },
                        color = Color(0xFF070B19),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when (connectionState) {
                ConnectionState.CONNECTED -> stringResource(R.string.status_connected)
                ConnectionState.CONNECTING -> stringResource(R.string.status_connecting)
                ConnectionState.DISCONNECTING -> stringResource(R.string.status_disconnecting)
                ConnectionState.DISCONNECTED -> stringResource(R.string.status_disconnected)
            },
            color = when (connectionState) {
                ConnectionState.CONNECTED -> NeonEmerald
                ConnectionState.CONNECTING -> AmberWarning
                ConnectionState.DISCONNECTING -> CoralDanger
                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (connectionState == ConnectionState.CONNECTED) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stats.formattedDuration(),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==========================================
        // 🌍 Ultimate World VPN Hub (All Flags & Custom DNS)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("world_vpn_hub_card"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0C1322)
            ),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (connectionState == ConnectionState.CONNECTED) NeonEmerald.copy(alpha = 0.5f) else Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🌍 Ultimate World VPN Hub (All Flags)",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Server Selector Label
                Text(
                    text = "Select Any Country Server & Flag:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Spinner / Dropdown for World Servers (All Flags)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spinnerWorldServers")
                ) {
                    Surface(
                        onClick = { isServerDropdownExpanded = !isServerDropdownExpanded },
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = server.flagEmoji,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${server.country} (${server.city})",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${server.pingMs} ms • ${server.loadPercent}% load • 100% Free",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Open Country Selector",
                                tint = CyanPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isServerDropdownExpanded,
                        onDismissRequest = { isServerDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 350.dp)
                            .background(Color(0xFF0F172A))
                    ) {
                        servers.forEach { s ->
                            val isChosen = s.id == server.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = s.flagEmoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${s.country} - ${s.city}",
                                                color = if (isChosen) CyanPrimary else Color.White,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${s.pingMs} ms • ${s.features.firstOrNull() ?: "Fast"}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (isChosen) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = CyanPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectServer(s)
                                    isServerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Custom DNS Header
                Text(
                    text = "Custom DNS (Enter Number IP or Domain Name):",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Primary DNS Input
                OutlinedTextField(
                    value = primaryDns,
                    onValueChange = onPrimaryDnsChange,
                    placeholder = {
                        Text(
                            text = "Primary DNS (e.g., 8.8.8.8 or dns.google)",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("etPrimaryDns")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary DNS Input
                OutlinedTextField(
                    value = secondaryDns,
                    onValueChange = onSecondaryDnsChange,
                    placeholder = {
                        Text(
                            text = "Secondary DNS (e.g., 1.1.1.1 or cloudflare)",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("etSecondaryDns")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Button 1: CONNECT & CHANGE IP (#10B981)
                Button(
                    onClick = onConnectVpn,
                    enabled = connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.CONNECTING,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .testTag("btnConnectVpn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONNECT & CHANGE IP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button 2: DISCONNECT VPN (#EF4444)
                Button(
                    onClick = onDisconnectVpn,
                    enabled = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .testTag("btnDisconnectVpn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISCONNECT VPN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Real-Time Speed Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("download_stat_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.speed_download),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTED) stats.formattedDownloadSpeed() else "0.0 Mbps",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTED) "Total: ${stats.formattedDataIn()}" else "Idle",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("upload_stat_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.speed_upload),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTED) stats.formattedUploadSpeed() else "0.0 Mbps",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTED) "Total: ${stats.formattedDataOut()}" else "Idle",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // IP Protection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSecurity() }
                .testTag("ip_protection_summary_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (ipInfo.isProtected) NeonEmerald.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (ipInfo.isProtected) NeonEmerald.copy(alpha = 0.15f) else CoralDanger.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ipInfo.isProtected) Icons.Default.Security else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (ipInfo.isProtected) NeonEmerald else CoralDanger,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (ipInfo.isProtected) "IP Address Protected" else "Real IP Exposed",
                            color = if (ipInfo.isProtected) NeonEmerald else CoralDanger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ipInfo.ip} • ${ipInfo.city}, ${ipInfo.country}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Security Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Free Forever Manifesto Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("free_forever_banner_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.free_banner_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.free_banner_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
