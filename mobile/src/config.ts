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
  process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://192.168.0.103/Smartvest/api';

export const DEMO_DEVICE_ID = 'VEST-DEMO';

/** Cambia este texto en el video para demostrar Fast Refresh. */
export const HOT_RELOAD_LABEL = 'SmartVest móvil — Semana 9';
