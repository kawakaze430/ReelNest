'use client';

import { useAuth } from 'react-oidc-context';
import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowUpCircle, CheckCircle2, Clapperboard, FileCheck2, LoaderCircle, Plus, ShieldCheck, UploadCloud, UserRound, XCircle } from 'lucide-react';
import { apiFetch } from '@/lib/api';
import {inspectSrt,type CaptionQa} from '@/lib/caption-qa';
import type { Creator, Draft, Rights, UploadIntent, MediaJob } from '@/types/creator';

const freshDraft = {name:'',synopsis:'',type:'MOVIE',genre:'Drama',language:'en',ageRating:'U',runtimeMinutes:'',releaseYear:'',accessTier:'FREE'};
const draftTypes = ['MOVIE','SERIES'];
const ratingChoices = ['U','UA7','UA13','UA16','A'];

export function CreatorStudio() {
  const auth = useAuth();
  const token = auth.user?.access_token;
  const [creator,setCreator] = useState<Creator|null>(null);
  const [lookup,setLookup] = useState(true);
  const [drafts,setDrafts] = useState<Draft[]>([]);
  const [selected,setSelected] = useState<string>('');
  const [rights,setRights] = useState<Rights|null>(null);
  const [uploads,setUploads] = useState<UploadIntent[]>([]);
  const [media,setMedia] = useState<MediaJob[]>([]);
  const [captions,setCaptions] = useState<{language:string;updatedAt:string}[]>([]);
  const [captionLanguage,setCaptionLanguage] = useState('en');
  const [captionFile,setCaptionFile] = useState<File|null>(null);
  const [captionQa,setCaptionQa] = useState<CaptionQa|null>(null);
  const [busy,setBusy] = useState(false);
  const [error,setError] = useState('');
  const [success,setSuccess] = useState('');
  const [percent,setPercent] = useState(0);
  const [file,setFile] = useState<File|null>(null);
  const [appForm,setAppForm] = useState({displayName:'',legalName:'',contactEmail:'',portfolioUrl:'',statement:''});
  const [form,setForm] = useState(freshDraft);
  const [rightsForm,setRightsForm] = useState({rightsBasis:'OWNER',proofReference:'',agreementNotes:'',validFrom:'',validUntil:'',territories:'IN'});

  const refreshCreator = useCallback(async()=>{
    if (!token) {setLookup(false);return;}
    try {setCreator(await apiFetch<Creator>('/api/v1/creators/me',token));}
    catch {setCreator(null);}
    finally {setLookup(false);}
  },[token]);
  const refreshDrafts = useCallback(async()=>{
    if (!token) return;
    try {setDrafts(await apiFetch<Draft[]>('/api/v1/creator/titles',token));}
    catch {setError('Could not retrieve titles from the backend.');}
  },[token]);
  const refreshSelected = useCallback(async(id:string)=>{
    if (!token || !id) return;
    const base = `/api/v1/creator/titles/${id}`;
    const [right,files,assets,tracks] = await Promise.all([
      apiFetch<Rights>(`${base}/rights`,token).catch(()=>null),
      apiFetch<UploadIntent[]>(`${base}/uploads`,token),
      apiFetch<MediaJob[]>(`${base}/media`,token),
      apiFetch<{language:string;updatedAt:string}[]>(`${base}/captions`,token)
    ]);
    setRights(right);setUploads(files);setMedia(assets);setCaptions(tracks);
    if (right) setRightsForm({rightsBasis:right.rightsBasis,proofReference:right.proofReference,
      agreementNotes:right.agreementNotes,validFrom:right.validFrom,validUntil:right.validUntil,
      territories:right.territories.join(',')});
  },[token]);

  useEffect(()=>{void refreshCreator();},[refreshCreator]);
  useEffect(()=>{if(creator?.status==='APPROVED') void refreshDrafts();},[creator?.status,refreshDrafts]);
  useEffect(()=>{if(selected) void refreshSelected(selected).catch(()=>setError('Failed to read title details.'));},[selected,refreshSelected]);

  // Poll only while an owned title has active processing work. No background
  // polling for completed jobs or when the creator navigates away.
  const hasActiveMedia=media.some(m=>['QUEUED','RUNNING','RETRY_WAIT'].includes(m.job_state??''));
  useEffect(()=>{
    if(!selected || !token || !hasActiveMedia)return;
    const timer=setInterval(()=>{void refreshSelected(selected).catch(()=>{});},10000);
    return()=>clearInterval(timer);
  },[selected,token,hasActiveMedia,refreshSelected]);

  useEffect(()=>{
    setCaptionQa(null);
    if(!captionFile)return;
    if(captionFile.size>200000){setCaptionQa({valid:false,cues:0,lastEndSeconds:0,errors:['SRT must be smaller than 200 KB.'],warnings:[]});return;}
    let cancelled=false;
    void captionFile.text().then(text=>{
      if(!cancelled)setCaptionQa(inspectSrt(text));
    }).catch(()=>{if(!cancelled)setCaptionQa({valid:false,cues:0,lastEndSeconds:0,errors:['Unable to read SRT file.'],warnings:[]});});
    return()=>{cancelled=true;};
  },[captionFile]);

  const selectedDraft=drafts.find(d=>d.id===selected);
  async function run(action:()=>Promise<void>) {
    setBusy(true);setError('');setSuccess('');
    try {await action();} catch(e){setError(e instanceof Error?e.message:'Request failed. Check local API and permissions.');}
    finally {setBusy(false);}
  }
  async function apply(e:React.FormEvent) {
    e.preventDefault();if(!token)return;
    await run(async()=>{setCreator(await apiFetch<Creator>('/api/v1/creators/applications',token,
      {method:'POST',body:JSON.stringify(appForm)}));setSuccess('Application submitted for administrator review.');});
  }
  async function createDraft(e:React.FormEvent) {
    e.preventDefault();if(!token)return;
    await run(async()=>{
      const created=await apiFetch<Draft>('/api/v1/creator/titles',token,{method:'POST',
        body:JSON.stringify({...form,runtimeMinutes:form.runtimeMinutes?Number(form.runtimeMinutes):null,
          releaseYear:form.releaseYear?Number(form.releaseYear):null})});
      setDrafts(old=>[created,...old]);setSelected(created.id);setForm(freshDraft);
      setSuccess('Draft created. Next: submit rights and upload your film.');
    });
  }
  async function saveRights(e:React.FormEvent) {
    e.preventDefault();if(!token||!selected)return;
    await run(async()=>{
      const countries=Array.from(new Set(rightsForm.territories.split(/[,\s]+/).map(s=>s.trim().toUpperCase()).filter(Boolean)));
      const result=await apiFetch<Rights>(`/api/v1/creator/titles/${selected}/rights`,token,
        {method:'PUT',body:JSON.stringify({...rightsForm,territories:countries})});
      setRights(result);setSuccess('Rights declaration saved. An admin must verify the evidence.');
    });
  }
  async function uploadCaptions(){
    if(!token||!selected||!captionFile)return;
    await run(async()=>{
      if(captionFile.size>200000 || !captionFile.name.toLowerCase().endsWith('.srt'))
        throw new Error('Upload a valid .srt file under 200 KB.');
      if(!/^[a-z]{2,3}(-[a-z]{2,8})?$/.test(captionLanguage))
        throw new Error('Use a lowercase language code such as en, hi, or en-us.');
      const srt=await captionFile.text();
      const report=inspectSrt(srt);
      if(!report.valid)throw new Error(report.errors[0]??'Invalid subtitle file.');
      await apiFetch(`/api/v1/creator/titles/${selected}/captions/${captionLanguage}`,token,{
        method:'PUT',body:JSON.stringify({srt})
      });
      setCaptionFile(null);
      await refreshSelected(selected);
      setSuccess('Caption source saved. Upload the video next so the worker can package this track.');
    });
  }
  async function removeCaptions(language:string){
    if(!token||!selected)return;
    await run(async()=>{
      await apiFetch<void>(`/api/v1/creator/titles/${selected}/captions/${language}`,token,{method:'DELETE'});
      await refreshSelected(selected);
      setSuccess(`Removed ${language} captions from this draft.`);
    });
  }
  async function uploadVideo() {
    if(!token||!selected||!file)return;
    await run(async()=>{
      const base=`/api/v1/creator/titles/${selected}/uploads`;
      const mime=file.type || ({'.mp4':'video/mp4','.mov':'video/quicktime','.mkv':'video/x-matroska'}[file.name.substring(file.name.lastIndexOf('.')).toLowerCase()]||'');
      const intent=await apiFetch<UploadIntent>(base,token,{method:'POST',
        body:JSON.stringify({filename:file.name,contentType:mime,sizeBytes:file.size})});
      const parts:{partNumber:number;eTag:string}[]=[];
      setPercent(0);
      try {
        for(let n=1;n<=intent.partCount;n++) {
          const signed=await apiFetch<{partNumber:number;url:string;expiresInSeconds:number}>(
            `${base}/${intent.id}/parts/${n}`,token);
          const start=(n-1)*intent.partSizeBytes;
          const chunk=file.slice(start,Math.min(file.size,start+intent.partSizeBytes));
          // S3 presigned URLs require direct binary PUT. Never attach the API bearer token.
          const sent=await fetch(signed.url,{method:'PUT',body:chunk});
          const etag=sent.headers.get('ETag');
          if(!sent.ok||!etag) throw new Error(`Part ${n} upload failed or ETag is not exposed by storage CORS.`);
          parts.push({partNumber:n,eTag:etag});
          setPercent(Math.round(n/intent.partCount*100));
        }
        await apiFetch<UploadIntent>(`${base}/${intent.id}/complete`,token,
          {method:'POST',body:JSON.stringify({parts})});
        setFile(null);setSuccess('Source uploaded. HLS processing has been queued; check the processing status below.');
        await refreshSelected(selected);
      } catch(err) {
        // Abort incomplete uploads. A retry starts a fresh multipart upload.
        await apiFetch<void>(`${base}/${intent.id}`,token,{method:'DELETE'}).catch(()=>{});
        throw err;
      }
    });
  }
  async function reopen() {
    if(!token||!selected)return;
    await run(async()=>{
      const updated=await apiFetch<Draft>(`/api/v1/creator/titles/${selected}/reopen`,token,{method:'POST'});
      setDrafts(old=>old.map(d=>d.id===updated.id?updated:d));
      setSuccess('Draft reopened. Correct the rights documents before resubmitting.');
    });
  }
  async function submit() {
    if(!token||!selected)return;
    await run(async()=>{
      const updated=await apiFetch<Draft>(`/api/v1/creator/titles/${selected}/submit`,token,{method:'POST'});
      setDrafts(old=>old.map(d=>d.id===updated.id?updated:d));
      setSuccess('Sent to administrator review. Publishing requires inspection, transcoding and approval.');
    });
  }

  if(auth.isLoading||lookup) return <main className="wrap studio-shell"><div className="empty"><LoaderCircle className="spin"/><p>Checking creator access...</p></div></main>;
  if(!auth.isAuthenticated) return <main className="wrap studio-shell"><div className="auth-gate"><div className="big-icon"><Clapperboard/></div><h1>Your films. A new audience.</h1><p>Sign in to apply as a creator, manage film drafts, document distribution rights and upload authorized videos.</p><button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in to Creator Studio</button></div></main>;

  return <main className="wrap studio-shell">
    <div className="eyebrow"><span className="live-dot"/> REELNEST / BUILD 03</div>
    <div className="studio-head"><div><h1>Creator Studio<span className="title-period">.</span></h1><p>Upload original work, manage film rights, and prepare content for review.</p></div><Link className="btn btn-outline" href="/browse">View catalog</Link></div>
    <div className="notice"><ShieldCheck size={17}/>Uploads are private and not published automatically. Only upload films you own or have written distribution permission to stream.</div>
    {error&&<div className="studio-alert studio-error" role="alert"><XCircle size={18}/>{error}</div>}
    {success&&<div className="studio-alert studio-success" role="status"><CheckCircle2 size={18}/>{success}</div>}
    {!creator&&<section className="studio-panel"><div className="studio-step">01 / CREATOR APPLICATION</div><h2>Apply to publish on ReelNest</h2><p>Administrator approval is required before you can create titles or upload videos.</p>
      <form className="studio-form" onSubmit={e=>void apply(e)}>
        <label>Display name<input required maxLength={100} value={appForm.displayName} onChange={e=>setAppForm({...appForm,displayName:e.target.value})} placeholder="Independent Film Studio"/></label>
        <label>Legal name<input required maxLength={160} value={appForm.legalName} onChange={e=>setAppForm({...appForm,legalName:e.target.value})} placeholder="Your full legal name"/></label>
        <label>Contact email<input required type="email" value={appForm.contactEmail} onChange={e=>setAppForm({...appForm,contactEmail:e.target.value})} placeholder="creator@example.com"/></label>
        <label>Portfolio link (optional)<input type="url" value={appForm.portfolioUrl} onChange={e=>setAppForm({...appForm,portfolioUrl:e.target.value})} placeholder="https://..."/></label>
        <label className="span2">Describe your work and rights to distribute it<textarea minLength={20} required rows={4} maxLength={2000} value={appForm.statement} onChange={e=>setAppForm({...appForm,statement:e.target.value})}/></label>
        <button className="btn btn-primary" disabled={busy} type="submit">{busy?'Submitting...':'Submit application'} <ArrowUpCircle size={16}/></button>
      </form>
    </section>}
    {creator&&creator.status!=='APPROVED'&&<section className="studio-panel"><div className="studio-step">CREATOR APPLICATION</div><h2>{creator.displayName}</h2><div className="studio-chip">{creator.status}</div><p>Only approved creators can upload titles. Admins review applications using the Review Console.</p>{creator.reviewNote&&<p><strong>Review note:</strong> {creator.reviewNote}</p>}</section>}
    {creator?.status==='APPROVED'&&<div className="studio-columns">
      <div className="studio-col"><section className="studio-panel"><div className="studio-step">01 / CREATE CONTENT</div><h2>New film draft</h2><form className="studio-form" onSubmit={e=>void createDraft(e)}>
        <label className="span2">Film title<input required value={form.name} maxLength={255} onChange={e=>setForm({...form,name:e.target.value})} placeholder="A story worth sharing"/></label>
        <label className="span2">Synopsis<textarea minLength={1} required rows={3} maxLength={3000} value={form.synopsis} onChange={e=>setForm({...form,synopsis:e.target.value})}/></label>
        <label>Content type<select value={form.type} onChange={e=>setForm({...form,type:e.target.value})}>{draftTypes.map(t=><option key={t}>{t}</option>)}</select></label>
        <label>Genre<input value={form.genre} maxLength={80} onChange={e=>setForm({...form,genre:e.target.value})}/></label>
        <label>Language<select value={form.language} onChange={e=>setForm({...form,language:e.target.value})}><option value="en">English</option><option value="hi">Hindi</option><option value="pa">Punjabi</option><option value="ta">Tamil</option><option value="te">Telugu</option></select></label>
        <label>Age rating<select value={form.ageRating} onChange={e=>setForm({...form,ageRating:e.target.value})}>{ratingChoices.map(r=><option key={r}>{r}</option>)}</select></label>
        <label>Duration (minutes)<input type="number" min="1" max="2000" value={form.runtimeMinutes} onChange={e=>setForm({...form,runtimeMinutes:e.target.value})}/></label>
        <label>Release year<input type="number" min="1888" max="2100" value={form.releaseYear} onChange={e=>setForm({...form,releaseYear:e.target.value})}/></label>
        <label>Viewer access<select value={form.accessTier} onChange={e=>setForm({...form,accessTier:e.target.value})}><option value="FREE">Free to watch</option><option value="PREMIUM">Paid subscription required</option></select></label>
        <button className="btn btn-primary" disabled={busy} type="submit"><Plus size={17}/> Create draft</button>
      </form></section>
      <section className="studio-panel"><div className="studio-step">02 / YOUR TITLES</div><h2>Film library</h2>{drafts.length===0?<p>No drafts yet. Create your first title above.</p>:<div className="studio-list">{drafts.map(d=><button key={d.id} className={`studio-list-item ${selected===d.id?'selected':''}`} onClick={()=>{setSelected(d.id);setRights(null);setUploads([]);setMedia([]);setFile(null);setPercent(0);setError('');setSuccess('');}}><span><strong>{d.name}</strong><small>{d.genre} · {d.language.toUpperCase()} · {d.accessTier}</small></span><em>{d.status}</em></button>)}</div>}</section></div>
      <div className="studio-col"><section className="studio-panel"><div className="studio-step">03 / RIGHTS & MEDIA</div>{!selectedDraft?<><h2>Select a film draft</h2><p>Choose a title to submit its rights and original media for review.</p><div className="studio-empty-icon"><FileCheck2 size={55}/></div></>:<><h2>{selectedDraft.name}</h2><p>Current state: <strong>{selectedDraft.status}</strong></p>
        {selectedDraft.status==='DRAFT'?<>
          <form className="studio-form" onSubmit={e=>void saveRights(e)}><h3 className="span2">Content rights declaration</h3>
            <label>Rights basis<select value={rightsForm.rightsBasis} onChange={e=>setRightsForm({...rightsForm,rightsBasis:e.target.value})}><option value="OWNER">I own the film</option><option value="LICENSED_DISTRIBUTOR">Licensed distributor</option></select></label>
            <label>Territories (ISO codes)<input value={rightsForm.territories} onChange={e=>setRightsForm({...rightsForm,territories:e.target.value})} placeholder="IN,GB,US"/></label>
            <label>Valid from<input required type="date" value={rightsForm.validFrom} onChange={e=>setRightsForm({...rightsForm,validFrom:e.target.value})}/></label>
            <label>Valid until<input required type="date" value={rightsForm.validUntil} onChange={e=>setRightsForm({...rightsForm,validUntil:e.target.value})}/></label>
            <label className="span2">Rights evidence reference<input required value={rightsForm.proofReference} maxLength={1000} onChange={e=>setRightsForm({...rightsForm,proofReference:e.target.value})} placeholder="Contract number or secure document reference"/></label>
            <label className="span2">Agreement notes<textarea required rows={3} minLength={20} maxLength={2000} value={rightsForm.agreementNotes} onChange={e=>setRightsForm({...rightsForm,agreementNotes:e.target.value})} placeholder="Describe the streaming permissions and their source."/></label>
            <button type="submit" disabled={busy} className="btn btn-primary"><FileCheck2 size={16}/> Save rights</button>
          </form>
          <div className="studio-separator"/>
          <h3>Optional subtitle tracks</h3>
          <p>Upload SRT subtitles before the video source. Up to eight language tracks; the media worker converts and validates them against the film duration. Files stay private.</p>
          <div className="studio-form">
            <label>Subtitle language (ISO tag)<input value={captionLanguage} maxLength={16} onChange={e=>setCaptionLanguage(e.target.value.toLowerCase())} placeholder="en"/></label>
            <label>SRT subtitle file<input type="file" accept=".srt,text/plain" onChange={e=>setCaptionFile(e.target.files?.[0]??null)}/></label>
            <button type="button" className="btn btn-outline" disabled={busy||!captionFile||!captionQa?.valid||media.some(m=>!['FAILED','REJECTED'].includes(m.processing_status))}
              onClick={()=>void uploadCaptions()}>Save subtitle track</button>
            {captionQa&&<div className="span2" role="status" aria-live="polite">
              <p><strong>{captionQa.valid?'SRT preflight passed':'SRT needs correction'}</strong> · {captionQa.cues} cues · last cue at {captionQa.lastEndSeconds.toFixed(1)}s. Final validation uses the encoded film duration.</p>
              {captionQa.errors.map((issue,i)=><p key={`error-${i}`} className="studio-file-note">Error: {issue}</p>)}
              {captionQa.warnings.map((issue,i)=><p key={`warning-${i}`} className="studio-file-note">Review: {issue}</p>)}
            </div>}
          </div>
          {captions.length>0&&<div className="studio-list">{captions.map(c=><div className="studio-list-item" key={c.language}>
            <span><strong>{c.language}</strong><small>Private SRT source</small></span>
            <button type="button" className="btn btn-outline" disabled={busy||media.some(m=>!['FAILED','REJECTED'].includes(m.processing_status))}
              onClick={()=>void removeCaptions(c.language)}>Remove</button>
          </div>)}</div>}
          <div className="studio-separator"/>
          <h3>Upload original video</h3><p>MP4, MOV or MKV · up to 5 GiB · 16 MiB multipart uploads. The application server never handles your video bytes.</p>
          <input className="studio-file" type="file" accept=".mp4,.mov,.mkv,video/mp4,video/quicktime,video/x-matroska" onChange={e=>{setFile(e.target.files?.[0]??null);setPercent(0);}}/>
          {file&&<p className="studio-file-note">{file.name} · {(file.size/1024/1024).toFixed(1)} MiB</p>}
          {busy&&percent>0&&<div className="studio-meter"><div style={{width:`${percent}%`}}/></div>}
          <button type="button" onClick={()=>void uploadVideo()} className="btn btn-outline" disabled={busy||!file}><UploadCloud size={17}/>{busy&&percent>0?`Uploading ${percent}%`:'Upload securely'}</button>
          <div className="studio-separator"/>
          <h3>Submit for review</h3>
          <p>Rights declaration: <b>{rights?.status||'Not submitted'}</b> · Uploaded films: <b>{uploads.filter(u=>u.status==='COMPLETED').length}</b></p>
          <button className="btn btn-primary" disabled={busy||!rights||!media.some(m=>!['FAILED','REJECTED'].includes(m.processing_status))} onClick={()=>void submit()}>Submit film for review <ArrowUpCircle size={16}/></button>
        </>:<div className="studio-alert studio-success"><CheckCircle2 size={18}/>Editing is locked while this title is {selectedDraft.status.toLowerCase()}. The media worker processes the uploaded film; administrators can publish only after rights approval and READY media.{selectedDraft.status==='REJECTED'&&<button className="btn btn-outline" onClick={()=>void reopen()} disabled={busy}>Reopen rejected draft</button>}</div>}
        <div className="studio-separator"/>
        <div className="studio-head"><h3>Media processing</h3><button className="btn btn-outline" disabled={busy} onClick={()=>void refreshSelected(selected).catch(()=>setError('Could not refresh media status.'))}>Refresh status</button></div>
        <p>Processing is automatic after uploading. READY means HLS manifests and segments have been validated in private storage.</p>
        <div className="studio-list">{media.length===0?<p>No processing jobs yet.</p>:media.map(m=><div className="studio-list-item" key={m.id}>
          <span><strong>{m.processing_status}</strong><small>Attempts: {m.attempts??0} / {m.max_attempts??3} {m.duration_seconds?`· ${m.duration_seconds}s`:''}</small>
          {m.progress_stage&&<small className="studio-file-note">{m.progress_stage} · {m.progress_percent??0}% (pipeline milestone)</small>}
          {m.job_state==='RUNNING'&&<progress aria-label="Media processing progress" max={100} value={m.progress_percent??0} style={{width:'100%'}}/>}
          {m.last_error&&<small className="studio-file-note">Worker: {m.last_error}</small>}</span><em>{m.job_state||'WAITING'}</em>
        </div>)}</div>
        <h3>Recent uploads</h3><div className="studio-list">{uploads.length===0?<p>No uploads recorded.</p>:uploads.map(u=><div className="studio-list-item" key={u.id}><span><strong>{u.filename}</strong><small>{(u.expectedBytes/1024/1024).toFixed(1)} MiB</small></span><em>{u.status}</em></div>)}</div>
      </>}</section></div>
    </div>}
  </main>;
}
