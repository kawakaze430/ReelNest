'use client';

import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';

type Scene = {ordinal:number;label:string;summary:string;startSeconds:number;endSeconds:number};
export function SceneExplorer({token,titleId,profileId,duration,canSeek,onSeek}:{
  token:string;titleId:string;profileId:string;duration:number;canSeek:boolean;onSeek:(seconds:number)=>void;
}) {
  const [scenes,setScenes]=useState<Scene[]>([]);
  const [query,setQuery]=useState('');
  const [status,setStatus]=useState('');
  useEffect(()=>{
    const abort=new AbortController();
    setScenes([]);setStatus('');
    void apiFetch<Scene[]>(`/api/v1/playback/scenes?profileId=${encodeURIComponent(profileId)}&titleId=${encodeURIComponent(titleId)}`,
      token,{signal:abort.signal}).then(data=>{if(!abort.signal.aborted)setScenes(data);})
      .catch(()=>{if(!abort.signal.aborted)setStatus('Scenes are unavailable for this profile or connection.');});
    return()=>abort.abort();
  },[titleId,profileId,token]);
  const needle=query.trim().toLocaleLowerCase();
  const filtered=scenes.filter(s=>(s.label+' '+s.summary).toLocaleLowerCase().includes(needle));
  if(!status && scenes.length===0)return null;
  return <section className="studio-panel" aria-label="Explore scenes" style={{marginTop:18}}>
    <h2>Explore scenes</h2>
    {status&&<p role="status">{status}</p>}
    {scenes.length>0&&<><label>Find a scene <input type="search" maxLength={80} value={query}
      onChange={e=>setQuery(e.target.value)} placeholder="Search reviewed chapter names"/></label>
    <div className="studio-list">{filtered.slice(0,40).map(scene=><div key={scene.ordinal} className="studio-list-item">
      <span><strong>{scene.label}</strong><small>{scene.summary}</small></span>
      <button type="button" className="btn btn-outline" disabled={!canSeek||scene.startSeconds>=duration}
        onClick={()=>onSeek(scene.startSeconds)}>Jump to {Math.floor(scene.startSeconds/60)}:{String(scene.startSeconds%60).padStart(2,'0')}</button>
    </div>)}</div>
    {filtered.length===0&&<p>No matching reviewed scenes.</p>}</>}
  </section>;
}
