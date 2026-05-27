import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Image,
  Modal,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { SafeAreaView } from 'react-native-safe-area-context';

import {
  ImageItem,
  VideoItem,
  fetchImages,
  fetchVideos,
  uploadImage,
  updateImage,
  deleteImage,
  uploadVideo,
  updateVideo,
  deleteVideo,
} from '../services/content';
import { ApiError } from '../services/api';
import VideoPlayer from '../components/VideoPlayer';

type Tab = 'images' | 'videos';

interface UploadForm {
  title: string;
  altText: string;
  description: string;
  caption: string;
  file: { uri: string; name: string; type: string } | null;
}

interface EditForm {
  title: string;
  description: string;
  isPublished: boolean;
}

const emptyUpload = (): UploadForm => ({
  title: '', altText: '', description: '', caption: '', file: null,
});

const emptyEdit = (item: ImageItem | VideoItem): EditForm => ({
  title: item.title ?? '',
  description: item.description ?? '',
  isPublished: item.isPublished ?? false,
});

export default function ContentScreen() {
  const [tab, setTab] = useState<Tab>('images');
  const [images, setImages] = useState<ImageItem[]>([]);
  const [videos, setVideos] = useState<VideoItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showUpload, setShowUpload] = useState(false);
  const [uploadForm, setUploadForm] = useState<UploadForm>(emptyUpload());
  const [saving, setSaving] = useState(false);

  const [editTarget, setEditTarget] = useState<{ type: Tab; item: ImageItem | VideoItem } | null>(null);
  const [editForm, setEditForm] = useState<EditForm>({ title: '', description: '', isPublished: false });

  const [playingVideoIndex, setPlayingVideoIndex] = useState<number | null>(null);
  const [viewingImage, setViewingImage] = useState<ImageItem | null>(null);

  // ── Load ──────────────────────────────────────────────────────────────────

  const load = useCallback(async (which: Tab, signal?: AbortSignal) => {
    setError(null);
    try {
      if (which === 'images') {
        const data = await fetchImages({ limit: 30, signal });
        if (!signal?.aborted) setImages(data);
      } else {
        const data = await fetchVideos({ limit: 30, signal });
        if (!signal?.aborted) setVideos(data);
      }
    } catch (e) {
      if (signal?.aborted) return;
      if (e instanceof ApiError) setError(`Fejl ${e.status}: ${e.message}`);
      else if (e instanceof Error) setError(e.message);
      else setError('Ukendt fejl');
    }
  }, []);

  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    load(tab, ctrl.signal).finally(() => {
      if (!ctrl.signal.aborted) setLoading(false);
    });
    return () => ctrl.abort();
  }, [tab, load]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await load(tab);
    setRefreshing(false);
  }, [tab, load]);

  // ── Pick file ─────────────────────────────────────────────────────────────

  const pickFile = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Tilladelse nægtet', 'Giv adgang til mediefiler i telefonens indstillinger.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: tab === 'images' ? ['images'] : ['videos'],
      quality: 1,
      allowsEditing: false,
    });
    if (result.canceled || result.assets.length === 0) return;
    const asset = result.assets[0];
    setUploadForm(f => ({
      ...f,
      file: {
        uri: asset.uri,
        name: asset.fileName ?? (tab === 'images' ? 'billede.jpg' : 'video.mp4'),
        type: asset.mimeType ?? (tab === 'images' ? 'image/jpeg' : 'video/mp4'),
      },
    }));
  };

  // ── Upload ────────────────────────────────────────────────────────────────

  const handleUpload = async () => {
    if (!uploadForm.file) {
      Alert.alert('Mangler fil', 'Vælg en fil først.');
      return;
    }
    if (!uploadForm.title.trim()) {
      Alert.alert('Mangler titel', 'Giv indholdet en titel.');
      return;
    }
    if (tab === 'images' && !uploadForm.altText.trim()) {
      Alert.alert('Mangler alt-tekst', 'Alt-tekst er påkrævet til billeder.');
      return;
    }
    setSaving(true);
    try {
      if (tab === 'images') {
        const img = await uploadImage(uploadForm.file, {
          title: uploadForm.title.trim(),
          alt_text: uploadForm.altText.trim(),
          description: uploadForm.description.trim() || undefined,
          caption: uploadForm.caption.trim() || undefined,
        });
        setImages(prev => [img, ...prev]);
      } else {
        const vid = await uploadVideo(uploadForm.file, {
          title: uploadForm.title.trim(),
          description: uploadForm.description.trim() || undefined,
        });
        setVideos(prev => [vid, ...prev]);
      }
      setShowUpload(false);
      setUploadForm(emptyUpload());
    } catch (e) {
      Alert.alert('Upload fejl', e instanceof ApiError ? e.message : 'Upload mislykkedes');
    } finally {
      setSaving(false);
    }
  };

  // ── Edit ──────────────────────────────────────────────────────────────────

  const openEdit = (type: Tab, item: ImageItem | VideoItem) => {
    setEditTarget({ type, item });
    setEditForm(emptyEdit(item));
  };

  const handleEdit = async () => {
    if (!editTarget) return;
    if (!editForm.title.trim()) {
      Alert.alert('Mangler titel', 'Titel er påkrævet.');
      return;
    }
    setSaving(true);
    try {
      if (editTarget.type === 'images') {
        const updated = await updateImage(editTarget.item.id, {
          title: editForm.title.trim(),
          description: editForm.description.trim() || undefined,
          isPublished: editForm.isPublished,
        });
        setImages(prev => prev.map(i => (i.id === updated.id ? updated : i)));
      } else {
        await updateVideo(editTarget.item.id, {
          title: editForm.title.trim(),
          description: editForm.description.trim() || undefined,
          isPublished: editForm.isPublished,
        });
        setVideos(prev =>
          prev.map(v =>
            v.id === editTarget.item.id
              ? { ...v, title: editForm.title, description: editForm.description, isPublished: editForm.isPublished }
              : v,
          ),
        );
      }
      setEditTarget(null);
    } catch (e) {
      Alert.alert('Fejl', e instanceof ApiError ? e.message : 'Opdatering mislykkedes');
    } finally {
      setSaving(false);
    }
  };

  // ── Delete ────────────────────────────────────────────────────────────────

  const handleDelete = (type: Tab, id: string, title: string) => {
    Alert.alert(
      'Slet indhold',
      `Er du sikker på, at du vil slette "${title}"?`,
      [
        { text: 'Annuller', style: 'cancel' },
        {
          text: 'Slet',
          style: 'destructive',
          onPress: async () => {
            try {
              if (type === 'images') {
                await deleteImage(id);
                setImages(prev => prev.filter(i => i.id !== id));
              } else {
                await deleteVideo(id);
                setVideos(prev => prev.filter(v => v.id !== id));
              }
            } catch (e) {
              Alert.alert('Fejl', e instanceof ApiError ? e.message : 'Sletning mislykkedes');
            }
          },
        },
      ],
    );
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <SafeAreaView style={styles.root} edges={['top']}>
      {/* Tab bar */}
      <View style={styles.tabBar}>
        <TabBtn label="Billeder" active={tab === 'images'} onPress={() => setTab('images')} />
        <TabBtn label="Videoer" active={tab === 'videos'} onPress={() => setTab('videos')} />
      </View>

      {loading ? (
        <Center>
          <ActivityIndicator size="large" color="#0ea5e9" />
          <Text style={styles.muted}>Henter {tab === 'images' ? 'billeder' : 'videoer'}…</Text>
        </Center>
      ) : error ? (
        <Center>
          <Text style={styles.errorText}>{error}</Text>
          <TouchableOpacity onPress={() => load(tab)} style={styles.retryBtn}>
            <Text style={styles.retryText}>Prøv igen</Text>
          </TouchableOpacity>
        </Center>
      ) : tab === 'images' ? (
        <FlatList
          data={images}
          keyExtractor={i => i.id}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
          renderItem={({ item }) => (
            <ImageCard
              item={item}
              onView={() => setViewingImage(item)}
              onEdit={() => openEdit('images', item)}
              onDelete={() => handleDelete('images', item.id, item.title ?? 'Untitled')}
            />
          )}
          ListEmptyComponent={<Text style={styles.emptyText}>Ingen billeder endnu.</Text>}
        />
      ) : (
        <FlatList
          data={videos}
          keyExtractor={v => v.id}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
          renderItem={({ item, index }) => (
            <VideoCard
              item={item}
              onPlay={() => setPlayingVideoIndex(index)}
              onEdit={() => openEdit('videos', item)}
              onDelete={() => handleDelete('videos', item.id, item.title ?? 'Untitled')}
            />
          )}
          ListEmptyComponent={<Text style={styles.emptyText}>Ingen videoer endnu.</Text>}
        />
      )}

      {/* FAB */}
      <TouchableOpacity
        style={styles.fab}
        onPress={() => { setUploadForm(emptyUpload()); setShowUpload(true); }}
        activeOpacity={0.85}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>

      {/* ── Upload modal ─────────────────────────────────────────────────── */}
      <Modal
        visible={showUpload}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setShowUpload(false)}
      >
        <SafeAreaView style={styles.modal}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>
              Upload {tab === 'images' ? 'billede' : 'video'}
            </Text>
            <TouchableOpacity onPress={() => setShowUpload(false)} hitSlop={12}>
              <Text style={styles.closeBtn}>✕</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalBody} keyboardShouldPersistTaps="handled">
            {uploadForm.file && tab === 'images' ? (
              <Image source={{ uri: uploadForm.file.uri }} style={styles.preview} resizeMode="cover" />
            ) : uploadForm.file ? (
              <View style={[styles.preview, styles.videoPreviewBox]}>
                <Text style={styles.videoPreviewIcon}>📹</Text>
                <Text style={styles.videoPreviewName} numberOfLines={1}>{uploadForm.file.name}</Text>
              </View>
            ) : null}

            <TouchableOpacity style={styles.pickBtn} onPress={pickFile}>
              <Text style={styles.pickBtnText}>
                {uploadForm.file
                  ? 'Skift fil'
                  : tab === 'images' ? 'Vælg billede fra galleri' : 'Vælg video fra galleri'}
              </Text>
            </TouchableOpacity>

            <FormField
              label="Titel *"
              value={uploadForm.title}
              onChange={t => setUploadForm(f => ({ ...f, title: t }))}
              placeholder="Giv indholdet en titel"
            />
            {tab === 'images' && (
              <FormField
                label="Alt-tekst *"
                value={uploadForm.altText}
                onChange={t => setUploadForm(f => ({ ...f, altText: t }))}
                placeholder="Kort beskrivelse til skærmlæsere"
              />
            )}
            <FormField
              label="Beskrivelse"
              value={uploadForm.description}
              onChange={t => setUploadForm(f => ({ ...f, description: t }))}
              placeholder="Valgfri beskrivelse"
              multiline
            />
            {tab === 'images' && (
              <FormField
                label="Billedtekst"
                value={uploadForm.caption}
                onChange={t => setUploadForm(f => ({ ...f, caption: t }))}
                placeholder="Valgfri billedtekst"
              />
            )}

            <TouchableOpacity
              style={[styles.submitBtn, saving && styles.submitBtnDisabled]}
              onPress={handleUpload}
              disabled={saving}
            >
              {saving
                ? <ActivityIndicator color="white" />
                : <Text style={styles.submitText}>Upload</Text>}
            </TouchableOpacity>
          </ScrollView>
        </SafeAreaView>
      </Modal>

      {/* ── Edit modal ───────────────────────────────────────────────────── */}
      <Modal
        visible={!!editTarget}
        animationType="slide"
        presentationStyle="formSheet"
        onRequestClose={() => setEditTarget(null)}
      >
        <SafeAreaView style={styles.modal}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Rediger indhold</Text>
            <TouchableOpacity onPress={() => setEditTarget(null)} hitSlop={12}>
              <Text style={styles.closeBtn}>✕</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalBody} keyboardShouldPersistTaps="handled">
            <FormField
              label="Titel *"
              value={editForm.title}
              onChange={t => setEditForm(f => ({ ...f, title: t }))}
            />
            <FormField
              label="Beskrivelse"
              value={editForm.description}
              onChange={t => setEditForm(f => ({ ...f, description: t }))}
              multiline
            />
            <View style={styles.switchRow}>
              <Text style={styles.fieldLabel}>Publiceret</Text>
              <Switch
                value={editForm.isPublished}
                onValueChange={v => setEditForm(f => ({ ...f, isPublished: v }))}
                trackColor={{ false: '#d1d5db', true: '#0ea5e9' }}
                thumbColor="white"
              />
            </View>

            <TouchableOpacity
              style={[styles.submitBtn, saving && styles.submitBtnDisabled]}
              onPress={handleEdit}
              disabled={saving}
            >
              {saving
                ? <ActivityIndicator color="white" />
                : <Text style={styles.submitText}>Gem ændringer</Text>}
            </TouchableOpacity>
          </ScrollView>
        </SafeAreaView>
      </Modal>

      {/* ── Fuld skærm billede modal ─────────────────────────────────────── */}
      <Modal
        visible={!!viewingImage}
        animationType="fade"
        presentationStyle="fullScreen"
        onRequestClose={() => setViewingImage(null)}
      >
        <SafeAreaView style={styles.imageViewerModal} edges={['top', 'bottom']}>
          <TouchableOpacity style={styles.closePlayer} onPress={() => setViewingImage(null)}>
            <Text style={styles.closePlayerText}>✕  Luk</Text>
          </TouchableOpacity>
          {viewingImage?.imageUrl ? (
            <Image
              source={{ uri: viewingImage.imageUrl }}
              style={styles.imageViewerFull}
              resizeMode="contain"
            />
          ) : null}
          {viewingImage?.title ? (
            <Text style={styles.imageViewerTitle}>{viewingImage.title}</Text>
          ) : null}
        </SafeAreaView>
      </Modal>

      {/* ── Video player modal ───────────────────────────────────────────── */}
      <Modal
        visible={playingVideoIndex !== null}
        animationType="slide"
        presentationStyle="fullScreen"
        onRequestClose={() => setPlayingVideoIndex(null)}
      >
        <SafeAreaView style={styles.playerModal} edges={['top']}>
          <TouchableOpacity style={styles.closePlayer} onPress={() => setPlayingVideoIndex(null)}>
            <Text style={styles.closePlayerText}>✕  Luk</Text>
          </TouchableOpacity>
          {playingVideoIndex !== null && (
            <VideoPlayer videos={videos} startIndex={playingVideoIndex} />
          )}
        </SafeAreaView>
      </Modal>
    </SafeAreaView>
  );
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function TabBtn({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <TouchableOpacity style={[styles.tabBtn, active && styles.tabBtnActive]} onPress={onPress}>
      <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>{label}</Text>
    </TouchableOpacity>
  );
}

