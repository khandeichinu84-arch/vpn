package com.example.model

import java.util.Locale

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

enum class VpnProtocol(val displayName: String, val description: String) {
    WIREGUARD("WireGuard", "Ultra-fast modern cryptography, low battery usage"),
    OPENVPN_UDP("OpenVPN UDP", "High-speed streaming & gaming without buffering"),
    OPENVPN_TCP("OpenVPN TCP", "Bypasses strict restrictive firewalls & networks"),
    IKEV2("IKEv2 / IPsec", "Instant reconnection when switching Wi-Fi / Mobile")
}

enum class DnsOption(
    val displayName: String,
    val primaryDns: String,
    val secondaryDns: String,
    val description: String
) {
    CLOUDFLARE("Cloudflare 1.1.1.1", "1.1.1.1", "1.0.0.1", "Privacy-focused & fastest response time"),
    GOOGLE("Google DNS", "8.8.8.8", "8.8.4.4", "Worldwide speed & rock-solid uptime"),
    QUAD9("Quad9 Cyber Defense", "9.9.9.9", "149.112.112.112", "Blocks malware, spyware & phishing"),
    ADGUARD("AdGuard Clean", "94.140.14.14", "94.140.15.15", "Blocks ads, telemetry & trackers")
}

data class VpnServer(
    val id: String,
    val country: String,
    val city: String,
    val flagEmoji: String,
    val host: String,
    val port: Int = 1194,
    val pingMs: Int = 45,
    val loadPercent: Int = 32,
    val protocols: List<VpnProtocol> = listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP),
    val isOptimal: Boolean = false,
    val isFreeForever: Boolean = true,
    val features: List<String> = listOf("P2P", "Streaming", "Zero Logs", "10Gbps")
) {
    val displayName: String
        get() = "$city, $country"
}

data class VpnStats(
    val durationSeconds: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val speedInBps: Long = 0L,
    val speedOutBps: Long = 0L
) {
    fun formattedDuration(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formattedDownloadSpeed(): String {
        val mbps = (speedInBps * 8.0) / (1024 * 1024)
        return if (mbps >= 1.0) {
            String.format(Locale.US, "%.1f Mbps", mbps)
        } else {
            val kbps = (speedInBps * 8.0) / 1024
            String.format(Locale.US, "%.0f Kbps", kbps)
        }
    }

    fun formattedUploadSpeed(): String {
        val mbps = (speedOutBps * 8.0) / (1024 * 1024)
        return if (mbps >= 1.0) {
            String.format(Locale.US, "%.1f Mbps", mbps)
        } else {
            val kbps = (speedOutBps * 8.0) / 1024
            String.format(Locale.US, "%.0f Kbps", kbps)
        }
    }

    fun formattedDataIn(): String {
        val mb = bytesIn / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    fun formattedDataOut(): String {
        val mb = bytesOut / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isBypassed: Boolean = false
)

data class IpInfo(
    val ip: String,
    val country: String,
    val city: String,
    val isp: String,
    val isProtected: Boolean
)

data class SpeedTestResult(
    val status: String = "Idle",
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val progress: Float = 0f,
    val isRunning: Boolean = false
)
