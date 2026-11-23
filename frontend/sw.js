// Offline support: cache-first for the app shell, network-first for API GETs.
// Writes made offline are queued and replayed by js/api.js when the browser is back online.
const CACHE = 'classsync-v2';
const SHELL = ['./', 'index.html', 'css/theme.css', 'js/config.js', 'js/api.js', 'js/data.js', 'js/app.js',
  'manifest.webmanifest', 'icons/icon.svg'];
self.addEventListener('install', e => e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting())));
self.addEventListener('activate', e => e.waitUntil(
  caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  const isApi = new URL(e.request.url).pathname.includes('/api/');
  if (isApi) {
    // never cache authenticated API responses across users; just fall back to cache when offline
    e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));
    return;
  }
  e.respondWith(caches.match(e.request).then(hit => hit || fetch(e.request).then(res => {
    const copy = res.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); return res;
  }).catch(() => caches.match('index.html'))));
});
