// =============================================================================
// INFOH303 - Client API et helpers communs
// =============================================================================

const API_BASE = '/api';

// ===== Session (stockee dans localStorage) =====
const Session = {
  get() {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); }
    catch { return null; }
  },
  set(user) { localStorage.setItem('user', JSON.stringify(user)); },
  clear() { localStorage.removeItem('user'); },
  uid() { return this.get()?.uid; },
  requireAuth() {
    if (!this.get()) {
      window.location.hash = '#/login';
      return false;
    }
    return true;
  }
};

// ===== Client HTTP =====
async function apiCall(method, path, body = null, extraHeaders = {}) {
  const headers = { 'Content-Type': 'application/json', ...extraHeaders };
  const uid = Session.uid();
  if (uid) headers['X-User-Id'] = uid;

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(API_BASE + path, opts);
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!res.ok) {
    const msg = (data && data.message) || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return data;
}

const Api = {
  get:  (p)        => apiCall('GET', p),
  post: (p, body)  => apiCall('POST', p, body),
  put:  (p, body)  => apiCall('PUT', p, body),
  del:  (p)        => apiCall('DELETE', p),
};

// ===== Toast =====
function toast(message, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => {
    el.style.opacity = '0';
    setTimeout(() => el.remove(), 200);
  }, 3500);
}

// ===== Modal =====
function modal({ title, body, onConfirm, confirmLabel = 'Confirmer', confirmClass = 'btn-primary' }) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay open';
  overlay.innerHTML = `
    <div class="modal">
      <div class="modal-title">${escape(title)}</div>
      <div>${typeof body === 'string' ? body : ''}</div>
      <div class="modal-actions">
        <button class="btn btn-secondary" id="modal-cancel">Annuler</button>
        <button class="btn ${confirmClass}" id="modal-confirm">${escape(confirmLabel)}</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);
  if (typeof body !== 'string') overlay.querySelector('.modal > div:nth-child(2)').appendChild(body);

  const close = () => overlay.remove();
  overlay.querySelector('#modal-cancel').onclick = close;
  overlay.querySelector('#modal-confirm').onclick = async () => {
    try { await onConfirm(); close(); }
    catch (e) { toast(e.message, 'error'); }
  };
  overlay.onclick = (e) => { if (e.target === overlay) close(); };
}

// ===== Helpers DOM =====
function escape(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({
    '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'
  }[c]));
}
function $(sel, ctx = document) { return ctx.querySelector(sel); }
function $$(sel, ctx = document) { return [...ctx.querySelectorAll(sel)]; }

function stars(note, max = 5) {
  if (note == null) return '<span class="text-muted">Aucune note</span>';
  const n = parseFloat(note);
  let html = '<span class="stars">';
  for (let i = 1; i <= max; i++) {
    html += `<span class="${i <= Math.round(n) ? '' : 'star-empty'}">★</span>`;
  }
  html += `</span> <span class="text-muted">${n.toFixed(1)}/5</span>`;
  return html;
}

function formatDate(d) {
  if (!d) return '';
  const dt = new Date(d);
  return dt.toLocaleDateString('fr-BE', { day: '2-digit', month: 'short', year: 'numeric' });
}

function initials(nom) {
  return (nom || '?').split(/[_\s]/).map(s => s[0]).join('').slice(0, 2).toUpperCase();
}

function facultyBadge(faculte) {
  const colors = {
    'Informatique': 'badge-blue',
    'Mathematiques': 'badge-purple',
    'Mathématiques': 'badge-purple',
    'Sciences': 'badge-green',
    'Economie': 'badge-yellow',
    'Économie': 'badge-yellow',
    'Langues': 'badge-red',
    'Gestion': 'badge-gray'
  };
  const cls = colors[faculte] || 'badge-gray';
  return `<span class="badge ${cls}">${escape(faculte)}</span>`;
}

function typeObjetBadge(type) {
  const map = {
    badge: ['badge-blue', 'Badge'],
    titre: ['badge-purple', 'Titre'],
    theme: ['badge-green', 'Theme'],
    cosmetique: ['badge-yellow', 'Cosmetique']
  };
  const [cls, label] = map[type] || ['badge-gray', type];
  return `<span class="badge ${cls}">${label}</span>`;
}

// ===== Navbar =====
function renderNavbar() {
  const user = Session.get();
  if (!user) return '';
  return `
    <nav class="navbar">
      <a href="#/dashboard" class="navbar-logo">📚 ResumeHub</a>
      <div class="navbar-links">
        <a href="#/dashboard" data-route="/dashboard">Dashboard</a>
        <a href="#/cours" data-route="/cours">Cours</a>
        <a href="#/leaderboard" data-route="/leaderboard">Leaderboard</a>
        <a href="#/boutique" data-route="/boutique">Boutique</a>
        <a href="#/analytics" data-route="/analytics">Analytics</a>
      </div>
      <div class="navbar-right">
        <div class="points-badge">⭐ ${user.points} pts</div>
        <div class="user-menu" onclick="toggleDropdown(event)">
          <div class="user-avatar">${initials(user.nom)}</div>
          <span>${escape(user.nom)}</span>
          <span style="opacity:0.6">▾</span>
          <div class="dropdown" id="user-dropdown">
            <a href="#/profil/${user.uid}">Mon profil</a>
            <a href="#/inventaire">Mon inventaire</a>
            <a href="#/transactions">Mes transactions</a>
            <a href="#" onclick="logout(event)">Deconnexion</a>
          </div>
        </div>
      </div>
    </nav>
  `;
}

function toggleDropdown(e) {
  e.stopPropagation();
  $('#user-dropdown')?.classList.toggle('open');
}
document.addEventListener('click', () => $('#user-dropdown')?.classList.remove('open'));

function logout(e) {
  e.preventDefault();
  Session.clear();
  window.location.hash = '#/login';
}

function highlightActiveLink() {
  const path = window.location.hash.replace('#', '').split('/').slice(0, 2).join('/');
  $$('.navbar-links a').forEach(a => {
    a.classList.toggle('active', a.dataset.route === path);
  });
}
