# Project Structure Audit

**Date:** 2026-04-25  
**Scope:** Web SPA component (`Biblioteca.html` + `components.jsx`)  
**Auditor:** Claude Code  

---

## Overview

The web component is a **single-file static SPA** with no server, no build step, and no package manager. It is self-contained in two files loaded by the browser directly:

| File | Role | Lines |
|------|------|-------|
| `Biblioteca.html` | Shell, CDN loader, app config, data seed, `App` root | ~402 |
| `components.jsx` | All React components, all business logic | ~982 |

Runtime stack:
- **React 18.3.1** — loaded from `unpkg.com` with SHA-384 SRI integrity hash
- **ReactDOM 18.3.1** — same CDN, SRI enforced
- **Babel Standalone 7.29.0** — runtime JSX transpilation in-browser, SRI enforced
- **No bundler, no TypeScript, no test runner, no package.json**

---

## 1. Entry Points

| Entry Point | Description |
|-------------|-------------|
| `Biblioteca.html` | Only entry point. Browser opens this file directly (or serves it statically). No `server.js`, no `app.js`, no Node process. |

Boot sequence:
1. Browser parses `Biblioteca.html`
2. CDN scripts load (React, ReactDOM, Babel) — blocked by SRI if tampered
3. `<script type="text/babel" src="components.jsx">` — Babel transpiles and executes `components.jsx`, exporting all components to `window`
4. Second inline `<script type="text/babel">` defines `App`, initialises state from `localStorage`, renders `<App />`

Config constants at top of `Biblioteca.html`:
```js
const TWEAK_DEFAULTS = {
  accentHue: 220,
  sidebarWidth: 220,
  cardColumns: 4,
  showPrice: false,
};
```

Seed data: 12 hardcoded classic books, 4 shelves. Written to `localStorage` only on first run (`bib_books` key absent).

---

## 2. Routes and Endpoints

**None.** Single-page, no URL routing. View state is held in React `useState`:

```js
const [view, setView] = useState(saved.view || 'grid');   // 'grid' | 'list'
const [selectedShelf, setSelectedShelf] = useState(null);  // shelf id or null
const [detailBook, setDetailBook] = useState(null);        // book object or null
```

Navigation is pure component state — no `react-router`, no `history.pushState`, no hash routing. Deep-linking and browser back/forward are **not supported**.

---

## 3. Middleware Chain

**N/A — static file, no server.** No Express/Koa/Hapi pipeline. No CORS headers, no CSP headers, no rate limiting middleware.

**Finding:** No Content-Security-Policy. The page loads CDN scripts, but the browser has no policy to enforce. A compromised CDN delivery would not be blocked (though SRI hashes on the three CDN tags mitigate this specific vector).

---

## 4. External Service Integrations

| Service | Endpoint | Called From | Auth |
|---------|----------|-------------|------|
| OpenLibrary Books API | `https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data` | `fetchByISBN(isbn)` in `components.jsx` | None — public |
| OpenLibrary Search API | `https://openlibrary.org/search.json?title={encoded}&limit=10` | `searchByTitle(title)` in `components.jsx` | None — public |
| OpenLibrary Covers CDN | `https://covers.openlibrary.org/b/isbn/{isbn}-{size}.jpg` | `COVERS(isbn, size)` helper, used in `CoverImage` | None — public |

All three calls are fire-and-forget `fetch()` with no timeout, no retry, no error boundary beyond a `catch` that silently clears loading state. Network failure shows nothing to the user.

**Input handling in external calls:**
- `fetchByISBN`: ISBN passed directly into URL string — no sanitization, but OpenLibrary rejects non-numeric ISBNs gracefully
- `searchByTitle`: uses `encodeURIComponent(title)` — correct
- Cover URL: ISBN interpolated directly — safe for URL context, no injection risk

---

## 5. Database Connection Points

**No database.** All persistence uses `localStorage` under five keys:

| Key | Type | Contents |
|-----|------|----------|
| `bib_books` | JSON array | All book objects |
| `bib_shelves` | JSON array | Shelf objects |
| `bib_shelf_books` | JSON object | `{ shelfId: [isbn, ...] }` |
| `bib_view` | string | `'grid'` or `'list'` |
| `bib_tweaks` | JSON object | `TWEAK_DEFAULTS` overrides |

Read/write pattern: full-array serialization on every mutation (`JSON.stringify` / `JSON.parse`). No partial updates. On a large library this becomes O(n) per interaction.

