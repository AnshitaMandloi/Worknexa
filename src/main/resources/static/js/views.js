import { api, session } from './api.js';
import { html, put, ICONS, money, when, ago, toast, openModal, closeModal, confirmBox, statusPill, pill, num, initials, STATUS, TASK, RISK, ROLE, ACTION, OP, EVENT } from './ui.js';

const me = () => session.user;
const REVIEWERS = ['ADMIN', 'MANAGER', 'FINANCE', 'COMPLIANCE'];
export const isReviewer = () => REVIEWERS.includes(me()?.role);
const $ = (root, sel) => root.querySelector(sel);

const head = (title, sub, actions) => html`<header class="page-head"><div><h1>${title}</h1>${sub ? html`<p class="sub">${sub}</p>` : ''}</div><div class="actions">${actions || ''}</div></header>`;
const empty = (title, hint, cta) => html`<div class="empty"><strong>${title}</strong><p>${hint}</p>${cta || ''}</div>`;
const pager = p => p && p.totalPages > 1 ? html`<div class="pager"><button class="btn sm" data-page="${p.page - 1}" ${p.page === 0 ? 'disabled' : ''}>Previous</button><span>Page ${p.page + 1} of ${p.totalPages}</span><button class="btn sm" data-page="${p.page + 1}" ${p.last ? 'disabled' : ''}>Next</button></div>` : '';
const bindPager = (root, fn) => root.querySelectorAll('[data-page]').forEach(b => b.onclick = () => fn(+b.dataset.page));
const busy = async (btn, fn) => { btn.disabled = true; try { return await fn(); } finally { btn.disabled = false; } };
const fail = e => toast(e.message || 'Something went wrong', 'bad');

function requestTable(items, withRequester) {
  if (!items.length) return empty('No requests yet', 'Requests will show up here once they are submitted.');
  return html`<div class="table-wrap"><table class="modern-table">
    <thead><tr><th>Number</th><th>Short description</th>${withRequester ? html`<th>Requester</th>` : ''}<th class="num">Amount</th><th>Status</th><th>Stage</th><th>Submitted</th><th></th></tr></thead>
    <tbody>${items.map(r => html`<tr>
      <td><a class="record-num-link" href="#/requests/${r.id}">${num(r.id)}</a></td><td>${r.title}<br><span class="category-tag">${r.category}</span></td>
      ${withRequester ? html`<td>${r.requesterName || ''}</td>` : ''}
      <td class="num">${money(r.amount)}</td><td>${statusPill(r.status)}</td>
      <td>${r.currentStageName || ''}</td><td title="${when(r.createdAt)}">${ago(r.createdAt)}</td>
      <td class="end"><a class="btn-open-pill" href="#/requests/${r.id}">Open</a></td></tr>`)}</tbody></table></div>`;
}

/* ---------------------------------------------------------------- Home */
function donut(segs, total) {
  let off = 0;
  const arcs = segs.filter(s => s.n).map(s => {
    const pct = s.n / total * 100;
    const a = html`<circle r="15.9155" cx="21" cy="21" fill="none" stroke="${s.color}" stroke-width="5" stroke-dasharray="${pct} ${100 - pct}" stroke-dashoffset="${-off}"><title>${s.label}: ${s.n}</title></circle>`;
    off += pct;
    return a;
  });
  return html`<svg class="donut-svg" viewBox="0 0 42 42" role="img" aria-label="Requests by state"><circle r="15.9155" cx="21" cy="21" fill="none" stroke="#e6ecea" stroke-width="5"/>${arcs}<text x="21" y="22.5" class="dn">${total}</text><text x="21" y="26.5" class="dl">requests</text></svg>`;
}

