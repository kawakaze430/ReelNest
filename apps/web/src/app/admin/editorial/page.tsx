'use client';
import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import {apiFetch} from '@/lib/api';
type Title={id:string;name:string};
type Collection={id:string;slug:string;heading:string;status:string;titles:Title[]};
type Program={id:string;titleId:string;title:string;startsAt:string;endsAt:string};
type Guide={channel:{id:string;slug:string;name:string;status:string};programs:Program[]};
export default function EditorialAdminPage(){
 const auth=useAuth();
 return <EditorialAdminView key={auth.isAuthenticated ? auth.user?.profile.sub : 'anonymous'} auth={auth}/>;
}
function EditorialAdminView({auth}:{auth:ReturnType<typeof useAuth>}){
 const token=auth.user?.access_token;
 const [collections,setCollections]=useState<Collection[]>([]),[channels,setChannels]=useState<Guide[]>([]),[error,setError]=useState(''),[message,setMessage]=useState('');
 const [kind,setKind]=useState<'collection'|'channel'>('collection'),[slug,setSlug]=useState(''),[heading,setHeading]=useState('');
 const [collectionId,setCollectionId]=useState(''),[channelId,setChannelId]=useState(''),[titleId,setTitleId]=useState('');
 const [starts,setStarts]=useState(''),[ends,setEnds]=useState(''),[busy,setBusy]=useState(false);
 async function refresh(){if(!token)return;try{const [c,ch]=await Promise.all([
  apiFetch<Collection[]>('/api/v1/admin/collections',token),apiFetch<Guide[]>('/api/v1/admin/channels',token)]);
  setCollections(c);setChannels(ch);setCollectionId(v=>v||c[0]?.id||'');setChannelId(v=>v||ch[0]?.channel.id||'');
 }catch{setError('Administrator access or backend is unavailable.')}}
 useEffect(()=>{
  if(!token)return;
  const controller=new AbortController();
  Promise.all([
   apiFetch<Collection[]>('/api/v1/admin/collections',token,{signal:controller.signal}),
   apiFetch<Guide[]>('/api/v1/admin/channels',token,{signal:controller.signal})
  ]).then(([c,ch])=>{
   if(controller.signal.aborted)return;
   setCollections(c);setChannels(ch);setCollectionId(v=>v||c[0]?.id||'');setChannelId(v=>v||ch[0]?.channel.id||'');
  }).catch(()=>{if(!controller.signal.aborted)setError('Administrator access or backend is unavailable.');});
  return()=>controller.abort();
 },[token]);
 async function action(run:()=>Promise<unknown>){setBusy(true);setError('');setMessage('');try{
  await run();setMessage('Change saved. Publication still requires actual rights approval and video entitlements.');await refresh();
 }catch(e){setError(String(e))}finally{setBusy(false)}}
 return <main className="wrap viewer-shell" style={{maxWidth:1000}}><div className="eyebrow">ADMIN / EDITORIAL</div><h1>Collections & channel guide<span className="title-period">.</span></h1>
 <p>Only administrators can stage and publish editorial rails. Channels provide schedules; they do not create an unrestricted live TV stream.</p>
 {!token?<button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in as administrator</button>:<>
 {error&&<p role="alert" className="notice">{error}</p>}{message&&<p role="status" className="notice">{message}</p>}
 <section className="notice"><h2>Create draft</h2><label htmlFor="editorial-kind">Kind</label><select id="editorial-kind" value={kind} onChange={e=>setKind(e.target.value as 'collection'|'channel')}><option value="collection">Editorial collection</option><option value="channel">Scheduled channel</option></select>
 <label htmlFor="editorial-slug">Unique slug</label><input id="editorial-slug" value={slug} onChange={e=>setSlug(e.target.value)} maxLength={90} placeholder="staff-picks"/>
 <label htmlFor="editorial-name">Public heading</label><input id="editorial-name" value={heading} onChange={e=>setHeading(e.target.value)} maxLength={100}/>
 <p><button className="btn btn-primary" disabled={busy||!slug||!heading} onClick={()=>void action(()=>apiFetch(kind==='collection'?'/api/v1/admin/collections':'/api/v1/admin/channels',token,{method:'POST',body:JSON.stringify(kind==='collection'?{slug,heading,description:'',language:null,position:100,startsAt:null,endsAt:null}:{slug,name:heading,description:''})}))}>Create draft</button></p>
 </section>
 <div style={{display:'grid',gap:20,gridTemplateColumns:'repeat(auto-fit,minmax(310px,1fr))',marginTop:20}}>
 <section className="notice"><h2>Collection editor</h2>
 <label htmlFor="collection-id">Collection</label><select id="collection-id" value={collectionId} onChange={e=>setCollectionId(e.target.value)}>{collections.map(c=><option value={c.id} key={c.id}>{c.heading} ({c.status})</option>)}</select>
 <label htmlFor="collection-title">Published film UUID</label><input id="collection-title" value={titleId} onChange={e=>setTitleId(e.target.value)} maxLength={36}/>
 <p><button className="btn btn-outline" disabled={busy||!collectionId||!titleId} onClick={()=>void action(()=>apiFetch(`/api/v1/admin/collections/${collectionId}/items`,token,{method:'PUT',body:JSON.stringify({titleId,position:100})}))}>Add film to draft</button></p>
 <button className="btn btn-primary" disabled={busy||!collectionId} onClick={()=>void action(()=>apiFetch(`/api/v1/admin/collections/${collectionId}/state`,token,{method:'PUT',body:JSON.stringify({status:'PUBLISHED'})}))}>Publish collection</button>
 </section>
 <section className="notice"><h2>Channel programming</h2>
 <label htmlFor="channel-id">Channel</label><select id="channel-id" value={channelId} onChange={e=>setChannelId(e.target.value)}>{channels.map(c=><option key={c.channel.id} value={c.channel.id}>{c.channel.name} ({c.channel.status})</option>)}</select>
 <label htmlFor="channel-title">Published film UUID</label><input id="channel-title" value={titleId} onChange={e=>setTitleId(e.target.value)} maxLength={36}/>
 <label htmlFor="channel-start">Start time</label><input type="datetime-local" id="channel-start" value={starts} onChange={e=>setStarts(e.target.value)}/>
 <label htmlFor="channel-end">End time</label><input type="datetime-local" id="channel-end" value={ends} onChange={e=>setEnds(e.target.value)}/>
 <p><button className="btn btn-outline" disabled={busy||!channelId||!titleId||!starts||!ends} onClick={()=>void action(()=>apiFetch(`/api/v1/admin/channels/${channelId}/programs`,token,{method:'POST',body:JSON.stringify({titleId,startsAt:new Date(starts).toISOString(),endsAt:new Date(ends).toISOString()})}))}>Add program</button></p>
 <button className="btn btn-primary" disabled={busy||!channelId} onClick={()=>void action(()=>apiFetch(`/api/v1/admin/channels/${channelId}/state`,token,{method:'PUT',body:JSON.stringify({status:'PUBLISHED'})}))}>Publish channel</button>
 </section></div>
 <section style={{marginTop:22}}><h2>Current collections</h2>{collections.map(c=><p key={c.id}>{c.heading} — {c.status}, {c.titles.length} film cards</p>)}
 <h2>Current guide</h2>{channels.map(c=><p key={c.channel.id}>{c.channel.name} — {c.channel.status}, {c.programs.length} scheduled programs</p>)}</section>
 </>}
 </main>;
}
