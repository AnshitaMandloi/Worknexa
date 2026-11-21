import { api, session } from './api.js';
import { html, put, toast, ago, initials, ROLE, ICONS } from './ui.js';
import * as V from './views.js';

const root = document.getElementById('root');
const modal = document.getElementById('modal');
modal.addEventListener('click', e => { if (e.target === modal || e.target.closest('[data-close]')) modal.close(); });

const REV = ['ADMIN', 'MANAGER', 'FINANCE', 'COMPLIANCE'];
const ROUTES = [
  [/^#\/dashboard$/, V.dashboard, null, 'Dashboard'],
  [/^#\/requests$/, V.myRequests, null, 'My Requests'],
  [/^#\/requests\/(\d+)$/, V.requestDetail, null, 'Request Record'],
  [/^#\/new$/, V.newRequest, null, 'Create New Request'],
  [/^#\/inbox$/, V.inbox, REV, 'Approvals Queue'],
  [/^#\/all$/, V.allRequests, REV, 'All Tenant Requests'],
  [/^#\/audit$/, V.audit, REV, 'System Audit Trail'],
  [/^#\/workflows$/, V.workflows, ['ADMIN'], 'Workflow Definitions'],
  [/^#\/workflows\/(\d+)$/, V.workflowDetail, ['ADMIN'], 'Workflow Stages & Rules'],
  [/^#\/users$/, V.users, ['ADMIN'], 'Team Directory']
];

const go = h => { if (location.hash === h) route(); else location.hash = h; };
let poll = null;

/* ------------------------------------------------------------ Auth / Login */
const DEMO = [
  { id: 'admin', label: 'Admin', role: 'Full Admin' },
  { id: 'manager', label: 'Manager', role: 'Reviewer' },
  { id: 'finance', label: 'Finance', role: 'Oversight' },
  { id: 'compliance', label: 'Compliance', role: 'Risk Auditor' },
  { id: 'employee', label: 'Employee', role: 'Requester' }
];

function authScreen(kind) {
  clearInterval(poll);
  const login = kind === 'login';
  put(root, html`
    <div class="auth-wrapper">
      <section class="auth-hero">
        <div class="auth-hero-badge">✦ AI-Assisted Workflow Platform</div>
        <h1>WorkNexa<br><span class="highlight">Intelligence & Governance</span></h1>
        <p>AI recommends, rules constrain, and humans remain in control of important decisions. Autonomous operations when safe, guided reviews when required.</p>
        
        <div class="hero-stats-row">
          <div class="hero-stat-box">
            <b>3 layers</b>
            <span>AI · Rules · Humans</span>
          </div>
          <div class="hero-stat-box">
            <b>5 roles</b>
            <span>Access control</span>
          </div>
          <div class="hero-stat-box">
            <b>Full trail</b>
            <span>Every step audited</span>
          </div>
        </div>
      </section>

      <section class="auth-form-side">
        <div class="auth-card-modern">
          <h2>${login ? 'Welcome back' : 'Register organization'}</h2>
          <p class="auth-sub">${login ? 'Sign in to access your organization instance' : 'Configure a new multi-tenant workspace'}</p>
          
          <form id="af">
            ${login ? html`
              <label>Organization (optional)</label>
              <input name="org" autocomplete="organization" placeholder="Acme Corp">
              
              <label>Email Address</label>
              <input name="email" type="email" required autocomplete="username" placeholder="name@acme.com">
              
              <label>Password</label>
              <input name="password" type="password" required autocomplete="current-password" placeholder="••••••••••••">
            ` : html`
              <label>Organization Name</label>
              <input name="org" required minlength="2" maxlength="120" placeholder="Acme Corp">
              
              <label>Admin Name</label>
              <input name="name" required minlength="2" maxlength="120" placeholder="Full name">
              
              <label>Admin Email</label>
              <input name="email" type="email" required placeholder="admin@acme.com">
              
              <label>Password</label>
              <input name="password" type="password" required minlength="6" autocomplete="new-password">
            `}

            <button class="btn-auth-primary" type="submit">${login ? 'Sign In' : 'Create Instance'}</button>
          </form>

          <p class="switch" style="margin-top:1.2rem; text-align:center; font-size:0.85rem; color:#8da29e;">
            ${login ? html`New organization? <a href="#/register">Register instance</a>` : html`Already have an account? <a href="#/login">Sign in</a>`}
          </p>

          ${login ? html`
            <div class="demo-personas-box">
              <span>Quick Demo Access (Acme Corp · Password@123):</span>
              <div class="persona-pills">
                ${DEMO.map(d => html`<button class="persona-chip" type="button" data-demo="${d.id}">${d.label}</button>`)}
              </div>
            </div>
          ` : ''}
        </div>
      </section>
    </div>
  `);

  const f = document.getElementById('af');
  root.querySelectorAll('[data-demo]').forEach(b => b.onclick = () => {
    f.org.value = 'Acme Corp';
    f.email.value = b.dataset.demo + '@acme.com';
    f.password.value = 'Password@123';
    f.querySelector('button[type=submit]').click();
  });

  f.onsubmit = async e => {
    e.preventDefault();
    const btn = f.querySelector('[type=submit]');
    btn.disabled = true;
    btn.textContent = 'Authenticating…';
    try {
      const data = login
        ? await api('/auth/login', { method: 'POST', body: { email: f.email.value.trim(), password: f.password.value, organizationName: f.org.value.trim() || null } })
        : await api('/auth/register-organization', { method: 'POST', body: { organizationName: f.org.value.trim(), adminName: f.name.value.trim(), adminEmail: f.email.value.trim(), adminPassword: f.password.value } });
      session.save(data);
      root.replaceChildren();
      go(login ? '#/dashboard' : '#/workflows');
    } catch (err) {
      toast(err.message, 'bad');
      btn.disabled = false;
      btn.textContent = login ? 'Sign In' : 'Create Instance';
    }
  };
}

/* ------------------------------------------------------------ Shell */
function mountShell() {
  if (document.querySelector('.app-container')) return;
  const u = session.user;
  const isRev = REV.includes(u.role);
  const isAdmin = u.role === 'ADMIN';

  put(root, html`
    <div class="app-container">
      <!-- Dark AeuxGlobal Sidebar -->
      <aside class="sidebar">
        <!-- Logo -->
        <a class="sidebar-logo" href="#/dashboard">
          <div class="logo-icon-bars">
            <span></span><span></span><span></span>
          </div>
          <span>WorkNexa</span>
        </a>

        <!-- Workspace Pill Card -->
        <div class="workspace-card" id="ws-pill" title="Current Tenant">
          <div class="workspace-icon-box">
            ${ICONS.sparkles}
          </div>
          <div class="workspace-info">
            <span class="w-lbl">Workspace</span>
            <span class="w-name">${session.org || 'Production'}</span>
          </div>
          <div class="workspace-dot"></div>
        </div>

        <!-- Search in Sidebar -->
        <div class="sidebar-search">
          <span class="s-icon">${ICONS.search}</span>
          <input id="nav-search" type="search" placeholder="Search record (REQ...)" aria-label="Quick search">
          <span class="s-tag">⌘K</span>
        </div>

        <!-- Navigation Section -->
        <div class="nav-section">
          <div class="nav-section-title">Navigation</div>
          <ul class="nav-list">
            <li class="nav-item">
              <a href="#/dashboard" data-nav="#/dashboard">
                <span class="nav-icon">${ICONS.dashboard}</span>
                <span>Dashboard</span>
              </a>
            </li>
            <li class="nav-item">
              <a href="#/requests" data-nav="#/requests">
                <span class="nav-icon">${ICONS.myRequests}</span>
                <span>My Requests</span>
              </a>
            </li>
            ${isRev ? html`
              <li class="nav-item">
                <a href="#/inbox" data-nav="#/inbox">
                  <span class="nav-icon">${ICONS.inbox}</span>
                  <span>Approvals</span>
                  <span class="nav-badge" id="inbox-badge" hidden>0</span>
                </a>
              </li>
              <li class="nav-item">
                <a href="#/all" data-nav="#/all">
                  <span class="nav-icon">${ICONS.allRequests}</span>
                  <span>All Requests</span>
                </a>
              </li>
              <li class="nav-item">
                <a href="#/audit" data-nav="#/audit">
                  <span class="nav-icon">${ICONS.audit}</span>
                  <span>Audit Trail</span>
                </a>
              </li>
            ` : ''}
          </ul>
        </div>

        <!-- Admin Management Section -->
        ${isAdmin ? html`
          <div class="nav-section">
            <div class="nav-section-title">Platform Admin</div>
            <ul class="nav-list">
              <li class="nav-item">
                <a href="#/workflows" data-nav="#/workflows">
                  <span class="nav-icon">${ICONS.workflows}</span>
                  <span>Workflows & Rules</span>
                </a>
              </li>
              <li class="nav-item">
                <a href="#/users" data-nav="#/users">
                  <span class="nav-icon">${ICONS.users}</span>
                  <span>Team Directory</span>
                </a>
              </li>
            </ul>
          </div>
        ` : ''}

        <!-- Bottom User Profile Card -->
        <div class="sidebar-footer">
          <div class="pop-wrap">
            <button class="user-account-btn" id="av" aria-label="Account menu" aria-expanded="false">
              <div class="user-avatar-circle">${initials(u.name)}</div>
              <div class="user-meta">
                <span class="u-name">${u.name}</span>
                <span class="u-tag">#${u.role.toLowerCase()}</span>
              </div>
            </button>

            <!-- Impersonate & Profile Popover -->
            <div class="pop" id="acct" hidden>
              <div class="who">
                <b>${u.name}</b>
                <span class="muted">${ROLE[u.role] || u.role} · ${session.org}</span>
              </div>
              ${session.org === 'Acme Corp' ? html`
                <div class="impersonate">
                  <span class="sub-label">Switch Persona:</span>
                  <div class="persona-chips">
                    ${DEMO.map(d => html`
                      <button class="chip ${u.email.startsWith(d.id) ? 'active' : ''}" type="button" data-switch="${d.id}">${d.label}</button>
                    `)}
                  </div>
                </div>
              ` : ''}
              <button class="menu-btn" id="logout">Sign out</button>
            </div>
          </div>
        </div>
      </aside>

      <!-- Main Canvas Area -->
      <main class="main-canvas">
        <header class="top-nav-bar">
          <div class="top-title-area">
            <h1 id="page-title">Dashboard</h1>
          </div>
          <div class="top-actions-area">
            <div class="pop-wrap">
              <button class="icon-circle-btn" id="bell" aria-label="Notifications">
                ${ICONS.bell}
                <span class="unread-indicator-dot" id="unread-dot" hidden></span>
              </button>
              <div class="pop" id="notes" hidden></div>
            </div>
            <a class="btn-dark-pill" href="#/new">
              ${ICONS.plus}
              <span>New Request</span>
            </a>
          </div>
        </header>

        <div id="view"></div>
      </main>
    </div>
  `);

  document.getElementById('logout').onclick = () => { session.clear(); clearInterval(poll); root.replaceChildren(); go('#/login'); };

  // Search input jump
  const searchInput = document.getElementById('nav-search');
  searchInput.onkeydown = e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const m = searchInput.value.match(/\d+/);
      if (!m) return toast('Enter request number (e.g. REQ0000007)', 'bad');
      searchInput.value = '';
      go('#/requests/' + Number(m[0]));
    }
  };

  // Persona Switcher
  root.querySelectorAll('[data-switch]').forEach(btn => btn.onclick = async () => {
    const roleId = btn.dataset.switch;
    try {
      const data = await api('/auth/login', {
        method: 'POST',
        body: { email: roleId + '@acme.com', password: 'Password@123', organizationName: 'Acme Corp' }
      });
      session.save(data);
      root.replaceChildren();
      toast('Switched to ' + roleId, 'ok');
      route();
    } catch (e) {
      toast('Could not switch: ' + e.message, 'bad');
    }
  });

  // Popovers
  const pops = [['bell', 'notes'], ['av', 'acct']];
  pops.forEach(([b, p]) => {
    const btn = document.getElementById(b);
    if (!btn) return;
    btn.onclick = async e => {
      e.stopPropagation();
      const box = document.getElementById(p), open = box.hidden;
      pops.forEach(([bb, pp]) => { const el = document.getElementById(pp); if (el) el.hidden = true; });
      box.hidden = !open;
      if (open && p === 'notes') await notes(true);
    };
  });
  document.addEventListener('click', e => {
    if (!e.target.closest('.pop')) pops.forEach(([bb, pp]) => { const x = document.getElementById(pp); if (x) x.hidden = true; });
  });

  notes(false);
  clearInterval(poll);
  poll = setInterval(() => notes(!document.getElementById('notes')?.hidden), 45000);
}

async function notes(render) {
  try {
    const list = await api('/notifications');
    const unread = list.filter(n => !n.read).length;
    const dot = document.getElementById('unread-dot');
    if (dot) dot.hidden = !unread;

    if (REV.includes(session.user?.role)) {
      try {
        const pending = await api('/tasks/pending');
        const badge = document.getElementById('inbox-badge');
        if (badge) {
          badge.hidden = !pending.length;
          badge.textContent = pending.length;
        }
      } catch { /* non-critical */ }
    }

    if (!render) return;
    const box = document.getElementById('notes');
    if (!box) return;
    put(box, list.length ? html`<ul>${list.slice(0, 15).map(n => html`<li><button data-n="${n.id}" data-req="${(n.message.match(/#(\d+)/) || [])[1] || ''}" class="${n.read ? '' : 'unread'}">${n.message}<span class="muted">${ago(n.createdAt)}</span></button></li>`)}</ul>`
      : html`<p class="muted pad">You are all caught up.</p>`);
    box.querySelectorAll('[data-n]').forEach(b => b.onclick = async () => {
      try { await api(`/notifications/${b.dataset.n}/read`, { method: 'PATCH' }); } catch { /* best effort */ }
      box.hidden = true;
      notes(false);
      if (b.dataset.req) go('#/requests/' + b.dataset.req);
    });
  } catch { /* best effort */ }
}

/* ------------------------------------------------------------ Router */
async function route() {
  const hash = location.hash || '#/dashboard';
  if (!session.token) return authScreen(hash === '#/register' ? 'register' : 'login');
  if (['#/login', '#/register', '#/', ''].includes(hash)) return go('#/dashboard');
  
  mountShell();
  const host = document.getElementById('view');
  const titleEl = document.getElementById('page-title');
  const view = document.createElement('div');
  host.replaceChildren(view);

  document.querySelectorAll('[data-nav]').forEach(a => {
    const on = hash === a.dataset.nav || (a.dataset.nav !== '#/dashboard' && hash.startsWith(a.dataset.nav + '/'));
    a.classList.toggle('on', on);
  });

  const hit = ROUTES.map(([re, fn, roles, title]) => [hash.match(re), fn, roles, title]).find(([m]) => m);
  if (!hit) {
    if (titleEl) titleEl.textContent = 'Not Found';
    return put(view, html`<div class="empty"><strong>Page not found</strong><p><a href="#/dashboard">Back to Dashboard</a></p></div>`);
  }

  const [m, fn, roles, title] = hit;
  if (titleEl) titleEl.textContent = title || 'WorkNexa';

  if (roles && !roles.includes(session.user.role)) {
    return put(view, html`<div class="empty"><strong>Access Restricted</strong><p>Your current role does not have permission to view this view.</p></div>`);
  }

  put(view, html`<p class="loading">Loading interface…</p>`);
  try {
    await fn(view, ...m.slice(1));
    window.scrollTo(0, 0);
  } catch (e) {
    put(view, html`<div class="empty"><strong>Could not load content</strong><p>${e.message}</p></div>`);
  }
}

window.addEventListener('hashchange', route);
route();