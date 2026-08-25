# NFC Wi-Fi

An **Android app** that writes a Wi-Fi **network name** and **password** to an NFC tag. Install it on the phone. No computer or web server is required.

iPhone cannot write these tags from a third-party app in a useful way, so this project is Android-only.

## Install on the phone

1. On the Android phone, open this GitHub repo (or the Actions run) and download `app-debug.apk`.
2. Allow installing from that source if Android asks.
3. Open **NFC Wi-Fi**, turn NFC on, enter the SSID and password, tap **Write NFC tag**, and hold the phone to an NDEF sticker.

After that, another phone can tap the tag to join the network.

## Build

Needs JDK 17+ and the Android SDK.

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew test assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## What gets written

One NDEF record:

- MIME type `application/vnd.wfa.wsc`
- Wi-Fi Simple Configuration credential (WPA2-Personal, or open if the password is left blank)

Leave the password empty for an open network. Typical home credentials fit on an NTAG213; a long SSID plus a long password is safer on NTAG215 or larger.

The password is never stored. The SSID may be remembered on the phone.