export async function dashboard(root) {
  const reviewer = isReviewer();
  const [s, recent] = await Promise.all([
    api('/dashboard/stats'),
    api((reviewer ? '/requests' : '/requests/my') + '?size=6&sort=id,desc')
  ]);
  const c = s.statusCounts || {};
  const segs = [['AUTO_APPROVED', 'Auto-approved', '#16a34a'], ['APPROVED', 'Approved', '#6ee7b7'], ['PENDING_REVIEW', 'Awaiting approval', '#f59e0b'], ['INFO_REQUESTED', 'Awaiting information', '#0ea5e9'], ['REJECTED', 'Rejected', '#ef4444']]
    .map(([k, label, color]) => ({ label, color, n: c[k] || 0 }));
  const total = segs.reduce((n, x) => n + x.n, 0);
  const max = Math.max(1, ...segs.map(x => x.n));
  const all = reviewer ? '#/all' : '#/requests';
  const first = reviewer
    ? { label: 'Awaiting your approval', val: s.pendingTasksForRole, sub: 'Open approvals queue →', href: '#/inbox' }
    : { label: 'Awaiting information', val: c.INFO_REQUESTED || 0, sub: 'View my requests →', href: '#/requests' };
  const bars = segs.map(x => html`<span style="height:${Math.max(12, Math.round(x.n / max * 100))}%"></span>`);

  put(root, html`
    <div class="kpi-grid">
      <a class="kpi-card kpi-dark" href="${first.href}"><div class="kpi-top-row"><span class="kpi-label">${first.label}</span><div class="mini-bars">${bars}</div></div>
        <div><div class="kpi-val">${first.val}</div><div class="kpi-sub">${first.sub}</div></div></a>
      <a class="kpi-card kpi-white" href="${all}"><div class="kpi-top-row"><span class="kpi-label">Awaiting approval</span></div>
        <div><div class="kpi-val">${c.PENDING_REVIEW || 0}</div><div class="kpi-sub">In the review chain</div></div></a>
      <a class="kpi-card kpi-white" href="${all}"><div class="kpi-top-row"><span class="kpi-label">Total requests</span></div>
        <div><div class="kpi-val">${s.totalRequests}</div><div class="kpi-sub">Across your organization</div></div></a>
      <div class="kpi-card kpi-sage"><div class="kpi-top-row"><span class="kpi-label">Approved without a human</span><span>${ICONS.trendUp}</span></div>
        <div><div class="kpi-val">${s.automationRatePercentage}%</div><div class="kpi-sub">Automation rate</div></div></div>
    </div>

    <div class="charts-grid">
      <section class="chart-card"><div class="chart-card-header"><h2 style="margin:0">Status breakdown</h2><span class="filter-pill-btn">${total} total</span></div>
        ${total ? html`<div class="bar-rows">${segs.map(x => html`<div class="bar-row"><span class="bar-label">${x.label}</span><div class="bar-track"><i style="width:${x.n / max * 100}%;background:${x.color}"></i></div><b>${x.n}</b></div>`)}</div>`
          : empty('Nothing to show yet', 'Create the first request to see how it is routed.')}
      </section>
      <section class="chart-card"><div class="chart-card-header"><h2 style="margin:0">Requests by state</h2></div>
        ${total ? html`<div class="donut-center-wrap">${donut(segs, total)}<ul class="donut-legend-list">${segs.map(x => html`<li class="donut-legend-item"><i class="legend-swatch" style="background:${x.color}"></i>${x.label}<b>${x.n}</b></li>`)}</ul></div>`
          : empty('No data', 'The chart appears after the first request.')}
      </section>
    </div>

    <section class="table-card"><div class="table-card-header"><h2>${reviewer ? 'Latest requests' : 'Your latest requests'}</h2><a class="btn-open-pill" href="${all}">View all</a></div>
      ${requestTable(recent.content || [], reviewer)}</section>`);
}

/* ---------------------------------------------------------------- Request lists */
const SORT = {
  id: r => r.id, title: r => (r.title || '').toLowerCase(), requester: r => (r.requesterName || '').toLowerCase(),
  state: r => (STATUS[r.status] || [r.status])[0], amount: r => Number(r.amount), stage: r => r.currentStageName || '', created: r => new Date(r.createdAt).getTime()
};

function requestList(root, { title, sub, p, withRequester, goPage, actions }) {
  let key = 'id', dir = -1, q = '', st = '';
  const cols = [['id', 'Number'], ['title', 'Short description'], ...(withRequester ? [['requester', 'Requester']] : []), ['state', 'State'], ['amount', 'Amount', 'num'], ['stage', 'Stage'], ['created', 'Created']];
  put(root, html`${head(title, sub, actions)}
    <section class="table-card">
      <div class="toolbar">
        <select id="st" aria-label="Filter by state"><option value="">All states</option>${Object.entries(STATUS).map(([k, [l]]) => html`<option value="${k}">${l}</option>`)}</select>
        <input id="q" type="search" placeholder="Search this page" aria-label="Search this page">
        <span class="count" id="cnt"></span>
      </div>
      <div class="table-wrap" id="tbl"></div>${pager(p)}
    </section>`);
  const draw = () => {
    const rows = p.content.filter(r => (!st || r.status === st) && (!q || (num(r.id) + ' ' + r.title + ' ' + (r.requesterName || '')).toLowerCase().includes(q)));
    rows.sort((a, b) => { const x = SORT[key](a), y = SORT[key](b); return (x > y ? 1 : x < y ? -1 : 0) * dir; });
    $(root, '#cnt').textContent = `${rows.length} of ${p.totalElements} ${p.totalElements === 1 ? 'record' : 'records'}`;
    put($(root, '#tbl'), rows.length ? html`<table class="modern-table"><thead><tr>${cols.map(([k, l, c]) => html`<th class="${c || ''} sortable" data-sort="${k}" aria-sort="${key === k ? (dir > 0 ? 'ascending' : 'descending') : 'none'}"><button>${l}${key === k ? (dir > 0 ? ' ▲' : ' ▼') : ''}</button></th>`)}</tr></thead>
      <tbody>${rows.map(r => html`<tr><td><a class="record-num-link" href="#/requests/${r.id}">${num(r.id)}</a></td><td>${r.title}<br><span class="category-tag">${r.category}</span></td>
        ${withRequester ? html`<td>${r.requesterName || ''}</td>` : ''}<td>${statusPill(r.status)}</td><td class="num">${money(r.amount)}</td><td>${r.currentStageName || ''}</td><td title="${when(r.createdAt)}">${ago(r.createdAt)}</td></tr>`)}</tbody></table>`
      : empty('No records to display', 'Try clearing the filters.'));
    root.querySelectorAll('[data-sort]').forEach(th => th.onclick = () => { dir = key === th.dataset.sort ? -dir : 1; key = th.dataset.sort; draw(); });
  };
  $(root, '#st').onchange = e => { st = e.target.value; draw(); };
  $(root, '#q').oninput = e => { q = e.target.value.trim().toLowerCase(); draw(); };
  draw();
  bindPager(root, goPage);
}

