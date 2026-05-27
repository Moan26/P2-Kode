import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as Location from 'expo-location';

import { EcopointListItem, fetchEcopointList } from '../services/ecopoints';
import { useAuth } from '../context/AuthContext';

type Props = {
  onNavigate: (tab: 'map' | 'content') => void;
};

export default function HomeScreen({ onNavigate }: Props) {
  const { logout } = useAuth();
  const [nearest, setNearest] = useState<EcopointListItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        let lat: number | undefined;
        let lng: number | undefined;

        if (status === 'granted') {
          const pos = await Location.getCurrentPositionAsync({
            accuracy: Location.Accuracy.Balanced,
          });
          lat = pos.coords.latitude;
          lng = pos.coords.longitude;
        }

        const list = await fetchEcopointList({ lat, lng, limit: 1, signal: controller.signal });
        if (!controller.signal.aborted) {
          setNearest(list[0] ?? null);
        }
      } catch (e) {
        if (!controller.signal.aborted) {
          setError(e instanceof Error ? e.message : 'Kunne ikke hente data');
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    })();

    return () => controller.abort();
  }, []);

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <Text style={styles.title}>Ecolink</Text>
      <Text style={styles.subtitle}>Find din nærmeste genbrugsstation</Text>

      <View style={styles.card}>
        <Text style={styles.cardLabel}>Nærmeste Ecopoint</Text>

        {loading ? (
          <ActivityIndicator style={{ marginTop: 12 }} />
        ) : error ? (
          <Text style={styles.errorText}>{error}</Text>
        ) : nearest ? (
          <>
            <Text style={styles.cardName}>{nearest.name}</Text>
            {typeof nearest.distanceKm === 'number' && (
              <Text style={styles.cardDetail}>{nearest.distanceKm.toFixed(2)} km væk</Text>
            )}
            {nearest.status && (
              <View style={[styles.badge, nearest.status === 'FULL' ? styles.badgeFull : styles.badgeOk]}>
                <Text style={styles.badgeText}>
                  {nearest.status === 'FULL' ? 'Fuld' : 'Ledig'}
                </Text>
              </View>
            )}
          </>
        ) : (
          <Text style={styles.cardDetail}>Ingen ecopoints fundet</Text>
        )}
      </View>

      <View style={styles.actions}>
        <TouchableOpacity style={styles.btn} onPress={() => onNavigate('map')}>
          <Text style={styles.btnText}>Se på kort</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.btn, styles.btnSecondary]} onPress={() => onNavigate('content')}>
          <Text style={[styles.btnText, styles.btnTextSecondary]}>Content</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.btnLogout} onPress={logout}>
          <Text style={styles.btnLogoutText}>Log ud</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: 'white', padding: 24, justifyContent: 'center' },
  title: { fontSize: 32, fontWeight: '800', color: '#0ea5e9', textAlign: 'center' },
  subtitle: { fontSize: 15, color: '#6b7280', textAlign: 'center', marginTop: 6, marginBottom: 32 },
  card: {
    backgroundColor: '#f0f9ff',
    borderRadius: 16,
    padding: 20,
    marginBottom: 32,
    borderWidth: 1,
    borderColor: '#bae6fd',
  },
  cardLabel: { fontSize: 12, fontWeight: '700', color: '#0369a1', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 },
  cardName: { fontSize: 20, fontWeight: '700', color: '#0c4a6e', marginBottom: 4 },
  cardDetail: { fontSize: 14, color: '#475569', marginBottom: 8 },
  errorText: { fontSize: 14, color: '#dc2626', marginTop: 8 },
  badge: { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20, marginTop: 4 },
  badgeOk: { backgroundColor: '#dcfce7' },
  badgeFull: { backgroundColor: '#fee2e2' },
  badgeText: { fontSize: 12, fontWeight: '700', color: '#374151' },
  actions: { gap: 12 },
  btn: { backgroundColor: '#0ea5e9', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  btnSecondary: { backgroundColor: 'white', borderWidth: 1.5, borderColor: '#0ea5e9' },
  btnText: { fontSize: 16, fontWeight: '700', color: 'white' },
  btnTextSecondary: { color: '#0ea5e9' },
  btnLogout: { alignItems: 'center', paddingVertical: 10 },
  btnLogoutText: { color: '#9ca3af', fontSize: 14 },
});
