import { describe, expect, it } from "vitest";
import { describeNfcError, nfcSupported, WSC_MEDIA_TYPE } from "./nfc";

describe("nfc helpers", () => {
  it("exposes the Wi-Fi Alliance configuration MIME type", () => {
    expect(WSC_MEDIA_TYPE).toBe("application/vnd.wfa.wsc");
  });

  it("reports NFC as unavailable in Node", () => {
    expect(nfcSupported()).toBe(false);
  });

  it("maps Web NFC errors to short messages", () => {
    expect(describeNfcError(Object.assign(new Error("x"), { name: "AbortError" }))).toBe(
      "Write cancelled.",
    );
    expect(
      describeNfcError(
        Object.assign(new Error("x"), { name: "NotSupportedError" }),
      ),
    ).toMatch(/Chrome on Android/);
    expect(describeNfcError("nope")).toBe("Could not write the tag.");
  });
});
