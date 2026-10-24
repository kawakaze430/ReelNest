'use client';
import {useSyncExternalStore} from 'react';

let now=0;
let timer:ReturnType<typeof setInterval>|undefined;
const listeners=new Set<()=>void>();
function subscribe(notify:()=>void){
  listeners.add(notify);
  now=Date.now();
  if(!timer)timer=setInterval(()=>{now=Date.now();for(const listener of listeners)listener();},1000);
  return()=>{listeners.delete(notify);if(!listeners.size&&timer){clearInterval(timer);timer=undefined;}};
}
function snapshot(){return now;}
function serverSnapshot(){return 0;}
export function useBrowserClock(){return useSyncExternalStore(subscribe,snapshot,serverSnapshot);}
