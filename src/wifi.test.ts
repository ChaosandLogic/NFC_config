import { describe, expect, it } from "vitest";
import {
  encodeWifiUri,
  encodeWscPayload,
  escapeWifiUri,
  parseTlv,
  utf8Bytes,
  validateCredentials,
} from "./wifi";

const VERSION = 0x104a;
const CREDENTIAL = 0x100e;
const NETWORK_INDEX = 0x1026;
const SSID = 0x1045;
const AUTH_TYPE = 0x1003;
const ENCRYPTION_TYPE = 0x100f;
const NETWORK_KEY = 0x1027;
const MAC_ADDRESS = 0x1020;

function u16(bytes: Uint8Array): number {
  return (bytes[0] << 8) | bytes[1];
}

function attr(
  list: { type: number; value: Uint8Array }[],
  type: number,
): Uint8Array {
  const found = list.find((item) => item.type === type);
  if (!found) {
    throw new Error(`Missing attribute 0x${type.toString(16)}`);
  }
  return found.value;
}

describe("validateCredentials", () => {
  it("requires a network name", () => {
    expect(validateCredentials("  ", "password1")).toEqual({
      field: "ssid",
      message: "Enter a network name.",
    });
  });

  it("rejects SSIDs longer than 32 bytes", () => {
    const ssid = "å".repeat(17);
    expect(utf8Bytes(ssid).length).toBeGreaterThan(32);
    expect(validateCredentials(ssid, "password1")?.field).toBe("ssid");
  });

  it("allows an empty password for open networks", () => {
    expect(validateCredentials("cafe", "")).toBeNull();
  });

  it("requires 8–63 characters when a password is set", () => {
    expect(validateCredentials("cafe", "short")?.field).toBe("password");
    expect(validateCredentials("cafe", "x".repeat(64))?.field).toBe("password");
    expect(validateCredentials("cafe", "longenough")).toBeNull();
  });
});

describe("encodeWscPayload", () => {
  it("wraps WPA2 credentials in a configuration token", () => {
    const payload = encodeWscPayload("GuestNet", "secretpass");
    const outer = parseTlv(payload);
    expect(attr(outer, VERSION)).toEqual(Uint8Array.of(0x10));

    const credential = parseTlv(attr(outer, CREDENTIAL));
    expect(attr(credential, NETWORK_INDEX)).toEqual(Uint8Array.of(1));
    expect(attr(credential, SSID)).toEqual(utf8Bytes("GuestNet"));
    expect(u16(attr(credential, AUTH_TYPE))).toBe(0x0020);
    expect(u16(attr(credential, ENCRYPTION_TYPE))).toBe(0x0008);
    expect(attr(credential, NETWORK_KEY)).toEqual(utf8Bytes("secretpass"));
    expect(attr(credential, MAC_ADDRESS)).toEqual(
      Uint8Array.of(0xff, 0xff, 0xff, 0xff, 0xff, 0xff),
    );
  });

  it("encodes an open network without a key", () => {
    const payload = encodeWscPayload("OpenCafe", "");
    const credential = parseTlv(attr(parseTlv(payload), CREDENTIAL));
    expect(u16(attr(credential, AUTH_TYPE))).toBe(0x0001);
    expect(u16(attr(credential, ENCRYPTION_TYPE))).toBe(0x0001);
    expect(attr(credential, NETWORK_KEY).length).toBe(0);
  });

  it("keeps typical home credentials small enough for NTAG213", () => {
    const payload = encodeWscPayload("Home", "correct-horse");
    // MIME type + NDEF header still fit; payload itself stays well under 144.
    expect(payload.length).toBeLessThan(90);
  });
});

describe("WIFI URI", () => {
  it("builds a WPA URI for QR codes", () => {
    expect(encodeWifiUri("Cafe", "latte123")).toBe(
      "WIFI:T:WPA;S:Cafe;P:latte123;H:false;;",
    );
  });

  it("builds a nopass URI for open networks", () => {
    expect(encodeWifiUri("Library", "")).toBe(
      "WIFI:T:nopass;S:Library;P:;H:false;;",
    );
  });

  it("escapes reserved characters in SSID and password", () => {
    expect(escapeWifiUri(`Net;work\\A,B:"x"`)).toBe(
      `Net\\;work\\\\A\\,B\\:\\"x\\"`,
    );
    expect(encodeWifiUri("A;B", "p:w,d")).toBe(
      "WIFI:T:WPA;S:A\\;B;P:p\\:w\\,d;H:false;;",
    );
  });
});