**No schema validation** on reads. A corrupted `localStorage` entry causes a silent parse failure; the app falls back to empty state (seed data re-applied) without warning the user.

**Storage limit:** `localStorage` is capped at ~5 MB per origin. No guard against overflow; `localStorage.setItem` silently fails or throws `QuotaExceededError` (not caught).

---

## 6. Authentication / Authorization Flow

**None.** The app has no login, no session, no token. All data is local to the browser profile.

**postMessage integration** (partial auth surface):

`Biblioteca.html` listens for messages from a parent frame:

```js
window.addEventListener('message', (e) => {
  if (e.data === '__activate_edit_mode')   setEditMode(true);
  if (e.data === '__deactivate_edit_mode') setEditMode(false);
});
```

On mount, it posts `__edit_mode_available` to `window.parent` (for embedding in an iframe).

**Finding (Medium):** The `message` listener does **not check `e.origin`**. Any frame on any origin that can obtain a reference to this window can enable/disable edit mode. In a cross-origin embedding scenario this allows a malicious parent to toggle edit mode without user interaction. Fix: validate `e.origin` against an expected value before acting.

```js
// Fix:
window.addEventListener('message', (e) => {
  if (e.origin !== 'https://expected-parent.example.com') return;
  ...
});
```

---

## 7. File Upload Handling

**None.** No `<input type="file">`. Cover images come from:
1. OpenLibrary CDN URL (fetched automatically by ISBN)
2. Manual URL string typed by the user into `AddBookModal`

The URL string is stored as-is in `book.imatge` and rendered as `<img src={book.imatge}>`. There is no URL validation — a `javascript:` URI would be blocked by modern browsers' `img` src handling, but a `data:` URI with large payload is accepted and stored in `localStorage`.

---

## 8. API Rate Limiting

**N/A — no server-side API.** The app is a pure client; it has no endpoints to rate-limit.

Client-side: no throttling or debouncing on the OpenLibrary search call. `searchByTitle` fires on every click of the Search button; rapid clicks issue multiple concurrent requests. OpenLibrary's public API has an undocumented rate limit (~100 req/min); sustained rapid search could result in temporary blocks. No retry-after logic.

---

## 9. Component Inventory

All components defined in `components.jsx` and exported to `window`:

| Component | Responsibility |
|-----------|----------------|
| `CoverImage` | Renders book cover with fallback placeholder |
| `StarRating` | 1–5 star read-only or interactive rating |
| `BookCard` | Grid-view card (cover + title + author + rating) |
| `BookRow` | List-view row (same fields in tabular layout) |
| `Sidebar` | Shelf list + counts + add-shelf button |
| `SidebarRow` | Single shelf entry with drag-reorder handle |
| `TopBar` | Search bar, view toggle, settings icon |
| `AddBookModal` | ISBN/title search tab + manual entry tab |
| `BookDetailPanel` | Slide-in detail/edit panel with blurred cover header |
| `StatsModal` | Reading statistics overlay |

`App` (defined inline in `Biblioteca.html`) is the root: holds all state, passes callbacks down, renders the layout.

---

## 10. Notable Design Findings

| ID | Severity | Description |
|----|----------|-------------|
| PS-1 | Medium | `postMessage` listener ignores `e.origin` — any frame can toggle edit mode |
| PS-2 | Low | No CSP header — relies solely on SRI for CDN script integrity |
| PS-3 | Low | `isbn: form.isbn \|\| String(Date.now())` — timestamp used as ISBN fallback; breaks ISBN-based cover lookup and deduplication |
| PS-4 | Low | `localStorage.setItem` not wrapped in try/catch — silent failure on quota exceeded |
| PS-5 | Low | No URL validation on manually entered cover image URL — arbitrary `data:` URI accepted |
| PS-6 | Info | No routing — browser back/forward non-functional; deep-links impossible |
| PS-7 | Info | Babel transpiles JSX at runtime — adds ~250 KB parse cost; acceptable for low-traffic internal tool |
| PS-8 | Info | All state is per-browser-profile; no sync, no export/import of library data |

---

## Summary

The web SPA is a self-contained browser utility with minimal attack surface (no server, no auth, no file upload). The two actionable findings are the unguarded `postMessage` handler (PS-1) and the missing `QuotaExceededError` handling (PS-4). The CDN dependency is mitigated by SRI hashes. Structurally the app is simple and consistent; the main scalability constraint is full-array `localStorage` serialization on every write.
