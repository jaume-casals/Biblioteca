let detailIsbn = null;

async function openDetail(isbn) {
    detailIsbn = isbn;
    const book = await api.books.get(isbn);
    const shelves = await api.books.shelves(isbn);
    const tags    = await api.books.tags(isbn);
    showDetailModal(book, shelves, tags, false);
}

function openNewBookDialog() {
    showDetailModal(null, [], [], true);
}

async function showDetailModal(book, shelves, tags, editMode) {
    const modal = document.getElementById('detail-modal');
    const isNew = !book;
    editMode = editMode || isNew;

    modal.innerHTML = `
        <div class="modal-overlay" id="modal-overlay"></div>
        <div class="modal-box">
            <div class="modal-header">
                <h2>${isNew ? t('modal_new_book') : (editMode ? t('modal_edit_book') : esc(book.nom))}</h2>
                <button class="modal-close" id="modal-close">✕</button>
            </div>
            <div class="modal-body">
                ${isNew || editMode ? renderEditForm(book) : renderViewMode(book, shelves, tags)}
            </div>
            <div class="modal-footer">
                ${isNew ? `<button id="btn-ol-lookup">${t('btn_lookup_isbn')}</button>` : ''}
                ${editMode
                    ? `<button id="btn-save-book" class="btn-primary">${t('btn_save')}</button><button id="btn-cancel-edit">${t('btn_cancel')}</button>`
                    : `<button id="btn-edit-book">${t('btn_edit')}</button><button id="btn-delete-book" class="btn-danger">${t('btn_delete')}</button>`}
            </div>
        </div>
    `;
    modal.style.display = 'flex';

    document.getElementById('modal-close').addEventListener('click', closeModal);
    document.getElementById('modal-overlay').addEventListener('click', closeModal);

    if (editMode) {
        document.getElementById('btn-save-book').addEventListener('click', () => saveBook(book));
        const cancelBtn = document.getElementById('btn-cancel-edit');
        if (cancelBtn) cancelBtn.addEventListener('click', () => {
            if (isNew) closeModal();
            else showDetailModal(book, shelves, tags, false);
        });
        if (isNew) {
            document.getElementById('btn-ol-lookup').addEventListener('click', () => olLookup());
        }
        const imgField = document.getElementById('field-imatge');
        if (imgField) {
            imgField.addEventListener('change', e => {
                const file = e.target.files[0];
                if (file) {
                    const prev = document.getElementById('cover-preview');
                    if (prev) prev.src = URL.createObjectURL(file);
                }
            });
        }
    } else {
        document.getElementById('btn-edit-book').addEventListener('click', () => showDetailModal(book, shelves, tags, true));
        document.getElementById('btn-delete-book').addEventListener('click', () => deleteBook(book.isbn));
        const loanBtn = document.getElementById('btn-loan');
        if (loanBtn) loanBtn.addEventListener('click', () => loanBook(book.isbn));
        const retBtn = document.getElementById('btn-return');
        if (retBtn) retBtn.addEventListener('click', () => returnBook(book.isbn));
        const addShelfBtn = document.getElementById('btn-manage-shelves');
        if (addShelfBtn) addShelfBtn.addEventListener('click', () => openShelvesManager(book.isbn));
    }
}

