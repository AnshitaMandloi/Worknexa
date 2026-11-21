// Tiny templating layer: html`...` escapes every interpolation unless it is already trusted markup.
const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
export const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ESC[c]);
class Raw { constructor(s) { this.s = s; } }
export const raw = s => new Raw(String(s));
const part = v => v instanceof Raw ? v.s : Array.isArray(v) ? v.map(part).join('') : (v == null || v === false) ? '' : esc(v);
export const html = (strs, ...vals) => new Raw(strs.reduce((o, s, i) => o + s + (i < vals.length ? part(vals[i]) : ''), ''));
export const put = (el, content) => { el.innerHTML = content instanceof Raw ? content.s : esc(content); };

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
export const money = n => inr.format(Number(n ?? 0));
export const when = iso => iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';
export function ago(iso) {
  const m = Math.round((Date.now() - new Date(iso)) / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return m + 'm ago';
  if (m < 1440) return Math.round(m / 60) + 'h ago';
  return Math.round(m / 1440) + 'd ago';
}

export const ICONS = {
  dashboard: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9" rx="2"></rect><rect x="14" y="3" width="7" height="5" rx="2"></rect><rect x="14" y="12" width="7" height="9" rx="2"></rect><rect x="3" y="16" width="7" height="5" rx="2"></rect></svg>`),
  myRequests: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>`),
  inbox: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 16 12 14 15 10 15 8 12 2 12"></polyline><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"></path></svg>`),
  allRequests: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>`),
  audit: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>`),
  workflows: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="6" y1="3" x2="6" y2="15"></line><circle cx="18" cy="6" r="3"></circle><circle cx="6" cy="18" r="3"></circle><path d="M18 9a9 9 0 0 1-9 9"></path></svg>`),
  users: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`),
  plus: raw(`<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>`),
  bell: raw(`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>`),
  search: raw(`<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>`),
  sparkles: raw(`<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"></path></svg>`),
  check: raw(`<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`),
  trendUp: raw(`<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>`)
};

export const STATUS = {
  SUBMITTED: ['Submitted', 'info'], AI_ANALYZING: ['AI analysing', 'info'], PENDING_REVIEW: ['Awaiting approval', 'wait'],
  INFO_REQUESTED: ['Awaiting info', 'info'], APPROVED: ['Approved', 'ok'], AUTO_APPROVED: ['Auto-approved', 'ok'], REJECTED: ['Rejected', 'bad']
};
export const TASK = { PENDING: ['Pending', 'wait'], APPROVED: ['Approved', 'ok'], REJECTED: ['Rejected', 'bad'], INFO_REQUESTED: ['Info requested', 'info'], CANCELLED: ['Cancelled', 'mute'] };
export const RISK = { LOW: ['Low risk', 'ok'], MEDIUM: ['Medium risk', 'wait'], HIGH: ['High risk', 'bad'] };
export const ROLE = { ADMIN: 'Admin', EMPLOYEE: 'Employee', MANAGER: 'Manager', FINANCE: 'Finance', COMPLIANCE: 'Compliance' };
export const ACTION = { AUTO_APPROVE: 'Auto-approve', MANAGER_REVIEW: 'Manager review', FINANCE_REVIEW: 'Finance review', COMPLIANCE_REVIEW: 'Compliance review', MANUAL_REVIEW: 'Manual review', REJECT: 'Reject' };
export const OP = { LTE: '≤', LT: '<', GT: '>', GTE: '≥', EQ: '=', NEQ: '≠', BETWEEN: 'between' };
export const EVENT = {
  REQUEST_CREATED: ['Request submitted', 'sys'], AI_ANALYSIS_STARTED: ['AI analysis started', 'ai'], AI_ANALYSIS_COMPLETED: ['AI analysis finished', 'ai'],
  AI_ANALYSIS_FAILED: ['AI analysis failed', 'ai'], RULE_TRIGGERED: ['Rule matched', 'rule'], DECISION_MADE: ['Routing decided', 'rule'],
  TASK_ASSIGNED: ['Sent to reviewer', 'sys'], TASK_APPROVED: ['Reviewer approved', 'human'], TASK_REJECTED: ['Reviewer rejected', 'human'],
  INFO_REQUESTED: ['More information requested', 'human'], INFO_PROVIDED: ['Information provided', 'human'],
  REQUEST_AUTO_APPROVED: ['Approved automatically', 'good'], REQUEST_APPROVED: ['Fully approved', 'good'], REQUEST_REJECTED: ['Rejected', 'stop'],
  WORKFLOW_UPDATED: ['Workflow updated', 'sys'], RULE_UPDATED: ['Rule updated', 'sys']
};
export const num = id => 'REQ' + String(id).padStart(7, '0');
export const initials = n => String(n || 'WN').split(/\s+/).map(w => w[0]).slice(0, 2).join('').toUpperCase();
export const pill = ([label, kind]) => html`<span class="pill ${kind}">${label}</span>`;
export const statusPill = s => pill(STATUS[s] || [s, 'mute']);

export function toast(msg, kind = 'ok') {
  const t = document.createElement('div');
  t.className = 'toast ' + kind;
  t.textContent = msg;
  document.getElementById('toasts').append(t);
  setTimeout(() => t.remove(), 4500);
}
export function openModal(content, bind) {
  const d = document.getElementById('modal');
  put(d, html`<div class="modal-body">${content}</div>`);
  d.showModal();
  bind?.(d);
}
export const closeModal = () => document.getElementById('modal').close();
export function confirmBox(message, label = 'Delete') {
  return new Promise(resolve => {
    openModal(html`<p>${message}</p><div class="row end"><button class="btn" data-close>Cancel</button><button class="btn danger" id="yes">${label}</button></div>`, d => {
      let done = false;
      d.querySelector('#yes').onclick = () => { done = true; d.close(); resolve(true); };
      d.addEventListener('close', () => { if (!done) resolve(false); }, { once: true });
    });
  });
}
