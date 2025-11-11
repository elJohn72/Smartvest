import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Carga las variables de entorno desde el archivo .env (si existe)
  // Fix: Cast process to any to avoid TS error regarding cwd method not existing on type Process
  const env = loadEnv(mode, (process as any).cwd(), '');
  
  return {
    plugins: [react()],
    define: {
      // Esto permite que 'process.env.API_KEY' funcione en el navegador
      'process.env.API_KEY': JSON.stringify(env.API_KEY),
    },
  };
});