
import React, { useEffect, useRef, useState } from 'react';
import { UserData, IotData } from '../types';
import { Button } from './Button';
import { Phone, MapPin, User, Activity, Home, AlertTriangle, Battery, Navigation, Cpu, X } from 'lucide-react';
import {
  subscribeToDevice,
  subscribeToConnectionStatus,
  simulateMovement,
  simulateSOS,
  fetchIotHistory,
  IoTConnectionStatus,
} from '../services/iotService';
import { DistanceHistoryChart } from './DistanceHistoryChart';
import { IotHistoryPoint } from '../types';
import { formatRelativeTime, hasValidGpsFix } from '../utils/formatRelativeTime';
import { getObstacleLevel, obstacleLevelLabel, obstacleLevelStyles } from '../utils/obstacleLevel';
import { EmergencyPrintSheet } from './EmergencyPrintSheet';
import { showToast } from '../services/toastService';
import { notifySosAlert, requestNotificationPermission } from '../services/notificationService';
import { playSosAlertSound } from '../utils/alertSound';
import { HapticGuideCard } from './HapticGuideCard';
import { Printer, Bell } from 'lucide-react';

interface Props {
  user: UserData;
  onBackHome?: () => void;
}

const isLocalDevHost = (): boolean => {
  if (typeof window === 'undefined') {
    return false;
  }
  const host = window.location.hostname;
  return host === 'localhost' || host === '127.0.0.1';
};

