'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Clapperboard, Info, PlayCircle, ShieldAlert, Settings2 } from 'lucide-react';

const manifest = '/sample/hls/index.m3u8';

export default function PlaybackLab() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [mode, setMode] = useState('Initializing HLS player...');
  const [error, setError] = useState('');

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    let disposed = false;
    let destroy: (() => void) | undefined;
    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = manifest;
      setMode('Native HLS playback');
    } else {
      import('hls.js').then(({default: Hls}) => {
        if (disposed) return;
        if (!Hls.isSupported()) { setMode('HLS unsupported'); setError('This browser cannot play HLS through MSE. Try a newer browser.'); return; }
        const player = new Hls({ enableWorker: true, lowLatencyMode: false });
        player.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) setError(`HLS playback error: ${data.details}`);
        });
        player.loadSource(manifest);
        player.attachMedia(video);
        destroy = () => player.destroy();
        setMode('hls.js over Media Source Extensions');
      }).catch(() => { if(!disposed)setError('Could not load the local HLS player dependency.'); });
    }
    return () => { disposed = true; destroy?.(); video.pause(); video.removeAttribute('src'); video.load(); };
  }, []);

  return <main className="wrap lab-page">
    <Link href="/browse" className="back-link"><ArrowLeft size={17}/> Return to catalog</Link>
    <div className="eyebrow lab-eyebrow"><Clapperboard size={14}/> MEDIA ENGINEERING / LAB 01</div>
    <h1>See the streaming pipeline<span className="title-period">.</span></h1>
    <p className="status-intro">This 8-second clip was generated specifically for ReelNest using FFmpeg. It demonstrates a real HLS playlist and segmented browser playback, but does <strong>not</strong> yet include adaptive bitrate, subscriber authorization, or DRM.</p>
    <div className="lab-player-wrap">
      <video ref={videoRef} className="lab-video" controls preload="metadata" playsInline aria-label="ReelNest original HLS test video" />
    </div>
    <div className="lab-stats"><div><PlayCircle size={20}/><span>Stream</span><strong>HLS / MPEG-TS</strong></div><div><Settings2 size={20}/><span>Playback mode</span><strong>{mode}</strong></div><div><Info size={20}/><span>Length</span><strong>8 seconds</strong></div></div>
    {error && <div className="notice" role="alert"><ShieldAlert size={19}/>{error}</div>}
    <div className="notice"><ShieldAlert size={18}/> Engineering demo only: public static files are deliberately accessible without an account. Do not use this approach for licensed premium media.</div>
    <p className="lab-subtitle">Files: <code>/sample/demo.mp4</code>, <code>/sample/hls/index.m3u8</code>, and four HLS segments. Regenerate them with <code>scripts/generate_demo_media.sh</code>.</p>
  </main>;
}
