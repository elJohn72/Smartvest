
import React, { useState } from 'react';
import { Input } from './Input';
import { Button } from './Button';
import { UserData } from '../types';
import { verifyAddressWithGemini } from '../services/geminiService';
import { showToast } from '../services/toastService';
import { validateRegistrationForm } from '../utils/validateRegistration';
import { MapPin, CheckCircle, Loader2, Camera, Upload, Cpu, AlertCircle } from 'lucide-react';

interface Props {
  onSubmit: (data: UserData) => void;
  onCancel: () => void;
}

export const RegistrationForm: React.FC<Props> = ({ onSubmit, onCancel }) => {
  const [loadingAddress, setLoadingAddress] = useState(false);
  const [addressVerified, setAddressVerified] = useState(false);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [addressNotice, setAddressNotice] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  
  const [formData, setFormData] = useState({
    fullName: '',
    nationalId: '',
    age: '',
    bloodType: 'O+',
    address: '',
    emergencyPhone: '',
    contactName: '',
    contactRelation: '',
    observations: '',
    username: '',
    password: '',
    deviceId: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (name === 'address') setAddressVerified(false);
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setPhotoError(null);

    if (file) {
      // Validate file size (max 2MB to preserve LocalStorage limits)
      if (file.size > 2 * 1024 * 1024) {
        setPhotoError("La imagen es demasiado pesada (Max 2MB). Por favor elige una más pequeña.");
        return;
      }

      const reader = new FileReader();
      reader.onloadend = () => {
        setPhotoPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleVerifyAddress = async () => {
    if (!formData.address) return;
    
    setLoadingAddress(true);
    setAddressNotice(null);
    const verified = await verifyAddressWithGemini(formData.address);
    setLoadingAddress(false);
    
    if (verified) {
        setFormData(prev => ({ ...prev, address: verified }));
        setAddressVerified(true);
        setAddressNotice('Dirección verificada y actualizada.');
        showToast('Dirección verificada y actualizada.', 'success');
    } else {
        setAddressNotice('No pudimos verificar la dirección con el servidor. Puedes continuar con la que ingresaste.');
        showToast('No se pudo verificar la dirección. Puedes continuar igual.', 'info');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const validationError = validateRegistrationForm(formData);
    if (validationError) {
      setFormError(validationError);
      showToast(validationError, 'error');
      return;
    }
    
    const newUser: UserData = {
      id: crypto.randomUUID(),
      fullName: formData.fullName,
      nationalId: formData.nationalId,
      age: parseInt(formData.age),
      bloodType: formData.bloodType,
      address: formData.address,
      emergencyPhone: formData.emergencyPhone,
      emergencyContact: {
        name: formData.contactName,
        relationship: formData.contactRelation,
        phone: formData.emergencyPhone 
      },
      medicalObservations: formData.observations || 'Ninguna',
      createdAt: new Date().toISOString(),
      photo: photoPreview || undefined,
      username: formData.username,
      password: formData.password,
      deviceId: formData.deviceId || `VEST-${Math.floor(Math.random()*1000)}`
    };

    onSubmit(newUser);
  };

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl mx-auto bg-white p-6 rounded-xl shadow-sm border border-slate-200">
      <h2 className="text-3xl font-bold text-smart-dark mb-8 border-b pb-4">Nuevo Registro</h2>

      {formError && (
        <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert" aria-live="polite">
          {formError}
        </div>
      )}
      
      <div className="mb-8 flex flex-col items-center justify-center">
        <div className="relative w-32 h-32 mb-4 group">
          {photoPreview ? (
            <img src={photoPreview} alt="Preview" className="w-full h-full object-cover rounded-full border-4 border-smart-primary shadow-md" />
          ) : (
            <div className="w-full h-full bg-smart-light rounded-full border-4 border-dashed border-smart-gray flex items-center justify-center text-gray-400">
              <Camera size={40} />
            </div>
          )}
          <label className="absolute bottom-0 right-0 bg-smart-primary text-white p-2 rounded-full cursor-pointer shadow-lg hover:bg-blue-700 transition-colors">
            <Upload size={20} />
            <input type="file" accept="image/*" onChange={handleImageUpload} className="hidden" />
          </label>
        </div>
        <span className="text-sm text-gray-500 font-medium">Subir Foto de Perfil</span>
        {photoError && (
            <p className="text-red-500 text-xs mt-2 flex items-center gap-1">
                <AlertCircle size={12} /> {photoError}
            </p>
        )}
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <Input 
          label="Nombres y Apellidos" 
          name="fullName" 
          value={formData.fullName} 
          onChange={handleChange} 
          placeholder="Ej. Juan Pérez"
          required 
        />
        
        <Input 
          label="Cédula / ID" 
          name="nationalId" 
          value={formData.nationalId} 
          onChange={handleChange} 
          placeholder="Ej. 1234567890"
          required 
        />
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <Input 
          label="Edad" 
          name="age" 
          type="number" 
          value={formData.age} 
          onChange={handleChange} 
          placeholder="Ej. 45"
          required 
        />
        
        <div className="mb-6">
            <label className="block text-smart-dark font-bold text-lg mb-2">Tipo de Sangre</label>
            <select 
                name="bloodType" 
                value={formData.bloodType} 
                onChange={handleChange}
                className="w-full p-4 text-lg bg-white border-2 border-smart-gray rounded-lg text-smart-dark focus:border-smart-primary focus:ring-2 focus:ring-blue-200 focus-visible:outline-none"
            >
                {['O+', 'O-', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-'].map(type => (
                    <option key={type} value={type}>{type}</option>
                ))}
            </select>
        </div>
      </div>

      <Input 
        label="Dirección Domiciliaria" 
        name="address" 
        value={formData.address} 
        onChange={handleChange} 
        placeholder="Calle Principal, Número, Ciudad"
        required
        rightAction={
            <button 
                type="button"
                onClick={handleVerifyAddress}
                disabled={loadingAddress || !formData.address}
                className={`h-full px-4 rounded-r-md border-y-2 border-r-2 font-semibold transition-colors flex items-center gap-2
                    ${addressVerified 
                        ? 'bg-green-100 text-green-800 border-green-200' 
                        : 'bg-blue-50 text-blue-700 border-smart-gray hover:bg-blue-100'
                    }`}
                title="Verificar dirección con IA"
            >
                {loadingAddress ? <Loader2 className="animate-spin" /> : (addressVerified ? <CheckCircle /> : <MapPin />)}
                <span className="hidden sm:inline">{addressVerified ? 'Verificado' : 'Validar'}</span>
            </button>
        }
      />

      {addressNotice && (
        <p className="-mt-4 mb-6 text-sm text-blue-800 bg-blue-50 border border-blue-100 rounded-lg px-4 py-2" role="status" aria-live="polite">
          {addressNotice}
        </p>
      )}

      <div className="bg-blue-50 p-4 rounded-lg mb-6 border-l-4 border-smart-primary">
        <h3 className="font-bold text-lg text-smart-primary mb-4">Datos de Emergencia</h3>
        
        <Input 
          label="Teléfono de Emergencia" 
          name="emergencyPhone" 
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          value={formData.emergencyPhone} 
          onChange={handleChange} 
          placeholder="Ej. 0991234567"
          required 
        />

        <div className="grid md:grid-cols-2 gap-4">
            <Input 
              label="Nombre del Contacto" 
              name="contactName" 
              value={formData.contactName} 
              onChange={handleChange} 
              placeholder="Ej. María Pérez"
              required 
            />
            <Input 
              label="Parentesco" 
              name="contactRelation" 
              value={formData.contactRelation} 
              onChange={handleChange} 
              placeholder="Ej. Hermana"
              required 
            />
        </div>
      </div>

      <Input 
        label="Observaciones Médicas" 
        name="observations" 
        value={formData.observations} 
        onChange={handleChange} 
        placeholder="Alergias, condiciones crónicas, medicamentos..."
        isTextArea
      />

      <div className="bg-slate-50 p-4 rounded-lg mb-6 border border-smart-gray">
        <h3 className="font-bold text-lg text-smart-dark mb-4 flex items-center gap-2">
             <Cpu size={20} /> Configuración del Dispositivo
        </h3>
        <Input 
            label="ID del Dispositivo SmartVest" 
            name="deviceId" 
            value={formData.deviceId} 
            onChange={handleChange} 
            placeholder="Ej. VEST-001"
          />
        <div className="grid md:grid-cols-2 gap-4 mt-4">
          <Input 
            label="Usuario / Email" 
            name="username" 
            value={formData.username} 
            onChange={handleChange} 
            placeholder="Usuario para el sistema"
            autoComplete="username"
            spellCheck={false}
            required
          />
          <Input 
            label="Contraseña" 
            name="password" 
            type="password"
            value={formData.password} 
            onChange={handleChange} 
            placeholder="Mínimo 6 caracteres"
            autoComplete="new-password"
            spellCheck={false}
            required
          />
        </div>
      </div>

      <div className="flex flex-col-reverse md:flex-row gap-4 mt-8">
        <Button type="button" variant="secondary" onClick={onCancel} fullWidth>
            Cancelar
        </Button>
        <Button type="submit" variant="primary" fullWidth>
            Generar Perfil y QR
        </Button>
      </div>
    </form>
  );
};
