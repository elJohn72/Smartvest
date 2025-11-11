import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline';
  fullWidth?: boolean;
  icon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({ 
  children, 
  variant = 'primary', 
  fullWidth = false, 
  className = '', 
  icon,
  ...props 
}) => {
  const baseStyles = "py-4 px-6 rounded-xl font-bold text-lg transition-colors duration-200 flex items-center justify-center gap-2 focus:outline-none focus:ring-4 focus:ring-offset-2";
  
  const variants = {
    primary: "bg-smart-primary text-white hover:bg-blue-900 focus:ring-blue-500 shadow-lg",
    secondary: "bg-smart-gray text-white hover:bg-slate-600 focus:ring-slate-400",
    outline: "border-2 border-smart-primary text-smart-primary hover:bg-blue-50 focus:ring-blue-500"
  };

  return (
    <button 
      className={`${baseStyles} ${variants[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...props}
    >
      {icon && <span className="w-6 h-6">{icon}</span>}
      {children}
    </button>
  );
};
