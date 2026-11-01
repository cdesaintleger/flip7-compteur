const CACHE='flip7-v4-card-art-1';
const FILES=['./','./index.html','./style.css','./theme.css','./assets/number-cards.webp','./assets/penalty-cards.webp','./app.js','./scoring.mjs','./icon.svg','./icon-180.png','./icon-192.png','./icon-512.png','./icon-maskable.png','./manifest.webmanifest'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(FILES)));self.skipWaiting()});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('flip7-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener('message',e=>{if(e.data?.type==='CHECK_OFFLINE')e.waitUntil(caches.open(CACHE).then(async c=>{const entries=await Promise.all(FILES.map(f=>c.match(f)));if(entries.every(Boolean))e.source?.postMessage({type:'OFFLINE_READY'})}))});
self.addEventListener('fetch',e=>{const u=new URL(e.request.url);if(e.request.method!=='GET'||u.origin!==self.location.origin)return;
 // Version cohérente et démarrage immédiat en mode avion ; les nouvelles versions
 // sont installées atomiquement dans un nouveau cache par le service worker.
 e.respondWith(caches.open(CACHE).then(async c=>{const hit=await c.match(e.request,{ignoreSearch:true});if(hit)return hit;if(e.request.mode==='navigate')return await c.match('./index.html')||fetch(e.request);return fetch(e.request)}));
});
