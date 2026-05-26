import React from 'react';
import { UserData } from '../types';

interface Props {
  user: UserData;
}

export const EmergencyPrintSheet: React.FC<Props> = ({ user }) => (
  <div
    id="smartvest-print-sheet"
    className="hidden"
    aria-hidden="true"
  >
    <h1 className="text-2xl font-bold mb-2">SmartVest — Ficha de emergencia</h1>
    <p className="text-sm mb-6">Impresa el {new Date().toLocaleString('es-EC')}</p>

    {user.photo && (
      <div className="mb-4">
        <img
          src={user.photo}
          alt=""
          className="w-28 h-28 rounded-lg object-cover border border-gray-300"
        />
      </div>
    )}

    <section className="mb-4">
      <h2 className="font-bold text-lg border-b border-gray-400 pb-1 mb-2">Persona usuaria del chaleco</h2>
      <p><strong>Nombre:</strong> {user.fullName}</p>
      <p><strong>C.I.:</strong> {user.nationalId}</p>
      <p><strong>Edad:</strong> {user.age} años</p>
      <p><strong>Tipo de sangre:</strong> {user.bloodType}</p>
      <p><strong>Dirección:</strong> {user.address || '—'}</p>
      {user.username && <p><strong>Usuario sistema:</strong> {user.username}</p>}
    </section>

    <section className="mb-4">
      <h2 className="font-bold text-lg border-b border-gray-400 pb-1 mb-2">Contacto de emergencia</h2>
      <p><strong>Nombre:</strong> {user.emergencyContact.name}</p>
      <p><strong>Parentesco:</strong> {user.emergencyContact.relationship}</p>
      <p><strong>Teléfono:</strong> {user.emergencyPhone}</p>
    </section>

    <section className="mb-4">
      <h2 className="font-bold text-lg border-b border-gray-400 pb-1 mb-2">Observaciones médicas</h2>
      <p className="whitespace-pre-wrap">{user.medicalObservations || 'Ninguna registrada'}</p>
    </section>

    <section>
      <h2 className="font-bold text-lg border-b border-gray-400 pb-1 mb-2">Dispositivo</h2>
      <p><strong>ID chaleco:</strong> {user.deviceId || 'VEST-DEMO'}</p>
      <p className="text-sm mt-2 text-gray-600">ID perfil: {user.id}</p>
    </section>
  </div>
);
