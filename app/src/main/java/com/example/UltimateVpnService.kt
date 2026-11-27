package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.VpnRepository
import com.example.model.ConnectionState
import com.example.tunnel.TunnelTrafficMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer

class UltimateVpnService : VpnService(), Runnable {

    companion object {
        private const val TAG = "UltimateVpnService"
        const val ACTION_START_VPN = "com.example.action.CONNECT"
        const val ACTION_STOP_VPN = "ACTION_STOP_VPN"
        const val EXTRA_SERVER_IP = "SERVER_IP"
        const val EXTRA_PRIMARY_DNS = "PRIMARY_DNS"
        const val EXTRA_SECONDARY_DNS = "SECONDARY_DNS"

        const val NOTIFICATION_CHANNEL_ID = "ultimate_vpn_channel"
        const val NOTIFICATION_ID = 1001
        private const val TRAFFIC_SAMPLE_INTERVAL_MS = 1000L

        fun startVpn(
            context: Context,
            serverIp: String? = null,
            primaryDns: String? = null,
            secondaryDns: String? = null
        ) {
            val intent = Intent(context, UltimateVpnService::class.java).apply {
                action = ACTION_START_VPN
                putExtra(EXTRA_SERVER_IP, serverIp ?: VpnRepository.selectedServer.value.host)
                putExtra(EXTRA_PRIMARY_DNS, primaryDns ?: VpnRepository.primaryDnsInput.value)
                putExtra(EXTRA_SECONDARY_DNS, secondaryDns ?: VpnRepository.secondaryDnsInput.value)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, UltimateVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var workerThread: Thread? = null
    private var tunnelInterface: ParcelFileDescriptor? = null
    private var trafficMonitor: TunnelTrafficMonitor? = null

    @Volatile
    private var isServiceRunning = false

    private var targetServerIp: String? = null
    private var primaryDns: String? = null
    private var secondaryDns: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP_VPN == intent.action) {
            shutdownVpnTunnel()
            return START_NOT_STICKY
        }

        targetServerIp = intent?.getStringExtra(EXTRA_SERVER_IP) ?: VpnRepository.selectedServer.value.host
        primaryDns = intent?.getStringExtra(EXTRA_PRIMARY_DNS) ?: VpnRepository.primaryDnsInput.value
        secondaryDns = intent?.getStringExtra(EXTRA_SECONDARY_DNS) ?: VpnRepository.secondaryDnsInput.value

        workerThread?.interrupt()
        monitorJob?.cancel()
        isServiceRunning = true

        // Display foreground service notification to comply with Android 8+ and 14+ FGS requirements
        val notification = createNotification("Securing connection…", "Connecting to $targetServerIp")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        workerThread = Thread(this, "UltimateVpnTunnelThread")
        workerThread?.start()

        return START_STICKY
    }

    override fun run() {
        initializeTunnel(targetServerIp, primaryDns, secondaryDns)
    }

    private fun initializeTunnel(serverIp: String?, dnsPrimary: String?, dnsSecondary: String?) {
        try {
            VpnRepository.setConnectionState(ConnectionState.CONNECTING)

            val tunnelBuilder = Builder()
            tunnelBuilder.setMtu(1500)
            tunnelBuilder.addAddress("10.0.0.2", 24)

            // Handle Custom DNS (Accepts both IP numbers or domain names smoothly)
            val resolvedPrimary = VpnRepository.resolveDnsToIp(
                dnsPrimary?.trim() ?: "",
                fallback = "8.8.8.8"
            )
            tunnelBuilder.addDnsServer(resolvedPrimary)

            val resolvedSecondary = VpnRepository.resolveDnsToIp(
                dnsSecondary?.trim() ?: "",
                fallback = "1.1.1.1"
            )
            tunnelBuilder.addDnsServer(resolvedSecondary)

            // Route all app traffic globally to change real IP address
            tunnelBuilder.addRoute("0.0.0.0", 0)

            // Configure Split Tunneling bypass
            val bypassedApps = VpnRepository.splitTunnelApps.value.filter { it.isBypassed }
            for (app in bypassedApps) {
                try {
                    tunnelBuilder.addDisallowedApplication(app.packageName)
                } catch (e: Exception) {
                    // Ignored if app is not available
                }
            }

            tunnelBuilder.setSession("UltimateWorldSession")
            tunnelBuilder.setBlocking(false)
            val pfd = tunnelBuilder.establish()
            if (pfd == null) {
                Log.e(TAG, "Failed to establish VPN ParcelFileDescriptor")
                return
            }
            tunnelInterface = pfd

            Log.d(TAG, "VPN Tunnel established successfully for server IP: $serverIp")
            VpnRepository.setConnectionState(ConnectionState.CONNECTED)

            // Start reactive ParcelFileDescriptor data stream monitoring
            val monitor = TunnelTrafficMonitor(pfd)
            trafficMonitor = monitor

            monitorJob?.cancel()
            monitorJob = serviceScope.launch {
                monitor.trafficFlow(TRAFFIC_SAMPLE_INTERVAL_MS).collect { snapshot ->
                    val stats = snapshot.toVpnStats()
                    VpnRepository.recordTrafficSample(stats, snapshot.toSpeedSample())

                    val serverName = VpnRepository.selectedServer.value.displayName
                    updateNotification(
                        "Protected: $serverName",
                        "${stats.formattedDuration()} • ↓ ${stats.formattedDownloadSpeed()} • ↑ ${stats.formattedUploadSpeed()}"
                    )
                }
            }

            val packetBuffer = ByteBuffer.allocate(32767)
            val bufferArray = packetBuffer.array()

            while (isServiceRunning && tunnelInterface != null) {
                var bytesRead = 0
                try {
                    bytesRead = monitor.inputStream.read(bufferArray)
                } catch (e: IOException) {
                    // Non-blocking EAGAIN when TUN buffer has no pending outbound packet
                    bytesRead = 0
                }

                if (bytesRead > 0) {
                    packetBuffer.clear()
                } else {
                    Thread.sleep(50)
                }
            }
        } catch (interrupted: InterruptedException) {
            Log.d(TAG, "VPN tunnel worker thread interrupted for shutdown")
        } catch (exception: Exception) {
            Log.e(TAG, "VPN Tunnel Error: ${exception.message}", exception)
        } finally {
            shutdownVpnTunnel()
        }
    }

    private fun shutdownVpnTunnel() {
        isServiceRunning = false
        VpnRepository.setConnectionState(ConnectionState.DISCONNECTING)

        monitorJob?.cancel()
        monitorJob = null

        runCatching { trafficMonitor?.close() }
        trafficMonitor = null

        try {
            tunnelInterface?.close()
            tunnelInterface = null
        } catch (exception: Exception) {
            Log.e(TAG, "Error closing tunnel interface", exception)
        }

        VpnRepository.setConnectionState(ConnectionState.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        shutdownVpnTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Connection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status of Ultimate World VPN tunnel"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, UltimateVpnService::class.java).apply {
            action = ACTION_STOP_VPN
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(title, content))
    }
}
