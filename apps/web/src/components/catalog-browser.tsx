'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Search, SlidersHorizontal, Film, ArrowRight, Play, Sparkles, AlertCircle } from 'lucide-react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { apiFetch, getCatalog } from '@/lib/api';
import { demoTitles } from '@/lib/demo-data';
import { Poster } from '@/components/poster';
import { ProfilePicker } from '@/components/profile-picker';
import { useActiveProfile } from '@/hooks/use-active-profile';
import type { Page, Title } from '@/types/catalog';
import {RecommendationFeed} from '@/components/recommendation-feed';
import {TvRemoteMode} from '@/components/tv-remote-mode';

export function CatalogBrowser() {
  const searchParams = useSearchParams();
  const [titles, setTitles] = useState<Title[]>(demoTitles);
  const [previewMode, setPreviewMode] = useState(true);
  const [loadingState, setLoading] = useState(true);
  const [initialized,setInitialized]=useState(false);
  const [searchErrorState,setSearchError]=useState('');
  const [resultKey,setResultKey]=useState<string|null>(null);
  const [genres,setGenres]=useState<string[]>(['All']);
  const [pageIndex,setPageIndex]=useState(0);
  const [totalPages,setTotalPages]=useState(1);
  const queryGeneration=useRef(0);
  const [query, setQuery] = useState('');
  const [genre, setGenre] = useState('All');
  const [suggestionResult,setSuggestionResult] = useState<{query:string;items:{titleId:string;name:string;slug:string}[]}|null>(null);
  const [language, setLanguage] = useState('All');
  const rawType=searchParams.get('type');
  const contentType=rawType==='MOVIE'||rawType==='SERIES'?rawType:null;
  const PAGE_SIZE=48;
  const requestKey=JSON.stringify([query.trim(),genre,language,contentType]);
  const loading=loadingState||(!previewMode&&initialized&&resultKey!==requestKey);
  const searchError=resultKey===requestKey?searchErrorState:'';
  const suggestions=!previewMode&&query.trim().length>=2&&suggestionResult?.query===query.trim()?suggestionResult.items:[];
  const searchPath=useCallback((page:number)=>{
    const params=new URLSearchParams({page:String(page),size:String(PAGE_SIZE)});
    if(genre!=='All')params.set('genre',genre);
    if(language!=='All')params.set('language',language);
    if(query.trim())params.set('q',query.trim());
    if(contentType)params.set('type',contentType);
    return '/api/v1/catalog?'+params.toString();
  },[genre,language,query,contentType]);
  const viewer = useActiveProfile();
  const allowed = useMemo(() => {
    if(!previewMode&&resultKey!==requestKey)return [];
    return viewer.active?.child ? titles.filter(t => t.ageRating === 'U' || (t.ageRating === 'U/A 7+' || t.ageRating === 'UA7')) : titles;
  }, [titles,viewer.active?.child,previewMode,resultKey,requestKey]);
  useEffect(() => {
    const controller = new AbortController();
    getCatalog(controller.signal)
      .then(result => {
        if(controller.signal.aborted)return;
        setTitles(result);
        setGenres(['All',...new Set(result.map(t=>t.genre).filter(Boolean))]);
        setPreviewMode(false);
      })
      .catch(() => { if (!controller.signal.aborted) setPreviewMode(true); })
      .finally(() => { if (!controller.signal.aborted) {setLoading(false);setInitialized(true);} });
    return () => controller.abort();
  }, []);
  useEffect(()=>{
    if(!initialized||previewMode)return;
    const generation=++queryGeneration.current;
    const controller=new AbortController();
    const handle=setTimeout(()=>{
      void apiFetch<Page<Title>>(searchPath(0),undefined,{signal:controller.signal})
        .then(response=>{
          if(controller.signal.aborted||queryGeneration.current!==generation)return;
          setTitles(response.content);
          setResultKey(requestKey);setSearchError('');
          setPageIndex(response.number);
          setTotalPages(response.totalPages);
        })
        .catch(()=>{
          if(controller.signal.aborted||queryGeneration.current!==generation)return;
          // Do not show a stale, previously licensed result on a failed request.
          setTitles([]);setResultKey(requestKey);setSearchError('Live catalog search failed. Retry after checking your connection.');
        })
        .finally(()=>{if(!controller.signal.aborted&&queryGeneration.current===generation)setLoading(false);});
    },220);
    return()=>{controller.abort();clearTimeout(handle);};
  },[searchPath,previewMode,initialized,requestKey]);
  async function loadMore(){
    if(previewMode||loading||pageIndex+1>=totalPages)return;
    const generation=queryGeneration.current;
    setLoading(true);setSearchError('');
    try{
      const response=await apiFetch<Page<Title>>(searchPath(pageIndex+1));
      if(queryGeneration.current!==generation)return;
      setTitles(previous=>{
        const seen=new Set(previous.map(t=>t.id));
        return [...previous,...response.content.filter(t=>!seen.has(t.id))];
      });
      setPageIndex(response.number);setTotalPages(response.totalPages);
    }catch{
      if(queryGeneration.current===generation)setSearchError('Could not load more titles.');
    }finally{if(queryGeneration.current===generation)setLoading(false);}
  }
  useEffect(()=>{
    if(previewMode || query.trim().length<2)return;
    const abort=new AbortController();
    const handle=setTimeout(()=>{
      apiFetch<{titleId:string;name:string;slug:string}[]>(`/api/v1/catalog/suggest?q=${encodeURIComponent(query.trim())}&limit=6`,undefined,{signal:abort.signal})
        .then(items=>{if(!abort.signal.aborted)setSuggestionResult({query:query.trim(),items});})
        .catch(()=>{if(!abort.signal.aborted)setSuggestionResult({query:query.trim(),items:[]});});
    },250);
    return ()=>{clearTimeout(handle);abort.abort();};
  },[query,previewMode]);
  const filtered = useMemo(() => allowed.filter(t =>
    (!contentType || t.type === contentType) &&
    (!previewMode || (
      (genre === 'All' || t.genre === genre) &&
      (language === 'All' || t.language === language) &&
      (t.name.toLowerCase().includes(query.toLowerCase()) ||
       t.synopsis.toLowerCase().includes(query.toLowerCase()))
    ))
  ), [allowed, genre, language, query, contentType, previewMode]);
  const featured = allowed.find(t => t.featured) ?? allowed[0];
  return <main className="browse-page" data-tv-zone>
    <div className="wrap" style={{paddingTop:12}}><TvRemoteMode/></div>
    {featured && <section className={`browse-hero poster-${featured.artTone}`}>
      <div className="hero-decoration" aria-hidden="true"><div className="hero-circle"/><div className="hero-rings"/></div>
      <div className="wrap hero-content">
        <div className="eyebrow"><Sparkles size={13}/> THE FEATURED COLLECTION <span className="dash"/></div>
        <h1>{featured.name}</h1>
        <div className="hero-info"><span className="rating-pill">{featured.ageRating}</span><span>{featured.releaseYear}</span><span>{featured.genre}</span><span>{featured.runtimeMinutes} min</span></div>
        <p>{featured.synopsis}</p>
        <div className="hero-buttons"><Link className="btn btn-primary" href={`/title/${featured.id}`}><Play size={17} fill="currentColor"/> Explore title</Link><a className="btn btn-outline" href="#catalog">Browse collection <ArrowRight size={17}/></a></div>
      </div>
      <div className="hero-footnote">FICTIONAL DEMO CATALOG · NO VIDEO MEDIA INCLUDED</div>
    </section>}
    <section id="catalog" className="wrap catalog-section">
      <div className="section-title"><div><div className="eyebrow">EXPLORE THE UNEXPECTED</div><h2>{contentType === 'MOVIE' ? 'All movies' : contentType === 'SERIES' ? 'All series' : 'Discover your next obsession'}<span className="title-period">.</span></h2><p>Handpicked concepts, made for discovery.</p></div><div><ProfilePicker compact/><span className="results-count">{filtered.length} loaded titles</span></div></div>
      {previewMode && !loading && <div className="notice"><AlertCircle size={17}/><div><strong>Offline preview</strong> — API is not connected. Displaying fictional sample metadata from the project.</div></div>}
      <div className="filter-bar"><label className="search-box"><Search size={19}/><input value={query} maxLength={120} onChange={e=>setQuery(e.target.value)} placeholder="Search titles, stories, or alternate spellings..." aria-label="Search titles" /></label><div className="filter-caption"><SlidersHorizontal size={16}/> FILTER BY</div><select aria-label="Filter by genre" value={genre} onChange={e=>setGenre(e.target.value)}>{genres.map(g=><option key={g}>{g}</option>)}</select><select aria-label="Filter by language" value={language} onChange={e=>setLanguage(e.target.value)}><option value="All">All languages</option><option value="en">English</option><option value="hi">Hindi</option></select></div>
      {query.trim().length>=2&&!previewMode&&suggestions.length>0&&<nav aria-label="Title search suggestions" className="search-suggestions">
        <strong>Suggested titles / alternate spellings</strong>
        {suggestions.map(item=><Link key={item.titleId} href={`/title/${item.titleId}`}>{item.name}</Link>)}
      </nav>}
      {searchError&&<p role="alert">{searchError}</p>}
      {loading&&<p role="status">Searching the live licensed catalog...</p>}
      <div className="catalog-grid">{filtered.map((title,index)=><Poster key={title.id} title={title} index={index}/>)}</div>
      {!previewMode&&pageIndex+1<totalPages&&<button type="button" className="btn btn-outline"
        disabled={loading} onClick={()=>void loadMore()}>Load more licensed titles</button>}
      {!loading&&filtered.length === 0 && <div className="empty"><Film size={36}/><h3>No matching titles</h3><p>Try different filters or a shorter search.</p><button onClick={()=>{setQuery('');setGenre('All');setLanguage('All');}}>Clear filters</button></div>}
      <div className="under-note">Seed titles are fictional metadata, not licensed videos. Protected playback now works only for approved creator content with READY HLS media. Subscription checkout and rights-checked recommendations are now implemented in Builds 05–06.</div>
    </section>
    <RecommendationFeed/>
  </main>;
}
