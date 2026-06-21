let currentBooks = [];

async function loadBooks() {
    const params = { page: store.page, ...store.filters };
    console.log('[loadBooks] params:', JSON.stringify(params));
    try {
        if (store.filters.shelfId) {
            currentBooks = await api.shelves.books(store.filters.shelfId);
        } else {
            currentBooks = await api.books.list(params);
        }
        console.log('[loadBooks] currentBooks length:', Array.isArray(currentBooks) ? currentBooks.length : typeof currentBooks, currentBooks);
    } catch (err) {
        console.error('[loadBooks] fetch error:', err);
        currentBooks = [];
    }
    renderView();
}

function renderView() {
    if (store.viewMode === 'table') renderTable();
    else renderGallery();
    renderPagination();
}

function renderTable() {
    const tv = document.getElementById('table-view');
    const cs = window.getComputedStyle(tv);
    console.log('[renderTable] books:', currentBooks.length, 'table-view display:', cs.display, 'height:', tv.offsetHeight, 'visibility:', cs.visibility);
    const tbody = document.getElementById('book-tbody');
    if (!tbody) { console.error('[renderTable] book-tbody not found!'); return; }
    tbody.innerHTML = '';
    for (const b of currentBooks) {
        const tr = document.createElement('tr');
        tr.dataset.isbn = b.isbn;
        tr.innerHTML = `
            <td class="cover-cell"><img src="${api.books.imageUrl(b.isbn)}" class="cover-thumb" alt="" loading="lazy"></td>
            <td title="${esc(b.nom)}">${esc(b.nom)}</td>
            <td title="${esc(b.autor || '')}">${esc(b.autor || '')}</td>
            <td>${b.any || ''}</td>
            <td>${b.valoracio != null ? b.valoracio.toFixed(1) : ''}</td>
            <td>${b.preu != null ? store.currency + b.preu.toFixed(2) : ''}</td>
            <td>${b.llegit ? '✓' : ''}</td>
            <td>${b.isbn}</td>
            ${store.loanedISBNs.has(String(b.isbn)) ? `<td class="loaned-badge">${t('loaned_badge')}</td>` : '<td></td>'}
        `;
        tr.addEventListener('click', () => openDetail(b.isbn));
        tbody.appendChild(tr);
    }
}

function renderGallery() {
    console.log('[renderGallery] books:', currentBooks.length, 'viewMode:', store.viewMode);
    const galleryView = document.getElementById('gallery-view');
    const gvcs = galleryView ? window.getComputedStyle(galleryView) : null;
    console.log('[renderGallery] gallery-view computed display:', gvcs?.display, 'height:', galleryView?.offsetHeight, 'clientHeight:', galleryView?.clientHeight);
    const grid = document.getElementById('gallery-grid');
    grid.innerHTML = '';
    const zoomClass = ['zoom-sm', 'zoom-md', 'zoom-lg', 'zoom-xl'][store.galleryZoom] || 'zoom-md';
    grid.className = 'gallery-grid ' + zoomClass;
    for (const b of currentBooks) {
        const card = document.createElement('div');
        card.className = 'gallery-card';
        card.dataset.isbn = b.isbn;
        card.innerHTML = `
            <img src="${api.books.imageUrl(b.isbn)}" alt="${esc(b.nom)}" loading="lazy">
            <div class="gallery-title">${esc(b.nom)}</div>
            <div class="gallery-author">${esc(b.autor || '')}</div>
        `;
        card.addEventListener('click', () => openDetail(b.isbn));
        grid.appendChild(card);
    }
}

async function renderPagination() {
    const counts = await api.books.count();
    store.totalBooks = counts.total;
    store.totalPages = counts.pages;

    const el = document.getElementById('pagination');
    el.innerHTML = '';
    if (store.filters.shelfId) return; // no pagination for shelf view

    const hasPrev = store.page > 0;
    const hasNext = store.page < store.totalPages;

    el.innerHTML = `
        <button id="btn-prev" ${hasPrev ? '' : 'disabled'}>‹</button>
        <span>${t('page_info', store.page + 1, store.totalPages + 1, counts.total)}</span>
        <button id="btn-next" ${hasNext ? '' : 'disabled'}>›</button>
    `;
    if (hasPrev) el.querySelector('#btn-prev').addEventListener('click', () => { store.page--; loadBooks(); });
    if (hasNext) el.querySelector('#btn-next').addEventListener('click', () => { store.page++; loadBooks(); });
}

async function refreshLoanStatus() {
    const data = await api.loans.list();
    store.loanedISBNs = new Set((data.loaned || []).map(String));
}

function wireSearchBar() {
    const bar = document.getElementById('search-bar');
    let timer;
    bar.addEventListener('input', () => {
        clearTimeout(timer);
        timer = setTimeout(() => {
            store.filters.title = bar.value;
            store.page = 0;
            loadBooks();
        }, 300);
    });

    document.getElementById('btn-new-book').addEventListener('click', () => openNewBookDialog());
    document.getElementById('btn-toggle-view').addEventListener('click', () => {
        store.viewMode = store.viewMode === 'table' ? 'gallery' : 'table';
        document.getElementById('btn-toggle-view').textContent = store.viewMode === 'table' ? t('btn_gallery') : t('btn_table');
        document.getElementById('table-view').style.display = store.viewMode === 'table' ? '' : 'none';
        document.getElementById('gallery-view').style.display = store.viewMode === 'gallery' ? '' : 'none';
        renderView();
    });

    document.getElementById('filter-form').addEventListener('submit', e => {
        e.preventDefault();
        applyFilterForm();
    });
    document.getElementById('btn-clear-filters').addEventListener('click', () => {
        document.getElementById('filter-form').reset();
        store.filters = { ...store.filters, yearMin: '', yearMax: '', ratingMin: '', ratingMax: '',
            priceMin: '', priceMax: '', read: '', tagId: '', editorial: '', serie: '', format: '', idioma: '' };
        store.page = 0;
        loadBooks();
    });
}

function applyFilterForm() {
    const f = document.getElementById('filter-form');
    const fd = new FormData(f);
    store.filters.author    = fd.get('author') || '';
    store.filters.yearMin   = fd.get('yearMin') || '';
    store.filters.yearMax   = fd.get('yearMax') || '';
    store.filters.ratingMin = fd.get('ratingMin') || '';
    store.filters.ratingMax = fd.get('ratingMax') || '';
    store.filters.priceMin  = fd.get('priceMin') || '';
    store.filters.priceMax  = fd.get('priceMax') || '';
    store.filters.read      = fd.get('read') || '';
    store.filters.tagId     = fd.get('tagId') || '';
    store.filters.editorial = fd.get('editorial') || '';
    store.filters.serie     = fd.get('serie') || '';
    store.filters.format    = fd.get('format') || '';
    store.filters.idioma    = fd.get('idioma') || '';
    store.page = 0;
    loadBooks();
}

function esc(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
