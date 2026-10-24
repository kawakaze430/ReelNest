import type { PlaybackCues } from './playback-enhancements';

export type PlaybackSession = {
  sessionId: string;
  titleId: string;
  manifestUrl: string;
  resumePositionSeconds: number;
  durationSeconds: number;
  expiresAt: string;
  heartbeatIntervalSeconds: number;
  cues: PlaybackCues;
  visibleWatermarkRequired: boolean;
};

export type ContinueItem = {
  titleId: string;
  name: string;
  genre: string;
  artTone: string;
  ageRating: string;
  mediaAssetId: string;
  positionSeconds: number;
  durationSeconds: number;
  updatedAt: string;
};

export function prettyTime(sec: number): string {
  if (!Number.isFinite(sec) || sec < 0) return '0:00';
  const seconds = Math.floor(sec);
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}

export function playbackMessage(status: number): string {
  switch (status) {
    case 401: return 'Please sign in again to continue watching.';
    case 402: return 'This title requires a paid entitlement. You need an active, verified subscription.';
    case 403: return 'Playback is restricted for this profile or your session has expired.';
    case 404: return 'There is no approved, licensed video available for this title in your region.';
    case 409: return 'Your account has reached its active stream limit, or playback is out of date.';
    default: return 'Playback is unavailable. Check that the API, media worker and MinIO are running.';
  }
}
