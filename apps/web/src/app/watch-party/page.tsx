'use client';
import Link from 'next/link';
import {useAuth} from 'react-oidc-context';
import {Suspense,useEffect,useState} from 'react';
import {useSearchParams} from 'next/navigation';
import {apiFetch} from '@/lib/api';
type Profile={id:string;name:string;child:boolean};
type Room={id:string;titleId:string;hostUserId:string;version:number;positionSeconds:number;paused:boolean;updatedAt:string;expiresAt:string;members:number};
export default function PartyPage(){
 return <Suspense fallback={<main className="wrap viewer-shell">Loading watch party…</main>}><PartyIdentity/></Suspense>;
}
function PartyIdentity(){
 const auth=useAuth(),params=useSearchParams();
 const initialTitle=params.get('titleId')||'';
 return <PartyView key={`${auth.user?.profile.sub ?? 'anonymous'}:${initialTitle}`} auth={auth} initialTitle={initialTitle}/>;
}
function PartyView({auth,initialTitle}:{auth:ReturnType<typeof useAuth>;initialTitle:string}){
 const token=auth.user?.access_token;
 const [profiles,setProfiles]=useState<Profile[]>([]),[profile,setProfile]=useState(''),[title,setTitle]=useState(initialTitle),[invitation,setInvitation]=useState('');
 const [room,setRoom]=useState<Room|null>(null),[code,setCode]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const [position,setPosition]=useState(0),[paused,setPaused]=useState(true);
 useEffect(()=>{if(!token)return;let on=true;apiFetch<Profile[]>('/api/v1/profiles',token).then(x=>{if(on){setProfiles(x);setProfile(p=>p||x[0]?.id||'')}})
 .catch(()=>{if(on)setError('Could not load profiles.')});return()=>{on=false}},[token]);
 const roomId=room?.id;
 useEffect(()=>{if(!token||!roomId)return;let active=true;const interval=setInterval(()=>{
 apiFetch<Room>(`/api/v1/watch-parties/${roomId}`,token).then(r=>{if(active){setRoom(r);setPosition(r.positionSeconds);setPaused(r.paused)}})
 .catch(()=>{if(active){setRoom(null);setError('The room expired, access changed, or the connection failed.')}});
 },5000);return()=>{active=false;clearInterval(interval)};},[roomId,token]);
 async function create(){if(!token||!profile||!title)return;setBusy(true);setError('');try{
 const r=await apiFetch<{room:Room;invitation:string}>('/api/v1/watch-parties',token,{method:'POST',body:JSON.stringify({profileId:profile,titleId:title})});
 setRoom(r.room);setCode(r.invitation);setPosition(0);setPaused(true);
 }catch(e){setError(`Cannot create party: ${String(e)}`)}finally{setBusy(false)}}
 async function join(){if(!token||!profile||!invitation)return;setBusy(true);setError('');try{
 const r=await apiFetch<Room>('/api/v1/watch-parties/join',token,{method:'POST',body:JSON.stringify({profileId:profile,invitation:invitation.trim()})});
 setRoom(r);setCode('');setPosition(r.positionSeconds);setPaused(r.paused);
 }catch(e){setError(`Cannot join party: ${String(e)}`)}finally{setBusy(false)}}
 async function synchronize(){if(!token||!room)return;setBusy(true);try{
 const r=await apiFetch<Room>(`/api/v1/watch-parties/${room.id}/sync`,token,{method:'PUT',body:JSON.stringify({expectedVersion:room.version,positionSeconds:position,paused})});setRoom(r);
 }catch(e){setError(`Could not synchronize: ${String(e)}`)}finally{setBusy(false)}}
 async function leave(){if(!token||!room)return;try{await apiFetch<void>(`/api/v1/watch-parties/${room.id}`,token,{method:'DELETE'});}catch{/* Expired rooms are already inactive */}setRoom(null);setCode('')}
 return <main className="wrap viewer-shell" style={{maxWidth:900}}><div className="eyebrow">VIEW TOGETHER</div><h1>Watch parties<span className="title-period">.</span></h1>
 <p>Synchronize viewing with invited friends. Rooms share time and pause state only. Each person must independently hold viewing rights and start their own player.</p>
 {!token?<button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in to start</button>:<>
 <label htmlFor="party-profile">Your profile</label><select id="party-profile" value={profile} onChange={e=>setProfile(e.target.value)}>{profiles.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select>
 {!room?<div style={{display:'grid',gap:20,gridTemplateColumns:'repeat(auto-fit,minmax(260px,1fr))',marginTop:22}}>
 <section className="notice"><h2>Create private room</h2><label htmlFor="party-title">Film ID (from film details)</label>
 <input id="party-title" value={title} onChange={e=>setTitle(e.target.value)} placeholder="Film UUID" maxLength={36}/>
 <p><button className="btn btn-primary" disabled={busy||!profile||!title} onClick={()=>void create()}>Create room</button></p></section>
 <section className="notice"><h2>Join room</h2><label htmlFor="party-code">Private invitation code</label>
 <input id="party-code" value={invitation} onChange={e=>setInvitation(e.target.value)} placeholder="48-character invitation" maxLength={48}/>
 <p><button className="btn btn-primary" disabled={busy||!profile||invitation.length!==48} onClick={()=>void join()}>Join</button></p></section>
 </div>:<section className="notice" aria-live="polite" style={{marginTop:20}}><h2>Party connected · {room.members}/10 viewers</h2>
 <p>Room expires at {new Date(room.expiresAt).toLocaleString()}</p>
 {code&&<p><strong>Share privately:</strong> <code style={{overflowWrap:'anywhere'}}>{code}</code> <button className="btn btn-outline" onClick={()=>void navigator.clipboard.writeText(code)}>Copy code</button></p>}
 <p>Playhead: {room.positionSeconds}s · {room.paused?'Paused':'Playing'} · version {room.version}</p>
 <label htmlFor="party-position">Set shared playhead (seconds)</label><input id="party-position" type="number" min={0} max={86400} value={position} onChange={e=>setPosition(Number(e.target.value))}/>
 <label htmlFor="party-paused"><input id="party-paused" type="checkbox" checked={paused} onChange={e=>setPaused(e.target.checked)}/> Pause group</label>
 <p style={{display:'flex',gap:12,flexWrap:'wrap'}}><button className="btn btn-primary" disabled={busy} onClick={()=>void synchronize()}>Host: update group state</button>
 <Link className="btn btn-outline" href={`/watch/${room.titleId}?profileId=${encodeURIComponent(profile)}`}>Open your authorized player</Link>
 <button className="btn btn-outline" onClick={()=>void leave()}>Leave / close</button></p>
 <small>Only the host can synchronize. Each player is independent; this release uses five-second polling, not frame-accurate synchronization.</small>
 </section>}
 </>}{error&&<p role="alert" className="notice">{error}</p>}
 </main>;
}
