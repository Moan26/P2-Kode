import React, { createContext, useContext, useEffect, useState } from 'react';
import { getToken, logout as doLogout } from '../services/authService';

type AuthContextType = {
  token: string | null;
  isLoading: boolean;
  setToken: (token: string | null) => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType>({
  token: null,
  isLoading: true,
  setToken: () => {},
  logout: async () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setTokenState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    getToken().then(t => {
      setTokenState(t);
      setIsLoading(false);
    });
  }, []);

  const setToken = (t: string | null) => setTokenState(t);

  const logout = async () => {
    await doLogout();
    setTokenState(null);
  };

  return (
    <AuthContext.Provider value={{ token, isLoading, setToken, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
