import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import MapView, { Marker, PROVIDER_GOOGLE, Region } from 'react-native-maps';
import * as Location from 'expo-location';

import {
  EcopointWithCoords,
  fetchEcopointsForMap,
} from '../services/ecopoints';
import { ApiError } from '../services/api';

const FALLBACK_REGION: Region = {
  latitude: 55.6761,
  longitude: 12.5683,
  latitudeDelta: 0.08,
  longitudeDelta: 0.08,
};

export default function MapScreen() {
  const [region, setRegion] = useState<Region>(FALLBACK_REGION);
  const [userLocation, setUserLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [points, setPoints] = useState<EcopointWithCoords[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPoint, setSelectedPoint] = useState<EcopointWithCoords | null>(null);

  const mapRef = useRef<MapView | null>(null);

  const loadPoints = useCallback(
    async (lat: number | undefined, lng: number | undefined, signal: AbortSignal) => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchEcopointsForMap({ lat, lng, signal });
        if (signal.aborted) return;
        console.log('[MapScreen] points loaded:', data.length, JSON.stringify(data));
        setPoints(data);
      } catch (e) {
        if (signal.aborted) return;
        if (e instanceof ApiError) {
          setError(`Backend error ${e.status}: ${e.message}`);
        } else if (e instanceof Error) {
          setError(e.message);
        } else {
          setError('Unknown error fetching ecopoints');
        }
      } finally {
        if (!signal.aborted) setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    const controller = new AbortController();
    let cancelled = false;

    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (cancelled) return;
        if (status === 'granted') {
          const pos = await Location.getCurrentPositionAsync({
            accuracy: Location.Accuracy.Balanced,
          });
          if (cancelled) return;
          const here = {
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
          };
          setUserLocation(here);
          setRegion({ ...here, latitudeDelta: 0.05, longitudeDelta: 0.05 });
          await loadPoints(here.latitude, here.longitude, controller.signal);
        } else {
          await loadPoints(undefined, undefined, controller.signal);
        }
      } catch {
        if (!cancelled) {
          await loadPoints(undefined, undefined, controller.signal);
        }
      }
    })();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [loadPoints]);

  const handleRecenter = useCallback(() => {
    const target = userLocation
      ? { ...userLocation, latitudeDelta: 0.03, longitudeDelta: 0.03 }
      : FALLBACK_REGION;
    mapRef.current?.animateToRegion(target, 500);
  }, [userLocation]);

  const handleRetry = useCallback(() => {
    const controller = new AbortController();
    void loadPoints(userLocation?.latitude, userLocation?.longitude, controller.signal);
  }, [loadPoints, userLocation]);

  return (
    <View style={styles.container}>
      <MapView
        ref={mapRef}
        provider={PROVIDER_GOOGLE}
        style={styles.map}
        initialRegion={region}
        showsUserLocation
        showsMyLocationButton={false}
      >
        {points.map((p) => (
          <Marker
            key={p.id}
            coordinate={{ latitude: p.latitude, longitude: p.longitude }}
            onPress={() => setSelectedPoint(p)}
          />
        ))}
      </MapView>

      <TouchableOpacity
        style={styles.recenter}
        onPress={handleRecenter}
        accessibilityRole="button"
        accessibilityLabel="Recenter map on me"
      >
        <Text style={styles.recenterText}>◎</Text>
      </TouchableOpacity>

      {loading ? (
        <View style={styles.banner}>
          <ActivityIndicator size="small" />
          <Text style={styles.bannerText}> Loading ecopoints…</Text>
        </View>
      ) : null}

      {error ? (
        <View style={[styles.banner, styles.errorBanner]}>
          <Text style={styles.errorText} numberOfLines={2}>
            {error}
          </Text>
          <TouchableOpacity onPress={handleRetry} style={styles.retryButton}>
            <Text style={styles.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      ) : null}

      {selectedPoint ? (
        <View style={styles.infoPanel}>
          <View style={styles.infoPanelHeader}>
            <Text style={styles.infoPanelTitle}>{selectedPoint.name}</Text>
            <TouchableOpacity onPress={() => setSelectedPoint(null)}>
              <Text style={styles.infoPanelClose}>✕</Text>
            </TouchableOpacity>
          </View>
          {selectedPoint.address ? (
            <Text style={styles.infoPanelLine}>{selectedPoint.address}</Text>
          ) : null}
          {selectedPoint.status ? (
            <Text style={styles.infoPanelLine}>Status: {selectedPoint.status}</Text>
          ) : null}
          {selectedPoint.condition ? (
            <Text style={styles.infoPanelLine}>Tilstand: {selectedPoint.condition}</Text>
          ) : null}
          {typeof selectedPoint.distanceKm === 'number' ? (
            <Text style={styles.infoPanelLine}>
              {selectedPoint.distanceKm.toFixed(2)} km væk
            </Text>
          ) : null}
          {selectedPoint.acceptedMaterials && selectedPoint.acceptedMaterials.length > 0 ? (
            <Text style={styles.infoPanelLine}>
              Accepterer: {selectedPoint.acceptedMaterials.join(', ')}
            </Text>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  map: { ...StyleSheet.absoluteFillObject },
  recenter: {
    position: 'absolute',
    right: 16,
    bottom: 32,
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: 'white',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  recenterText: { fontSize: 22 },
  banner: {
    position: 'absolute',
    top: Platform.OS === 'ios' ? 56 : 16,
    left: 16,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'white',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 8,
    elevation: 3,
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  bannerText: { fontSize: 14 },
  errorBanner: { backgroundColor: '#fee2e2' },
  errorText: { flex: 1, fontSize: 13, color: '#991b1b' },
  retryButton: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: '#991b1b',
    borderRadius: 4,
    marginLeft: 8,
  },
  retryText: { color: 'white', fontWeight: '600' },
  infoPanel: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'white',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    padding: 16,
    elevation: 8,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: -2 },
  },
  infoPanelHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  infoPanelTitle: { fontWeight: '700', fontSize: 16, color: '#111827', flex: 1 },
  infoPanelClose: { fontSize: 18, color: '#6b7280', paddingLeft: 12 },
  infoPanelLine: { fontSize: 13, color: '#374151', marginBottom: 4 },
});