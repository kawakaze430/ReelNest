'use client';

import { useAuth } from 'react-oidc-context';
import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';

type PlaybackSession = {
  id: string;
  profileId: string;
  createdAt: string;
  lastHeartbeat: string;
  expiresAt: string;
  status: string;
};
type Revocation = { affected: number; scope: string };

/** Playback sessions, not a complete inventory of Keycloak SSO/browser devices. */
export default function AccountSessionsPage() {
  const auth = useAuth();
  const token = auth.user?.access_token;
  const [sessions, setSessions] = useState<PlaybackSession[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    apiFetch<PlaybackSession[]>('/api/v1/me/sessions', token)
      .then(rows => { if (!cancelled) setSessions(rows); })
      .catch(() => { if (!cancelled) setError('Cannot load playback sessions.'); });
    return () => { cancelled = true; };
  }, [token]);

  async function revoke(id: string) {
    if (!token) return;
    setBusy(true); setError('');
    try {
      await apiFetch<void>(`/api/v1/me/sessions/${encodeURIComponent(id)}`, token, { method: 'DELETE' });
      setSessions(old => old.map(s => s.id === id ? { ...s, status: 'REVOKED' } : s));
      setNotice('Playback session revoked.');
    } catch { setError('Failed to revoke the selected playback session.'); }
    finally { setBusy(false); }
  }

  async function revokeAll() {
    if (!token || !window.confirm('Revoke all ReelNest API sessions and sign out? Your identity-provider browser session must also be closed.')) return;
    setBusy(true); setError('');
    try {
      const result = await apiFetch<Revocation>('/api/v1/me/sessions/revoke-all', token, { method: 'POST' });
      setNotice(`${result.affected} playback sessions revoked. Signing out of the identity provider.`);
      setSessions([]);
      await auth.signoutRedirect();
    } catch { setError('Session revocation or identity-provider sign-out could not complete. Check your account before continuing.'); }
    finally { setBusy(false); }
  }

  return <main className="wrap viewer-shell" style={{maxWidth:800}}>
    <div className="eyebrow">REELNEST / ACCOUNT SECURITY</div>
    <h1>Playback sessions<span className="title-period">.</span></h1>
    <p>Review your recent ReelNest streaming sessions. This does not list every Keycloak sign-in or other browser session.</p>
    {!token ? <button className="btn btn-primary" onClick={() => void auth.signinRedirect()}>Sign in</button> : <>
      {error && <p role="alert" className="notice">{error}</p>}
      {notice && <p role="status" className="notice">{notice}</p>}
      <div style={{display:'flex',justifyContent:'flex-end',margin:'16px 0'}}>
        <button className="btn btn-outline" disabled={busy} onClick={() => void revokeAll()}>Revoke all and sign out</button>
      </div>
      {sessions.length === 0 ? <p>No recent playback sessions were found.</p> : <ul style={{display:'grid',gap:16,listStyle:'none',padding:0}}>
        {sessions.map(session => <li key={session.id} className="notice" style={{display:'flex',justifyContent:'space-between',gap:12,alignItems:'center'}}>
          <div><strong>{session.status}</strong><div>Started: {new Date(session.createdAt).toLocaleString()}</div><div>Last activity: {new Date(session.lastHeartbeat).toLocaleString()}</div></div>
          {session.status === 'ACTIVE' && <button className="btn btn-outline" disabled={busy} onClick={() => void revoke(session.id)}>Revoke</button>}
        </li>)}
      </ul>}
    </>}
  </main>;
}
