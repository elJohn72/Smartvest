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

export type HistoryResponse = {
  success: boolean;
  data?: IotState[];
  message?: string;
  cacheTtlSeconds?: number;
};

export type PublicUser = {
  id: string;
  fullName: string;
  nationalId: string;
  age: number;
  bloodType: string;
  address: string;
  emergencyPhone: string;
  medicalObservations: string;
  username: string | null;
  deviceId: string | null;
};

export type UserResponse = {
  success: boolean;
  user?: PublicUser | null;
  message?: string;
  cacheTtlSeconds?: number;
  token?: string;
  authNote?: string;
  deletedId?: string;
};

export type LabProfileInput = {
  fullName: string;
  nationalId: string;
  age: number;
  bloodType: string;
  address: string;
  emergencyPhone: string;
  medicalObservations: string;
  username: string;
  password: string;
  deviceId: string;
};

function apiUrl(path: string): string {
  return `${API_BASE_URL.replace(/\/$/, '')}/${path}`;
}

export function iotStateUrl(deviceId: string): string {
  return `${apiUrl('iot.php')}?deviceId=${encodeURIComponent(deviceId)}`;
}

export function iotHistoryUrl(deviceId: string, limit: number): string {
  return `${iotStateUrl(deviceId)}&history=1&limit=${limit}`;
}

async function request(url: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(url, init);
  } catch {
    throw new Error('No hay conexión con la API. Revisa que el servidor esté encendido y que el teléfono esté en la misma red.');
  }
}

async function readJson<T>(response: Response): Promise<T> {
  const body = (await response.json()) as T & { message?: string };
  if (!response.ok) {
    throw new Error(body.message || `HTTP ${response.status}`);
  }
  return body;
}

export async function fetchIotState(deviceId: string): Promise<IotResponse> {
  return readJson<IotResponse>(await request(iotStateUrl(deviceId)));
}

export async function fetchIotHistory(deviceId: string, limit: number): Promise<HistoryResponse> {
  return readJson<HistoryResponse>(await request(iotHistoryUrl(deviceId, limit)));
}

export async function loginUser(username: string, password: string): Promise<UserResponse> {
  return readJson<UserResponse>(
    await request(apiUrl('users.php'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'login', username, password }),
    }),
  );
}

export async function registerLabUser(input: LabProfileInput): Promise<UserResponse> {
  return readJson<UserResponse>(
    await request(apiUrl('users.php'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'register_lab', ...input }),
    }),
  );
}

export async function fetchUser(id: string): Promise<UserResponse> {
  return readJson<UserResponse>(await request(`${apiUrl('users.php')}?id=${encodeURIComponent(id)}`));
}

export async function updateObservations(user: PublicUser, medicalObservations: string): Promise<UserResponse> {
  const response = await request(apiUrl('users.php'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: user.id,
      fullName: user.fullName,
      nationalId: user.nationalId,
      age: user.age,
      bloodType: user.bloodType,
      address: user.address,
      emergencyPhone: user.emergencyPhone,
      medicalObservations,
      username: user.username,
      deviceId: user.deviceId,
    }),
  });
  const body = (await response.json()) as UserResponse;
  if (!response.ok || !body.success) {
    throw new Error(body.message || `HTTP ${response.status}`);
  }
  return fetchUser(user.id);
}

export async function deleteLabUser(username: string, password: string): Promise<UserResponse> {
  return readJson<UserResponse>(
    await request(apiUrl('users.php'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'delete_lab', username, password }),
    }),
  );
}
