'use client';
import {useAuth} from 'react-oidc-context';
import {useState} from 'react';
import Link from 'next/link';
import {API_BASE_URL,apiFetch} from '@/lib/api';

type Batch={id:string;status:string;rows:number;digest:string;expiresAt:string};
type Schedule={titleId:string;publishAt:string;state:string;lastError:string|null};

export function AdminCatalogOperations(){
 const auth=useAuth();const token=auth.user?.access_token;
 const [csv,setCsv]=useState('id,name,genre,language,age_rating\n');
 const [batchId,setBatchId]=useState('');const [batch,setBatch]=useState<Batch|null>(null);
 const [titleId,setTitleId]=useState('');const [date,setDate]=useState('');
 const [schedules,setSchedules]=useState<Schedule[]>([]);
 const [error,setError]=useState('');const [message,setMessage]=useState('');const [busy,setBusy]=useState(false);
 async function csvCall(path:string){if(!token)return;setBusy(true);setError('');setMessage('');try{
   const response=await fetch(`${API_BASE_URL}/api/v1/admin/catalog/import${path}`,{
      method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'text/csv'},body:csv
   });if(!response.ok)throw new Error(`CSV ${response.status}: ${(await response.text()).slice(0,300)}`);
   const result=await response.json() as Batch;setBatch(result);setBatchId(result.id);setMessage('Batch staged. Another administrator must approve before applying.');
 }catch(e){setError(e instanceof Error?e.message:'CSV staging failed')}finally{setBusy(false)}}
 async function action(path:string,method:'POST'|'DELETE'='POST',body?:unknown){
   if(!token)return;setBusy(true);setError('');setMessage('');try{
     const result=await apiFetch<unknown>(path,token,{method,body:body===undefined?undefined:JSON.stringify(body)});
     setMessage(`Operation recorded: ${method} ${path}. ${result?JSON.stringify(result).slice(0,250):''}`);
   }catch(e){setError(e instanceof Error?e.message:'Operation failed')}finally{setBusy(false)} }
 async function loadSchedules(){if(!token)return;try{setSchedules(await apiFetch<Schedule[]>('/api/v1/admin/publication-schedules',token));}catch(e){setError(e instanceof Error?e.message:'Could not read schedules')}}
 if(!auth.isAuthenticated)return <main className="wrap"><h1>Admin catalog operations</h1><p>Sign in with an administrator account.</p></main>;
 return <main className="wrap catalog-section"><p><Link href="/admin">← Admin dashboard</Link></p><h1>Catalog operations</h1>
   <p>Metadata bulk changes are restricted to DRAFT titles and require approval from a different administrator. No changes are applied at the preview/stage step.</p>
   {error&&<div role="alert" className="notice">{error}</div>}{message&&<div role="status" className="notice">{message}</div>}
   <section><h2>Reviewed CSV changes</h2><p>Expected columns: id,name,genre,language,age_rating. Maximum 500 rows.</p>
   <label>Metadata CSV<textarea rows={7} value={csv} onChange={e=>setCsv(e.target.value)}/></label>
   <div className="hero-buttons"><button className="btn btn-outline" disabled={busy} onClick={()=>void csvCall('/stage')}>Stage draft changes</button></div>
   {batch&&<p>Batch {batch.id} · {batch.status} · {batch.rows} rows · Expiry {new Date(batch.expiresAt).toLocaleString()}</p>}
   <label>Pending batch ID<input value={batchId} onChange={e=>setBatchId(e.target.value)}/></label>
   <div className="hero-buttons"><button className="btn btn-outline" disabled={busy||!batchId} onClick={()=>void action(`/api/v1/admin/catalog/import/${batchId}/approve`)}>Approve as separate admin</button>
   <button className="btn btn-primary" disabled={busy||!batchId} onClick={()=>void action(`/api/v1/admin/catalog/import/${batchId}/apply`)}>Apply approved batch</button></div></section>
   <section><h2>Scheduled publication</h2><p>Only rights-approved IN_REVIEW titles with READY HLS media can be scheduled.</p>
   <label>Title UUID<input value={titleId} onChange={e=>setTitleId(e.target.value)} placeholder="Approved title ID"/></label>
   <label>Publish date/time (your device time)<input type="datetime-local" value={date} onChange={e=>setDate(e.target.value)}/></label>
   <button className="btn btn-primary" disabled={busy||!titleId||!date} onClick={()=>void action(`/api/v1/admin/publication-schedules/${titleId}`,'POST',{publishAt:new Date(date).toISOString()})}>Schedule publication</button>
   <button className="btn btn-outline" onClick={()=>void loadSchedules()}>Refresh schedule status</button>
   <ul>{schedules.map(s=><li key={s.titleId}>{s.titleId} — {s.state} — {new Date(s.publishAt).toLocaleString()}{s.lastError&&` — ${s.lastError}`}</li>)}</ul></section>
 </main>;
}
