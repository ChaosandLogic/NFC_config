import QRCode from "qrcode";
import { encodeWifiUri } from "./wifi";

export async function wifiQrDataUrl(
  ssid: string,
  password: string,
): Promise<string> {
  return QRCode.toDataURL(encodeWifiUri(ssid, password), {
    margin: 1,
    width: 280,
    errorCorrectionLevel: "M",
    color: {
      dark: "#1b1814",
      light: "#00000000",
    },
  });
}
