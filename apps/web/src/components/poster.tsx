import Link from 'next/link';
import { Play, ArrowUpRight } from 'lucide-react';
import type { Title } from '@/types/catalog';

export function Poster({ title, index = 0 }: { title: Title; index?: number }) {
  return <Link className={`poster poster-${title.artTone}`} href={`/title/${title.id}`} aria-label={`Details for ${title.name}`}>
    <div className="poster-art" aria-hidden="true"><span className="art-orbit orbit-one"/><span className="art-orbit orbit-two"/><span className="art-glow"/><span className="poster-number">{String(index + 1).padStart(2,'0')}</span></div>
    <div className="poster-overlay"><div className="poster-subtitle">REELNEST ORIGINAL CONCEPT</div><div className="poster-title">{title.name}</div><div className="poster-meta">{title.genre} <span>·</span> {title.releaseYear} <span>·</span> {title.language.toUpperCase()}</div><div className="poster-bottom"><span className="poster-btn"><Play size={14} fill="currentColor"/> Details</span><ArrowUpRight size={18}/></div></div>
  </Link>;
}
