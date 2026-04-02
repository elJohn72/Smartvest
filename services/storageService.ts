
import { UserData } from '../types';

const STORAGE_KEY = 'smartvest_users';

const getLocalUsers = (): UserData[] => {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored ? JSON.parse(stored) : [];
};

const saveLocalUsers = (users: UserData[]): void => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(users));
};

const upsertLocalUser = (user: UserData): void => {
  const users = getLocalUsers();
  const index = users.findIndex(u => u.id === user.id);

  if (index >= 0) {
    users[index] = user;
  } else {
    users.push(user);
  }

  saveLocalUsers(users);
};

const getApiBasePath = (): string => {
  if (typeof window === 'undefined') {
    return '/api';
  }

  const { pathname } = window.location;

  if (pathname.includes('/dist/')) {
    return `${pathname.split('/dist/')[0]}/api`;
  }

  const [firstSegment] = pathname.split('/').filter(Boolean);
  return firstSegment ? `/${firstSegment}/api` : '/api';
};

const buildApiUrl = (endpoint: string): string => `${getApiBasePath()}/${endpoint}`;

const isSuccessfulResponse = (response: Response): boolean => response.ok;

export const saveUser = async (user: UserData): Promise<void> => {
  try {
    const response = await fetch(buildApiUrl('users.php'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(user),
    });

    if (!isSuccessfulResponse(response)) {
      throw new Error('API unavailable');
    }
  } catch (_error) {
    upsertLocalUser(user);
  }
};

export const getUsers = async (): Promise<UserData[]> => {
  try {
    const response = await fetch(buildApiUrl('users.php'));

    if (!isSuccessfulResponse(response)) {
      throw new Error('API unavailable');
    }

    const payload = await response.json();
    if (Array.isArray(payload.users)) {
      saveLocalUsers(payload.users);
      return payload.users;
    }
  } catch (_error) {
    return getLocalUsers();
  }

  return getLocalUsers();
};

export const getUserById = async (id: string): Promise<UserData | undefined> => {
  try {
    const response = await fetch(`${buildApiUrl('users.php')}?id=${encodeURIComponent(id)}`);

    if (!isSuccessfulResponse(response)) {
      throw new Error('API unavailable');
    }

    const payload = await response.json();
    if (payload.user) {
      upsertLocalUser(payload.user);
      return payload.user;
    }
  } catch (_error) {
    return getLocalUsers().find(u => u.id === id);
  }

  return getLocalUsers().find(u => u.id === id);
};

export const loginUser = async (username: string, password: string): Promise<UserData | undefined> => {
  const response = await fetch(buildApiUrl('users.php'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      action: 'login',
      username,
      password,
    }),
  });

  const payload = await response.json();

  if (!isSuccessfulResponse(response) || !payload.user) {
    throw new Error(payload.message || 'No se pudo iniciar sesión.');
  }

  upsertLocalUser(payload.user);
  return payload.user;
};

export const exportToCSV = async (): Promise<void> => {
  const users = await getUsers();
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
export const exportToJSON = async (): Promise<void> => {
  const users = await getUsers();
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
export const importFromJSON = async (jsonString: string): Promise<{ success: boolean; count: number; message: string }> => {
    try {
        const importedUsers: any[] = JSON.parse(jsonString);
        
        if(!Array.isArray(importedUsers)) {
            return { success: false, count: 0, message: "El archivo no tiene el formato correcto (debe ser una lista)." };
        }

        const currentUsers = await getUsers();
        const userMap = new Map(currentUsers.map(u => [u.id, u]));
        let newCount = 0;

        importedUsers.forEach(u => {
            // Validación básica
            if(u.id && u.fullName && u.nationalId) {
                if (!userMap.has(u.id)) newCount++;
                userMap.set(u.id, u);
            }
        });

        const mergedUsers = Array.from(userMap.values());

        try {
            const response = await fetch(buildApiUrl('users.php'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ action: 'import', users: mergedUsers }),
            });

            if (!isSuccessfulResponse(response)) {
                throw new Error('API unavailable');
            }
        } catch (_error) {
            saveLocalUsers(mergedUsers);
        }

        return { success: true, count: newCount, message: `Importación exitosa. ${newCount} usuarios nuevos añadidos.` };

    } catch (e) {
        console.error("Error importing JSON", e);
        return { success: false, count: 0, message: "Error al leer el archivo. Asegúrate de que sea un JSON válido de SmartVest." };
    }
};
