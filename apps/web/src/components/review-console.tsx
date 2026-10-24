'use client';

import { useAuth } from 'react-oidc-context';
import { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, ClipboardCheck, LoaderCircle, RefreshCcw, Shield, XCircle } from 'lucide-react';
import { apiFetch } from '@/lib/api';
import { MediaCueEditor } from '@/components/media-cue-editor';
import { SceneMarkerEditor } from '@/components/scene-marker-editor';
import type { Creator, ReviewTitle, Rights, MediaJob } from '@/types/creator';

export function ReviewConsole() {
  const auth=useAuth();
  return <AccountReviewConsole key={auth.isAuthenticated ? auth.user?.profile.sub : 'anonymous'} auth={auth}/>;
}
function AccountReviewConsole({auth}:{auth:ReturnType<typeof useAuth>}) {
  const token=auth.user?.access_token;
  const [creators,setCreators]=useState<Creator[]>([]);
  const [titles,setTitles]=useState<ReviewTitle[]>([]);
  const [seoTitles,setSeoTitles]=useState<{id:string;name:string;seo_index_approved:boolean}[]>([]);
  const [rights,setRights]=useState<Record<string,Rights>>({});
  const [media,setMedia]=useState<Record<string,MediaJob[]>>({});
  const [note,setNote]=useState<Record<string,string>>({});
  const [loading,setLoading]=useState(Boolean(token));
  const [error,setError]=useState('');const [message,setMessage]=useState('');
  const refresh=useCallback(async()=>{
    if(!token)return;
    setLoading(true);setError('');
    try {
      const [apps,pending,seo]=await Promise.all([
        apiFetch<Creator[]>('/api/v1/admin/creators/pending',token),
        apiFetch<ReviewTitle[]>('/api/v1/admin/titles/review',token),
        apiFetch<{id:string;name:string;seo_index_approved:boolean}[]>('/api/v1/admin/titles/seo-review',token)
      ]);
      setCreators(apps);setTitles(pending);setSeoTitles(seo);setRights({});setMedia({});
    } catch(e) {setError(e instanceof Error?e.message:'Admin API inaccessible. Sign in as the local reviewer account.');}
    finally {setLoading(false);}
  },[token]);
  useEffect(()=>{
    if(!token)return;
    const controller=new AbortController();
    Promise.all([
      apiFetch<Creator[]>('/api/v1/admin/creators/pending',token,{signal:controller.signal}),
      apiFetch<ReviewTitle[]>('/api/v1/admin/titles/review',token,{signal:controller.signal}),
      apiFetch<{id:string;name:string;seo_index_approved:boolean}[]>('/api/v1/admin/titles/seo-review',token,{signal:controller.signal})
    ]).then(([apps,pending,seo])=>{
      if(controller.signal.aborted)return;
      setCreators(apps);setTitles(pending);setSeoTitles(seo);setRights({});setMedia({});
    }).catch(e=>{
      if(!controller.signal.aborted)setError(e instanceof Error?e.message:'Admin API inaccessible. Sign in as the local reviewer account.');
    }).finally(()=>{if(!controller.signal.aborted)setLoading(false);});
    return()=>controller.abort();
  },[token]);

  async function act(path:string,body:unknown,confirmation:string) {
    if(!token)return;
    setError('');setMessage('');setLoading(true);
    try {
      await apiFetch<void>(path,token,{method:'POST',body:JSON.stringify(body)});
      setMessage(confirmation);await refresh();
    } catch(e) {setError(e instanceof Error?e.message:'Review failed');}
    finally {setLoading(false);}
  }
  async function loadRights(titleId:string) {
    if(!token)return;
    try {const r=await apiFetch<Rights>(`/api/v1/admin/titles/${titleId}/rights`,token);
      setRights(v=>({...v,[titleId]:r}));}
    catch(e){setError(e instanceof Error?e.message:'Could not fetch rights');}
  }

  async function loadMedia(titleId:string) {
    if(!token)return;
    try {const results=await apiFetch<MediaJob[]>(`/api/v1/admin/titles/${titleId}/media`,token);
      setMedia(v=>({...v,[titleId]:results}));}
    catch(e){setError(e instanceof Error?e.message:'Could not fetch processing status');}
  }

  if(auth.isLoading)return <main className="wrap studio-shell"><div className="empty"><LoaderCircle className="spin"/></div></main>;
  if(!auth.isAuthenticated)return <main className="wrap studio-shell"><div className="auth-gate"><div className="big-icon"><Shield/></div><h1>Content review</h1><p>Authorized administrators can review creator applications and content rights.</p><button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in as reviewer</button></div></main>;

  return <main className="wrap studio-shell"><div className="eyebrow">REELNEST / MODERATION</div>
    <div className="studio-head"><div><h1>Review Console<span className="title-period">.</span></h1><p>Administrator-only creator verification and distribution rights approval.</p></div><button className="btn btn-outline" onClick={()=>void refresh()}><RefreshCcw size={17}/> Refresh</button></div>
    <div className="notice"><Shield size={17}/>Review title agreements against original documentation. Approving rights does not bypass missing media inspection or automatically publish films.</div>
    {error&&<div className="studio-alert studio-error" role="alert"><XCircle size={18}/>{error}</div>}
    {message&&<div className="studio-alert studio-success" role="status"><CheckCircle2 size={18}/>{message}</div>}
    <section className="studio-panel"><div className="studio-step">01 / IDENTITY</div><h2>Creator applications <span className="count-pill">{creators.length}</span></h2>
      {creators.length===0?<p>No pending applications.</p>:<div className="review-grid">{creators.map(c=><article className="review-card" key={c.id}>
        <h3>{c.displayName}</h3><p>{c.legalName} · {c.contactEmail}</p>
        <p>{c.statement}</p>{c.portfolioUrl&&<a href={c.portfolioUrl} target="_blank" rel="noopener noreferrer">Creator portfolio ↗</a>}
        <label>Review note<input maxLength={1000} value={note[c.id]||''} onChange={e=>setNote(v=>({...v,[c.id]:e.target.value}))} placeholder="Reason and evidence checked"/></label>
        <div className="review-actions"><button disabled={loading} className="btn btn-primary" onClick={()=>void act(`/api/v1/admin/creators/${c.id}/review`,{decision:'APPROVED',note:note[c.id]||''},'Creator approved.')}>Approve</button>
        <button disabled={loading} className="btn btn-outline" onClick={()=>void act(`/api/v1/admin/creators/${c.id}/review`,{decision:'REJECTED',note:note[c.id]||''},'Application rejected.')}>Reject</button></div>
      </article>)}</div>}
    </section>
    <section className="studio-panel"><div className="studio-step">02 / CONTENT</div><h2>Film review queue <span className="count-pill">{titles.length}</span></h2>
      {titles.length===0?<p>No films waiting for review. Creators must complete a private upload and submit distribution rights first.</p>:<div className="review-grid">{titles.map(t=><article className="review-card" key={t.id}>
        <h3>{t.name}</h3><p>Creator: {t.creator_name} · Viewing tier: <strong>{t.access_tier||'FREE'}</strong></p><p>Rights: <strong>{t.rights_status||'NOT PROVIDED'}</strong> · Uploaded videos: <strong>{t.uploaded_files}</strong> · READY: <strong>{t.ready_assets||0}</strong></p>
        <button className="btn btn-outline" onClick={()=>void loadRights(t.id)}><ClipboardCheck size={16}/> Inspect rights</button>
        <button className="btn btn-outline" onClick={()=>void loadMedia(t.id)}>Inspect encoding jobs</button>
        {media[t.id]&&<div className="rights-summary">{media[t.id].length===0?<p>No media uploaded.</p>:media[t.id].map(m=><div className="media-review-item" key={m.id}>
          <p><strong>{m.processing_status}</strong> · {m.job_state||'WAITING'} · {m.attempts??0} / {m.max_attempts??3} attempts
          {m.last_error&&<span> · {m.last_error}</span>}</p>
          {m.processing_status==='READY' && m.duration_seconds !== null && m.duration_seconds>0 && token &&
            <><MediaCueEditor titleId={t.id} assetId={m.id} duration={m.duration_seconds} token={token}/>
            <SceneMarkerEditor titleId={t.id} assetId={m.id} duration={m.duration_seconds} token={token}/></>}
          {m.job_state==='FAILED'&&<button className="btn btn-outline" disabled={loading} onClick={()=>void act(`/api/v1/admin/titles/${t.id}/media/${m.id}/retry`,{},'Media re-queued for processing.')}>Retry transient failure</button>}
        </div>)}</div>}
        {rights[t.id]&&<div className="rights-summary"><p><strong>{rights[t.id].rightsBasis}</strong> · {rights[t.id].validFrom} to {rights[t.id].validUntil}</p><p>Territories: {rights[t.id].territories.join(', ')}</p><p>Evidence: {rights[t.id].proofReference}</p><p>{rights[t.id].agreementNotes}</p>
          <label>Review note<input maxLength={1000} value={note[t.id]||''} onChange={e=>setNote(v=>({...v,[t.id]:e.target.value}))} placeholder="What evidence did you verify?"/></label>
          {rights[t.id].status==='PENDING'&&<div className="review-actions">
            <button disabled={loading} className="btn btn-primary" onClick={()=>void act(`/api/v1/admin/titles/${t.id}/rights-review`,{decision:'APPROVED',note:note[t.id]||''},'Rights approved. Film awaits media validation.')}>Approve rights</button>
            <button disabled={loading} className="btn btn-outline" onClick={()=>void act(`/api/v1/admin/titles/${t.id}/rights-review`,{decision:'REJECTED',note:note[t.id]||''},'Rights rejected.')}>Reject rights</button>
          </div>}
        </div>}
        <div className="studio-separator"/>
        <div className="review-actions"><button disabled={loading} className="btn btn-outline" onClick={()=>void act(`/api/v1/admin/titles/${t.id}/publish`,{},'Title published.')}>Attempt publish</button>
        <button disabled={loading} className="text-btn danger" onClick={()=>{const reason=window.prompt('Reason for rejection (required):');if(reason?.trim())void act(`/api/v1/admin/titles/${t.id}/reject`,{note:reason.trim()},'Film rejected.');}}>Reject title</button></div>
        <p className="studio-file-note">Publication requires approved rights and successfully transcoded READY media. Refresh encoding status before publishing.</p>
      </article>)}</div>}
    </section>
    <section className="studio-panel"><div className="studio-step">03 / PUBLIC DISCOVERY</div><h2>Search indexing approval</h2>
      <p>Default is off. Only approved, published, currently licensed titles with READY media can be indexed; live rights checks still apply. Approval is not a licence.</p>
      <div className="studio-list">{seoTitles.length===0?<p>No published titles available for SEO review.</p>:seoTitles.map(t=><div className="studio-list-item" key={t.id}>
        <span><strong>{t.name}</strong><small>{t.seo_index_approved?'Approved for eligible-market indexing':'Not indexed'}</small></span>
        <button type="button" disabled={loading} className="btn btn-outline" onClick={()=>void act(`/api/v1/admin/titles/${t.id}/seo-approval`,
          {approved:!t.seo_index_approved},t.seo_index_approved?'Search indexing revoked.':'Search indexing approved (subject to current rights).')}>
          {t.seo_index_approved?'Revoke SEO':'Approve SEO'}
        </button>
      </div>)}</div>
    </section>
  </main>;
}
