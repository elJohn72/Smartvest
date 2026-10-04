/**
 * URL del backend PHP de SmartVest.
 *
 * Emulador Android (AVD):  http://10.0.2.2/Smartvest/api
 * Simulador iOS / Expo web: http://127.0.0.1/Smartvest/api
 * Dispositivo físico:      http://<IP-LAN-del-Mac>/Smartvest/api
 *
 * HTTP sin TLS solo en desarrollo y acotado a este host.
 * Quitar usesCleartextTraffic / ATS laxo antes de cualquier distribución.
 */
export const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://192.168.0.104/Smartvest/api';

// Default defined by the ESP32 firmware. If a build overrides it, keep this in sync.
export const VEST_DEVICE_ID = 'VEST-001';

/** Prefijo que el backend acepta en register_lab y delete_lab. */
export const LAB_USERNAME = 'eva.lab.presentacion';

/** El historial real pide este tope. La API no baja de 10 ni sube de 120. */
export const HISTORY_LIMIT = 10;

/** Cambia este texto en el video para demostrar Fast Refresh. */