export async function myRequests(root, page = 0) {
  const p = await api(`/requests/my?page=${page}&size=15&sort=id,desc`);
  requestList(root, { title: 'My requests', sub: 'Everything you have submitted.', p, withRequester: false, goPage: n => myRequests(root, n), actions: html`<a class="btn primary" href="#/new">Create new request</a>` });
}
export async function allRequests(root, page = 0) {
  const p = await api(`/requests?page=${page}&size=15&sort=id,desc`);
  requestList(root, { title: 'All requests', sub: 'Every request in your organization.', p, withRequester: true, goPage: n => allRequests(root, n) });
}

/* ---------------------------------------------------------------- New request */
export async function newRequest(root) {
  let types = [];
  try { types = [...new Set((await api('/workflows')).filter(w => w.active).map(w => w.requestType))]; } catch { /* handled below */ }
  if (!types.length) {
    put(root, html`${head('Create new request')}<section class="panel">${empty('No workflow is set up yet',
      me().role === 'ADMIN' ? 'Create a workflow first so requests know where to go.' : 'Ask your admin to set up a workflow for your organization.',
      me().role === 'ADMIN' ? html`<a class="btn primary" href="#/workflows">Set up a workflow</a>` : '')}</section>`);
    return;
  }
  put(root, html`${head('Create new request', 'Rules and the AI risk check decide whether it is approved straight away or goes to a reviewer.')}
    <form class="panel form" id="f">
      <div class="frow"><label>Requested by</label><div class="ro">${me().name}</div></div>
      <div class="frow"><label class="req" for="t">Short description</label><input id="t" name="title" required minlength="3" maxlength="150" placeholder="e.g. Client dinner, Pune"></div>
      <div class="frow"><label for="c">Category</label><select id="c" name="category">${types.map(t => html`<option>${t}</option>`)}</select></div>
      <div class="frow"><label class="req" for="a">Amount (₹)</label><input id="a" name="amount" type="number" min="0.01" step="0.01" required></div>
      <div class="frow"><label for="d">Description</label><textarea id="d" name="description" rows="4" maxlength="1000" placeholder="What is this for?"></textarea></div>
      <div class="frow"><label for="fl">Attachment</label><div><input id="fl" name="file" type="file"><span class="hint">Only the file name is recorded for now. Requests without a document are routed to manual review.</span></div></div>
      <div class="row end"><a class="btn" href="#/dashboard">Cancel</a><button class="btn primary" type="submit">Submit</button></div>
    </form>`);
  $(root, '#f').onsubmit = async e => {
    e.preventDefault();
    const f = e.target, file = f.file.files[0];
    await busy($(f, 'button[type=submit]'), async () => {
      try {
        const r = await api('/requests', { method: 'POST', body: { title: f.title.value.trim(), amount: Number(f.amount.value), category: f.category.value, description: f.description.value.trim(), documentAttached: !!file, documentName: file ? file.name : null } });
        toast({ AUTO_APPROVED: 'Approved automatically', PENDING_REVIEW: 'Sent for approval', REJECTED: 'Rejected by a rule' }[r.status] || 'Request submitted');
        location.hash = '#/requests/' + r.id;
      } catch (err) { fail(err); }
    });
  };
}

/* ---------------------------------------------------------------- Request detail */
function actionableTask(r, tasks) {
  if (r.status !== 'PENDING_REVIEW' || r.requesterId === me().id) return null;
  const next = tasks.find(t => t.status !== 'APPROVED');
  if (!next || next.status !== 'PENDING') return null;
  return (me().role === next.assignedRole || me().role === 'ADMIN') ? next : null;
}
const field = (label, value, full) => html`<div class="field-item ${full ? 'full' : ''}"><label>${label}</label><div class="field-box">${value}</div></div>`;
const PATH = ['Submitted', 'AI analysis', 'Review', 'Completed'];
const PATH_AT = { SUBMITTED: 0, AI_ANALYZING: 1, PENDING_REVIEW: 2, INFO_REQUESTED: 2, APPROVED: 3, AUTO_APPROVED: 3, REJECTED: 3 };

