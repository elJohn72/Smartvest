export interface RegistrationFormValues {
  fullName: string;
  nationalId: string;
  age: string;
  address: string;
  emergencyPhone: string;
  contactName: string;
  contactRelation: string;
  username: string;
  password: string;
}

export const validateRegistrationForm = (values: RegistrationFormValues): string | null => {
  if (!values.fullName.trim() || values.fullName.trim().length < 3) {
    return 'Ingresa el nombre completo (mínimo 3 caracteres).';
  }

  if (!values.nationalId.trim()) {
    return 'La cédula o identificación es obligatoria.';
  }

  const age = parseInt(values.age, 10);
  if (!Number.isFinite(age) || age < 1 || age > 120) {
    return 'Ingresa una edad válida entre 1 y 120.';
  }

  if (!values.address.trim()) {
    return 'La dirección domiciliaria es obligatoria.';
  }

  const phoneDigits = values.emergencyPhone.replace(/\D/g, '');
  if (phoneDigits.length < 9) {
    return 'El teléfono de emergencia debe tener al menos 9 dígitos.';
  }

  if (!values.contactName.trim() || !values.contactRelation.trim()) {
    return 'Completa el nombre y parentesco del contacto de emergencia.';
  }

  if (!values.username.trim() || values.username.trim().length < 4) {
    return 'El usuario debe tener al menos 4 caracteres.';
  }

  if (!values.password || values.password.length < 6) {
    return 'La contraseña debe tener al menos 6 caracteres.';
  }

  return null;
};

/** Edición: contraseña opcional (vacía = no cambiar en servidor). */
export const validateProfileEditForm = (values: RegistrationFormValues): string | null => {
  if (!values.fullName.trim() || values.fullName.trim().length < 3) {
    return 'Ingresa el nombre completo (mínimo 3 caracteres).';
  }

  if (!values.nationalId.trim()) {
    return 'La cédula o identificación es obligatoria.';
  }

  const age = parseInt(values.age, 10);
  if (!Number.isFinite(age) || age < 1 || age > 120) {
    return 'Ingresa una edad válida entre 1 y 120.';
  }

  if (!values.address.trim()) {
    return 'La dirección domiciliaria es obligatoria.';
  }

  const phoneDigits = values.emergencyPhone.replace(/\D/g, '');
  if (phoneDigits.length < 9) {
    return 'El teléfono de emergencia debe tener al menos 9 dígitos.';
  }

  if (!values.contactName.trim() || !values.contactRelation.trim()) {
    return 'Completa el nombre y parentesco del contacto de emergencia.';
  }

  if (!values.username.trim() || values.username.trim().length < 4) {
    return 'El usuario debe tener al menos 4 caracteres.';
  }

  if (values.password.length > 0 && values.password.length < 6) {
    return 'La contraseña nueva debe tener al menos 6 caracteres (o déjala vacía).';
  }

  return null;
};
