
import { UserData } from '../types';

const STORAGE_KEY = 'smartvest_users';

export const saveUser = (user: UserData): void => {
  const users = getUsers();
  // Check if user exists, update if so, otherwise push
  const index = users.findIndex(u => u.id === user.id);
  if (index >= 0) {
    users[index] = user;
  } else {
    users.push(user);
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(users));
};

export const getUsers = (): UserData[] => {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored ? JSON.parse(stored) : [];
};

export const getUserById = (id: string): UserData | undefined => {
  const users = getUsers();
  return users.find(u => u.id === id);
};

export const exportToCSV = (): void => {
  const users = getUsers();
  if (users.length === 0) {
    alert("No hay registros para exportar.");
    return;
  }

  const headers = ['ID', 'Nombre', 'Cédula', 'Edad', 'Tipo Sangre', 'Dirección', 'Tel. Emergencia', 'Contacto', 'Parentesco', 'Observaciones', 'Usuario'];
  
  const rows = users.map(u => [
    u.id,
    `"${u.fullName}"`,
    `"${u.nationalId}"`,
    u.age,
    u.bloodType,
    `"${u.address}"`,
    u.emergencyPhone,
    `"${u.emergencyContact.name}"`,
    `"${u.emergencyContact.relationship}"`,
    `"${u.medicalObservations}"`,
    `"${u.username || ''}"`
  ]);

  const csvContent = [
    headers.join(','),
    ...rows.map(row => row.join(','))
  ].join('\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  
  link.setAttribute('href', url);
  link.setAttribute('download', `smartvest_registros_${new Date().toISOString().slice(0,10)}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// Función para guardar respaldo completo (JSON) que se puede restaurar
export const exportToJSON = (): void => {
  const users = getUsers();
  if (users.length === 0) {
    alert("No hay datos para realizar una copia de seguridad.");
    return;
  }
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(users, null, 2));
  const link = document.createElement('a');
  link.setAttribute("href", dataStr);
  link.setAttribute("download", "smartvest_backup_" + new Date().toISOString().slice(0,10) + ".json");
  document.body.appendChild(link);
  link.click();
  link.remove();
};

// Función para restaurar datos desde un archivo JSON
export const importFromJSON = (jsonString: string): { success: boolean; count: number; message: string } => {
    try {
        const importedUsers: any[] = JSON.parse(jsonString);
        
        if(!Array.isArray(importedUsers)) {
            return { success: false, count: 0, message: "El archivo no tiene el formato correcto (debe ser una lista)." };
        }

        const currentUsers = getUsers();
        const userMap = new Map(currentUsers.map(u => [u.id, u]));
        let newCount = 0;

        importedUsers.forEach(u => {
            // Validación básica
            if(u.id && u.fullName && u.nationalId) {
                if (!userMap.has(u.id)) newCount++;
                userMap.set(u.id, u);
            }
        });
        
        localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(userMap.values())));
        return { success: true, count: newCount, message: `Importación exitosa. ${newCount} usuarios nuevos añadidos.` };

    } catch (e) {
        console.error("Error importing JSON", e);
        return { success: false, count: 0, message: "Error al leer el archivo. Asegúrate de que sea un JSON válido de SmartVest." };
    }
};
