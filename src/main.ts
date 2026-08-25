import "./styles.css";
import { describeNfcError, nfcSupported, writeWifiTag } from "./nfc";
import { wifiQrDataUrl } from "./qr";
import { validateCredentials } from "./wifi";

const SSID_KEY = "nfc-config:ssid";

const form = document.querySelector<HTMLFormElement>("#wifi-form")!;
const ssidInput = document.querySelector<HTMLInputElement>("#ssid")!;
const passwordInput = document.querySelector<HTMLInputElement>("#password")!;
const togglePassword = document.querySelector<HTMLButtonElement>("#toggle-password")!;
const writeBtn = document.querySelector<HTMLButtonElement>("#write-btn")!;
const cancelBtn = document.querySelector<HTMLButtonElement>("#cancel-btn")!;
const qrBtn = document.querySelector<HTMLButtonElement>("#qr-btn")!;
const statusEl = document.querySelector<HTMLParagraphElement>("#status")!;
const hintEl = document.querySelector<HTMLParagraphElement>("#nfc-hint")!;
const qrPanel = document.querySelector<HTMLElement>("#qr-panel")!;
const qrImage = document.querySelector<HTMLImageElement>("#qr-image")!;

let writeAbort: AbortController | null = null;

function setStatus(message: string, kind: "ok" | "error" | "busy" | "" = "") {
  statusEl.textContent = message;
  if (kind) {
    statusEl.dataset.kind = kind;
  } else {
    delete statusEl.dataset.kind;
  }
}

function readForm(): { ssid: string; password: string } | null {
  const ssid = ssidInput.value.trim();
  const password = passwordInput.value;
  const error = validateCredentials(ssid, password);
  if (error) {
    setStatus(error.message, "error");
    (error.field === "ssid" ? ssidInput : passwordInput).focus();
    return null;
  }
  return { ssid, password };
}

function setWriting(writing: boolean) {
  writeBtn.disabled = writing;
  qrBtn.disabled = writing;
  ssidInput.disabled = writing;
  passwordInput.disabled = writing;
  cancelBtn.hidden = !writing;
  writeBtn.textContent = writing ? "Waiting for tag…" : "Write NFC tag";
}

if (nfcSupported()) {
  hintEl.textContent = "Use an NDEF tag. Hold the phone still until it writes.";
} else {
  hintEl.textContent =
    "Writing tags needs Chrome on Android with NFC. You can still make a QR code here.";
}

try {
  const saved = localStorage.getItem(SSID_KEY);
  if (saved) {
    ssidInput.value = saved;
  }
} catch {
  // Ignore blocked storage.
}

togglePassword.addEventListener("click", () => {
  const hidden = passwordInput.type === "password";
  passwordInput.type = hidden ? "text" : "password";
  togglePassword.textContent = hidden ? "Hide" : "Show";
  togglePassword.setAttribute("aria-pressed", String(hidden));
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const creds = readForm();
  if (!creds) {
    return;
  }

  try {
    localStorage.setItem(SSID_KEY, creds.ssid);
  } catch {
    // Ignore blocked storage.
  }

  if (!nfcSupported()) {
    setStatus("NFC writing needs Chrome on Android. Show a QR code instead.", "error");
    return;
  }

  writeAbort?.abort();
  writeAbort = new AbortController();
  setWriting(true);
  setStatus("Hold your phone against the tag.", "busy");

  try {
    await writeWifiTag(creds.ssid, creds.password, writeAbort.signal);
    setStatus("Tag written. Tap it with a phone to join the network.", "ok");
  } catch (error) {
    setStatus(describeNfcError(error), "error");
  } finally {
    setWriting(false);
    writeAbort = null;
  }
});

cancelBtn.addEventListener("click", () => {
  writeAbort?.abort();
});

qrBtn.addEventListener("click", async () => {
  const creds = readForm();
  if (!creds) {
    qrPanel.hidden = true;
    return;
  }

  try {
    qrImage.src = await wifiQrDataUrl(creds.ssid, creds.password);
    qrPanel.hidden = false;
    setStatus("QR ready.", "ok");
    qrPanel.scrollIntoView({ behavior: "smooth", block: "nearest" });
  } catch {
    setStatus("Could not build a QR code.", "error");
  }
});
