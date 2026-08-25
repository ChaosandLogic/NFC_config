/// <reference types="vite/client" />

declare module "qrcode" {
  export function toDataURL(
    text: string,
    options?: {
      margin?: number;
      width?: number;
      color?: { dark?: string; light?: string };
      errorCorrectionLevel?: "L" | "M" | "Q" | "H";
    },
  ): Promise<string>;
}

interface NDEFRecordInit {
  recordType: string;
  mediaType?: string;
  data?: ArrayBuffer | Uint8Array | DataView | string;
}

interface NDEFMessageInit {
  records: NDEFRecordInit[];
}

interface NDEFWriteOptions {
  overwrite?: boolean;
  signal?: AbortSignal;
}

interface NDEFReader {
  write(message: NDEFMessageInit, options?: NDEFWriteOptions): Promise<void>;
}

interface Window {
  NDEFReader?: { new (): NDEFReader };
}
