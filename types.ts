
export interface EmergencyContact {
  name: string;
  relationship: string;
  phone: string;
}

export interface UserData {
  id: string; // UUID
  fullName: string;
  nationalId: string;
  age: number;
  bloodType: string;
  address: string;
  emergencyPhone: string;
  emergencyContact: EmergencyContact;
  medicalObservations: string;
  createdAt: string;
  // New fields
  photo?: string; // Base64 string
  username?: string;
  password?: string;
  deviceId?: string; // ID of the Device
}

export interface IotData {
  deviceId: string;
  latitude: number;
  longitude: number;
  sosActive: boolean;
  lastUpdate: number;
  batteryLevel?: number;
}

export enum AppScreen {
  LANDING = 'LANDING',
  LOGIN = 'LOGIN',
  REGISTER = 'REGISTER',
  QR_VIEW = 'QR_VIEW',
  PROFILE = 'PROFILE'
}
