package com.example.tunnel

import android.os.ParcelFileDescriptor
import com.example.model.SpeedSample
import com.example.model.VpnStats
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.io.Closeable
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * Snapshot emitted at each sampling tick of [TunnelTrafficMonitor.trafficFlow].
 */
data class TrafficSnapshot(
    val timestampMs: Long,
    val durationSeconds: Long,
    val totalBytesReceived: Long,
    val totalBytesSent: Long,
    val downloadBytesPerSec: Long,
    val uploadBytesPerSec: Long
) {
    fun toVpnStats(): VpnStats = VpnStats(
        durationSeconds = durationSeconds,
        bytesIn = totalBytesReceived,
        bytesOut = totalBytesSent,
        speedInBps = downloadBytesPerSec,
        speedOutBps = uploadBytesPerSec
    )

    fun toSpeedSample(): SpeedSample = SpeedSample(
        timestampMs = timestampMs,
        downloadBytesPerSec = downloadBytesPerSec,
        uploadBytesPerSec = uploadBytesPerSec
    )
}

/**
 * [FilterInputStream] wrapper that atomically counts every byte read from the underlying stream.
 */
class CountingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
    private val totalBytes = AtomicLong(0L)

    val byteCount: Long
        get() = totalBytes.get()

    override fun read(): Int {
        val byteRead = `in`.read()
        if (byteRead >= 0) {
            totalBytes.incrementAndGet()
        }
        return byteRead
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val bytesRead = `in`.read(b, off, len)
        if (bytesRead > 0) {
            totalBytes.addAndGet(bytesRead.toLong())
        }
        return bytesRead
    }
}

/**
 * [FilterOutputStream] wrapper that atomically counts every byte written to the underlying stream.
 */
class CountingOutputStream(delegate: OutputStream) : FilterOutputStream(delegate) {
    private val totalBytes = AtomicLong(0L)

    val byteCount: Long
        get() = totalBytes.get()

    override fun write(b: Int) {
        out.write(b)
        totalBytes.incrementAndGet()
    }

    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        if (len > 0) {
            totalBytes.addAndGet(len.toLong())
        }
    }
}

/**
 * Monitors bytes sent (read from the TUN [ParcelFileDescriptor] outbound from device apps)
 * and bytes received (written to the TUN [ParcelFileDescriptor] inbound to device apps)
 * and exposes a reactive [Flow] of [TrafficSnapshot] throughput samples.
 */
class TunnelTrafficMonitor(
    readFd: FileDescriptor,
    writeFd: FileDescriptor
) : Closeable {

    constructor(parcelFileDescriptor: ParcelFileDescriptor) : this(
        readFd = parcelFileDescriptor.fileDescriptor,
        writeFd = parcelFileDescriptor.fileDescriptor
    )

    constructor(
        readPfd: ParcelFileDescriptor,
        writePfd: ParcelFileDescriptor
    ) : this(
        readFd = readPfd.fileDescriptor,
        writeFd = writePfd.fileDescriptor
    )

    val inputStream: CountingInputStream = CountingInputStream(FileInputStream(readFd))
    val outputStream: CountingOutputStream = CountingOutputStream(FileOutputStream(writeFd))

    /** Total bytes read from the TUN interface (outbound packets sent by apps into the VPN tunnel). */
    val bytesSent: Long
        get() = inputStream.byteCount

    /** Total bytes written to the TUN interface (inbound packets received from the VPN tunnel). */
    val bytesReceived: Long
        get() = outputStream.byteCount

    /**
     * Emits a [TrafficSnapshot] every [sampleIntervalMs] with exact upload/download byte rates
     * calculated from the [CountingInputStream] and [CountingOutputStream] counters.
     */
    fun trafficFlow(sampleIntervalMs: Long = 1000L): Flow<TrafficSnapshot> = flow {
        val startNanos = System.nanoTime()
        var lastSampleNanos = startNanos
        var lastSent = bytesSent
        var lastReceived = bytesReceived

        while (currentCoroutineContext().isActive) {
            delay(sampleIntervalMs)

            val nowNanos = System.nanoTime()
            val elapsedNanos = (nowNanos - lastSampleNanos).coerceAtLeast(1L)
            val totalDurationSeconds = (nowNanos - startNanos) / 1_000_000_000L

            val currentSent = bytesSent
            val currentReceived = bytesReceived

            val deltaSent = (currentSent - lastSent).coerceAtLeast(0L)
            val deltaReceived = (currentReceived - lastReceived).coerceAtLeast(0L)

            val uploadBps = (deltaSent * 1_000_000_000L) / elapsedNanos
            val downloadBps = (deltaReceived * 1_000_000_000L) / elapsedNanos

            lastSampleNanos = nowNanos
            lastSent = currentSent
            lastReceived = currentReceived

            emit(
                TrafficSnapshot(
                    timestampMs = System.currentTimeMillis(),
                    durationSeconds = totalDurationSeconds,
                    totalBytesReceived = currentReceived,
                    totalBytesSent = currentSent,
                    downloadBytesPerSec = downloadBps,
                    uploadBytesPerSec = uploadBps
                )
            )
        }
    }

    override fun close() {
        runCatching { inputStream.close() }
        runCatching { outputStream.close() }
    }
}
