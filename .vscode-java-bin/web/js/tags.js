async function loadTags() {
    store.tags = await api.tags.list();
    renderTagFilter();
}

function renderTagFilter() {
    const sel = document.getElementById('filter-tag');
    if (!sel) return;
    sel.innerHTML = `<option value="">${t('filter_all_tags')}</option>`;
    for (const tag of store.tags) {
        const o = document.createElement('option');
        o.value = tag.id;
        o.textContent = tag.nom;
        sel.appendChild(o);
    }
}

function wireTagManagement() {
    const btn = document.getElementById('btn-manage-tags');
    if (!btn) return;
    btn.addEventListener('click', showTagsDialog);
}

function showTagsDialog() {
    const modal = document.getElementById('detail-modal');
    modal.innerHTML = `
        <div class="modal-overlay" id="modal-overlay"></div>
        <div class="modal-box modal-sm">
            <div class="modal-header">
                <h2>${t('modal_manage_tags')}</h2>
                <button class="modal-close" id="modal-close">✕</button>
            </div>
            <div class="modal-body">
                <ul id="tag-list-mgmt"></ul>
                <div class="tag-add-row">
                    <input id="new-tag-input" type="text" placeholder="${t('tag_new_placeholder')}">
                    <button id="btn-create-tag" class="btn-primary">${t('btn_add')}</button>
                </div>
            </div>
        </div>
    `;
    modal.style.display = 'flex';
    renderTagsList();

    document.getElementById('modal-close').addEventListener('click', () => {
        modal.style.display = 'none'; modal.innerHTML = ''; loadTags();
    });
    document.getElementById('modal-overlay').addEventListener('click', () => {
        modal.style.display = 'none'; modal.innerHTML = ''; loadTags();
    });
    document.getElementById('btn-create-tag').addEventListener('click', async () => {
        const nom = document.getElementById('new-tag-input').value.trim();
        if (!nom) return;
        await api.tags.create(nom);
        store.tags = await api.tags.list();
        renderTagsList();
        document.getElementById('new-tag-input').value = '';
    });
}

function renderTagsList() {
    const ul = document.getElementById('tag-list-mgmt');
    if (!ul) return;
    ul.innerHTML = '';
    for (const tag of store.tags) {
        const li = document.createElement('li');
        li.className = 'tag-mgmt-item';
        li.innerHTML = `<span>${esc(tag.nom)}</span><button data-id="${tag.id}" class="btn-icon btn-danger">✕</button>`;
        li.querySelector('button').addEventListener('click', async () => {
            if (!confirm(t('confirm_delete_tag') + ' "' + tag.nom + '"?')) return;
            await api.tags.del(tag.id);
            store.tags = await api.tags.list();
            renderTagsList();
        });
        ul.appendChild(li);
    }
}

function esc(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
