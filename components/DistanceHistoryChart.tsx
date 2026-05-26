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
  const barWidthPx = 7;
  const barGapPx = 2;
  const chartMinWidth = samples.length * barWidthPx + (samples.length - 1) * barGapPx;

  return (
    <div className="w-full min-w-0">
      <div
        className="w-full overflow-x-auto overflow-y-hidden rounded-md border border-slate-100 bg-slate-50/50"
        tabIndex={0}
        aria-label="Gráfico de distancia al obstáculo en los últimos minutos; desliza horizontalmente si hay muchas lecturas"
      >
        <div
          className="flex items-end h-[140px] border-b border-slate-200 pb-1 px-1"
          style={{ minWidth: chartMinWidth, gap: barGapPx }}
          role="img"
          aria-hidden="true"
        >
          {samples.map((point, index) => {
            const distance = point.distanceCm ?? 0;
            const height = Math.max(8, Math.round((Math.min(distance, maxDistanceCm) / maxDistanceCm) * chartHeight));
            const isDanger = distance <= 40;
            const isAlert = distance <= 100 && !isDanger;

            return (
              <div
                key={`${point.lastUpdate}-${index}`}
                className="flex shrink-0 flex-col justify-end items-center"
                style={{ width: barWidthPx }}
                title={`${Math.round(distance)} cm · ${formatTime(point.lastUpdate)}`}
              >
                <div
                  className={`w-full rounded-t ${
                    isDanger ? 'bg-red-500' : isAlert ? 'bg-amber-400' : 'bg-blue-500'
                  }`}
                  style={{ height: `${height}px` }}
                />
              </div>
            );
          })}
        </div>
      </div>
      <div className="flex justify-between gap-2 text-xs text-slate-500 mt-2 px-0.5">
        <span className="shrink-0">Más antiguo ←</span>
        <span className="text-center truncate">{samples.length} lecturas</span>
        <span className="shrink-0">→ Ahora</span>
      </div>
    </div>
  );
};