function renderViewMode(book, shelves, tags) {
    const loaned = store.loanedISBNs.has(String(book.isbn));
    return `
        <div class="detail-view">
            <div class="detail-cover-col">
                <img src="${api.books.imageUrl(book.isbn)}" class="detail-cover" alt="">
                ${loaned ? `<div class="loaned-badge">${t('loaned_badge')}</div>` : ''}
                ${loaned
                    ? `<button id="btn-return" class="btn-sm">${t('btn_return')}</button>`
                    : `<button id="btn-loan" class="btn-sm">${t('btn_loan')}</button>`}
            </div>
            <div class="detail-info-col">
                <h3>${esc(book.nom)}</h3>
                <div class="detail-field"><label>${t('field_author')}</label><span>${esc(book.autor || '')}</span></div>
                <div class="detail-field"><label>${t('field_year')}</label><span>${book.any || ''}</span></div>
                <div class="detail-field"><label>${t('field_isbn')}</label><span>${book.isbn}</span></div>
                <div class="detail-field"><label>${t('field_rating')}</label><span>${book.valoracio != null ? book.valoracio.toFixed(1) + '/10' : '—'}</span></div>
                <div class="detail-field"><label>${t('field_price')}</label><span>${book.preu != null ? store.currency + book.preu.toFixed(2) : '—'}</span></div>
                <div class="detail-field"><label>${t('field_read')}</label><span>${book.llegit ? t('yes') : t('no')}</span></div>
                ${book.pagines ? `<div class="detail-field"><label>${t('field_pages')}</label><span>${book.paginesLlegides}/${book.pagines}</span></div>` : ''}
                ${book.editorial ? `<div class="detail-field"><label>${t('field_publisher')}</label><span>${esc(book.editorial)}</span></div>` : ''}
                ${book.serie ? `<div class="detail-field"><label>${t('field_series')}</label><span>${esc(book.serie)} vol.${book.volum}</span></div>` : ''}
                ${book.idioma ? `<div class="detail-field"><label>${t('field_language')}</label><span>${esc(book.idioma)}</span></div>` : ''}
                ${book.format ? `<div class="detail-field"><label>${t('field_format')}</label><span>${esc(book.format)}</span></div>` : ''}
                ${book.paisOrigen ? `<div class="detail-field"><label>${t('field_country')}</label><span>${esc(book.paisOrigen)}</span></div>` : ''}
                ${book.dataCompra ? `<div class="detail-field"><label>${t('field_purchased')}</label><span>${book.dataCompra}</span></div>` : ''}
                ${book.dataLectura ? `<div class="detail-field"><label>${t('field_read_on')}</label><span>${book.dataLectura}</span></div>` : ''}
                ${book.desitjat ? `<div class="detail-field"><label>${t('field_wishlist')}</label><span>${t('yes')}</span></div>` : ''}
                ${book.notes ? `<div class="detail-field"><label>${t('field_notes')}</label><span class="detail-notes">${esc(book.notes)}</span></div>` : ''}
                <div class="detail-field"><label>${t('field_shelves')}</label><span>${shelves.map(s => esc(s.nom)).join(', ') || '—'}</span></div>
                <div class="detail-field"><label>${t('field_tags')}</label><span>${tags.map(tg => `<span class="tag-chip">${esc(tg.nom)}</span>`).join(' ') || '—'}</span></div>
                <button id="btn-manage-shelves" class="btn-sm">${t('btn_manage_shelves')}</button>
            </div>
            ${book.descripcio ? `<div class="detail-description"><label>${t('field_description')}</label><p>${esc(book.descripcio)}</p></div>` : ''}
        </div>
    `;
}

function renderEditForm(book) {
    const b = book || {};
    return `
        <div class="edit-form">
            <div class="edit-cover-col">
                <img id="cover-preview" src="${b.isbn ? api.books.imageUrl(b.isbn) : '/img/default-cover.png'}" class="detail-cover" alt="">
                <label class="btn-sm btn-file">${t('btn_change_cover')}<input id="field-imatge" type="file" accept="image/*" style="display:none"></label>
            </div>
            <div class="edit-fields-col">
                <div class="edit-row"><label>${t('field_isbn')}</label><input id="ef-isbn" type="number" value="${b.isbn || ''}" ${b.isbn ? 'readonly' : ''}></div>
                <div class="edit-row"><label>${t('field_title_req')}</label><input id="ef-nom" type="text" value="${esc(b.nom || '')}"></div>
                <div class="edit-row"><label>${t('field_author')}</label><input id="ef-autor" type="text" value="${esc(b.autor || '')}"></div>
                <div class="edit-row"><label>${t('field_year')}</label><input id="ef-any" type="number" value="${b.any || ''}"></div>
                <div class="edit-row"><label>${t('field_rating')} (0-10)</label><input id="ef-valoracio" type="number" step="0.1" min="0" max="10" value="${b.valoracio != null ? b.valoracio : ''}"></div>
                <div class="edit-row"><label>${t('field_price')}</label><input id="ef-preu" type="number" step="0.01" min="0" value="${b.preu != null ? b.preu : ''}"></div>
                <div class="edit-row"><label>${t('field_read')}</label><input id="ef-llegit" type="checkbox" ${b.llegit ? 'checked' : ''}></div>
                <div class="edit-row"><label>${t('field_wishlist')}</label><input id="ef-desitjat" type="checkbox" ${b.desitjat ? 'checked' : ''}></div>
                <div class="edit-row"><label>${t('field_pages')}</label><input id="ef-pagines" type="number" min="0" value="${b.pagines || ''}"></div>
                <div class="edit-row"><label>${t('field_pages_read')}</label><input id="ef-paginesLlegides" type="number" min="0" value="${b.paginesLlegides || ''}"></div>
                <div class="edit-row"><label>${t('field_publisher')}</label><input id="ef-editorial" type="text" value="${esc(b.editorial || '')}"></div>
                <div class="edit-row"><label>${t('field_series')}</label><input id="ef-serie" type="text" value="${esc(b.serie || '')}"></div>
                <div class="edit-row"><label>${t('field_volume')}</label><input id="ef-volum" type="number" min="0" value="${b.volum || ''}"></div>
                <div class="edit-row"><label>${t('field_language')}</label><input id="ef-idioma" type="text" value="${esc(b.idioma || '')}"></div>
                <div class="edit-row"><label>${t('field_format')}</label><input id="ef-format" type="text" value="${esc(b.format || '')}"></div>
                <div class="edit-row"><label>${t('field_country')}</label><input id="ef-paisOrigen" type="text" value="${esc(b.paisOrigen || '')}"></div>
                <div class="edit-row"><label>${t('field_purchased')}</label><input id="ef-dataCompra" type="date" value="${b.dataCompra || ''}"></div>
                <div class="edit-row"><label>${t('field_read_on')}</label><input id="ef-dataLectura" type="date" value="${b.dataLectura || ''}"></div>
                <div class="edit-row full-width"><label>${t('field_description')}</label><textarea id="ef-descripcio" rows="3">${esc(b.descripcio || '')}</textarea></div>
                <div class="edit-row full-width"><label>${t('field_notes')}</label><textarea id="ef-notes" rows="3">${esc(b.notes || '')}</textarea></div>
            </div>
        </div>
    `;
}

