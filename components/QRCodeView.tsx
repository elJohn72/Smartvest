
import React, { useRef, useState } from 'react';
import { QRCodeCanvas } from 'qrcode.react';
import { UserData } from '../types';
import { Button } from './Button';
import { Download, Home, FileText, Link as LinkIcon, Globe, Share2, Copy, UserCircle } from 'lucide-react';
import { buildAppUrl } from '../utils/buildAppUrl';
import { copyTextToClipboard } from '../utils/clipboard';
import { showToast } from '../services/toastService';

interface Props {
  user: UserData;
  onBackHome: () => void;
  onViewProfile?: () => void;
}

type QrMode = 'text' | 'link' | 'uid';

export const QRCodeView: React.FC<Props> = ({ user, onBackHome, onViewProfile }) => {
  const qrRef = useRef<HTMLDivElement>(null);
  const [mode, setMode] = useState<QrMode>('link');

  const getPublicProfile = () => ({
    id: user.id,
    fullName: user.fullName,
    age: user.age,
    bloodType: user.bloodType,
    emergencyPhone: user.emergencyPhone,
    emergencyContact: user.emergencyContact,
    medicalObservations: user.medicalObservations,
    deviceId: user.deviceId,
    photo: user.photo,
  });

  const getMonitoringUrl = (): string => {
    try {
      const jsonString = JSON.stringify(getPublicProfile());
      const encodedData = btoa(unescape(encodeURIComponent(jsonString)));
      return buildAppUrl({ data: encodedData });
    } catch (error) {
      console.error('Error encoding QR data', error);
      return '';
    }
  };

  const getUidUrl = (): string => buildAppUrl({ uid: user.id });

  const getQRData = (): string => {
    if (mode === 'uid') {
      return getUidUrl();
    }

    if (mode === 'link') {
      return getMonitoringUrl();
    }

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

  const getShareableUrl = (): string => {
    if (mode === 'uid') {
      return getUidUrl();
    }

    if (mode === 'link') {
      return getMonitoringUrl();
    }

    return '';
  };

  const downloadQR = () => {
    const canvas = qrRef.current?.querySelector('canvas');
    if (!canvas) {
      showToast('No se pudo generar la imagen del QR.', 'error');
      return;
    }

    const url = canvas.toDataURL('image/png');
    const link = document.createElement('a');
    link.href = url;
    link.download = `smartvest-${user.fullName.replace(/\s+/g, '-').toLowerCase()}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Imagen QR descargada.', 'success');
  };

  const copyShareLink = async () => {
    const url = getShareableUrl();
    if (!url) {
      showToast('El modo texto no tiene enlace web para copiar.', 'info');
      return;
    }

    const copied = await copyTextToClipboard(url);
    showToast(
      copied ? 'Enlace copiado al portapapeles.' : 'No se pudo copiar el enlace.',
      copied ? 'success' : 'error',
    );
  };

  const qrValue = getQRData();

  return (
    <div className="max-w-2xl mx-auto space-y-6 text-center pb-12">
      <div className="bg-white p-8 rounded-2xl shadow-xl border-2 border-smart-gray">
        
        <div className="flex flex-col items-center mb-6">
            <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-4">
                {mode === 'text' ? <FileText size={32} aria-hidden="true" /> : <Globe size={32} aria-hidden="true" />}
            </div>
            <h2 className="text-3xl font-bold text-smart-dark">Registro exitoso</h2>
            <p className="text-gray-500">El usuario quedó guardado en el sistema.</p>
        </div>
        
        <div className="bg-slate-100 p-1 rounded-lg inline-flex mb-6 flex-wrap justify-center gap-1" role="tablist" aria-label="Tipo de código QR">
            <button
                type="button"
                role="tab"
                aria-selected={mode === 'text'}
                onClick={() => setMode('text')}
                className={`px-3 py-2 rounded-md text-sm font-bold transition-all flex items-center gap-2 ${mode === 'text' ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
                <FileText size={16} aria-hidden="true" /> Texto offline
            </button>
            <button
                type="button"
                role="tab"
                aria-selected={mode === 'link'}
                onClick={() => setMode('link')}
                className={`px-3 py-2 rounded-md text-sm font-bold transition-all flex items-center gap-2 ${mode === 'link' ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
                <LinkIcon size={16} aria-hidden="true" /> Enlace completo
            </button>
            <button
                type="button"
                role="tab"
                aria-selected={mode === 'uid'}
                onClick={() => setMode('uid')}
                className={`px-3 py-2 rounded-md text-sm font-bold transition-all flex items-center gap-2 ${mode === 'uid' ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
                <Share2 size={16} aria-hidden="true" /> Solo ID
            </button>
        </div>

        {mode === 'text' ? (
            <p className="text-sm text-slate-600 mb-4 max-w-md mx-auto bg-slate-50 p-2 rounded">
                Muestra los datos como <strong>texto plano</strong> al escanear. Funciona sin internet.
            </p>
        ) : mode === 'uid' ? (
             <div className="bg-emerald-50 text-emerald-900 p-3 rounded-lg text-sm mb-4 flex items-start gap-2 text-left max-w-md mx-auto border border-emerald-100">
                 <Share2 size={16} className="mt-0.5 flex-shrink-0" aria-hidden="true" />
                 <p>
                    <strong>Enlace mínimo:</strong> Solo abre el perfil si este servidor ya tiene los datos del usuario. QR más pequeño y menos datos en la URL.
                 </p>
             </div>
        ) : (
             <div className="bg-blue-50 text-blue-800 p-3 rounded-lg text-sm mb-4 flex items-start gap-2 text-left max-w-md mx-auto border border-blue-100">
                 <Share2 size={16} className="mt-0.5 flex-shrink-0" aria-hidden="true" />
                 <p>
                    <strong>Enlace portable:</strong> Incluye datos médicos básicos en la URL para monitoreo en otro dispositivo (sin usuario ni contraseña).
                 </p>
             </div>
        )}

        <div ref={qrRef} className="flex flex-col items-center justify-center mb-6 p-6 bg-white rounded-xl border border-slate-200 shadow-inner w-fit mx-auto">
          <QRCodeCanvas 
            value={qrValue} 
            size={mode === 'uid' ? 220 : 250}
            level={mode === 'uid' ? 'L' : 'M'}
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

        <div className="text-left bg-slate-50 p-4 rounded-xl border border-slate-200 flex items-center gap-4 max-w-md mx-auto">
            {user.photo ? (
                <img src={user.photo} alt={user.fullName} className="w-12 h-12 rounded-full object-cover border border-gray-300" />
            ) : (
                <div className="w-12 h-12 bg-gray-200 rounded-full flex-shrink-0" aria-hidden="true"></div>
            )}
            <div className="overflow-hidden min-w-0">
                <h3 className="font-bold text-smart-dark truncate">{user.fullName}</h3>
                <p className="text-gray-500 text-xs">Contacto: {user.emergencyPhone}</p>
                <p className="text-gray-400 text-xs font-mono truncate">Dispositivo: {user.deviceId || 'VEST-DEMO'}</p>
            </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-lg mx-auto">
        <Button variant="primary" onClick={downloadQR} icon={<Download size={20} />}>
          Descargar QR
        </Button>
        {mode !== 'text' && (
          <Button variant="outline" onClick={() => void copyShareLink()} icon={<Copy size={20} />}>
            Copiar enlace
          </Button>
        )}
        <Button variant="secondary" onClick={onBackHome} icon={<Home size={20} />}>
          Ir al inicio
        </Button>
        {onViewProfile && (
          <Button variant="outline" onClick={onViewProfile} icon={<UserCircle size={20} />}>
            Ver perfil y mapa
          </Button>
        )}
      </div>
    </div>
  );
};
