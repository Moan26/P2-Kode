import { Platform } from 'react-native';
import { apiRequest } from './api';

const BASE = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';

function fixUrl(url: string | null): string | null {
  if (!url) return null;
  if (url.startsWith('/')) return BASE + url;
  if (url.startsWith('http')) return url.replace('localhost', '10.0.2.2');
  return url;
}

function imageFileIdToUrl(fileId: string | null): string | null {
  if (!fileId) return null;
  return `${BASE}/api/images/stream/${fileId}`;
}

export interface ImageItem {
  id: string;
  imageUrl: string | null;
  title: string | null;
  alt_text: string | null;
  description: string | null;
  caption: string | null;
  uploadedAt: string | null;
  isPublished: boolean | null;
  createdBy: string | null;
}

export interface VideoItem {
  id: string;
  watchUrl: string | null;
  title: string | null;
  description: string | null;
  thumbnail: string | null;
  duration: number | null;
  uploadedAt: string | null;
  isPublished: boolean;
  captions: string | null;
}

interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  pagination: { page: number; limit: number; total: number; totalPages: number };
}

export async function fetchImages(
  opts: { isPublished?: boolean; page?: number; limit?: number; signal?: AbortSignal } = {},
): Promise<ImageItem[]> {
  const res = await apiRequest<PaginatedResponse<ImageItem>>('/api/images', {
    query: { isPublished: opts.isPublished, page: opts.page ?? 1, limit: opts.limit ?? 20 },
    signal: opts.signal,
  });
  return (res.data ?? []).map(i => ({ ...i, imageUrl: imageFileIdToUrl(i.imageUrl) }));
}

export async function fetchVideos(
  opts: { isPublished?: boolean; page?: number; limit?: number; signal?: AbortSignal } = {},
): Promise<VideoItem[]> {
  const res = await apiRequest<PaginatedResponse<VideoItem>>('/api/videos', {
    query: { isPublished: opts.isPublished, page: opts.page ?? 1, limit: opts.limit ?? 20 },
    signal: opts.signal,
  });
  return (res.data ?? []).map(v => ({
    ...v,
    watchUrl: fixUrl(v.watchUrl),
    thumbnail: fixUrl(v.thumbnail),
  }));
}

export async function uploadImage(
  file: { uri: string; name: string; type: string },
  meta: { title: string; alt_text: string; description?: string; caption?: string },
): Promise<ImageItem> {
  const form = new FormData();
  form.append('imageFile', { uri: file.uri, name: file.name, type: file.type } as any);
  form.append('title', meta.title);
  form.append('alt_text', meta.alt_text);
  if (meta.description) form.append('description', meta.description);
  if (meta.caption) form.append('caption', meta.caption);
  return apiRequest<ImageItem>('/api/images', { method: 'POST', body: form });
}

export async function updateImage(
  id: string,
  changes: { title?: string; description?: string; isPublished?: boolean },
): Promise<ImageItem> {
  const form = new FormData();
  if (changes.title !== undefined) form.append('title', changes.title);
  if (changes.description !== undefined) form.append('description', changes.description);
  if (changes.isPublished !== undefined) form.append('isPublished', String(changes.isPublished));
  return apiRequest<ImageItem>(`/api/images/${id}`, { method: 'PATCH', body: form });
}

export async function deleteImage(id: string): Promise<void> {
  await apiRequest<unknown>(`/api/images/${id}`, { method: 'DELETE' });
}

export async function uploadVideo(
  file: { uri: string; name: string; type: string },
  meta: { title: string; description?: string },
): Promise<VideoItem> {
  const form = new FormData();
  form.append('videoFile', { uri: file.uri, name: file.name, type: file.type } as any);
  form.append('title', meta.title);
  if (meta.description) form.append('description', meta.description);
  return apiRequest<VideoItem>('/api/videos', { method: 'POST', body: form });
}

export async function updateVideo(
  id: string,
  changes: { title?: string; description?: string; isPublished?: boolean },
): Promise<void> {
  await apiRequest<unknown>(`/api/videos/${id}`, { method: 'PATCH', query: changes });
}

export async function deleteVideo(id: string): Promise<void> {
  await apiRequest<unknown>(`/api/videos/${id}`, { method: 'DELETE' });
}