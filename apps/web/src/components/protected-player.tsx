'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from 'react-oidc-context';
import {
  ArrowLeft, AlertTriangle, Captions, Clock3, Film, Maximize, Minimize, Pause,
  PictureInPicture2, Play, RotateCcw, ShieldCheck, SkipBack, SkipForward, Volume2, VolumeX
} from 'lucide-react';
import type Hls from 'hls.js';
import { apiFetch, API_BASE_URL, ApiError } from '@/lib/api';
import { ProfilePicker } from '@/components/profile-picker';
import { SceneExplorer } from '@/components/scene-explorer';
import { useActiveProfile } from '@/hooks/use-active-profile';
import { prettyTime, playbackMessage, type PlaybackSession } from '@/lib/playback';
import { boundedElapsedMs, hlsRecoveryAction, skipAction, telemetryErrorCode } from '@/lib/playback-enhancements';
import {parsePreviewVtt,parseSpriteVtt,previewAt,previewUrl,type PreviewCue} from '@/lib/seek-previews';
import {QoeReplayQueue} from '@/lib/qoe-replay';
import {nativeAudioList,nativeAudioOptions,selectNativeAudio,type NativeAudioTrackList} from '@/lib/native-audio';
import {
  DEFAULT_PLAYER_PREFERENCES, cappedQualityIndex, clampSeek, loadPreferences,
  matchingTrack, savePreferences, type PlayerPreferences, type TrackOption
} from '@/lib/player-preferences';

type Quality = { level: number; label: string; height: number };
const LANGUAGE_NAMES: Record<string,string> = { en:'English',hi:'Hindi',es:'Spanish',fr:'French',de:'German',ta:'Tamil',te:'Telugu',eng:'English',hin:'Hindi',spa:'Spanish',fra:'French',tam:'Tamil',tel:'Telugu' };
function trackLabel(language:string, label:string):string {
  const name=LANGUAGE_NAMES[language.toLowerCase()]||language;
  if(/^Audio [1-8]$/.test(label) && name)return `${label} (${name})`;
  return label || name || 'Unknown';
}

