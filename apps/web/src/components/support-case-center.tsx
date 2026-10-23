'use client';

import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import {apiFetch,ApiError} from '@/lib/api';

type Category='TECHNICAL'|'BILLING'|'ACCOUNT'|'CREATOR'|'RIGHTS_COMPLAINT'|'OTHER';
type CaseRecord={id:string;titleId:string|null;parentCaseId:string|null;category:string;subject:string;description:string;status:string;resolution:string|null;createdAt:string;updatedAt:string};
const CATEGORIES: {value:Category;label:string}[]=[
 {value:'TECHNICAL',label:'Video playback or app issue'},
 {value:'BILLING',label:'Membership and billing'},
 {value:'ACCOUNT',label:'Account and profiles'},
 {value:'CREATOR',label:'Creator onboarding and upload'},
 {value:'RIGHTS_COMPLAINT',label:'Copyright or distribution rights'},
 {value:'OTHER',label:'Other request'}
];

export function SupportCaseCenter(){
 const auth=useAuth();const token=auth.user?.access_token;
 const [items,setItems]=useState<CaseRecord[]>([]);const [category,setCategory]=useState<Category>('TECHNICAL');
 const [subject,setSubject]=useState('');const [description,setDescription]=useState('');const [titleId,setTitleId]=useState('');
 const [message,setMessage]=useState('');const [busy,setBusy]=useState(false);const [loading,setLoading]=useState(false);
 const [appealId,setAppealId]=useState('');const [appealReason,setAppealReason]=useState('');
 async function refresh(){if(!token)return;setLoading(true);try {setItems(await apiFetch<CaseRecord[]>('/api/v1/support/cases',token));}catch(e){setMessage(e instanceof ApiError?e.message:'Could not load cases');}finally{setLoading(false);}}
 useEffect(()=>{if(token)void refresh();else setItems([]);},[token]); // authenticated account owns case list
 async function submit(e:React.FormEvent){e.preventDefault();if(!token)return;
  setBusy(true);setMessage('');try{
   await apiFetch<CaseRecord>('/api/v1/support/cases',token,{method:'POST',body:JSON.stringify({subject,description,category,titleId:category==='RIGHTS_COMPLAINT'?titleId||null:null})});
   setSubject('');setDescription('');setTitleId('');setMessage('Your request was submitted.');await refresh();
  }catch(e){setMessage(e instanceof Error?e.message:'Unable to submit request');}finally{setBusy(false);}}
 async function appeal(e:React.FormEvent){e.preventDefault();if(!token||!appealId)return;setBusy(true);setMessage('');
  try {await apiFetch(`/api/v1/support/cases/${appealId}/appeal`,token,{method:'POST',body:JSON.stringify({reason:appealReason})});
   setAppealReason('');setAppealId('');setMessage('Appeal submitted.');await refresh();}
  catch(e){setMessage(e instanceof Error?e.message:'Unable to submit appeal');}finally{setBusy(false);}}
 if(!auth.isAuthenticated)return <section className="support-case-center"><h2>Private support requests</h2><p>Sign in to create a ticket and track its status. Do not send passwords, access tokens or payment-card details.</p><button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in to request support</button></section>;
 return <section className="support-case-center" aria-label="Account support requests">
  <h2>Contact the ReelNest team</h2><p>Tickets are private to your account and reviewed by authorized moderators. Responses are not immediate.</p>
  <form onSubmit={e=>void submit(e)} className="support-ticket-form">
   <label>Request type<select value={category} onChange={e=>setCategory(e.target.value as Category)}>{CATEGORIES.map(c=><option key={c.value} value={c.value}>{c.label}</option>)}</select></label>
   {category==='RIGHTS_COMPLAINT'&&<label>Film ID (UUID)<input required value={titleId} onChange={e=>setTitleId(e.target.value)} placeholder="Film identifier" pattern="[0-9a-fA-F-]{36}"/></label>}
   <label>Subject<input required maxLength={160} value={subject} onChange={e=>setSubject(e.target.value)}/></label>
   <label>Description<textarea required minLength={15} maxLength={4000} rows={5} value={description} onChange={e=>setDescription(e.target.value)} placeholder="Explain the issue without sharing secrets or full payment details"/></label>
   <button className="btn btn-primary" type="submit" disabled={busy}>{busy?'Submitting…':'Create support ticket'}</button>
  </form>
  {message&&<p role="status" className="notice">{message}</p>}
  <h3>Your requests</h3>{loading&&<p>Loading requests…</p>}
  {!loading&&items.length===0&&<p>No requests submitted yet.</p>}
  <div className="support-case-list">{items.map(c=><article key={c.id} className="support-case-item"><div><strong>{c.subject}</strong> <span className="support-case-state">{c.status.replaceAll('_',' ')}</span></div><small>{new Date(c.createdAt).toLocaleDateString()} · {c.category.replaceAll('_',' ')}</small><p>{c.description}</p>{c.resolution&&<p><strong>Response:</strong> {c.resolution}</p>}
   {(c.status==='RESOLVED'||c.status==='REJECTED')&&c.category!=='APPEAL'&&!items.some(v=>v.parentCaseId===c.id)&&
    <button className="btn btn-outline" type="button" onClick={()=>{setAppealId(c.id);setAppealReason('');}}>Appeal this decision</button>}
  </article>)}</div>
  {appealId&&<form onSubmit={e=>void appeal(e)} className="support-ticket-form"><h3>Request a review of the decision</h3><label>Reason for appeal<textarea rows={4} minLength={15} maxLength={4000} required value={appealReason} onChange={e=>setAppealReason(e.target.value)}/></label><div className="hero-buttons"><button className="btn btn-primary" type="submit" disabled={busy}>Submit appeal</button><button className="btn btn-outline" type="button" onClick={()=>setAppealId('')}>Cancel</button></div></form>}
 </section>;
}
