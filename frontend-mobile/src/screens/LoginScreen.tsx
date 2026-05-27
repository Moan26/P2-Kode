import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { login, register } from '../services/authService';
import { useAuth } from '../context/AuthContext';

type Mode = 'login' | 'register';

export default function LoginScreen() {
  const { setToken } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit() {
    setError(null);
    setLoading(true);
    try {
      if (mode === 'login') {
        const data = await login(email.trim(), password);
        setToken(data.token);
      } else {
        await register(username.trim(), email.trim(), password);
        const data = await login(email.trim(), password);
        setToken(data.token);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Noget gik galt');
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={styles.flex} edges={['top']}>
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.logo}>🌿 Ecolink</Text>
        <Text style={styles.title}>
          {mode === 'login' ? 'Log ind' : 'Opret konto'}
        </Text>

        {mode === 'register' && (
          <TextInput
            style={styles.input}
            placeholder="Brugernavn"
            autoCapitalize="none"
            value={username}
            onChangeText={setUsername}
          />
        )}

        <TextInput
          style={styles.input}
          placeholder="Email"
          autoCapitalize="none"
          keyboardType="email-address"
          value={email}
          onChangeText={setEmail}
        />

        <TextInput
          style={styles.input}
          placeholder="Adgangskode"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        {error && <Text style={styles.error}>{error}</Text>}

        <TouchableOpacity
          style={styles.button}
          onPress={handleSubmit}
          disabled={loading}
        >
          {loading
            ? <ActivityIndicator color="white" />
            : <Text style={styles.buttonText}>
                {mode === 'login' ? 'Log ind' : 'Opret konto'}
              </Text>
          }
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null); }}
        >
          <Text style={styles.switchText}>
            {mode === 'login'
              ? 'Ingen konto? Opret en her'
              : 'Har du allerede en konto? Log ind'}
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: 'white' },
  container: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 32,
  },
  logo: {
    fontSize: 40,
    textAlign: 'center',
    marginBottom: 8,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 32,
    color: '#111827',
  },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    padding: 14,
    fontSize: 16,
    marginBottom: 12,
    backgroundColor: '#f9fafb',
  },
  error: {
    color: '#ef4444',
    textAlign: 'center',
    marginBottom: 12,
  },
  button: {
    backgroundColor: '#0ea5e9',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginBottom: 16,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  switchText: {
    color: '#0ea5e9',
    textAlign: 'center',
    fontSize: 14,
  },
});