export async function requestDetail(root, id) {
  const d = await api('/requests/' + id);
  const r = d.request, ai = d.aiAnalysis;
  const tasks = [...(d.tasks || [])].sort((a, b) => a.sequenceNo - b.sequenceNo);
  const trail = [...(d.timeline || [])].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt) || a.id - b.id);
  const live = tasks.filter(t => t.status !== 'CANCELLED');
  const approved = live.filter(t => t.status === 'APPROVED').length;
  const matched = trail.filter(e => e.eventType === 'RULE_TRIGGERED').length;
  const why = (trail.filter(e => e.eventType === 'DECISION_MADE').pop()?.details || '').replace(/^Outcome: \w+\. Explanation: /, '');
  const task = actionableTask(r, tasks);
  const askedInfo = tasks.find(t => t.status === 'INFO_REQUESTED' || (r.status === 'INFO_REQUESTED' && t.comment));
  const humanLine = !live.length ? (r.status === 'AUTO_APPROVED' ? 'No review needed' : r.status === 'REJECTED' ? 'Stopped by a rule' : 'No reviewers assigned')
    : `${approved} of ${live.length} reviewers approved`;
  const at = PATH_AT[r.status] ?? 0, final = at === 3;
  const stepClass = i => i < at ? 'done' : i === at ? (final ? (r.status === 'REJECTED' ? 'bad' : 'okfinal') : 'now') : '';

  put(root, html`
    <a class="back" href="#/${isReviewer() ? 'all' : 'requests'}">Requests</a>
    ${head(`${num(r.id)} — ${r.title}`, `Opened by ${r.requesterName || 'unknown'} · ${when(r.createdAt)}`, html`<div class="row">${task ? html`<button class="btn good sm" onclick="document.getElementById('decide')?.scrollIntoView({behavior:'smooth'});document.getElementById('cmt')?.focus();">Take Action</button>` : ''}${statusPill(r.status)}</div>`)}
    <div class="stepper-flow" aria-label="Progress">${PATH.map((n, i) => html`<div class="stepper-step ${stepClass(i)}" ${i === at ? html`aria-current="step"` : ''}><span class="stepper-num">${i < at || (final && i === at && r.status !== 'REJECTED') ? ICONS.check : i + 1}</span><div class="stepper-text"><b>${i === 3 && final ? STATUS[r.status][0] : n}</b><span>${i < at ? 'Done' : i === at ? (final ? 'Final state' : 'In progress') : 'Waiting'}</span></div></div>`)}</div>

    ${task ? html`<section class="action-banner" id="decide"><h2>Your approval is required</h2>
      <label>Comment<textarea rows="3" id="cmt" placeholder="Required when rejecting or asking for more information"></textarea></label>
      <div class="row"><button class="btn good" data-d="APPROVE">Approve</button><button class="btn" data-d="REQUEST_INFO">Ask for information</button><button class="btn danger" data-d="REJECT">Reject</button></div></section>` : ''}
    ${r.status === 'INFO_REQUESTED' && r.requesterId === me().id ? html`<section class="action-banner" id="respond"><h2>A reviewer needs more information</h2>
      ${askedInfo?.comment ? html`<p class="ask">${askedInfo.comment}</p>` : ''}
      <label>Your reply<textarea rows="3" id="reply" required></textarea></label>
      <label>Attach a document<input type="file" id="rfile"></label>
      <div class="row"><button class="btn primary" id="send">Send reply</button></div></section>` : ''}

    <section class="verdict-grid" aria-label="How this request was decided">
      <div class="verdict-box"><span class="v-badge">AI recommends</span><strong>${ai ? `${RISK[ai.riskLevel]?.[0] || ai.riskLevel} · ${ai.confidence}%` : 'Not analysed'}</strong><span class="muted">${ai?.recommendation || ''}</span></div>
      <div class="verdict-box"><span class="v-badge">Rules constrain</span><strong>${matched} ${matched === 1 ? 'rule' : 'rules'} matched</strong><span class="muted">${why}</span></div>
      <div class="verdict-box"><span class="v-badge">Humans decide</span><strong>${humanLine}</strong><span class="muted">${live.map(t => ROLE[t.assignedRole]).join(' → ')}</span></div>
    </section>

    <section class="form-fields-card"><div class="fields-grid">
      ${field('Number', num(r.id))}${field('State', statusPill(r.status))}
      ${field('Requester', r.requesterName || '')}${field('Category', r.category)}
      ${field('Amount', money(r.amount))}${field('Current stage', r.currentStageName || '—')}
      ${field('Document', r.documentAttached ? (r.documentName || 'Attached') : 'None attached')}${field('Opened', when(r.createdAt))}
      ${r.description ? field('Description', r.description, true) : ''}
    </div></section>

    <section class="panel">
      <div class="tabs" role="tablist">
        <button role="tab" data-tab="act" aria-selected="true">Activity</button>
        <button role="tab" data-tab="appr" aria-selected="false">Approvals (${tasks.length})</button>
        ${ai ? html`<button role="tab" data-tab="ai" aria-selected="false">AI assessment</button>` : ''}
      </div>
      <div data-pane="act"><ol class="stream">${trail.map(e => { const [label, kind] = EVENT[e.eventType] || [e.eventType, 'sys']; return html`
        <li class="${kind}"><span class="av">${initials(e.actorName)}</span><div><div class="hd"><b>${e.actorName || 'System'}</b><span class="what">${label}</span><time>${when(e.createdAt)}</time></div>${e.details ? html`<p>${e.details}</p>` : ''}</div></li>`; })}</ol></div>
      <div data-pane="appr" hidden>${tasks.length ? html`<div class="table-wrap"><table class="modern-table"><thead><tr><th>Order</th><th>Approver</th><th>State</th><th>Decided by</th><th>Comment</th></tr></thead>
        <tbody>${tasks.map(t => html`<tr><td>${t.sequenceNo}</td><td>${ROLE[t.assignedRole]}</td><td>${pill(TASK[t.status] || [t.status, 'mute'])}</td><td>${t.decidedByName || ''}</td><td class="wrap">${t.comment || ''}</td></tr>`)}</tbody></table></div>`
        : html`<p class="muted">No human review was needed for this request.</p>`}</div>
      ${ai ? html`<div data-pane="ai" hidden><div class="row">${pill(RISK[ai.riskLevel] || [ai.riskLevel, 'mute'])}<span class="muted">${ai.confidence}% confidence</span></div>
        <div class="meter"><span style="width:${ai.confidence}%"></span></div>
        <p>${ai.recommendation || ''}</p><ul class="reasons">${(ai.reasons || []).map(x => html`<li>${x}</li>`)}</ul>
        <p class="note">The AI only recommends. Rules and reviewers make the final call.</p></div>` : ''}
    </section>`);

  root.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => {
    root.querySelectorAll('[data-tab]').forEach(x => x.setAttribute('aria-selected', String(x === b)));
    root.querySelectorAll('[data-pane]').forEach(p => p.hidden = p.dataset.pane !== b.dataset.tab);
  });
  root.querySelectorAll('[data-d]').forEach(b => b.onclick = () => {
    const decision = b.dataset.d, comment = $(root, '#cmt').value.trim();
    if (decision !== 'APPROVE' && !comment) return toast('Add a comment so the requester knows why.', 'bad');
    busy(b, async () => {
      try {
        await api(`/tasks/${task.id}/decision`, { method: 'POST', body: { decision, comment } });
        toast({ APPROVE: 'Approved', REJECT: 'Rejected', REQUEST_INFO: 'Information requested' }[decision]);
        await requestDetail(root, id);
      } catch (e) { fail(e); }
    });
  });
  const send = $(root, '#send');
  if (send) send.onclick = () => {
    const comment = $(root, '#reply').value.trim(), file = $(root, '#rfile').files[0];
    if (!comment) return toast('Write a short reply first.', 'bad');
    busy(send, async () => {
      try {
        await api(`/requests/${id}/respond-info`, { method: 'POST', body: { comment, documentAttached: file ? true : null, documentName: file ? file.name : null } });
        toast('Reply sent');
        await requestDetail(root, id);
      } catch (e) { fail(e); }
    });
  };
}

