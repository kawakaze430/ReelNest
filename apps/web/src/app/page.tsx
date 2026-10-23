import type { Metadata } from 'next';
import Link from 'next/link';
import { ArrowRight, Sparkles, Play, ShieldCheck, Clapperboard, Tv2, CheckCircle2 } from 'lucide-react';
import { Poster } from '@/components/poster';
import { demoTitles } from '@/lib/demo-data';

export const metadata: Metadata = { title: 'ReelNest — Stories worth finding', description: 'ReelNest streaming platform concept and original sample films.', alternates: { canonical: '/' } };
export default function Home() {
  const featured = demoTitles[0];
  return <main>
    <section className="landing-hero"><div className="landing-glow"/><div className="landing-grid"/><div className="wrap landing-content">
      <div className="landing-text"><div className="eyebrow hero-eyebrow"><span className="live-dot"/> WELCOME TO A DIFFERENT KIND OF SCREEN</div><h1>Stories that<br/><em>stay with you.</em></h1><p>An independent streaming destination built around discovery, creators, and the movies you never knew you&apos;d love.</p><div className="hero-buttons"><Link href="/browse" className="btn btn-primary"><Play size={17} fill="currentColor"/> Explore the collection</Link><Link href="/status" className="btn btn-outline">View build progress <ArrowRight size={17}/></Link></div><div className="landing-stats"><div><strong>8</strong><span>Fictional sample titles</span></div><div><strong>2</strong><span>Languages in prototype</span></div><div><strong>06</strong><span>Development milestone</span></div></div></div>
      <div className="landing-feature"><div className="floating-label"><Sparkles size={14}/> EDITOR&apos;S PICK / CONCEPT ART</div><div className={`feature-art poster-${featured.artTone}`}><div className="feature-orb"/><div className="feature-line"/><div className="feature-caption"><span>REELNEST PRESENTS</span><strong>NEON<br/>HORIZON</strong><small>A world that never stays still.</small></div></div><div className="feature-glass"><div className="glass-pulse"/><span>Curated storytelling<br/><strong>Inspired by possibility</strong></span><ArrowRight size={17}/></div></div>
    </div></section>
    <section className="wrap content-block"><div className="section-title"><div><div className="eyebrow">BEYOND THE ORDINARY</div><h2>Discover a world of stories<span className="title-period">.</span></h2><p>Sample film concepts created specifically for this prototype.</p></div><Link className="text-link" href="/browse">See all titles <ArrowRight size={18}/></Link></div><div className="catalog-grid preview-grid">{demoTitles.slice(0,4).map((t,i)=><Poster key={t.id} title={t} index={i}/>)}</div></section>
    <section className="wrap pillars"><div className="pillar"><div className="pillar-icon"><Clapperboard size={23}/></div><h3>Built for creators</h3><p>A future home for filmmakers to publish legally licensed originals and reach their audience.</p></div><div className="pillar"><div className="pillar-icon"><Tv2 size={23}/></div><h3>Crafted for viewing</h3><p>Adaptive streaming, beautiful browsing and seamless continue-watching are in the roadmap.</p></div><div className="pillar"><div className="pillar-icon"><ShieldCheck size={23}/></div><h3>Rights-first design</h3><p>Content permissions, privacy and security belong in the architecture from day one.</p></div></section>
    <section className="wrap build-banner"><div><div className="eyebrow"><CheckCircle2 size={14}/> BUILDS 01–06</div><h2>Watch the platform take shape.</h2><p>Six development builds: protected HLS playback, creator uploads, subscriptions, personalized discovery and admin metrics. Real provider payments require merchant test credentials and verification.</p></div><Link href="/lab" className="btn btn-primary">Try HLS playback lab <Play size={17}/></Link></section>
  </main>;
}
