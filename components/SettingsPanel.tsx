import React from 'react';
import { UserData } from '../types';
import { Button } from './Button';
import {
  User,
  Pencil,
  Printer,
  QrCode,
  Bell,
  Cpu,
  ArrowLeft,
  Home,
  LogOut,
} from 'lucide-react';
import { requestNotificationPermission } from '../services/notificationService';
import { showToast } from '../services/toastService';

interface Props {
  user: UserData;
  onBackToProfile: () => void;
  onEditProfile: () => void;
  onViewQr: () => void;
  onPrint: () => void;
  onBackHome: () => void;
}

export const SettingsPanel: React.FC<Props> = ({
  user,
  onBackToProfile,
  onEditProfile,
  onViewQr,
  onPrint,
  onBackHome,
}) => {
  const handleNotifications = () => {
    void requestNotificationPermission().then(permission => {
      if (permission === 'granted') {
        showToast('Notificaciones activadas para alertas SOS.', 'success');
      } else {
        showToast('Permite notificaciones en la configuración del navegador.', 'info');
      }
    });
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <div className="mb-6 flex items-center gap-3">
        <button
          type="button"
          onClick={onBackToProfile}
          className="p-2 rounded-lg hover:bg-slate-200 text-slate-600"
          aria-label="Volver al perfil"
        >
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-2xl font-bold text-smart-dark">Configuración</h1>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden mb-6">
        <div className="p-6 border-b border-slate-100 flex items-center gap-4">
          {user.photo ? (
            <img src={user.photo} alt="" className="w-16 h-16 rounded-full object-cover" />
          ) : (
            <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
              <User size={32} />
            </div>
          )}
          <div>
            <p className="font-bold text-lg">{user.fullName}</p>
            <p className="text-sm text-slate-500">{user.deviceId || 'Sin dispositivo'}</p>
          </div>
        </div>

        <ul className="divide-y divide-slate-100">
          <li>
            <button
              type="button"
              onClick={onEditProfile}
              className="w-full flex items-center gap-4 px-6 py-4 text-left hover:bg-slate-50 transition-colors"
            >
              <Pencil className="text-blue-600" size={22} aria-hidden="true" />
              <div>
                <p className="font-semibold text-slate-800">Editar datos de la persona</p>
                <p className="text-sm text-slate-500">Nombre, médicos, contacto, foto, chaleco</p>
              </div>
            </button>
          </li>
          <li>
            <button
              type="button"
              onClick={onViewQr}
              className="w-full flex items-center gap-4 px-6 py-4 text-left hover:bg-slate-50 transition-colors"
            >
              <QrCode className="text-purple-600" size={22} aria-hidden="true" />
              <div>
                <p className="font-semibold text-slate-800">Código QR de emergencia</p>
                <p className="text-sm text-slate-500">Ver o descargar el QR del perfil</p>
              </div>
            </button>
          </li>
          <li>
            <button
              type="button"
              onClick={onPrint}
              className="w-full flex items-center gap-4 px-6 py-4 text-left hover:bg-slate-50 transition-colors"
            >
              <Printer className="text-slate-700" size={22} aria-hidden="true" />
              <div>
                <p className="font-semibold text-slate-800">Imprimir ficha de emergencia</p>
                <p className="text-sm text-slate-500">Datos médicos en papel para paramédicos</p>
              </div>
            </button>
          </li>
          <li>
            <button
              type="button"
              onClick={handleNotifications}
              className="w-full flex items-center gap-4 px-6 py-4 text-left hover:bg-slate-50 transition-colors"
            >
              <Bell className="text-amber-600" size={22} aria-hidden="true" />
              <div>
                <p className="font-semibold text-slate-800">Notificaciones SOS</p>
                <p className="text-sm text-slate-500">Alertas en este dispositivo cuando hay pánico</p>
              </div>
            </button>
          </li>
        </ul>
      </div>

      <div className="bg-slate-50 rounded-xl border border-slate-200 p-4 mb-6 text-sm text-slate-600">
        <p className="flex items-center gap-2 font-semibold text-slate-800 mb-2">
          <Cpu size={18} aria-hidden="true" /> Resumen del chaleco
        </p>
        <p>ID: <span className="font-mono">{user.deviceId || '—'}</span></p>
        <p>Teléfono emergencia SMS/web: <span className="font-mono">{user.emergencyPhone}</span></p>
      </div>

      <div className="flex flex-col gap-3">
        <Button variant="outline" onClick={onBackToProfile} icon={<ArrowLeft size={20} />}>
          Volver al monitoreo
        </Button>
        <Button variant="secondary" onClick={onBackHome} icon={<LogOut size={20} />}>
          Cerrar sesión
        </Button>
        <Button variant="secondary" onClick={onBackHome} icon={<Home size={20} />} className="!bg-transparent !text-slate-500 !shadow-none border border-slate-200">
          Ir al inicio
        </Button>
      </div>
    </div>
  );
};
