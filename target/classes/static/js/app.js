// =============================================================================
// INFOH303 - Application SPA - Router et controllers de pages
// =============================================================================

const app = {
  routes: {},
  register(path, handler) { this.routes[path] = handler; },
  async navigate() {
    const hash = window.location.hash || '#/login';
    const parts = hash.replace('#/', '').split('/');
    const route = '/' + parts[0];
    const params = parts.slice(1);
    const handler = this.routes[route] || this.routes['/login'];

    const root = document.getElementById('root');
    if (route !== '/login' && !Session.get()) {
      window.location.hash = '#/login';
      return;
    }

    root.innerHTML = (route === '/login' ? '' : renderNavbar()) +
                     '<div id="page" class="container"><div class="loading">Chargement...</div></div>';
    highlightActiveLink();
    try { await handler(params); }
    catch (e) { $('#page').innerHTML = `<div class="card"><p style="color:var(--danger)">Erreur : ${escape(e.message)}</p></div>`; }
  }
};

window.addEventListener('hashchange', () => app.navigate());
window.addEventListener('DOMContentLoaded', () => app.navigate());

// ============================================================================
// S0 - LOGIN / REGISTER
// ============================================================================
app.register('/login', async () => {
  document.getElementById('root').innerHTML = `
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-logo">📚 ResumeHub · INFOH303</div>
        <div class="auth-title" id="auth-title">Connexion</div>
        <div id="auth-form">
          <div class="form-group">
            <label class="form-label">Nom d'utilisateur</label>
            <input type="text" class="form-input" id="auth-nom" placeholder="alice_dupont">
          </div>
          <div class="form-group" id="email-group" style="display:none">
            <label class="form-label">Email</label>
            <input type="email" class="form-input" id="auth-email" placeholder="alice@example.com">
          </div>
          <div class="form-group">
            <label class="form-label">Mot de passe (optionnel pour les comptes pre-existants)</label>
            <input type="password" class="form-input" id="auth-mdp" placeholder="••••••••">
          </div>
          <button class="btn btn-primary btn-block btn-lg" id="auth-submit">Se connecter</button>
          <div class="auth-toggle">
            <span id="auth-msg">Pas de compte ?</span>
            <a href="#" id="auth-toggle">S'inscrire</a>
          </div>
        </div>
      </div>
    </div>
  `;

  let mode = 'login';
  const titleEl = $('#auth-title');
  const submitEl = $('#auth-submit');
  const msgEl = $('#auth-msg');
  const toggleEl = $('#auth-toggle');
  const emailGroup = $('#email-group');

  toggleEl.onclick = (e) => {
    e.preventDefault();
    mode = mode === 'login' ? 'register' : 'login';
    titleEl.textContent = mode === 'login' ? 'Connexion' : 'Inscription';
    submitEl.textContent = mode === 'login' ? 'Se connecter' : 'S\'inscrire';
    emailGroup.style.display = mode === 'register' ? 'block' : 'none';
    msgEl.textContent = mode === 'login' ? 'Pas de compte ?' : 'Deja inscrit ?';
    toggleEl.textContent = mode === 'login' ? 'S\'inscrire' : 'Se connecter';
  };

  submitEl.onclick = async () => {
    const nom = $('#auth-nom').value.trim();
    const email = $('#auth-email').value.trim();
    const mdp = $('#auth-mdp').value;
    if (!nom) return toast('Nom requis', 'error');
    try {
      let user;
      if (mode === 'login') {
        user = await Api.post('/utilisateurs/login', { nom, motDePasse: mdp });
      } else {
        if (!email) return toast('Email requis', 'error');
        user = await Api.post('/utilisateurs/register', { nom, email, motDePasse: mdp || 'default' });
      }
      Session.set(user);
      toast(`Bienvenue, ${user.nom} !`, 'success');
      window.location.hash = '#/dashboard';
    } catch (e) { toast(e.message, 'error'); }
  };
});

