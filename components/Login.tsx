
import React, { useState } from 'react';
import { Input } from './Input';
import { Button } from './Button';
import { ScanLine, ArrowRight } from 'lucide-react';

interface Props {
  onLoginSuccess: (id: string) => Promise<void>;
  onCredentialLogin: (username: string, password: string) => Promise<void>;
  onCancel: () => void;
}

export const Login: React.FC<Props> = ({ onLoginSuccess, onCredentialLogin, onCancel }) => {
  const [authMode, setAuthMode] = useState<'user' | 'id'>('user');
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [manualId, setManualId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    if (authMode === 'id') {
      if (!manualId.trim()) {
        setError('Ingresa un ID de usuario válido.');
        return;
      }

      setIsSubmitting(true);
      try {
        await onLoginSuccess(manualId.trim());
      } catch (loginError) {
        setError(loginError instanceof Error ? loginError.message : 'No se pudo buscar el perfil.');
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    if (!credentials.username.trim() || !credentials.password) {
      setError('Ingresa tu usuario y contraseña.');
      return;
    }

    setIsSubmitting(true);
    try {
      await onCredentialLogin(credentials.username.trim(), credentials.password);
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : 'No se pudo iniciar sesión.');
    } finally {
      setIsSubmitting(false);
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

                {error && (
                    <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                        {error}
                    </div>
                )}

                <div className="space-y-3 mt-6">
                    <Button fullWidth type="submit" icon={<ArrowRight size={20} />} disabled={isSubmitting}>
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
