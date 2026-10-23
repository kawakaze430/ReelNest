'use client';
import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import {apiFetch} from '@/lib/api';

type Prefs={inAppEnabled:boolean;emailOptIn:boolean;productUpdatesOptIn:boolean};
type Notice={id:string;kind:string;title:string;body:string;createdAt:string;readAt:string|null};
export default function NotificationsPage(){
  const auth=useAuth(); const token=auth.user?.access_token;
  const [prefs,setPrefs]=useState<Prefs>({inAppEnabled:true,emailOptIn:false,productUpdatesOptIn:false});
  const [notices,setNotices]=useState<Notice[]>([]);
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('');
  useEffect(()=>{if(!token)return;let ignore=false;
    Promise.all([apiFetch<Prefs>('/api/v1/me/notifications/preferences',token),apiFetch<Notice[]>('/api/v1/me/notifications?limit=50',token)])
      .then(([p,n])=>{if(!ignore){setPrefs(p);setNotices(n);}})
      .catch(()=>{if(!ignore)setError('Cannot load notifications.');});
    return()=>{ignore=true};},[token]);
  async function save(){if(!token)return;setBusy(true);setError('');try{
    const p=await apiFetch<Prefs>('/api/v1/me/notifications/preferences',token,{method:'PUT',body:JSON.stringify(prefs)});
    setPrefs(p);setNotice('Your notification preferences were saved. No emails are being sent by this development build.');
  }catch{setError('Could not save preferences.');}finally{setBusy(false)}}
  async function change(id:string,remove:boolean){if(!token)return;setBusy(true);try{
    await apiFetch<void>(`/api/v1/me/notifications/${encodeURIComponent(id)}${remove?'':'/read'}`,token,{method:remove?'DELETE':'POST'});
    setNotices(prev=>remove?prev.filter(x=>x.id!==id):prev.map(n=>n.id===id?{...n,readAt:new Date().toISOString()}:n));
  }catch{setError('Could not update notification.');}finally{setBusy(false)}}
  return <main className="wrap viewer-shell" style={{maxWidth:800}}>
    <div className="eyebrow">ACCOUNT / NOTIFICATIONS</div><h1>Notifications<span className="title-period">.</span></h1>
    <p>Private in-app notices, including rights-expiry reminders for approved creators. Email delivery is not enabled.</p>
    {!token?<button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in</button>:<>
      {error&&<p role="alert" className="notice">{error}</p>}{notice&&<p role="status" className="notice">{notice}</p>}
      <section className="notice" aria-label="Notification preferences">
        <label><input type="checkbox" checked={prefs.inAppEnabled} onChange={e=>setPrefs(p=>({...p,inAppEnabled:e.target.checked}))}/> Enable in-app notifications</label>
        <p><label><input type="checkbox" checked={prefs.emailOptIn} onChange={e=>setPrefs(p=>({...p,emailOptIn:e.target.checked}))}/> Consent to future email notifications (delivery not implemented)</label></p>
        <p><label><input type="checkbox" checked={prefs.productUpdatesOptIn} onChange={e=>setPrefs(p=>({...p,productUpdatesOptIn:e.target.checked}))}/> Opt in to product announcements</label></p>
        <button className="btn btn-primary" disabled={busy} onClick={()=>void save()}>Save preferences</button>
      </section>
      <h2>Inbox</h2>
      {notices.length===0?<p>No notifications yet.</p>:<ul style={{listStyle:'none',padding:0,display:'grid',gap:12}}>
        {notices.map(n=><li key={n.id} className="notice"><strong>{n.title}</strong> {!n.readAt&&<small>(unread)</small>}
          <p>{n.body}</p><small>{new Date(n.createdAt).toLocaleString()}</small>
          <div style={{display:'flex',gap:10,marginTop:10}}>{!n.readAt&&<button className="btn btn-outline" disabled={busy} onClick={()=>void change(n.id,false)}>Mark read</button>}
            <button className="btn btn-outline" disabled={busy} onClick={()=>void change(n.id,true)}>Dismiss</button></div>
        </li>)}
      </ul>}
    </>}
  </main>;
}
