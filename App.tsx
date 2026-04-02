
import React, { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { LandingPage } from './components/LandingPage';
import { RegistrationForm } from './components/RegistrationForm';
import { QRCodeView } from './components/QRCodeView';
import { UserProfile } from './components/UserProfile';
import { Login } from './components/Login';
import { Button } from './components/Button';
import { AppScreen, UserData } from './types';
import { saveUser, getUserById, loginUser } from './services/storageService';
import './services/iotService'; 

const createImportedUser = (payload: Partial<UserData>): UserData | null => {
  if (
    !payload.id ||
    !payload.fullName ||
    typeof payload.age !== 'number' ||
    !payload.bloodType ||
    !payload.emergencyPhone ||
    !payload.emergencyContact?.name ||
    !payload.emergencyContact?.relationship
  ) {
    return null;
  }

  return {
    id: payload.id,
    fullName: payload.fullName,
    nationalId: payload.nationalId || '',
    age: payload.age,
    bloodType: payload.bloodType,
    address: payload.address || '',
    emergencyPhone: payload.emergencyPhone,
    emergencyContact: {
      name: payload.emergencyContact.name,
      relationship: payload.emergencyContact.relationship,
      phone: payload.emergencyContact.phone || payload.emergencyPhone,
    },
    medicalObservations: payload.medicalObservations || 'Ninguna',
    createdAt: payload.createdAt || new Date().toISOString(),
    photo: payload.photo,
    username: undefined,
    password: undefined,
    deviceId: payload.deviceId || 'VEST-DEMO',
  };
};

const App: React.FC = () => {
  const [currentScreen, setCurrentScreen] = useState<AppScreen>(AppScreen.LANDING);
  const [currentUser, setCurrentUser] = useState<UserData | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);

  // Check for URL parameters on mount (Handling both Local ID and Portable Data Links)
  useEffect(() => {
    const hydrateFromUrl = async () => {
    if (typeof window !== 'undefined') {
        const params = new URLSearchParams(window.location.search);
        
        // OPTION A: Monitoring Link (Contains only the public profile needed to identify the vest)
        const encodedData = params.get('data');
        if (encodedData) {
            try {
                // Decode safe Base64 (Reverse the encoding from QRCodeView)
                const jsonString = decodeURIComponent(escape(atob(encodedData)));
                const importedUser = createImportedUser(JSON.parse(jsonString));
                
                if (importedUser && importedUser.id) {
                    // Save the imported public profile so the monitor can reopen it locally later.
                    await saveUser(importedUser);
                    setCurrentUser(importedUser);
                    setCurrentScreen(AppScreen.PROFILE);
                    return; // Stop processing
                }
            } catch (e) {
                console.error("Error decoding portable link:", e);
                setProfileError("El enlace de datos parece estar dañado o incompleto.");
                setCurrentScreen(AppScreen.PROFILE);
            }
        }

        // OPTION B: Local ID Link (Only works if data is already on device)
        const uid = params.get('uid');
        if (uid) {
          const foundUser = await getUserById(uid);
          if (foundUser) {
            setCurrentUser(foundUser);
            setCurrentScreen(AppScreen.PROFILE);
          } else {
            setProfileError("Usuario no encontrado en este dispositivo. Si escaneaste un código antiguo, asegúrate de tener los datos sincronizados.");
            setCurrentScreen(AppScreen.PROFILE);
          }
        }
    }
    };

    void hydrateFromUrl();
  }, []);

  const handleNewRegister = () => {
    setCurrentScreen(AppScreen.REGISTER);
    clearUrlParams();
  };

  const handleLoginClick = () => {
    setCurrentScreen(AppScreen.LOGIN);
  };

  const handleRegisterSubmit = async (user: UserData) => {
    await saveUser(user);
    setCurrentUser(user);
    setCurrentScreen(AppScreen.QR_VIEW);
  };

  const handleLoginSuccess = async (id: string) => {
    const user = await getUserById(id);
    if (user) {
        setCurrentUser(user);
        setCurrentScreen(AppScreen.PROFILE);
    } else {
        throw new Error('Usuario no encontrado con ese ID.');
    }
  };

  const handleCredentialLogin = async (username: string, password: string) => {
    const user = await loginUser(username, password);

    if (!user) {
      throw new Error('No se encontró el usuario.');
    }

    setCurrentUser(user);
    setCurrentScreen(AppScreen.PROFILE);
  };

  const handleBackHome = () => {
    setCurrentUser(null);
    setCurrentScreen(AppScreen.LANDING);
    setProfileError(null);
    clearUrlParams();
  };

  const clearUrlParams = () => {
    if (window.location.search) {
        const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        window.history.pushState({path: newUrl}, '', newUrl);
    }
  };

  return (
    <>
      <Header onLogoClick={handleBackHome} />
      <main className="flex-grow bg-smart-light flex flex-col">
        {currentScreen === AppScreen.LANDING && (
          <LandingPage 
            onLogin={handleLoginClick} 
            onRegister={handleNewRegister} 
          />
        )}

        {currentScreen === AppScreen.LOGIN && (
            <Login 
                onLoginSuccess={handleLoginSuccess}
                onCredentialLogin={handleCredentialLogin}
                onCancel={handleBackHome}
            />
        )}
        
        {currentScreen === AppScreen.REGISTER && (
            <div className="container mx-auto px-4 py-8">
                <RegistrationForm 
                    onSubmit={handleRegisterSubmit} 
                    onCancel={handleBackHome} 
                />
            </div>
        )}

        {currentScreen === AppScreen.QR_VIEW && currentUser && (
           <div className="container mx-auto px-4 py-8">
                <QRCodeView 
                    user={currentUser} 
                    onBackHome={handleBackHome} 
                />
          </div>
        )}

        {currentScreen === AppScreen.PROFILE && (
            currentUser ? (
                <div className="container mx-auto px-4 py-8">
                    <UserProfile 
                        user={currentUser} 
                        onBackHome={handleBackHome} 
                    />
                </div>
            ) : (
                <div className="container mx-auto px-4 py-12 text-center">
                    <div className="bg-white p-8 rounded-2xl shadow-xl inline-block max-w-md border-t-4 border-red-500">
                        <h2 className="text-3xl font-bold mb-4 text-gray-800">Error de Lectura</h2>
                        <p className="text-gray-600 mb-6">{profileError || "El enlace no contiene datos válidos."}</p>
                        <Button onClick={handleBackHome}>Volver al Inicio</Button>
                    </div>
                </div>
            )
        )}
      </main>
      <footer className="bg-smart-dark text-gray-500 py-8 border-t border-gray-800 mt-auto">
        <div className="container mx-auto px-4 text-center">
            <div className="flex justify-center gap-4 mb-4">
                <span className="w-2 h-2 rounded-full bg-gray-600"></span>
                <span className="w-2 h-2 rounded-full bg-gray-600"></span>
                <span className="w-2 h-2 rounded-full bg-gray-600"></span>
            </div>
            <p>© {new Date().getFullYear()} SmartVest Technologies.</p>
            <p className="text-sm mt-2 opacity-60">Innovación en asistencia visual.</p>
            <p className="text-xs mt-4 text-gray-600 font-medium">
                Realizada por <span className="text-gray-500 hover:text-gray-400 transition-colors">AJTecnology.com</span>
            </p>
        </div>
      </footer>
    </>
  );
};

export default App;
