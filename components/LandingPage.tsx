
import React from 'react';
import { Button } from './Button';
import { LogIn, UserPlus, Activity, MapPin, Mail, Phone, Map, Users, Heart, Target, Battery, AlertTriangle, Accessibility, Eye } from 'lucide-react';

interface Props {
  onLogin: () => void;
  onRegister: () => void;
}

export const LandingPage: React.FC<Props> = ({ onLogin, onRegister }) => {
  return (
    <div className="flex flex-col w-full">
      {/* Hero Section */}
      <section className="relative bg-smart-dark text-white overflow-hidden pb-16">
        <div className="absolute inset-0 bg-[url('https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=2072&auto=format&fit=crop')] bg-cover bg-center opacity-10"></div>
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-smart-dark/50 to-smart-dark"></div>
        
        <div className="max-w-7xl mx-auto px-4 py-12 md:py-20 relative z-10 flex flex-col md:flex-row items-center gap-12 lg:gap-20">
            <div className="flex-1 text-center md:text-left space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-700">
                <div className="inline-flex items-center gap-2 px-4 py-2 bg-blue-500/10 border border-blue-500/20 rounded-full text-blue-300 text-sm font-medium backdrop-blur-md">
                    <span className="flex h-2 w-2 rounded-full bg-blue-400 animate-pulse"></span>
                    Tecnología de Asistencia Visual v2.0
                </div>
                
                <h1 className="text-5xl md:text-6xl lg:text-7xl font-black leading-tight tracking-tight">
                    Seguridad que se <br/>
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-purple-400 to-blue-400 animate-gradient bg-300%">Siente</span> y Conecta.
                </h1>
                
                <p className="text-xl text-gray-300 max-w-xl mx-auto md:mx-0 leading-relaxed">
                    SmartVest es más que un chaleco; es tu compañero inteligente. Monitoreo en tiempo real, alertas SOS instantáneas y un perfil médico vital accesible con un solo escaneo.
                </p>
                
                <div className="flex flex-col sm:flex-row gap-4 pt-6 justify-center md:justify-start">
                    <Button 
                        onClick={onLogin} 
                        className="!bg-white hover:!bg-gray-100 !text-smart-dark font-bold px-8 py-5 text-lg shadow-lg hover:scale-105 transition-transform border-2 border-white"
                    >
                        <LogIn size={22} className="mr-2 text-smart-primary" /> Iniciar Sesión
                    </Button>
                    <Button 
                        onClick={onRegister} 
                        className="bg-blue-600 hover:bg-blue-700 text-white font-bold px-8 py-5 text-lg shadow-lg hover:scale-105 transition-transform border-2 border-transparent"
                    >
                        <UserPlus size={22} className="mr-2" /> Crear Cuenta
                    </Button>
                </div>
            </div>

            <div className="flex-1 relative flex justify-center items-center perspective-1000 mt-8 md:mt-0">
                {/* Glow Effect behind phone */}
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[320px] h-[600px] bg-blue-600/20 rounded-full blur-3xl animate-pulse pointer-events-none"></div>
                
                {/* iPhone Mockup (CSS) */}
                <div className="relative mx-auto border-gray-800 bg-gray-900 border-[8px] rounded-[3.5rem] h-[650px] w-[320px] shadow-2xl flex flex-col overflow-hidden ring-1 ring-white/10 transform transition-transform duration-500 hover:scale-105">
                    
                    {/* Side Buttons (Titanium style) */}
                    <div className="h-[32px] w-[4px] bg-gray-700 absolute -left-[12px] top-[100px] rounded-s-lg"></div>
                    <div className="h-[50px] w-[4px] bg-gray-700 absolute -left-[12px] top-[150px] rounded-s-lg"></div>
                    <div className="h-[70px] w-[4px] bg-gray-700 absolute -right-[12px] top-[170px] rounded-e-lg"></div>
                    
                    {/* Screen */}
                    <div className="rounded-[3rem] overflow-hidden w-full h-full bg-white relative flex flex-col">
                        
                        {/* Dynamic Island */}
                        <div className="absolute top-3 left-1/2 transform -translate-x-1/2 w-[100px] h-[28px] bg-black rounded-full z-40 flex justify-center items-center">
                             {/* Camera lens dot */}
                             <div className="absolute right-3 w-2 h-2 bg-gray-800/50 rounded-full blur-[1px]"></div>
                        </div>

                        {/* Status Bar */}
                        <div className="h-12 bg-transparent w-full z-30 flex items-center justify-between px-8 pt-3">
                             <span className="text-xs text-slate-900 font-semibold pl-2">{new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                             <div className="flex gap-1.5 pr-1">
                                 <div className="w-4 h-2.5 rounded-[2px] border border-slate-900 relative">
                                     <div className="bg-slate-900 h-full w-[92%] absolute left-0 top-0"></div>
                                 </div>
                             </div>
                        </div>

                        {/* App Header inside Phone */}
                        <div className="pt-2 pb-4 px-6 bg-white z-10">
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full bg-gray-200 overflow-hidden border border-gray-200 ring-2 ring-blue-100">
                                        <img src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=1000&auto=format&fit=crop" alt="User" className="w-full h-full object-cover"/>
                                    </div>
                                    <div>
                                        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mb-0.5">Usuario</p>
                                        <h2 className="text-lg font-bold text-slate-900 leading-tight">Juan Pérez</h2>
                                    </div>
                                </div>
                                <div className="flex flex-col items-end">
                                    <div className="flex items-center gap-1 text-green-600 bg-green-50 px-2 py-0.5 rounded-md border border-green-100 shadow-sm">
                                        <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
                                        <span className="text-[10px] font-bold uppercase">Activo</span>
                                    </div>
                                    <div className="flex items-center gap-1 mt-1 text-gray-500">
                                        <Battery size={12} className="text-slate-800"/>
                                        <span className="text-[10px] font-mono font-bold">92%</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* App Content */}
                        <div className="flex-1 overflow-hidden px-5 pb-4 space-y-4 relative bg-slate-50/50 rounded-t-3xl border-t border-gray-100 pt-6">

                            {/* SOS Alert Card */}
                            <div className="bg-red-500 text-white p-4 rounded-2xl shadow-lg shadow-red-500/30 animate-pulse border border-red-400 relative overflow-hidden">
                                <div className="absolute top-0 right-0 w-20 h-20 bg-white/10 rounded-full blur-2xl"></div>
                                <div className="flex items-start gap-3 mb-2 relative z-10">
                                    <div className="bg-white/20 p-2 rounded-full">
                                         <AlertTriangle size={24} className="text-white" />
                                    </div>
                                    <div>
                                        <h3 className="font-black text-lg tracking-tight leading-none mb-1">¡ALERTA SOS!</h3>
                                        <p className="text-xs text-white/90 font-medium">Botón de pánico presionado.</p>
                                    </div>
                                </div>
                                <div className="mt-3 bg-white/20 rounded-lg p-2 text-center text-xs font-bold cursor-pointer hover:bg-white/30 transition-colors flex items-center justify-center gap-2">
                                    <Phone size={12} /> Contactar Emergencia
                                </div>
                            </div>

                            {/* Location Card */}
                            <div className="bg-white p-3 rounded-2xl shadow-sm border border-slate-100">
                                <div className="flex items-center justify-between mb-2 px-1">
                                    <div className="flex items-center gap-1.5">
                                        <MapPin size={14} className="text-blue-500" />
                                        <span className="text-xs font-bold text-slate-700">Última Ubicación</span>
                                    </div>
                                    <span className="text-[10px] text-gray-400 flex items-center gap-1">
                                        <Activity size={10} /> En tiempo real
                                    </span>
                                </div>
                                <div className="h-40 w-full bg-slate-100 rounded-xl overflow-hidden relative border border-slate-200">
                                    <div className="absolute inset-0 opacity-80 bg-[url('https://www.google.com/maps/vt/data=LyN1d0o4WjFhWlF3')] bg-cover bg-center scale-150"></div>
                                    {/* Radar effect */}
                                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-32 h-32 bg-red-500/10 rounded-full animate-ping duration-1000"></div>
                                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-4 h-4 bg-red-500 rounded-full border-2 border-white shadow-md z-10 ring-4 ring-red-500/20"></div>
                                    
                                    {/* Address Tag */}
                                    <div className="absolute bottom-2 left-2 right-2 bg-white/95 backdrop-blur-sm p-2 rounded-lg shadow-lg border border-gray-100 flex items-center gap-2">
                                        <div className="bg-red-50 p-1.5 rounded text-red-500">
                                            <MapPin size={12} />
                                        </div>
                                        <div className="min-w-0">
                                            <p className="text-[10px] font-bold text-slate-800 truncate">Av. Amazonas y Naciones Unidas</p>
                                            <p className="text-[9px] text-gray-500 truncate">Quito, Ecuador</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Home Indicator */}
                        <div className="absolute bottom-2 left-1/2 transform -translate-x-1/2 w-32 h-1.5 bg-black rounded-full z-50"></div>
                    </div>
                </div>
            </div>
        </div>
      </section>

      {/* Features Strip */}
      <section className="bg-white py-20 border-b border-gray-100 relative z-20 -mt-12 rounded-t-[3rem] shadow-[0_-20px_40px_rgba(0,0,0,0.1)]">
        <div className="max-w-6xl mx-auto px-4 grid grid-cols-1 md:grid-cols-3 gap-8 text-center">
            {/* Card 1: Autonomía */}
            <div className="p-8 rounded-3xl bg-slate-50 hover:bg-white hover:shadow-xl transition-all duration-300 border border-transparent hover:border-blue-100 group">
                <div className="w-16 h-16 bg-blue-100 group-hover:bg-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-6 text-blue-600 group-hover:text-white transition-colors duration-300">
                    <Accessibility size={28} />
                </div>
                <h3 className="font-bold text-xl text-smart-dark mb-3">Autonomía e Independencia</h3>
                <p className="text-gray-500 leading-relaxed">
                    Dispositivo IoT diseñado para devolver la confianza y libertad de movimiento, mejorando la autosuficiencia diaria de personas con discapacidad visual.
                </p>
            </div>
            
            {/* Card 2: Vision IA */}
            <div className="p-8 rounded-3xl bg-slate-50 hover:bg-white hover:shadow-xl transition-all duration-300 border border-transparent hover:border-purple-100 group">
                <div className="w-16 h-16 bg-purple-100 group-hover:bg-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-6 text-purple-600 group-hover:text-white transition-colors duration-300">
                    <Eye size={28} />
                </div>
                <h3 className="font-bold text-xl text-smart-dark mb-3">Visión Artificial IA</h3>
                <p className="text-gray-500 leading-relaxed">
                    Asistente visual inteligente que analiza el entorno en tiempo real para identificar objetos y peligros, guiando al usuario de forma segura.
                </p>
            </div>
            
            {/* Card 3: Localización */}
            <div className="p-8 rounded-3xl bg-slate-50 hover:bg-white hover:shadow-xl transition-all duration-300 border border-transparent hover:border-orange-100 group">
                <div className="w-16 h-16 bg-orange-100 group-hover:bg-orange-600 rounded-2xl flex items-center justify-center mx-auto mb-6 text-orange-600 group-hover:text-white transition-colors duration-300">
                    <MapPin size={28} />
                </div>
                <h3 className="font-bold text-xl text-smart-dark mb-3">Localización Precisa</h3>
                <p className="text-gray-500 leading-relaxed">
                    GPS de alta precisión y conectividad constante para compartir ubicación y estado en tiempo real con familiares y cuidadores.
                </p>
            </div>
        </div>
      </section>

      {/* Nosotros (About Us) Section - Integrated */}
      <section className="bg-smart-light py-20">
        <div className="max-w-6xl mx-auto px-4">
            <div className="flex flex-col md:flex-row items-center gap-16">
                <div className="flex-1 space-y-6">
                    <div className="inline-flex items-center gap-2 text-blue-600 font-bold uppercase tracking-wider text-sm">
                        <Users size={16} /> Sobre Nosotros
                    </div>
                    <h2 className="text-4xl font-black text-smart-dark">Empoderando la independencia a través de la tecnología</h2>
                    <p className="text-lg text-gray-600 leading-relaxed">
                        SmartVest nace con la misión de romper barreras. Combinamos hardware IoT de última generación con una plataforma de software accesible para brindar seguridad, confianza y libertad a personas con discapacidad visual.
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-4">
                        <div className="flex gap-4">
                            <div className="bg-blue-100 p-3 rounded-lg text-blue-600 h-min"><Target size={24}/></div>
                            <div>
                                <h4 className="font-bold text-smart-dark">Misión</h4>
                                <p className="text-sm text-gray-500 mt-1">Reducir accidentes y mejorar la respuesta ante emergencias.</p>
                            </div>
                        </div>
                        <div className="flex gap-4">
                            <div className="bg-red-100 p-3 rounded-lg text-red-600 h-min"><Heart size={24}/></div>
                            <div>
                                <h4 className="font-bold text-smart-dark">Visión</h4>
                                <p className="text-sm text-gray-500 mt-1">Un mundo donde la discapacidad no limite la movilidad segura.</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="flex-1 relative">
                    <div className="absolute inset-0 bg-gradient-to-tr from-blue-600 to-purple-600 rounded-2xl transform rotate-3 opacity-20"></div>
                    {/* 
                        NOTE: This image is a placeholder resembling the provided device user.
                        Replace 'https://images.unsplash.com/...' with your actual device image file path.
                    */}
                    <img 
                        src="https://images.unsplash.com/photo-1548543604-a87c9909abec?q=80&w=1978&auto=format&fit=crop" 
                        alt="Usuario SmartVest con dispositivo" 
                        className="relative rounded-2xl shadow-2xl border-4 border-white w-full h-[500px] object-cover"
                    />
                </div>
            </div>
        </div>
      </section>

      {/* Contacto (Contact) Section - Integrated */}
      <section className="bg-white py-20 border-t border-gray-200">
        <div className="max-w-4xl mx-auto px-4 text-center">
            <h2 className="text-3xl font-bold text-smart-dark mb-4">¿Tienes dudas o sugerencias?</h2>
            <p className="text-gray-500 mb-12 max-w-2xl mx-auto">Estamos aquí para escucharte. Contáctanos para soporte técnico, ventas o información sobre el proyecto.</p>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                <div className="flex flex-col items-center p-6 bg-slate-50 rounded-2xl border border-slate-100 hover:shadow-lg transition-shadow">
                    <div className="w-12 h-12 bg-blue-600 text-white rounded-full flex items-center justify-center mb-4">
                        <Mail size={20} />
                    </div>
                    <h3 className="font-bold text-gray-900">Email</h3>
                    <p className="text-blue-600 mt-2 text-sm font-medium">contacto@smartvest.app</p>
                </div>
                
                <div className="flex flex-col items-center p-6 bg-slate-50 rounded-2xl border border-slate-100 hover:shadow-lg transition-shadow">
                    <div className="w-12 h-12 bg-green-600 text-white rounded-full flex items-center justify-center mb-4">
                        <Phone size={20} />
                    </div>
                    <h3 className="font-bold text-gray-900">Teléfono</h3>
                    <p className="text-gray-600 mt-2 text-sm">098726671</p>
                </div>

                <div className="flex flex-col items-center p-6 bg-slate-50 rounded-2xl border border-slate-100 hover:shadow-lg transition-shadow">
                    <div className="w-12 h-12 bg-purple-600 text-white rounded-full flex items-center justify-center mb-4">
                        <Map size={20} />
                    </div>
                    <h3 className="font-bold text-gray-900">Matriz</h3>
                    <p className="text-gray-600 mt-2 text-sm">Quinindé Unidad Educativa Andrés Bello</p>
                </div>
            </div>
        </div>
      </section>
    </div>
  );
};
