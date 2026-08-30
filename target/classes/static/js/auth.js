/**
 * auth.js — Supabase auth helpers for LeetTrack frontend.
 *
 * Strategy:
 *  1. Frontend loads Supabase JS SDK from CDN.
 *  2. User logs in/signs up via Supabase directly (no Spring Boot involvement).
 *  3. Supabase returns a JWT; we store it in localStorage.
 *  4. Every API call to Spring Boot sends: Authorization: Bearer <jwt>
 *  5. Spring Boot validates the JWT against Supabase JWKS — never sees passwords.
 *
 * Replace SUPABASE_URL and SUPABASE_ANON_KEY with your project's values.
 * These are SAFE to expose in frontend code (anon key has RLS restrictions).
 */

// ─── CONFIGURE THESE ──────────────────────────────────────────────────────────
const SUPABASE_URL      = window.SUPABASE_URL  || "https://evdcqabtwuoxkdcsceca.supabase.co";
const SUPABASE_ANON_KEY = window.SUPABASE_ANON || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV2ZGNxYWJ0d3VveGtkY3NjZWNhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgwMzc0OTAsImV4cCI6MjEwMzYxMzQ5MH0.Cdt1jlDiopU6KVH9rrXcmjVFkRERGmcsftAqr6imCn8";
// ─────────────────────────────────────────────────────────────────────────────

// Supabase client (loaded from CDN in HTML)
let _supabase = null;

function getSupabase() {
  if (!_supabase) {
    _supabase = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
  }
  return _supabase;
}

/** Get the current session's JWT, or null if not logged in. */
async function getToken() {
  const { data } = await getSupabase().auth.getSession();
  return data?.session?.access_token ?? null;
}

/** Redirect to index if not logged in. Call at top of protected pages. */
async function requireAuth() {
  const token = await getToken();
  if (!token) {
    window.location.href = "/index.html";
  }
  return token;
}

/** Authenticated fetch wrapper — adds Bearer token header automatically. */
async function apiFetch(path, options = {}) {
  const token = await getToken();
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { "Authorization": "Bearer " + token } : {}),
    ...(options.headers || {})
  };
  return fetch(path, { ...options, headers });
}

/** Show user email in nav and wire up logout button. */
async function setupNav() {
  const { data } = await getSupabase().auth.getSession();
  const email = data?.session?.user?.email;
  const emailEl = document.getElementById("userEmail");
  if (emailEl && email) emailEl.textContent = email;

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
      await getSupabase().auth.signOut();
      window.location.href = "/index.html";
    });
  }
}

/** Show a status message element. */
function showMsg(el, text, type = "ok") {
  el.textContent = text;
  el.className = "msg msg-" + type;
  el.style.display = "block";
  setTimeout(() => { el.style.display = "none"; }, 4000);
}

/** Difficulty badge CSS class. */
function diffBadge(diff) {
  if (!diff) return "";
  const d = diff.toLowerCase();
  if (d === "easy") return "badge-easy";
  if (d === "medium") return "badge-medium";
  if (d === "hard") return "badge-hard";
  return "";
}

/** Platform badge CSS class. */
function platBadge(plat) {
  if (!plat) return "";
  const p = plat.toUpperCase();
  if (p === "LEETCODE") return "badge-lc";
  if (p === "CODEFORCES") return "badge-cf";
  return "badge-gh";
}
