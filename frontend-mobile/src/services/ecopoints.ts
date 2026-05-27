import { apiRequest } from './api';

export interface EcopointListItem {
  id: string;
  name: string;
  address: string | null;
  distanceKm: number | null;
  acceptedMaterials: string[] | null;
  status: string | null;
  thumbnailUrl: string | null;
}

export interface EcopointDetail {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  status: string | null;
  condition: string | null;
}

export interface EcopointWithCoords extends EcopointListItem {
  latitude: number;
  longitude: number;
  condition: string | null;
}

interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  pagination: { page: number; limit: number; total: number; totalPages: number };
}

interface DetailEnvelope {
  success: boolean;
  data: {
    id: string;
    name: string;
    coordinates?: { type: string; coordinates: [number, number] } | null;
    latitude?: number;
    longitude?: number;
    status?: string | null;
    Status?: string | null;
    conditionEcopoint?: string | null;
    condition?: string | null;
    Condition?: string | null;
  };
  message: string;
}

export async function fetchEcopointList(
  opts: { lat?: number; lng?: number; page?: number; limit?: number; signal?: AbortSignal } = {},
): Promise<EcopointListItem[]> {
  const res = await apiRequest<PaginatedResponse<EcopointListItem>>(
    '/api/ecopoints',
    {
      query: {
        lat: opts.lat,
        lng: opts.lng,
        page: opts.page ?? 1,
        limit: opts.limit ?? 50,
      },
      signal: opts.signal,
    },
  );
  return res.data ?? [];
}

export async function fetchEcopointDetail(
  id: string,
  signal?: AbortSignal,
): Promise<EcopointDetail> {
  const res = await apiRequest<DetailEnvelope>(`/api/ecopoints/${encodeURIComponent(id)}`, {
    signal,
  });
  const d = res.data;
  const lng = d.coordinates?.coordinates[0] ?? d.longitude ?? 0;
  const lat = d.coordinates?.coordinates[1] ?? d.latitude ?? 0;
  return {
    id: d.id,
    name: d.name,
    latitude: lat,
    longitude: lng,
    status: d.status ?? d.Status ?? null,
    condition: d.conditionEcopoint ?? d.condition ?? d.Condition ?? null,
  };
}

// Backend list endpoint does not include coordinates, so we fetch detail per
// item in parallel and merge. Acceptable for demo-sized datasets.
export async function fetchEcopointsForMap(
  opts: { lat?: number; lng?: number; signal?: AbortSignal } = {},
): Promise<EcopointWithCoords[]> {
  const rawRes = await apiRequest<unknown>('/api/ecopoints', { query: { page: 1, limit: 50 }, signal: opts.signal });
  console.log('[ecopoints] raw response:', JSON.stringify(rawRes));

  const list = await fetchEcopointList({
    lat: opts.lat,
    lng: opts.lng,
    limit: 50,
    signal: opts.signal,
  });
  console.log('[ecopoints] list count:', list.length, list.map(i => i.id));

  const details = await Promise.all(
    list.map((item) =>
      fetchEcopointDetail(item.id, opts.signal).catch((e) => {
        console.log('[ecopoints] detail failed for', item.id, String(e));
        return null;
      }),
    ),
  );
  console.log('[ecopoints] details:', JSON.stringify(details));

  const merged: EcopointWithCoords[] = [];
  for (let i = 0; i < list.length; i++) {
    const item = list[i];
    const detail = details[i];
    if (!detail) continue;
    if (
      typeof detail.latitude !== 'number' ||
      typeof detail.longitude !== 'number'
    ) {
      continue;
    }
    merged.push({
      ...item,
      latitude: detail.latitude,
      longitude: detail.longitude,
      status: item.status ?? detail.status,
      condition: detail.condition,
      distanceKm: item.distanceKm,
    });
  }
  return merged;
}