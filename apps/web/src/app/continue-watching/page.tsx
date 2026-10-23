'use client';
import {useEffect,useState} from 'react';
import Link from 'next/link';
import {Play,Clock3,Trash2} from 'lucide-react';
import {ProfilePicker} from '@/components/profile-picker';
import {useActiveProfile} from '@/hooks/use-active-profile';
import {apiFetch} from '@/lib/api';
import {prettyTime,type ContinueItem} from '@/lib/playback';

export default function ContinueWatchingPage(){
  const p=useActiveProfile();
  const [items,setItems]=useState<ContinueItem[]>([]);
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState('');
  useEffect(()=>{
    if(!p.token||!p.profileId){setItems([]);setLoading(false);return;}
    let dead=false;setLoading(true);setError('');
    apiFetch<ContinueItem[]>(`/api/v1/playback/continue-watching?profileId=${p.profileId}`,p.token)
      .then(rows=>{if(!dead)setItems(rows);})
      .catch(()=>{if(!dead)setError('Could not retrieve playback history.');})
      .finally(()=>{if(!dead)setLoading(false);});
    return ()=>{dead=true;};
  },[p.token,p.profileId]);
  async function clear(titleId:string){
    if(!p.token||!p.profileId)return;
    try{
      await apiFetch<void>(`/api/v1/playback/continue-watching/${titleId}?profileId=${p.profileId}`,p.token,{method:'DELETE'});
      setItems(list=>list.filter(x=>x.titleId!==titleId));
    }catch{setError('Could not clear this viewing history.');}
  }
  return <main className="wrap library-shell">
    <div className="library-heading"><div className="eyebrow"><Clock3 size={15}/> YOUR VIEWING HISTORY</div><h1>Continue Watching<span className="title-period">.</span></h1><p>Resume unfinished, currently authorized films.</p><ProfilePicker/></div>
    {error&&<div className="notice studio-error" role="alert">{error}</div>}
    {loading&&<p>Loading watch progress...</p>}
    {!loading&&!items.length&&<div className="library-empty"><Clock3 size={37}/><h2>No films in progress</h2><p>Once you watch an approved film, it appears here. Fictional demo titles have no playable video.</p><Link href="/browse" className="btn btn-primary">Browse titles</Link></div>}
    <div className="continue-grid">{items.map(item=><div className={`continue-card poster-${item.artTone}`} key={item.mediaAssetId}>
      <div className="eyebrow">{item.genre} · {item.ageRating}</div><h2>{item.name}</h2>
      <div className="continue-progress"><div style={{width:`${Math.min(100,Math.round(100*item.positionSeconds/Math.max(1,item.durationSeconds)))}%`}}/></div>
      <p>{prettyTime(item.positionSeconds)} / {prettyTime(item.durationSeconds)}</p>
      <div className="row" style={{display:'flex',gap:10,flexWrap:'wrap',marginTop:'auto'}}>
        <Link href={`/watch/${item.titleId}`} className="btn btn-primary"><Play size={16}/> Resume playback</Link>
        <button className="btn btn-outline" title="Remove from Continue Watching" onClick={()=>void clear(item.titleId)}><Trash2 size={15}/> Remove</button>
      </div>
    </div>)}</div>
  </main>;
}
