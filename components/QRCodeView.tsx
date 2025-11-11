
import React, { useRef, useState } from 'react';
import { QRCodeCanvas } from 'qrcode.react';
import { UserData } from '../types';
import { Button } from './Button';
import { Download, Eye, Home, FileText, Link as LinkIcon, Globe, Share2 } from 'lucide-react';

interface Props {
  user: UserData;
  onBackHome: () => void;
}

export const QRCodeView: React.FC<Props> = ({ user, onBackHome }) => {
  const qrRef = useRef<HTMLDivElement>(null);
  const [mode, setMode] = useState<'data' | 'link'>('data'); 

  const downloadQR = () => {
    const canvas = qrRef.current?.querySelector('canvas');
    if (canvas) {
      const url = canvas.toDataURL('image/png');
      const link = document.createElement('a');
      link.href = url;
      link.download = `smartvest-${user.fullName.replace(/\s+/g, '-').toLowerCase()}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  // Generate QR Data
  const getQRData = () => {
    if (mode === 'link') {
        // PORTABLE LINK STRATEGY:
        // Encode the full user object into the URL so it works on other devices without a database.
        try {
            const jsonString = JSON.stringify(user);
            // Safe Base64 encoding for UTF-8 strings (handles accents properly)
            const encodedData = btoa(unescape(encodeURIComponent(jsonString)));
            
            const origin = window.location.origin;
            const path = window.location.pathname;
            const cleanPath = path.endsWith('/') ? path.slice(0, -1) : path;
            
            // The URL will contain the data parameter
            return `${origin}${cleanPath}?data=${encodedData}`;
        } catch (e) {
            console.error("Error encoding data", e);
            return "ErrorGenerandoQR";
        }
    }

    // Offline Text Mode
    return `Emergencia SmartVest
------------------
PACIENTE: ${user.fullName}
C.I.: ${user.nationalId}
SANGRE: ${user.bloodType}
EDAD: ${user.age}
------------------
CONTACTO: ${user.emergencyContact.name}
RELACIÓN: ${user.emergencyContact.relationship}
TEL: ${user.emergencyPhone}
------------------
OBSERVACIONES: ${user.medicalObservations}`;
  };

  const qrValue = getQRData();

  return (
    <div className="max-w-2xl mx-auto space-y-6 text-center pb-12">
      <div className="bg-white p-8 rounded-2xl shadow-xl border-2 border-smart-gray">
        
        <div className="flex flex-col items-center mb-6">
            <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-4">
                {mode === 'data' ? <FileText size={32} /> : <Globe size={32} />}
            </div>
            <h2 className="text-3xl font-bold text-smart-dark">Registro Exitoso</h2>
            <p className="text-gray-500">El usuario ha sido guardado localmente.</p>
        </div>
        
        {/* Toggle Modes */}
        <div className="bg-slate-100 p-1 rounded-lg inline-flex mb-6 flex-wrap justify-center">
            <button 
                onClick={() => setMode('data')}
                className={`px-4 py-2 rounded-md text-sm font-bold transition-all flex items-center gap-2 ${mode === 'data' ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
                <FileText size={16} /> Modo Texto (Offline)
            </button>
            <button 
                onClick={() => setMode('link')}
                className={`px-4 py-2 rounded-md text-sm font-bold transition-all flex items-center gap-2 ${mode === 'link' ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
                <LinkIcon size={16} /> Modo Web (Hosting)
            </button>
        </div>

        {/* Instructions */}
        {mode === 'data' ? (
            <p className="text-sm text-slate-600 mb-4 max-w-md mx-auto bg-slate-50 p-2 rounded">
                Muestra los datos como <strong>Texto Plano</strong> al escanear. Funciona sin internet y sin servidor. Ideal para uso inmediato.
            </p>
        ) : (
             <div className="bg-blue-50 text-blue-800 p-3 rounded-lg text-sm mb-4 flex items-start gap-2 text-left max-w-md mx-auto border border-blue-100">
                <Share2 size={16} className="mt-0.5 flex-shrink-0" />
                <p>
                    <strong>Enlace Inteligente:</strong> Este QR contiene toda la información en el enlace. Cuando subas la app a tu hosting, cualquiera que lo escanee verá el perfil completo <strong>aunque no tengan la base de datos</strong>.
                </p>
            </div>
        )}

        {/* QR Display */}
        <div ref={qrRef} className="flex flex-col items-center justify-center mb-6 p-6 bg-white rounded-xl border border-slate-200 shadow-inner w-fit mx-auto">
          <QRCodeCanvas 
            value={qrValue} 
            size={250} 
            level={"M"}
            includeMargin={true}
            imageSettings={{
                src: "https://cdn-icons-png.flaticon.com/512/1256/1256650.png",
                x: undefined,
                y: undefined,
                height: 30,
                width: 30,
                excavate: true,
            }}
          />
          <div className="mt-2 text-xs font-mono text-gray-400">ID: {user.id.split('-')[0]}</div>
        </div>

        {/* User Summary Mini-Card */}
        <div className="text-left bg-slate-50 p-4 rounded-xl border border-slate-200 flex items-center gap-4 max-w-md mx-auto">
            {user.photo ? (
                <img src={user.photo} alt="Profile" className="w-12 h-12 rounded-full object-cover border border-gray-300" />
            ) : (
                <div className="w-12 h-12 bg-gray-200 rounded-full flex-shrink-0"></div>
            )}
            <div className="overflow-hidden">
                <h3 className="font-bold text-smart-dark truncate">{user.fullName}</h3>
                <p className="text-gray-500 text-xs">Contacto: {user.emergencyPhone}</p>
            </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-lg mx-auto">
        <Button variant="primary" onClick={downloadQR} icon={<Download size={20} />}>
          Descargar Imagen QR
        </Button>
        <Button variant="secondary" onClick={onBackHome} icon={<Home size={20} />}>
          Ir al Inicio
        </Button>
      </div>
      
      <button onClick={() => window.location.href = `?uid=${user.id}`} className="text-gray-400 text-sm underline hover:text-blue-500">
          Ver Perfil (Solo en este dispositivo)
      </button>
    </div>
  );
};