/* ---------------------------------------------------------------- Approval inbox */
export async function inbox(root) {
  const items = await api('/tasks/pending');
  put(root, html`${head('My approvals', 'Requests waiting for your decision.')}
    <section class="table-card"><div class="toolbar"><span class="count">${items.length} ${items.length === 1 ? 'record' : 'records'}</span></div>
    ${items.length ? html`<div class="table-wrap"><table class="modern-table">
      <thead><tr><th>Approval for</th><th>Requester</th><th class="num">Amount</th><th>Document</th><th>Step</th><th>Created</th><th></th></tr></thead>
      <tbody>${items.map(t => html`<tr><td><a class="record-num-link" href="#/requests/${t.requestId}">${num(t.requestId)}</a><span class="muted block">${t.requestTitle}</span></td><td>${t.requesterName}</td>
        <td class="num">${money(t.amount)}</td><td>${t.documentAttached ? (t.documentName || 'Attached') : html`<span class="pill bad">Missing</span>`}</td>
        <td>${ROLE[t.assignedRole]} · step ${t.sequenceNo}</td><td>${ago(t.createdAt)}</td><td class="end"><a class="btn-open-pill" href="#/requests/${t.requestId}">Open</a></td></tr>`)}</tbody></table></div>`
      : empty('Nothing awaiting your approval', 'New tasks appear here as soon as a request needs your role.')}</section>`);
}

