import React from 'react';
import { UserData } from '../types';

interface Props {
  user: UserData;
}

export const EmergencyPrintSheet: React.FC<Props> = ({ user }) => (
  <div className="hidden print:block print:p-8 print:text-black">
    <h1 className="text-2xl font-bold mb-2">SmartVest — Ficha de emergencia</h1>
    <p className="text-sm mb-6">Impresa el {new Date().toLocaleString('es-EC')}</p>

    <section className="mb-4">
      <h2 className="font-bold text-lg">Paciente</h2>
      <p>{user.fullName}</p>
      <p>C.I.: {user.nationalId}</p>
      <p>Edad: {user.age} · Sangre: {user.bloodType}</p>
      <p>Dirección: {user.address}</p>
    </section>

    <section className="mb-4">
      <h2 className="font-bold text-lg">Contacto de emergencia</h2>
      <p>{user.emergencyContact.name} ({user.emergencyContact.relationship})</p>
      <p className="text-xl font-bold">{user.emergencyPhone}</p>
    </section>

    <section className="mb-4">
      <h2 className="font-bold text-lg">Observaciones médicas</h2>
      <p>{user.medicalObservations || 'Ninguna'}</p>
    </section>

    <section>
      <h2 className="font-bold text-lg">Dispositivo IoT</h2>
      <p className="font-mono">{user.deviceId || 'VEST-DEMO'}</p>
      <p className="text-sm mt-2">ID perfil: {user.id}</p>
    </section>
  </div>
);
