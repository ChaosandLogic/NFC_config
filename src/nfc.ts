import { encodeWscPayload } from "./wifi";

export const WSC_MEDIA_TYPE = "application/vnd.wfa.wsc";

export function nfcSupported(): boolean {
  return typeof window !== "undefined" && "NDEFReader" in window;
}

export function describeNfcError(error: unknown): string {
  if (!(error instanceof Error)) {
    return "Could not write the tag.";
  }
  switch (error.name) {
    case "AbortError":
      return "Write cancelled.";
    case "NotAllowedError":
      return "NFC permission was denied.";
    case "NotSupportedError":
      return "This device cannot write NFC tags. Use Chrome on Android.";
    case "NotReadableError":
      return "NFC is busy. Close other NFC apps and try again.";
    case "NetworkError":
      return "No tag found, or the tag is locked / too small.";
    case "InvalidStateError":
      return "NFC is already in use. Try again.";
    default:
      return error.message || "Could not write the tag.";
  }
}

export async function writeWifiTag(
  ssid: string,
  password: string,
  signal?: AbortSignal,
): Promise<void> {
  if (!nfcSupported() || !window.NDEFReader) {
    throw Object.assign(new Error("NFC writing needs Chrome on Android."), {
      name: "NotSupportedError",
    });
  }

  const reader = new window.NDEFReader();
  await reader.write(
    {
      records: [
        {
          recordType: "mime",
          mediaType: WSC_MEDIA_TYPE,
          data: encodeWscPayload(ssid, password),
        },
      ],
    },
    { overwrite: true, signal },
  );
}
