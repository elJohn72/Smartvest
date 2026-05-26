export type ObstacleLevel = 'clear' | 'caution' | 'alert' | 'danger' | 'unknown';

const DANGER_CM = 40;
const ALERT_CM = 100;
const CAUTION_CM = 200;

export const getObstacleLevel = (distanceCm: number | null | undefined): ObstacleLevel => {
  if (distanceCm === null || distanceCm === undefined || !Number.isFinite(distanceCm)) {
    return 'unknown';
  }

  if (distanceCm <= DANGER_CM) {
    return 'danger';
  }

  if (distanceCm <= ALERT_CM) {
    return 'alert';
  }

  if (distanceCm <= CAUTION_CM) {
    return 'caution';
  }

  return 'clear';
};

export const obstacleLevelLabel: Record<ObstacleLevel, string> = {
  clear: 'Camino despejado',
  caution: 'Precaución',
  alert: 'Alerta',
  danger: 'Peligro cercano',
  unknown: 'Sin lectura',
};

export const obstacleLevelStyles: Record<ObstacleLevel, string> = {
  clear: 'bg-green-100 text-green-800 border-green-200',
  caution: 'bg-yellow-100 text-yellow-900 border-yellow-200',
  alert: 'bg-orange-100 text-orange-900 border-orange-200',
  danger: 'bg-red-100 text-red-900 border-red-200',
  unknown: 'bg-slate-100 text-slate-600 border-slate-200',
};
