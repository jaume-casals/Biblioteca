document.addEventListener('DOMContentLoaded', async () => {
    // Load config
    const config = await api.config.get();
    store.darkMode    = config.darkMode;
    store.currency    = config.currencySymbol || '€';
    // Normalize Catalan 'galeria' → 'gallery', 'taula' → 'table'
    const rawMode = config.viewMode || 'table';
    store.viewMode    = (rawMode === 'galeria' || rawMode === 'gallery') ? 'gallery' : 'table';
    store.galleryZoom = config.galleryZoom || 2;

    if (store.darkMode) document.body.classList.add('dark');

    // Set initial view display state BEFORE rendering
    const isGallery = store.viewMode === 'gallery';
    document.getElementById('table-view').style.display   = isGallery ? 'none' : '';
    document.getElementById('gallery-view').style.display = isGallery ? ''     : 'none';
    document.getElementById('btn-toggle-view').textContent = isGallery ? t('btn_table') : t('btn_gallery');

    // Populate tag filter
    await loadTags();
    await loadShelves();
    await refreshLoanStatus();

    // Wire UI
    wireSearchBar();
    wireShelfAdd();
    wireTagManagement();
    wireSettingsPanel();

    // Initial load
    await loadBooks();

    // Dark mode toggle
    document.getElementById('btn-dark-toggle').addEventListener('click', async () => {
        store.darkMode = !store.darkMode;
        document.body.classList.toggle('dark', store.darkMode);
        await api.config.set({ darkMode: store.darkMode });
    });

    // Stats link
    const statsEl = document.getElementById('db-stats');
    if (statsEl) {
        const size = await api.meta.dbsize();
        statsEl.textContent = t('db_size') + ' ' + (size.bytes / 1024 / 1024).toFixed(1) + ' MB';
    }
});

