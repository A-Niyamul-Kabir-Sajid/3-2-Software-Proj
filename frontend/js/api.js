// The ONLY place that talks to the backend.
// Adapter: turns fetch + JSON + ApiError bodies into a small promise API (Api.get/post/...).
// Proxy: sits in front of the network, attaches the JWT, handles 401 and queues writes made while offline.
(function () {
  const BASE = (window.APP_CONFIG && window.APP_CONFIG.API_BASE) || 'http://localhost:8080/api/v1';
  const KEYS = { token: 'classsync.token', user: 'classsync.user', queue: 'classsync.queue' };

  // localStorage can be unavailable (private mode, blocked storage): never let that break the app.
  const store = {
    get(k) { try { return JSON.parse(localStorage.getItem(k)); } catch { return null; } },
    set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* ignore */ } },
    del(k) { try { localStorage.removeItem(k); } catch { /* ignore */ } }
  };

  class ApiError extends Error {
    constructor(status, body) {
      super((body && body.message) || `Request failed (${status})`);
      this.status = status;
      this.body = body || {};
      this.fieldErrors = (body && body.fieldErrors) || [];
    }
  }

  const emit = (name, detail) => window.dispatchEvent(new CustomEvent(name, { detail }));

  function clearSession() { store.del(KEYS.token); store.del(KEYS.user); }

  async function send(method, path, body, opts = {}) {
    const headers = {};
    const token = store.get(KEYS.token);
    if (token && !opts.anonymous) headers.Authorization = 'Bearer ' + token;
    let payload;
    if (body instanceof FormData) payload = body;
    else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }

    let res;
    try {
      res = await fetch(BASE + path, { method, headers, body: payload });
    } catch {
      const canQueue = method !== 'GET' && opts.queueable !== false && !(body instanceof FormData);
      if (canQueue) { enqueue({ method, path, body }); return { queued: true }; }
      throw new ApiError(0, { message: 'You are offline or the server is unreachable' });
    }

    if (res.status === 401 && token && !opts.anonymous) { clearSession(); emit('api:unauthorized'); }
    if (res.status === 204) return null;
    const isJson = (res.headers.get('content-type') || '').includes('application/json');
    const data = isJson ? await res.json() : await res.text();
    if (!res.ok) throw new ApiError(res.status, isJson ? data : { message: data });
    return data;
  }

  // ---- offline write queue (replayed in order when the browser comes back online) ----
  function enqueue(item) {
    const q = store.get(KEYS.queue) || [];
    q.push({ ...item, at: Date.now() });
    store.set(KEYS.queue, q);
    emit('api:queued', { size: q.length });
  }

  let flushing = false;
  async function flushQueue() {
    if (flushing || !navigator.onLine) return;
    flushing = true;
    try {
      let q = store.get(KEYS.queue) || [];
      while (q.length) {
        const item = q[0];
        try {
          await send(item.method, item.path, item.body, { queueable: false });
        } catch (err) {
          if (err.status === 0) break;          // still offline: keep the rest for later
          emit('api:sync-failed', { item, error: err.message }); // server rejected it: drop it
        }
        q = q.slice(1);
        store.set(KEYS.queue, q);
      }
      emit('api:synced', { remaining: q.length });
    } finally {
      flushing = false;
    }
  }
  window.addEventListener('online', flushQueue);

  window.Api = {
    ApiError,
    get: (p) => send('GET', p),
    post: (p, b) => send('POST', p, b),
    put: (p, b) => send('PUT', p, b),
    patch: (p, b) => send('PATCH', p, b),
    del: (p) => send('DELETE', p),
    upload: (p, formData) => send('POST', p, formData),
    pending: () => (store.get(KEYS.queue) || []).length,
    flushQueue,

    // ---- session ----
    user: () => store.get(KEYS.user),
    isLoggedIn: () => !!store.get(KEYS.token),
    async login(email, password) {
      const res = await send('POST', '/auth/login', { email, password }, { anonymous: true, queueable: false });
      store.set(KEYS.token, res.token);
      store.set(KEYS.user, res.user);
      return res.user;
    },
    register: (name, email, password, role) =>
      send('POST', '/auth/register', { name, email, password, role }, { anonymous: true, queueable: false }),
    async logout() {
      try { await send('POST', '/auth/logout', undefined, { queueable: false }); } catch { /* token dies anyway */ }
      clearSession();
    },
    async refreshMe() {
      const me = await send('GET', '/auth/me');
      store.set(KEYS.user, me);
      return me;
    }
  };
})();