function Center({ children }: { children: React.ReactNode }) {
  return <View style={styles.center}>{children}</View>;
}

function FormField({
  label, value, onChange, placeholder, multiline,
}: {
  label: string;
  value: string;
  onChange: (t: string) => void;
  placeholder?: string;
  multiline?: boolean;
}) {
  return (
    <View style={styles.fieldWrap}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        style={[styles.input, multiline && styles.inputMulti]}
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor="#9ca3af"
        multiline={multiline}
        numberOfLines={multiline ? 3 : 1}
        textAlignVertical={multiline ? 'top' : 'center'}
      />
    </View>
  );
}

function StatusBadge({ published }: { published: boolean | null }) {
  return (
    <View style={[styles.badge, published ? styles.badgeGreen : styles.badgeGray]}>
      <Text style={[styles.badgeText, published ? styles.badgeTextGreen : styles.badgeTextGray]}>
        {published ? 'Publiceret' : 'Kladde'}
      </Text>
    </View>
  );
}

function ImageCard({
  item, onView, onEdit, onDelete,
}: { item: ImageItem; onView: () => void; onEdit: () => void; onDelete: () => void }) {
  return (
    <View style={styles.card}>
      <TouchableOpacity onPress={onView} activeOpacity={0.8}>
        {item.imageUrl ? (
          <Image source={{ uri: item.imageUrl }} style={styles.cardThumb} resizeMode="cover" />
        ) : (
          <View style={[styles.cardThumb, styles.thumbEmpty]}>
            <Text style={styles.muted}>–</Text>
          </View>
        )}
      </TouchableOpacity>
      <View style={styles.cardBody}>
        <Text style={styles.cardTitle} numberOfLines={1}>{item.title ?? 'Uden titel'}</Text>
        {item.description ? (
          <Text style={styles.cardDesc} numberOfLines={2}>{item.description}</Text>
        ) : null}
        <StatusBadge published={item.isPublished} />
      </View>
      <View style={styles.cardActions}>
        <TouchableOpacity style={styles.actionBtn} onPress={onEdit}>
          <Text style={styles.actionBtnText}>Rediger</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.actionBtn, styles.actionBtnRed]} onPress={onDelete}>
          <Text style={[styles.actionBtnText, styles.actionBtnTextRed]}>Slet</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

