'use client';
import Link from 'next/link';
import {useEffect,useState,useSyncExternalStore} from 'react';
import {apiFetch} from '@/lib/api';
type Program={id:string;titleId:string;title:string;startsAt:string;endsAt:string};
type Guide={channel:{id:string;slug:string;name:string;description:string};programs:Program[]};
let clockTime=0;
function subscribeClock(notify:()=>void){
 clockTime=Date.now();
 const timer=window.setInterval(()=>{clockTime=Date.now();notify();},1000);
 return()=>window.clearInterval(timer);
}
function clockSnapshot(){return clockTime;}
function serverClockSnapshot(){return 0;}
export default function ChannelsPage(){
 const [guides,setGuides]=useState<Guide[]>([]),[error,setError]=useState(''),[busy,setBusy]=useState(true);
 useEffect(()=>{const c=new AbortController();apiFetch<Guide[]>('/api/v1/catalog/channels',undefined,{signal:c.signal})
 .then(setGuides).catch(()=>{if(!c.signal.aborted)setError('Guide could not be loaded.')})
 .finally(()=>{if(!c.signal.aborted)setBusy(false)});return()=>c.abort();},[]);
 const now=useSyncExternalStore(subscribeClock,clockSnapshot,serverClockSnapshot);
 return <main className="wrap viewer-shell"><div className="eyebrow">DISCOVER / PROGRAM GUIDE</div><h1>Scheduled channels<span className="title-period">.</span></h1>
 <p>Explore editorial schedules. This is an on-demand program guide, not a live broadcast; each film still requires a separate authorized playback session.</p>
 {busy&&<p>Loading guide…</p>}{error&&<p role="alert">{error}</p>}
 {!busy&&!error&&guides.length===0&&<p>No channels are currently published.</p>}
 {guides.map(g=><section key={g.channel.id} style={{margin:'28px 0'}}><h2>{g.channel.name}</h2><p>{g.channel.description}</p>
 <ol style={{display:'grid',gap:10,paddingLeft:20}}>{g.programs.map(p=>{const current=Date.parse(p.startsAt)<=now&&now<Date.parse(p.endsAt);
 return <li key={p.id} className="notice"><time dateTime={p.startsAt}>{new Date(p.startsAt).toLocaleString()}</time>–<time dateTime={p.endsAt}>{new Date(p.endsAt).toLocaleTimeString()}</time>
 {current&&<strong> · Scheduled now</strong>} — <Link href={`/title/${encodeURIComponent(p.titleId)}`}>{p.title}</Link></li>})}</ol>
 {g.programs.length===0&&<p>No eligible films are currently scheduled.</p>}</section>)}
 </main>;
}
