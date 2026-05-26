export const formatRelativeTime = (timestampMs: number, nowMs: number = Date.now()): string => {
  const diffSeconds = Math.max(0, Math.floor((nowMs - timestampMs) / 1000));

  if (diffSeconds < 10) {
    return 'hace unos segundos';
  }

  if (diffSeconds < 60) {
    return `hace ${diffSeconds} s`;
  }

  const diffMinutes = Math.floor(diffSeconds / 60);
  if (diffMinutes < 60) {
    return `hace ${diffMinutes} min`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `hace ${diffHours} h`;
  }

  const diffDays = Math.floor(diffHours / 24);
  return `hace ${diffDays} d`;
};

export const hasValidGpsFix = (latitude: number, longitude: number): boolean => {
  if (latitude === 0 && longitude === 0) {
    return false;
  }

  return Number.isFinite(latitude) && Number.isFinite(longitude);
};
