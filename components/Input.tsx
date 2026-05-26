import React, { useId } from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement | HTMLTextAreaElement> {
  label: string;
  error?: string;
  isTextArea?: boolean;
  rightAction?: React.ReactNode;
}

export const Input: React.FC<InputProps> = ({ label, error, isTextArea, className = '', rightAction, id, ...props }) => {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = error ? `${inputId}-error` : undefined;
  const baseInputStyles =
    'w-full p-4 text-lg bg-white border-2 border-smart-gray rounded-lg text-smart-dark focus:border-smart-primary focus:ring-2 focus:ring-blue-200 focus-visible:outline-none transition-all';

  return (
    <div className="mb-6">
      <label htmlFor={inputId} className="block text-smart-dark font-bold text-lg mb-2">
        {label}
      </label>
      <div className="relative flex gap-2">
        {isTextArea ? (
          <textarea
            id={inputId}
            className={`${baseInputStyles} min-h-[120px] ${className}`}
            aria-invalid={error ? true : undefined}
            aria-describedby={errorId}
            {...(props as React.TextareaHTMLAttributes<HTMLTextAreaElement>)}
          />
        ) : (
          <input
            id={inputId}
            className={`${baseInputStyles} ${className}`}
            aria-invalid={error ? true : undefined}
            aria-describedby={errorId}
            {...(props as React.InputHTMLAttributes<HTMLInputElement>)}
          />
        )}
        {rightAction && <div className="flex-shrink-0">{rightAction}</div>}
      </div>
      {error && (
        <p id={errorId} className="text-red-600 font-bold mt-1 text-base" role="alert">
          ⚠️ {error}
        </p>
      )}
    </div>
  );
};
