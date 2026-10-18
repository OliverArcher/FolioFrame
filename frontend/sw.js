const CACHE='folioframe-v11';
const ASSETS=['./','./index.html','./manifest.webmanifest','./icon.svg','./assets/icons/anime.svg','./assets/icons/book.svg','./assets/icons/home.svg','./assets/icons/search.svg','./assets/icons/settings.svg','./assets/icons/movie.png','./assets/icons/stats.png','./assets/icons/tv.png','./assets/fonts/sarasa-gothic-sc-regular.ttf','./assets/fonts/sarasa-gothic-sc-bold.ttf','./assets/vendor/capacitor.js','./assets/vendor/jszip.min.js'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)).then(()=>self.skipWaiting()));});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()));});
// Network-first: the served HTML must always be the newest file, otherwise a stale cache
// keeps serving an old UI after an update. The cache only kicks in when offline.
self.addEventListener('fetch',e=>{
  const u=e.request.url;
  if(u.includes('fonts.googleapis.com')||u.includes('fonts.gstatic.com')||u.includes('api.bgm.tv')||u.includes('themoviedb.org')||u.includes('googleapis.com'))return;
  if(e.request.method!=='GET')return;
  e.respondWith(
    fetch(e.request).then(res=>{
      if(res.ok){const cc=res.clone();caches.open(CACHE).then(c=>c.put(e.request,cc));}
      return res;
    }).catch(()=>caches.match(e.request).then(r=>r||(e.request.mode==='navigate'?caches.match('./index.html'):Response.error()))) );
});
