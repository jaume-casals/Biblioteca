async function loadShelves() {
    store.shelves = await api.shelves.list();
    renderSidebar();
}

function renderSidebar() {
    const list = document.getElementById('shelf-list');
    list.innerHTML = '';

    const allItem = document.createElement('li');
    allItem.className = 'shelf-item' + (!store.filters.shelfId ? ' active' : '');
    allItem.innerHTML = `<span>${t('all_books')}</span>`;
    allItem.addEventListener('click', () => {
        store.filters.shelfId = null;
        store.page = 0;
        document.querySelectorAll('.shelf-item').forEach(el => el.classList.remove('active'));
        allItem.classList.add('active');
        loadBooks();
    });
    list.appendChild(allItem);

    for (const s of store.shelves) {
        const li = document.createElement('li');
        li.className = 'shelf-item' + (store.filters.shelfId === s.id ? ' active' : '');
        if (s.color) li.style.borderLeftColor = s.color;
        li.innerHTML = `
            <span class="shelf-name">${esc(s.nom)}</span>
            <span class="shelf-count">${s.count || ''}</span>
            <span class="shelf-actions">
                <button class="btn-icon" title="${t('btn_move_up')}" data-action="up" data-id="${s.id}">↑</button>
                <button class="btn-icon" title="${t('btn_move_down')}" data-action="down" data-id="${s.id}">↓</button>
                <button class="btn-icon btn-danger" title="${t('btn_delete')}" data-action="del" data-id="${s.id}">✕</button>
            </span>
        `;
        li.querySelector('.shelf-name').addEventListener('click', () => {
            store.filters.shelfId = s.id;
            store.page = 0;
            document.querySelectorAll('.shelf-item').forEach(el => el.classList.remove('active'));
            li.classList.add('active');
            loadBooks();
        });
        li.querySelector('[data-action=up]').addEventListener('click', async e => {
            e.stopPropagation();
            await api.shelves.up(s.id);
            loadShelves();
        });
        li.querySelector('[data-action=down]').addEventListener('click', async e => {
            e.stopPropagation();
            await api.shelves.down(s.id);
            loadShelves();
        });
        li.querySelector('[data-action=del]').addEventListener('click', async e => {
            e.stopPropagation();
            if (!confirm(t('confirm_delete_shelf') + ' "' + s.nom + '"?')) return;
            await api.shelves.del(s.id);
            if (store.filters.shelfId === s.id) { store.filters.shelfId = null; loadBooks(); }
            loadShelves();
        });
        list.appendChild(li);
    }
}

function wireShelfAdd() {
    document.getElementById('btn-add-shelf').addEventListener('click', async () => {
        const nom = prompt(t('prompt_new_shelf'));
        if (!nom) return;
        await api.shelves.create(nom);
        loadShelves();
    });
}

function esc(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