async function saveBook(existingBook) {
    const isNew = !existingBook;
    const isbn = parseInt(document.getElementById('ef-isbn').value);
    if (!isbn) { showToast(t('toast_isbn_required'), 'error'); return; }

    const data = {
        isbn,
        nom:            document.getElementById('ef-nom').value,
        autor:          document.getElementById('ef-autor').value,
        any:            parseInt(document.getElementById('ef-any').value) || null,
        valoracio:      parseFloat(document.getElementById('ef-valoracio').value) || 0,
        preu:           parseFloat(document.getElementById('ef-preu').value) || 0,
        llegit:         document.getElementById('ef-llegit').checked,
        desitjat:       document.getElementById('ef-desitjat').checked,
        pagines:        parseInt(document.getElementById('ef-pagines').value) || 0,
        paginesLlegides:parseInt(document.getElementById('ef-paginesLlegides').value) || 0,
        editorial:      document.getElementById('ef-editorial').value,
        serie:          document.getElementById('ef-serie').value,
        volum:          parseInt(document.getElementById('ef-volum').value) || 0,
        idioma:         document.getElementById('ef-idioma').value || null,
        format:         document.getElementById('ef-format').value || null,
        paisOrigen:     document.getElementById('ef-paisOrigen').value || null,
        dataCompra:     document.getElementById('ef-dataCompra').value || null,
        dataLectura:    document.getElementById('ef-dataLectura').value || null,
        descripcio:     document.getElementById('ef-descripcio').value,
        notes:          document.getElementById('ef-notes').value,
    };

    try {
        if (isNew) await api.books.add(data);
        else       await api.books.update(isbn, data);

        const imgInput = document.getElementById('field-imatge');
        if (imgInput && imgInput.files[0]) {
            await api.books.uploadImage(isbn, imgInput.files[0]);
        }

        showToast(isNew ? t('toast_book_added') : t('toast_book_updated'));
        closeModal();
        loadBooks();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteBook(isbn) {
    if (!confirm(t('confirm_delete_book'))) return;
    try {
        await api.books.del(isbn);
        showToast(t('toast_book_deleted'));
        closeModal();
        loadBooks();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loanBook(isbn) {
    const persona = prompt(t('prompt_borrower'));
    if (!persona || !persona.trim()) return;
    try {
        await api.loans.loan(isbn, persona.trim());
        store.loanedISBNs.add(String(isbn));
        showToast(t('toast_book_loaned') + ' ' + persona);
        closeModal();
        loadBooks();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function returnBook(isbn) {
    try {
        await api.loans.return(isbn);
        store.loanedISBNs.delete(String(isbn));
        showToast(t('toast_book_returned'));
        closeModal();
        loadBooks();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function olLookup() {
    const isbn = document.getElementById('ef-isbn').value;
    if (!isbn) { showToast(t('toast_isbn_first'), 'error'); return; }
    try {
        const info = await api.openlibrary.byIsbn(isbn);
        if (info.error) { showToast(info.error, 'error'); return; }
        if (info.title)     document.getElementById('ef-nom').value = info.title;
        if (info.autor)     document.getElementById('ef-autor').value = info.autor;
        if (info.any)       document.getElementById('ef-any').value = info.any;
        if (info.descripcio)document.getElementById('ef-descripcio').value = info.descripcio;
        if (info.pagines)   document.getElementById('ef-pagines').value = info.pagines;
        if (info.editorial) document.getElementById('ef-editorial').value = info.editorial;
        if (info.idioma)    document.getElementById('ef-idioma').value = info.idioma;
        showToast(t('toast_ol_loaded'));
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ── Shelves manager ───────────────────────────────────────────────────────────

async function openShelvesManager(isbn) {
    const modal = document.getElementById('detail-modal');

    async function render() {
        const currentShelves = await api.books.shelves(isbn);
        const currentIds = new Set(currentShelves.map(s => s.id));
        const available = store.shelves.filter(s => !currentIds.has(s.id));

        const rowsHtml = currentShelves.length === 0
            ? `<p class="shelf-mgr-empty">${t('shelf_not_on_any')}</p>`
            : currentShelves.map(s => `
                <div class="shelf-mgr-row" data-shelf-id="${s.id}">
                    <span class="shelf-mgr-name">${esc(s.nom)}</span>
                    <label class="shelf-mgr-label">${t('shelf_valoracio')}</label>
                    <input class="shelf-mgr-val" type="number" step="0.1" min="0" max="10"
                        value="${s.valoracioLlibre != null ? s.valoracioLlibre : ''}" style="width:5em">
                    <label class="shelf-mgr-label">
                        <input class="shelf-mgr-read" type="checkbox" ${s.llegitLlibre ? 'checked' : ''}>
                        ${t('shelf_llegit')}
                    </label>
                    <button class="btn-sm btn-shelf-update" data-id="${s.id}">${t('btn_update')}</button>
                    <button class="btn-icon btn-danger btn-shelf-remove" data-id="${s.id}">✕</button>
                </div>
            `).join('');

        const addSection = available.length > 0 ? `
            <div class="shelf-mgr-add">
                <select id="shelf-mgr-select">
                    ${available.map(s => `<option value="${s.id}">${esc(s.nom)}</option>`).join('')}
                </select>
                <label class="shelf-mgr-label">${t('shelf_valoracio')}</label>
                <input id="shelf-mgr-add-val" type="number" step="0.1" min="0" max="10" value="" style="width:5em">
                <label class="shelf-mgr-label">
                    <input id="shelf-mgr-add-read" type="checkbox">
                    ${t('shelf_llegit')}
                </label>
                <button id="btn-shelf-do-add" class="btn-sm btn-primary">${t('btn_add_to_shelf')}</button>
            </div>
        ` : '';

        modal.innerHTML = `
            <div class="modal-overlay" id="modal-overlay"></div>
            <div class="modal-box modal-sm">
                <div class="modal-header">
                    <h2>${t('modal_manage_shelves')}</h2>
                    <button class="modal-close" id="modal-close">✕</button>
                </div>
                <div class="modal-body">
                    <div id="shelf-mgr-list">${rowsHtml}</div>
                    ${addSection}
                </div>
                <div class="modal-footer">
                    <button id="btn-shelf-mgr-back">${t('btn_cancel')}</button>
                </div>
            </div>
        `;
        modal.style.display = 'flex';

        document.getElementById('modal-close').addEventListener('click', closeModal);
        document.getElementById('modal-overlay').addEventListener('click', closeModal);
        document.getElementById('btn-shelf-mgr-back').addEventListener('click', () => openDetail(isbn));

        document.querySelectorAll('.btn-shelf-update').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = parseInt(btn.dataset.id);
                const row = btn.closest('.shelf-mgr-row');
                const valoracio = parseFloat(row.querySelector('.shelf-mgr-val').value) || 0;
                const llegit    = row.querySelector('.shelf-mgr-read').checked;
                try {
                    await api.shelves.updateBook(id, isbn, valoracio, llegit);
                    showToast(t('toast_shelf_updated'));
                    render();
                } catch (err) { showToast(err.message, 'error'); }
            });
        });

        document.querySelectorAll('.btn-shelf-remove').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = parseInt(btn.dataset.id);
                try {
                    await api.shelves.removeBook(id, isbn);
                    showToast(t('toast_shelf_removed'));
                    render();
                } catch (err) { showToast(err.message, 'error'); }
            });
        });

        const addBtn = document.getElementById('btn-shelf-do-add');
        if (addBtn) {
            addBtn.addEventListener('click', async () => {
                const id       = parseInt(document.getElementById('shelf-mgr-select').value);
                const valoracio = parseFloat(document.getElementById('shelf-mgr-add-val').value) || 0;
                const llegit    = document.getElementById('shelf-mgr-add-read').checked;
                try {
                    await api.shelves.addBook(id, isbn, valoracio, llegit);
                    showToast(t('toast_added_to_shelf'));
                    render();
                } catch (err) { showToast(err.message, 'error'); }
            });
        }
    }

    await render();
}

function closeModal() {
    document.getElementById('detail-modal').style.display = 'none';
    document.getElementById('detail-modal').innerHTML = '';
    detailIsbn = null;
}

function showToast(msg, type) {
    const tEl = document.createElement('div');
    tEl.className = 'toast' + (type === 'error' ? ' toast-error' : '');
    tEl.textContent = msg;
    document.body.appendChild(tEl);
    setTimeout(() => tEl.remove(), 3000);
}
