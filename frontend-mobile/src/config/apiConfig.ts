import { Platform } from 'react-native';
import Constants from 'expo-constants';

type Extra = {
  BACKEND_URL?: string;
  BACKEND_USERNAME?: string;
  BACKEND_PASSWORD?: string;
};

function getExtra(): Extra {
  const fromExpo =
    (Constants.expoConfig?.extra as Extra | undefined) ??
    ((Constants.manifest as unknown as { extra?: Extra } | null)?.extra);
  return fromExpo ?? {};
}

export function getBackendUrl(): string {
  const override = getExtra().BACKEND_URL;
  if (override && override.trim().length > 0) return override.trim();

  if (Platform.OS === 'android') return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

function base64Encode(value: string): string {
  const g = globalThis as unknown as { btoa?: (s: string) => string };
  if (typeof g.btoa === 'function') return g.btoa(value);

  const chars =
    'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  let out = '';
  let i = 0;
  while (i < value.length) {
    const c1 = value.charCodeAt(i++);
    const c2 = i < value.length ? value.charCodeAt(i++) : Number.NaN;
    const c3 = i < value.length ? value.charCodeAt(i++) : Number.NaN;
    const e1 = c1 >> 2;
    const e2 = ((c1 & 3) << 4) | (Number.isNaN(c2) ? 0 : c2 >> 4);
    const e3 = Number.isNaN(c2)
      ? 64
      : ((c2 & 15) << 2) | (Number.isNaN(c3) ? 0 : c3 >> 6);
    const e4 = Number.isNaN(c3) ? 64 : c3 & 63;
    out += chars.charAt(e1) + chars.charAt(e2) + chars.charAt(e3) + chars.charAt(e4);
  }
  return out;
}

export function getBasicAuthHeader(): string | undefined {
  const { BACKEND_USERNAME, BACKEND_PASSWORD } = getExtra();
  if (!BACKEND_USERNAME || !BACKEND_PASSWORD) return undefined;
  return 'Basic ' + base64Encode(`${BACKEND_USERNAME}:${BACKEND_PASSWORD}`);
}