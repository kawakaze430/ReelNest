'use client';
import {useEffect,useState} from 'react';
import Link from 'next/link';
import {BrainCircuit,ShieldCheck} from 'lucide-react';
import {apiFetch} from '@/lib/api';
import {useActiveProfile} from '@/hooks/use-active-profile';
import {Poster} from '@/components/poster';
import type {Title} from '@/types/catalog';

type Recommendation={title:Title;score:number;reason:string};
export function RecommendationFeed(){
  const viewer=useActiveProfile();const [data,setData]=useState<Recommendation[]>([]);
  const [error,setError]=useState('');const [loading,setLoading]=useState(false);
  useEffect(()=>{
    if(!viewer.token||!viewer.profileId){setData([]);return;}
    let active=true;setLoading(true);setError('');
    apiFetch<Recommendation[]>(`/api/v1/discovery/home?profileId=${viewer.profileId}&limit=18`,viewer.token)
      .then(results=>{if(active)setData(results);})
      .catch(()=>{if(active)setError('Recommendations could not be loaded.');})
      .finally(()=>{if(active)setLoading(false);});
    return()=>{active=false;};
  },[viewer.token,viewer.profileId]);
  return <section className="wrap recommendation-shell">
    <div className="section-title"><div><div className="eyebrow"><BrainCircuit size={15}/> PROFILE-AWARE DISCOVERY</div><h2>Recommended for you<span className="title-period">.</span></h2><p>Rights-checked films ranked using your genres, watch activity, preferred language and recent popularity.</p></div><Link href="/browse" className="text-link">Explore catalog</Link></div>
    {!viewer.token?<div className="library-empty"><BrainCircuit size={28}/><h3>Sign in for recommendations</h3><p>Your preferences and viewing activity can personalize your discovery feed.</p><Link className="btn btn-primary" href="/login">Sign in</Link></div>:
      loading?<p className="recommendation-note">Finding playable titles in your territory…</p>:
      error?<div className="notice" role="alert">{error}</div>:
      !data.length?<div className="library-empty"><ShieldCheck size={27}/><h3>No licensed films to recommend yet</h3><p>Creator videos must be approved, processed and published. Fictional seed titles intentionally do not appear as playable recommendations.</p><Link href="/browse" className="btn btn-outline">Browse concept catalog</Link></div>:
      <div className="recommendation-grid">{data.map((item,i)=><div key={item.title.id}><Poster title={item.title} index={i}/><p className="recommendation-note">{item.reason} · Match {item.score}</p></div>)}</div>}
  </section>;
}
