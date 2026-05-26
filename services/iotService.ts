import { IotData, IotHistoryPoint } from '../types';

type IoTListener = (data: IotData) => void;
export type IoTConnectionStatus = 'connecting' | 'online' | 'stale' | 'offline';

const listeners = new Map<string, IoTListener[]>();
const pollingIntervals = new Map<string, number>();
const connectionListeners = new Map<string, Array<(status: IoTConnectionStatus) => void>>();

const STALE_AFTER_MS = 30_000;
const POLL_INTERVAL_MS = 2_000;
const POLL_INTERVAL_SOS_MS = 1_000;

/** Solo desarrollo local; debe coincidir con SMARTVEST_IOT_API_KEY_DEFAULT en api/config.php */
const LOCAL_IOT_API_KEY = 'smartvest-local-dev-key';

const defaultDeviceState: IotData = {
  deviceId: 'VEST-DEMO',
  distanceCm: 85,
  latitude: -0.180653,
  longitude: -78.467834,
  sosActive: false,
  lastUpdate: Date.now(),
  batteryLevel: 85,
};

const deviceStates = new Map<string, IotData>([[defaultDeviceState.deviceId, defaultDeviceState]]);
const connectionStatus = new Map<string, IoTConnectionStatus>();

const getApiBasePath = (): string => {
  if (typeof window === 'undefined') {
    return '/api';
  }

  const { pathname } = window.location;

  if (pathname.includes('/dist/')) {
    return `${pathname.split('/dist/')[0]}/api`;
  }

  const [firstSegment] = pathname.split('/').filter(Boolean);
  return firstSegment ? `/${firstSegment}/api` : '/api';
};

const buildApiUrl = (endpoint: string): string => `${getApiBasePath()}/${endpoint}`;

const isLocalHost = (): boolean => {
  if (typeof window === 'undefined') {
    return false;
  }

  const host = window.location.hostname;
  return host === 'localhost' || host === '127.0.0.1';
};

const buildIotHeaders = (): HeadersInit => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (isLocalHost()) {
    headers['X-SmartVest-Api-Key'] = LOCAL_IOT_API_KEY;
  }

  return headers;
};

const getDeviceState = (deviceId: string): IotData => {
  const existingState = deviceStates.get(deviceId);
  if (existingState) {
    return existingState;
  }

  const seededState = {
    ...defaultDeviceState,
    deviceId,
    lastUpdate: Date.now(),
  };

  deviceStates.set(deviceId, seededState);
  return seededState;
};

const deriveConnectionStatus = (deviceId: string, fromApi: boolean): IoTConnectionStatus => {
  if (!fromApi) {
    return connectionStatus.get(deviceId) === 'online' ? 'stale' : 'offline';
  }

  const state = deviceStates.get(deviceId);
  if (!state) {
    return 'offline';
  }

  const age = Date.now() - state.lastUpdate;
  if (age > STALE_AFTER_MS) {
    return 'stale';
  }

  return 'online';
};

const setConnectionStatus = (deviceId: string, status: IoTConnectionStatus) => {
  if (connectionStatus.get(deviceId) === status) {
    return;
  }

  connectionStatus.set(deviceId, status);
  (connectionListeners.get(deviceId) || []).forEach(listener => listener(status));
};

const notifyListeners = (deviceId: string, data: IotData) => {
  (listeners.get(deviceId) || []).forEach(listener => listener(data));
};

const getPollInterval = (deviceId: string): number => {
  const state = deviceStates.get(deviceId);
  if (state?.sosActive) {
    return POLL_INTERVAL_SOS_MS;
  }

  return POLL_INTERVAL_MS;
};

const restartPolling = (deviceId: string) => {
  if (!pollingIntervals.has(deviceId) || typeof window === 'undefined') {
    return;
  }

  stopPolling(deviceId);
  startPolling(deviceId);
};

