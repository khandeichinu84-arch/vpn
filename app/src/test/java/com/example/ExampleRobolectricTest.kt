package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VpnRepository
import com.example.model.ConnectionState
import com.example.model.VpnStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ultimate World VPN", appName)
  }

  @Test
  fun `verify vpn repository server list has free servers`() {
    val servers = VpnRepository.servers.value
    assertTrue("Servers should not be empty", servers.isNotEmpty())
    val optimal = servers.firstOrNull { it.isOptimal }
    assertNotNull("Should have an optimal server option", optimal)
  }

  @Test
  fun `verify vpn stats formatting`() {
    val stats = VpnStats(
      durationSeconds = 3665,
      bytesIn = 1024 * 1024 * 15,
      bytesOut = 1024 * 1024 * 5,
      speedInBps = 1024 * 1024 * 2,
      speedOutBps = 1024 * 512
    )
    assertEquals("01:01:05", stats.formattedDuration())
    assertEquals("15.0 MB", stats.formattedDataIn())
    assertEquals("5.0 MB", stats.formattedDataOut())
    assertTrue(stats.formattedDownloadSpeed().contains("Mbps"))
  }

  @Test
  fun `verify custom dns resolution supports numbers and names`() {
    val googleIp = VpnRepository.resolveDnsToIp("8.8.8.8", "1.1.1.1")
    assertEquals("8.8.8.8", googleIp)

    val googleName = VpnRepository.resolveDnsToIp("dns.google", "1.1.1.1")
    assertEquals("8.8.8.8", googleName)

    val cloudflareName = VpnRepository.resolveDnsToIp("cloudflare", "8.8.8.8")
    assertEquals("1.1.1.1", cloudflareName)

    val quad9Name = VpnRepository.resolveDnsToIp("quad9", "8.8.8.8")
    assertEquals("9.9.9.9", quad9Name)
  }

  @Test
  fun `verify world servers directory contains all requested countries`() {
    val servers = VpnRepository.servers.value
    assertTrue("Should have more than 50 servers", servers.size >= 50)
    assertTrue("Should have Bangladesh server", servers.any { it.country.contains("Bangladesh") })
    assertTrue("Should have Singapore server", servers.any { it.country.contains("Singapore") })
    assertTrue("Should have India server", servers.any { it.country.contains("India") })
    assertTrue("Should have Pakistan server", servers.any { it.country.contains("Pakistan") })
    assertTrue("Should have UAE server", servers.any { it.country.contains("UAE") })
    assertTrue("Should have UK server", servers.any { it.country.contains("United Kingdom") })
    assertTrue("Should have USA server", servers.any { it.country.contains("USA") })
  }
}
