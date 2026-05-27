import * as SecureStore from 'expo-secure-store';
import { getBackendUrl } from '../config/apiConfig';

const TOKEN_KEY = 'jwt_token';

export async function login(email: string, password: string): Promise<{ token: string; user: { id: string; username: string; email: string } }> {
  const res = await fetch(`${getBackendUrl()}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message ?? 'Login fejlede');
  await SecureStore.setItemAsync(TOKEN_KEY, data.token);
  return data;
}

export async function register(username: string, email: string, password: string): Promise<void> {
  const res = await fetch(`${getBackendUrl()}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message ?? 'Registrering fejlede');
}

export async function logout(): Promise<void> {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}

export async function getToken(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}