const syncDeviceState = async (deviceId: string): Promise<void> => {
  setConnectionStatus(deviceId, connectionStatus.get(deviceId) === 'online' ? 'online' : 'connecting');

  try {
    const response = await fetch(`${buildApiUrl('iot.php')}?deviceId=${encodeURIComponent(deviceId)}`);

    if (!response.ok) {
      throw new Error('API unavailable');
    }

    const payload = await response.json();
    if (!payload.data) {
      setConnectionStatus(deviceId, 'offline');
      return;
    }

    deviceStates.set(deviceId, payload.data);
    notifyListeners(deviceId, payload.data);
    setConnectionStatus(deviceId, deriveConnectionStatus(deviceId, true));
    restartPolling(deviceId);
  } catch (_error) {
    const localState = getDeviceState(deviceId);
    notifyListeners(deviceId, localState);
    setConnectionStatus(deviceId, deriveConnectionStatus(deviceId, false));
  }
};

const startPolling = (deviceId: string) => {
  if (pollingIntervals.has(deviceId) || typeof window === 'undefined') {
    return;
  }

  const tick = () => {
    void syncDeviceState(deviceId);
  };

  tick();
  const intervalId = window.setInterval(tick, getPollInterval(deviceId));
  pollingIntervals.set(deviceId, intervalId);
};

const stopPolling = (deviceId: string) => {
  const intervalId = pollingIntervals.get(deviceId);
  if (intervalId === undefined) {
    return;
  }

  window.clearInterval(intervalId);
  pollingIntervals.delete(deviceId);
};

export const subscribeToDevice = (deviceId: string, callback: IoTListener) => {
  const currentListeners = listeners.get(deviceId) || [];
  listeners.set(deviceId, [...currentListeners, callback]);

  callback(getDeviceState(deviceId));
  void syncDeviceState(deviceId);
  startPolling(deviceId);

  return () => {
    const registeredListeners = listeners.get(deviceId) || [];
    const nextListeners = registeredListeners.filter(listener => listener !== callback);
    listeners.set(deviceId, nextListeners);

    if (nextListeners.length === 0) {
      stopPolling(deviceId);
      connectionListeners.delete(deviceId);
      connectionStatus.delete(deviceId);
    }
  };
};

export const subscribeToConnectionStatus = (
  deviceId: string,
  callback: (status: IoTConnectionStatus) => void,
) => {
  const current = connectionListeners.get(deviceId) || [];
  connectionListeners.set(deviceId, [...current, callback]);
  callback(connectionStatus.get(deviceId) || 'connecting');

  return () => {
    const next = (connectionListeners.get(deviceId) || []).filter(listener => listener !== callback);
    connectionListeners.set(deviceId, next);
  };
};

export const receiveDeviceUpdate = async (data: Partial<IotData>) => {
  const deviceId = data.deviceId || defaultDeviceState.deviceId;
  const currentData = getDeviceState(deviceId);
  const nextData = { ...currentData, ...data, deviceId, lastUpdate: Date.now() };

  deviceStates.set(deviceId, nextData);
  notifyListeners(deviceId, nextData);
  restartPolling(deviceId);

  try {
    const response = await fetch(buildApiUrl('iot.php'), {
      method: 'POST',
      headers: buildIotHeaders(),
      body: JSON.stringify(nextData),
    });

    if (response.ok) {
      setConnectionStatus(deviceId, 'online');
    }
  } catch (_error) {
    setConnectionStatus(deviceId, 'stale');
  }
};

export const simulateMovement = (deviceId: string = defaultDeviceState.deviceId) => {
  const currentData = getDeviceState(deviceId);
  const moveAmount = 0.001;
  const newLat = currentData.latitude + (Math.random() - 0.5) * moveAmount;
  const newLng = currentData.longitude + (Math.random() - 0.5) * moveAmount;

  void receiveDeviceUpdate({
    deviceId,
    latitude: newLat,
    longitude: newLng,
  });
};

export const simulateSOS = (active: boolean, deviceId: string = defaultDeviceState.deviceId) => {
  void receiveDeviceUpdate({ deviceId, sosActive: active });
};

export const fetchIotHistory = async (
  deviceId: string,
  limit = 60,
): Promise<IotHistoryPoint[]> => {
  try {
    const response = await fetch(
      `${buildApiUrl('iot.php')}?deviceId=${encodeURIComponent(deviceId)}&history=1&limit=${limit}`,
    );

    if (!response.ok) {
      return [];
    }

    const payload = await response.json();
    return Array.isArray(payload.data) ? payload.data : [];
  } catch {
    return [];
  }
};

(window as typeof window & { smartVestIoT?: unknown }).smartVestIoT = {
  receiveDeviceUpdate,
  simulateMovement,
  simulateSOS,
};
