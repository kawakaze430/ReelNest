'use client';
import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import {apiFetch} from '@/lib/api';

type PrivacyRequest={id:string;type:string;status:string;createdAt:string};
export default function PrivacyPage(){
 const auth=useAuth();const token=auth.user?.access_token;
 const [requests,setRequests]=useState<PrivacyRequest[]>([]);
 const [error,setError]=useState('');const [message,setMessage]=useState('');const [working,setWorking]=useState(false);
 useEffect(()=>{if(!token)return;let live=true;
   apiFetch<PrivacyRequest[]>('/api/v1/privacy/requests',token).then(rows=>{if(live)setRequests(rows);})
   .catch(()=>{if(live)setError('Unable to load your privacy requests.');});
   return()=>{live=false;};
 },[token]);
 async function exportData(){if(!token)return;setWorking(true);setError('');
   try{const data=await apiFetch<Record<string,unknown>>('/api/v1/privacy/export',token);
     const blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'});
     const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;
     a.download='reelnest-my-data.json';a.click();URL.revokeObjectURL(url);
     setMessage('Application data export requested. The JSON download contains private information.');
   }catch{setError('Data export unavailable.');}finally{setWorking(false);}
 }
 async function deletion(){if(!token||!window.confirm('Request an account deletion review? This does not immediately close billing or delete your identity provider account.'))return;
   setWorking(true);setError('');
   try{const r=await apiFetch<PrivacyRequest>('/api/v1/privacy/deletion-request',token,{method:'POST'});
     setRequests(old=>[r,...old.filter(x=>x.id!==r.id)]);
     setMessage('Your deletion review request was received. Please also cancel active subscriptions if needed.');
   }catch{setError('Unable to submit the request.');}finally{setWorking(false);}
 }
 return <main className="wrap viewer-shell" style={{maxWidth:760}}>
   <div className="eyebrow">REELNEST / PRIVACY</div><h1>Privacy controls<span className="title-period">.</span></h1>
   <p>Export your ReelNest application data or request account deletion review. Exports do not include external identity-provider or statutory billing records.</p>
   {!token?<button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in</button>:<>
   {error&&<p role="alert" className="notice">{error}</p>}{message&&<p role="status" className="notice">{message}</p>}
   <div style={{display:'flex',gap:12,flexWrap:'wrap',margin:'20px 0'}}>
      <button className="btn btn-primary" disabled={working} onClick={()=>void exportData()}>Export my data (JSON)</button>
      <button className="btn btn-outline" disabled={working} onClick={()=>void deletion()}>Request account deletion review</button>
   </div>
   <h2>Your requests</h2>{requests.length===0?<p>No pending requests.</p>:<ul>{requests.map(r=><li key={r.id}>{r.type}: {r.status} — {new Date(r.createdAt).toLocaleDateString()}</li>)}</ul>}
   </>}
   <p style={{marginTop:32}}>Deletion is not automatic. A verified administrator must handle retained transaction data and identity-provider records separately.</p>
 </main>;
}
