const store = {
    // current filter/search state
    filters: {
        title: '', author: '', isbn: '',
        yearMin: '', yearMax: '',
        ratingMin: '', ratingMax: '',
        priceMin: '', priceMax: '',
        read: '', tagId: '', editorial: '',
        serie: '', format: '', idioma: '',
        shelfId: null,
    },
    page: 0,
    totalPages: 0,
    totalBooks: 0,
    viewMode: 'table',   // 'table' | 'gallery'
    galleryZoom: 2,
    shelves: [],
    tags: [],
    loanedISBNs: new Set(),
    currency: '€',
    darkMode: false,

    // subscribers
    _listeners: {},
    on(event, fn) {
        (this._listeners[event] = this._listeners[event] || []).push(fn);
    },
    emit(event, data) {
        (this._listeners[event] || []).forEach(fn => fn(data));
    },
};
