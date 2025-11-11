
import React, { useRef, useState } from 'react';
import { Button } from './Button';
import { UserPlus, FileSpreadsheet, Smartphone, Download, Upload, Database, Server, HelpCircle, X } from 'lucide-react';
import { exportToCSV, exportToJSON, importFromJSON } from '../services/storageService';

interface Props {
  onNewRegister: () => void;
}

export const Home: React.FC<Props> = ({ onNewRegister }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showHostingHelp, setShowHostingHelp] = useState(false);

  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      const result = importFromJSON(content);
      alert(result.message);
      // Reset input
      if (fileInputRef.current) fileInputRef.current.value = '';
    };
    reader.readAsText(file);
  };

  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 max-w-4xl mx-auto w-full">
      
      {/* Hosting Help Modal */}
      {showHostingHelp && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl relative animate-in fade-in zoom-in duration-200">
            <button onClick={() => setShowHostingHelp(false)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
              <X size={24} />
            </button>
            
            <div className="flex items-center gap-3 mb-4 text-blue-700">
               <Server size={32} />
               <h3 className="text-2xl font-bold">Guía para subir a Internet</h3>
            </div>
            
            <div className="space-y-4 text-gray-600">
              <p>
                <strong>1. ¿Te piden un archivo .sql?</strong><br/>
                No te preocupes. Esta app es moderna y no usa bases de datos antiguas. He incluido un archivo <code>database.sql</code> en el proyecto. Si tu hosting te obliga a subir uno, sube ese archivo para que te deje pasar.
              </p>
              <p>
                <strong>2. ¿Cómo subir los archivos?</strong><br/>
                Descarga el proyecto completo (usando el botón de descarga de tu editor). Obtendrás un <strong>.zip</strong>. Sube ese archivo a la carpeta <code>public_html</code> de tu hosting.
              </p>
              <p>
                <strong>3. ¿Cómo guardo los datos?</strong><br/>
                Como es una web estática, los datos viven en el dispositivo. Usa el botón <strong>"Guardar Copia de Seguridad"</strong> (aquí abajo) frecuentemente para no perder nada si cambias de celular o borras el historial.
              </p>
            </div>
            
            <div className="mt-6 flex justify-end">
              <Button onClick={() => setShowHostingHelp(false)}>Entendido</Button>
            </div>
          </div>
        </div>
      )}

      <div className="mb-10 text-center relative">
        <button 
          onClick={() => setShowHostingHelp(true)}
          className="absolute -top-8 right-0 md:-right-12 text-blue-600 hover:text-blue-800 flex items-center gap-1 text-sm font-medium bg-blue-50 px-3 py-1 rounded-full border border-blue-100"
        >
          <HelpCircle size={16} /> Ayuda Hosting
        </button>

        <div className="w-20 h-20 bg-gradient-to-br from-smart-primary to-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-6 shadow-lg shadow-blue-500/30 transform rotate-3">
            <Smartphone size={40} className="text-white" />
        </div>
        <h2 className="text-3xl md:text-4xl font-bold text-smart-dark mb-3">Panel de Gestión</h2>
        <p className="text-lg text-gray-500 max-w-xl mx-auto">
          Administra los registros de usuarios SmartVest y genera identificaciones.
        </p>
      </div>

      <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Columna Principal */}
        <div className="space-y-4">
            <div className="bg-white p-6 rounded-2xl shadow-md border border-slate-100">
                <h3 className="font-bold text-lg mb-4 text-smart-dark flex items-center gap-2">
                    <UserPlus size={20} className="text-blue-500"/> Acciones Principales
                </h3>
                <Button 
                    onClick={onNewRegister} 
                    fullWidth 
                    icon={<UserPlus size={20} />}
                    className="py-4 shadow-xl hover:shadow-2xl transition-all"
                >
                Nuevo Registro
                </Button>
            </div>

             <div className="bg-white p-6 rounded-2xl shadow-md border border-slate-100">
                <h3 className="font-bold text-lg mb-4 text-smart-dark flex items-center gap-2">
                    <FileSpreadsheet size={20} className="text-green-600"/> Reportes
                </h3>
                <Button 
                    variant="outline" 
                    onClick={exportToCSV} 
                    fullWidth
                    icon={<FileSpreadsheet size={20} />}
                    className="border-gray-300 text-gray-700 hover:bg-gray-50"
                >
                Exportar a Excel (CSV)
                </Button>
            </div>
        </div>

        {/* Columna Datos */}
        <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200 h-fit">
             <h3 className="font-bold text-lg mb-4 text-smart-dark flex items-center gap-2">
                <Database size={20} className="text-purple-500"/> Base de Datos Local
            </h3>
            <p className="text-sm text-gray-500 mb-6">
                Los datos se guardan en este dispositivo. Usa estas opciones para mover datos entre tu computadora y tu celular.
            </p>
            
            <div className="space-y-3">
                <Button 
                    variant="secondary" 
                    onClick={exportToJSON} 
                    fullWidth
                    icon={<Download size={18} />}
                    className="bg-white text-smart-dark border border-gray-200 hover:bg-blue-50"
                >
                Guardar Copia de Seguridad
                </Button>

                <Button 
                    variant="secondary" 
                    onClick={handleImportClick} 
                    fullWidth
                    icon={<Upload size={18} />}
                    className="bg-white text-smart-dark border border-gray-200 hover:bg-blue-50"
                >
                Restaurar Copia de Seguridad
                </Button>
                <input 
                    type="file" 
                    ref={fileInputRef} 
                    onChange={handleFileChange} 
                    accept=".json" 
                    className="hidden" 
                />
            </div>
        </div>
      </div>
    </div>
  );
};