'use client';
import { useEffect, useState } from 'react';
import { apiFetch } from '@/lib/api';

type Scene = {label:string;summary:string;startSeconds:number;endSeconds:number};
const empty:Scene={label:'',summary:'',startSeconds:0,endSeconds:1};
export function SceneMarkerEditor({titleId,assetId,duration,token}:{
  titleId:string;assetId:string;duration:number;token:string;
}) {
  const [rows,setRows]=useState<Scene[]>([]);
  const [busy,setBusy]=useState(false);
  const [status,setStatus]=useState('');
  const url=`/api/v1/admin/titles/${titleId}/media/${assetId}/scenes`;
  useEffect(()=>{
    const ctrl=new AbortController();
    void apiFetch<Scene[]>(url,token,{signal:ctrl.signal}).then(data=>{if(!ctrl.signal.aborted)setRows(data);})
      .catch(()=>{if(!ctrl.signal.aborted)setStatus('Unable to load reviewed scenes.');});
    return()=>ctrl.abort();
  },[url,token]);
  function update(index:number,patch:Partial<Scene>) {
    setRows(before=>before.map((scene,i)=>i===index?{...scene,...patch}:scene));
  }
  async function save() {
    if(rows.some((r,i)=>r.label.trim().length<2||r.label.length>90||r.summary.length>240||
      r.startSeconds<0||r.endSeconds>duration||r.endSeconds<=r.startSeconds||
      (i>0 && r.startSeconds<rows[i-1].endSeconds))){
      setStatus('Use ordered, non-overlapping scene ranges within the duration and short labels.');return;
    }
    setBusy(true);setStatus('');
    try{
      const result=await apiFetch<Scene[]>(url,token,{method:'PUT',body:JSON.stringify({scenes:rows})});
      setRows(result);setStatus('Editor-reviewed scene markers saved.');
    }catch(e){setStatus(e instanceof Error?e.message:'Unable to save scenes.');}
    finally{setBusy(false);}
  }
  return <fieldset className="cue-editor" disabled={busy}>
    <legend>Human-reviewed scene chapters · {duration}s</legend>
    <p>Only READY media. These labels are visible to eligible viewers and never authorize streaming.</p>
    {rows.map((row,index)=><div key={index} className="cue-editor-fields">
      <label>Scene label<input maxLength={90} value={row.label} onChange={e=>update(index,{label:e.target.value})}/></label>
      <label>Short description<input maxLength={240} value={row.summary} onChange={e=>update(index,{summary:e.target.value})}/></label>
      <label>Start second<input type="number" min={0} max={duration} value={row.startSeconds}
        onChange={e=>update(index,{startSeconds:Number(e.target.value)})}/></label>
      <label>End second<input type="number" min={1} max={duration} value={row.endSeconds}
        onChange={e=>update(index,{endSeconds:Number(e.target.value)})}/></label>
      <button type="button" className="btn btn-outline" onClick={()=>setRows(old=>old.filter((_,i)=>i!==index))}>Remove scene</button>
    </div>)}
    <div className="review-actions">
      <button className="btn btn-outline" disabled={rows.length>=40} type="button" onClick={()=>setRows(old=>[...old,{...empty,startSeconds:old.length?old[old.length-1].endSeconds:0,endSeconds:Math.min(duration,(old.length?old[old.length-1].endSeconds:0)+1)}])}>Add scene</button>
      <button className="btn btn-primary" type="button" onClick={()=>void save()}>Save reviewed chapters</button>
    </div>
    {status&&<p role="status">{status}</p>}
  </fieldset>;
}
