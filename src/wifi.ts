const SSID_MAX_BYTES = 32;
const WPA_PASSWORD_MIN = 8;
const WPA_PASSWORD_MAX = 63;

const ATTR = {
  VERSION: 0x104a,
  CREDENTIAL: 0x100e,
  NETWORK_INDEX: 0x1026,
  SSID: 0x1045,
  AUTH_TYPE: 0x1003,
  ENCRYPTION_TYPE: 0x100f,
  NETWORK_KEY: 0x1027,
  MAC_ADDRESS: 0x1020,
} as const;

const AUTH_OPEN = 0x0001;
const AUTH_WPA2_PERSONAL = 0x0020;
const ENC_NONE = 0x0001;
const ENC_AES = 0x0008;
const BROADCAST_MAC = Uint8Array.of(0xff, 0xff, 0xff, 0xff, 0xff, 0xff);

export type WifiCredentials = {
  ssid: string;
  password: string;
};

export type ValidationError = {
  field: "ssid" | "password";
  message: string;
};

export function utf8Bytes(value: string): Uint8Array {
  return new TextEncoder().encode(value);
}

function concat(parts: Uint8Array[]): Uint8Array {
  const total = parts.reduce((sum, part) => sum + part.length, 0);
  const out = new Uint8Array(total);
  let offset = 0;
  for (const part of parts) {
    out.set(part, offset);
    offset += part.length;
  }
  return out;
}

function tlv(type: number, value: Uint8Array): Uint8Array {
  const out = new Uint8Array(4 + value.length);
  out[0] = (type >> 8) & 0xff;
  out[1] = type & 0xff;
  out[2] = (value.length >> 8) & 0xff;
  out[3] = value.length & 0xff;
  out.set(value, 4);
  return out;
}

function u8(value: number): Uint8Array {
  return Uint8Array.of(value);
}

function u16(value: number): Uint8Array {
  return Uint8Array.of((value >> 8) & 0xff, value & 0xff);
}

export function validateCredentials(
  ssid: string,
  password: string,
): ValidationError | null {
  const trimmedSsid = ssid.trim();
  if (!trimmedSsid) {
    return { field: "ssid", message: "Enter a network name." };
  }
  if (utf8Bytes(trimmedSsid).length > SSID_MAX_BYTES) {
    return {
      field: "ssid",
      message: "Network names can be at most 32 bytes.",
    };
  }

  if (password.length === 0) {
    return null;
  }
  if (password.length < WPA_PASSWORD_MIN) {
    return {
      field: "password",
      message: "Use at least 8 characters, or leave blank for an open network.",
    };
  }
  if (password.length > WPA_PASSWORD_MAX) {
    return {
      field: "password",
      message: "Passwords can be at most 63 characters.",
    };
  }
  return null;
}

/**
 * Wi-Fi Simple Configuration token for MIME type application/vnd.wfa.wsc.
 * Android reads this to offer a one-tap join. Open networks omit a password.
 */
export function encodeWscPayload(ssid: string, password: string): Uint8Array {
  const open = password.length === 0;
  const credential = concat([
    tlv(ATTR.NETWORK_INDEX, u8(1)),
    tlv(ATTR.SSID, utf8Bytes(ssid)),
    tlv(ATTR.AUTH_TYPE, u16(open ? AUTH_OPEN : AUTH_WPA2_PERSONAL)),
    tlv(ATTR.ENCRYPTION_TYPE, u16(open ? ENC_NONE : ENC_AES)),
    tlv(ATTR.NETWORK_KEY, utf8Bytes(password)),
    tlv(ATTR.MAC_ADDRESS, BROADCAST_MAC),
  ]);

  return concat([tlv(ATTR.VERSION, u8(0x10)), tlv(ATTR.CREDENTIAL, credential)]);
}

export function encodeWifiUri(ssid: string, password: string): string {
  const type = password.length === 0 ? "nopass" : "WPA";
  return `WIFI:T:${type};S:${escapeWifiUri(ssid)};P:${escapeWifiUri(password)};H:false;;`;
}

export function escapeWifiUri(value: string): string {
  return value.replace(/[\\;,":]/g, (char) => `\\${char}`);
}

export type WscAttribute = {
  type: number;
  value: Uint8Array;
};

export function parseTlv(bytes: Uint8Array): WscAttribute[] {
  const attributes: WscAttribute[] = [];
  let offset = 0;
  while (offset + 4 <= bytes.length) {
    const type = (bytes[offset] << 8) | bytes[offset + 1];
    const length = (bytes[offset + 2] << 8) | bytes[offset + 3];
    offset += 4;
    if (offset + length > bytes.length) {
      throw new Error("Truncated WSC attribute");
    }
    attributes.push({ type, value: bytes.slice(offset, offset + length) });
    offset += length;
  }
  if (offset !== bytes.length) {
    throw new Error("Trailing bytes in WSC payload");
  }
  return attributes;
}
