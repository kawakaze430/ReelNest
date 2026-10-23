'use client';
import {useState} from 'react';
import {probeDrm,type Capability} from '@/lib/drm-probe';
export default function DrmDiagnosticsPage(){
 const [result,setResult]=useState<Capability[]>([]),[busy,setBusy]=useState(false),[error,setError]=useState('');
 async function check(){setBusy(true);setError('');try{setResult(await probeDrm())}catch{setError('This browser did not allow DRM capability detection.')}finally{setBusy(false)}}
 return <main className="wrap viewer-shell" style={{maxWidth:850}}><div className="eyebrow">LAB / DEVICE SUPPORT</div>
 <h1>DRM capability diagnostic<span className="title-period">.</span></h1>
 <p>This is a local device compatibility probe; it does not play encrypted films, contact a license server or prove commercial Widevine, FairPlay or PlayReady readiness.</p>
 <button className="btn btn-primary" disabled={busy} onClick={()=>void check()}>{busy?'Checking…':'Check my browser'}</button>
 {error&&<p role="alert">{error}</p>}
 <ul style={{marginTop:24,padding:0,listStyle:'none',display:'grid',gap:10}}>{result.map(r=><li className="notice" key={r.name}>
 <strong>{r.name} — {r.supported?'EME configuration reported supported':'Not available with this configuration'}</strong>
 <p>{r.reason}</p><small>{r.keySystem}</small></li>)}</ul>
 <p>Production DRM additionally requires encrypted DASH/HLS packaging, a licensed key service, secure keys, playback entitlement checks, renewal handling, and device testing.</p>
 </main>;
}
