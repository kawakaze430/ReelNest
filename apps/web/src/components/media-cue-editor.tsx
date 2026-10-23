'use client';
import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';
import type { PlaybackCues } from '@/lib/playback-enhancements';

type Key = keyof PlaybackCues;
const FIELDS: {key:Key;label:string}[] = [
  {key:'introStartSeconds',label:'Intro start'},
  {key:'introEndSeconds',label:'Intro end'},
  {key:'recapStartSeconds',label:'Recap start'},
  {key:'recapEndSeconds',label:'Recap end'},
  {key:'creditsStartSeconds',label:'Credits start'},
];
const empty=():Record<Key,string>=>({
  introStartSeconds:'',introEndSeconds:'',recapStartSeconds:'',recapEndSeconds:'',creditsStartSeconds:''
});

/** Admin-only editor: the server rechecks role, asset ownership and cue bounds. */
export function MediaCueEditor({titleId,assetId,duration,token}:{
  titleId:string;assetId:string;duration:number;token:string
}) {
  const [fields,setFields]=useState<Record<Key,string>>(empty);
  const [status,setStatus]=useState('');
  const [busy,setBusy]=useState(false);
  const endpoint=`/api/v1/admin/titles/${titleId}/media/${assetId}/cues`;
  useEffect(()=>{
    let dead=false;
    void apiFetch<PlaybackCues>(endpoint,token).then(cues=>{
      if(dead)return;
      const next=empty();
      for(const {key} of FIELDS)next[key]=cues[key]===null?'':String(cues[key]);
      setFields(next);
    }).catch(()=>{if(!dead)setStatus('Could not load existing skip markers.');});
    return()=>{dead=true;};
  },[endpoint,token]);
  async function save(){
    const parsed={} as PlaybackCues;
    for(const {key} of FIELDS){
      const value=fields[key].trim();
      if(value!=='' && (!/^\d+$/.test(value) || Number(value)>duration)){
        setStatus('Use whole seconds within the media duration.');return;
      }
      parsed[key]=value===''?null:Number(value);
    }
    const validRange=(start:number|null,end:number|null)=>
      (start===null && end===null) || (start!==null && end!==null && start<end && end<=duration);
    if(!validRange(parsed.introStartSeconds,parsed.introEndSeconds) ||
       !validRange(parsed.recapStartSeconds,parsed.recapEndSeconds) ||
       (parsed.creditsStartSeconds!==null && parsed.creditsStartSeconds>=duration)){
      setStatus('Intro/recap require both start and end, in order. Credits must start before the end.');return;
    }
    setBusy(true);setStatus('');
    try {
      await apiFetch<PlaybackCues>(endpoint,token,{method:'PUT',body:JSON.stringify(parsed)});
      setStatus('Skip markers saved for this encoded asset.');
    }catch(e){setStatus(e instanceof Error?e.message:'Could not save skip markers.');}
    finally{setBusy(false);}
  }
  return <fieldset className="cue-editor" disabled={busy}>
    <legend>Playback skip markers · {duration}s</legend>
    <p>Optional, editor-reviewed seconds. Leave a pair empty to hide that skip button.</p>
    <div className="cue-editor-fields">{FIELDS.map(({key,label})=><label key={key}>{label}
      <input type="number" min="0" max={duration} step="1" inputMode="numeric" value={fields[key]}
        onChange={e=>setFields(old=>({...old,[key]:e.target.value}))}/>
    </label>)}</div>
    <button type="button" className="btn btn-outline" onClick={()=>void save()} disabled={busy}>Save skip markers</button>
    {status&&<p role="status">{status}</p>}
  </fieldset>;
}
