'use client';
import {useEffect,useState} from 'react';
import Link from 'next/link';
import {useActiveProfile} from '@/hooks/use-active-profile';
import {apiFetch} from '@/lib/api';

type Badge={code:string;label:string;earned:boolean;distinctTitlesRequired:number};
type Summary={enabled:boolean;completedDistinctTitles:number;scope:string;badges:Badge[]};
export function AchievementsView() {
  const viewer=useActiveProfile();
  const [data,setData]=useState<Summary|null>(null);
  const [error,setError]=useState('');
  useEffect(()=>{
    const token=viewer.token,profileId=viewer.profileId;
    if(!token||!profileId){setData(null);return;}
    const abort=new AbortController();
    setData(null);setError('');
    void apiFetch<Summary>(`/api/v1/profiles/${encodeURIComponent(profileId)}/achievements`,token,
      {signal:abort.signal}).then(result=>{if(!abort.signal.aborted)setData(result);})
      .catch(()=>{if(!abort.signal.aborted)setError('Could not load your milestones.');});
    return()=>abort.abort();
  },[viewer.token,viewer.profileId]);
  return <main className="wrap studio-shell">
    <div className="eyebrow">REELNEST / VIEWING MILESTONES</div>
    <h1>Your stories</h1>
    <p>Private, non-redeemable acknowledgements based on completed films in the past 90 days.
      No spending, cash, competitive rankings or daily streaks.</p>
    {!viewer.token&&<p><Link href="/profiles">Sign in and choose your profile</Link> to view milestones.</p>}
    {viewer.token&&!viewer.profileId&&<p>Select a profile to continue.</p>}
    {error&&<p role="alert">{error}</p>}
    {data&&!data.enabled&&<p role="status">Milestones are disabled for this profile.
      Adult viewers can opt in via <Link href="/profiles">profile personalization settings</Link>.</p>}
    {data?.enabled&&<>
      <p role="status">{data.completedDistinctTitles} different completed titles recorded in the past 90 days.</p>
      <div className="review-grid">{data.badges.map(b=><article className="review-card" key={b.code}>
        <h2>{b.label}</h2><p>{b.earned?'Earned':'Not yet earned'} · {b.distinctTitlesRequired}
          {' '}{b.distinctTitlesRequired===1?'title':'titles'}</p>
      </article>)}</div>
    </>}
  </main>;
}