export function ProtectedPlayer({titleId}:{titleId:string}) {
  const auth = useAuth();
  const router = useRouter();
  const profile = useActiveProfile();
  const token = auth.user?.access_token;
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls|null>(null);
  const prefsRef = useRef<PlayerPreferences>({...DEFAULT_PLAYER_PREFERENCES});
  const prefsVersionRef = useRef(0);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout>|null>(null);
  const saveChainRef = useRef<Promise<void>>(Promise.resolve());
  const touchTapRef = useRef<{time:number,x:number}|null>(null);
  const [prefs,setPrefs] = useState<PlayerPreferences>({...DEFAULT_PLAYER_PREFERENCES});
  const [session,setSession] = useState<PlaybackSession|null>(null);
  const [ready,setReady] = useState(false);
  const [error,setError] = useState('');
  const [mode,setMode] = useState('Starting...');
  const [position,setPosition] = useState(0);
  const [playing,setPlaying] = useState(false);
  const [muted,setMuted] = useState(false);
  const [volume,setVolume] = useState(1);
  const [fullscreen,setFullscreen] = useState(false);
  const [pip,setPip] = useState(false);
  const [pipAvailable,setPipAvailable] = useState(false);
  const [qualities,setQualities] = useState<Quality[]>([]);
  const [selectedQuality,setSelectedQuality] = useState(-1);
  const [activeQuality,setActiveQuality] = useState('Auto');
  const [audioTracks,setAudioTracks] = useState<TrackOption[]>([]);
  const [subtitleTracks,setSubtitleTracks] = useState<TrackOption[]>([]);
  const [audioTrack,setAudioTrack] = useState(-1);
  const [subtitleTrack,setSubtitleTrack] = useState(-1);
  const [retry,setRetry] = useState(0);
  const [networkRecovery,setNetworkRecovery] = useState('');
  const [previews,setPreviews] = useState<PreviewCue[]>([]);
  const [hoverTime,setHoverTime] = useState<number|null>(null);
  const [previewImage,setPreviewImage] = useState<string|null>(null);
  const previewObjectsRef=useRef<Map<string,string>>(new Map());
  const previewAbortRef=useRef<AbortController|null>(null);
  const qoeQueueRef=useRef<QoeReplayQueue|null>(null);
  const [nextEpisode,setNextEpisode] = useState<{titleId:string;name:string;seasonNumber:number;episodeNumber:number}|null>(null);
  const [ended,setEnded] = useState(false);
  const [autoplayCancelled,setAutoplayCancelled] = useState(false);
  const [countdown,setCountdown] = useState<number|null>(null);

  // Keep device settings as a fallback, but synchronize to the owned server profile.
  // Only the latest profile/unsaved revision may apply a server GET result.
  const changePrefs = useCallback((patch:Partial<PlayerPreferences>) => {
    const updated={...prefsRef.current,...patch};
    prefsVersionRef.current++;
    prefsRef.current=updated;
    setPrefs(updated);
    if(!profile.profileId)return;
    savePreferences(profile.profileId,updated);
    if(saveTimerRef.current)clearTimeout(saveTimerRef.current);
    if(!token)return;
    const profileId=profile.profileId;
    saveTimerRef.current=setTimeout(()=>{
      saveChainRef.current=saveChainRef.current.catch(()=>{}).then(async()=>{
        await apiFetch(`/api/v1/profiles/${profileId}/player-preferences`,token,{
          method:'PUT',body:JSON.stringify(updated)
        });
      }).catch(()=>{ /* Device-local preferences remain usable if profile sync is offline. */ });
    },500);
  },[profile.profileId,token]);

  useEffect(() => {
    const profileId=profile.profileId;
    const version=++prefsVersionRef.current;
    if(saveTimerRef.current)clearTimeout(saveTimerRef.current);
    const next=profileId ? loadPreferences(profileId) : {...DEFAULT_PLAYER_PREFERENCES};
    prefsRef.current=next;
    setPrefs(next);
    if(videoRef.current)videoRef.current.playbackRate=next.speed;
    if(!profileId || !token)return;
    let dead=false;
    void apiFetch<PlayerPreferences>(`/api/v1/profiles/${profileId}/player-preferences`,token)
      .then(remote=>{
        if(dead || version!==prefsVersionRef.current)return;
        // The API returns a strictly validated whitelist; clean again defensively.
        const clean={...DEFAULT_PLAYER_PREFERENCES,...remote};
        prefsRef.current=clean;
        setPrefs(clean);
        savePreferences(profileId,clean);
      }).catch(()=>{});
    return()=>{dead=true;if(saveTimerRef.current)clearTimeout(saveTimerRef.current);};
  },[profile.profileId,token]);

  useEffect(() => {
    if(videoRef.current) videoRef.current.playbackRate=prefs.speed;
  },[prefs.speed]);

  useEffect(() => {
    const hls=hlsRef.current;
    if(!hls)return;
    const levels=hls.levels.map(l=>l.height);
    hls.autoLevelCapping=prefs.dataSaver ? cappedQualityIndex(levels) : -1;
    if(prefs.dataSaver && selectedQuality>=0 && levels[selectedQuality]>480) {
      hls.currentLevel=-1;setSelectedQuality(-1);
    }
  },[prefs.dataSaver,selectedQuality]);

  useEffect(()=>{
    qoeQueueRef.current?.setEnabled(prefs.qoeConsent);
  },[prefs.qoeConsent]);

  useEffect(() => {
    const currentVideo=videoRef.current;
    if (!currentVideo || !token || !profile.profileId || !auth.isAuthenticated) return;
    // The stable non-null alias is safe in asynchronous HLS callbacks after teardown guards.
    const video: HTMLVideoElement = currentVideo;
    let dead=false;
    let tick: ReturnType<typeof setInterval>|undefined;
    let heart: ReturnType<typeof setInterval>|undefined;
    let current: PlaybackSession|undefined;
    let seq=0;
    let lastReported=-1;
    let startupReported=false;
    let waitingSince:number|null=null;
    let recoveryAttempts=0;
    let fatalAuthError=false;
    let recoveryTimer:ReturnType<typeof setTimeout>|undefined;
    let lastTelemetryAt=0;
    const controller=new AbortController();
    const sessionRequestedAt=performance.now();
    const qoeQueue=new QoeReplayQueue(async event=>{
      if(!current || dead || !prefsRef.current.qoeConsent)return;
      await apiFetch<void>(`/api/v1/playback/sessions/${current.sessionId}/qoe`,token,{
        method:'POST',body:JSON.stringify(event)
      });
    });
    qoeQueueRef.current=qoeQueue;
    qoeQueue.setEnabled(prefsRef.current.qoeConsent);
    const emitQoe=(eventType:'STARTUP'|'STALL'|'QUALITY_SWITCH'|'PLAYER_ERROR'|'RECOVERY',
                   elapsedMs?:number|null,qualityHeight?:number,errorCode?:string)=>{
      if(!prefsRef.current.qoeConsent || !current || dead)return;
      const now=performance.now();
      // Bound event frequency even on a badly behaving browser/device.
      if(now-lastTelemetryAt<350 && eventType==='QUALITY_SWITCH')return;
      lastTelemetryAt=now;
      qoeQueue.enqueue({eventId:crypto.randomUUID(),eventType,
        elapsedMs:elapsedMs??null,qualityHeight:qualityHeight??null,errorCode:errorCode??null});
    };
    setSession(null);setReady(false);setError('');setMode('Checking your access...');setNetworkRecovery('');
    setQualities([]);setSelectedQuality(-1);setActiveQuality('Auto');
    setAudioTracks([]);setSubtitleTracks([]);setAudioTrack(-1);setSubtitleTrack(-1);
    setPreviews([]);setHoverTime(null);setPreviewImage(null);setEnded(false);setCountdown(null);setAutoplayCancelled(false);
    previewAbortRef.current?.abort();
    for(const url of previewObjectsRef.current.values())URL.revokeObjectURL(url);
    previewObjectsRef.current.clear();
    hlsRef.current=null;

    async function report(force=false) {
      if (!video || !current || dead || !Number.isFinite(video.currentTime)) return;
      const seconds=Math.max(0,Math.floor(video.currentTime));
      if (!force && (seconds===lastReported || Math.abs(seconds-lastReported)<10)) return;
      lastReported=seconds;
      try {
        await apiFetch(`/api/v1/playback/sessions/${current.sessionId}/progress`, token, {
          method:'PUT',keepalive:force, body:JSON.stringify({positionSeconds:seconds,sequence:++seq})
        });
      } catch (e) {
        if (!dead && e instanceof ApiError && [401,402,403,404].includes(e.status)) {
          video.pause(); setError(playbackMessage(e.status));
        }
      }
    }
    const onPause=()=>void report(true);
    const onEnded=()=>{void report(true);setEnded(true);};
    const onWaiting=()=>{if(waitingSince===null)waitingSince=performance.now();};
    const onPlaying=()=>{
      const now=performance.now();
      if(!startupReported){startupReported=true;emitQoe('STARTUP',boundedElapsedMs(sessionRequestedAt,now));}
      if(waitingSince!==null){emitQoe('STALL',boundedElapsedMs(waitingSince,now));waitingSince=null;}
      setNetworkRecovery('');
    };
    const onOnline=()=>{
      if(!dead && current && !fatalAuthError && hlsRef.current){
        hlsRef.current.startLoad();setNetworkRecovery('Network restored. Resuming stream...');
      }
    };
    video.addEventListener('pause',onPause);
    video.addEventListener('ended',onEnded);
    video.addEventListener('waiting',onWaiting);
    video.addEventListener('playing',onPlaying);
    window.addEventListener('online',onOnline);
    const replayOnline=()=>void qoeQueue.flush();
    window.addEventListener('online',replayOnline);

    const syncNativeTextTracks = () => {
      if(!video || dead)return;
      const tracks:TrackOption[]=Array.from(video.textTracks).map((t,i)=>({id:i,label:trackLabel(t.language,t.label),language:t.language}));
      setSubtitleTracks(tracks);
      const match=matchingTrack(tracks,prefsRef.current.preferredSubtitles);
      if(match>=0) { Array.from(video.textTracks).forEach((t,i)=>{t.mode=i===match?'showing':'disabled';});setSubtitleTrack(match); }
    };
    video.textTracks?.addEventListener?.('addtrack',syncNativeTextTracks);
    const nativeAudio:NativeAudioTrackList|null=nativeAudioList(video as HTMLVideoElement & {audioTracks?:NativeAudioTrackList});
    const syncNativeAudioTracks=()=>{
      if(!nativeAudio || dead || hlsRef.current)return;
      const tracks=nativeAudioOptions(nativeAudio).map(t=>({...t,label:trackLabel(t.language,t.label)}));
      setAudioTracks(tracks);
      const preferred=matchingTrack(tracks,prefsRef.current.preferredAudio);
      if(preferred>=0)selectNativeAudio(nativeAudio,preferred);
      const active=tracks.find(t=>nativeAudio[t.id]?.enabled)?.id??-1;
      setAudioTrack(active);
    };
    nativeAudio?.addEventListener?.('addtrack',syncNativeAudioTracks);
    nativeAudio?.addEventListener?.('removetrack',syncNativeAudioTracks);
    nativeAudio?.addEventListener?.('change',syncNativeAudioTracks);

    async function start() {
      try {
        const created=await apiFetch<PlaybackSession>('/api/v1/playback/sessions',token,{
          method:'POST',body:JSON.stringify({titleId,profileId:profile.profileId}),signal:controller.signal
        });
        if (dead) {
          void apiFetch<void>(`/api/v1/playback/sessions/${created.sessionId}`,token,{method:'DELETE'}).catch(()=>{});
          return;
        }
        current=created;
        setSession(created);
        setPosition(created.resumePositionSeconds);
        // Prefer a protected sprite atlas (one fetch per 100 frames), fall back to
        // individual private thumbnails for older encoded assets.
        const readPreviewIndex=async(filename:'sprites.vtt'|'preview.vtt')=>{
          const url=previewUrl(created.manifestUrl,filename);
          if(!url)return [] as PreviewCue[];
          try{
            const response=await fetch(url.startsWith('https://')?url:API_BASE_URL+url,{
              credentials:'include',headers:url.startsWith('https://')?{}:{Authorization:`Bearer ${token}`},
              signal:controller.signal,cache:'no-store'
            });
            if(!response.ok)return [] as PreviewCue[];
            const body=await response.text();
            return filename==='sprites.vtt'?parseSpriteVtt(body):parsePreviewVtt(body);
          }catch{return [] as PreviewCue[];}
        };
        void (async()=>{
          let cues=await readPreviewIndex('sprites.vtt');
          if(!cues.length)cues=await readPreviewIndex('preview.vtt');
          if(!dead)setPreviews(cues);
        })();
        const isCdn=created.manifestUrl.startsWith('https://');
        const manifest=isCdn?created.manifestUrl:API_BASE_URL+created.manifestUrl;
        video.playbackRate=prefsRef.current.speed;
        if(!isCdn) video.removeAttribute('crossorigin');
        // Safari native HLS can't send bearer headers: use ONLY for signed CDN cookie sessions.
        if (isCdn && video.canPlayType('application/vnd.apple.mpegurl')) {
          video.crossOrigin='use-credentials';
          video.addEventListener('loadedmetadata',()=>{
            if(!dead && created.resumePositionSeconds>0) video.currentTime=created.resumePositionSeconds;
            syncNativeTextTracks();syncNativeAudioTracks();setReady(true);
          },{once:true});
          video.src=manifest;
          setMode('Authorized native HLS');
        } else {
          const {default:Hls}=await import('hls.js');
          if (dead) return;
          if (!Hls.isSupported()) {
            setError('This browser requires protected CDN HLS. Local authenticated proxy playback needs Media Source Extensions.');
            setMode('Unsupported browser');return;
          }
          const player=new Hls({
            enableWorker:true, lowLatencyMode:false,
            maxBufferLength:30,maxMaxBufferLength:60,backBufferLength:30,
            capLevelToPlayerSize:true,startLevel:-1,
            xhrSetup: (xhr: XMLHttpRequest) => {
              if(isCdn) xhr.withCredentials=true;
              else xhr.setRequestHeader('Authorization',`Bearer ${token}`);
            }
          });
          hlsRef.current=player;
          player.on(Hls.Events.MANIFEST_PARSED,(_event,data)=>{
            if(dead)return;
            setMode('Authorized adaptive HLS');setReady(true);
            const list=data.levels.map((l,i)=>({level:i,label:`${l.height}p`,height:l.height}));
            setQualities(list);
            player.autoLevelCapping=prefsRef.current.dataSaver ? cappedQualityIndex(list.map(l=>l.height)) : -1;
            // Seeking before media metadata is available can fail on MSE implementations.
          });
          video.addEventListener('loadedmetadata',()=>{
            if(!dead && created.resumePositionSeconds>0 && Number.isFinite(video.duration))
              video.currentTime=Math.min(created.resumePositionSeconds,Math.max(0,video.duration-1));
          },{once:true});
          player.on(Hls.Events.LEVEL_SWITCHED,(_event,data)=>{
            if(!dead){
              const height=player.levels[data.level]?.height;
              setActiveQuality(height ? `${height}p`:'Auto');
              if(height)emitQoe('QUALITY_SWITCH',null,height);
            }
          });
          player.on(Hls.Events.AUDIO_TRACKS_UPDATED,(_event,data)=>{
            if(dead)return;
            const tracks=data.audioTracks.map((t,i)=>({id:i,label:trackLabel(t.lang || '',t.name || ''),language:t.lang || ''}));
            setAudioTracks(tracks);
            const match=matchingTrack(tracks,prefsRef.current.preferredAudio);
            if(match>=0)player.audioTrack=match;
            setAudioTrack(player.audioTrack);
          });
          player.on(Hls.Events.SUBTITLE_TRACKS_UPDATED,(_event,data)=>{
            if(dead)return;
            const tracks=data.subtitleTracks.map((t,i)=>({id:i,label:trackLabel(t.lang || '',t.name || ''),language:t.lang || ''}));
            setSubtitleTracks(tracks);
            const match=matchingTrack(tracks,prefsRef.current.preferredSubtitles);
            if(match>=0)player.subtitleTrack=match;
            setSubtitleTrack(player.subtitleTrack);
          });
          player.on(Hls.Events.AUDIO_TRACK_SWITCHED,()=>{if(!dead)setAudioTrack(player.audioTrack);});
          player.on(Hls.Events.SUBTITLE_TRACK_SWITCH,()=>{if(!dead)setSubtitleTrack(player.subtitleTrack);});
          player.on(Hls.Events.ERROR,(_event,data)=>{
            if(dead || !data.fatal)return;
            const status=(data.response as {code?:number}|undefined)?.code;
            if([401,402,403,404].includes(status??0))fatalAuthError=true;
            emitQoe('PLAYER_ERROR',null,undefined,telemetryErrorCode(data.details));
            const action=hlsRecoveryAction(data.type,status,recoveryAttempts);
            if(action!=='stop'){
              recoveryAttempts++;
              setNetworkRecovery(`Recovering protected stream (attempt ${recoveryAttempts}/2)...`);
              if(recoveryTimer)clearTimeout(recoveryTimer);
              recoveryTimer=setTimeout(()=>{
                if(dead)return;
                if(action==='network')player.startLoad();
                else player.recoverMediaError();
                emitQoe('RECOVERY');
              },recoveryAttempts*1200);
              return;
            }
            video.pause();
            setNetworkRecovery('');
            setError(status ? playbackMessage(status) : `Streaming failure (${data.details}). Retry the authorized playback session.`);
          });
          player.loadSource(manifest);
          player.attachMedia(video);
        }
        tick=setInterval(()=>void report(),12000);
        heart=setInterval(()=>{
          if (!current || dead) return;
          void apiFetch(`/api/v1/playback/sessions/${current.sessionId}/heartbeat`,token,{method:'POST'})
            .catch(e=>{if(!dead){video.pause();setError(e instanceof ApiError?playbackMessage(e.status):'Playback session expired. Retry to authorize again.');}});
        },Math.max(10,created.heartbeatIntervalSeconds)*1000);
      } catch(e) {
        if (dead) return;
        setMode('Unavailable');
        setError(e instanceof ApiError ? playbackMessage(e.status) : 'Unable to connect to the playback service. Retry after checking your network.');
      }
    }
    void start();
    return ()=>{
      // Attempt a final authenticated progress write before teardown. Never send a bearer token
      // through sendBeacon (which cannot set Authorization headers).
      void report(true);
      dead=true;controller.abort();
      qoeQueue.dispose();
      if(qoeQueueRef.current===qoeQueue)qoeQueueRef.current=null;
      previewAbortRef.current?.abort();
      for(const url of previewObjectsRef.current.values())URL.revokeObjectURL(url);
      previewObjectsRef.current.clear();
      if(tick)clearInterval(tick);if(heart)clearInterval(heart);if(recoveryTimer)clearTimeout(recoveryTimer);
      video.removeEventListener('pause',onPause);video.removeEventListener('ended',onEnded);
      video.removeEventListener('waiting',onWaiting);video.removeEventListener('playing',onPlaying);
      window.removeEventListener('online',onOnline);
      window.removeEventListener('online',replayOnline);
      video.textTracks?.removeEventListener?.('addtrack',syncNativeTextTracks);
      nativeAudio?.removeEventListener?.('addtrack',syncNativeAudioTracks);
      nativeAudio?.removeEventListener?.('removetrack',syncNativeAudioTracks);
      nativeAudio?.removeEventListener?.('change',syncNativeAudioTracks);
      hlsRef.current?.destroy();hlsRef.current=null;
      video.pause();video.removeAttribute('src');video.removeAttribute('crossorigin');video.load();
      if(current) {
        void apiFetch<void>(`/api/v1/playback/sessions/${current.sessionId}`,token,{method:'DELETE'}).catch(()=>{});
      }
    };
  },[token,profile.profileId,titleId,auth.isAuthenticated,retry]);

  // Resolve next episode on the server: it checks profile ownership, country rights,
  // age policy, processed media and subscription before returning any navigation target.
  useEffect(()=>{
    setNextEpisode(null);
    if(!token || !profile.profileId || !auth.isAuthenticated)return;
    let dead=false;
    void apiFetch<{titleId:string;name:string;seasonNumber:number;episodeNumber:number}|undefined>(
      `/api/v1/playback/next-episode?profileId=${profile.profileId}&titleId=${titleId}`,token)
      .then(next=>{if(!dead)setNextEpisode(next??null);}).catch(()=>{});
    return()=>{dead=true;};
  },[token,profile.profileId,titleId,auth.isAuthenticated]);

  useEffect(()=>{
    if(!ended || !nextEpisode || !prefs.autoplayNext || autoplayCancelled){setCountdown(null);return;}
    setCountdown(10);
    const timer=setInterval(()=>setCountdown(v=>v===null?null:Math.max(0,v-1)),1000);
    return()=>clearInterval(timer);
  },[ended,nextEpisode,prefs.autoplayNext,autoplayCancelled]);
  useEffect(()=>{
    if(countdown===0 && nextEpisode && ended && !autoplayCancelled && prefs.autoplayNext){
      setCountdown(null);
      router.push(`/watch/${encodeURIComponent(nextEpisode.titleId)}`);
    }
  },[countdown,nextEpisode,ended,autoplayCancelled,prefs.autoplayNext,router]);

  // Load only the hovered private JPEG through a bearer-authenticated fetch (or CDN cookies).
  // Never embed a bearer token in a URL, and bound object URLs to twelve cached images.
  useEffect(()=>{
    // Do not reveal future frames while scrubbing: only already-watched content
    // (plus a small seek tolerance) receives a visual preview.
    const cue=hoverTime!==null && hoverTime<=position+5 ? previewAt(previews,hoverTime) : null;
    const manifest=session?.manifestUrl;
    const imagePath=manifest&&cue?previewUrl(manifest,cue.filename):null;
    if(!imagePath || !token){setPreviewImage(null);return;}
    const cached=previewObjectsRef.current.get(imagePath);
    if(cached){setPreviewImage(cached);return;}
    setPreviewImage(null); // Never display a stale frame from a different atlas.
    const controller=new AbortController();
    previewAbortRef.current?.abort();previewAbortRef.current=controller;
    const timer=setTimeout(()=>{
      void fetch(imagePath.startsWith('https://')?imagePath:API_BASE_URL+imagePath,{
        credentials:'include',signal:controller.signal,cache:'no-store',
        headers:imagePath.startsWith('https://')?{}:{Authorization:`Bearer ${token}`}
      }).then(r=>r.ok&&r.headers.get('content-type')?.startsWith('image/jpeg')?r.blob():null)
        .then(blob=>{
          if(!blob || controller.signal.aborted || blob.size>1000000)return;
          const url=URL.createObjectURL(blob);
          previewObjectsRef.current.set(imagePath,url);
          if(previewObjectsRef.current.size>12){
            const first=previewObjectsRef.current.keys().next().value;
            if(first){URL.revokeObjectURL(previewObjectsRef.current.get(first)!);previewObjectsRef.current.delete(first);}
          }
          setPreviewImage(url);
        }).catch(()=>{});
    },120);
    return()=>{clearTimeout(timer);controller.abort();};
  },[previews,hoverTime,position,session?.manifestUrl,token]);

  function chooseQuality(level:number) {
    const player=hlsRef.current;
    if(!player)return;
    const index=level>=0&&level<player.levels.length?level:-1;
    if(index>=0 && prefs.dataSaver && player.levels[index].height>480) return;
    player.currentLevel=index;
    setSelectedQuality(index);
  }
  function chooseAudio(id:number) {
    if(!audioTracks.some(t=>t.id===id))return;
    const player=hlsRef.current;
    if(player)player.audioTrack=id;
    else {
      const video=videoRef.current;
      const list=video&&nativeAudioList(video as HTMLVideoElement & {audioTracks?:NativeAudioTrackList});
      if(!list || !selectNativeAudio(list,id))return;
    }
    setAudioTrack(id);
    changePrefs({preferredAudio:audioTracks.find(t=>t.id===id)?.language||''});
  }
  function chooseSubtitle(id:number) {
    const video=videoRef.current;
    if(id!==-1 && !subtitleTracks.some(t=>t.id===id))return;
    if(hlsRef.current){hlsRef.current.subtitleTrack=id;hlsRef.current.subtitleDisplay=id>=0;}
    else if(video) Array.from(video.textTracks).forEach((t,i)=>{t.mode=i===id?'showing':'disabled';});
    setSubtitleTrack(id);
    changePrefs({preferredSubtitles:subtitleTracks.find(t=>t.id===id)?.language||''});
  }
  function seek(diff:number) {
    const video=videoRef.current;
    if(video && Number.isFinite(video.currentTime)) video.currentTime=clampSeek(video.currentTime,video.duration,diff);
  }
  async function togglePlay() {
    const video=videoRef.current;
    if(!video || !ready)return;
    try {if(video.paused) await video.play();else video.pause();}
    catch {setError('Your browser blocked playback. Select Play after interacting with the player.');}
  }
  async function toggleFullscreen() {
    const screen=videoRef.current?.parentElement;
    if(!screen)return;
    try {if(document.fullscreenElement)await document.exitFullscreen();else await screen.requestFullscreen();}
    catch {setError('Fullscreen is unavailable on this browser or device.');}
  }
  async function togglePiP() {
    const video=videoRef.current;
    if(!video || !document.pictureInPictureEnabled)return;
    try {if(document.pictureInPictureElement)await document.exitPictureInPicture();else await video.requestPictureInPicture();}
    catch {setError('Picture-in-picture is unavailable for this video or device.');}
  }
  function handleTouchEnd(e:import('react').TouchEvent<HTMLVideoElement>) {
    // Double-tap the left/right side to jump 10s. The middle preserves native play controls.
    if(e.changedTouches.length!==1)return;
    const v=videoRef.current;
    if(!v || !session || !ready)return;
    const x=e.changedTouches[0].clientX;
    const now=Date.now();
    const previous=touchTapRef.current;
    touchTapRef.current={time:now,x};
    if(previous && now-previous.time<350 && Math.abs(x-previous.x)<55){
      const bounds=v.getBoundingClientRect();
      if(x<bounds.left+bounds.width/3){e.preventDefault();seek(-10);}
      else if(x>bounds.left+bounds.width*2/3){e.preventDefault();seek(10);}
      touchTapRef.current=null;
    }
  }
  function toggleMute() {
    const v=videoRef.current;
    if(v)v.muted=!v.muted;
  }
  const nativeHls=mode==='Authorized native HLS';
  const activeCue=skipAction(session?.cues,position,session?.durationSeconds??0);
  useEffect(()=>{
    const onFull=()=>setFullscreen(Boolean(document.fullscreenElement));
    // React's video element props do not type PiP events. Attach native listeners.
    const video=videoRef.current;
    const onEnterPiP=()=>setPip(true);
    const onLeavePiP=()=>setPip(false);
    document.addEventListener('fullscreenchange',onFull);
    video?.addEventListener('enterpictureinpicture',onEnterPiP);
    video?.addEventListener('leavepictureinpicture',onLeavePiP);
    setPipAvailable(Boolean(video && document.pictureInPictureEnabled && !video.disablePictureInPicture));
    setPip(Boolean(video && document.pictureInPictureElement===video));
    return ()=>{
      document.removeEventListener('fullscreenchange',onFull);
      video?.removeEventListener('enterpictureinpicture',onEnterPiP);
      video?.removeEventListener('leavepictureinpicture',onLeavePiP);
    };
  },[auth.isAuthenticated,profile.profileId]);

  if (auth.isLoading || profile.loading) return <main className="wrap viewer-shell"><p>Loading viewer identity...</p></main>;
  if (!auth.isAuthenticated) return <main className="wrap viewer-shell"><h1>Sign in to watch</h1><p>Video playback requires an authenticated viewer account.</p><button className="btn btn-primary" onClick={()=>void auth.signinRedirect()}>Sign in</button></main>;
  if (!profile.profileId) return <main className="wrap viewer-shell"><h1>Select a viewer profile</h1><ProfilePicker/></main>;
  return <main className="wrap viewer-shell">
    <div className="viewer-toolbar"><Link href={`/title/${titleId}`} className="back-link"><ArrowLeft size={17}/> Title details</Link><ProfilePicker compact/></div>
    <div className="viewer-heading"><div className="eyebrow"><ShieldCheck size={14}/> PRIVATE STREAMING</div><h1>Now watching<span className="title-period">.</span></h1></div>
    <div className="viewer-screen" onKeyDown={e=>{
      // Don't override native keyboard interactions of toolbar buttons/selects.
      if(e.target!==videoRef.current)return;
      switch(e.key.toLowerCase()) {
        case 'arrowright':e.preventDefault();seek(10);break;
        case 'arrowleft':e.preventDefault();seek(-10);break;
        case ' ':case 'k':e.preventDefault();void togglePlay();break;
        case 'm':e.preventDefault();toggleMute();break;
        case 'f':e.preventDefault();void toggleFullscreen();break;
        case 'c':e.preventDefault();chooseSubtitle(subtitleTrack===-1 && subtitleTracks.length?subtitleTracks[0].id:-1);break;
        case 'j':e.preventDefault();seek(-10);break;
        case 'l':e.preventDefault();seek(10);break;
      }
    }}>
      <video ref={videoRef} controls playsInline preload="metadata" className={`viewer-video caption-${prefs.captionSize} caption-${prefs.captionContrast}`}
        onTimeUpdate={e=>setPosition(e.currentTarget.currentTime)}
        onTouchEnd={handleTouchEnd}
        onPlay={()=>setPlaying(true)} onPause={()=>setPlaying(false)}
        onVolumeChange={e=>{setMuted(e.currentTarget.muted);setVolume(e.currentTarget.volume);}}
        onError={()=>setError('The browser could not decode the stream. Retry the playback session or select another supported device.')}
        tabIndex={0} aria-label="Private OTT video player. Space play/pause; arrows seek; M mute; F fullscreen; C captions." />
    </div>
    {session&&<div className="viewer-seek-area">
      <label htmlFor="viewer-seek">Seek position: {prettyTime(position)} of {prettyTime(session.durationSeconds)}</label>
      <input id="viewer-seek" type="range" min={0} max={Math.max(1,session.durationSeconds)} step={1}
        value={Math.min(position,session.durationSeconds)} disabled={!ready}
        onChange={e=>{const v=videoRef.current;if(v&&ready)v.currentTime=Number(e.target.value);}}
        onPointerMove={e=>{const rect=e.currentTarget.getBoundingClientRect();if(rect.width>0)
          setHoverTime(Math.max(0,Math.min(session.durationSeconds,Math.round((e.clientX-rect.left)/rect.width*session.durationSeconds))));}}
        onPointerLeave={()=>{setHoverTime(null);setPreviewImage(null);}}
        onBlur={()=>{setHoverTime(null);setPreviewImage(null);}}
        aria-label="Seek within video"/>
      {hoverTime!==null&&<div className="viewer-seek-preview" role="status">
        {previewImage&&hoverTime<=position+5&&(previewAt(previews,hoverTime)?.crop ?
          <span role="img" aria-label="Video frame preview" style={{display:'block',width:160,height:90,
            backgroundImage:`url(${previewImage})`,backgroundSize:'1600px 900px',
            backgroundPosition:`-${(previewAt(previews,hoverTime)?.crop?.x??0)/2}px -${(previewAt(previews,hoverTime)?.crop?.y??0)/2}px`}}/> :
          <img src={previewImage} alt="Video frame preview" width={160} height={90}/>)}
        <span>{prettyTime(hoverTime)}{hoverTime>position+5?' · Preview hidden to avoid spoilers':''}</span>
      </div>}
    </div>}
    {ended&&nextEpisode&&<section className="viewer-up-next" aria-label="Next episode" aria-live="polite">
      <div><strong>Up next: S{nextEpisode.seasonNumber} E{nextEpisode.episodeNumber} — {nextEpisode.name}</strong>
        <p>{countdown!==null?`Playing next episode in ${countdown} seconds`:'Continue the series when you are ready.'}</p></div>
      <div className="viewer-up-next-actions">
        <button type="button" className="btn btn-primary" onClick={()=>router.push(`/watch/${encodeURIComponent(nextEpisode.titleId)}`)}>Play next</button>
        {countdown!==null&&<button type="button" className="btn btn-outline" onClick={()=>{setAutoplayCancelled(true);setCountdown(null);}}>Cancel autoplay</button>}
      </div>
    </section>}
    <div className="viewer-transport" role="group" aria-label="Video playback controls">
      <button type="button" disabled={!session || !ready} onClick={()=>void togglePlay()} aria-label={playing?'Pause video':'Play video'}>{playing?<Pause size={17}/>:<Play size={17}/>}<span>{playing?'Pause':'Play'}</span></button>
      <button type="button" disabled={!session || !ready} onClick={()=>seek(-10)} aria-label="Rewind ten seconds"><SkipBack size={17}/> <span>10s</span></button>
      <button type="button" disabled={!session || !ready} onClick={()=>seek(10)} aria-label="Forward ten seconds"><SkipForward size={17}/> <span>10s</span></button>
      {activeCue&&<button type="button" disabled={!ready} className="viewer-skip-cue"
        onClick={()=>{const v=videoRef.current;if(v)v.currentTime=activeCue.targetSeconds;}}
        aria-label={activeCue.label}><SkipForward size={17}/><span>{activeCue.label}</span></button>}
      <button type="button" disabled={!session || !ready} onClick={toggleMute} aria-label={muted?'Unmute':'Mute'}>{muted?<VolumeX size={17}/>:<Volume2 size={17}/>}<span>{muted?'Unmute':'Mute'}</span></button>
      <label className="viewer-volume">Volume <input type="range" aria-label="Volume" min={0} max={1} step={0.05} value={volume} onChange={e=>{const v=videoRef.current;if(v){v.volume=Number(e.target.value);v.muted=false;}}}/></label>
      <button type="button" disabled={!session || !ready} onClick={()=>void toggleFullscreen()} aria-label={fullscreen?'Exit fullscreen':'Enter fullscreen'}>{fullscreen?<Minimize size={17}/>:<Maximize size={17}/>}<span>Fullscreen</span></button>
      {pipAvailable&&<button type="button" disabled={!session || !ready} onClick={()=>void togglePiP()} aria-label={pip?'Exit picture in picture':'Enter picture in picture'}><PictureInPicture2 size={17}/><span>{pip?'Exit PiP':'PiP'}</span></button>}
    </div>
    <div className="viewer-meta">
      <div className="viewer-mode" aria-live="polite"><ShieldCheck size={18}/><span>{mode}</span>{session&&<span className="viewer-position"><Clock3 size={16}/> {prettyTime(position)} / {prettyTime(session.durationSeconds)}</span>}</div>
      <span className="viewer-quality-readout">Current video: {activeQuality}</span>
    </div>
    <div className="viewer-settings" aria-label="Video playback preferences">
      <label>Quality <select disabled={!qualities.length} value={selectedQuality} onChange={e=>chooseQuality(Number(e.target.value))}><option value={-1}>Auto</option>{qualities.filter(q=>!prefs.dataSaver || q.height<=480).map(q=><option key={q.level} value={q.level}>{q.label}</option>)}</select></label>
      <label>Speed <select value={prefs.speed} onChange={e=>changePrefs({speed:Number(e.target.value)})}>{[0.5,0.75,1,1.25,1.5,2].map(x=><option key={x} value={x}>{x}×</option>)}</select></label>
      <label>Audio <select disabled={!audioTracks.length} value={audioTrack} onChange={e=>chooseAudio(Number(e.target.value))}><option value={-1}>Default</option>{audioTracks.map(t=><option key={t.id} value={t.id}>{t.label}</option>)}</select></label>
      <label><Captions size={16}/> Subtitles <select disabled={!subtitleTracks.length} value={subtitleTrack} onChange={e=>chooseSubtitle(Number(e.target.value))}><option value={-1}>Off</option>{subtitleTracks.map(t=><option key={t.id} value={t.id}>{t.label}</option>)}</select></label>
      <label>Caption size <select value={prefs.captionSize} onChange={e=>changePrefs({captionSize:e.target.value as PlayerPreferences['captionSize']})}><option value="small">Small</option><option value="normal">Normal</option><option value="large">Large</option></select></label>
      <label>Caption contrast <select value={prefs.captionContrast} onChange={e=>changePrefs({captionContrast:e.target.value as PlayerPreferences['captionContrast']})}><option value="standard">Standard</option><option value="high">High contrast</option></select></label>
      <label className="viewer-saver"><input type="checkbox" checked={prefs.autoplayNext}
        onChange={e=>changePrefs({autoplayNext:e.target.checked})}/> Autoplay next episode (off by default)</label>
      <label className="viewer-saver"><input type="checkbox" checked={prefs.dataSaver} onChange={e=>changePrefs({dataSaver:e.target.checked})}/> Data saver (prefer 480p or lower)</label>
      <label className="viewer-consent"><input type="checkbox" checked={prefs.qoeConsent} onChange={e=>changePrefs({qoeConsent:e.target.checked})}/>
        Share optional playback quality metrics linked to this viewer profile (off by default)</label>
    </div>
    {session&&token&&profile.profileId&&<SceneExplorer
      token={token} titleId={titleId} profileId={profile.profileId}
      duration={session.durationSeconds} canSeek={ready}
      onSeek={seconds=>{const v=videoRef.current;if(v&&ready&&Number.isFinite(v.duration))
        v.currentTime=Math.max(0,Math.min(seconds,Math.max(0,v.duration-1)));}}/>}
    {networkRecovery&&<p className="viewer-support-note" role="status" aria-live="polite">{networkRecovery}</p>}
    {nativeHls&&<p className="viewer-support-note">Safari native HLS manages adaptive quality internally. Manual quality control is unavailable here. Audio language selection uses native track controls only when Safari exposes them; captions are available when the stream provides them. Data saver does not cap native playback.</p>}
    {(!audioTracks.length || !subtitleTracks.length)&&<p className="viewer-support-note">Audio and subtitle choices appear only when the licensed HLS package includes alternate tracks. Preferences are stored on this device and synchronized to your signed-in profile when the API is available.</p>}
    {error&&<div className="notice studio-error" role="alert"><AlertTriangle size={18}/><div>{error}<p>Check your account access, streaming rights and network. The Retry button requests a fresh authorized session; it does not bypass entitlements.</p></div><button className="btn btn-outline" onClick={()=>setRetry(v=>v+1)}><RotateCcw size={16}/> Retry</button></div>}
    <div className="viewer-hint"><Film size={17}/> Only approved, licensed and READY titles can stream. Premium playback additionally requires a verified subscription. The private API-to-MinIO proxy is for local testing only.</div>
  </main>;
}
