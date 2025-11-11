
import React, { useEffect, useState } from 'react';
import { UserData, IotData } from '../types';
import { Button } from './Button';
import { Phone, MapPin, User, Activity, Home, AlertTriangle, Battery, Navigation, Cpu, X } from 'lucide-react';
import { subscribeToDevice, simulateMovement, simulateSOS } from '../services/iotService';

interface Props {
  user: UserData;
  onBackHome?: () => void;
}

export const UserProfile: React.FC<Props> = ({ user, onBackHome }) => {
  const [iotData, setIotData] = useState<IotData | null>(null);
  const [isSOS, setIsSOS] = useState(false);
  const [showDevModal, setShowDevModal] = useState(false);

  // Subscribe to simulated/real IoT Data
  useEffect(() => {
    const deviceId = user.deviceId || 'VEST-DEMO';
    const unsubscribe = subscribeToDevice(deviceId, (data) => {
      setIotData(data);
      setIsSOS(data.sosActive);
    });
    return () => unsubscribe();
  }, [user.deviceId]);

  const openMaps = () => {
    if (iotData) {
      window.open(`https://www.google.com/maps/search/?api=1&query=${iotData.latitude},${iotData.longitude}`, '_blank');
    }
  };

  const getBatteryColor = (level?: number) => {
    if (!level) return 'text-gray-400';
    if (level > 50) return 'text-green-500';
    if (level > 20) return 'text-yellow-500';
    return 'text-red-500';
  };

  return (
    <div className="max-w-3xl mx-auto pb-12 relative">
      
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
             <button 
                onClick={() => simulateSOS(false)} 
                className="mt-8 text-sm underline opacity-80 hover:opacity-100"
             >
               Desactivar Alerta (Simulación)
             </button>
           </div>
        </div>
      )}

      {/* Developer / Hardware Guide Modal */}
      {showDevModal && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl relative">
                <button onClick={() => setShowDevModal(false)} className="absolute top-4 right-4 p-2 hover:bg-gray-100 rounded-full text-gray-500">
                    <X size={24} />
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
  "latitude": -0.180653,
  "longitude": -78.467834,
  "sosActive": true,
  "batteryLevel": 85
}`}</pre>
                    </div>
                    <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 text-sm text-yellow-800">
                        Usa los botones de "Simulación" en el perfil para probar sin hardware.
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
                {iotData && (
                    <div className="absolute bottom-2 right-2 bg-green-500 w-8 h-8 rounded-full border-4 border-slate-900 flex items-center justify-center shadow-lg" title="Online">
                        <div className="w-2 h-2 bg-white rounded-full animate-ping"></div>
                    </div>
                )}
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
                        <Phone size={18} /> Llamar Contacto
                    </a>
                    <button 
                        onClick={() => setShowDevModal(true)}
                        className="bg-white/10 hover:bg-white/20 text-white px-4 py-2 rounded-full font-medium backdrop-blur-sm border border-white/10 transition-colors flex items-center gap-2"
                    >
                        <Cpu size={18} /> Conexión Disp.
                    </button>
                </div>
            </div>
        </div>
      </div>

      {/* Dashboard Grid */}
      <div className="px-4 grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* Real-Time Location Card */}
        <div className="bg-white p-6 rounded-2xl shadow-lg border border-slate-100 md:col-span-2">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-slate-800 text-xl flex items-center gap-2">
                    <Navigation className="text-blue-600" /> Ubicación en Tiempo Real
                </h3>
                {iotData && (
                    <div className="flex items-center gap-2 text-sm font-medium bg-slate-100 px-3 py-1 rounded-full">
                         <Battery size={16} className={getBatteryColor(iotData.batteryLevel)} />
                         <span>{iotData.batteryLevel}%</span>
                    </div>
                )}
            </div>
            
            <div className="h-64 bg-slate-100 rounded-xl overflow-hidden relative border border-slate-200 group">
                {iotData ? (
                    <>
                      <iframe 
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
                ) : (
                    <div className="flex items-center justify-center h-full text-gray-400 flex-col gap-2">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                        <p>Buscando señal de SmartVest...</p>
                    </div>
                )}
            </div>
            
            {/* Simulation Controls */}
            <div className="mt-4 pt-4 border-t border-dashed border-slate-200">
                <p className="text-xs text-gray-400 uppercase font-bold mb-2">Simulación (Solo Demo)</p>
                <div className="flex gap-2">
                    <button onClick={simulateMovement} className="px-3 py-1 bg-slate-100 hover:bg-slate-200 rounded text-xs font-medium text-slate-600">
                        Mover GPS
                    </button>
                    <button onClick={() => simulateSOS(true)} className="px-3 py-1 bg-red-100 hover:bg-red-200 rounded text-xs font-medium text-red-600">
                        Activar SOS
                    </button>
                </div>
            </div>
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
