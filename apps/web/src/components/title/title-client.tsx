'use client';

import {useParams} from 'next/navigation';
import Link from 'next/link';
import {useEffect,useState} from 'react';
import {ArrowLeft,ShieldAlert,Play,BadgeInfo,BookmarkPlus,BookmarkCheck} from 'lucide-react';
import {apiFetch,ApiError} from '@/lib/api';
import {demoTitles} from '@/lib/demo-data';
import {useActiveProfile} from '@/hooks/use-active-profile';
import {ProfilePicker} from '@/components/profile-picker';
import {ContextualPreviewPanel} from '@/components/contextual-preview-panel';
import type {Title} from '@/types/catalog';

type SeriesEpisode = {titleId:string;name:string;synopsis:string;seasonNumber:number;episodeNumber:number;
  runtimeMinutes:number|null;ageRating:string;artTone:string};
export function TitleClient(){
  const {id}=useParams<{id:string}>();
  const viewer=useActiveProfile();
  const [reload,setReload]=useState(0);
  return <TitleView key={`${id}:${viewer.auth.user?.profile.sub ?? 'anonymous'}:${viewer.profileId}:${reload}`}
    id={id} viewer={viewer} onRetry={()=>setReload(n=>n+1)}/>;
}

function TitleView({id,viewer,onRetry}:{id:string;viewer:ReturnType<typeof useActiveProfile>;onRetry:()=>void}){
  const [title,setTitle]=useState<Title|undefined>(()=>demoTitles.find(t=>t.id===id));
  const [notFound,setNotFound]=useState(false);
  const [loading,setLoading]=useState(true);
  const [catalogError,setCatalogError]=useState(false);
  const [notice,setNotice]=useState('');
  const [saved,setSaved]=useState(false);
  const [busy,setBusy]=useState(false);
  const [episodes,setEpisodes]=useState<SeriesEpisode[]>([]);
  const [season,setSeason]=useState(1);
  const [availability,setAvailability]=useState<{playable:boolean;code:string;message:string}|null>(null);
  const [availabilityLoading,setAvailabilityLoading]=useState(Boolean(viewer.token&&viewer.profileId));
  useEffect(()=>{
    let active=true;
    const fallback=demoTitles.find(t=>t.id===id);
    apiFetch<Title>(`/api/v1/catalog/${encodeURIComponent(id)}`)
      .then(t=>{if(active){setTitle(t);setLoading(false);}})
      .catch(e=>{if(active){
        setLoading(false);
        if(!fallback){
          if(e instanceof ApiError && e.status===404)setNotFound(true);
          else setCatalogError(true);
        }
      }});
    return()=>{active=false;};
  },[id]);
  useEffect(()=>{
    if(title?.type!=='SERIES' || title.id!==id)return;
    let dead=false;
    void apiFetch<SeriesEpisode[]>(`/api/v1/catalog/${encodeURIComponent(id)}/episodes`)
      .then(items=>{if(!dead)setEpisodes(items);}).catch(()=>{});
    return()=>{dead=true;};
  },[id,title?.id,title?.type]);
  useEffect(()=>{
    if(!viewer.token||!viewer.profileId)return;
    let dead=false;
    apiFetch<Title[]>(`/api/v1/watchlist?profileId=${viewer.profileId}`,viewer.token)
      .then(list=>{if(!dead)setSaved(list.some(t=>t.id===id));})
      .catch(()=>{if(!dead)setSaved(false);});
    return()=>{dead=true;};
  },[id,viewer.token,viewer.profileId]);
  useEffect(()=>{
    if(!viewer.token||!viewer.profileId)return;
    let dead=false;
    apiFetch<{playable:boolean;code:string;message:string}>(
      `/api/v1/playback/availability?profileId=${viewer.profileId}&titleId=${id}`,viewer.token)
      .then(result=>{if(!dead)setAvailability(result);})
      .catch(()=>{if(!dead)setAvailability({playable:false,code:'NOT_AVAILABLE',message:'Playback availability could not be verified'});})
      .finally(()=>{if(!dead)setAvailabilityLoading(false);});
    return()=>{dead=true;};
  },[viewer.token,viewer.profileId,id]);
  useEffect(()=>{
    if(!viewer.token||!viewer.profileId||!title||title.id!==id)return;
    const eventId=crypto.randomUUID();
    void apiFetch<void>('/api/v1/discovery/events',viewer.token,{
      method:'POST',body:JSON.stringify({eventId,profileId:viewer.profileId,titleId:id,eventType:'DETAIL_VIEW'})
    }).catch(()=>{});
  },[viewer.token,viewer.profileId,id,title]);
  async function toggleSave(){
    if(!viewer.token||!viewer.profileId){setNotice('Sign in and choose a viewer profile to save this title.');return;}
    setBusy(true);setNotice('');
    try{
      await apiFetch<void>(`/api/v1/watchlist/${id}?profileId=${viewer.profileId}`,viewer.token,{method:saved?'DELETE':'PUT'});
      setSaved(!saved);setNotice(saved?'Removed from My List.':'Saved to My List for this profile.');
    }catch{setNotice('Could not update your list. Check that you are signed in and the API is available.');}
    finally{setBusy(false);}
  }
  if(loading&&!title)return <main className="wrap empty" aria-busy="true" role="status">
    <div className="reelnest-skeleton"/><div className="reelnest-skeleton reelnest-skeleton-short"/>
    <p>Loading title details...</p></main>;
  if(catalogError&&!title)return <main className="wrap empty" role="alert"><h1>Catalog temporarily unavailable</h1>
    <p>We could not reach the catalog. Your account and playback access were not changed.</p>
    <button className="btn btn-primary" type="button" onClick={onRetry}>Try again</button>
    <Link href="/browse" className="btn btn-outline">Back to catalog</Link></main>;
  if(notFound||!title)return <main className="wrap empty"><h1>Title not available</h1><Link href="/browse" className="btn btn-outline">Back to catalog</Link></main>;
  return <main className={`detail-page poster-${title.artTone}`}><div className="detail-backdrop"><div className="detail-orb"/></div><div className="wrap detail-container">
    <Link href="/browse" className="back-link"><ArrowLeft size={17}/> All titles</Link>
    <div className="detail-content"><div className="eyebrow">REELNEST / {title.type}</div><h1>{title.name}</h1>
      <div className="hero-info"><span className="rating-pill">{title.ageRating}</span><span>{title.releaseYear}</span><span>{title.genre}</span><span>{title.runtimeMinutes} min</span><span>{title.language.toUpperCase()}</span></div>
      <p>{title.synopsis}</p>
      <div className="hero-buttons">
        {availability?.playable ?
          <Link className="btn btn-primary" href={`/watch/${title.id}`}><Play size={17} fill="currentColor"/> Watch now</Link> :
          <button className="btn btn-primary" type="button" disabled title={availability?.message ?? 'Select a profile to check playback'}>
            <Play size={17} fill="currentColor"/> {availabilityLoading?'Checking availability...':'Video unavailable'}
          </button>}
        <button type="button" className={`btn btn-outline ${saved?'detail-saved':''}`} onClick={()=>void toggleSave()} disabled={busy}>
          {saved?<BookmarkCheck size={16}/>:<BookmarkPlus size={16}/>} {saved?'Saved to My List':'Add to My List'}
        </button>
        {availability?.code==='SUBSCRIPTION_REQUIRED'&&<Link href="/pricing" className="btn btn-primary">See membership plans</Link>}
        {availability?.playable&&<Link className="btn btn-outline" href={`/watch-party?titleId=${encodeURIComponent(title.id)}`}>Watch with friends</Link>}
        <Link href="/my-list" className="btn btn-outline">View My List</Link>
      </div>
      <div style={{marginTop:23}}><ProfilePicker compact/></div>
      {availability?.playable&&viewer.token&&viewer.profileId&&title.accessTier==='FREE'&&
        <ContextualPreviewPanel token={viewer.token} profileId={viewer.profileId} titleId={title.id}/>}
      {availability&&!availability.playable&&<div className="notice notice-dark" role="status"><ShieldAlert size={18}/>{availability.message}</div>}
      {notice&&<div className="notice notice-dark" role="status"><BadgeInfo size={18}/>{notice}</div>}
      <div className="notice notice-dark" role="note"><ShieldAlert size={18}/> Fictional seed catalog entries contain metadata only and do not have licensed videos. Playback requires a published, rights-approved creator film with READY HLS media. <Link href="/lab">View the public original HLS lab clip →</Link></div>
      {title.type==='SERIES'&&<section className="series-episodes" aria-label="Series episodes">
        <div className="section-title"><h2>Episodes</h2>
          {episodes.length>0&&<label>Season <select value={season} onChange={e=>setSeason(Number(e.target.value))}>
            {Array.from(new Set(episodes.map(e=>e.seasonNumber))).map(s=><option key={s} value={s}>{s}</option>)}
          </select></label>}</div>
        {episodes.length===0?<p>No published episodes have been linked to this series yet.</p>:
          <div className="series-episode-grid">{episodes.filter(e=>e.seasonNumber===season).map(e=><article key={e.titleId} className="series-episode-card">
            <span>EPISODE {e.episodeNumber} · {e.runtimeMinutes??'?'} MIN · {e.ageRating}</span>
            <h3>{e.name}</h3><p>{e.synopsis}</p>
            <Link className="btn btn-outline" href={`/title/${encodeURIComponent(e.titleId)}`}>View episode and availability →</Link>
          </article>)}</div>}
      </section>}
    </div><div className="detail-facts">    </div><div className="detail-facts"><div><BadgeInfo size={19}/><span>Media access</span><strong>Verified at play time</strong></div><div><ShieldAlert size={19}/><span>Content rights</span><strong>Required to stream</strong></div></div>
  </div></main>;
}
