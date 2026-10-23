/** Playback-only UI decisions. These values never grant access to media. */
export type PlaybackCues = {
  introStartSeconds: number | null;
  introEndSeconds: number | null;
  recapStartSeconds: number | null;
  recapEndSeconds: number | null;
  creditsStartSeconds: number | null;
};
export type SkipAction = { label: 'Skip Intro' | 'Skip Recap' | 'Skip Credits'; targetSeconds: number };

export function skipAction(cues: PlaybackCues | undefined, position: number, duration: number): SkipAction | null {
  if (!cues || !Number.isFinite(position) || !Number.isFinite(duration) || duration <= 0) return null;
  const range = (start: number | null, end: number | null, label: SkipAction['label']): SkipAction | null =>
    start !== null && end !== null && Number.isInteger(start) && Number.isInteger(end) &&
    start >= 0 && end > start && end <= duration && position >= start && position < end
      ? { label, targetSeconds: end } : null;
  return range(cues.introStartSeconds, cues.introEndSeconds, 'Skip Intro') ??
    range(cues.recapStartSeconds, cues.recapEndSeconds, 'Skip Recap') ??
    (cues.creditsStartSeconds !== null && Number.isInteger(cues.creditsStartSeconds) &&
      cues.creditsStartSeconds >= 0 && cues.creditsStartSeconds < duration &&
      position >= cues.creditsStartSeconds && position < duration - 1
      ? { label: 'Skip Credits', targetSeconds: Math.max(0, duration - 0.5) } : null);
}

export function boundedElapsedMs(startMs: number, endMs: number): number | null {
  if (!Number.isFinite(startMs) || !Number.isFinite(endMs) || endMs < startMs) return null;
  return Math.min(3_600_000, Math.round(endMs - startMs));
}

export function telemetryErrorCode(detail: string): string {
  return detail.toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 32) || 'UNKNOWN';
}

/** At most two attempts. Authentication/entitlement errors must never be retried blindly. */
export function hlsRecoveryAction(
  type: string, httpStatus: number | undefined, attempts: number
): 'network' | 'media' | 'stop' {
  if ([401, 402, 403, 404].includes(httpStatus ?? 0) || attempts >= 2) return 'stop';
  if (type === 'networkError') return 'network';
  if (type === 'mediaError') return 'media';
  return 'stop';
}
