'use client';

import { useAuth } from 'react-oidc-context';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Plus, Trash2, UserRound, LogIn, LoaderCircle } from 'lucide-react';
import { apiFetch } from '@/lib/api';
import { PROFILE_EVENT } from '@/hooks/use-active-profile';
import type { Profile } from '@/types/catalog';
import {DeviceHandoff} from '@/components/device-handoff';

export function ProfileManager() {
  const auth = useAuth();
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [name, setName] = useState('');
  const [language, setLanguage] = useState<'en' | 'hi'>('en');
  const [child, setChild] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [pinInput,setPinInput] = useState<Record<string,string>>({});
  const [newPin,setNewPin] = useState<Record<string,string>>({});
  const [pinStatus,setPinStatus] = useState<Record<string,boolean>>({});
  const [personalization,setPersonalization] = useState<Record<string,boolean>>({});
  const [selectedProfile,setSelectedProfile] = useState('');
  const token = auth.user?.access_token;
  useEffect(()=>{
    const key=`reelnest:active-profile:${auth.user?.profile.sub??'anonymous'}`;
    const sync=()=>setSelectedProfile(window.localStorage.getItem(key)??'');
    sync();
    window.addEventListener(PROFILE_EVENT,sync);
    return()=>window.removeEventListener(PROFILE_EVENT,sync);
  },[auth.user?.profile.sub]);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    apiFetch<Profile[]>('/api/v1/profiles', token)
      .then(data => { if (!cancelled) { setProfiles(data); Promise.all(data.filter(p=>!p.child).map(async p=>{try {const state=await apiFetch<{configured:boolean}>(`/api/v1/profiles/${p.id}/pin`,token);return [p.id,state.configured] as const;}catch{return [p.id,false] as const;}})).then(rows=>{if(!cancelled)setPinStatus(Object.fromEntries(rows));});} })
      .catch(() => { if (!cancelled) setError('Could not load your profiles. Check the API and Keycloak services.'); });
    return () => { cancelled = true; };
  }, [token]);

  useEffect(()=>{
    if(!token||profiles.length===0)return;
    let cancelled=false;
    Promise.all(profiles.map(async p=>{
      try{const value=await apiFetch<{enabled:boolean}>(`/api/v1/profiles/${p.id}/personalization`,token);return [p.id,value.enabled] as const;}
      catch{return [p.id,false] as const;}
    })).then(rows=>{if(!cancelled)setPersonalization(Object.fromEntries(rows));});
    return ()=>{cancelled=true;};
  },[token,profiles]);
  async function togglePersonalization(id:string){
    if(!token)return; const next=!personalization[id];setBusy(true);setError('');
    try{const value=await apiFetch<{enabled:boolean}>(`/api/v1/profiles/${id}/personalization`,token,
      {method:'PUT',body:JSON.stringify({enabled:next})});
      setPersonalization(old=>({...old,[id]:value.enabled}));
    }catch(e){setError(e instanceof Error?e.message:'Cannot update personalization');}
    finally{setBusy(false);}
  }
  async function create(e: React.FormEvent) {
    e.preventDefault(); if (!token || !name.trim()) return;
    setBusy(true); setError('');
    try {
      const p = await apiFetch<Profile>('/api/v1/profiles', token, { method:'POST', body: JSON.stringify({name:name.trim(),preferredLanguage:language,child}) });
      setProfiles(old=>[...old,p]); setName(''); setChild(false);
    } catch { setError('Could not create profile. Check your connection and the five-profile limit.'); }
    finally { setBusy(false); }
  }
  async function savePin(id:string){
    if(!token)return;setBusy(true);setError('');
    try { await apiFetch(`/api/v1/profiles/${id}/pin`,token,{method:'PUT',body:JSON.stringify({newPin:newPin[id]||'',currentPin:pinInput[id]||null})});
      setPinStatus(p=>({...p,[id]:true}));setNewPin(p=>({...p,[id]:''}));setPinInput(p=>({...p,[id]:''}));
    } catch(e){setError(e instanceof Error?e.message:'Could not set PIN');}finally{setBusy(false);} }
  async function remove(id: string) {
    if (!token || !window.confirm('Remove this profile?')) return;
    setBusy(true); setError('');
    try {
      await apiFetch<void>(`/api/v1/profiles/${id}`, token, {
        method:'DELETE', headers:pinInput[id]?{'X-Profile-Pin':pinInput[id]}:{}
      });
      const remaining=profiles.filter(p=>p.id!==id);
      setProfiles(remaining);
      // Deleted profiles must not remain selected in another viewer component.
      const selectionKey=`reelnest:active-profile:${auth.user?.profile.sub ?? 'anonymous'}`;
      if(window.localStorage.getItem(selectionKey)===id) {
        if(remaining.length) window.localStorage.setItem(selectionKey,remaining[0].id);
        else window.localStorage.removeItem(selectionKey);
        window.dispatchEvent(new Event(PROFILE_EVENT));
      }
      setPinStatus(old=>{const next={...old};delete next[id];return next;});
    } catch(e) {
      // Explain authorization/lockout denials instead of showing an unhelpful generic error.
      setError(e instanceof Error?e.message:'Could not remove the profile.');
    } finally {
      // Never keep a submitted PIN in component state after an authorization attempt.
      setPinInput(old=>({...old,[id]:''}));
      setNewPin(old=>({...old,[id]:''}));
      setBusy(false);
    }
  }
  if (auth.isLoading) return <div className="empty"><LoaderCircle className="spin"/><p>Checking your sign-in...</p></div>;
  if (auth.error) return <div className="notice">Authentication error: {auth.error.message}. <Link href="/status">Check setup</Link></div>;
  if (!auth.isAuthenticated) return <div className="auth-gate"><div className="big-icon"><UserRound size={28}/></div><h2>Your profiles, your stories.</h2><p>Sign in with the local Keycloak identity service to create and manage viewer profiles.</p><button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}><LogIn size={16}/> Sign in</button><div className="under-note">Development identity service must be running.</div></div>;
  return <div className="profiles-panel"><div className="section-title"><div><div className="eyebrow">PERSONAL SPACES</div><h2>Who's watching<span className="title-period">?</span></h2><p>Signed in as {auth.user?.profile.email ?? 'viewer'}</p></div><button className="text-btn" onClick={()=>void auth.signoutRedirect()}>Sign out</button></div>
    {error && <div className="notice" role="alert">{error}</div>}
    <div className="profile-grid">{profiles.map((p,index)=><div className="profile-tile" key={p.id}><div className={`avatar avatar-${index%4}`}><UserRound size={30}/></div><strong>{p.name}</strong><span>{p.child?'Kids profile':'Standard profile'} · {p.preferredLanguage.toUpperCase()}</span><button onClick={()=>{window.localStorage.setItem(`reelnest:active-profile:${auth.user?.profile.sub ?? 'anonymous'}`,p.id);window.dispatchEvent(new Event(PROFILE_EVENT));}} className="btn btn-outline">Watch as {p.name}</button>{!p.child&&<div className="profile-pin-controls">
      <label>Adult profile PIN (six to twelve digits)
        <input aria-label={`Current PIN for ${p.name}`} type="password" inputMode="numeric" autoComplete="off" value={pinInput[p.id]||''} maxLength={12} onChange={e=>setPinInput(old=>({...old,[p.id]:e.target.value}))}/>
      </label>
      <label>{pinStatus[p.id]?'Change PIN':'Set PIN'}
        <input aria-label={`New PIN for ${p.name}`} type="password" inputMode="numeric" autoComplete="new-password" value={newPin[p.id]||''} maxLength={12} onChange={e=>setNewPin(old=>({...old,[p.id]:e.target.value}))}/>
      </label>
      <button type="button" className="btn btn-outline" disabled={busy||!(newPin[p.id]||'').match(/^[0-9]{6,12}$/)} onClick={()=>void savePin(p.id)}>Save PIN</button>
      {pinStatus[p.id]&&<small>PIN required to remove this adult profile. It does not yet lock all playback endpoints.</small>}
    </div>}
    <label className="check"><input type="checkbox" checked={!!personalization[p.id]} disabled={busy}
      onChange={()=>void togglePersonalization(p.id)}/> Personalized recommendations (optional)</label>
    <button onClick={()=>void remove(p.id)} disabled={busy} className="text-btn danger"><Trash2 size={15}/> Remove</button></div>)}</div>
    <DeviceHandoff activeProfileId={profiles.some(p=>p.id===selectedProfile)?selectedProfile:profiles[0]?.id??''} profileIds={profiles.map(p=>p.id)}/>
    {profiles.length < 5 && <form onSubmit={e=>void create(e)} className="profile-form"><h3><Plus size={19}/> Add a profile</h3><div className="form-fields"><label>Profile name<input value={name} maxLength={40} required placeholder="e.g. Alex" onChange={e=>setName(e.target.value)}/></label><label>Language<select value={language} onChange={e=>setLanguage(e.target.value as 'en'|'hi')}><option value="en">English</option><option value="hi">Hindi</option></select></label><label className="check"><input type="checkbox" checked={child} onChange={e=>setChild(e.target.checked)}/> Child profile</label><button type="submit" disabled={busy || !name.trim()} className="btn btn-primary">{busy?'Saving...':'Create profile'}</button></div></form>}
  </div>;
}
