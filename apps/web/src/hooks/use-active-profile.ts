'use client';

import { useAuth } from 'react-oidc-context';
import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';
import type { Profile } from '@/types/catalog';

export const PROFILE_EVENT = 'reelnest:profile-changed';

type ProfileResolution = {
  token: string;
  key: string;
  profiles: Profile[];
  profileId: string;
  error: string;
};

export function useActiveProfile() {
  const auth = useAuth();
  const [resolution, setResolution] = useState<ProfileResolution|null>(null);
  const token = auth.isAuthenticated ? auth.user?.access_token : undefined;
  const key = `reelnest:active-profile:${auth.user?.profile.sub ?? 'anonymous'}`;
  // Results from an earlier identity/token are never rendered while the next
  // request resolves. Loading and empty state follow identity, not effect timing.
  const current = token && resolution?.token === token && resolution.key === key ? resolution : null;
  const profiles = current?.profiles ?? [];
  const profileId = current?.profileId ?? '';
  const loading = auth.isLoading || Boolean(token && !current);
  const error = current?.error ?? '';

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    const controller = new AbortController();
    apiFetch<Profile[]>('/api/v1/profiles', token, {signal: controller.signal})
      .then(data => {
        if (cancelled) return;
        let saved: string|null = null;
        try { saved = window.localStorage.getItem(key); } catch { /* Storage may be disabled. */ }
        const selected = data.some(p => p.id === saved) ? saved! : (data[0]?.id ?? '');
        setResolution({token,key,profiles:data,profileId:selected,error:''});
        try { if (selected) window.localStorage.setItem(key, selected); } catch { /* Selection stays usable in memory. */ }
      })
      .catch(() => { if (!cancelled) setResolution({token,key,profiles:[],profileId:'',error:'Unable to load viewer profiles.'}); });
    return () => { cancelled = true; controller.abort(); };
  }, [token, key]);

  useEffect(() => {
    const sync = () => {
      let saved: string|null = null;
      try { saved = window.localStorage.getItem(key); } catch { return; }
      setResolution(previous => previous?.key === key && previous.token === token
        ? {...previous,profileId:previous.profiles.some(p => p.id === saved) ? saved! : ''}
        : previous);
    };
    window.addEventListener(PROFILE_EVENT, sync);
    window.addEventListener('storage', sync);
    return () => { window.removeEventListener(PROFILE_EVENT, sync); window.removeEventListener('storage', sync); };
  }, [key, token]);

  function select(id: string) {
    if (!profiles.some(p => p.id === id)) return;
    try { window.localStorage.setItem(key, id); } catch { /* Selection stays usable in memory. */ }
    setResolution(previous => previous?.key === key && previous.token === token ? {...previous,profileId:id} : previous);
    window.dispatchEvent(new Event(PROFILE_EVENT));
  }
  return { auth, token, profiles, profileId, active: profiles.find(p=>p.id===profileId), select, loading, error };
}
