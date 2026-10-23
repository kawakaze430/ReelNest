/* ReelNest offline shell. Private/account/API/media and auth responses are NEVER cached. */
const VERSION = 'reelnest-shell-v23';
const STATIC = ['/offline.html','/reelnest-icon.svg','/manifest.webmanifest'];
self.addEventListener('install', event => {
 event.waitUntil(caches.open(VERSION).then(cache => cache.addAll(STATIC)));
 self.skipWaiting();
});
self.addEventListener('activate', event => {
 event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== VERSION && k.startsWith('reelnest-shell-')).map(k => caches.delete(k)))));
 self.clients.claim();
});
self.addEventListener('fetch', event => {
 const request = event.request;
 if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
 const url = new URL(request.url);
 // No routes carrying an account, session, media, protected content or possible personal page state.
 if (/^\/(api|auth|login|account|admin|creator|watch|watch-party|my-list|continue-watching|profiles|title|_next\/data|sample)(\/|$)/.test(url.pathname)) return;
 if (request.mode === 'navigate') {
   // Network only for HTML; offline shell is static and has no account data.
   event.respondWith(fetch(request).catch(() => caches.match('/offline.html')));
   return;
 }
 if (STATIC.includes(url.pathname)) {
   event.respondWith(caches.match(url.pathname).then(cached => cached || fetch(request)));
 }
});
