'use client';
import {useEffect} from 'react';
/** Register only for production HTTPS origins. No private application data is precached. */
export function ServiceWorkerRegistration(){
 useEffect(()=>{if(process.env.NODE_ENV!=='production'||!('serviceWorker' in navigator)||
    !(location.protocol==='https:'||location.hostname==='localhost'))return;
   navigator.serviceWorker.register('/sw.js',{scope:'/'}).catch(()=>{/* Browser can continue without offline shell. */});
 },[]);return null;
}
