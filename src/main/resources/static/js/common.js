/**
 * INFOH303 - Client API et helpers communs.
 * <p>
 * Fournit le client HTTP, la gestion de session, les composants UI réutilisables
 * (toast, modale, navbar) et les fonctions utilitaires DOM pour l'application ResumeHub.
 * </p>
 */

const API_BASE = '/api';

/**
 * Gestionnaire de session utilisateur stockée dans le localStorage.
 * <p>
 * Fournit les méthodes pour lire, écrire et supprimer la session,
 * ainsi qu'un raccourci pour récupérer l'identifiant utilisateur courant.
 * </p>
 */
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

/**
 * Effectue un appel HTTP vers l'API REST backend.
 * <p>
 * Ajoute automatiquement l'identifiant utilisateur dans le header {@code X-User-Id}
 * si une session est active. Parse la réponse JSON et lève une erreur en cas de statut HTTP non-OK.
 * </p>
 *
 * @param {string}  method       - Méthode HTTP (GET, POST, PUT, DELETE).
 * @param {string}  path         - Chemin relatif de l'endpoint (ex: {@code /utilisateurs/1}).
 * @param {Object}  body         - Corps de la requête (optionnel).
 * @param {Object}  extraHeaders - Headers supplémentaires (optionnel).
 * @returns {Promise<Object>} La réponse JSON parsée.
 */
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

/**
 * Raccourcis pour les appels API par méthode HTTP.
 */
const Api = {
  get:  (p)        => apiCall('GET', p),
  post: (p, body)  => apiCall('POST', p, body),
  put:  (p, body)  => apiCall('PUT', p, body),
  del:  (p)        => apiCall('DELETE', p),
};

/**
 * Affiche une notification toast temporaire.
 *
 * @param {string} message - Le message à afficher.
 * @param {string} type    - Le type de notification ({@code 'info'}, {@code 'success'}, {@code 'error'}).
 */
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

/**
 * Ouvre une fenêtre modale de confirmation avec actions personnalisables.
 *
 * @param {Object}   options              - Configuration de la modale.
 * @param {string}   options.title        - Titre de la modale.
 * @param {string}   options.body         - Contenu HTML ou texte du corps.
 * @param {Function} options.onConfirm    - Callback exécuté lors de la confirmation.
 * @param {string}   options.confirmLabel - Libellé du bouton de confirmation.
 * @param {string}   options.confirmClass - Classe CSS du bouton de confirmation.
 */
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

/**
 * Fonctions utilitaires pour la manipulation du DOM et l'échappement HTML.
 */

/**
 * Échappe les caractères spéciaux HTML pour prévenir les injections XSS.
 *
 * @param {string} s - La chaîne à échapper.
 * @returns {string} La chaîne échappée.
 */
function escape(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({
    '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'
  }[c]));
}

/** Raccourci pour {@code document.querySelector}. */
function $(sel, ctx = document) { return ctx.querySelector(sel); }

/** Raccourci pour {@code document.querySelectorAll} retournant un tableau. */
function $$(sel, ctx = document) { return [...ctx.querySelectorAll(sel)]; }

/**
 * Génère le rendu HTML des étoiles de notation.
 *
 * @param {number|null} note - La note à afficher (entre 0 et 5), ou {@code null} si aucune note.
 * @param {number}      max  - Le nombre maximum d'étoiles.
 * @returns {string} Chaîne HTML des étoiles.
 */
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

/**
 * Formate une date au format localisé français-belge (ex: 07 mai 2026).
 *
 * @param {string} d - La date au format ISO.
 * @returns {string} La date formatée.
 */
function formatDate(d) {
  if (!d) return '';
  const dt = new Date(d);
  return dt.toLocaleDateString('fr-BE', { day: '2-digit', month: 'short', year: 'numeric' });
}

/**
 * Extrait les initiales d'un nom d'utilisateur (max 2 caractères).
 *
 * @param {string} nom - Le nom de l'utilisateur.
 * @returns {string} Les initiales en majuscules.
 */
function initials(nom) {
  return (nom || '?').split(/[_\s]/).map(s => s[0]).join('').slice(0, 2).toUpperCase();
}

/**
 * Génère un badge coloré en fonction de la faculté.
 *
 * @param {string} faculte - Le nom de la faculté.
 * @returns {string} Chaîne HTML du badge.
 */
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

/**
 * Génère un badge coloré en fonction du type d'objet cosmétique.
 *
 * @param {string} type - Le type d'objet ({@code 'badge'}, {@code 'titre'}, {@code 'theme'}, {@code 'cosmetique'}).
 * @returns {string} Chaîne HTML du badge.
 */
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

/**
 * Génère le HTML de la barre de navigation principale.
 * <p>
 * Affiche le logo, les liens de navigation, les points de l'utilisateur
 * et un menu déroulant avec les actions du profil.
 * </p>
 *
 * @returns {string} Chaîne HTML de la navbar, ou chaîne vide si aucun utilisateur connecté.
 */
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

/** Bascule l'affichage du menu déroulant utilisateur. */
function toggleDropdown(e) {
  e.stopPropagation();
  $('#user-dropdown')?.classList.toggle('open');
}
document.addEventListener('click', () => $('#user-dropdown')?.classList.remove('open'));

/** Déconnecte l'utilisateur et redirige vers la page de connexion. */
function logout(e) {
  e.preventDefault();
  Session.clear();
  window.location.hash = '#/login';
}

/** Met en surbrillance le lien de navigation correspondant à la route active. */
function highlightActiveLink() {
  const path = window.location.hash.replace('#', '').split('/').slice(0, 2).join('/');
  $$('.navbar-links a').forEach(a => {
    a.classList.toggle('active', a.dataset.route === path);
  });
}