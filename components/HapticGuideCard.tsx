import React from 'react';
import { AlertTriangle, Vibrate } from 'lucide-react';

export const HapticGuideCard: React.FC = () => (
  <div className="bg-slate-50 p-6 rounded-2xl shadow-lg border border-slate-200 md:col-span-2">
    <h3 className="font-bold text-slate-800 text-lg mb-2 flex items-center gap-2">
      <Vibrate size={20} className="text-blue-600" aria-hidden="true" />
      Guía de alertas del chaleco
    </h3>
    <p className="text-sm text-slate-600 mb-4">
      El usuario siente buzzer y vibrador según la distancia frontal. Mismos umbrales que los colores de esta pantalla.
    </p>
    <ul className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
      <li className="flex gap-2 items-start">
        <span className="w-3 h-3 rounded-full bg-green-500 mt-1 shrink-0" aria-hidden="true" />
        <span><strong>Más de 200 cm</strong> — sin alerta</span>
      </li>
      <li className="flex gap-2 items-start">
        <span className="w-3 h-3 rounded-full bg-yellow-400 mt-1 shrink-0" aria-hidden="true" />
        <span><strong>≤ 200 cm</strong> — precaución, pulso lento</span>
      </li>
      <li className="flex gap-2 items-start">
        <span className="w-3 h-3 rounded-full bg-orange-500 mt-1 shrink-0" aria-hidden="true" />
        <span><strong>≤ 100 cm</strong> — alerta, pulso medio</span>
      </li>
      <li className="flex gap-2 items-start">
        <AlertTriangle size={14} className="text-red-600 mt-0.5 shrink-0" aria-hidden="true" />
        <span><strong>≤ 40 cm</strong> — peligro, pulso rápido</span>
      </li>
    </ul>
    <p className="text-xs text-slate-500 mt-4">
      Botón SOS: alarma continua hasta soltar. Detalle técnico en{' '}
      <code className="bg-white px-1 rounded">docs/ALERTAS-ACCESIBLES.md</code>.
    </p>
  </div>
);
