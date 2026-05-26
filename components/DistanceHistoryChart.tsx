import React, { useMemo } from 'react';
import { IotHistoryPoint } from '../types';

interface Props {
  points: IotHistoryPoint[];
  maxDistanceCm?: number;
}

const formatTime = (timestampMs: number): string => {
  const date = new Date(timestampMs);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

export const DistanceHistoryChart: React.FC<Props> = ({ points, maxDistanceCm = 400 }) => {
  const samples = useMemo(
    () =>
      points
        .filter(point => point.distanceCm != null && point.distanceCm < 9000)
        .slice(-40),
    [points],
  );

  if (samples.length < 2) {
    return (
      <p className="text-sm text-slate-500">
        Aún no hay suficientes lecturas guardadas. El historial se llena con cada envío del chaleco.
      </p>
    );
  }

  const chartHeight = 140;

  return (
    <div>
      <div
        className="flex items-end gap-1 h-[140px] border-b border-slate-200 pb-1"
        role="img"
        aria-label="Gráfico de distancia al obstáculo en los últimos minutos"
      >
        {samples.map((point, index) => {
          const distance = point.distanceCm ?? 0;
          const height = Math.max(8, Math.round((Math.min(distance, maxDistanceCm) / maxDistanceCm) * chartHeight));
          const isDanger = distance <= 40;
          const isAlert = distance <= 100 && !isDanger;

          return (
            <div
              key={`${point.lastUpdate}-${index}`}
              className="flex-1 min-w-[6px] flex flex-col justify-end items-center group"
              title={`${Math.round(distance)} cm · ${formatTime(point.lastUpdate)}`}
            >
              <div
                className={`w-full rounded-t transition-colors ${
                  isDanger ? 'bg-red-500' : isAlert ? 'bg-amber-400' : 'bg-blue-500'
                }`}
                style={{ height: `${height}px` }}
              />
            </div>
          );
        })}
      </div>
      <div className="flex justify-between text-xs text-slate-500 mt-2">
        <span>Más antiguo</span>
        <span>Últimas {samples.length} lecturas</span>
        <span>Ahora</span>
      </div>
    </div>
  );
};
