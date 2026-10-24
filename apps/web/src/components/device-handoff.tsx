'use client';

import {useState} from 'react';
import {useAuth} from 'react-oidc-context';
import {apiFetch} from '@/lib/api';
import {PROFILE_EVENT} from '@/hooks/use-active-profile';

type Invitation={code:string;expiresAt:string};
type Selection={profileId:string;profileName:string};

/** Both devices MUST already be signed into the exact same OIDC account. */
export function DeviceHandoff({activeProfileId,profileIds}:{
  activeProfileId:string;profileIds:string[];
}) {
  const auth=useAuth();
  const [invitation,setInvitation]=useState<Invitation|null>(null);
  const [code,setCode]=useState('');
  const [busy,setBusy]=useState(false);
  const [status,setStatus]=useState('');
  const token=auth.user?.access_token;
  const subject=auth.user?.profile.sub;
  async function issue() {
    if(!token||!profileIds.includes(activeProfileId))return;
    setBusy(true);setStatus('');setInvitation(null);
    try{
      const reply=await apiFetch<Invitation>('/api/v1/device-handoff',token,{
        method:'POST',body:JSON.stringify({profileId:activeProfileId})});
      setInvitation(reply);
      setStatus('Enter this one-time code on another device signed in to the same account. It expires in five minutes.');
    }catch(e){setStatus(e instanceof Error?e.message:'Device pairing unavailable');}
    finally{setBusy(false);}
  }
  async function redeem() {
    if(!token||!subject||!/^[A-Za-z0-9_-]{32}$/.test(code))return;
    setBusy(true);setStatus('');
    try{
      const target=await apiFetch<Selection>('/api/v1/device-handoff/redeem',token,{
        method:'POST',body:JSON.stringify({code})});
      if(!profileIds.includes(target.profileId)){
        setStatus('The code belongs to a profile not in this signed-in account. Reload your profiles.');
        return;
      }
      window.localStorage.setItem(`reelnest:active-profile:${subject}`,target.profileId);
      window.dispatchEvent(new Event(PROFILE_EVENT));
      setStatus(`Switched to ${target.profileName}. Media access is checked separately for each device.`);
    }catch(e){setStatus(e instanceof Error?e.message:'Code expired, used, or belongs to another account');}
    finally{setBusy(false);setCode('');}
  }
  return <section className="studio-panel" aria-label="Device profile handoff">
    <h3>Continue on another signed-in device</h3>
    <p>Transfer your selected profile without sharing passwords, access tokens or private video URLs.
      Both devices must already be signed in to the same account. This does not pair an unauthenticated TV.</p>
    <button type="button" className="btn btn-outline" disabled={busy||!token||!profileIds.includes(activeProfileId)}
      onClick={()=>void issue()}>Create one-time handoff code</button>
    {invitation&&<p role="status">Code: <code>{invitation.code}</code>. Expires {new Date(invitation.expiresAt).toLocaleTimeString()}.</p>}
    <label>Enter code from your other signed-in device
      <input aria-label="One-time device handoff code" maxLength={32} autoComplete="off"
        value={code} onChange={e=>setCode(e.target.value.trim())}/>
    </label>
    <button type="button" className="btn btn-outline" disabled={busy||!/^[A-Za-z0-9_-]{32}$/.test(code)}
      onClick={()=>void redeem()}>Use handoff code</button>
    {status&&<p role="status">{status}</p>}
  </section>;
}
