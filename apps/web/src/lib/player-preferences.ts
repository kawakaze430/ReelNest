/** Device-local, per-profile playback preferences. No auth tokens or viewing history are persisted. */
export type PlayerPreferences = {
  speed: number;
  captionSize: 'small' | 'normal' | 'large';
  captionContrast: 'standard' | 'high';
  preferredAudio: string;
  preferredSubtitles: string;
  dataSaver: boolean;
  qoeConsent: boolean;
  autoplayNext: boolean;
};
export const DEFAULT_PLAYER_PREFERENCES: PlayerPreferences = Object.freeze({
  speed: 1,
  captionSize: 'normal',
  captionContrast: 'standard',
  preferredAudio: '',
  preferredSubtitles: '',
  dataSaver: false,
  qoeConsent: false,
  autoplayNext: false,
});
const SUPPORTED_SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2] as const;
const SIZE = ['small','normal','large'] as const;
const CONTRAST = ['standard','high'] as const;
export function normalizeLanguage(value: unknown): string {
  return typeof value === 'string' && /^[a-zA-Z]{2,3}(?:-[a-zA-Z]{2,8})?$/.test(value)
    ? value.toLowerCase() : '';
}
export function cleanPreferences(value: unknown): PlayerPreferences {
  const v = value && typeof value === 'object' ? value as Record<string, unknown> : {};
  return {
    speed: typeof v.speed === 'number' && SUPPORTED_SPEEDS.includes(v.speed as typeof SUPPORTED_SPEEDS[number]) ? v.speed : 1,
    captionSize: typeof v.captionSize === 'string' && (SIZE as readonly string[]).includes(v.captionSize) ? v.captionSize as PlayerPreferences['captionSize'] : 'normal',
    captionContrast: typeof v.captionContrast === 'string' && (CONTRAST as readonly string[]).includes(v.captionContrast) ? v.captionContrast as PlayerPreferences['captionContrast'] : 'standard',
    preferredAudio: normalizeLanguage(v.preferredAudio),
    preferredSubtitles: normalizeLanguage(v.preferredSubtitles),
    dataSaver: v.dataSaver === true,
    qoeConsent: v.qoeConsent === true,
    autoplayNext: v.autoplayNext === true,
  };
}
export function prefsKey(profileId: string): string {
  // UUID only. Avoid arbitrary keys/large user-controlled localStorage paths.
  if (!/^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(profileId))
    throw new Error('Invalid profile identifier');
  return `reelnest:player:v1:${profileId.toLowerCase()}`;
}
export function loadPreferences(profileId: string): PlayerPreferences {
  if (typeof window === 'undefined') return {...DEFAULT_PLAYER_PREFERENCES};
  try {
    const raw = window.localStorage.getItem(prefsKey(profileId));
    return raw ? cleanPreferences(JSON.parse(raw) as unknown) : {...DEFAULT_PLAYER_PREFERENCES};
  } catch { return {...DEFAULT_PLAYER_PREFERENCES}; }
}
export function savePreferences(profileId: string, value: PlayerPreferences): void {
  if (typeof window === 'undefined') return;
  try { window.localStorage.setItem(prefsKey(profileId), JSON.stringify(cleanPreferences(value))); }
  catch { /* privacy settings or storage quota may block device-local preferences */ }
}
export type TrackOption = { id: number; label: string; language: string };
export function matchingTrack(tracks: TrackOption[], language: string): number {
  const preferred = normalizeLanguage(language);
  if (!preferred) return -1;
  const direct = tracks.find(t => normalizeLanguage(t.language) === preferred);
  if (direct) return direct.id;
  return tracks.find(t => normalizeLanguage(t.language).split('-')[0] === preferred.split('-')[0])?.id ?? -1;
}
/** Select the highest rendition at or below the data-saver cap. -1 means no playable match. */
export function cappedQualityIndex(heights: number[], maxHeight = 480): number {
  let best = -1;
  for (let i = 0; i < heights.length; i++) {
    if (Number.isFinite(heights[i]) && heights[i] > 0 && heights[i] <= maxHeight && (best < 0 || heights[i] > heights[best])) best = i;
  }
  if (best >= 0) return best;
  return heights.length ? heights.reduce((lowest, height, idx) => height < heights[lowest] ? idx : lowest, 0) : -1;
}
export function clampSeek(currentTime: number, duration: number, difference: number): number {
  if (!Number.isFinite(currentTime) || !Number.isFinite(difference)) return 0;
  const end = Number.isFinite(duration) && duration >= 0 ? duration : Infinity;
  return Math.max(0, Math.min(end, currentTime + difference));
}
