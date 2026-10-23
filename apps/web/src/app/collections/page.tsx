'use client';
import Link from 'next/link';
import {useEffect,useState} from 'react';
import {apiFetch} from '@/lib/api';
type Card={id:string;name:string;genre:string;language:string;ageRating:string;accessTier:string};
type Collection={id:string;slug:string;heading:string;description:string;language:string|null;titles:Card[]};
export default function CollectionsPage(){
 const [data,setData]=useState<Collection[]>([]),[error,setError]=useState(''),[loading,setLoading]=useState(true);
 useEffect(()=>{const controller=new AbortController();apiFetch<Collection[]>('/api/v1/catalog/collections',undefined,{signal:controller.signal})
 .then(setData).catch(e=>{if(!controller.signal.aborted)setError(String(e))}).finally(()=>{if(!controller.signal.aborted)setLoading(false)});
 return()=>controller.abort();},[]);
 return <main className="wrap viewer-shell"><div className="eyebrow">DISCOVER / CURATED</div><h1>Editorial collections<span className="title-period">.</span></h1>
 <p>Thoughtfully selected stories. Streaming is always subject to your subscription, profile restrictions and current film rights.</p>
 {loading&&<p role="status">Loading collections…</p>}{error&&<p role="alert">Collections are temporarily unavailable. {error}</p>}
 {!loading&&!error&&data.length===0&&<p>No active collections have been published.</p>}
 {data.map(c=><section key={c.id} aria-label={c.heading} style={{margin:'28px 0'}}><h2>{c.heading}</h2><p>{c.description}</p>
 <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fit,minmax(210px,1fr))',gap:16}}>
 {c.titles.map(t=><Link key={t.id} href={`/title/${encodeURIComponent(t.id)}`} className="notice" style={{textDecoration:'none'}}>
 <strong>{t.name}</strong><p>{t.genre} · {t.language} · {t.ageRating}</p><small>{t.accessTier==='PREMIUM'?'Premium access':'Free selection'}</small></Link>)}
 </div>{c.titles.length===0&&<p>Films in this collection are not currently available.</p>}</section>)}
 </main>;
}
