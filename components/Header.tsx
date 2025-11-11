
import React from 'react';
import { Menu } from 'lucide-react';

interface Props {
  onLogoClick: () => void;
}

export const Header: React.FC<Props> = ({ onLogoClick }) => {
  return (
    <header className="bg-smart-dark text-white p-4 shadow-md sticky top-0 z-50 backdrop-blur-md bg-opacity-95">
      <div className="max-w-6xl mx-auto flex items-center justify-between">
        <div 
            className="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity"
            onClick={onLogoClick}
        >
          {/* Custom Logo SVG based on provided design */}
          <div className="bg-white p-1.5 rounded-xl h-12 w-12 flex items-center justify-center shadow-lg">
             <svg viewBox="0 0 100 100" className="w-full h-full" fill="none" stroke="#0f172a" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round">
                {/* Vest Outline */}
                <path d="M25 25 L25 15 Q50 45 75 15 L75 25 Q85 50 85 60 L85 85 Q50 92 15 85 L15 60 Q15 50 25 25 Z" />
                {/* Wifi Signal */}
                <path d="M35 50 Q50 38 65 50" strokeWidth="7" />
                <path d="M42 62 Q50 55 58 62" strokeWidth="7" />
                <circle cx="50" cy="74" r="5" fill="#0f172a" stroke="none" />
             </svg>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-wide font-sans">SmartVest</h1>
            <p className="text-[10px] text-gray-300 uppercase tracking-widest">Sistema de Emergencia</p>
          </div>
        </div>
        
        {/* Navigation removed as requested, content is now on Landing Page */}
      </div>
    </header>
  );
};