function VideoCard({
  item, onPlay, onEdit, onDelete,
}: { item: VideoItem; onPlay: () => void; onEdit: () => void; onDelete: () => void }) {
  return (
    <View style={styles.videoCard}>
      <TouchableOpacity onPress={onPlay} activeOpacity={0.85} style={styles.videoThumbWrap}>
        {item.thumbnail ? (
          <Image source={{ uri: item.thumbnail }} style={styles.videoThumb} resizeMode="cover" />
        ) : (
          <View style={[styles.videoThumb, styles.thumbEmpty]} />
        )}
        <View style={styles.playOverlay}>
          <View style={styles.playCircle}>
            <Text style={styles.playIcon}>▶</Text>
          </View>
          {item.duration != null && (
            <View style={styles.durationBadge}>
              <Text style={styles.durationText}>{formatDuration(item.duration)}</Text>
            </View>
          )}
        </View>
      </TouchableOpacity>

      <View style={styles.videoCardBody}>
        <Text style={styles.cardTitle} numberOfLines={1}>{item.title ?? 'Uden titel'}</Text>
        {item.description ? (
          <Text style={styles.cardDesc} numberOfLines={2}>{item.description}</Text>
        ) : null}
        <View style={styles.videoCardFooter}>
          <StatusBadge published={item.isPublished} />
          <View style={styles.videoCardActions}>
            <TouchableOpacity style={styles.actionBtn} onPress={onEdit}>
              <Text style={styles.actionBtnText}>Rediger</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.actionBtn, styles.actionBtnRed]} onPress={onDelete}>
              <Text style={[styles.actionBtnText, styles.actionBtnTextRed]}>Slet</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </View>
  );
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

