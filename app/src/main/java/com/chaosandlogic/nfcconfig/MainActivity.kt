package com.chaosandlogic.nfcconfig

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private var writing by mutableStateOf(false)
    private var status by mutableStateOf<Status>(Status.Idle)
    private var nfcEnabled by mutableStateOf(false)
    private var pendingSsid: String = ""
    private var pendingPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NfcWifiApp(
                initialSsid = prefs.getString(KEY_SSID, "").orEmpty(),
                writing = writing,
                status = status,
                nfcAvailable = nfcAdapter != null,
                nfcEnabled = nfcEnabled,
                onWrite = ::startWrite,
                onCancel = ::cancelWrite,
                onOpenNfcSettings = {
                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        nfcEnabled = nfcAdapter?.isEnabled == true
    }

    override fun onPause() {
        cancelWrite()
        super.onPause()
    }

    private fun startWrite(ssid: String, password: String) {
        val adapter = nfcAdapter
        if (adapter == null) {
            status = Status.Error("This phone cannot write NFC tags.")
            return
        }
        if (!adapter.isEnabled) {
            status = Status.Error("Turn on NFC in Settings, then try again.")
            return
        }
        prefs.edit().putString(KEY_SSID, ssid).apply()
        pendingSsid = ssid
        pendingPassword = password
        writing = true
        status = Status.Busy("Hold your phone against the tag.")
        adapter.enableReaderMode(
            this,
            ::onTag,
            READER_FLAGS,
            null,
        )
    }

    private fun cancelWrite() {
        if (!writing) return
        runCatching { nfcAdapter?.disableReaderMode(this) }
        writing = false
        if (status is Status.Busy) {
            status = Status.Idle
        }
    }

    private fun onTag(tag: Tag) {
        try {
            NfcWifiWriter.write(tag, pendingSsid, pendingPassword)
            runOnUiThread {
                runCatching { nfcAdapter?.disableReaderMode(this) }
                writing = false
                status = Status.Ok("Tag written. Tap it with a phone to join the network.")
            }
        } catch (error: IOException) {
            runOnUiThread {
                status = Status.Error(error.message ?: "Could not write the tag.")
            }
        } catch (error: RuntimeException) {
            runOnUiThread {
                status = Status.Error(error.message ?: "Could not write the tag.")
            }
        }
    }

    companion object {
        private const val PREFS = "nfc_wifi"
        private const val KEY_SSID = "ssid"
        // Do not set FLAG_READER_SKIP_NDEF_CHECK. That flag leaves Ndef and
        // NdefFormatable off the tag tech list, so writes fail even on tags
        // that NFC Tools can program with the same Wi-Fi NDEF payload.
        private const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V
    }
}

sealed class Status {
    data object Idle : Status()
    data class Busy(val message: String) : Status()
    data class Ok(val message: String) : Status()
    data class Error(val message: String) : Status()
}

private val Cream = Color(0xFFEFE8DC)
private val Card = Color(0xFFFBF7F0)
private val Ink = Color(0xFF1B1814)
private val Muted = Color(0xFF6E675C)
private val Line = Color(0xFFD8CEC0)
private val Accent = Color(0xFFB54712)
private val AccentHover = Color(0xFF93390D)
private val OkGreen = Color(0xFF2F6A45)
private val Danger = Color(0xFF9B2C2C)

@Composable
private fun NfcWifiApp(
    initialSsid: String,
    writing: Boolean,
    status: Status,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    onWrite: (String, String) -> Unit,
    onCancel: () -> Unit,
    onOpenNfcSettings: () -> Unit,
) {
    var ssid by rememberSaveable { mutableStateOf(initialSsid) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
        ) {
            Text(
                text = "NFC CONFIG",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Wi-Fi on a tap.",
                color = Ink,
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Enter the network name and password, then write them to a tag.",
                color = Muted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Card)
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FieldLabel("Network name")
                CredentialField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    placeholder = "SSID",
                    enabled = !writing,
                    capitalization = KeyboardCapitalization.None,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FieldLabel("Password")
                    TextButton(
                        onClick = { showPassword = !showPassword },
                        enabled = !writing,
                    ) {
                        Text(
                            text = if (showPassword) "Hide" else "Show",
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                CredentialField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Leave blank if open",
                    enabled = !writing,
                    password = !showPassword,
                )

                Text(
                    text = when {
                        !nfcAvailable -> "This phone has no NFC reader."
                        !nfcEnabled -> "NFC is off. Turn it on to write a tag."
                        else -> "Use an NDEF tag. Hold the phone still until it writes."
                    },
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                Button(
                    onClick = {
                        val error = WifiNdef.validate(ssid, password)
                        if (error != null) {
                            formError = error.message
                        } else {
                            formError = ""
                            onWrite(ssid.trim(), password)
                        }
                    },
                    enabled = !writing,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFFFFF7EF),
                        disabledContainerColor = Accent.copy(alpha = 0.45f),
                        disabledContentColor = Color(0xFFFFF7EF),
                    ),
                ) {
                    Text(
                        text = if (writing) "Waiting for tag…" else "Write NFC tag",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (writing) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                    ) {
                        Text("Cancel")
                    }
                }

                if (!nfcEnabled && nfcAvailable) {
                    OutlinedButton(
                        onClick = onOpenNfcSettings,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                    ) {
                        Text("Open NFC settings")
                    }
                }

                val message = formError.ifEmpty {
                    when (status) {
                        is Status.Idle -> ""
                        is Status.Busy -> status.message
                        is Status.Ok -> status.message
                        is Status.Error -> status.message
                    }
                }
                val messageColor = when {
                    formError.isNotEmpty() -> Danger
                    status is Status.Error -> Danger
                    status is Status.Ok -> OkGreen
                    else -> Ink
                }
                Text(
                    text = message,
                    color = messageColor,
                    fontSize = 14.sp,
                    minLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun CredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    password: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(placeholder, color = Color(0xFFB0A89C)) },
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = Ink,
            unfocusedBorderColor = Line,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            cursorColor = Ink,
        ),
    )
}
