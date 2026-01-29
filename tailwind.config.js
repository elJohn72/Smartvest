/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./App.tsx",
    "./components/**/*.{tsx,jsx}",
    "./pages/**/*.{tsx,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        smart: {
          dark: '#0f172a',
          primary: '#1e40af',
          accent: '#3b82f6',
          light: '#f8fafc',
          gray: '#64748b',
        },
      },
    },
  },
  plugins: [],
}