export const UserProfile: React.FC<Props> = ({ user, onBackHome }) => {
  const showIoTSimulation = isLocalDevHost();
  const [iotData, setIotData] = useState<IotData | null>(null);
  const [isSOS, setIsSOS] = useState(false);
  const [showDevModal, setShowDevModal] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<IoTConnectionStatus>('connecting');
  const [nowMs, setNowMs] = useState(Date.now());
  const [historyPoints, setHistoryPoints] = useState<IotHistoryPoint[]>([]);
  const [notificationPermission, setNotificationPermission] = useState<'default' | 'granted' | 'denied'>('default');
  const prevSosRef = useRef(false);
  const deviceId = user.deviceId || 'VEST-DEMO';

  useEffect(() => {
    const tick = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(tick);
  }, []);

  useEffect(() => {
    const unsubscribeData = subscribeToDevice(deviceId, (data) => {
      setIotData(data);
      setIsSOS(data.sosActive);
    });
    const unsubscribeStatus = subscribeToConnectionStatus(deviceId, setConnectionStatus);
    return () => {
      unsubscribeData();
      unsubscribeStatus();
    };
  }, [deviceId]);

  useEffect(() => {
    const loadHistory = () => {
      void fetchIotHistory(deviceId, 60).then(setHistoryPoints);
    };

    loadHistory();
    const intervalId = window.setInterval(loadHistory, 15_000);
    return () => window.clearInterval(intervalId);
  }, [deviceId, iotData?.lastUpdate]);

  useEffect(() => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return;
    }

    setNotificationPermission(Notification.permission);
    if (Notification.permission === 'default') {
      void requestNotificationPermission().then(setNotificationPermission);
    }
  }, []);

  useEffect(() => {
    if (isSOS && !prevSosRef.current) {
      notifySosAlert(user.fullName, deviceId);
      playSosAlertSound();
      showToast('¡SOS activo en el chaleco!', 'error');
    }
    prevSosRef.current = isSOS;
  }, [isSOS, user.fullName, deviceId]);

  const handleEnableNotifications = () => {
    void requestNotificationPermission().then(permission => {
      setNotificationPermission(permission);
      if (permission === 'granted') {
        showToast('Notificaciones activadas para alertas SOS.', 'success');
      } else {
        showToast('Activa las notificaciones en la configuración del navegador.', 'info');
      }
    });
  };

  const connectionLabel: Record<IoTConnectionStatus, string> = {
    connecting: 'Conectando…',
    online: 'En línea',
    stale: 'Sin señal reciente',
    offline: 'Sin datos del chaleco',
  };

  const connectionClass: Record<IoTConnectionStatus, string> = {
    connecting: 'bg-amber-500',
    online: 'bg-green-500',
    stale: 'bg-amber-500',
    offline: 'bg-slate-400',
  };

  const gpsReady = iotData ? hasValidGpsFix(iotData.latitude, iotData.longitude) : false;
  const obstacleLevel = getObstacleLevel(iotData?.distanceCm);
  const obstacleStyle = obstacleLevelStyles[obstacleLevel];

  const handlePrint = () => {
    window.print();
    showToast('Abre el diálogo de impresión del navegador.', 'info');
  };
  const lastUpdateLabel = iotData
    ? `Actualizado ${formatRelativeTime(iotData.lastUpdate, nowMs)}`
    : null;

  const openMaps = () => {
    if (iotData && gpsReady) {
      window.open(`https://www.google.com/maps/search/?api=1&query=${iotData.latitude},${iotData.longitude}`, '_blank', 'noopener,noreferrer');
    }
  };

  const getBatteryColor = (level: number) => {
    if (level > 50) return 'text-green-500';
    if (level > 20) return 'text-yellow-500';
    return 'text-red-500';
  };

  const batteryStatusLabel =
    iotData?.batteryLevel != null
      ? `${iotData.batteryLevel}%`
      : 'Sin sensor en PCB';

  return (
    <div className="max-w-3xl mx-auto pb-12 relative print:hidden">
      <EmergencyPrintSheet user={user} />
      
      {/* SOS Full Screen Overlay */}
      {isSOS && (
        <div className="fixed inset-0 z-[100] bg-red-600 animate-pulse flex flex-col items-center justify-center text-white p-4 text-center">
           <AlertTriangle size={120} className="mb-6 animate-bounce" />
           <h1 className="text-5xl font-black mb-4">¡ALERTA SOS!</h1>
           <p className="text-2xl mb-8">El usuario ha activado el botón de pánico.</p>
           <div className="flex gap-4 flex-col w-full max-w-md">
             <a href={`tel:${user.emergencyPhone}`} className="bg-white text-red-600 py-4 rounded-full font-bold text-xl shadow-xl hover:bg-gray-100 flex items-center justify-center gap-2">
               <Phone /> Llamar a Emergencia
             </a>
             <button 
                onClick={openMaps}
                className="bg-black/30 border-2 border-white text-white py-4 rounded-full font-bold text-xl hover:bg-black/50 flex items-center justify-center gap-2"
             >
               <Navigation /> Ver Ubicación
             </button>
              {showIoTSimulation && (
                <button
                  type="button"
                  onClick={() => simulateSOS(false, deviceId)}
                  className="mt-8 text-sm underline opacity-80 hover:opacity-100"
                >
                  Desactivar Alerta (Simulación)
                </button>
              )}
           </div>
        </div>
      )}

      {/* Developer / Hardware Guide Modal */}
      {showDevModal && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl relative">
                <button
                    type="button"
                    onClick={() => setShowDevModal(false)}
                    aria-label="Cerrar guía de hardware"
                    className="absolute top-4 right-4 p-2 hover:bg-gray-100 rounded-full text-gray-500"
                >
                    <X size={24} aria-hidden="true" />
                </button>
                
                <div className="p-8">
                    <h2 className="text-2xl font-bold text-smart-dark mb-4 flex items-center gap-2">
                        <Cpu /> Integración de Hardware
                    </h2>
                    
                    <p className="mb-4 text-gray-600">
                        Para conectar el chaleco SmartVest a esta aplicación, envía los datos a tu backend:
                    </p>
                    
                    <div className="bg-slate-900 text-slate-200 p-4 rounded-lg font-mono text-sm mb-6 overflow-x-auto">
                        <p className="text-gray-500 mb-2">// Payload JSON</p>
                        <pre>{`{
  "deviceId": "${user.deviceId || 'VEST-001'}",
  "distanceCm": 85.4,
  "latitude": -0.180653,
  "longitude": -78.467834,
  "sosActive": true,
  "batteryLevel": 85
}`}</pre>
                    </div>
                    <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 text-sm text-yellow-800 space-y-2">
                        <p>El esquema actual <strong>no incluye divisor de batería al ADC</strong>. El firmware envía <code>batteryLevel</code> solo si defines <code>SMARTVEST_BATTERY_ADC_PIN</code> (por ejemplo GPIO34) en <code>smartvest_config.h</code>.</p>
                        {showIoTSimulation && (
                          <p>En localhost puedes usar los botones de «Simulación» para probar sin hardware.</p>
                        )}
                    </div>
                </div>
            </div>
        </div>
      )}

      {/* Modern Header */}
      <div className="bg-gradient-to-r from-slate-900 to-blue-900 text-white rounded-b-3xl p-8 pt-12 shadow-2xl mb-8 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-full opacity-10 bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')]"></div>
        
        <div className="relative z-10 flex flex-col md:flex-row items-center gap-8">
            <div className="relative">
                <div className="w-36 h-36 rounded-full p-1 bg-gradient-to-tr from-blue-400 to-purple-500">
                    {user.photo ? (
                        <img src={user.photo} alt={user.fullName} className="w-full h-full rounded-full object-cover border-4 border-slate-900" />
                    ) : (
                        <div className="w-full h-full rounded-full bg-slate-800 flex items-center justify-center text-gray-400 border-4 border-slate-900">
                            <User size={64} />
                        </div>
                    )}
                </div>
                <div
                    className={`absolute bottom-2 right-2 ${connectionClass[connectionStatus]} w-8 h-8 rounded-full border-4 border-slate-900 flex items-center justify-center shadow-lg`}
                    title={connectionLabel[connectionStatus]}
                    aria-label={`Estado del chaleco: ${connectionLabel[connectionStatus]}`}
                >
                    {connectionStatus === 'online' && (
                      <div className="w-2 h-2 bg-white rounded-full animate-ping" aria-hidden="true"></div>
                    )}
                </div>
            </div>
            
            <div className="text-center md:text-left flex-1">
                <div className="flex items-center justify-center md:justify-start gap-3 mb-2">
                    <h1 className="text-3xl md:text-4xl font-bold">{user.fullName}</h1>
                    <span className="px-2 py-0.5 bg-blue-500/30 border border-blue-400 rounded text-xs font-mono tracking-wider">
                        {user.bloodType}
                    </span>
                </div>
                <p className="text-blue-200 text-lg mb-4">ID: {user.nationalId}</p>
                
                <div className="flex flex-wrap justify-center md:justify-start gap-3">
                    <a href={`tel:${user.emergencyPhone}`} className="bg-red-500 hover:bg-red-600 text-white px-6 py-2 rounded-full font-bold shadow-lg transition-transform hover:-translate-y-0.5 flex items-center gap-2">
                        <Phone size={18} aria-hidden="true" /> Llamar contacto
                    </a>
                    <button
                        type="button"
                        onClick={handlePrint}
                        aria-label="Imprimir ficha de emergencia"
                        className="bg-white/10 hover:bg-white/20 text-white px-4 py-2 rounded-full font-medium backdrop-blur-sm border border-white/10 transition-colors flex items-center gap-2"
                    >
                        <Printer size={18} aria-hidden="true" /> Imprimir ficha
                    </button>
                    <button
                        type="button"
                        onClick={() => setShowDevModal(true)}
                        aria-label="Ver detalles de conexión del dispositivo"
                        className="bg-white/10 hover:bg-white/20 text-white px-4 py-2 rounded-full font-medium backdrop-blur-sm border border-white/10 transition-colors flex items-center gap-2"
                    >
                        <Cpu size={18} aria-hidden="true" /> {connectionLabel[connectionStatus]}
                    </button>
                </div>
            </div>
        </div>
      </div>

      {notificationPermission !== 'granted' && (
        <div className="mx-4 mb-4 flex flex-col sm:flex-row items-start sm:items-center gap-3 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900">
          <Bell size={20} className="shrink-0" aria-hidden="true" />
          <p className="flex-1">
            Activa las notificaciones para recibir alertas SOS aunque cambies de pestaña.
          </p>
          <button
            type="button"
            onClick={handleEnableNotifications}
            className="shrink-0 rounded-lg bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700"
          >
            Activar
          </button>
        </div>
      )}

      {/* Dashboard Grid */}
      <div className="px-4 grid grid-cols-1 md:grid-cols-2 gap-6">

        <HapticGuideCard />
        
        {/* Real-Time Location Card */}
        <div className="bg-white p-6 rounded-2xl shadow-lg border border-slate-100 md:col-span-2">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-slate-800 text-xl flex items-center gap-2">
                    <Navigation className="text-blue-600" /> Ubicación en Tiempo Real
                </h3>
                <div className="flex flex-col items-end gap-1">
                  {lastUpdateLabel && (
                    <span className="text-xs text-slate-500 font-medium">{lastUpdateLabel}</span>
                  )}
                  <div
                    className={`flex items-center gap-2 text-sm font-medium px-3 py-1 rounded-full ${
                      iotData?.batteryLevel != null ? 'bg-slate-100' : 'bg-amber-50 text-amber-800'
                    }`}
                    title={
                      iotData?.batteryLevel != null
                        ? 'Nivel reportado por el firmware'
                        : 'La PCB actual no mide batería; ver guía de hardware'
                    }
                  >
                    <Battery
                      size={16}
                      className={
                        iotData?.batteryLevel != null
                          ? getBatteryColor(iotData.batteryLevel)
                          : 'text-amber-600'
                      }
                      aria-hidden="true"
                    />
                    <span>{batteryStatusLabel}</span>
                  </div>
                </div>
            </div>

            <p className="text-xs text-slate-500 mb-3">
              Dispositivo: <span className="font-mono">{deviceId}</span>
              {iotData?.distanceCm != null && (
                <> · Obstáculo: <strong>{Math.round(iotData.distanceCm)} cm</strong></>
              )}
            </p>
            
            <div className="h-64 bg-slate-100 rounded-xl overflow-hidden relative border border-slate-200 group">
                {iotData && gpsReady ? (
                    <>
                      <iframe 
                        title={`Mapa de ubicación de ${user.fullName}`}
                        width="100%" 
                        height="100%" 
                        frameBorder="0" 
                        scrolling="no" 
                        src={`https://maps.google.com/maps?q=${iotData.latitude},${iotData.longitude}&z=15&output=embed`}
                        className="w-full h-full opacity-90 group-hover:opacity-100 transition-opacity"
                      ></iframe>
                      
                      <div className="absolute bottom-4 right-4 flex gap-2">
                         <Button 
                            onClick={openMaps} 
                            className="bg-white text-blue-900 hover:bg-blue-50 py-2 px-4 text-sm shadow-lg"
                         >
                            Abrir en Google Maps
                         </Button>
                      </div>
                    </>
                ) : iotData ? (
                    <div className="flex items-center justify-center h-full text-slate-500 flex-col gap-2 px-6 text-center">
                        <MapPin size={32} className="text-slate-400" aria-hidden="true" />
                        <p className="font-medium">GPS sin fijar todavía</p>
                        <p className="text-sm">
                          El chaleco está en línea. El NEO-6M necesita cielo abierto o ventana para obtener fix
                          {showIoTSimulation ? '; en demo usa «Mover GPS».' : '.'}
                        </p>
                        <p className="text-xs text-slate-400 font-mono">
                          Último reporte: {iotData.latitude.toFixed(5)}, {iotData.longitude.toFixed(5)}
                        </p>
                    </div>
                ) : (
                    <div className="flex items-center justify-center h-full text-gray-400 flex-col gap-2">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                        <p>Buscando señal de SmartVest...</p>
                    </div>
                )}
            </div>
            
            {showIoTSimulation && (
              <details className="mt-4 pt-4 border-t border-dashed border-slate-200">
                  <summary className="text-xs text-gray-400 uppercase font-bold cursor-pointer select-none">
                    Simulación (solo demo)
                  </summary>
                  <div className="flex flex-wrap gap-2 mt-3">
                      <button type="button" onClick={() => simulateMovement(deviceId)} className="px-3 py-1 bg-slate-100 hover:bg-slate-200 rounded text-xs font-medium text-slate-600">
                          Mover GPS
                      </button>
                      <button type="button" onClick={() => simulateSOS(true, deviceId)} className="px-3 py-1 bg-red-100 hover:bg-red-200 rounded text-xs font-medium text-red-600">
                          Activar SOS
                      </button>
                      <button type="button" onClick={() => simulateSOS(false, deviceId)} className="px-3 py-1 bg-slate-100 hover:bg-slate-200 rounded text-xs font-medium text-slate-600">
                          Apagar SOS
                      </button>
                  </div>
              </details>
            )}
        </div>

        <div className={`p-6 rounded-2xl shadow-lg border-2 ${obstacleStyle}`}>
            <h3 className="font-bold text-sm uppercase tracking-wider mb-4 opacity-80">Sensado de obstáculos</h3>
            <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full flex items-center justify-center bg-white/60">
                    <AlertTriangle size={24} aria-hidden="true" />
                </div>
                <div>
                    <p className="font-bold text-lg">
                        {iotData?.distanceCm !== null && iotData?.distanceCm !== undefined
                          ? `${iotData.distanceCm.toFixed(1)} cm`
                          : 'Sin lectura'}
                    </p>
                    <p className="text-sm font-semibold">{obstacleLevelLabel[obstacleLevel]}</p>
                    <p className="text-sm opacity-80 mt-1">
                        Umbrales: peligro ≤40 cm · alerta ≤100 cm · precaución ≤200 cm
                    </p>
                    <p className="text-sm opacity-80 mt-2">
                      Buzzer y vibrador en el chaleco siguen estos mismos umbrales automáticamente.
                    </p>
                </div>
            </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-lg border border-slate-100 md:col-span-2">
            <h3 className="font-bold text-slate-800 text-lg mb-1">Historial de distancia</h3>
            <p className="text-sm text-slate-500 mb-4">Últimos minutos según telemetría guardada en el servidor.</p>
            <DistanceHistoryChart points={historyPoints} />
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-lg border border-slate-100">
            <h3 className="font-bold text-slate-400 text-sm uppercase tracking-wider mb-4">Contacto de Emergencia</h3>
            <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center text-red-500">
                    <Phone size={24} />
                </div>
                <div>
                    <p className="font-bold text-lg text-slate-800">{user.emergencyContact.name}</p>
                    <p className="text-slate-500 text-sm">{user.emergencyContact.relationship}</p>
                    <p className="font-mono text-slate-600 mt-1">{user.emergencyPhone}</p>
                </div>
            </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-lg border border-slate-100">
            <h3 className="font-bold text-slate-400 text-sm uppercase tracking-wider mb-4">Información Médica</h3>
            <div className="flex items-start gap-4">
                <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-500 flex-shrink-0">
                    <Activity size={24} />
                </div>
                <div>
                    <p className="text-slate-700 leading-relaxed text-sm">
                        {user.medicalObservations || "No registra alergias ni condiciones médicas específicas."}
                    </p>
                </div>
            </div>
        </div>

         <div className="bg-white p-6 rounded-2xl shadow-lg border border-slate-100 md:col-span-2">
            <div className="flex items-start gap-4">
                <MapPin className="text-slate-400 mt-1" />
                <div>
                    <h3 className="font-bold text-slate-800 mb-1">Dirección Domiciliaria</h3>
                    <p className="text-slate-600">{user.address}</p>
                </div>
            </div>
        </div>

      </div>

      {onBackHome && (
        <div className="mt-12 px-4 text-center">
            <Button variant="secondary" onClick={onBackHome} icon={<Home size={20} />} className="bg-transparent text-slate-500 border border-slate-200 hover:bg-slate-50 shadow-none">
                Volver al Inicio
            </Button>
        </div>
      )}
    </div>
  );
};
