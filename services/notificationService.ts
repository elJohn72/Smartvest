const getAppBasePath = (): string => {
  if (typeof window === 'undefined') {
    return '/Smartvest/';
  }

  const { pathname } = window.location;
  const [firstSegment] = pathname.split('/').filter(Boolean);
  return firstSegment ? `/${firstSegment}/` : '/';
};

export const registerServiceWorker = async (): Promise<void> => {
  if (!('serviceWorker' in navigator)) {
    return;
  }

  try {
    await navigator.serviceWorker.register(`${getAppBasePath()}sw.js`, { scope: getAppBasePath() });
  } catch (error) {
    console.warn('SmartVest: no se pudo registrar el service worker.', error);
  }
};

export const requestNotificationPermission = async (): Promise<NotificationPermission> => {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return 'denied';
  }

  if (Notification.permission === 'granted' || Notification.permission === 'denied') {
    return Notification.permission;
  }

  return Notification.requestPermission();
};

export const notifySosAlert = (userName: string, deviceId: string): void => {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return;
  }

  if (Notification.permission !== 'granted') {
    return;
  }

  const title = 'SmartVest — SOS activo';
  const body = `${userName} activó el botón de pánico (${deviceId}). Abre el perfil de inmediato.`;

  try {
    const notification = new Notification(title, {
      body,
      tag: 'smartvest-sos',
      requireInteraction: true,
    });

    notification.onclick = () => {
      window.focus();
      notification.close();
    };
  } catch (error) {
    console.warn('SmartVest: notificación SOS no mostrada.', error);
  }
};