// ============================================================================
// S1 - DASHBOARD
// ============================================================================
app.register('/dashboard', async () => {
  const user = Session.get();
  const [resumes, leaderboard] = await Promise.all([
    Api.get(`/resumes?auteur=${user.uid}`),
    Api.get('/leaderboard?top=5')
  ]);

  const noteMoy = resumes.length > 0
      ? (resumes.reduce((s, r) => s + (r.noteMoyenne || 0), 0) / resumes.filter(r => r.noteMoyenne).length || 0)
      : 0;

  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">Bienvenue, ${escape(user.nom)} 👋</div>
      ${user.titreActif ? `<div class="badge badge-purple">${escape(user.titreActif.nom)}</div>` : ''}
      ${user.badgeActif ? `<span class="badge badge-blue" style="margin-left:6px">🏅 ${escape(user.badgeActif.nom)}</span>` : ''}
    </div>

    <div class="grid grid-cols-4 mb-6">
      <div class="stat-card"><div class="stat-label">Mes points</div><div class="stat-value warning">⭐ ${user.points}</div></div>
      <div class="stat-card"><div class="stat-label">Mon niveau</div><div class="stat-value accent">🎯 ${user.niveau}</div></div>
      <div class="stat-card"><div class="stat-label">Mes resumes</div><div class="stat-value">📄 ${resumes.length}</div></div>
      <div class="stat-card"><div class="stat-label">Note moyenne</div><div class="stat-value success">${noteMoy ? noteMoy.toFixed(1) + '/5' : '—'}</div></div>
    </div>

    <div class="grid grid-cols-3" style="grid-template-columns: 2fr 1fr">
      <div>
        <div class="flex-between mb-4">
          <div class="card-title" style="margin:0">Mes resumes recents</div>
          <a href="#/resume-form" class="btn btn-primary btn-sm">+ Publier</a>
        </div>
        <div class="table-wrapper">
          ${resumes.length === 0 ? `
            <div class="empty-state">
              <div class="empty-state-icon">📝</div>
              <div>Vous n'avez pas encore publie de resume</div>
              <a href="#/resume-form" class="btn btn-primary mt-4">Publier mon premier resume</a>
            </div>
          ` : `
            <table>
              <thead>
                <tr><th>Titre</th><th>Cours</th><th>Date</th><th>Note</th><th>Actions</th></tr>
              </thead>
              <tbody>
                ${resumes.slice(0, 5).map(r => `
                  <tr>
                    <td><a href="#/resume/${r.rid}">${escape(r.titre)}</a></td>
                    <td><span class="text-muted">${escape(r.codeCours)}</span> ${escape(r.coursNom)}</td>
                    <td>${formatDate(r.datePublication)}</td>
                    <td>${stars(r.noteMoyenne)}</td>
                    <td>
                      <a href="#/resume-form/${r.rid}" class="btn btn-sm btn-secondary">Modifier</a>
                    </td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          `}
        </div>
      </div>

      <div>
        <div class="card-title">Top 5</div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>#</th><th>Nom</th><th>Pts</th></tr></thead>
            <tbody>
              ${leaderboard.map(e => `
                <tr ${e.uid === user.uid ? 'class="highlighted"' : ''}>
                  <td>${e.rang}</td>
                  <td><a href="#/profil/${e.uid}">${escape(e.nom)}</a></td>
                  <td>${e.points}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
        <a href="#/leaderboard" class="btn btn-secondary btn-block mt-2">Voir le classement complet</a>
      </div>
    </div>
  `;
});

// ============================================================================
// S2 + S3 - LISTE COURS / DETAIL COURS (fusionnes en un seul handler)
// ============================================================================
app.register('/cours', async (params) => {
  if (params.length > 0) {
    // ---- S3 - DETAIL COURS ----
    const code = params[0];
    const [cours, resumes] = await Promise.all([
      Api.get(`/cours/${code}`),
      Api.get(`/resumes?cours=${code}`)
    ]);

    $('#page').innerHTML = `
      <div class="breadcrumb"><a href="#/cours">Cours</a><span>›</span>${escape(cours.nom)}</div>
      <div class="page-header">
        <div class="page-title">${escape(cours.nom)}</div>
        <div class="flex gap-2 mt-2">
          <span class="badge badge-gray">${escape(cours.code)}</span>
          ${facultyBadge(cours.faculte)}
          <span class="badge badge-yellow">${cours.credits} credits</span>
          <span class="badge badge-gray">${escape(cours.anneeAcademique)}</span>
        </div>
      </div>

      <div class="flex-between mb-4">
        <div class="card-title" style="margin:0">${resumes.length} resume(s) publie(s)</div>
        <a href="#/resume-form?cours=${cours.code}" class="btn btn-primary btn-sm">+ Publier un resume pour ce cours</a>
      </div>

      <div class="table-wrapper">
        ${resumes.length === 0 ? `
          <div class="empty-state">
            <div class="empty-state-icon">📭</div>
            <div>Aucun resume pour ce cours pour l'instant</div>
          </div>
        ` : `
          <table>
            <thead>
              <tr><th>Titre</th><th>Auteur</th><th>Date</th><th>Version</th><th>Note moyenne</th></tr>
            </thead>
            <tbody>
              ${resumes.map(r => `
                <tr>
                  <td><a href="#/resume/${r.rid}">${escape(r.titre)}</a></td>
                  <td><a href="#/profil/${r.auteurUid}">${escape(r.auteurNom)}</a></td>
                  <td>${formatDate(r.datePublication)}</td>
                  <td>v${r.version}</td>
                  <td>${stars(r.noteMoyenne)} ${r.nbEvaluations ? `<span class="text-muted">(${r.nbEvaluations})</span>` : ''}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        `}
      </div>
    `;
  } else {
    // ---- S2 - LISTE DES COURS ----
    const cours = await Api.get('/cours');
    const allResumes = await Api.get('/resumes');
    const nbParCours = {};
    allResumes.forEach(r => { nbParCours[r.codeCours] = (nbParCours[r.codeCours] || 0) + 1; });

    const facultes = [...new Set(cours.map(c => c.faculte))].sort();

    $('#page').innerHTML = `
      <div class="page-header">
        <div class="page-title">Cours</div>
        <div class="page-subtitle">${cours.length} cours disponibles</div>
      </div>

      <div class="card mb-4">
        <div class="grid grid-cols-3">
          <div class="form-group" style="margin:0">
            <input type="text" class="form-input" id="filter-q" placeholder="Rechercher un cours...">
          </div>
          <div class="form-group" style="margin:0">
            <select class="form-select" id="filter-fac">
              <option value="">Toutes les facultes</option>
              ${facultes.map(f => `<option>${escape(f)}</option>`).join('')}
            </select>
          </div>
          <div class="form-group" style="margin:0">
            <select class="form-select" id="filter-cred">
              <option value="">Tous les credits</option>
              <option value="3">3 credits</option>
              <option value="5">5 credits</option>
            </select>
          </div>
        </div>
      </div>

      <div class="table-wrapper">
        <table id="cours-table">
          <thead>
            <tr><th>Code</th><th>Nom</th><th>Faculte</th><th>Credits</th><th>Resumes</th></tr>
          </thead>
          <tbody>
            ${cours.map(c => `
              <tr data-fac="${escape(c.faculte)}" data-cred="${c.credits}" data-search="${escape((c.code+' '+c.nom).toLowerCase())}">
                <td><a href="#/cours/${c.code}"><strong>${escape(c.code)}</strong></a></td>
                <td><a href="#/cours/${c.code}">${escape(c.nom)}</a></td>
                <td>${facultyBadge(c.faculte)}</td>
                <td>${c.credits}</td>
                <td>${nbParCours[c.code] || 0}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

    const filter = () => {
      const q = $('#filter-q').value.toLowerCase();
      const f = $('#filter-fac').value;
      const c = $('#filter-cred').value;
      $$('#cours-table tbody tr').forEach(tr => {
        const ok = (!q || tr.dataset.search.includes(q)) &&
                   (!f || tr.dataset.fac === f) &&
                   (!c || tr.dataset.cred === c);
        tr.style.display = ok ? '' : 'none';
      });
    };
    ['filter-q','filter-fac','filter-cred'].forEach(id => $('#'+id).oninput = filter);
  }
});

// ============================================================================
// S5 - DETAIL RESUME
// ============================================================================
app.register('/resume', async (params) => {
  const rid = params[0];
  const user = Session.get();
  const [resume, evaluations] = await Promise.all([
    Api.get(`/resumes/${rid}`),
    Api.get(`/evaluations?rid=${rid}`)
  ]);

  const isAuthor = resume.auteurUid === user.uid;
  const hasEvaluated = evaluations.some(e => e.evaluateurUid === user.uid);
  const myEval = evaluations.find(e => e.evaluateurUid === user.uid);

  $('#page').innerHTML = `
    <div class="breadcrumb">
      <a href="#/cours">Cours</a><span>›</span>
      <a href="#/cours/${resume.codeCours}">${escape(resume.coursNom)}</a><span>›</span>
      ${escape(resume.titre)}
    </div>

    <div class="page-header flex-between">
      <div>
        <div class="page-title">${escape(resume.titre)}</div>
        <div class="text-muted mt-2">
          Par <a href="#/profil/${resume.auteurUid}">${escape(resume.auteurNom)}</a> ·
          ${formatDate(resume.datePublication)} ·
          v${resume.version} ·
          <span class="badge ${resume.visibilite === 'public' ? 'badge-green' : 'badge-gray'}">${resume.visibilite}</span>
        </div>
      </div>
      <div class="text-center">
        <div style="font-size:32px;font-weight:600">${resume.noteMoyenne ? resume.noteMoyenne.toFixed(1) : '—'}</div>
        ${stars(resume.noteMoyenne)}
        <div class="text-sm text-muted">${resume.nbEvaluations || 0} evaluation(s)</div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">Contenu</div>
      <div style="white-space:pre-wrap;line-height:1.7">${escape(resume.description)}</div>
    </div>

    ${isAuthor ? `
      <div class="flex gap-2 mb-6">
        <a href="#/resume-form/${resume.rid}" class="btn btn-secondary">Modifier</a>
        <button class="btn btn-danger" id="btn-delete">Supprimer</button>
      </div>
    ` : ''}

    <div class="card">
      <div class="card-title">Evaluations (${evaluations.length})</div>
      ${isAuthor ? `
        <div class="text-muted">Vous ne pouvez pas evaluer votre propre resume.</div>
      ` : hasEvaluated ? `
        <div class="text-muted">Vous avez deja evalue ce resume : ${stars(myEval.note)} <em>${escape(myEval.commentaire || '')}</em></div>
      ` : `
        <div class="form-group">
          <label class="form-label">Votre note</label>
          <div class="stars stars-input" id="note-input">
            ${[1,2,3,4,5].map(i => `<span data-n="${i}" class="star-empty">★</span>`).join('')}
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Commentaire (optionnel)</label>
          <textarea class="form-textarea" id="comment-input" placeholder="Votre avis..."></textarea>
        </div>
        <button class="btn btn-primary" id="btn-eval">Envoyer l'evaluation</button>
      `}

      ${evaluations.length > 0 ? `
        <hr style="border-color:var(--border);margin:20px 0">
        ${evaluations.map(e => `
          <div style="padding:12px 0;border-bottom:1px solid var(--border)">
            <div class="flex-between">
              <div>
                <a href="#/profil/${e.evaluateurUid}"><strong>${escape(e.evaluateurNom)}</strong></a>
                <span class="text-muted text-sm"> · ${formatDate(e.dateEval)}</span>
              </div>
              ${stars(e.note)}
            </div>
            ${e.commentaire ? `<div class="mt-2">${escape(e.commentaire)}</div>` : ''}
          </div>
        `).join('')}
      ` : ''}
    </div>
  `;

  // Etoiles cliquables
  let noteValue = 0;
  $$('#note-input span').forEach(el => {
    el.onclick = () => {
      noteValue = parseInt(el.dataset.n);
      $$('#note-input span').forEach(s =>
        s.classList.toggle('star-empty', parseInt(s.dataset.n) > noteValue));
    };
  });

  if ($('#btn-eval')) $('#btn-eval').onclick = async () => {
    if (noteValue === 0) return toast('Note obligatoire', 'error');
    try {
      await Api.post('/evaluations', {
        rid: parseInt(rid),
        note: noteValue,
        commentaire: $('#comment-input').value || null
      });
      toast('Evaluation envoyee !', 'success');
      // Refresh user points (the author's points changed, but our points may have changed via reload)
      app.navigate();
    } catch (e) { toast(e.message, 'error'); }
  };

  if ($('#btn-delete')) $('#btn-delete').onclick = () => {
    modal({
      title: 'Supprimer ce resume ?',
      body: 'Cette action est irreversible.',
      confirmLabel: 'Supprimer',
      confirmClass: 'btn-danger',
      onConfirm: async () => {
        await Api.del(`/resumes/${rid}`);
        toast('Resume supprime', 'success');
        window.location.hash = '#/dashboard';
      }
    });
  };
});

// ============================================================================
// S4 - PUBLIER / EDITER RESUME
// ============================================================================
app.register('/resume-form', async (params) => {
  const editing = params.length > 0;
  const rid = editing ? params[0] : null;
  const queryParams = new URLSearchParams(window.location.hash.split('?')[1] || '');
  const preCours = queryParams.get('cours');

  const cours = await Api.get('/cours');
  let existing = null;
  if (editing) existing = await Api.get(`/resumes/${rid}`);

  $('#page').innerHTML = `
    <div class="breadcrumb"><a href="#/dashboard">Dashboard</a><span>›</span>${editing ? 'Modifier' : 'Publier'} un resume</div>
    <div class="page-header">
      <div class="page-title">${editing ? 'Modifier le resume' : 'Publier un resume'}</div>
    </div>

    <div class="card" style="max-width:700px">
      <div class="form-group">
        <label class="form-label">Cours *</label>
        <select class="form-select" id="f-cours" ${editing ? 'disabled' : ''}>
          <option value="">-- Choisir un cours --</option>
          ${cours.map(c => `
            <option value="${escape(c.code)}" ${(existing && existing.codeCours === c.code) || preCours === c.code ? 'selected' : ''}>
              ${escape(c.code)} - ${escape(c.nom)}
            </option>
          `).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Titre *</label>
        <input type="text" class="form-input" id="f-titre" maxlength="200" value="${escape(existing?.titre || '')}">
      </div>
      <div class="form-group">
        <label class="form-label">Description *</label>
        <textarea class="form-textarea" id="f-desc" placeholder="Contenu du resume...">${escape(existing?.description || '')}</textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Visibilite</label>
        <select class="form-select" id="f-vis">
          <option value="public" ${existing?.visibilite !== 'prive' ? 'selected' : ''}>Public</option>
          <option value="prive" ${existing?.visibilite === 'prive' ? 'selected' : ''}>Prive</option>
        </select>
      </div>
      <div class="flex gap-2">
        <button class="btn btn-primary" id="btn-save">${editing ? 'Enregistrer' : 'Publier (+10 pts)'}</button>
        <a href="#/dashboard" class="btn btn-secondary">Annuler</a>
      </div>
    </div>
  `;

  $('#btn-save').onclick = async () => {
    const body = {
      titre: $('#f-titre').value.trim(),
      description: $('#f-desc').value.trim(),
      visibilite: $('#f-vis').value,
      codeCours: $('#f-cours').value
    };
    if (!body.titre || !body.description || !body.codeCours)
      return toast('Tous les champs * sont obligatoires', 'error');
    try {
      let r;
      if (editing) {
        r = await Api.put(`/resumes/${rid}`, body);
        toast('Resume mis a jour', 'success');
      } else {
        r = await Api.post('/resumes', body);
        toast('Resume publie ! +10 points', 'success');
        // Refresh user's points
        const u = await Api.get(`/utilisateurs/${Session.uid()}`);
        Session.set(u);
      }
      window.location.hash = `#/resume/${r.rid}`;
    } catch (e) { toast(e.message, 'error'); }
  };
});

// ============================================================================
// S6 - PROFIL UTILISATEUR
// ============================================================================
app.register('/profil', async (params) => {
  const uid = params[0] || Session.uid();
  const isOwn = parseInt(uid) === Session.uid();
  const user = await Api.get(`/utilisateurs/${uid}`);
  const [resumes, evaluations, possessions] = await Promise.all([
    Api.get(`/resumes?auteur=${uid}`),
    Api.get(`/evaluations?evaluateur=${uid}`),
    isOwn ? Api.get(`/possessions?uid=${uid}`) : Promise.resolve([])
  ]);

  $('#page').innerHTML = `
    <div class="profile-header">
      <div class="profile-avatar">${initials(user.nom)}</div>
      <div>
        <div class="page-title">${escape(user.nom)}</div>
        ${user.titreActif ? `<div class="badge badge-purple mt-2">${escape(user.titreActif.nom)}</div>` : ''}
        ${user.badgeActif ? `<span class="badge badge-blue mt-2" style="margin-left:6px">🏅 ${escape(user.badgeActif.nom)}</span>` : ''}
        <div class="text-muted text-sm mt-2">Membre depuis ${formatDate(user.dateInscription)} · Niveau ${user.niveau}</div>
      </div>
    </div>

    <div class="grid grid-cols-3 mb-6">
      <div class="stat-card"><div class="stat-label">Points</div><div class="stat-value warning">⭐ ${user.points}</div></div>
      <div class="stat-card"><div class="stat-label">Resumes publies</div><div class="stat-value">${resumes.length}</div></div>
      <div class="stat-card"><div class="stat-label">Evaluations donnees</div><div class="stat-value">${evaluations.length}</div></div>
    </div>

    <div class="tabs">
      <button class="tab active" data-tab="resumes">Resumes (${resumes.length})</button>
      <button class="tab" data-tab="evals">Evaluations donnees (${evaluations.length})</button>
      ${isOwn ? `<button class="tab" data-tab="objets">Mes objets (${possessions.length})</button>` : ''}
    </div>

    <div id="tab-resumes" class="tab-content">
      <div class="table-wrapper">
        ${resumes.length === 0 ? `<div class="empty-state">Aucun resume publie</div>` : `
          <table>
            <thead><tr><th>Titre</th><th>Cours</th><th>Date</th><th>Note</th></tr></thead>
            <tbody>
              ${resumes.map(r => `
                <tr>
                  <td><a href="#/resume/${r.rid}">${escape(r.titre)}</a></td>
                  <td>${escape(r.coursNom)}</td>
                  <td>${formatDate(r.datePublication)}</td>
                  <td>${stars(r.noteMoyenne)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        `}
      </div>
    </div>

    <div id="tab-evals" class="tab-content" style="display:none">
      <div class="table-wrapper">
        ${evaluations.length === 0 ? `<div class="empty-state">Aucune evaluation donnee</div>` : `
          <table>
            <thead><tr><th>Resume</th><th>Note</th><th>Commentaire</th><th>Date</th></tr></thead>
            <tbody>
              ${evaluations.map(e => `
                <tr>
                  <td><a href="#/resume/${e.rid}">${escape(e.resumeTitre)}</a></td>
                  <td>${stars(e.note)}</td>
                  <td>${escape(e.commentaire || '—')}</td>
                  <td>${formatDate(e.dateEval)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        `}
      </div>
    </div>

    ${isOwn ? `
      <div id="tab-objets" class="tab-content" style="display:none">
        <div class="shop-grid">
          ${possessions.length === 0 ? `<div class="empty-state">Aucun objet possede.<br><a href="#/boutique" class="btn btn-primary mt-4">Aller a la boutique</a></div>` : possessions.map(p => `
            <div class="shop-card">
              <div class="shop-card-name">${escape(p.objet.nom)}</div>
              ${typeObjetBadge(p.objet.type)}
              <div class="text-muted text-sm">${escape(p.objet.description || '')}</div>
              ${(p.objet.type === 'badge' || p.objet.type === 'titre') ? renderActivateBtn(user, p.objet) : ''}
            </div>
          `).join('')}
        </div>
      </div>
    ` : ''}
  `;

  $$('.tab').forEach(t => t.onclick = () => {
    $$('.tab').forEach(x => x.classList.remove('active'));
    t.classList.add('active');
    $$('.tab-content').forEach(c => c.style.display = 'none');
    $('#tab-' + t.dataset.tab).style.display = 'block';
  });
});

function renderActivateBtn(user, objet) {
  const isActive = (objet.type === 'badge' && user.badgeActif?.oid === objet.oid)
                 || (objet.type === 'titre' && user.titreActif?.oid === objet.oid);
  if (isActive) {
    return `<button class="btn btn-success btn-sm" onclick="deactivateObjet(${objet.oid}, '${objet.type}')">✓ Actif (cliquer pour desactiver)</button>`;
  }
  return `<button class="btn btn-secondary btn-sm" onclick="activateObjet(${objet.oid}, '${objet.type}')">Activer</button>`;
}

async function activateObjet(oid, type) {
  try {
    const u = await Api.put(`/utilisateurs/${Session.uid()}`, {
      [type === 'badge' ? 'badgeActif' : 'titreActif']: oid
    });
    Session.set(u);
    toast(`${type} active !`, 'success');
    app.navigate();
  } catch (e) { toast(e.message, 'error'); }
}
async function deactivateObjet(oid, type) {
  try {
    const u = await Api.put(`/utilisateurs/${Session.uid()}`, {
      [type === 'badge' ? 'badgeActif' : 'titreActif']: null
    });
    Session.set(u);
    toast(`${type} desactive`, 'success');
    app.navigate();
  } catch (e) { toast(e.message, 'error'); }
}

// ============================================================================
// S7 - LEADERBOARD
// ============================================================================
app.register('/leaderboard', async () => {
  const lb = await Api.get('/leaderboard');
  const me = Session.uid();

  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">🏆 Leaderboard</div>
      <div class="page-subtitle">Classement des contributeurs par points</div>
    </div>

    ${lb.length >= 3 ? `
      <div class="podium">
        <div class="podium-card podium-2">
          <div class="medal">🥈</div>
          <div class="font-bold">${escape(lb[1].nom)}</div>
          <div class="text-muted">${lb[1].points} pts</div>
          <a href="#/profil/${lb[1].uid}" class="btn btn-secondary btn-sm mt-2">Profil</a>
        </div>
        <div class="podium-card podium-1">
          <div class="medal">🥇</div>
          <div class="font-bold">${escape(lb[0].nom)}</div>
          <div class="text-muted">${lb[0].points} pts</div>
          <a href="#/profil/${lb[0].uid}" class="btn btn-secondary btn-sm mt-2">Profil</a>
        </div>
        <div class="podium-card podium-3">
          <div class="medal">🥉</div>
          <div class="font-bold">${escape(lb[2].nom)}</div>
          <div class="text-muted">${lb[2].points} pts</div>
          <a href="#/profil/${lb[2].uid}" class="btn btn-secondary btn-sm mt-2">Profil</a>
        </div>
      </div>
    ` : ''}

    <div class="table-wrapper">
      <table>
        <thead>
          <tr><th>Rang</th><th>Utilisateur</th><th>Points</th><th>Niveau</th><th>Titre</th></tr>
        </thead>
        <tbody>
          ${lb.map(e => `
            <tr ${e.uid === me ? 'class="highlighted"' : ''}>
              <td>${e.rang === 1 ? '🥇' : e.rang === 2 ? '🥈' : e.rang === 3 ? '🥉' : `#${e.rang}`}</td>
              <td><a href="#/profil/${e.uid}">${escape(e.nom)}</a></td>
              <td><strong>${e.points}</strong></td>
              <td>Niveau ${e.niveau}</td>
              <td>${e.titreActif ? `<span class="badge badge-purple">${escape(e.titreActif)}</span>` : '—'}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;
});

// ============================================================================
// S8 - BOUTIQUE
// ============================================================================
app.register('/boutique', async () => {
  const user = await Api.get(`/utilisateurs/${Session.uid()}`);
  Session.set(user);
  const [objets, possessions] = await Promise.all([
    Api.get('/objets'),
    Api.get(`/possessions?uid=${user.uid}`)
  ]);
  const ownedOids = new Set(possessions.map(p => p.oid));

  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">🛍️ Boutique</div>
      <div class="page-subtitle">Votre solde : <span class="badge badge-yellow">⭐ ${user.points} pts</span></div>
    </div>

    <div class="tabs">
      <button class="tab active" data-type="">Tous</button>
      <button class="tab" data-type="badge">Badges</button>
      <button class="tab" data-type="titre">Titres</button>
      <button class="tab" data-type="theme">Themes</button>
      <button class="tab" data-type="cosmetique">Cosmetiques</button>
    </div>

    <div class="shop-grid" id="shop-grid">
      ${objets.map(o => renderShopCard(o, user.points, ownedOids)).join('')}
    </div>
  `;

  $$('.tab').forEach(t => t.onclick = () => {
    $$('.tab').forEach(x => x.classList.remove('active'));
    t.classList.add('active');
    const type = t.dataset.type;
    $('#shop-grid').innerHTML = objets
        .filter(o => !type || o.type === type)
        .map(o => renderShopCard(o, user.points, ownedOids))
        .join('');
  });
});

function renderShopCard(o, points, ownedOids) {
  const owned = ownedOids.has(o.oid);
  const canAfford = points >= o.prix;
  let btn;
  if (owned) btn = `<button class="btn btn-success btn-sm" disabled>✓ Possede</button>`;
  else if (!canAfford) btn = `<button class="btn btn-secondary btn-sm" disabled>Fonds insuffisants</button>`;
  else btn = `<button class="btn btn-primary btn-sm" onclick="buyObjet(${o.oid}, '${escape(o.nom)}', ${o.prix})">Acheter</button>`;

  return `
    <div class="shop-card">
      <div class="flex-between">
        <div class="shop-card-name">${escape(o.nom)}</div>
        ${typeObjetBadge(o.type)}
      </div>
      ${o.description ? `<div class="text-muted text-sm">${escape(o.description)}</div>` : ''}
      <div class="shop-card-price">⭐ ${o.prix} pts</div>
      ${btn}
    </div>
  `;
}

async function buyObjet(oid, nom, prix) {
  modal({
    title: `Acheter "${nom}" pour ${prix} pts ?`,
    body: `<div class="text-muted">Apres achat : ${Session.get().points - prix} pts</div>`,
    confirmLabel: 'Confirmer',
    onConfirm: async () => {
      try {
        await Api.post('/possessions', { uid: Session.uid(), oid });
        const u = await Api.get(`/utilisateurs/${Session.uid()}`);
        Session.set(u);
        toast(`"${nom}" achete !`, 'success');
        app.navigate();
      } catch (e) { toast(e.message, 'error'); }
    }
  });
}

// ============================================================================
// S9 - INVENTAIRE
// ============================================================================
app.register('/inventaire', async () => {
  const user = Session.get();
  const possessions = await Api.get(`/possessions?uid=${user.uid}`);

  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">🎒 Mon inventaire</div>
      <div class="page-subtitle">${possessions.length} objet(s) possede(s)</div>
    </div>

    ${possessions.length === 0 ? `
      <div class="empty-state card">
        <div class="empty-state-icon">📦</div>
        <div>Vous n'avez pas encore d'objets</div>
        <a href="#/boutique" class="btn btn-primary mt-4">Aller a la boutique</a>
      </div>
    ` : `
      <div class="shop-grid">
        ${possessions.map(p => `
          <div class="shop-card">
            <div class="flex-between">
              <div class="shop-card-name">${escape(p.objet.nom)}</div>
              ${typeObjetBadge(p.objet.type)}
            </div>
            ${p.objet.description ? `<div class="text-muted text-sm">${escape(p.objet.description)}</div>` : ''}
            <div class="text-sm text-muted">Acquis le ${formatDate(p.dateAchat)}</div>
            ${(p.objet.type === 'badge' || p.objet.type === 'titre') ? renderActivateBtn(user, p.objet) : ''}
          </div>
        `).join('')}
      </div>
    `}
  `;
});

// ============================================================================
// S10 - HISTORIQUE TRANSACTIONS
// ============================================================================
app.register('/transactions', async () => {
  const user = Session.get();
  const transactions = await Api.get(`/transactions?uid=${user.uid}`);
  const totalGain = transactions.filter(t => t.montant > 0).reduce((s, t) => s + t.montant, 0);
  const totalDep = Math.abs(transactions.filter(t => t.montant < 0).reduce((s, t) => s + t.montant, 0));

  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">💰 Mes transactions</div>
    </div>

    <div class="grid grid-cols-3 mb-6">
      <div class="stat-card"><div class="stat-label">Total gagne</div><div class="stat-value success">+${totalGain} pts</div></div>
      <div class="stat-card"><div class="stat-label">Total depense</div><div class="stat-value" style="color:var(--danger)">-${totalDep} pts</div></div>
      <div class="stat-card"><div class="stat-label">Solde actuel</div><div class="stat-value warning">${user.points} pts</div></div>
    </div>

    <div class="table-wrapper">
      ${transactions.length === 0 ? `<div class="empty-state">Aucune transaction</div>` : `
        <table>
          <thead><tr><th>Date</th><th>Type</th><th>Montant</th><th>Source</th></tr></thead>
          <tbody>
            ${transactions.map(t => {
              const typeColor = t.typeTransaction === 'Dépense' ? 'badge-red' : 'badge-green';
              return `
              <tr>
                <td>${formatDate(t.dateTransaction)}</td>
                <td><span class="badge ${typeColor}">${t.typeTransaction}</span></td>
                <td style="font-weight:600;color:${t.montant > 0 ? 'var(--success)' : 'var(--danger)'}">${t.montant > 0 ? '+' : ''}${t.montant} pts</td>
                <td>${escape(t.description)}</td>
              </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      `}
    </div>
  `;
});

// ============================================================================
// S11 - ANALYTICS (les 8 requetes)
// ============================================================================
app.register('/analytics', async () => {
  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">📊 Requetes analytiques</div>
      <div class="page-subtitle">Les 8 requetes imposees par l'enonce du projet</div>
    </div>
    <div class="loading">Execution des requetes...</div>
  `;

  const [r1, r2, r3, r4, r5, r6, r7, r8] = await Promise.all([
    Api.get('/analytics/top10'),
    Api.get('/analytics/multi-cours'),
    Api.get('/analytics/cours-populaire'),
    Api.get('/analytics/best-resumes'),
    Api.get('/analytics/no-resume'),
    Api.get('/analytics/objet-populaire'),
    Api.get('/analytics/overspenders'),
    Api.get('/analytics/avg-resumes')
  ]);

  $('#page').innerHTML = `
    <div class="page-header">
      <div class="page-title">📊 Requetes analytiques</div>
      <div class="page-subtitle">Les 8 requetes imposees par l'enonce</div>
    </div>

    <div class="card">
      <div class="card-title">Q1 — Top 10 utilisateurs par points</div>
      <table>
        <thead><tr><th>Rang</th><th>Nom</th><th>Points</th></tr></thead>
        <tbody>${r1.map(e => `<tr><td>#${e.rang}</td><td>${escape(e.nom)}</td><td>${e.points}</td></tr>`).join('')}</tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-title">Q2 — Utilisateurs avec resumes dans 3+ cours differents</div>
      ${r2.length === 0 ? '<div class="text-muted">Aucun resultat</div>' : `
        <table>
          <thead><tr><th>Nom</th><th>Email</th><th>Points</th></tr></thead>
          <tbody>${r2.map(u => `<tr><td>${escape(u.nom)}</td><td>${escape(u.email)}</td><td>${u.points}</td></tr>`).join('')}</tbody>
        </table>
      `}
    </div>

    <div class="card">
      <div class="card-title">Q3 — Cours avec le plus de resumes</div>
      ${r3 ? `<div class="text-2xl font-bold">${escape(r3.code)} - ${escape(r3.nom)}</div><div class="text-muted">${r3.nbResumes} resume(s)</div>` : '<div>—</div>'}
    </div>

    <div class="card">
      <div class="card-title">Q4 — Meilleurs resumes par cours</div>
      <table>
        <thead><tr><th>Cours</th><th>Resume</th><th>Note moyenne</th></tr></thead>
        <tbody>${r4.map(r => `<tr><td>${escape(r.codeCours)}</td><td><a href="#/resume/${r.rid}">${escape(r.titre)}</a></td><td>${stars(r.noteMoyenne)}</td></tr>`).join('')}</tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-title">Q5 — Utilisateurs n'ayant jamais publie de resume</div>
      ${r5.length === 0 ? '<div class="text-muted">Tous les utilisateurs ont publie au moins un resume</div>' : `
        <table>
          <thead><tr><th>Nom</th><th>Email</th><th>Date d'inscription</th></tr></thead>
          <tbody>${r5.map(u => `<tr><td>${escape(u.nom)}</td><td>${escape(u.email)}</td><td>${formatDate(u.dateInscription)}</td></tr>`).join('')}</tbody>
        </table>
      `}
    </div>

    <div class="card">
      <div class="card-title">Q6 — Objet cosmetique le plus achete</div>
      ${r6 ? `<div class="text-2xl font-bold">${escape(r6.nom)} ${typeObjetBadge(r6.type)}</div><div class="text-muted">${r6.nbAchats} achat(s)</div>` : '<div>—</div>'}
    </div>

    <div class="card">
      <div class="card-title">Q7 — Utilisateurs ayant depense plus que disponible</div>
      ${r7.length === 0 ? '<div class="text-muted">Aucun utilisateur en negatif (contrainte respectee ✓)</div>' : `
        <table>
          <thead><tr><th>Nom</th><th>Points actuels</th><th>Total depense</th></tr></thead>
          <tbody>${r7.map(u => `<tr><td>${escape(u.nom)}</td><td>${u.pointsActuels}</td><td>${u.totalDepense}</td></tr>`).join('')}</tbody>
        </table>
      `}
    </div>

    <div class="card">
      <div class="card-title">Q8 — Nombre moyen de resumes par utilisateur</div>
      <div class="text-2xl font-bold">${r8.moyenne ? r8.moyenne.toFixed(2) : '—'} resumes / utilisateur</div>
    </div>
  `;
});
