
import { IotData } from '../types';

// Simple event bus for demo purposes. 
// In production, this would be a WebSocket connection or Firebase subscription.

type IoTListener = (data: IotData) => void;
const listeners: IoTListener[] = [];

// Initial mock state
let currentData: IotData = {
  deviceId: 'VEST-DEMO',
  latitude: -0.180653,
  longitude: -78.467834, // Example coordinates (Quito, Ecuador)
  sosActive: false,
  lastUpdate: Date.now(),
  batteryLevel: 85
};

export const subscribeToDevice = (_deviceId: string, callback: IoTListener) => {
  listeners.push(callback);
  // Send immediate current state
  callback(currentData);
  
  return () => {
    const index = listeners.indexOf(callback);
    if (index > -1) listeners.splice(index, 1);
  };
};

// This function represents the endpoint your ESP32 would hit
export const receiveDeviceUpdate = (data: Partial<IotData>) => {
  currentData = { ...currentData, ...data, lastUpdate: Date.now() };
  console.log("📡 [IOT] Update Received:", currentData);
  
  // Notify all listeners (React Components)
  listeners.forEach(cb => cb(currentData));
};

// --- SIMULATION TOOLS FOR DEMO ---
export const simulateMovement = () => {
  const moveAmount = 0.001;
  const newLat = currentData.latitude + (Math.random() - 0.5) * moveAmount;
  const newLng = currentData.longitude + (Math.random() - 0.5) * moveAmount;
  
  receiveDeviceUpdate({
    latitude: newLat,
    longitude: newLng
  });
};

export const simulateSOS = (active: boolean) => {
  receiveDeviceUpdate({ sosActive: active });
};

// Expose to window for console testing
(window as any).smartVestIoT = {
  receiveDeviceUpdate,
  simulateMovement,
  simulateSOS
};
