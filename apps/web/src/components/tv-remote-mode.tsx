'use client';
import {useEffect,useState} from 'react';
import {nearestDirectional, type Direction} from '@/lib/tv-focus';

/** Opt-in keyboard remote, does not capture arrow keys until activated. */
export function TvRemoteMode(){
  const [active,setActive]=useState(false);
  useEffect(()=>{
    if(!active)return;
    function onKey(event:KeyboardEvent){
      const directions:Record<string,Direction>={ArrowUp:'up',ArrowDown:'down',ArrowLeft:'left',ArrowRight:'right'};
      if(event.key==='Escape'){setActive(false);return;}
      const dir=directions[event.key];if(!dir)return;
      const target=event.target as HTMLElement|null;
      if(target && (target.closest('input,textarea,select,[contenteditable="true"]')))return;
      const zone=document.querySelector('[data-tv-zone]');if(!zone)return;
      const nodes=[...zone.querySelectorAll<HTMLElement>('a[href],button:not([disabled]),[tabindex="0"]')]
        .filter(n=>n.getClientRects().length>0 && !n.getAttribute('aria-hidden'));
      if(nodes.length===0)return;
      const selected=document.activeElement instanceof HTMLElement?document.activeElement:null;
      const current=selected && nodes.includes(selected)?selected:nodes[0];
      const idx=nearestDirectional(current.getBoundingClientRect(),nodes.map(n=>n.getBoundingClientRect()),dir);
      if(idx!==-1){event.preventDefault();nodes[idx].focus({preventScroll:true});nodes[idx].scrollIntoView({block:'nearest',inline:'nearest'});}
    }
    window.addEventListener('keydown',onKey);
    return()=>window.removeEventListener('keydown',onKey);
  },[active]);
  return <button type="button" className="btn btn-outline" aria-pressed={active} onClick={()=>setActive(v=>!v)}>
    {active?'Exit remote navigation (Esc)':'Enable TV remote navigation'}
  </button>;
}
