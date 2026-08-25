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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            NekoNfcApp(
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
        private const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }
}

sealed class Status {
    data object Idle : Status()
    data class Busy(val message: String) : Status()
    data class Ok(val message: String) : Status()
    data class Error(val message: String) : Status()
}

@Composable
private fun NekoNfcApp(
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
    val cardShape = RoundedCornerShape(8.dp)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NekoColors.Oatmeal,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NekoMonogram(size = 22.dp)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "NEKO HEALTH AB",
                    color = NekoColors.Terracotta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.4.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Spacer(Modifier.height(18.dp))
            NekoBrandHeader()
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Ahead of Your Health",
                color = NekoColors.Stone,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Wi-Fi on a tap.",
                color = NekoColors.Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 36.sp,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Write a clinic or guest network onto an NFC tag so a phone can join with one tap.",
                color = NekoColors.Stone,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(NekoColors.Porcelain)
                    .border(1.dp, NekoColors.Line, cardShape)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FieldLabel("Network name")
                CredentialField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    placeholder = "SSID",
                    enabled = !writing,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FieldLabel("Password")
                    TextButton(
                        onClick = { showPassword = !showPassword },
                        enabled = !writing,
                    ) {
                        Text(
                            text = if (showPassword) "HIDE" else "SHOW",
                            color = NekoColors.Terracotta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.4.sp,
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
                    color = NekoColors.Stone,
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
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NekoColors.Ink,
                        contentColor = NekoColors.OnAccent,
                        disabledContainerColor = NekoColors.Ink.copy(alpha = 0.40f),
                        disabledContentColor = NekoColors.OnAccent,
                    ),
                ) {
                    Text(
                        text = if (writing) "Waiting for tag…" else "Write NFC tag",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (writing) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NekoColors.Ink),
                    ) {
                        Text("Cancel")
                    }
                }

                if (!nfcEnabled && nfcAvailable) {
                    OutlinedButton(
                        onClick = onOpenNfcSettings,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NekoColors.Ink),
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
                    formError.isNotEmpty() -> NekoColors.Danger
                    status is Status.Error -> NekoColors.Danger
                    status is Status.Ok -> NekoColors.Teal
                    else -> NekoColors.Ink
                }
                Text(
                    text = message,
                    color = messageColor,
                    fontSize = 14.sp,
                    minLines = 1,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Neko Health AB  ·  Stockholm",
                color = NekoColors.Stone,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = NekoColors.Stone,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
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
        placeholder = { Text(placeholder, color = NekoColors.Placeholder) },
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = NekoColors.Porcelain,
            unfocusedContainerColor = NekoColors.Porcelain,
            disabledContainerColor = NekoColors.Porcelain,
            focusedBorderColor = NekoColors.Ink,
            unfocusedBorderColor = NekoColors.Line,
            focusedTextColor = NekoColors.Ink,
            unfocusedTextColor = NekoColors.Ink,
            cursorColor = NekoColors.Ink,
        ),
    )
}
