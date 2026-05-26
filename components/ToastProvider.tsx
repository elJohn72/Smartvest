import React, { useEffect, useState } from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { subscribeToToasts, ToastMessage } from '../services/toastService';

const icons = {
  success: CheckCircle,
  error: AlertCircle,
  info: Info,
};

const styles = {
  success: 'border-green-200 bg-green-50 text-green-900',
  error: 'border-red-200 bg-red-50 text-red-900',
  info: 'border-blue-200 bg-blue-50 text-blue-900',
};

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    return subscribeToToasts(toast => {
      setToasts(current => [...current, toast]);

      window.setTimeout(() => {
        setToasts(current => current.filter(item => item.id !== toast.id));
      }, 4500);
    });
  }, []);

  const dismiss = (id: string) => {
    setToasts(current => current.filter(item => item.id !== id));
  };

  return (
    <>
      {children}
      <div
        className="fixed bottom-6 right-4 z-[200] flex max-w-sm flex-col gap-2 pointer-events-none"
        aria-live="polite"
        aria-relevant="additions"
      >
        {toasts.map(toast => {
          const Icon = icons[toast.type];
          return (
            <div
              key={toast.id}
              role="status"
              className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg ${styles[toast.type]}`}
            >
              <Icon size={20} className="flex-shrink-0 mt-0.5" aria-hidden="true" />
              <p className="flex-1 font-medium">{toast.message}</p>
              <button
                type="button"
                onClick={() => dismiss(toast.id)}
                className="rounded p-1 opacity-70 hover:opacity-100"
                aria-label="Cerrar aviso"
              >
                <X size={16} aria-hidden="true" />
              </button>
            </div>
          );
        })}
      </div>
    </>
  );
};
