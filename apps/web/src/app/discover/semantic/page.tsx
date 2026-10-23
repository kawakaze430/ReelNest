'use client';
import {useAuth} from 'react-oidc-context';
import {useState} from 'react';
import {useActiveProfile} from '@/hooks/use-active-profile';
import {apiFetch} from '@/lib/api';
import type {Title} from '@/types/catalog';
type Item={title:Title;score:number;reason:string};
export default function SemanticDiscovery(){
  const auth=useAuth(); const viewer=useActiveProfile(); const [query,setQuery]=useState('');
  const [results,setResults]=useState<Item[]>([]),[busy,setBusy]=useState(false),[error,setError]=useState('');
  async function search(){if(!auth.user?.access_token)return;
    setBusy(true);setError('');setResults([]);
    try{
      if(!viewer.profileId)throw new Error('Select a profile on the Profiles page first.');
      const q=query.trim();if(!q || q.length>200)throw new Error('Enter a search under 200 characters.');
      const params=new URLSearchParams({profileId:viewer.profileId,q,limit:'12'});
      setResults(await apiFetch<Item[]>(`/api/v1/discovery/search/semantic?${params}`,auth.user.access_token));
    }catch(e){setError(e instanceof Error?e.message:'Search failed');}finally{setBusy(false)}
  }
  return <main className="wrap viewer-shell"><div className="eyebrow">DISCOVER / PRIVATE SEARCH</div><h1>Explore by description<span className="title-period">.</span></h1>
    <p>Searches only the current profile&apos;s rights-eligible catalog. Uses TF-IDF retrieval, not a conversational LLM.</p>
    {!auth.user?<button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in</button>:<>
      <p>Searching for active profile: <strong>{viewer.active?.name ?? "Select a profile first"}</strong></p>
      <label>What would you like to watch? <input maxLength={200} value={query} onChange={e=>setQuery(e.target.value)} placeholder="A mystery involving space explorers" /></label>
      <button className="btn btn-primary" onClick={()=>void search()} disabled={busy}>Search available films</button>
      {error&&<p role="alert" className="notice">{error}</p>}
      {!busy&&results.length===0&&!error&&<p>Enter a description to find matching licensed titles.</p>}
      <ul>{results.map(item=><li key={item.title.id}><a href={`/title/${item.title.id}`}>{item.title.name}</a> — {item.title.genre}</li>)}</ul>
    </>}
  </main>
}
