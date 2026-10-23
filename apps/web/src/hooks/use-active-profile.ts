'use client';

import { useAuth } from 'react-oidc-context';
import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';
import type { Profile } from '@/types/catalog';

export const PROFILE_EVENT = 'reelnest:profile-changed';

export function useActiveProfile() {
  const auth = useAuth();
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [profileId, setProfileId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const token = auth.user?.access_token;
  const key = `reelnest:active-profile:${auth.user?.profile.sub ?? 'anonymous'}`;

  useEffect(() => {
    if (!token) {
      setProfiles([]); setProfileId(''); setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true); setError('');
    apiFetch<Profile[]>('/api/v1/profiles', token)
      .then(data => {
        if (cancelled) return;
        setProfiles(data);
        const saved = window.localStorage.getItem(key);
        const selected = data.some(p => p.id === saved) ? saved! : (data[0]?.id ?? '');
        setProfileId(selected);
        if (selected) window.localStorage.setItem(key, selected);
      })
      .catch(() => { if (!cancelled) setError('Unable to load viewer profiles.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [token, key]);

  useEffect(() => {
    const sync = () => setProfileId(window.localStorage.getItem(key) ?? '');
    window.addEventListener(PROFILE_EVENT, sync);
    return () => window.removeEventListener(PROFILE_EVENT, sync);
  }, [key]);

  function select(id: string) {
    if (!profiles.some(p => p.id === id)) return;
    window.localStorage.setItem(key, id);
    setProfileId(id);
    window.dispatchEvent(new Event(PROFILE_EVENT));
  }
  return { auth, token, profiles, profileId, active: profiles.find(p=>p.id===profileId), select, loading, error };
}
