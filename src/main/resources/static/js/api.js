// Thin fetch wrapper + session storage. Same-origin: the UI is served by Spring Boot itself.
export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

export const session = {
  get token() { return localStorage.getItem("wn_token"); },
  get user() { try { return JSON.parse(localStorage.getItem("wn_user")); } catch { return null; } },
  get org() { return localStorage.getItem("wn_org") || ""; },
  save({ token, user, organizationName }) {
    localStorage.setItem("wn_token", token);
    localStorage.setItem("wn_user", JSON.stringify(user));
    localStorage.setItem("wn_org", organizationName || "");
  },
  clear() { ["wn_token", "wn_user", "wn_org"].forEach(k => localStorage.removeItem(k)); }
};

export async function api(path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (session.token) headers.Authorization = "Bearer " + session.token;
  let res;
  try {
    res = await fetch("/api" + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError("Cannot reach the server. Check that the backend is running.", 0);
  }
  let json = null;
  try { json = await res.json(); } catch { /* empty body */ }
  if (res.status === 401 && session.token && !path.startsWith("/auth/")) {
    session.clear();
    location.hash = "#/login";
    throw new ApiError("Your session expired. Log in again.", 401);
  }
  if (!res.ok) throw new ApiError(json?.message || json?.error || "Something went wrong (" + res.status + ")", res.status);
  return json && "data" in json ? json.data : json;
}