/* ---------------------------------------------------------------- Workflows & rules (admin) */
const STARTER_RULES = [
  { name: 'Missing Receipt Manual Audit', field: 'documentAttached', operator: 'EQ', value: 'false', action: 'MANUAL_REVIEW', priority: 5, mandatory: true },
  { name: 'Low Value Auto-Approval', field: 'amount', operator: 'LTE', value: '10000', action: 'AUTO_APPROVE', priority: 10, mandatory: false },
  { name: 'High AI Risk Compliance Review', field: 'riskLevel', operator: 'EQ', value: 'HIGH', action: 'COMPLIANCE_REVIEW', priority: 15, mandatory: true },
  { name: 'Mid Value Manager Review', field: 'amount', operator: 'BETWEEN', value: '10000', valueMax: '50000', action: 'MANAGER_REVIEW', priority: 20, mandatory: false },
  { name: 'High Value Finance Oversight', field: 'amount', operator: 'GT', value: '50000', action: 'FINANCE_REVIEW', priority: 30, mandatory: true }
];
async function createStarter() {
  const wf = await api('/workflows', { method: 'POST', body: { name: 'Expense Approval', requestType: 'EXPENSE', description: 'Low-value expenses are approved automatically; the rest go to reviewers.' } });
  const stages = [['Request Submission', 'SUBMISSION', null], ['AI Risk Analysis', 'AI_ANALYSIS', null], ['Managerial Review', 'HUMAN_REVIEW', 'MANAGER'], ['Process Completion', 'COMPLETION', null]];
  for (const [i, [name, stageType, assignedRole]] of stages.entries()) await api(`/workflows/${wf.id}/stages`, { method: 'POST', body: { name, stageType, assignedRole, orderIndex: i + 1 } });
  for (const rule of STARTER_RULES) await api(`/workflows/${wf.id}/rules`, { method: 'POST', body: { ...rule, active: true } });
  return wf;
}

export async function workflows(root) {
  const list = await api('/workflows');
  put(root, html`${head('Workflows', 'Each workflow defines the stages and rules for one type of request.')}
    <section class="panel">${list.length ? html`<div class="table-wrap"><table><thead><tr><th>Name</th><th>Request type</th><th>Status</th><th></th></tr></thead>
      <tbody>${list.map(w => html`<tr><td>${w.name}<span class="muted block">${w.description || ''}</span></td><td>${w.requestType}</td>
        <td>${w.active ? pill(['Active', 'ok']) : pill(['Inactive', 'mute'])}</td><td class="end"><a class="btn sm" href="#/workflows/${w.id}">Stages and rules</a></td></tr>`)}</tbody></table></div>`
      : empty('No workflows yet', 'Start from the expense template, or create your own below.', html`<button class="btn primary" id="starter">Create expense workflow</button>`)}</section>
    <form class="panel form" id="nf"><h2>New workflow</h2>
      <div class="grid2"><label>Name<input name="name" required minlength="2" placeholder="Leave approval"></label>
      <label>Request type<input name="requestType" required minlength="2" placeholder="LEAVE"></label></div>
      <label>Description<input name="description" maxlength="500"></label>
      <div class="row end"><button class="btn primary" type="submit">Create workflow</button></div></form>`);
  const st = $(root, '#starter');
  if (st) st.onclick = () => busy(st, async () => { try { const w = await createStarter(); toast('Expense workflow created'); location.hash = '#/workflows/' + w.id; } catch (e) { fail(e); } });
  $(root, '#nf').onsubmit = async e => {
    e.preventDefault();
    const f = e.target;
    await busy($(f, 'button'), async () => {
      try {
        const w = await api('/workflows', { method: 'POST', body: { name: f.name.value.trim(), requestType: f.requestType.value.trim().toUpperCase(), description: f.description.value.trim() } });
        toast('Workflow created'); location.hash = '#/workflows/' + w.id;
      } catch (err) { fail(err); }
    });
  };
}

const opt = (list, sel) => Object.entries(list).map(([k, v]) => html`<option value="${k}" ${k === sel ? 'selected' : ''}>${v}</option>`);
const ruleText = r => r.operator === 'BETWEEN' ? `${r.field} between ${r.value} and ${r.valueMax}` : `${r.field} ${OP[r.operator]} ${r.value}`;

