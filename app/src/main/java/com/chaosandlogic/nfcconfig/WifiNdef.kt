package com.chaosandlogic.nfcconfig

data class ValidationError(
    val field: Field,
    val message: String,
) {
    enum class Field { SSID, PASSWORD }
}

object WifiNdef {
    const val MEDIA_TYPE = "application/vnd.wfa.wsc"

    private const val SSID_MAX_BYTES = 32
    private const val WPA_PASSWORD_MIN = 8
    private const val WPA_PASSWORD_MAX = 63

    private const val ATTR_VERSION = 0x104A
    private const val ATTR_CREDENTIAL = 0x100E
    private const val ATTR_NETWORK_INDEX = 0x1026
    private const val ATTR_SSID = 0x1045
    private const val ATTR_AUTH_TYPE = 0x1003
    private const val ATTR_ENCRYPTION_TYPE = 0x100F
    private const val ATTR_NETWORK_KEY = 0x1027
    private const val ATTR_MAC_ADDRESS = 0x1020

    private const val AUTH_OPEN = 0x0001
    private const val AUTH_WPA2_PERSONAL = 0x0020
    private const val ENC_NONE = 0x0001
    private const val ENC_AES = 0x0008

    private val broadcastMac = byteArrayOf(
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
    )

    fun validate(ssid: String, password: String): ValidationError? {
        val trimmed = ssid.trim()
        if (trimmed.isEmpty()) {
            return ValidationError(ValidationError.Field.SSID, "Enter a network name.")
        }
        if (trimmed.toByteArray(Charsets.UTF_8).size > SSID_MAX_BYTES) {
            return ValidationError(
                ValidationError.Field.SSID,
                "Network names can be at most 32 bytes.",
            )
        }
        if (password.isEmpty()) return null
        if (password.length < WPA_PASSWORD_MIN) {
            return ValidationError(
                ValidationError.Field.PASSWORD,
                "Use at least 8 characters, or leave blank for an open network.",
            )
        }
        if (password.length > WPA_PASSWORD_MAX) {
            return ValidationError(
                ValidationError.Field.PASSWORD,
                "Passwords can be at most 63 characters.",
            )
        }
        return null
    }

    /**
     * Wi-Fi Simple Configuration token. Android offers a one-tap join when it
     * reads MIME type [MEDIA_TYPE]. An empty password means an open network.
     */
    fun encodeWscPayload(ssid: String, password: String): ByteArray {
        val open = password.isEmpty()
        val credential = concat(
            tlv(ATTR_NETWORK_INDEX, byteArrayOf(1)),
            tlv(ATTR_SSID, ssid.toByteArray(Charsets.UTF_8)),
            tlv(ATTR_AUTH_TYPE, u16(if (open) AUTH_OPEN else AUTH_WPA2_PERSONAL)),
            tlv(ATTR_ENCRYPTION_TYPE, u16(if (open) ENC_NONE else ENC_AES)),
            tlv(ATTR_NETWORK_KEY, password.toByteArray(Charsets.UTF_8)),
            tlv(ATTR_MAC_ADDRESS, broadcastMac),
        )
        return concat(
            tlv(ATTR_VERSION, byteArrayOf(0x10)),
            tlv(ATTR_CREDENTIAL, credential),
        )
    }

    data class Attribute(val type: Int, val value: ByteArray)

    fun parseTlv(bytes: ByteArray): List<Attribute> {
        val attributes = ArrayList<Attribute>()
        var offset = 0
        while (offset + 4 <= bytes.size) {
            val type = u16At(bytes, offset)
            val length = u16At(bytes, offset + 2)
            offset += 4
            require(offset + length <= bytes.size) { "Truncated WSC attribute" }
            attributes += Attribute(type, bytes.copyOfRange(offset, offset + length))
            offset += length
        }
        require(offset == bytes.size) { "Trailing bytes in WSC payload" }
        return attributes
    }

    private fun tlv(type: Int, value: ByteArray): ByteArray {
        val out = ByteArray(4 + value.size)
        out[0] = (type shr 8).toByte()
        out[1] = type.toByte()
        out[2] = (value.size shr 8).toByte()
        out[3] = value.size.toByte()
        System.arraycopy(value, 0, out, 4, value.size)
        return out
    }

    private fun u16(value: Int): ByteArray =
        byteArrayOf((value shr 8).toByte(), value.toByte())

    private fun u16At(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun concat(vararg parts: ByteArray): ByteArray {
        val out = ByteArray(parts.sumOf { it.size })
        var offset = 0
        for (part in parts) {
            System.arraycopy(part, 0, out, offset, part.size)
            offset += part.size
        }
        return out
    }
}
