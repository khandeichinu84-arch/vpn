package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VpnRepository
import com.example.model.ConnectionState
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeedTestScreen
import com.example.ui.screens.SplitTunnelScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonEmerald
import kotlinx.coroutines.launch

enum class Screen(val title: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    HOME("Home", Icons.Filled.Security, Icons.Outlined.Security),
    SERVERS("Servers", Icons.Filled.Public, Icons.Outlined.Public),
    SPEED_TEST("Speed Test", Icons.Filled.Speed, Icons.Outlined.Speed),
    SECURITY("Security", Icons.Filled.VpnLock, Icons.Outlined.VpnLock),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    SPLIT_TUNNEL("Split Tunneling", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var previousScreen by remember { mutableStateOf(Screen.HOME) }

    val connectionState by VpnRepository.connectionState.collectAsStateWithLifecycle()
    val stats by VpnRepository.stats.collectAsStateWithLifecycle()
    val servers by VpnRepository.servers.collectAsStateWithLifecycle()
    val selectedServer by VpnRepository.selectedServer.collectAsStateWithLifecycle()
    val selectedProtocol by VpnRepository.selectedProtocol.collectAsStateWithLifecycle()
    val selectedDns by VpnRepository.selectedDns.collectAsStateWithLifecycle()
    val primaryDns by VpnRepository.primaryDnsInput.collectAsStateWithLifecycle()
    val secondaryDns by VpnRepository.secondaryDnsInput.collectAsStateWithLifecycle()
    val killSwitch by VpnRepository.killSwitchEnabled.collectAsStateWithLifecycle()
    val autoConnect by VpnRepository.autoConnectEnabled.collectAsStateWithLifecycle()
    val splitTunnelApps by VpnRepository.splitTunnelApps.collectAsStateWithLifecycle()
    val ipInfo by VpnRepository.currentIpInfo.collectAsStateWithLifecycle()
    val speedTestResult by VpnRepository.speedTestResult.collectAsStateWithLifecycle()

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // VPN Prepare permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            UltimateVpnService.startVpn(
                context = context,
                serverIp = selectedServer.host,
                primaryDns = primaryDns,
                secondaryDns = secondaryDns
            )
            android.widget.Toast.makeText(
                context,
                "Connecting to ${selectedServer.flagEmoji} ${selectedServer.country} Server...",
                android.widget.Toast.LENGTH_LONG
            ).show()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Connecting to ${selectedServer.displayName}…")
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "VPN Permission was denied.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("VPN permission denied by user")
            }
        }
    }

    fun handleToggleVpn() {
        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            UltimateVpnService.stopVpn(context)
            android.widget.Toast.makeText(
                context,
                "VPN Disconnected.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Disconnected from VPN")
            }
        } else {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                UltimateVpnService.startVpn(
                    context = context,
                    serverIp = selectedServer.host,
                    primaryDns = primaryDns,
                    secondaryDns = secondaryDns
                )
                android.widget.Toast.makeText(
                    context,
                    "Connecting to ${selectedServer.flagEmoji} ${selectedServer.country} Server...",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Connecting to ${selectedServer.displayName}…")
                }
            }
        }
    }

    // Back handling for sub-screens
    if (currentScreen != Screen.HOME) {
        BackHandler {
            if (currentScreen == Screen.SPLIT_TUNNEL) {
                currentScreen = Screen.SETTINGS
            } else {
                currentScreen = Screen.HOME
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            Screen.HOME -> "Ultimate World VPN"
                            Screen.SERVERS -> "Global Server Locations"
                            Screen.SPEED_TEST -> "VPN Speed Gauge"
                            Screen.SECURITY -> "IP & Privacy Guard"
                            Screen.SETTINGS -> "Settings & Security"
                            Screen.SPLIT_TUNNEL -> "Split Tunneling"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (currentScreen == Screen.HOME && connectionState == ConnectionState.CONNECTED) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    color = NeonEmerald.copy(alpha = 0.2f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SHIELD ACTIVE",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentScreen != Screen.SPLIT_TUNNEL) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val navItems = listOf(
                        Screen.HOME,
                        Screen.SERVERS,
                        Screen.SPEED_TEST,
                        Screen.SECURITY,
                        Screen.SETTINGS
                    )

                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                previousScreen = currentScreen
                                currentScreen = screen
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.filledIcon else screen.outlinedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                indicatorColor = CyanPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        connectionState = connectionState,
                        stats = stats,
                        server = selectedServer,
                        servers = servers,
                        protocol = selectedProtocol,
                        ipInfo = ipInfo,
                        primaryDns = primaryDns,
                        secondaryDns = secondaryDns,
                        onPrimaryDnsChange = { VpnRepository.setPrimaryDnsInput(it) },
                        onSecondaryDnsChange = { VpnRepository.setSecondaryDnsInput(it) },
                        onSelectServer = { server ->
                            VpnRepository.selectServer(server)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Selected ${server.displayName}")
                            }
                        },
                        onConnectVpn = {
                            if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.CONNECTING) {
                                handleToggleVpn()
                            }
                        },
                        onDisconnectVpn = {
                            if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
                                handleToggleVpn()
                            }
                        },
                        onToggleConnection = { handleToggleVpn() },
                        onNavigateToServers = { currentScreen = Screen.SERVERS },
                        onNavigateToSecurity = { currentScreen = Screen.SECURITY }
                    )
                }

                Screen.SERVERS -> {
                    ServersScreen(
                        servers = servers,
                        selectedServer = selectedServer,
                        onSelectServer = { server ->
                            VpnRepository.selectServer(server)
                            currentScreen = Screen.HOME
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Selected ${server.displayName}")
                            }
                        },
                        onRefreshPings = {
                            VpnRepository.refreshServerLatencies()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Ping latency updated")
                            }
                        }
                    )
                }

                Screen.SPEED_TEST -> {
                    SpeedTestScreen(
                        speedTestResult = speedTestResult,
                        selectedServer = selectedServer,
                        connectionState = connectionState,
                        onStartTest = { VpnRepository.runSpeedTest() }
                    )
                }

                Screen.SECURITY -> {
                    SecurityScreen(
                        ipInfo = ipInfo,
                        connectionState = connectionState,
                        server = selectedServer,
                        protocol = selectedProtocol,
                        onRefreshIp = {
                            VpnRepository.refreshIpInfo()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("IP & leak scan refreshed")
                            }
                        }
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        currentProtocol = selectedProtocol,
                        currentDns = selectedDns,
                        killSwitch = killSwitch,
                        autoConnect = autoConnect,
                        onProtocolChange = { VpnRepository.setProtocol(it) },
                        onDnsChange = { VpnRepository.setDns(it) },
                        onKillSwitchToggle = { VpnRepository.toggleKillSwitch(it) },
                        onAutoConnectToggle = { VpnRepository.toggleAutoConnect(it) },
                        onNavigateToSplitTunnel = { currentScreen = Screen.SPLIT_TUNNEL }
                    )
                }

                Screen.SPLIT_TUNNEL -> {
                    SplitTunnelScreen(
                        apps = splitTunnelApps,
                        onToggleApp = { packageName ->
                            VpnRepository.toggleAppSplitTunnel(packageName)
                        }
                    )
                }
            }
        }
    }
}