export async function workflowDetail(root, id) {
  const [w, stages, rules] = await Promise.all([api('/workflows/' + id), api(`/workflows/${id}/stages`), api(`/workflows/${id}/rules`)]);
  stages.sort((a, b) => a.orderIndex - b.orderIndex);
  rules.sort((a, b) => a.priority - b.priority);
  put(root, html`<a class="back" href="#/workflows">Back to workflows</a>
    ${head(w.name, `Request type ${w.requestType}`, html`<button class="btn danger" id="delwf">Delete workflow</button>`)}
    <section class="panel"><h2>Stages</h2>
      ${stages.length ? html`<ol class="stages">${stages.map(s => html`<li><b>${s.name}</b><span class="muted">${s.stageType.replace('_', ' ').toLowerCase()}${s.assignedRole ? ' · ' + ROLE[s.assignedRole] : ''}</span><button class="btn sm" data-ds="${s.id}" aria-label="Delete stage ${s.name}">Remove</button></li>`)}</ol>` : empty('No stages', 'Add at least a submission, AI analysis, human review and completion stage.')}
      <form class="inline" id="sf"><input name="name" placeholder="Stage name" required minlength="2">
        <select name="stageType">${opt({ SUBMISSION: 'Submission', AI_ANALYSIS: 'AI analysis', HUMAN_REVIEW: 'Human review', COMPLETION: 'Completion' })}</select>
        <select name="assignedRole"><option value="">No role</option>${opt({ MANAGER: 'Manager', FINANCE: 'Finance', COMPLIANCE: 'Compliance', ADMIN: 'Admin' })}</select>
        <input name="orderIndex" type="number" min="1" value="${stages.length + 1}" aria-label="Order" required><button class="btn primary" type="submit">Add stage</button></form>
    </section>
    <section class="panel"><div class="row between"><h2>Business rules</h2><button class="btn primary" id="addrule">Add rule</button></div>
      ${rules.length ? html`<div class="table-wrap"><table><thead><tr><th>Priority</th><th>Rule</th><th>When</th><th>Then</th><th></th></tr></thead>
        <tbody>${rules.map(r => html`<tr class="${r.active ? '' : 'off'}"><td>${r.priority}</td><td>${r.name}${r.active ? '' : html` ${pill(['Off', 'mute'])}`}</td><td><code>${ruleText(r)}</code></td>
          <td>${ACTION[r.action]}${r.mandatory ? html` ${pill(['Mandatory', 'wait'])}` : ''}</td>
          <td class="end"><button class="btn sm" data-er="${r.id}">Edit</button> <button class="btn sm" data-dr="${r.id}">Delete</button></td></tr>`)}</tbody></table></div>`
        : empty('No rules', 'Without rules every request is routed by the AI risk level alone.')}
      <p class="note">Mandatory rules always override the AI recommendation.</p></section>`);

  $(root, '#delwf').onclick = async () => { if (await confirmBox(`Delete "${w.name}" and its stages and rules?`)) try { await api('/workflows/' + id, { method: 'DELETE' }); toast('Workflow deleted'); location.hash = '#/workflows'; } catch (e) { fail(e); } };
  $(root, '#sf').onsubmit = async e => {
    e.preventDefault(); const f = e.target;
    await busy($(f, 'button'), async () => { try { await api(`/workflows/${id}/stages`, { method: 'POST', body: { name: f.name.value.trim(), stageType: f.stageType.value, assignedRole: f.assignedRole.value || null, orderIndex: Number(f.orderIndex.value) } }); toast('Stage added'); await workflowDetail(root, id); } catch (err) { fail(err); } });
  };
  root.querySelectorAll('[data-ds]').forEach(b => b.onclick = async () => { try { await api(`/workflows/${id}/stages/${b.dataset.ds}`, { method: 'DELETE' }); toast('Stage removed'); await workflowDetail(root, id); } catch (e) { fail(e); } });
  root.querySelectorAll('[data-dr]').forEach(b => b.onclick = async () => { if (await confirmBox('Delete this rule?')) try { await api(`/workflows/${id}/rules/${b.dataset.dr}`, { method: 'DELETE' }); toast('Rule deleted'); await workflowDetail(root, id); } catch (e) { fail(e); } });
  $(root, '#addrule').onclick = () => ruleModal(id, null, () => workflowDetail(root, id));
  root.querySelectorAll('[data-er]').forEach(b => b.onclick = () => ruleModal(id, rules.find(r => r.id === +b.dataset.er), () => workflowDetail(root, id)));
}

function ruleModal(workflowId, rule, done) {
  const r = rule || { field: 'amount', operator: 'LTE', action: 'MANAGER_REVIEW', priority: 50, mandatory: false, active: true };
  openModal(html`<h2>${rule ? 'Edit rule' : 'Add rule'}</h2>
    <form class="form" id="rf">
      <label>Name<input name="name" required minlength="2" value="${r.name || ''}"></label>
      <div class="grid3"><label>Field<select name="field">${opt({ amount: 'Amount', documentAttached: 'Document attached', riskLevel: 'AI risk level', riskScore: 'AI confidence' }, r.field)}</select></label>
        <label>Operator<select name="operator">${opt(OP, r.operator)}</select></label>
        <label>Value<input name="value" required value="${r.value || ''}" placeholder="10000, true, HIGH"></label></div>
      <label id="max">Upper value (for between)<input name="valueMax" value="${r.valueMax || ''}"></label>
      <div class="grid2"><label>Then<select name="action">${opt(ACTION, r.action)}</select></label>
        <label>Priority<input name="priority" type="number" required value="${r.priority}"></label></div>
      <div class="row"><label class="check"><input type="checkbox" name="mandatory" ${r.mandatory ? 'checked' : ''}>Mandatory (overrides the AI)</label>
        <label class="check"><input type="checkbox" name="active" ${r.active ? 'checked' : ''}>Active</label></div>
      <div class="row end"><button class="btn" type="button" data-close>Cancel</button><button class="btn primary" type="submit">Save rule</button></div></form>`, d => {
    const f = $(d, '#rf'), max = $(d, '#max');
    const sync = () => max.hidden = f.operator.value !== 'BETWEEN';
    f.operator.onchange = sync; sync();
    f.onsubmit = async e => {
      e.preventDefault();
      const body = { name: f.name.value.trim(), field: f.field.value, operator: f.operator.value, value: f.value.value.trim(), valueMax: f.operator.value === 'BETWEEN' ? f.valueMax.value.trim() : null, action: f.action.value, priority: Number(f.priority.value), mandatory: f.mandatory.checked, active: f.active.checked };
      await busy($(f, '[type=submit]'), async () => {
        try { await api(`/workflows/${workflowId}/rules${rule ? '/' + rule.id : ''}`, { method: rule ? 'PUT' : 'POST', body }); closeModal(); toast('Rule saved'); done(); } catch (err) { fail(err); }
      });
    };
  });
}

