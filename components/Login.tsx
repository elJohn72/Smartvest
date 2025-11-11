
import React, { useState } from 'react';
import { Input } from './Input';
import { Button } from './Button';
import { LogIn, ScanLine, ArrowRight } from 'lucide-react';

interface Props {
  onLoginSuccess: (id: string) => void;
  onCancel: () => void;
}

export const Login: React.FC<Props> = ({ onLoginSuccess, onCancel }) => {
  const [authMode, setAuthMode] = useState<'user' | 'id'>('user');
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [manualId, setManualId] = useState('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    // In a real app, verify credentials against a DB. 
    // For this local demo, we'll ask for the User ID directly to simulate the "Scan" or "Login"
    // Since we can't easily query LocalStorage by username/pass without indexing, 
    // we will assume for this demo the user knows their ID or we mock it.
    
    if (authMode === 'id') {
        if (manualId) onLoginSuccess(manualId);
    } else {
        alert("En esta demo local, por favor ingresa usando el ID del QR o el ID que generaste (visible en el CSV). O usa la opción 'Escanear ID' para simular.");
    }
  };

  return (
    <div className="flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden border border-gray-100">
        <div className="bg-smart-dark p-6 text-white text-center">
            <h2 className="text-2xl font-bold">Bienvenido de nuevo</h2>
            <p className="text-gray-400 text-sm mt-1">Accede a tu perfil SmartVest</p>
        </div>

        <div className="p-8">
            <div className="flex gap-2 mb-6 bg-gray-100 p-1 rounded-lg">
                <button 
                    onClick={() => setAuthMode('user')}
                    className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${authMode === 'user' ? 'bg-white shadow text-smart-dark' : 'text-gray-500'}`}
                >
                    Usuario
                </button>
                <button 
                    onClick={() => setAuthMode('id')}
                    className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${authMode === 'id' ? 'bg-white shadow text-smart-dark' : 'text-gray-500'}`}
                >
                    ID / QR Code
                </button>
            </div>

            <form onSubmit={handleLogin}>
                {authMode === 'user' ? (
                    <>
                        <Input 
                            label="Usuario" 
                            value={credentials.username} 
                            onChange={e => setCredentials({...credentials, username: e.target.value})}
                            placeholder="Ingresa tu usuario"
                        />
                        <Input 
                            label="Contraseña" 
                            type="password"
                            value={credentials.password} 
                            onChange={e => setCredentials({...credentials, password: e.target.value})}
                            placeholder="••••••••"
                        />
                    </>
                ) : (
                    <div className="mb-4">
                        <div className="bg-blue-50 p-4 rounded-lg border border-blue-100 mb-4 flex items-start gap-3">
                            <ScanLine className="text-blue-500 mt-1 flex-shrink-0" />
                            <p className="text-sm text-blue-800">
                                Si la cámara no funciona, ingresa el <strong>ID del Usuario</strong> que aparece bajo el código QR.
                            </p>
                        </div>
                        <Input 
                            label="ID de Usuario (UUID)" 
                            value={manualId} 
                            onChange={e => setManualId(e.target.value)}
                            placeholder="Ej. 550e8400-e29b..."
                        />
                    </div>
                )}

                <div className="space-y-3 mt-6">
                    <Button fullWidth type="submit" icon={<ArrowRight size={20} />}>
                        {authMode === 'user' ? 'Ingresar' : 'Buscar Perfil'}
                    </Button>
                    <Button fullWidth type="button" variant="secondary" onClick={onCancel}>
                        Volver
                    </Button>
                </div>
            </form>
        </div>
      </div>
    </div>
  );
};
