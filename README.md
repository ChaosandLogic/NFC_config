# NFC Wi-Fi

A small **web app** (not a Windows or Play Store install) that writes a Wi-Fi **network name** and **password** to an NFC tag. That is all it does.

Open it in **Chrome on Android**, fill in the SSID and password, then tap **Write NFC tag** and hold the phone to an NDEF sticker. Phones that tap the tag afterwards can join the network.

If NFC writing is not available (Windows, desktop, iPhone, other browsers), use **Show QR code**. Camera apps understand the same credentials.

| What you want | Where it works |
| --- | --- |
| Write an NFC tag | Chrome on **Android** with NFC on |
| Join from a written tag | Android (tap to join); iPhone (tap, then confirm) |
| QR code | Any phone camera |
| Fill in SSID / password | Any modern browser, including Windows |

## Use

```bash
npm install
npm run dev
```

Then open the local URL on the phone that will write the tag. Chrome only treats NFC as available in a secure context (`https://` or `http://localhost`).

```bash
npm test
npm run build
```

## What gets written

The tag receives one NDEF record:

- MIME type `application/vnd.wfa.wsc`
- Wi-Fi Simple Configuration credential (WPA2-Personal, or open if the password is left blank)

Leave the password empty for an open network. Typical home credentials fit on an NTAG213; a long SSID plus a long password is safer on NTAG215 or larger.

## Limits

- Writing uses the [Web NFC](https://developer.chrome.com/docs/capabilities/nfc) API: **Android Chrome** with NFC enabled.
- Reading/joining is handled by the phone OS, not this app.
- Nothing is uploaded. The SSID may be remembered in `localStorage` on this device; the password is not.
