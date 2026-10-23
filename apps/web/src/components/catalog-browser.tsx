'use client';

import { useEffect, useMemo, useState } from 'react';
import { Search, SlidersHorizontal, Film, ArrowRight, Play, Sparkles, AlertCircle } from 'lucide-react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { apiFetch, getCatalog } from '@/lib/api';
import { demoTitles } from '@/lib/demo-data';
import { Poster } from '@/components/poster';
import { ProfilePicker } from '@/components/profile-picker';
import { useActiveProfile } from '@/hooks/use-active-profile';
import type { Title } from '@/types/catalog';
import {RecommendationFeed} from '@/components/recommendation-feed';
import {TvRemoteMode} from '@/components/tv-remote-mode';

export function CatalogBrowser() {
  const searchParams = useSearchParams();
  const [titles, setTitles] = useState<Title[]>(demoTitles);
  const [previewMode, setPreviewMode] = useState(true);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [genre, setGenre] = useState('All');
  const [suggestions,setSuggestions] = useState<{titleId:string;name:string;slug:string}[]>([]);
  const [language, setLanguage] = useState('All');
  const contentType = searchParams.get('type');
  const viewer = useActiveProfile();
  const allowed = useMemo(() => viewer.active?.child ? titles.filter(t => t.ageRating === 'U' || (t.ageRating === 'U/A 7+' || t.ageRating === 'UA7')) : titles, [titles,viewer.active?.child]);
  useEffect(() => {
    const controller = new AbortController();
    getCatalog(controller.signal)
      .then(result => { setTitles(result); setPreviewMode(false); })
      .catch(() => { if (!controller.signal.aborted) setPreviewMode(true); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, []);
  useEffect(()=>{
    if(previewMode || query.trim().length<2){return;}
    const abort=new AbortController();
    const handle=setTimeout(()=>{
      apiFetch<{titleId:string;name:string;slug:string}[]>(`/api/v1/catalog/suggest?q=${encodeURIComponent(query.trim())}&limit=6`,undefined,{signal:abort.signal})
        .then(items=>{if(!abort.signal.aborted)setSuggestions(items);})
        .catch(()=>{if(!abort.signal.aborted)setSuggestions([]);});
    },250);
    return ()=>{clearTimeout(handle);abort.abort();};
  },[query,previewMode]);
  const genres = useMemo(() => ['All', ...new Set(allowed.map(title => title.genre))], [allowed]);
  const filtered = useMemo(() => allowed.filter(t =>
    (genre === 'All' || t.genre === genre) &&
    (language === 'All' || t.language === language) &&
    (!contentType || t.type === contentType) &&
    (t.name.toLowerCase().includes(query.toLowerCase()) || t.synopsis.toLowerCase().includes(query.toLowerCase()))
  ), [allowed, genre, language, query, contentType]);
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
      <div className="section-title"><div><div className="eyebrow">EXPLORE THE UNEXPECTED</div><h2>{contentType === 'MOVIE' ? 'All movies' : contentType === 'SERIES' ? 'All series' : 'Discover your next obsession'}<span className="title-period">.</span></h2><p>Handpicked concepts, made for discovery.</p></div><div><ProfilePicker compact/><span className="results-count">{filtered.length} titles</span></div></div>
      {previewMode && !loading && <div className="notice"><AlertCircle size={17}/><div><strong>Offline preview</strong> — API is not connected. Displaying fictional sample metadata from the project.</div></div>}
      <div className="filter-bar"><label className="search-box"><Search size={19}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search titles, stories..." aria-label="Search titles" /></label><div className="filter-caption"><SlidersHorizontal size={16}/> FILTER BY</div><select aria-label="Filter by genre" value={genre} onChange={e=>setGenre(e.target.value)}>{genres.map(g=><option key={g}>{g}</option>)}</select><select aria-label="Filter by language" value={language} onChange={e=>setLanguage(e.target.value)}><option value="All">All languages</option><option value="en">English</option><option value="hi">Hindi</option></select></div>
      {query.trim().length>=2&&!previewMode&&suggestions.length>0&&<nav aria-label="Title search suggestions" className="search-suggestions">
        <strong>Suggested titles / alternate spellings</strong>
        {suggestions.map(item=><Link key={item.titleId} href={`/title/${item.titleId}`}>{item.name}</Link>)}
      </nav>}
      <div className="catalog-grid">{filtered.map((title,index)=><Poster key={title.id} title={title} index={index}/>)}</div>
      {filtered.length === 0 && <div className="empty"><Film size={36}/><h3>No matching titles</h3><p>Try different filters or a shorter search.</p><button onClick={()=>{setQuery('');setGenre('All');setLanguage('All');}}>Clear filters</button></div>}
      <div className="under-note">Seed titles are fictional metadata, not licensed videos. Protected playback now works only for approved creator content with READY HLS media. Subscription checkout and rights-checked recommendations are now implemented in Builds 05–06.</div>
    </section>
    <RecommendationFeed/>
  </main>;
}
