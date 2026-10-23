'use client';
import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import Link from 'next/link';
import {apiFetch} from '@/lib/api';
type CaseItem={id:string;ownerId:string;titleId:string|null;category:string;subject:string;description:string;status:string;resolution:string|null;createdAt:string};
type State='OPEN'|'IN_REVIEW'|'RESOLVED'|'REJECTED';
export function AdminSupportCases(){
 const auth=useAuth();const token=auth.user?.access_token;
 const [filter,setFilter]=useState<State>('OPEN');const [items,setItems]=useState<CaseItem[]>([]);
 const [error,setError]=useState('');const [reason,setReason]=useState<Record<string,string>>({});const [busy,setBusy]=useState<string|null>(null);
 async function reload(){if(!token)return;try{setItems(await apiFetch<CaseItem[]>(`/api/v1/admin/support/cases?status=${filter}`,token));setError('');}catch{setError('You must be a moderator or administrator to review cases.');}}
 useEffect(()=>{void reload();},[token,filter]);
 async function decide(id:string,status:State,unpublishTitle=false){if(!token)return;
  if(unpublishTitle&&!window.confirm('Unpublish this title immediately and revoke application playback sessions? This is a privileged action.'))return;
  setBusy(id);setError('');try{
   await apiFetch(`/api/v1/admin/support/cases/${id}/decision`,token,{method:'POST',body:JSON.stringify({status,reason:reason[id]||'',unpublishTitle})});await reload();
  }catch(e){setError(e instanceof Error?e.message:'Could not save review');}finally{setBusy(null);}}
 if(!auth.isAuthenticated)return <p>Sign in as an authorized moderator or administrator to review cases.</p>;
 return <main className="wrap admin-support"><div className="eyebrow">REELNEST OPERATIONS</div><h1>Support and rights-review queue</h1><p>Decisions are recorded in an audit trail. Publishing restrictions remain enforced independently by the media API.</p><p><Link href="/admin">← Return to admin dashboard</Link></p>
  <label>Queue<select value={filter} onChange={e=>setFilter(e.target.value as State)}><option>OPEN</option><option>IN_REVIEW</option><option>RESOLVED</option><option>REJECTED</option></select></label>
  {error&&<p role="alert" className="notice">{error}</p>}
  {items.length===0&&<p>No cases in this queue.</p>}
  <div className="support-case-list">{items.map(c=><article className="support-case-item" key={c.id}>
    <h2>{c.subject}</h2><small>{c.category} · {new Date(c.createdAt).toLocaleString()} · Case {c.id}</small><p>{c.description}</p>{c.titleId&&<p>Linked title: <code>{c.titleId}</code></p>}
    {c.resolution&&<p>Recorded decision: {c.resolution}</p>}
    {(c.status==='OPEN'||c.status==='IN_REVIEW')&&<><label>Reason for decision<textarea maxLength={2000} minLength={10} value={reason[c.id]||''} rows={3} onChange={e=>setReason(old=>({...old,[c.id]:e.target.value}))}/></label>
      <div className="hero-buttons">{c.status==='OPEN'&&<button className="btn btn-outline" disabled={busy===c.id || (reason[c.id]||'').trim().length<10} onClick={()=>void decide(c.id,'IN_REVIEW')}>Begin review</button>}<button className="btn btn-outline" disabled={busy===c.id || (reason[c.id]||'').trim().length<10} onClick={()=>void decide(c.id,'REJECTED')}>Reject</button><button className="btn btn-primary" disabled={busy===c.id || (reason[c.id]||'').trim().length<10} onClick={()=>void decide(c.id,'RESOLVED')}>Resolve</button>
      {c.category==='RIGHTS_COMPLAINT'&&c.titleId&&<button className="btn btn-outline" disabled={busy===c.id || (reason[c.id]||'').trim().length<10} onClick={()=>void decide(c.id,'RESOLVED',true)}>Resolve and unpublish film (admin)</button>}
      </div></>}
   </article>)}</div>
 </main>;
}
