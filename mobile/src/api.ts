import { API_BASE_URL } from './config';

export type IotState = {
  deviceId: string;
  distanceCm: number | null;
  latitude: number;
  longitude: number;
  sosActive: boolean;
  lastUpdate: number;
  batteryLevel: number | null;
};

export type IotResponse = {
  success: boolean;
  data: IotState | null;
  message?: string;
  cacheTtlSeconds?: number;
};

export function iotStateUrl(deviceId: string): string {
  return `${API_BASE_URL.replace(/\/$/, '')}/iot.php?deviceId=${encodeURIComponent(deviceId)}`;
}

export async function fetchIotState(deviceId: string): Promise<IotResponse> {
  const url = iotStateUrl(deviceId);
  const response = await fetch(url);
  const body = (await response.json()) as IotResponse;
  if (!response.ok) {
    throw new Error(body.message || `HTTP ${response.status} en ${url}`);
  }
  return body;
}
