import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement | HTMLTextAreaElement> {
  label: string;
  error?: string;
  isTextArea?: boolean;
  rightAction?: React.ReactNode;
}

export const Input: React.FC<InputProps> = ({ label, error, isTextArea, className = '', rightAction, ...props }) => {
  const baseInputStyles = "w-full p-4 text-lg bg-white border-2 border-smart-gray rounded-lg text-smart-dark focus:border-smart-primary focus:ring-2 focus:ring-blue-200 outline-none transition-all";
  
  return (
    <div className="mb-6">
      <label className="block text-smart-dark font-bold text-lg mb-2">
        {label}
      </label>
      <div className="relative flex gap-2">
        {isTextArea ? (
            <textarea 
                className={`${baseInputStyles} min-h-[120px] ${className}`}
                {...(props as React.TextareaHTMLAttributes<HTMLTextAreaElement>)}
            />
        ) : (
            <input 
                className={`${baseInputStyles} ${className}`}
                {...(props as React.InputHTMLAttributes<HTMLInputElement>)}
            />
        )}
        {rightAction && (
            <div className="flex-shrink-0">
                {rightAction}
            </div>
        )}
      </div>
      {error && <p className="text-red-600 font-bold mt-1 text-base">⚠️ {error}</p>}
    </div>
  );
};
