// ==UserScript==
// @name         Biblioteca UI STRESS TEST (hard)
// @namespace    http://localhost:7070/
// @version      2.0
// @description  Aggressive full-stack stress test: API hammering + UI automation + edge cases
// @match        http://localhost:7070/*
// @grant        none
// @run-at       document-idle
// ==/UserScript==

(async () => {
'use strict';

// ── config ─────────────────────────────────────────────────────────────────
const BASE        = '';       // same origin
const BOOK_COUNT  = 30;       // test books to create
const RAPID_OPENS = 25;       // modal open/close cycles
const ISBN_BASE   = 9780000100000; // start ISBN (13 digits)
const ISBN_END    = ISBN_BASE + BOOK_COUNT + 200; // covers edge cases

// ── runner ──────────────────────────────────────────────────────────────────
let pass = 0, fail = 0, warn = 0, totalMs = 0;
const log = [];

const PASS  = (m, ms) => { pass++;  addLog('PASS',  m, ms); };
const FAIL  = (m, ms) => { fail++;  addLog('FAIL',  m, ms); };
const WARN  = (m, ms) => { warn++;  addLog('WARN',  m, ms); };
const PHASE = (m)     => { addLog('PHASE', m); console.log(`\n[STRESS] ── ${m} ──`); };

function addLog(type, msg, ms) {
    const e = { type, msg, ms };
    log.push(e);
    const icons = { PASS:'✓', FAIL:'✗', WARN:'!', PHASE:'══' };
    console.log(`[STRESS] ${icons[type]||'?'} ${type}: ${msg}${ms ? ` (${ms}ms)` : ''}`);
    updatePanel();
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

// ── API helpers ─────────────────────────────────────────────────────────────
async function req(method, path, body) {
    const t0 = performance.now();
    const opts = { method, headers: {} };
    if (body !== undefined) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
    }
    try {
        const r = await fetch(BASE + path, opts);
        const ms = Math.round(performance.now() - t0);
        totalMs += ms;
        let data = null;
        try { data = await r.json(); } catch { try { data = await r.text(); } catch {} }
        return { status: r.status, data, ms };
    } catch (e) {
        return { status: 0, data: null, ms: 0, err: e.message };
    }
}

async function checkReq(desc, method, path, body, expectStatus) {
    const r = await req(method, path, body);
    if (r.status === expectStatus) PASS(`${desc} → ${r.status}`, r.ms);
    else FAIL(`${desc} — expected ${expectStatus}, got ${r.status}`, r.ms);
    return r;
}

// ── DOM helpers ─────────────────────────────────────────────────────────────
const el       = id => document.getElementById(id);
const qs       = sel => document.querySelector(sel);
const qsa      = sel => [...document.querySelectorAll(sel)];

function waitFor(fn, timeout = 4000) {
    return new Promise(resolve => {
        const t = Date.now();
        const tick = () => {
            const v = fn();
            if (v) return resolve(v);
            if (Date.now() - t > timeout) return resolve(null);
            setTimeout(tick, 80);
        };
        tick();
    });
}

const topModal = () => {
    const modals = qsa('[id*="modal"], .modal-overlay, .modal-box, [role="dialog"]')
        .filter(e => {
            const s = window.getComputedStyle(e);
            return s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0';
        });
    return modals.length ? modals[modals.length - 1] : null;
};

function dismissModal() {
    const m = topModal();
    if (!m) return false;
    const close = m.querySelector('[id*="close"], button[class*="close"], .modal-close');
    if (close) { close.click(); return true; }
    // Try Escape
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
    return true;
}

const domNodeCount = () => document.querySelectorAll('*').length;

// ── overlay panel ───────────────────────────────────────────────────────────
const panel = document.createElement('div');
panel.id = 'stress-panel';
panel.style.cssText = [
    'position:fixed','top:8px','right:8px','z-index:2147483647',
    'background:#0d1117','color:#c9d1d9','font:12px/1.6 monospace',
    'padding:10px 14px','border-radius:10px','width:360px',
    'max-height:70vh','overflow-y:auto',
    'box-shadow:0 8px 32px #000a','border:1px solid #30363d'
].join(';');
document.body.appendChild(panel);

function updatePanel() {
    const last = log.slice(-30);
    panel.innerHTML =
        `<div style="margin-bottom:6px"><b style="color:#58a6ff">STRESS TEST</b> ` +
        `<span style="color:#4ade80">✓${pass}</span> ` +
        `<span style="color:#f87171">✗${fail}</span> ` +
        `<span style="color:#fbbf24">!${warn}</span></div>` +
        `<div style="border-bottom:1px solid #30363d;margin-bottom:4px"></div>` +
        last.map(e => {
            if (e.type === 'PHASE') return `<div style="color:#58a6ff;margin-top:4px">── ${e.msg}</div>`;
            const c = e.type === 'PASS' ? '#4ade80' : e.type === 'FAIL' ? '#f87171' : '#fbbf24';
            const t = e.ms ? ` <span style="color:#8b949e">${e.ms}ms</span>` : '';
            return `<span style="color:${c}">${e.type==='PASS'?'✓':'!'} ${e.msg}${t}</span>`;
        }).join('<br>');
}

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 0 — Pre-cleanup: wipe any leftover test data from previous runs
// ══════════════════════════════════════════════════════════════════════════════
await sleep(1000);
PHASE('0 — Pre-cleanup (leftover test data)');

// Delete books in the ISBN range used by this test
const allBooksR = await req('GET', '/api/books');
const allBooks = Array.isArray(allBooksR.data) ? allBooksR.data : [];
const staleBooks = allBooks.filter(b => b.isbn >= ISBN_BASE && b.isbn < ISBN_END);
await Promise.all(staleBooks.map(b => req('DELETE', `/api/books/${b.isbn}`)));
if (staleBooks.length > 0) addLog('WARN', `Pre-cleanup: deleted ${staleBooks.length} leftover books`);

// Delete tags named stress-tag-*
const allTagsR = await req('GET', '/api/tags');
const stressTags = (Array.isArray(allTagsR.data) ? allTagsR.data : [])
    .filter(t => t.nom?.startsWith('stress-tag-'));
await Promise.all(stressTags.map(t => req('DELETE', `/api/tags/${t.id}`)));
if (stressTags.length > 0) addLog('WARN', `Pre-cleanup: deleted ${stressTags.length} leftover tags`);

// Delete shelves named Stress Shelf *
const allShelvesR = await req('GET', '/api/shelves');
const stressShelves = (Array.isArray(allShelvesR.data) ? allShelvesR.data : [])
    .filter(s => s.nom?.startsWith('Stress Shelf'));
await Promise.all(stressShelves.map(s => req('DELETE', `/api/shelves/${s.id}`)));
if (stressShelves.length > 0) addLog('WARN', `Pre-cleanup: deleted ${stressShelves.length} leftover shelves`);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 1 — API: create 30 books (sequential, measure throughput)
// ══════════════════════════════════════════════════════════════════════════════
PHASE('1 — Bulk create 30 books');

const createdISBNs = [];
const GENRES  = ['Fiction','Science','History','Poetry','Philosophy','Art','Tech','Travel'];
const AUTHORS = ['Alice Vega','Bob Müller','Carla Pérez','Dmytro Kovalenko','Emre Şahin',
                 'Fiona MacLeod','Giovanni Russo','Hana Sato','Iwan Petrov','Jana Nováková'];

const t0_create = performance.now();
for (let i = 0; i < BOOK_COUNT; i++) {
    const isbn = String(ISBN_BASE + i);
    const body = {
        isbn, nom: `Stress Book ${i} — ${GENRES[i % GENRES.length]}`,
        autor: AUTHORS[i % AUTHORS.length],
        autors: [AUTHORS[i % AUTHORS.length]],
        any: 1950 + i * 2,
        editorial: `Publisher ${String.fromCharCode(65 + (i % 10))}`,
        valoracio: (i % 11), preu: +(1.5 + i * 0.75).toFixed(2),
        llegit: i % 3 === 0,
        descripcio: `Auto-generated book #${i} for stress testing. ${'x'.repeat(200)}`,
        imatge: ''
    };
    const r = await req('POST', '/api/books', body);
    if (r.status === 201) createdISBNs.push(isbn);
    else FAIL(`Create book ${i} (isbn=${isbn}) → ${r.status}`);
}
const ms_create = Math.round(performance.now() - t0_create);
if (createdISBNs.length === BOOK_COUNT)
    PASS(`Created all ${BOOK_COUNT} books`, ms_create);
else
    FAIL(`Only created ${createdISBNs.length}/${BOOK_COUNT} books`);

// ── edge cases ───────────────────────────────────────────────────────────────
PHASE('1b — Edge case book inputs');

const edgeCases = [
    { nom: 'A', autor: '', isbn: String(ISBN_BASE + 100) }, // minimal
    { nom: '你好世界 — Bonjour — مرحبا — Привіт', autor: 'Ünïcödé Âùthör', isbn: String(ISBN_BASE + 101) },
    { nom: `Title with "quotes" and 'apostrophes' & <html>`, autor: 'Edge Case', isbn: String(ISBN_BASE + 102) },
];
for (const ec of edgeCases) {
    const r = await req('POST', '/api/books', { ...ec, any: 2000, valoracio: 0, preu: 0, llegit: false, imatge: '' });
    if (r.status === 201) { createdISBNs.push(ec.isbn); PASS(`Edge case book created: "${ec.nom.slice(0,30)}…"`); }
    else FAIL(`Edge case book rejected (status ${r.status}): "${ec.nom.slice(0,30)}"`);
}

// Long title must be rejected (>255 chars)
const r_longTitle = await req('POST', '/api/books', { isbn: String(ISBN_BASE + 103), nom: 'x'.repeat(500), autor: 'y'.repeat(200), any: 2000, valoracio: 0, preu: 0, llegit: false, imatge: '' });
if (r_longTitle.status === 400) PASS('POST /api/books with 500-char title → 400 (expected)');
else WARN(`POST 500-char title → ${r_longTitle.status} (expected 400)`);

// ── invalid inputs ───────────────────────────────────────────────────────────
const r_noTitle = await req('POST', '/api/books', { isbn: String(ISBN_BASE + 200), nom: '', valoracio: 0, preu: 0, llegit: false, imatge: '' });
if (r_noTitle.status === 400) PASS('POST /api/books with empty title → 400 (expected rejection)');
else WARN(`POST empty title → ${r_noTitle.status} (expected 400)`);

const r_badIsbn = await req('POST', '/api/books', { isbn: '123', nom: 'Bad', valoracio: 0, preu: 0, llegit: false, imatge: '' });
if (r_badIsbn.status === 400) PASS('POST /api/books with 3-digit ISBN → 400');
else WARN(`POST bad ISBN → ${r_badIsbn.status} (expected 400)`);

const r_dupIsbn = await req('POST', '/api/books', { isbn: createdISBNs[0], nom: 'Duplicate', valoracio: 0, preu: 0, llegit: false, imatge: '' });
if (r_dupIsbn.status === 400 || r_dupIsbn.status === 409) PASS('POST duplicate ISBN → error');
else WARN(`POST duplicate ISBN → ${r_dupIsbn.status}`);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 2 — API: parallel read stress
// ══════════════════════════════════════════════════════════════════════════════
PHASE('2 — Parallel reads (20 concurrent)');

const t0_read = performance.now();
const readResults = await Promise.all(
    createdISBNs.slice(0, 20).map(isbn => req('GET', `/api/books/${isbn}`))
);
const ms_read = Math.round(performance.now() - t0_read);
const readOK = readResults.filter(r => r.status === 200).length;
if (readOK === 20) PASS(`20 parallel GETs all 200`, ms_read);
else FAIL(`Parallel reads: ${readOK}/20 returned 200`);

// ── concurrent writes ─────────────────────────────────────────────────────────
PHASE('2b — Concurrent updates (10 simultaneous)');

const t0_upd = performance.now();
const updResults = await Promise.all(
    createdISBNs.slice(0, 10).map((isbn, i) =>
        req('PUT', `/api/books/${isbn}`, {
            nom: `Updated ${i}`, autor: AUTHORS[i % AUTHORS.length],
            any: 2000 + i, valoracio: i % 11, preu: i * 2.5,
            llegit: i % 2 === 0, descripcio: 'Updated', imatge: ''
        })
    )
);
const ms_upd = Math.round(performance.now() - t0_upd);
const updOK = updResults.filter(r => r.status === 200).length;
if (updOK === 10) PASS(`10 concurrent updates all 200`, ms_upd);
else FAIL(`Concurrent updates: ${updOK}/10 returned 200`);

// ── verify no corruption ──────────────────────────────────────────────────────
for (let i = 0; i < 5; i++) {
    const r = await req('GET', `/api/books/${createdISBNs[i]}`);
    if (r.status === 200 && r.data?.nom === `Updated ${i}`)
        PASS(`Update ${i} persisted correctly`);
    else if (r.status === 200)
        FAIL(`Update ${i} — wrong nom: "${r.data?.nom}"`);
    else
        FAIL(`Update ${i} — GET returned ${r.status}`);
}

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 3 — API: shelves at scale
// ══════════════════════════════════════════════════════════════════════════════
PHASE('3 — Shelves: create 5, add all books to each');

const shelfIds = [];
for (let i = 0; i < 5; i++) {
    const r = await req('POST', '/api/shelves', { nom: `Stress Shelf ${i}`, color: `#${(0x3a9ad9 + i * 0x112233).toString(16).slice(0,6)}` });
    if (r.status === 201 && r.data?.id) shelfIds.push(r.data.id);
    else FAIL(`Create shelf ${i} → ${r.status}`);
}
PASS(`Created ${shelfIds.length} shelves`);

// Add first 10 books to all 5 shelves concurrently
if (shelfIds.length > 0) {
    const addOps = [];
    for (const shelfId of shelfIds) {
        for (const isbn of createdISBNs.slice(0, 10)) {
            addOps.push(req('POST', `/api/shelves/${shelfId}/books/${isbn}`, { valoracio: 3, llegit: false }));
        }
    }
    const t0_add = performance.now();
    const addResults = await Promise.all(addOps);
    const ms_add = Math.round(performance.now() - t0_add);
    const addOK = addResults.filter(r => r.status === 201).length;
    if (addOK === addOps.length) PASS(`Added 10 books × 5 shelves (${addOps.length} ops)`, ms_add);
    else FAIL(`Shelf add ops: ${addOK}/${addOps.length} succeeded`);

    // Verify shelf membership
    for (const shelfId of shelfIds.slice(0, 2)) {
        const r = await req('GET', `/api/shelves/${shelfId}/books`);
        if (r.status === 200 && Array.isArray(r.data) && r.data.length >= 10)
            PASS(`Shelf ${shelfId} contains ${r.data.length} books`);
        else
            FAIL(`Shelf ${shelfId} books: status=${r.status} count=${r.data?.length}`);
    }

    // Reorder all shelves (up/down cycle)
    for (const shelfId of shelfIds) {
        const up   = await req('POST', `/api/shelves/${shelfId}/up`);
        const down = await req('POST', `/api/shelves/${shelfId}/down`);
        if (up.status !== 200 || down.status !== 200)
            WARN(`Shelf ${shelfId} reorder: up=${up.status} down=${down.status}`);
    }
    PASS('Shelf reorder cycle (all 5 shelves up+down)');

    // Update all memberships concurrently
    const updateOps = shelfIds.flatMap(shelfId =>
        createdISBNs.slice(0, 5).map(isbn =>
            req('PUT', `/api/shelves/${shelfId}/books/${isbn}`, { valoracio: 7, llegit: true })
        )
    );
    const updMem = await Promise.all(updateOps);
    const updMemOK = updMem.filter(r => r.status === 200).length;
    if (updMemOK === updateOps.length) PASS(`${updateOps.length} membership updates OK`);
    else FAIL(`Membership updates: ${updMemOK}/${updateOps.length}`);
}

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 4 — API: tags at scale
// ══════════════════════════════════════════════════════════════════════════════
PHASE('4 — Tags: create 10, tag all books');

const tagIds = [];
for (let i = 0; i < 10; i++) {
    const r = await req('POST', '/api/tags', { nom: `stress-tag-${i}` });
    if (r.status === 201 && r.data?.id) tagIds.push(r.data.id);
    else FAIL(`Create tag ${i} → ${r.status}`);
}
PASS(`Created ${tagIds.length} tags`);

// Tag every book with every tag concurrently
const tagOps = tagIds.flatMap(tagId =>
    createdISBNs.slice(0, 15).map(isbn =>
        req('POST', `/api/books/${isbn}/tags/${tagId}`)
    )
);
const t0_tag = performance.now();
const tagResults = await Promise.all(tagOps);
const ms_tag = Math.round(performance.now() - t0_tag);
const tagOK = tagResults.filter(r => r.status === 201).length;
if (tagOK === tagOps.length) PASS(`${tagOps.length} tag ops (15 books × 10 tags)`, ms_tag);
else FAIL(`Tag ops: ${tagOK}/${tagOps.length}`);

// Verify tags on a book
const r_tags = await req('GET', `/api/books/${createdISBNs[0]}/tags`);
if (r_tags.status === 200 && r_tags.data?.length >= 10)
    PASS(`Book has ${r_tags.data.length} tags after bulk tagging`);
else
    WARN(`Book tags: status=${r_tags.status} count=${r_tags.data?.length}`);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 5 — API: loans
// ══════════════════════════════════════════════════════════════════════════════
PHASE('5 — Loans: loan 10 books, verify, return all');

const loanISBNs = createdISBNs.slice(0, 10);
const borrowers = ['Alice','Bob','Carla','Dave','Eve','Frank','Grace','Hank','Iris','Jack'];

// Sequential loans (can't loan same book twice)
for (let i = 0; i < loanISBNs.length; i++) {
    const r = await req('POST', `/api/loans/${loanISBNs[i]}`, { persona: borrowers[i] });
    if (r.status !== 201) FAIL(`Loan book ${loanISBNs[i]} to ${borrowers[i]} → ${r.status}`);
}
const r_loans = await req('GET', '/api/loans');
const loanedSet = new Set(r_loans.data?.loaned || []);
const loanedCount = loanISBNs.filter(isbn => loanedSet.has(+isbn) || loanedSet.has(isbn)).length;
if (loanedCount >= 8) PASS(`${loanedCount}/10 books confirmed loaned`);
else WARN(`Only ${loanedCount}/10 books appear in loans list`);

// Try to loan an already-loaned book
const r_dup = await req('POST', `/api/loans/${loanISBNs[0]}`, { persona: 'Duplicate' });
if (r_dup.status === 400 || r_dup.status === 409) PASS('Double-loan rejected correctly');
else WARN(`Double-loan → ${r_dup.status} (expected 400/409)`);

// Return all
const returnResults = await Promise.all(loanISBNs.map(isbn => req('DELETE', `/api/loans/${isbn}`)));
const returnOK = returnResults.filter(r => r.status === 204).length;
if (returnOK >= 9) PASS(`${returnOK}/10 books returned`);
else FAIL(`Returns: ${returnOK}/10`);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 6 — API: search & filter stress
// ══════════════════════════════════════════════════════════════════════════════
PHASE('6 — Search & filter combinations (20+ queries)');

const filterTests = [
    ['/api/books?title=Stress+Book',   r => Array.isArray(r.data) && r.data.length >= Math.floor(BOOK_COUNT * 0.6), 'title=Stress+Book returns majority of test books'],
    ['/api/books?author=Alice+Vega',   r => Array.isArray(r.data) && r.data.length >= 1,               'author=Alice+Vega'],
    ['/api/books?yearMin=2000',        r => Array.isArray(r.data),                                     'yearMin=2000'],
    ['/api/books?yearMax=1999',        r => Array.isArray(r.data),                                     'yearMax=1999 returns array'],
    ['/api/books?ratingMin=5&ratingMax=10', r => Array.isArray(r.data),                               'ratingMin=5&ratingMax=10'],
    ['/api/books?ratingMin=0&ratingMax=0',  r => Array.isArray(r.data),                               'ratingMin=0&ratingMax=0'],
    ['/api/books?read=true',           r => Array.isArray(r.data),                                     'read=true'],
    ['/api/books?read=false',          r => Array.isArray(r.data),                                     'read=false'],
    ['/api/books?editorial=Publisher+A', r => Array.isArray(r.data) && r.data.length >= 1,            'editorial=Publisher+A'],
    [`/api/books?tagId=${tagIds[0]}`,  r => Array.isArray(r.data) && r.data.length >= 1,              'tagId filter'],
    [`/api/shelves/${shelfIds[0]}/books`, r => Array.isArray(r.data),                                 'shelf books list'],
    ['/api/books?title=ZZZNOMATCH',    r => Array.isArray(r.data) && r.data.length === 0,             'title=ZZZNOMATCH (empty)'],
    ['/api/books?page=0',              r => Array.isArray(r.data),                                     'page=0'],
    ['/api/books?page=1',              r => Array.isArray(r.data),                                     'page=1'],
    ['/api/books?yearMin=1980&yearMax=2060&read=false', r => Array.isArray(r.data),                   'compound filter'],
    ['/api/books/count',               r => typeof r.data?.total === 'number',                         'count has numeric total'],
    ['/api/books/recent',              r => Array.isArray(r.data),                                     'recent books'],
    ['/api/meta/authors',              r => Array.isArray(r.data),                                     'meta/authors'],
    ['/api/meta/distinct/editorial',   r => Array.isArray(r.data),                                     'meta/distinct/editorial'],
    ['/api/meta/distinct/idioma',      r => Array.isArray(r.data),                                     'meta/distinct/idioma'],
    ['/api/meta/distinct/format',      r => Array.isArray(r.data),                                     'meta/distinct/format'],
    ['/api/meta/dbsize',               r => typeof r.data?.bytes === 'number',                         'meta/dbsize is numeric'],
];

const t0_filter = performance.now();
const filterResults = await Promise.all(filterTests.map(([path]) => req('GET', path)));
const ms_filter = Math.round(performance.now() - t0_filter);

for (let i = 0; i < filterTests.length; i++) {
    const [, check, desc] = filterTests[i];
    const r = filterResults[i];
    if (r.status === 200 && check(r)) PASS(desc, r.ms);
    else FAIL(`${desc} — status=${r.status}`);
}
PASS(`${filterTests.length} filter queries (parallel)`, ms_filter);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 7 — API: export/import round-trip
// ══════════════════════════════════════════════════════════════════════════════
PHASE('7 — Export/import round-trip');

const r_expJson = await req('GET', '/api/export/json');
if (r_expJson.status === 200) {
    const books = Array.isArray(r_expJson.data?.llibres) ? r_expJson.data.llibres : [];
    const hasTestBooks = books.filter(b => String(b.isbn||'').startsWith('978000010')).length;
    if (hasTestBooks >= BOOK_COUNT - 2)
        PASS(`JSON export: ${books.length} total books, ${hasTestBooks} test books`, r_expJson.ms);
    else
        WARN(`JSON export: only ${hasTestBooks}/${BOOK_COUNT} test books found`);
} else FAIL(`GET /api/export/json → ${r_expJson.status}`);

const r_csv = await req('GET', '/api/export/csv/goodreads');
if (r_csv.status === 200) PASS('CSV/Goodreads export OK', r_csv.ms);
else FAIL(`CSV export → ${r_csv.status}`);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 8 — UI: rapid modal open/close (25 cycles)
// ══════════════════════════════════════════════════════════════════════════════
PHASE(`8 — UI: rapid modal open/close ×${RAPID_OPENS}`);

await sleep(500);
const rows = document.querySelectorAll('#book-tbody tr');
if (rows.length === 0) {
    WARN('No rows in table — reloading');
    window.store?.emit?.('filter-changed');
    await sleep(1500);
}

const nodesBefore = domNodeCount();
let openCount = 0, closeCount = 0;
for (let i = 0; i < RAPID_OPENS; i++) {
    const row = document.querySelector('#book-tbody tr');
    if (!row) { WARN(`Rapid open: no rows at cycle ${i}`); break; }
    row.click();
    const modal = await waitFor(topModal, 1500);
    if (modal) {
        openCount++;
        await sleep(50);
        dismissModal();
        await waitFor(() => !topModal(), 1000);
        closeCount++;
    }
    await sleep(20);
}
const nodesAfter = domNodeCount();
const nodeLeak = nodesAfter - nodesBefore;

if (openCount >= RAPID_OPENS - 2) PASS(`Rapid open/close: ${openCount}/${RAPID_OPENS} opened`);
else FAIL(`Rapid open/close: only ${openCount}/${RAPID_OPENS} dialogs appeared`);

if (nodeLeak < 500) PASS(`DOM node leak check: +${nodeLeak} nodes after ${RAPID_OPENS} modal cycles`);
else WARN(`Possible DOM leak: +${nodeLeak} nodes after modal cycling`);

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 9 — UI: all sidebar shelves + back
// ══════════════════════════════════════════════════════════════════════════════
PHASE('9 — UI: cycle through all sidebar shelves');

const shelfItems = qsa('#shelf-list li, #shelf-list [data-shelf-id], #shelf-list button, #shelf-list a');
let shelfClicks = 0;
for (const item of shelfItems) {
    item.click();
    await sleep(200);
    shelfClicks++;
}
if (shelfClicks > 0) PASS(`Clicked ${shelfClicks} sidebar shelf items — no crash`);
else WARN('No shelf items found in sidebar');

// Navigate back to all books
const allBtn = qsa('button, li, a').find(e => /Tots|All|Todo/i.test(e.textContent?.trim()));
if (allBtn) { allBtn.click(); await sleep(400); PASS('Returned to all-books view'); }
else WARN('"Tots / All" button not found in sidebar');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 10 — UI: view toggle rapid cycling (20×)
// ══════════════════════════════════════════════════════════════════════════════
PHASE('10 — UI: rapid view toggle (table↔gallery) ×20');

const toggleBtn = el('btn-toggle-view');
if (toggleBtn) {
    const t0_toggle = performance.now();
    for (let i = 0; i < 20; i++) {
        toggleBtn.click();
        await sleep(30);
    }
    const ms_toggle = Math.round(performance.now() - t0_toggle);
    // End on table view
    const tableVis = el('table-view') && window.getComputedStyle(el('table-view')).display !== 'none';
    PASS(`20× view toggle no crash — table visible: ${tableVis}`, ms_toggle);
} else FAIL('#btn-toggle-view not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 11 — UI: search bar spam (10 rapid inputs)
// ══════════════════════════════════════════════════════════════════════════════
PHASE('11 — UI: search bar rapid input');

const searchBar = el('search-bar');
if (searchBar) {
    const terms = ['a','ab','Stress','Stress B','Stress Bo','Stress Boo','Updated','',
                   'Alice','Fiction','x'.repeat(200),''];
    const t0_search = performance.now();
    for (const term of terms) {
        searchBar.value = term;
        searchBar.dispatchEvent(new Event('input', { bubbles: true }));
        await sleep(50);
    }
    await sleep(500);
    const ms_search = Math.round(performance.now() - t0_search);
    PASS(`${terms.length} rapid search terms — no crash`, ms_search);
} else FAIL('#search-bar not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 12 — UI: filter panel all combinations
// ══════════════════════════════════════════════════════════════════════════════
PHASE('12 — UI: filter panel combinations');

const filterToggle = el('btn-filter-toggle');
if (filterToggle) {
    filterToggle.click(); await sleep(300);
    const filterPanel = el('filter-panel');
    if (filterPanel && window.getComputedStyle(filterPanel).display !== 'none') {
        const inputs = filterPanel.querySelectorAll('input, select');
        const testVals = { text: 'test', number: '5', select: null };
        for (const inp of inputs) {
            if (inp.type === 'checkbox') {
                inp.checked = !inp.checked;
                inp.dispatchEvent(new Event('change', { bubbles: true }));
            } else if (inp.tagName === 'SELECT') {
                if (inp.options.length > 1) {
                    inp.selectedIndex = 1;
                    inp.dispatchEvent(new Event('change', { bubbles: true }));
                    await sleep(100);
                    inp.selectedIndex = 0;
                    inp.dispatchEvent(new Event('change', { bubbles: true }));
                }
            } else {
                inp.value = inp.type === 'number' ? '5' : 'test';
                inp.dispatchEvent(new Event('input', { bubbles: true }));
                await sleep(80);
                inp.value = '';
                inp.dispatchEvent(new Event('input', { bubbles: true }));
            }
            await sleep(30);
        }
        PASS(`Exercised ${inputs.length} filter inputs`);
        const clearBtn = el('btn-clear-filters');
        if (clearBtn) { clearBtn.click(); PASS('Clear filters clicked'); }
    } else WARN('Filter panel did not open');
    filterToggle.click(); await sleep(200);
} else FAIL('#btn-filter-toggle not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 13 — UI: open book + all sub-buttons
// ══════════════════════════════════════════════════════════════════════════════
PHASE('13 — UI: book detail — all sub-buttons');

const firstRow2 = qs('#book-tbody tr');
if (firstRow2) {
    firstRow2.click();
    const modal = await waitFor(topModal, 3000);
    if (modal) {
        PASS('Book detail modal opened');

        // Edit cycle
        const editBtn = el('btn-edit-book') || qs('[id*="edit-book"]');
        if (editBtn) {
            editBtn.click(); await sleep(400);
            // Modify a field
            const nomField = qs('#ef-nom, #field-nom, input[name="nom"], #edit-nom');
            if (nomField) {
                nomField.value = nomField.value + ' (edited)';
                nomField.dispatchEvent(new Event('input', { bubbles: true }));
                PASS('Edit mode: nom field modified');
            } else WARN('nom field not found in edit mode');
            const cancelBtn = el('btn-cancel-edit') || qs('[id*="cancel"]');
            if (cancelBtn) { cancelBtn.click(); await sleep(300); PASS('Edit cancelled'); }
            else { dismissModal(); await sleep(300); }
        } else WARN('#btn-edit-book not found');

        // Shelves button
        await waitFor(topModal, 1000);
        const shelvesBtn = el('btn-manage-shelves') || qs('[id*="shelves"]');
        if (shelvesBtn) {
            shelvesBtn.click(); await sleep(600);
            const sub = topModal();
            if (sub) { PASS('Manage shelves sub-dialog opened'); dismissModal(); await sleep(300); }
            else WARN('Shelves sub-dialog did not open');
        } else WARN('#btn-manage-shelves not found');

        dismissModal(); await sleep(400);
    } else WARN('Book detail modal did not open');
} else WARN('No book rows available');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 14 — UI: settings panel full exercise
// ══════════════════════════════════════════════════════════════════════════════
PHASE('14 — UI: settings panel full exercise');

const settingsBtn = el('btn-settings');
if (settingsBtn) {
    settingsBtn.click(); await sleep(700);
    const exportJsonBtn  = el('btn-export-json');
    const exportCsvBtn   = el('btn-export-goodreads');
    const backupBtn      = el('btn-do-backup');
    const saveBtn        = el('btn-save-settings');

    if (exportJsonBtn) {
        exportJsonBtn.click(); await sleep(400);
        PASS('Export JSON button clicked in settings');
    } else WARN('#btn-export-json not found');

    if (exportCsvBtn) {
        exportCsvBtn.click(); await sleep(400);
        PASS('Export CSV/Goodreads button clicked');
    } else WARN('#btn-export-goodreads not found');

    if (backupBtn) {
        backupBtn.click(); await sleep(600);
        const confirmDlg = await waitFor(topModal, 1500);
        if (confirmDlg) {
            PASS('Backup dialog/confirm appeared');
            dismissModal(); await sleep(300);
        } else WARN('Backup did not open dialog');
    } else WARN('#btn-do-backup not found');

    // Change currency field and revert
    const currency = el('s-currency');
    if (currency) {
        const orig = currency.value;
        currency.value = '£'; currency.dispatchEvent(new Event('input', { bubbles: true }));
        await sleep(100);
        currency.value = orig; currency.dispatchEvent(new Event('input', { bubbles: true }));
        PASS('Currency field modified and reverted');
    }

    if (saveBtn) { saveBtn.click(); await sleep(500); PASS('Save settings clicked'); }
    dismissModal(); await sleep(400);
} else FAIL('#btn-settings not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 15 — UI: dark mode 10× toggle
// ══════════════════════════════════════════════════════════════════════════════
PHASE('15 — UI: dark mode 10× rapid toggle');

const darkBtn = el('btn-dark-toggle');
if (darkBtn) {
    const origDark = document.body.classList.contains('dark');
    for (let i = 0; i < 10; i++) { darkBtn.click(); await sleep(30); }
    // Restore original
    if (document.body.classList.contains('dark') !== origDark) darkBtn.click();
    PASS('Dark mode 10× toggle — no crash');
} else FAIL('#btn-dark-toggle not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 16 — UI: new book + OpenLibrary lookup
// ══════════════════════════════════════════════════════════════════════════════
PHASE('16 — UI: new book dialog + OpenLibrary lookup');

const newBookBtn = el('btn-new-book');
if (newBookBtn) {
    newBookBtn.click();
    const modal = await waitFor(topModal, 2000);
    if (modal) {
        PASS('New book dialog opened');
        const isbnInput = qs('#field-isbn, input[name="isbn"]');
        if (isbnInput) {
            isbnInput.value = '9780140328721'; // Watership Down
            isbnInput.dispatchEvent(new Event('input', { bubbles: true }));
            const lookupBtn = el('btn-ol-lookup') || qs('[id*="lookup"]');
            if (lookupBtn) {
                lookupBtn.click(); await sleep(2000);
                const nomField = qs('#field-nom, input[name="nom"]');
                if (nomField?.value?.length > 0) PASS(`OpenLibrary lookup filled nom: "${nomField.value.slice(0,30)}"`);
                else WARN('OpenLibrary lookup did not fill nom field (network?)');
            } else WARN('#btn-ol-lookup not found');
        }
        dismissModal(); await sleep(400);
    } else WARN('New book dialog did not open');
} else FAIL('#btn-new-book not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 17 — UI: pagination navigation
// ══════════════════════════════════════════════════════════════════════════════
PHASE('17 — UI: pagination navigation');

const pagination = el('pagination');
if (pagination) {
    const btns = pagination.querySelectorAll('button');
    const nextBtn = [...btns].find(b => /next|›|»|→|▶/i.test(b.textContent));
    const prevBtn = [...btns].find(b => /prev|‹|«|←|◀/i.test(b.textContent));

    let pages = 0;
    while (nextBtn && !nextBtn.disabled && pages < 10) {
        nextBtn.click(); await sleep(300); pages++;
    }
    if (pages > 0) PASS(`Navigated ${pages} pages forward`);
    else WARN('Could not navigate pages (1 page or next btn not found)');

    // Go back
    let back = 0;
    while (prevBtn && !prevBtn.disabled && back < pages) {
        prevBtn.click(); await sleep(300); back++;
    }
    if (back > 0) PASS(`Navigated ${back} pages back`);
} else FAIL('#pagination not found');

// ══════════════════════════════════════════════════════════════════════════════
// PHASE 18 — API: cleanup (delete all test data)
// ══════════════════════════════════════════════════════════════════════════════
PHASE('18 — Cleanup: delete all test data');

// Remove tags from books first
const removeTagOps = tagIds.flatMap(tagId =>
    createdISBNs.slice(0, 15).map(isbn => req('DELETE', `/api/books/${isbn}/tags/${tagId}`))
);
await Promise.all(removeTagOps);

// Delete tags by pattern (catches any not in tagIds due to prior partial run)
const allTagsFinal = await req('GET', '/api/tags');
const finalStressTags = (Array.isArray(allTagsFinal.data) ? allTagsFinal.data : [])
    .filter(t => t.nom?.startsWith('stress-tag-'));
await Promise.all(finalStressTags.map(t => req('DELETE', `/api/tags/${t.id}`)));
const tagsLeft = (await req('GET', '/api/tags')).data?.filter(t => t.nom?.startsWith('stress-tag-'))?.length ?? 0;
if (tagsLeft === 0) PASS('All stress tags deleted');
else FAIL(`${tagsLeft} stress tags remain after deletion`);

// Delete shelves by pattern
const allShelvesFinal = await req('GET', '/api/shelves');
const finalStressShelves = (Array.isArray(allShelvesFinal.data) ? allShelvesFinal.data : [])
    .filter(s => s.nom?.startsWith('Stress Shelf'));
await Promise.all(finalStressShelves.map(s => req('DELETE', `/api/shelves/${s.id}`)));
const shelvesLeft = (await req('GET', '/api/shelves')).data?.filter(s => s.nom?.startsWith('Stress Shelf'))?.length ?? 0;
if (shelvesLeft === 0) PASS('All stress shelves deleted');
else FAIL(`${shelvesLeft} stress shelves remain`);

// Delete all test books by ISBN range
const allBooksFinal = await req('GET', '/api/books');
const finalStressBooks = (Array.isArray(allBooksFinal.data) ? allBooksFinal.data : [])
    .filter(b => b.isbn >= ISBN_BASE && b.isbn < ISBN_END);
const t0_del = performance.now();
await Promise.all(finalStressBooks.map(b => req('DELETE', `/api/books/${b.isbn}`)));
const ms_del = Math.round(performance.now() - t0_del);
PASS(`Deleted ${finalStressBooks.length} test books`, ms_del);

// Verify spot-check
const spotISBNs = createdISBNs.slice(0, 5).length ? createdISBNs.slice(0, 5)
    : [ISBN_BASE, ISBN_BASE+1, ISBN_BASE+2, ISBN_BASE+3, ISBN_BASE+4].map(String);
const verifyResults = await Promise.all(spotISBNs.map(isbn => req('GET', `/api/books/${isbn}`)));
const goneOK = verifyResults.filter(r => r.status === 400 || r.status === 404).length;
if (goneOK === spotISBNs.length) PASS('Spot-check: deleted books return 400/404');
else FAIL(`Spot-check: only ${goneOK}/${spotISBNs.length} deleted books give error status`);

// ══════════════════════════════════════════════════════════════════════════════
// FINAL SUMMARY
// ══════════════════════════════════════════════════════════════════════════════
console.log(`\n[STRESS] ════════════════════ SUMMARY ════════════════════`);
console.log(`[STRESS] PASS : ${pass}`);
console.log(`[STRESS] FAIL : ${fail}`);
console.log(`[STRESS] WARN : ${warn}`);
console.log(`[STRESS] TOTAL: ${pass + fail + warn}`);
console.log(`[STRESS] API time total: ${(totalMs/1000).toFixed(1)}s`);

panel.innerHTML =
    `<div style="margin-bottom:8px"><b style="color:#58a6ff;font-size:14px">STRESS TEST — DONE</b></div>` +
    `<div style="font-size:13px;line-height:2">` +
    `<span style="color:#4ade80">✓ PASS: ${pass}</span><br>` +
    `<span style="color:#f87171">✗ FAIL: ${fail}</span><br>` +
    `<span style="color:#fbbf24">! WARN: ${warn}</span><br>` +
    `<span style="color:#8b949e">Total: ${pass+fail+warn}</span><br>` +
    `<span style="color:#8b949e">API: ${(totalMs/1000).toFixed(1)}s</span>` +
    `</div><hr style="border-color:#30363d;margin:8px 0">` +
    `<div style="font-size:11px;max-height:300px;overflow-y:auto">` +
    log.filter(e => e.type !== 'PASS').map(e => {
        if (e.type === 'PHASE') return `<div style="color:#58a6ff;margin-top:4px">── ${e.msg}</div>`;
        const c = e.type === 'FAIL' ? '#f87171' : '#fbbf24';
        return `<span style="color:${c}">✗ ${e.msg}</span>`;
    }).join('<br>') +
    (fail === 0 && warn === 0 ? '<div style="color:#4ade80">All clean ✓</div>' : '') +
    '</div>';

// ── save log to file ────────────────────────────────────────────────────────
const ts = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
const txt = log.map(e =>
    e.type === 'PHASE'
        ? `\n══ ${e.msg}`
        : `[${e.type}] ${e.msg}${e.ms ? ` (${e.ms}ms)` : ''}`
).join('\n') +
`\n\nSUMMARY\nPASS : ${pass}\nFAIL : ${fail}\nWARN : ${warn}\nTOTAL: ${pass+fail+warn}\nAPI  : ${(totalMs/1000).toFixed(1)}s\n`;
const blob = new Blob([txt], { type: 'text/plain' });
const a = document.createElement('a');
a.href = URL.createObjectURL(blob);
a.download = `stress-test-${ts}.txt`;
a.click();

})();
