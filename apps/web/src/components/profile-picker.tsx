'use client';

import Link from 'next/link';
import { useActiveProfile } from '@/hooks/use-active-profile';

export function ProfilePicker({compact=false}:{compact?:boolean}) {
  const p = useActiveProfile();
  if (!p.auth.isAuthenticated) return <Link className="btn btn-outline" href="/login">Sign in to watch</Link>;
  if (p.loading) return <span className="profile-picker-loading">Loading your profiles...</span>;
  if (!p.profiles.length) return <Link className="btn btn-outline" href="/profiles">Create a viewer profile</Link>;
  return <label className="viewer-profile-picker">{!compact&&<span>Watching as</span>}
    <select aria-label="Active viewer profile" value={p.profileId} onChange={e=>p.select(e.target.value)}>
      {p.profiles.map(profile=><option value={profile.id} key={profile.id}>{profile.name}{profile.child ? ' (Kids)' : ''}</option>)}
    </select>
  </label>;
}
