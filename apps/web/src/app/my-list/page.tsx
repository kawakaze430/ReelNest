'use client';

import {useEffect, useState} from 'react';
import Link from 'next/link';
import {BookmarkX, Heart, LoaderCircle, Trash2} from 'lucide-react';
import {ProfilePicker} from '@/components/profile-picker';
import {Poster} from '@/components/poster';
import {useActiveProfile} from '@/hooks/use-active-profile';
import {apiFetch} from '@/lib/api';
import type {Title} from '@/types/catalog';

export default function MyListPage(){
  const p=useActiveProfile();
  return <MyListView key={`${p.auth.user?.profile.sub ?? 'anonymous'}:${p.profileId}`} p={p}/>;
}

function MyListView({p}:{p:ReturnType<typeof useActiveProfile>}){
  const [titles,setTitles]=useState<Title[]>([]);
  const [busy,setBusy]=useState(Boolean(p.token&&p.profileId));
  const [error,setError]=useState('');
  useEffect(()=>{
    if(!p.token||!p.profileId)return;
    let cancelled=false;
    apiFetch<Title[]>(`/api/v1/watchlist?profileId=${p.profileId}`,p.token)
      .then(data=>{if(!cancelled)setTitles(data);})
      .catch(()=>{if(!cancelled)setError('Could not load your watchlist.');})
      .finally(()=>{if(!cancelled)setBusy(false);});
    return ()=>{cancelled=true;};
  },[p.token,p.profileId]);
  async function remove(id:string){
    if(!p.token||!p.profileId)return;
    try{await apiFetch<void>(`/api/v1/watchlist/${id}?profileId=${p.profileId}`,p.token,{method:'DELETE'});setTitles(list=>list.filter(t=>t.id!==id));}
    catch{setError('Could not remove this title.');}
  }
  return <main className="wrap library-shell">
    <div className="library-heading"><div className="eyebrow"><Heart size={15}/> YOUR COLLECTION</div><h1>My List<span className="title-period">.</span></h1><p>Saved titles belong to the selected viewer profile.</p><ProfilePicker/></div>
    {error&&<div className="notice studio-error" role="alert">{error}</div>}
    {busy&&<p><LoaderCircle className="spin" size={17}/> Loading your list...</p>}
    {!busy&&!titles.length&&<div className="library-empty"><BookmarkX size={37}/><h2>No saved titles yet</h2><p>Browse the catalog and save films for later.</p><Link className="btn btn-primary" href="/browse">Explore movies</Link></div>}
    <div className="catalog-grid library-grid">{titles.map(t=><div key={t.id} className="library-card"><Poster title={t}/><button className="btn btn-outline library-remove" onClick={()=>void remove(t.id)}><Trash2 size={14}/> Remove</button></div>)}</div>
  </main>;
}
