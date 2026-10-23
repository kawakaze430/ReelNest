'use client';
import buildStatus from '@/data/build21-status.json';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowRight, CircleCheck, CircleDashed, Server, ShieldAlert, Database, Code2 } from 'lucide-react';
import { API_BASE_URL } from '@/lib/api';

const checkpoints = [
  { name:'01–02 — Foundation and creator workflow',status:'Source implemented', detail:'Next.js, Spring Boot, auth, catalog, creator onboarding and rights review',done:true },
  { name:'03–04 — Streaming and viewer experience',status:'Media tested / integration pending',detail:'FFmpeg HLS, sessions, progress, watchlists and profile restrictions',done:true },
  { name:'05–06 — Billing, recommendations, analytics',status:'Code + offline policy tests',detail:'Razorpay adapter, reversible entitlements, rules-based discovery, offline ML lab',done:true },
  { name:'07–08 — Cloud and privacy controls',status:'Staging scaffold only',detail:'CloudFront, AWS Terraform, security headers, privacy review workflow',done:true },
  { name:'09–21 — Release safeguards and feature development',status:'Offline gates available',detail:'Production configuration guard, single-territory CDN allowlist, separate database role, CI release report',done:true },
  { name:'Live end-to-end integration',status:'Not verified',detail:'Requires Docker/Maven/npm and a real PostgreSQL/MinIO/Keycloak runtime',done:false },
  { name:'Provider and AWS deployment',status:'Not verified',detail:'Needs sandbox Razorpay, Terraform validation, HTTPS identity and CDN tests',done:false },
  { name:'Public launch clearance',status:'Blocked',detail:'Content rights, privacy deletion, security review, backups, and operations testing',done:false }
];
export default function StatusPage() {
  const [health,setHealth] = useState<'checking'|'online'|'offline'>('checking');
  useEffect(()=>{
    const controller=new AbortController();
    fetch(`${API_BASE_URL}/actuator/health`,{signal:controller.signal})
      .then(r=>setHealth(r.ok?'online':'offline'))
      .catch(()=>{if(!controller.signal.aborted)setHealth('offline');});
    return ()=>controller.abort();
  },[]);
  return <main className="wrap status-page">
    <div className="eyebrow">DEVELOPMENT / BUILD 21</div>
    <h1>ReelNest release status<span className="title-period">.</span></h1>
    <p className="status-intro">Code exists for the core platform, but integration, vendor testing and legal clearance are not finished. Offline tests are not a production certificate.</p>
    <div className="status-cards">
      <div className="status-card"><Server size={24}/><span>API health</span><strong className={health==='online'?'online':health==='offline'?'offline':''}>{health==='online'?'Connected':health==='offline'?'Not reachable':'Checking...'}</strong></div>
      <div className="status-card"><Database size={24}/><span>Schema</span><strong>V1–V20 defined (unverified)</strong></div>
      <div className="status-card"><Code2 size={24}/><span>Build stage</span><strong>Build 21 source</strong></div>
      <div className="status-card"><ShieldAlert size={24}/><span>Public launch</span><strong className="offline">Not approved</strong></div>
    </div>
    <section className="build20-statistics" aria-label="282 task source status"><h2>Audited implementation progress — {buildStatus.total} tasks</h2>
      <p>Source-readiness indicators only; {buildStatus.acceptanceVerified} tasks have full production acceptance sign-off.</p>
      <div className="build20-status-bar" role="img" aria-label={buildStatus.statuses.map(s=>`${s.name}: ${s.count}`).join('; ')}>
        {buildStatus.statuses.map(s=><div key={s.name} style={{width:`${s.count/buildStatus.total*100}%`,background:s.color}}/>)}</div>
      <div className="build20-status-legend">{buildStatus.statuses.map(s=><div key={s.name}><span className="build20-status-dot" style={{background:s.color}}/><span>{s.name}</span><strong>{s.count}</strong></div>)}</div>
      <p>The two blocked categories belong to the local/Codex/account-validation track. They are not counted as completed coding work.</p>
    </section>
    <div className="timeline">{checkpoints.map((m,i)=><div className="timeline-item" key={m.name}><div className="timeline-rail">{m.done?<CircleCheck size={21}/>:<CircleDashed size={21}/>}</div><div><div className="timeline-header"><h3>{m.name}</h3><span className={m.done?'done-tag':'pending-tag'}>{m.status}</span></div><p>{m.detail}</p></div></div>)}</div>
    <div className="notice" role="status">The application has not yet completed full Docker, Next.js, Spring, payment-provider, or AWS verification. Do not enable production checkout or release copyrighted media without permission.</div>
    <Link className="btn btn-primary" href="/browse">Explore the demo catalog <ArrowRight size={16}/></Link>
  </main>;
}
