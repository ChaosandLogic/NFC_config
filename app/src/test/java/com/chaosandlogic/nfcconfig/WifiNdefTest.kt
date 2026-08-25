package com.chaosandlogic.nfcconfig

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets

class WifiNdefTest {
    private val version = 0x104A
    private val credential = 0x100E
    private val networkIndex = 0x1026
    private val ssid = 0x1045
    private val authType = 0x1003
    private val encryptionType = 0x100F
    private val networkKey = 0x1027
    private val macAddress = 0x1020

    @Test
    fun requiresNetworkName() {
        val error = WifiNdef.validate("  ", "password1")
        assertEquals(ValidationError.Field.SSID, error?.field)
        assertEquals("Enter a network name.", error?.message)
    }

    @Test
    fun rejectsLongSsid() {
        val tooLong = "å".repeat(17)
        assertEquals(ValidationError.Field.SSID, WifiNdef.validate(tooLong, "password1")?.field)
    }

    @Test
    fun allowsEmptyPasswordForOpenNetwork() {
        assertNull(WifiNdef.validate("cafe", ""))
    }

    @Test
    fun requiresEightToSixtyThreeCharacterPassword() {
        assertEquals(ValidationError.Field.PASSWORD, WifiNdef.validate("cafe", "short")?.field)
        assertEquals(
            ValidationError.Field.PASSWORD,
            WifiNdef.validate("cafe", "x".repeat(64))?.field,
        )
        assertNull(WifiNdef.validate("cafe", "longenough"))
    }

    @Test
    fun wrapsWpa2CredentialsInConfigurationToken() {
        val payload = WifiNdef.encodeWscPayload("GuestNet", "secretpass")
        val outer = WifiNdef.parseTlv(payload)
        assertArrayEquals(byteArrayOf(0x10), attr(outer, version))

        val inner = WifiNdef.parseTlv(attr(outer, credential))
        assertArrayEquals(byteArrayOf(1), attr(inner, networkIndex))
        assertArrayEquals("GuestNet".toByteArray(StandardCharsets.UTF_8), attr(inner, ssid))
        assertEquals(0x0020, u16(attr(inner, authType)))
        assertEquals(0x0008, u16(attr(inner, encryptionType)))
        assertArrayEquals("secretpass".toByteArray(StandardCharsets.UTF_8), attr(inner, networkKey))
        assertArrayEquals(
            byteArrayOf(
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            ),
            attr(inner, macAddress),
        )
    }

    @Test
    fun encodesOpenNetworkWithoutKey() {
        val payload = WifiNdef.encodeWscPayload("OpenCafe", "")
        val inner = WifiNdef.parseTlv(attr(WifiNdef.parseTlv(payload), credential))
        assertEquals(0x0001, u16(attr(inner, authType)))
        assertEquals(0x0001, u16(attr(inner, encryptionType)))
        assertEquals(0, attr(inner, networkKey).size)
    }

    @Test
    fun typicalHomeCredentialsStaySmall() {
        val payload = WifiNdef.encodeWscPayload("Home", "correct-horse")
        assert(payload.size < 90)
    }

    private fun attr(list: List<WifiNdef.Attribute>, type: Int): ByteArray {
        val found = list.firstOrNull { it.type == type }
            ?: error("Missing attribute 0x${type.toString(16)}")
        return found.value
    }

    private fun u16(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
}
