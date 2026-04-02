import { IotData } from '../types';

type IoTListener = (data: IotData) => void;

const listeners = new Map<string, IoTListener[]>();
const pollingIntervals = new Map<string, number>();

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

const notifyListeners = (deviceId: string, data: IotData) => {
  (listeners.get(deviceId) || []).forEach(listener => listener(data));
};

const syncDeviceState = async (deviceId: string): Promise<void> => {
  try {
    const response = await fetch(`${buildApiUrl('iot.php')}?deviceId=${encodeURIComponent(deviceId)}`);

    if (!response.ok) {
      throw new Error('API unavailable');
    }

    const payload = await response.json();
    if (!payload.data) {
      return;
    }

    deviceStates.set(deviceId, payload.data);
    notifyListeners(deviceId, payload.data);
  } catch (_error) {
    const localState = getDeviceState(deviceId);
    notifyListeners(deviceId, localState);
  }
};

const startPolling = (deviceId: string) => {
  if (pollingIntervals.has(deviceId) || typeof window === 'undefined') {
    return;
  }

  const intervalId = window.setInterval(() => {
    void syncDeviceState(deviceId);
  }, 3000);

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
    }
  };
};

export const receiveDeviceUpdate = async (data: Partial<IotData>) => {
  const deviceId = data.deviceId || defaultDeviceState.deviceId;
  const currentData = getDeviceState(deviceId);
  const nextData = { ...currentData, ...data, deviceId, lastUpdate: Date.now() };

  deviceStates.set(deviceId, nextData);
  notifyListeners(deviceId, nextData);

  try {
    await fetch(buildApiUrl('iot.php'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(nextData),
    });
  } catch (_error) {
    // Fallback local: keep the last in-memory state when the PHP API is not available.
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

(window as typeof window & { smartVestIoT?: unknown }).smartVestIoT = {
  receiveDeviceUpdate,
  simulateMovement,
  simulateSOS,
};
