const API_BASE = '/api';

async function req(method, path, body, opts_extra) {
    const opts = { method, headers: {}, ...opts_extra };
    if (body !== undefined && body !== null) {
        if (body instanceof ArrayBuffer || body instanceof Uint8Array) {
            // raw binary (image upload)
            opts.body = body;
        } else {
            opts.headers['Content-Type'] = 'application/json';
            opts.body = JSON.stringify(body);
        }
    }
    const r = await fetch(API_BASE + path, opts);
    if (r.status === 204) return null;
    const text = await r.text();
    if (!text) return null;
    const json = JSON.parse(text);
    if (!r.ok) throw new Error(json.error || r.statusText);
    return json;
}

const api = {
    get:    (path)        => req('GET',    path),
    post:   (path, body)  => req('POST',   path, body),
    put:    (path, body)  => req('PUT',    path, body),
    del:    (path)        => req('DELETE', path),

    books: {
        count:       ()      => api.get('/books/count'),
        recent:      ()      => api.get('/books/recent'),
        list:        (params)=> api.get('/books?' + new URLSearchParams(Object.fromEntries(Object.entries(params || {}).filter(([,v]) => v !== null && v !== undefined && v !== '')))),
        get:         (isbn)  => api.get('/books/' + isbn),
        add:         (data)  => api.post('/books', data),
        update:      (isbn, data) => api.put('/books/' + isbn, data),
        del:         (isbn)  => api.del('/books/' + isbn),
        imageUrl:    (isbn)  => API_BASE + '/books/' + isbn + '/image',
        uploadImage: async (isbn, file) => {
            const buf = await file.arrayBuffer();
            return req('POST', '/books/' + isbn + '/image', buf, { headers: { 'Content-Type': file.type || 'image/jpeg' } });
        },
        shelves:     (isbn)  => api.get('/books/' + isbn + '/shelves'),
        tags:        (isbn)  => api.get('/books/' + isbn + '/tags'),
        addTag:      (isbn, tagId)    => api.post('/books/' + isbn + '/tags/' + tagId),
        removeTag:   (isbn, tagId)    => api.del('/books/' + isbn + '/tags/' + tagId),
    },

    shelves: {
        list:   ()                          => api.get('/shelves'),
        create: (nom)                       => api.post('/shelves', { nom }),
        del:    (id)                        => api.del('/shelves/' + id),
        color:  (id, color)                 => api.put('/shelves/' + id + '/color', { color }),
        up:     (id)                        => api.post('/shelves/' + id + '/up'),
        down:   (id)                        => api.post('/shelves/' + id + '/down'),
        books:  (id)                        => api.get('/shelves/' + id + '/books'),
        addBook:(id, isbn, valoracio, llegit)=> api.post('/shelves/' + id + '/books/' + isbn, { valoracio, llegit }),
        updateBook:(id, isbn, valoracio, llegit) => api.put('/shelves/' + id + '/books/' + isbn, { valoracio, llegit }),
        removeBook:(id, isbn)               => api.del('/shelves/' + id + '/books/' + isbn),
    },

    tags: {
        list:   ()          => api.get('/tags'),
        create: (nom)       => api.post('/tags', { nom }),
        del:    (id)        => api.del('/tags/' + id),
    },

    loans: {
        list:   ()              => api.get('/loans'),
        loan:   (isbn, persona) => api.post('/loans/' + isbn, { persona }),
        return: (isbn)          => api.del('/loans/' + isbn),
    },

    meta: {
        distinct: (col)  => api.get('/meta/distinct/' + col),
        authors:  ()     => api.get('/meta/authors'),
        dbsize:   ()     => api.get('/meta/dbsize'),
    },

    config: {
        get: ()     => api.get('/config'),
        set: (data) => api.put('/config', data),
    },

    backup: {
        run:     ()    => api.post('/backup'),
        restore: async (file) => {
            const buf = await file.arrayBuffer();
            return req('POST', '/restore', buf, { headers: { 'Content-Type': 'text/plain' } });
        },
        clear:   ()    => api.post('/clear'),
    },

    importexport: {
        exportJson: async () => {
            const r = await fetch(API_BASE + '/export/json');
            if (!r.ok) throw new Error('Export failed');
            const blob = await r.blob();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url; a.download = 'biblioteca.json'; a.click();
            URL.revokeObjectURL(url);
        },
        importJson: async (file) => {
            const text = await file.text();
            return req('POST', '/import/json', JSON.parse(text));
        },
        importCsv: async (file) => {
            const text = await file.text();
            return req('POST', '/import/csv', null, { body: text, headers: { 'Content-Type': 'text/plain' } });
        },
        fetchCovers: () => api.post('/covers/fetch'),
        exportGoodreads: async () => {
            const r = await fetch(API_BASE + '/export/csv/goodreads');
            if (!r.ok) throw new Error('Export failed');
            const blob = await r.blob();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url; a.download = 'goodreads_export.csv'; a.click();
            URL.revokeObjectURL(url);
        },
    },

    openlibrary: {
        byIsbn:  (isbn)  => api.get('/openlibrary/isbn/' + isbn),
        byTitle: (title) => api.get('/openlibrary/title/' + encodeURIComponent(title)),
    },
};
