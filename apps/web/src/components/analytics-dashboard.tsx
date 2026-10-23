'use client';
import {useAuth} from 'react-oidc-context';
import {useEffect,useState} from 'react';
import {BarChart3,ShieldAlert} from 'lucide-react';
import {apiFetch} from '@/lib/api';
type Overview={viewers:number;titles:number;paidSubscribers:number;playbackStarts:number;watchlistAdds:number};
type Popular={titleId:string;name:string;plays:number};
type QoeSummary={consentingProfiles:number;startupSamples:number;stalls:number;
  meanStartupMs:number|null;meanStallMs:number|null;enoughSamples:boolean};
export function AnalyticsDashboard(){
  const auth=useAuth(),token=auth.user?.access_token;
  const [summary,setSummary]=useState<Overview|null>(null);
  const [top,setTop]=useState<Popular[]>([]);
  const [qoe,setQoe]=useState<QoeSummary|null>(null);
  const [error,setError]=useState('');
  useEffect(()=>{if(!token)return;let alive=true;
    void apiFetch<QoeSummary>('/api/v1/admin/qoe/summary',token)
      .then(q=>{if(alive)setQoe(q);}).catch(()=>{});
    Promise.all([apiFetch<Overview>('/api/v1/discovery/admin/overview',token),apiFetch<Popular[]>('/api/v1/discovery/admin/top-titles',token)])
      .then(([s,t])=>{if(alive){setSummary(s);setTop(t)}})
      .catch(()=>{if(alive)setError('Analytics require an ADMIN role and a running API.');});
    return()=>{alive=false};},[token]);
  return <main className="wrap analytics-shell">
    <div className="eyebrow"><BarChart3 size={15}/> INTERNAL REPORTING</div><h1>Platform analytics<span className="title-period">.</span></h1>
    <p>Aggregated system counters. Playback and watchlist events represent submitted signals, not independently audited viewing sessions.</p>
    {!token&&<div className="notice"><ShieldAlert size={18}/> Sign in as an authorized administrator.</div>}
    {error&&<div className="notice" role="alert">{error}</div>}
    {summary&&<div className="analytics-grid">{([
      ['User accounts',summary.viewers],['Published titles',summary.titles],['Paid subscribers',summary.paidSubscribers],
      ['30-day play events',summary.playbackStarts],['30-day watchlist events',summary.watchlistAdds]
    ] as [string,number][]).map(([name,n])=><article className="analytics-card" key={name}><strong>{n.toLocaleString()}</strong><span>{name}</span></article>)}</div>}
    <section className="analytics-table" aria-label="Opt-in playback quality">
      <h2>Playback quality · last 7 days</h2>
      <p>Only opt-in, profile-linked telemetry is counted. Individual viewing sessions are never shown here; fewer than five distinct viewer profiles are suppressed.</p>
      {!qoe?<p>Playback quality metrics are unavailable or the backend has not been updated.</p>:
        !qoe.enoughSamples?<p>Not enough consenting profiles for an aggregate report.</p>:
        <div className="analytics-grid">{([
          ['Opt-in profiles',qoe.consentingProfiles],['Startup samples',qoe.startupSamples],
          ['Stalls',qoe.stalls],['Mean startup (ms)',qoe.meanStartupMs===null?'—':Math.round(qoe.meanStartupMs)],
          ['Mean stall (ms)',qoe.meanStallMs===null?'—':Math.round(qoe.meanStallMs)]
        ] as [string,number|string][]).map(([label,n])=><article className="analytics-card" key={label}>
          <strong>{typeof n==='number'?n.toLocaleString():n}</strong><span>{label}</span></article>)}</div>}
    </section>
    <section className="analytics-table"><h2>Most started films (last 30 days)</h2>{!top.length?<p>No viewing events have been recorded yet.</p>:<ol>{top.map(t=><li key={t.titleId}><span>{t.name}</span><strong>{t.plays} starts</strong></li>)}</ol>}</section>
  </main>;
}
