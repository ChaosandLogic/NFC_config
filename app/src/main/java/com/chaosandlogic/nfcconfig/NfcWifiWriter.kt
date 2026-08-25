package com.chaosandlogic.nfcconfig

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException

object NfcWifiWriter {
    fun write(tag: Tag, ssid: String, password: String) {
        val payload = WifiNdef.encodeWscPayload(ssid, password)
        val record = NdefRecord.createMime(WifiNdef.MEDIA_TYPE, payload)
        val message = NdefMessage(arrayOf(record))
        val size = message.toByteArray().size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) {
                    throw IOException("This tag is locked and cannot be written.")
                }
                if (ndef.maxSize < size) {
                    throw IOException("This tag is too small for these credentials.")
                }
                ndef.writeNdefMessage(message)
            } finally {
                runCatching { ndef.close() }
            }
            return
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            formatable.connect()
            try {
                formatable.format(message)
            } catch (error: FormatException) {
                throw IOException("Could not format this tag for Wi-Fi.", error)
            } finally {
                runCatching { formatable.close() }
            }
            return
        }

        throw IOException("This tag cannot store NDEF Wi-Fi data.")
    }
}
