// Runtime configuration. API_BASE is chosen by hostname so the same build works locally and on Render.
window.APP_CONFIG = {
  API_BASE: ['localhost', '127.0.0.1'].includes(location.hostname)
    ? 'http://localhost:8080/api/v1'
    : 'https://classsync-api.onrender.com/api/v1' // E6 sets the real Render backend URL
};