/* ---------------------------------------------------------------- Users (admin) */
export async function users(root, page = 0) {
  const p = await api(`/users?page=${page}&size=20&sort=id`);
  put(root, html`${head('People', 'Add teammates and choose what they can do.', html`<button class="btn primary" id="adduser">Add person</button>`)}
    <section class="panel"><div class="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
      <tbody>${p.content.map(u => html`<tr><td>${u.name}</td><td>${u.email}</td><td>${ROLE[u.role]}</td><td>${u.active ? pill(['Active', 'ok']) : pill(['Deactivated', 'mute'])}</td>
        <td class="end"><button class="btn sm" data-eu="${u.id}">Edit</button> ${u.id === me().id ? '' : html`<button class="btn sm" data-tu="${u.id}">${u.active ? 'Deactivate' : 'Activate'}</button>`}</td></tr>`)}</tbody></table></div>${pager(p)}</section>`);
  bindPager(root, n => users(root, n));
  const reload = () => users(root, page);
  $(root, '#adduser').onclick = () => openModal(html`<h2>Add person</h2><form class="form" id="uf">
    <label>Name<input name="name" required minlength="2"></label><label>Email<input name="email" type="email" required></label>
    <label>Temporary password<input name="password" type="password" required minlength="6"></label>
    <label>Role<select name="role">${opt(ROLE, 'EMPLOYEE')}</select></label>
    <div class="row end"><button class="btn" type="button" data-close>Cancel</button><button class="btn primary" type="submit">Add person</button></div></form>`, d => {
    const f = $(d, '#uf');
    f.onsubmit = async e => { e.preventDefault(); await busy($(f, '[type=submit]'), async () => { try { await api('/users', { method: 'POST', body: { name: f.name.value.trim(), email: f.email.value.trim(), password: f.password.value, role: f.role.value } }); closeModal(); toast('Person added'); reload(); } catch (err) { fail(err); } }); };
  });
  root.querySelectorAll('[data-eu]').forEach(b => b.onclick = () => {
    const u = p.content.find(x => x.id === +b.dataset.eu);
    openModal(html`<h2>Edit ${u.name}</h2><form class="form" id="uf"><label>Name<input name="name" required minlength="2" value="${u.name}"></label>
      <label>Role<select name="role">${opt(ROLE, u.role)}</select></label>
      <div class="row end"><button class="btn" type="button" data-close>Cancel</button><button class="btn primary" type="submit">Save</button></div></form>`, d => {
      const f = $(d, '#uf');
      f.onsubmit = async e => { e.preventDefault(); try { await api('/users/' + u.id, { method: 'PUT', body: { name: f.name.value.trim(), role: f.role.value } }); closeModal(); toast('Saved'); reload(); } catch (err) { fail(err); } };
    });
  });
  root.querySelectorAll('[data-tu]').forEach(b => b.onclick = async () => {
    const u = p.content.find(x => x.id === +b.dataset.tu);
    try { await api(`/users/${u.id}/status`, { method: 'PATCH', body: { active: !u.active } }); toast(u.active ? 'Deactivated' : 'Activated'); reload(); } catch (e) { fail(e); }
  });
}

/* ---------------------------------------------------------------- Audit log */
export async function audit(root, page = 0, requestId = '') {
  const p = await api(`/audit?page=${page}&size=30&sort=id,desc${requestId ? '&requestId=' + encodeURIComponent(requestId) : ''}`);
  put(root, html`${head('Audit log', 'Every important action, in order.')}
    <section class="panel"><form class="inline" id="af"><input name="rid" type="number" min="1" placeholder="Filter by request number" value="${requestId}"><button class="btn" type="submit">Filter</button>${requestId ? html`<a href="#/audit">Clear</a>` : ''}</form>
      ${p.content.length ? html`<div class="table-wrap"><table><thead><tr><th>When</th><th>Event</th><th>Request</th><th>By</th><th>Details</th></tr></thead>
        <tbody>${p.content.map(e => html`<tr><td title="${when(e.createdAt)}">${ago(e.createdAt)}</td><td>${(EVENT[e.eventType] || [e.eventType])[0]}</td>
          <td><a href="#/requests/${e.requestId}">#${e.requestId}</a></td><td>${e.actorName || 'System'}</td><td class="wrap">${e.details}</td></tr>`)}</tbody></table></div>`
        : empty('No events', 'Nothing matches that filter.')}${pager(p)}</section>`);
  bindPager(root, n => audit(root, n, requestId));
  $(root, '#af').onsubmit = e => { e.preventDefault(); audit(root, 0, e.target.rid.value); };
}