// ─── Styles ──────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#f3f4f6' },

  tabBar: {
    flexDirection: 'row',
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  tabBtn: {
    flex: 1,
    paddingVertical: 13,
    alignItems: 'center',
    borderBottomWidth: 3,
    borderBottomColor: 'transparent',
  },
  tabBtnActive: { borderBottomColor: '#0ea5e9' },
  tabLabel: { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  tabLabelActive: { color: '#0ea5e9' },

  list: { padding: 12, paddingBottom: 90 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  muted: { fontSize: 12, color: '#9ca3af' },
  emptyText: { textAlign: 'center', color: '#9ca3af', marginTop: 48, fontSize: 15 },
  errorText: { color: '#991b1b', textAlign: 'center', marginBottom: 12, fontSize: 14 },
  retryBtn: { paddingHorizontal: 20, paddingVertical: 10, backgroundColor: '#0ea5e9', borderRadius: 8 },
  retryText: { color: 'white', fontWeight: '700' },

  // Image card
  card: {
    flexDirection: 'row',
    backgroundColor: 'white',
    borderRadius: 12,
    marginBottom: 10,
    overflow: 'hidden',
    elevation: 2,
    shadowColor: '#000',
    shadowOpacity: 0.07,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  cardThumb: { width: 88, height: 88, backgroundColor: '#e5e7eb' },
  thumbEmpty: { alignItems: 'center', justifyContent: 'center' },
  cardBody: { flex: 1, padding: 10, justifyContent: 'center' },
  cardTitle: { fontSize: 14, fontWeight: '700', color: '#111827', marginBottom: 3 },
  cardDesc: { fontSize: 12, color: '#6b7280', marginBottom: 6 },
  cardActions: { justifyContent: 'center', paddingRight: 10, gap: 6 },

  // Video card
  videoCard: {
    backgroundColor: 'white',
    borderRadius: 12,
    marginBottom: 14,
    overflow: 'hidden',
    elevation: 2,
    shadowColor: '#000',
    shadowOpacity: 0.07,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  videoThumbWrap: { width: '100%', aspectRatio: 16 / 9, backgroundColor: '#111827' },
  videoThumb: { width: '100%', height: '100%' },
  playOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playCircle: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(0,0,0,0.55)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  playIcon: { fontSize: 20, color: 'white', marginLeft: 3 },
  durationBadge: {
    position: 'absolute',
    bottom: 8,
    right: 8,
    backgroundColor: 'rgba(0,0,0,0.65)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  durationText: { color: 'white', fontSize: 11, fontWeight: '700' },
  videoCardBody: { padding: 10 },
  videoCardFooter: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 },
  videoCardActions: { flexDirection: 'row', gap: 6 },

  // Shared
  badge: { paddingHorizontal: 7, paddingVertical: 3, borderRadius: 5, alignSelf: 'flex-start' },
  badgeGreen: { backgroundColor: '#dcfce7' },
  badgeGray: { backgroundColor: '#f3f4f6' },
  badgeText: { fontSize: 10, fontWeight: '700' },
  badgeTextGreen: { color: '#166534' },
  badgeTextGray: { color: '#6b7280' },
  actionBtn: {
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 7,
    borderWidth: 1.5,
    borderColor: '#0ea5e9',
  },
  actionBtnText: { fontSize: 11, fontWeight: '700', color: '#0ea5e9' },
  actionBtnRed: { borderColor: '#ef4444' },
  actionBtnTextRed: { color: '#ef4444' },

  // FAB
  fab: {
    position: 'absolute',
    bottom: 20,
    right: 20,
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: '#0ea5e9',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 6,
    shadowColor: '#0ea5e9',
    shadowOpacity: 0.4,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 4 },
  },
  fabText: { fontSize: 30, color: 'white', lineHeight: 34 },

  // Modals
  modal: { flex: 1, backgroundColor: 'white' },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  modalTitle: { fontSize: 17, fontWeight: '700', color: '#111827' },
  closeBtn: { fontSize: 20, color: '#6b7280', paddingHorizontal: 4 },
  modalBody: { flex: 1, paddingHorizontal: 16, paddingTop: 16 },

  // Form
  fieldWrap: { marginBottom: 16 },
  fieldLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 11,
    fontSize: 15,
    color: '#111827',
    backgroundColor: '#fafafa',
  },
  inputMulti: { minHeight: 90 },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
    paddingVertical: 4,
  },
  preview: {
    width: '100%',
    height: 200,
    borderRadius: 10,
    marginBottom: 12,
    backgroundColor: '#e5e7eb',
  },
  videoPreviewBox: {
    backgroundColor: '#1f2937',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  videoPreviewIcon: { fontSize: 40 },
  videoPreviewName: { color: '#d1d5db', fontSize: 13, paddingHorizontal: 16 },
  pickBtn: {
    borderWidth: 1.5,
    borderColor: '#0ea5e9',
    borderStyle: 'dashed',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 20,
    backgroundColor: '#f0f9ff',
  },
  pickBtnText: { fontSize: 15, fontWeight: '600', color: '#0ea5e9' },
  submitBtn: {
    backgroundColor: '#0ea5e9',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 4,
    marginBottom: 32,
  },
  submitBtnDisabled: { backgroundColor: '#bae6fd' },
  submitText: { color: 'white', fontWeight: '700', fontSize: 16 },

  // Image viewer modal
  imageViewerModal: { flex: 1, backgroundColor: '#000' },
  imageViewerFull: { flex: 1, width: '100%' },
  imageViewerTitle: { color: 'white', fontSize: 15, fontWeight: '600', padding: 16, textAlign: 'center' },

  // Video player modal
  playerModal: { flex: 1, backgroundColor: '#000' },
  closePlayer: { paddingHorizontal: 16, paddingVertical: 12 },
  closePlayerText: { color: 'white', fontSize: 16, fontWeight: '600' },
});