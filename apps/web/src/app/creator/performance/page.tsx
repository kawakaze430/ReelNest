'use client';
import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import {apiFetch} from '@/lib/api';
type Row={titleId:string;name:string;eligibleProfiles:number;starts:number;completions:number;suppressionApplied:boolean};
type Statement={note:string;days:number;titles:Row[]};
export default function CreatorPerformance(){
  const auth=useAuth(),token=auth.user?.access_token;
  const [days,setDays]=useState(30),[data,setData]=useState<Statement|null>(null),[error,setError]=useState('');
  useEffect(()=>{if(!token)return;let ignore=false;apiFetch<Statement>(`/api/v1/creator/performance?days=${days}`,token)
      .then(d=>{if(!ignore){setData(d);setError('')}}).catch(()=>{if(!ignore)setError('Creator approval is required or the service is unavailable.')});
      return()=>{ignore=true};},[token,days]);
  return <main className="wrap viewer-shell" style={{maxWidth:1000}}><div className="eyebrow">CREATOR / PERFORMANCE</div>
    <h1>Viewing insights<span className="title-period">.</span></h1><p>Privacy-thresholded engagement. This is not a royalty or earnings statement, and does not determine payments.</p>
    {!token?<button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in</button>:<>
      <label>Report period <select value={days} onChange={e=>setDays(Number(e.target.value))}><option value={7}>7 days</option><option value={30}>30 days</option><option value={90}>90 days</option></select></label>
      {error&&<p className="notice" role="alert">{error}</p>}{data&&<><p role="status">{data.note}</p>
        <table style={{width:'100%',textAlign:'left'}}><thead><tr><th>Film</th><th>Eligible viewers</th><th>Starts</th><th>Completions</th></tr></thead><tbody>
          {data.titles.map(row=><tr key={row.titleId}><td>{row.name}</td><td>{row.suppressionApplied?'Suppressed':row.eligibleProfiles}</td>
          <td>{row.suppressionApplied?'—':row.starts}</td><td>{row.suppressionApplied?'—':row.completions}</td></tr>)}
        </tbody></table></>}
    </>}
  </main>
}
