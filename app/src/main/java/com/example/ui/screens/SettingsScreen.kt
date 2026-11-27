package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DnsOption
import com.example.model.VpnProtocol
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonEmerald

@Composable
fun SettingsScreen(
    currentProtocol: VpnProtocol,
    currentDns: DnsOption,
    killSwitch: Boolean,
    autoConnect: Boolean,
    onProtocolChange: (VpnProtocol) -> Unit,
    onDnsChange: (DnsOption) -> Unit,
    onKillSwitchToggle: (Boolean) -> Unit,
    onAutoConnectToggle: (Boolean) -> Unit,
    onNavigateToSplitTunnel: () -> Unit
) {
    var showProtocolDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }
    var showFreeGuaranteeDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Section: Connection & Protocol
        Text(
            text = "VPN PROTOCOL & NETWORK",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Protocol selector
                SettingsRow(
                    icon = Icons.Default.Route,
                    title = "VPN Protocol",
                    subtitle = "${currentProtocol.displayName} - ${currentProtocol.description}",
                    onClick = { showProtocolDialog = true },
                    modifier = Modifier.testTag("setting_protocol_row")
                )

                // DNS Selector
                SettingsRow(
                    icon = Icons.Default.Dns,
                    title = "Custom DNS Resolver",
                    subtitle = "${currentDns.displayName} (${currentDns.primaryDns})",
                    onClick = { showDnsDialog = true },
                    modifier = Modifier.testTag("setting_dns_row")
                )

                // Split Tunneling
                SettingsRow(
                    icon = Icons.Default.Tune,
                    title = "Split Tunneling",
                    subtitle = "Select apps to bypass the VPN tunnel",
                    onClick = onNavigateToSplitTunnel,
                    modifier = Modifier.testTag("setting_split_tunnel_row")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Security & Automation
        Text(
            text = "SECURITY & AUTOMATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Kill Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Kill Switch",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Blocks all internet traffic if the VPN connection drops unexpectedly",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Switch(
                        checked = killSwitch,
                        onCheckedChange = onKillSwitchToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("kill_switch_toggle")
                    )
                }

                // Auto Connect
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkWifi,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Auto-Connect",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Automatically secure connection on untrusted Wi-Fi & public networks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Switch(
                        checked = autoConnect,
                        onCheckedChange = onAutoConnectToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("auto_connect_toggle")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: About & Free Manifesto
        Text(
            text = "ABOUT & PROMISE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.VolunteerActivism,
                    title = "100% Free Forever Promise",
                    subtitle = "No subscriptions, no fees, no credit card required",
                    onClick = { showFreeGuaranteeDialog = true },
                    modifier = Modifier.testTag("setting_free_promise_row")
                )

                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "Zero Logs Guarantee",
                    subtitle = "We never store your activity, browsing history or IP",
                    onClick = { showFreeGuaranteeDialog = true }
                )

                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Ultimate World VPN v1.0",
                    subtitle = "Build 2026.1 • Powered by Android VpnService",
                    onClick = { }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Protocol Dialog
    if (showProtocolDialog) {
        AlertDialog(
            onDismissRequest = { showProtocolDialog = false },
            title = { Text("Choose VPN Protocol", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    VpnProtocol.values().forEach { protocol ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onProtocolChange(protocol)
                                    showProtocolDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = currentProtocol == protocol,
                                onClick = {
                                    onProtocolChange(protocol)
                                    showProtocolDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = protocol.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = protocol.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProtocolDialog = false }) {
                    Text("Close", color = CyanPrimary)
                }
            }
        )
    }

    // DNS Dialog
    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("Choose DNS Resolver", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    DnsOption.values().forEach { dns ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDnsChange(dns)
                                    showDnsDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = currentDns == dns,
                                onClick = {
                                    onDnsChange(dns)
                                    showDnsDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = dns.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = dns.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDnsDialog = false }) {
                    Text("Close", color = CyanPrimary)
                }
            }
        )
    }

    // Free Promise Dialog
    if (showFreeGuaranteeDialog) {
        AlertDialog(
            onDismissRequest = { showFreeGuaranteeDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "100% Free Forever",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ultimate World VPN is built on a mission to safeguard digital privacy and open internet access worldwide.\n\n" +
                                "• No In-App Purchases\n" +
                                "• No Subscriptions\n" +
                                "• No Bandwidth Throttling\n" +
                                "• No Log Storage\n" +
                                "• Unlimited High-Speed Relay\n\n" +
                                "You will never be asked for payment, card details, or hidden charges. Enjoy unrestricted global internet access!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFreeGuaranteeDialog = false }) {
                    Text("Understood", color = NeonEmerald, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
