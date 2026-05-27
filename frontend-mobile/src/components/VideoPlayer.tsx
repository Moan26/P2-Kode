import React, { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { VideoView, useVideoPlayer } from 'expo-video';
import { VideoItem } from '../services/content';

interface VttCue {
    startMs: number;
    endMs: number;
    text: string;
}

function timeToMs(t: string): number {
    const parts = t.split(':');
    let h = 0, m = 0, s = 0;
    if (parts.length === 3) {
        h = parseInt(parts[0]);
        m = parseInt(parts[1]);
        s = parseFloat(parts[2]);
    } else {
        m = parseInt(parts[0]);
        s = parseFloat(parts[1]);
    }
    return (h * 3600 + m * 60 + s) * 1000;
}

function parseVtt(text: string): VttCue[] {
    const cues: VttCue[] = [];
    for (const block of text.split('\n\n')) {
        const lines = block.trim().split('\n');
        const timeLine = lines.find(l => l.includes('-->'));
        if (!timeLine) continue;
        const [startStr, endStr] = timeLine.split('-->').map(s => s.trim());
        const textLines = lines.filter(
            l => !l.includes('-->') && !/^\d+$/.test(l) && l !== 'WEBVTT',
        );
        if (textLines.length > 0) {
            cues.push({ startMs: timeToMs(startStr), endMs: timeToMs(endStr),
                text: textLines.join('\n') });
        }
    }
    return cues;
}

interface Props {
    videos: VideoItem[];
    startIndex?: number;
}

export default function VideoPlayer({ videos, startIndex = 0 }: Props) {
    const [index, setIndex] = useState(startIndex);
    const [cues, setCues] = useState<VttCue[]>([]);
    const [currentCaption, setCurrentCaption] = useState<string | null>(null);
    const [loadError, setLoadError] = useState<string | null>(null);

    const current = videos[index];

    const player = useVideoPlayer(current?.watchUrl ?? null, p => {
        p.timeUpdateEventInterval = 0.25;
    });

    const prevIndexRef = useRef(index);
    useEffect(() => {
        if (prevIndexRef.current !== index) {
            prevIndexRef.current = index;
            setLoadError(null);
            if (current?.watchUrl) {
                player.replace(current.watchUrl);
            }
        }
    }, [index, current?.watchUrl, player]);

    useEffect(() => {
        const sub = player.addListener('statusChange', ({ status, error }) => {
            if (status === 'error' && error) {
                setLoadError(error.message ?? 'Video kunne ikke afspilles');
            }
        });
        return () => sub.remove();
    }, [player]);

    useEffect(() => {
        const sub = player.addListener('timeUpdate', ({ currentTime }) => {
            const posMs = currentTime * 1000;
            const cue = cues.find(c => posMs >= c.startMs && posMs <= c.endMs);
            setCurrentCaption(cue?.text ?? null);
        });
        return () => sub.remove();
    }, [player, cues]);

    useEffect(() => {
        setCues([]);
        setCurrentCaption(null);
        if (!current?.captions) return;
        fetch(current.captions)
            .then(r => r.text())
            .then(text => setCues(parseVtt(text)))
            .catch(() => {});
    }, [current?.captions]);

    const goTo = useCallback((newIndex: number) => {
        setIndex(newIndex);
        setCurrentCaption(null);
        setLoadError(null);
    }, []);

    const hasPrev = index > 0;
    const hasNext = index < videos.length - 1;

    if (!current) return <View />;

    return (
        <View style={styles.container}>
            <View style={styles.videoWrapper}>
                <VideoView
                    player={player}
                    style={styles.video}
                    contentFit="contain"
                    nativeControls
                    allowsFullscreen
                />
                {currentCaption ? (
                    <View style={styles.captionBox}>
                        <Text style={styles.captionText}>{currentCaption}</Text>
                    </View>
                ) : null}
            </View>

            {loadError ? (
                <Text style={styles.errorText}>Fejl: {loadError}</Text>
            ) : null}

            <Text style={styles.title}>{current.title ?? 'Untitled'}</Text>
            {current.description ? (
                <Text style={styles.desc}>{current.description}</Text>
            ) : null}

            <View style={styles.nav}>
                <TouchableOpacity
                    style={[styles.navBtn, !hasPrev && styles.navBtnDisabled]}
                    onPress={() => hasPrev && goTo(index - 1)}
                    disabled={!hasPrev}
                >
                    <Text style={styles.navBtnText}>← Forrige</Text>
                </TouchableOpacity>

                <Text style={styles.counter}>{index + 1} / {videos.length}</Text>

                <TouchableOpacity
                    style={[styles.navBtn, !hasNext && styles.navBtnDisabled]}
                    onPress={() => hasNext && goTo(index + 1)}
                    disabled={!hasNext}
                >
                    <Text style={styles.navBtnText}>Næste →</Text>
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f9fafb' },
    videoWrapper: { backgroundColor: '#000', aspectRatio: 16 / 9 },
    video: { width: '100%', height: '100%' },
    captionBox: {
        position: 'absolute',
        bottom: 8,
        left: 8,
        right: 8,
        alignItems: 'center',
    },
    captionText: {
        backgroundColor: 'rgba(0,0,0,0.65)',
        color: 'white',
        fontSize: 14,
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 4,
        textAlign: 'center',
    },
    errorText: { color: '#dc2626', fontSize: 12, paddingHorizontal: 12, paddingTop: 4 },
    title: { fontSize: 16, fontWeight: '700', padding: 12, color: '#111827' },
    desc: { fontSize: 13, color: '#4b5563', paddingHorizontal: 12, marginBottom: 8 },
    nav: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 12,
        paddingVertical: 10,
        borderTopWidth: 1,
        borderTopColor: '#e5e7eb',
    },
    navBtn: {
        backgroundColor: '#0ea5e9',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 8,
    },
    navBtnDisabled: { backgroundColor: '#cbd5e1' },
    navBtnText: { color: 'white', fontWeight: '700', fontSize: 14 },
    counter: { fontSize: 14, color: '#6b7280', fontWeight: '600' },
});