function wireSettingsPanel() {
    const btn = document.getElementById('btn-settings');
    if (!btn) return;
    btn.addEventListener('click', async () => {
        const modal = document.getElementById('detail-modal');
        const config = await api.config.get();
        modal.innerHTML = `
            <div class="modal-overlay" id="modal-overlay"></div>
            <div class="modal-box modal-sm">
                <div class="modal-header"><h2>${t('modal_settings')}</h2><button class="modal-close" id="modal-close">✕</button></div>
                <div class="modal-body">
                    <div class="edit-row"><label>${t('s_currency')}</label><input id="s-currency" type="text" value="${config.currencySymbol || '€'}"></div>
                    <div class="edit-row"><label>${t('s_reading_goal')}</label><input id="s-goal" type="number" min="0" value="${config.readingGoal || 0}"></div>
                    <div class="edit-row"><label>${t('s_db_type')}</label>
                        <select id="s-dbtype">
                            <option value="h2" ${config.dbType === 'h2' ? 'selected' : ''}>${t('s_db_h2')}</option>
                            <option value="mariadb" ${config.dbType === 'mariadb' ? 'selected' : ''}>${t('s_db_mariadb')}</option>
                        </select>
                    </div>
                    <div class="edit-row"><label>${t('s_db_host')}</label><input id="s-dbhost" type="text" value="${config.dbHost || 'localhost'}"></div>
                    <div class="edit-row"><label>${t('s_db_user')}</label><input id="s-dbuser" type="text" value="${config.dbUser || ''}"></div>
                    <div class="edit-row"><label>${t('s_db_password')}</label><input id="s-dbpass" type="password" placeholder="${t('unchanged_pw')}"></div>
                    <hr>
                    <h3>${t('s_backup_restore')}</h3>
                    <div class="backup-row">
                        <button id="btn-do-backup" class="btn-primary">${t('btn_export_backup')}</button>
                        <label class="btn-sm btn-file">${t('btn_import_backup')}<input id="btn-do-restore" type="file" accept=".sql" style="display:none"></label>
                        <button id="btn-do-clear" class="btn-danger">${t('btn_clear_all')}</button>
                    </div>
                    <div id="backup-msg"></div>
                    <hr>
                    <h3>${t('s_import_export')}</h3>
                    <div class="backup-row">
                        <button id="btn-export-json" class="btn-primary">${t('btn_export_json_lbl')}</button>
                        <button id="btn-export-goodreads">${t('btn_export_goodreads_lbl')}</button>
                        <label class="btn-sm btn-file">${t('btn_import_json_lbl')}<input id="btn-import-json" type="file" accept=".json" style="display:none"></label>
                        <label class="btn-sm btn-file">${t('btn_import_csv')}<input id="btn-import-csv" type="file" accept=".csv" style="display:none"></label>
                        <button id="btn-fetch-covers">${t('btn_fetch_covers_lbl')}</button>
                    </div>
                    <div id="importexport-msg"></div>
                </div>
                <div class="modal-footer">
                    <button id="btn-save-settings" class="btn-primary">${t('btn_save')}</button>
                </div>
            </div>
        `;
        modal.style.display = 'flex';

        document.getElementById('modal-close').addEventListener('click', () => { modal.style.display = 'none'; modal.innerHTML = ''; });
        document.getElementById('modal-overlay').addEventListener('click', () => { modal.style.display = 'none'; modal.innerHTML = ''; });

        document.getElementById('btn-save-settings').addEventListener('click', async () => {
            const pw = document.getElementById('s-dbpass').value;
            const data = {
                currencySymbol: document.getElementById('s-currency').value,
                readingGoal:    parseInt(document.getElementById('s-goal').value) || 0,
                dbType:         document.getElementById('s-dbtype').value,
                dbHost:         document.getElementById('s-dbhost').value,
                dbUser:         document.getElementById('s-dbuser').value,
            };
            if (pw) data.dbPassword = pw;
            await api.config.set(data);
            store.currency = data.currencySymbol;
            showToast(t('toast_settings_saved'));
            modal.style.display = 'none'; modal.innerHTML = '';
        });

        document.getElementById('btn-do-backup').addEventListener('click', async () => {
            try {
                const r = await api.backup.run();
                document.getElementById('backup-msg').textContent = t('saved_to') + ' ' + r.file;
                showToast(t('toast_backup_created'));
            } catch (e) { showToast(e.message, 'error'); }
        });

        document.getElementById('btn-do-restore').addEventListener('change', async e => {
            const file = e.target.files[0];
            if (!file || !confirm(t('confirm_restore'))) return;
            try {
                await api.backup.restore(file);
                showToast(t('toast_restored'));
                modal.style.display = 'none'; modal.innerHTML = '';
                await loadBooks(); await loadShelves(); await loadTags();
            } catch (err) { showToast(err.message, 'error'); }
        });

        document.getElementById('btn-do-clear').addEventListener('click', async () => {
            if (!confirm(t('confirm_clear_all'))) return;
            await api.backup.clear();
            showToast(t('toast_data_cleared'));
            modal.style.display = 'none'; modal.innerHTML = '';
            await loadBooks(); await loadShelves(); await loadTags();
        });

        document.getElementById('btn-export-json').addEventListener('click', async () => {
            try { await api.importexport.exportJson(); showToast(t('toast_export_done')); }
            catch (e) { showToast(e.message, 'error'); }
        });

        document.getElementById('btn-export-goodreads').addEventListener('click', async () => {
            try { await api.importexport.exportGoodreads(); showToast(t('toast_export_done')); }
            catch (e) { showToast(e.message, 'error'); }
        });

        document.getElementById('btn-import-json').addEventListener('change', async e => {
            const file = e.target.files[0];
            if (!file) return;
            try {
                const r = await api.importexport.importJson(file);
                document.getElementById('importexport-msg').textContent =
                    t('import_result', r.ok, r.skipped, r.errors);
                showToast(t('toast_import_done'));
                await loadBooks();
            } catch (err) { showToast(err.message, 'error'); }
        });

        document.getElementById('btn-import-csv').addEventListener('change', async e => {
            const file = e.target.files[0];
            if (!file) return;
            try {
                const r = await api.importexport.importCsv(file);
                document.getElementById('importexport-msg').textContent =
                    t('import_result', r.ok, r.skipped, r.errors);
                showToast(t('toast_import_done'));
                await loadBooks();
            } catch (err) { showToast(err.message, 'error'); }
        });

        document.getElementById('btn-fetch-covers').addEventListener('click', async () => {
            try {
                const r = await api.importexport.fetchCovers();
                showToast(t('toast_fetch_covers_queued', r.queued));
            } catch (e) { showToast(e.message, 'error'); }
        });
    });
}
