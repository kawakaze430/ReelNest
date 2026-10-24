'use client';
import {useEffect,useState} from 'react';
import {apiFetch} from '@/lib/api';

type Preferences={contextualPreviewsOptIn:boolean;anonymousMeasurementOptIn:boolean};
type Creative={id:string;advertiser:string;message:string};
type Receipt={nonBillable:boolean;duplicate:boolean;note:string};
export function ContextualPreviewPanel({token,profileId,titleId}:{
  token:string;profileId:string;titleId:string;
}){
  const [prefs,setPrefs]=useState<Preferences|null>(null);
  const [creatives,setCreatives]=useState<Creative[]>([]);
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[status,setStatus]=useState('');
  useEffect(()=>{
    const abort=new AbortController();
    setPrefs(null);setCreatives([]);setError('');setStatus('');
    void apiFetch<Preferences>(`/api/v1/avod/preferences?profileId=${encodeURIComponent(profileId)}`,
      token,{signal:abort.signal})
      .then(value=>{if(!abort.signal.aborted)setPrefs(value);})
      .catch(()=>{if(!abort.signal.aborted)setError('Optional ad-preview preferences unavailable.');});
    return()=>abort.abort();
  },[token,profileId]);
  useEffect(()=>{
    if(!prefs?.contextualPreviewsOptIn){setCreatives([]);return;}
    const abort=new AbortController();
    void apiFetch<Creative[]>(`/api/v1/avod/preview?profileId=${encodeURIComponent(profileId)}&titleId=${encodeURIComponent(titleId)}`,
      token,{signal:abort.signal})
      .then(data=>{if(!abort.signal.aborted)setCreatives(data);})
      .catch(()=>{if(!abort.signal.aborted)setCreatives([]);});
    return()=>abort.abort();
  },[token,profileId,titleId,prefs?.contextualPreviewsOptIn]);
  async function save(value:Preferences){
    setBusy(true);setError('');setStatus('');
    try{
      const changed=await apiFetch<Preferences>(
        `/api/v1/avod/preferences?profileId=${encodeURIComponent(profileId)}`,
        token,{method:'PUT',body:JSON.stringify(value)});
      setPrefs(changed);setStatus('Advertising-preview preferences saved.');
      if(!changed.contextualPreviewsOptIn)setCreatives([]);
    }catch(e){setError(e instanceof Error?e.message:'Cannot save preferences.');}
    finally{setBusy(false);}
  }
  async function record(creative:Creative){
    if(!prefs?.contextualPreviewsOptIn||!prefs.anonymousMeasurementOptIn)return;
    setBusy(true);setError('');setStatus('');
    try{
      const receipt=await apiFetch<Receipt>('/api/v1/avod/preview-events',token,{
        method:'POST',body:JSON.stringify({
          profileId,titleId,creativeId:creative.id,eventId:crypto.randomUUID()})});
      setStatus(receipt.note);
    }catch(e){setError(e instanceof Error?e.message:'Cannot record preview signal.');}
    finally{setBusy(false);}
  }
  return <section className="studio-panel" aria-label="Optional contextual sponsor previews">
    <h2>Contextual sponsor preview lab</h2>
    <p>Opt-in experiments only. No advertising is inserted into films, no advertiser is billed, and no personalized targeting is enabled.</p>
    {error&&<p role="alert">{error}</p>}
    {prefs&&<>
      <label><input type="checkbox" checked={prefs.contextualPreviewsOptIn} disabled={busy}
        onChange={e=>void save({contextualPreviewsOptIn:e.target.checked,
          anonymousMeasurementOptIn:e.target.checked&&prefs.anonymousMeasurementOptIn})}/>
        Allow contextual sponsor previews for this adult profile</label>
      <p><label><input type="checkbox" checked={prefs.anonymousMeasurementOptIn} disabled={busy||!prefs.contextualPreviewsOptIn}
        onChange={e=>void save({...prefs,anonymousMeasurementOptIn:e.target.checked})}/>
        Separately opt in to non-billable, on-site preview signals</label></p>
      {status&&<p role="status">{status}</p>}
      {prefs.contextualPreviewsOptIn&&creatives.length===0&&<p>No currently reviewed sponsor previews for this licensed title.</p>}
      {creatives.map(c=><article className="notice notice-dark" key={c.id}>
        <strong>Preview only — {c.advertiser}</strong>
        <p>{c.message}</p>
        {prefs.anonymousMeasurementOptIn&&<button type="button" className="btn btn-outline" disabled={busy}
          onClick={()=>void record(c)}>Record optional test signal</button>}
      </article>)}
    </>}
  </section>;
}
