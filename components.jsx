
// components.jsx — exported to window for use in Biblioteca.html
const { useState, useEffect, useRef, useCallback } = React;

// ── Helpers ──────────────────────────────────────────────────────────────────
const bookHue = (nom = '') => {
  let h = 0;
  for (const c of nom) h = (h * 31 + c.charCodeAt(0)) & 0xffff;
  return (h % 300) + 20; // avoid greenish range, keep warm+cool tones
};

const COVERS = (isbn, size = 'M') =>
  `https://covers.openlibrary.org/b/isbn/${isbn}-${size}.jpg`;

const toStars = v => (v || 0) / 2;
const fromStars = s => s * 2;

const fetchByISBN = async (isbn) => {
  try {
    const r = await fetch(
      `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`
    );
    const data = await r.json();
    const key = `ISBN:${isbn}`;
    if (!data[key]) return null;
    const b = data[key];
    const year = b.publish_date
      ? parseInt((b.publish_date.match(/\b(\d{4})\b/) || [])[1]) || null
      : null;
    return {
      nom: b.title || '',
      autor: b.authors?.[0]?.name || '',
      any: year,
      descripcio:
        typeof b.notes === 'string'
          ? b.notes
          : b.notes?.value || b.description?.value || b.description || '',
    };
  } catch { return null; }
};

const searchByTitle = async (title) => {
  try {
    const enc = encodeURIComponent(title);
    const r = await fetch(
      `https://openlibrary.org/search.json?title=${enc}&limit=8&fields=title,author_name,first_publish_year,isbn`
    );
    const data = await r.json();
    return (data.docs || []).map(d => ({
      isbn: d.isbn?.[0] || '',
      nom: d.title || '',
      autor: d.author_name?.[0] || '',
      any: d.first_publish_year || null,
    }));
  } catch { return []; }
};

// ── CoverImage ────────────────────────────────────────────────────────────────
const CoverImage = ({ isbn, nom = '', autor = '', size = 'M', fullHeight = false }) => {
  const [failed, setFailed] = useState(false);
  const hue = bookHue(nom);

  const placeholder = (
    <div style={{
      width: '100%', height: '100%',
      background: `linear-gradient(155deg, oklch(38% 0.13 ${hue}), oklch(26% 0.10 ${(hue + 45) % 360}))`,
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      padding: '16px 12px', boxSizing: 'border-box',
      position: 'relative', gap: 8,
    }}>
      <div style={{
        position: 'absolute', left: 9, top: 0, bottom: 0,
        width: 3, background: 'rgba(255,255,255,0.12)',
        borderRadius: '0 2px 2px 0',
      }} />
      <div style={{
        color: 'rgba(255,255,255,0.93)',
        fontFamily: 'Lora, serif',
        fontSize: fullHeight ? 16 : 13,
        fontStyle: 'italic',
        textAlign: 'center', lineHeight: 1.4,
        textShadow: '0 1px 6px rgba(0,0,0,0.5)',
        overflow: 'hidden',
        display: '-webkit-box',
        WebkitLineClamp: fullHeight ? 6 : 4,
        WebkitBoxOrient: 'vertical',
      }}>{nom}</div>
      {autor && (
        <div style={{
          color: 'rgba(255,255,255,0.5)',
          fontFamily: 'Outfit, sans-serif',
          fontSize: fullHeight ? 12 : 10,
          textAlign: 'center',
        }}>{autor}</div>
      )}
    </div>
  );

  if (failed || !isbn) return placeholder;

  return (
    <img
      src={COVERS(isbn, size)}
      alt={nom}
      onError={() => setFailed(true)}
      style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
    />
  );
};

// ── StarRating ────────────────────────────────────────────────────────────────
const StarRating = ({ value = 0, onChange, size = 16, readOnly = false }) => {
  const [hover, setHover] = useState(null);
  const stars = 5;
  const filled = (value / 10) * stars;

  return (
    <div style={{ display: 'flex', gap: 1, alignItems: 'center' }}>
      {Array.from({ length: stars }, (_, i) => {
        const active = hover !== null ? i < hover : filled > i + 0.25;
        const half = hover === null && filled > i + 0.1 && filled < i + 0.75;
        return (
          <span
            key={i}
            onMouseEnter={readOnly ? undefined : () => setHover(i + 1)}
            onMouseLeave={readOnly ? undefined : () => setHover(null)}
            onClick={readOnly ? undefined : () => onChange && onChange(((i + 1) / stars) * 10)}
            style={{
              cursor: readOnly ? 'default' : 'pointer',
              fontSize: size,
              color: active || half ? '#c8920a' : '#ddd',
              lineHeight: 1,
              transition: 'transform 0.1s, color 0.1s',
              transform: !readOnly && hover === i + 1 ? 'scale(1.3)' : 'scale(1)',
              display: 'inline-block',
              userSelect: 'none',
            }}
          >
            {active ? '★' : half ? '⯨' : '☆'}
          </span>
        );
      })}
      {!readOnly && value > 0 && (
        <span style={{
          fontSize: size - 4, color: 'var(--text-muted)',
          marginLeft: 4, fontFamily: 'Outfit, sans-serif',
        }}>{value.toFixed(1)}</span>
      )}
    </div>
  );
};

// ── BookCard (grid) ───────────────────────────────────────────────────────────
const BookCard = ({ book, onClick, isSelected }) => {
  const [hov, setHov] = useState(false);
  const pct = book.pagines > 0 ? book.paginesLlegides / book.pagines : 0;

  return (
    <div
      onClick={onClick}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      style={{
        cursor: 'pointer',
        display: 'flex', flexDirection: 'column',
        background: '#fff',
        borderRadius: 8,
        overflow: 'hidden',
        border: isSelected ? '2px solid var(--accent)' : '2px solid transparent',
        boxShadow: hov
          ? '0 10px 32px rgba(0,0,0,0.15)'
          : '0 2px 8px rgba(0,0,0,0.07)',
        transform: hov ? 'translateY(-4px)' : 'translateY(0)',
        transition: 'box-shadow 0.2s, transform 0.2s, border-color 0.15s',
      }}
    >
      <div style={{ aspectRatio: '2/3', position: 'relative', overflow: 'hidden', flexShrink: 0 }}>
        <CoverImage isbn={book.isbn} nom={book.nom} autor={book.autor} />
        <div style={{
          position: 'absolute', top: 7, right: 7,
          width: 9, height: 9, borderRadius: '50%',
          background: book.llegit ? '#2d9e6b' : '#bbb',
          border: '2px solid rgba(255,255,255,0.85)',
          boxShadow: '0 1px 3px rgba(0,0,0,0.25)',
        }} title={book.llegit ? 'Read' : 'Unread'} />
      </div>
      <div style={{ padding: '10px 10px 9px', display: 'flex', flexDirection: 'column', gap: 3 }}>
        <div style={{
          fontFamily: 'Lora, serif', fontSize: 12.5, fontWeight: 600,
          color: 'var(--text)', lineHeight: 1.35,
          overflow: 'hidden', display: '-webkit-box',
          WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
        }}>{book.nom}</div>
        <div style={{
          fontFamily: 'Outfit, sans-serif', fontSize: 11,
          color: 'var(--text-muted)',
          overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis',
        }}>{book.autor}</div>
        <div style={{ marginTop: 3, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <StarRating value={book.valoracio || 0} readOnly size={11} />
          {book.any ? <span style={{ fontSize: 10, color: 'var(--text-muted)', fontFamily: 'Outfit, sans-serif' }}>{book.any}</span> : null}
        </div>
        {book.pagines > 0 && !book.llegit && pct > 0 && (
          <div style={{ marginTop: 3, height: 2, background: '#eee', borderRadius: 1 }}>
            <div style={{ height: '100%', width: `${pct * 100}%`, background: 'var(--accent)', borderRadius: 1 }} />
          </div>
        )}
      </div>
    </div>
  );
};

// ── BookRow (list view) ───────────────────────────────────────────────────────
const BookRow = ({ book, onClick, isSelected }) => {
  const [hov, setHov] = useState(false);
  return (
    <div
      onClick={onClick}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      style={{
        display: 'grid',
        gridTemplateColumns: '40px 1fr 180px 60px 120px 70px 60px',
        gap: 12,
        alignItems: 'center',
        padding: '10px 20px',
        cursor: 'pointer',
        background: isSelected ? 'oklch(97% 0.015 34)' : hov ? '#faf8f5' : '#fff',
        borderBottom: '1px solid var(--border)',
        transition: 'background 0.12s',
        borderLeft: isSelected ? '3px solid var(--accent)' : '3px solid transparent',
      }}
    >
      <div style={{ width: 32, height: 48, borderRadius: 3, overflow: 'hidden', flexShrink: 0 }}>
        <CoverImage isbn={book.isbn} nom={book.nom} autor={book.autor} />
      </div>
      <div>
        <div style={{ fontFamily: 'Lora, serif', fontSize: 14, fontWeight: 600, color: 'var(--text)', lineHeight: 1.3, overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>{book.nom}</div>
        <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>{book.autor}</div>
      </div>
      <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)', overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>{book.autor}</div>
      <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)', textAlign: 'center' }}>{book.any || '—'}</div>
      <StarRating value={book.valoracio || 0} readOnly size={13} />
      <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, textAlign: 'center' }}>
        <span style={{
          padding: '2px 8px', borderRadius: 20,
          background: book.llegit ? '#e8f7f0' : '#f0f0f0',
          color: book.llegit ? '#2d9e6b' : '#888',
          fontSize: 11, fontWeight: 500,
        }}>{book.llegit ? 'Read' : 'Unread'}</span>
      </div>
      <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)', textAlign: 'right' }}>
        {book.preu ? `€${book.preu.toFixed(2)}` : '—'}
      </div>
    </div>
  );
};

// ── Sidebar ───────────────────────────────────────────────────────────────────
const Sidebar = ({ shelves, selectedShelf, onSelectShelf, onAddShelf, onDeleteShelf, books, onShowStats }) => {
  const [adding, setAdding] = useState(false);
  const [newName, setNewName] = useState('');
  const totalRead = books.filter(b => b.llegit).length;

  const confirm = () => {
    if (newName.trim()) { onAddShelf(newName.trim()); }
    setNewName(''); setAdding(false);
  };

  return (
    <aside style={{
      width: 220, flexShrink: 0,
      background: 'var(--sidebar-bg)',
      display: 'flex', flexDirection: 'column',
      height: '100vh',
    }}>
      {/* Logo */}
      <div style={{ padding: '26px 20px 18px', borderBottom: '1px solid rgba(255,255,255,0.07)' }}>
        <div style={{ fontFamily: 'Lora, serif', fontStyle: 'italic', fontSize: 26, color: 'var(--sidebar-accent)', letterSpacing: '-0.5px' }}>
          biblioteca
        </div>
        <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'rgba(255,255,255,0.25)', marginTop: 2, letterSpacing: '1.5px', textTransform: 'uppercase' }}>
          personal library
        </div>
      </div>

      {/* Quick nav */}
      <div style={{ padding: '10px 0 8px', borderBottom: '1px solid rgba(255,255,255,0.07)' }}>
        {[
          { id: null,     label: 'All Books', icon: '◈', count: books.length },
          { id: 'read',   label: 'Read',      icon: '✓', count: totalRead },
          { id: 'unread', label: 'Unread',    icon: '○', count: books.length - totalRead },
        ].map(item => (
          <SidebarRow key={String(item.id)} {...item} active={selectedShelf === item.id} onClick={() => onSelectShelf(item.id)} />
        ))}
      </div>

      {/* Shelves */}
      <div style={{ flex: 1, overflow: 'auto', padding: '10px 0 6px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 20px 6px' }}>
          <span style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'rgba(255,255,255,0.28)', textTransform: 'uppercase', letterSpacing: '1.2px', fontWeight: 600 }}>Shelves</span>
          <button onClick={() => setAdding(v => !v)} style={{ background: 'none', border: 'none', color: 'rgba(255,255,255,0.35)', cursor: 'pointer', fontSize: 18, lineHeight: 1, padding: 0 }} title="Add shelf">+</button>
        </div>

        {adding && (
          <div style={{ padding: '0 12px 8px' }}>
            <input
              autoFocus
              value={newName}
              onChange={e => setNewName(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') confirm(); if (e.key === 'Escape') { setNewName(''); setAdding(false); } }}
              onBlur={confirm}
              placeholder="Shelf name…"
              style={{
                width: '100%', boxSizing: 'border-box',
                background: 'rgba(255,255,255,0.07)',
                border: '1px solid rgba(255,255,255,0.15)',
                borderRadius: 5, color: '#fff',
                padding: '6px 10px', fontSize: 12,
                fontFamily: 'Outfit, sans-serif', outline: 'none',
              }}
            />
          </div>
        )}

        {shelves.length === 0 && !adding && (
          <div style={{ padding: '6px 20px', fontSize: 12, color: 'rgba(255,255,255,0.2)', fontFamily: 'Outfit, sans-serif', fontStyle: 'italic' }}>No shelves yet</div>
        )}
        {shelves.map(s => (
          <SidebarRow
            key={s.id}
            label={s.nom}
            icon="▸"
            active={selectedShelf === s.id}
            onClick={() => onSelectShelf(s.id)}
            onDelete={() => onDeleteShelf(s.id)}
          />
        ))}
      </div>

      {/* Bottom */}
      <div style={{ borderTop: '1px solid rgba(255,255,255,0.07)', padding: '8px 0 20px' }}>
        <SidebarRow label="Statistics" icon="◎" active={false} onClick={onShowStats} />
        <div style={{ padding: '6px 20px 0', fontSize: 10, color: 'rgba(255,255,255,0.18)', fontFamily: 'Outfit, sans-serif' }}>
          {totalRead} of {books.length} books read
        </div>
      </div>
    </aside>
  );
};

const SidebarRow = ({ label, icon, count, active, onClick, onDelete }) => {
  const [hov, setHov] = useState(false);
  return (
    <div
      onClick={onClick}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '7px 20px', cursor: 'pointer',
        background: active ? 'rgba(255,255,255,0.07)' : hov ? 'rgba(255,255,255,0.035)' : 'transparent',
        transition: 'background 0.12s',
        position: 'relative',
      }}
    >
      {active && <div style={{ position: 'absolute', left: 0, top: 3, bottom: 3, width: 3, background: 'var(--sidebar-accent)', borderRadius: '0 3px 3px 0' }} />}
      <span style={{ color: active ? 'var(--sidebar-accent)' : 'rgba(255,255,255,0.28)', fontSize: 11 }}>{icon}</span>
      <span style={{
        flex: 1, fontSize: 13, fontFamily: 'Outfit, sans-serif',
        color: active ? '#fff' : 'rgba(255,255,255,0.62)',
        fontWeight: active ? 500 : 400,
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
      }}>{label}</span>
      {count !== undefined && (
        <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.22)', fontFamily: 'Outfit, sans-serif' }}>{count}</span>
      )}
      {onDelete && hov && (
        <span
          onClick={e => { e.stopPropagation(); onDelete(); }}
          style={{ color: 'rgba(255,255,255,0.3)', cursor: 'pointer', fontSize: 16, padding: '0 2px', lineHeight: 1 }}
        >×</span>
      )}
    </div>
  );
};

// ── TopBar ────────────────────────────────────────────────────────────────────
const TopBar = ({ query, onQuery, viewMode, onViewMode, onAdd, filterRead, onFilterRead, shelfName, count }) => (
  <div style={{
    display: 'flex', alignItems: 'center', gap: 10,
    padding: '14px 24px',
    borderBottom: '1px solid var(--border)',
    background: '#fff',
    flexShrink: 0,
  }}>
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 8, padding: '8px 14px', gap: 8 }}>
      <span style={{ color: 'var(--text-muted)', fontSize: 15, lineHeight: 1 }}>⌕</span>
      <input
        value={query}
        onChange={e => onQuery(e.target.value)}
        placeholder={`Search ${shelfName}…`}
        style={{ flex: 1, border: 'none', background: 'transparent', fontFamily: 'Outfit, sans-serif', fontSize: 14, color: 'var(--text)', outline: 'none' }}
      />
      {query && <button onClick={() => onQuery('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', fontSize: 18, lineHeight: 1 }}>×</button>}
    </div>

    <select
      value={filterRead}
      onChange={e => onFilterRead(e.target.value)}
      style={{ border: '1px solid var(--border)', borderRadius: 6, padding: '8px 10px', background: '#fff', fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'var(--text)', cursor: 'pointer', outline: 'none' }}
    >
      <option value="all">All</option>
      <option value="read">Read</option>
      <option value="unread">Unread</option>
    </select>

    <div style={{ display: 'flex', border: '1px solid var(--border)', borderRadius: 6, overflow: 'hidden' }}>
      {['grid', 'list'].map(m => (
        <button key={m} onClick={() => onViewMode(m)} style={{
          padding: '7px 13px', border: 'none', cursor: 'pointer',
          background: viewMode === m ? 'var(--accent)' : '#fff',
          color: viewMode === m ? '#fff' : 'var(--text-muted)',
          fontSize: 15, transition: 'background 0.12s',
        }}>{m === 'grid' ? '⊞' : '☰'}</button>
      ))}
    </div>

    <button onClick={onAdd} style={{
      background: 'var(--accent)', color: '#fff',
      border: 'none', borderRadius: 8,
      padding: '8px 20px',
      fontFamily: 'Outfit, sans-serif', fontSize: 14, fontWeight: 600,
      cursor: 'pointer', whiteSpace: 'nowrap',
      boxShadow: '0 2px 10px oklch(46% 0.14 34 / 0.25)',
    }}>+ Add Book</button>
  </div>
);

// ── AddBookModal ──────────────────────────────────────────────────────────────
const AddBookModal = ({ onClose, onSave, shelves, existingBook }) => {
  const editing = !!existingBook;
  const [tab, setTab] = useState(editing ? 'manual' : 'search');
  const [searchQ, setSearchQ] = useState('');
  const [searchType, setSearchType] = useState('isbn');
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [form, setForm] = useState(existingBook ? { ...existingBook } : {
    isbn: '', nom: '', autor: '', any: '', descripcio: '',
    valoracio: 0, preu: '', llegit: false, pagines: '', paginesLlegides: '', notes: '',
  });
  const [shelfIds, setShelfIds] = useState([]);
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const doSearch = async () => {
    if (!searchQ.trim()) return;
    setSearching(true); setSearchResults([]);
    if (searchType === 'isbn') {
      const meta = await fetchByISBN(searchQ.trim());
      if (meta) {
        setForm(f => ({ ...f, isbn: searchQ.trim(), ...meta, any: meta.any || '' }));
        setTab('manual');
      } else {
        setSearchResults([]);
      }
    } else {
      const results = await searchByTitle(searchQ.trim());
      setSearchResults(results);
    }
    setSearching(false);
  };

  const pickResult = async (r) => {
    let meta = { ...r };
    if (r.isbn) {
      const fetched = await fetchByISBN(r.isbn);
      if (fetched) meta = { ...meta, ...fetched };
    }
    setForm(f => ({ ...f, ...meta, any: meta.any || '' }));
    setTab('manual');
  };

  const handleSave = () => {
    if (!form.nom.trim()) return;
    setSaving(true);
    const book = {
      ...form,
      isbn: form.isbn || String(Date.now()),
      any: parseInt(form.any) || null,
      valoracio: parseFloat(form.valoracio) || 0,
      preu: parseFloat(form.preu) || 0,
      pagines: parseInt(form.pagines) || 0,
      paginesLlegides: parseInt(form.paginesLlegides) || 0,
    };
    onSave(book, shelfIds);
    setSaving(false);
  };

  const inputStyle = {
    width: '100%', boxSizing: 'border-box',
    border: '1px solid var(--border)', borderRadius: 6,
    padding: '8px 11px', fontFamily: 'Outfit, sans-serif',
    fontSize: 13, color: 'var(--text)', background: '#fdfcfb',
    outline: 'none',
  };
  const labelStyle = { fontSize: 11, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 4, display: 'block' };

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(20,16,12,0.55)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center' }} onClick={e => e.target === e.currentTarget && onClose()}>
      <div style={{ background: '#fff', borderRadius: 12, width: 560, maxWidth: '95vw', maxHeight: '90vh', display: 'flex', flexDirection: 'column', boxShadow: '0 20px 60px rgba(0,0,0,0.25)' }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '20px 24px 0' }}>
          <h2 style={{ fontFamily: 'Lora, serif', fontSize: 20, fontWeight: 600, color: 'var(--text)', margin: 0 }}>{editing ? 'Edit Book' : 'Add Book'}</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 22, color: 'var(--text-muted)', lineHeight: 1 }}>×</button>
        </div>

        {/* Tabs */}
        {!editing && (
          <div style={{ display: 'flex', gap: 0, padding: '16px 24px 0', borderBottom: '1px solid var(--border)' }}>
            {['search', 'manual'].map(t => (
              <button key={t} onClick={() => setTab(t)} style={{
                background: 'none', border: 'none', cursor: 'pointer',
                padding: '8px 16px',
                fontFamily: 'Outfit, sans-serif', fontSize: 13,
                color: tab === t ? 'var(--accent)' : 'var(--text-muted)',
                fontWeight: tab === t ? 600 : 400,
                borderBottom: tab === t ? '2px solid var(--accent)' : '2px solid transparent',
                marginBottom: -1,
              }}>{t === 'search' ? 'Search OpenLibrary' : 'Manual Entry'}</button>
            ))}
          </div>
        )}

        <div style={{ flex: 1, overflow: 'auto', padding: '20px 24px' }}>
          {tab === 'search' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', gap: 8 }}>
                <select value={searchType} onChange={e => setSearchType(e.target.value)} style={{ ...inputStyle, width: 110, flexShrink: 0 }}>
                  <option value="isbn">ISBN</option>
                  <option value="title">Title</option>
                </select>
                <input
                  value={searchQ}
                  onChange={e => setSearchQ(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && doSearch()}
                  placeholder={searchType === 'isbn' ? 'Enter ISBN…' : 'Enter book title…'}
                  style={{ ...inputStyle, flex: 1 }}
                />
                <button onClick={doSearch} disabled={searching} style={{
                  background: 'var(--accent)', color: '#fff', border: 'none', borderRadius: 6,
                  padding: '8px 18px', fontFamily: 'Outfit, sans-serif', fontSize: 13, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap',
                }}>{searching ? '…' : 'Search'}</button>
              </div>

              {searching && (
                <div style={{ textAlign: 'center', padding: '24px', color: 'var(--text-muted)', fontFamily: 'Outfit, sans-serif', fontSize: 13 }}>
                  Searching OpenLibrary…
                </div>
              )}

              {searchResults.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {searchResults.map((r, i) => (
                    <div key={i} onClick={() => pickResult(r)} style={{
                      display: 'flex', gap: 12, alignItems: 'center',
                      padding: '10px 12px', borderRadius: 8, cursor: 'pointer',
                      border: '1px solid var(--border)',
                      transition: 'background 0.12s',
                    }}
                    onMouseEnter={e => e.currentTarget.style.background = '#faf8f5'}
                    onMouseLeave={e => e.currentTarget.style.background = '#fff'}
                    >
                      <div style={{ width: 36, height: 52, flexShrink: 0, borderRadius: 3, overflow: 'hidden' }}>
                        <CoverImage isbn={r.isbn} nom={r.nom} autor={r.autor} />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontFamily: 'Lora, serif', fontSize: 14, fontWeight: 600, color: 'var(--text)', overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>{r.nom}</div>
                        <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>{r.autor} {r.any ? `· ${r.any}` : ''}</div>
                        {r.isbn && <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 11, color: 'var(--text-muted)', marginTop: 1, opacity: 0.6 }}>ISBN {r.isbn}</div>}
                      </div>
                      <span style={{ color: 'var(--accent)', fontSize: 13, fontFamily: 'Outfit, sans-serif' }}>Select →</span>
                    </div>
                  ))}
                </div>
              )}

              {!searching && searchResults.length === 0 && searchQ && (
                <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-muted)', fontFamily: 'Outfit, sans-serif', fontSize: 13 }}>
                  No results found. Try <button onClick={() => setTab('manual')} style={{ background: 'none', border: 'none', color: 'var(--accent)', cursor: 'pointer', fontSize: 13, fontFamily: 'Outfit, sans-serif', textDecoration: 'underline', padding: 0 }}>manual entry</button>.
                </div>
              )}
            </div>
          )}

          {tab === 'manual' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              {/* Cover preview */}
              {(form.isbn || form.nom) && (
                <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
                  <div style={{ width: 80, height: 120, borderRadius: 6, overflow: 'hidden', flexShrink: 0, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}>
                    <CoverImage isbn={form.isbn} nom={form.nom} autor={form.autor} fullHeight />
                  </div>
                  <div style={{ flex: 1 }}>
                    <label style={labelStyle}>Title *</label>
                    <input value={form.nom} onChange={e => set('nom', e.target.value)} placeholder="Book title" style={inputStyle} />
                  </div>
                </div>
              )}

              {!form.isbn && !form.nom && (
                <div>
                  <label style={labelStyle}>Title *</label>
                  <input value={form.nom} onChange={e => set('nom', e.target.value)} placeholder="Book title" style={inputStyle} />
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label style={labelStyle}>Author</label>
                  <input value={form.autor} onChange={e => set('autor', e.target.value)} placeholder="Author name" style={inputStyle} />
                </div>
                <div>
                  <label style={labelStyle}>ISBN</label>
                  <input value={form.isbn} onChange={e => set('isbn', e.target.value)} placeholder="9780…" style={inputStyle} />
                </div>
                <div>
                  <label style={labelStyle}>Year</label>
                  <input value={form.any} onChange={e => set('any', e.target.value)} placeholder="2024" type="number" style={inputStyle} />
                </div>
                <div>
                  <label style={labelStyle}>Price (€)</label>
                  <input value={form.preu} onChange={e => set('preu', e.target.value)} placeholder="12.99" type="number" step="0.01" style={inputStyle} />
                </div>
                <div>
                  <label style={labelStyle}>Total pages</label>
                  <input value={form.pagines} onChange={e => set('pagines', e.target.value)} placeholder="320" type="number" style={inputStyle} />
                </div>
                <div>
                  <label style={labelStyle}>Pages read</label>
                  <input value={form.paginesLlegides} onChange={e => set('paginesLlegides', e.target.value)} placeholder="0" type="number" style={inputStyle} />
                </div>
              </div>

              <div>
                <label style={labelStyle}>Rating</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <StarRating value={form.valoracio} onChange={v => set('valoracio', v)} size={22} />
                  <span style={{ fontSize: 12, color: 'var(--text-muted)', fontFamily: 'Outfit, sans-serif' }}>({(form.valoracio || 0).toFixed(1)} / 10)</span>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <input type="checkbox" id="llegit-add" checked={form.llegit} onChange={e => set('llegit', e.target.checked)} style={{ width: 16, height: 16, cursor: 'pointer' }} />
                <label htmlFor="llegit-add" style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, cursor: 'pointer', color: 'var(--text)' }}>Mark as read</label>
              </div>

              <div>
                <label style={labelStyle}>Description</label>
                <textarea value={form.descripcio || ''} onChange={e => set('descripcio', e.target.value)} placeholder="Book description…" rows={3} style={{ ...inputStyle, resize: 'vertical', lineHeight: 1.5 }} />
              </div>

              <div>
                <label style={labelStyle}>Personal notes</label>
                <textarea value={form.notes || ''} onChange={e => set('notes', e.target.value)} placeholder="Your thoughts…" rows={2} style={{ ...inputStyle, resize: 'vertical', lineHeight: 1.5 }} />
              </div>

              {shelves.length > 0 && (
                <div>
                  <label style={labelStyle}>Add to shelf</label>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    {shelves.map(s => (
                      <button key={s.id} onClick={() => setShelfIds(ids => ids.includes(s.id) ? ids.filter(x => x !== s.id) : [...ids, s.id])} style={{
                        padding: '4px 12px', borderRadius: 20,
                        border: `1px solid ${shelfIds.includes(s.id) ? 'var(--accent)' : 'var(--border)'}`,
                        background: shelfIds.includes(s.id) ? 'oklch(96% 0.02 34)' : '#fff',
                        color: shelfIds.includes(s.id) ? 'var(--accent)' : 'var(--text-muted)',
                        fontFamily: 'Outfit, sans-serif', fontSize: 12,
                        cursor: 'pointer', transition: 'all 0.12s',
                      }}>{s.nom}</button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, padding: '14px 24px', borderTop: '1px solid var(--border)' }}>
          <button onClick={onClose} style={{ padding: '8px 18px', borderRadius: 7, border: '1px solid var(--border)', background: '#fff', fontFamily: 'Outfit, sans-serif', fontSize: 13, cursor: 'pointer', color: 'var(--text-muted)' }}>Cancel</button>
          {tab === 'manual' && (
            <button onClick={handleSave} disabled={!form.nom.trim() || saving} style={{
              padding: '8px 22px', borderRadius: 7, border: 'none',
              background: form.nom.trim() ? 'var(--accent)' : '#ccc',
              color: '#fff', fontFamily: 'Outfit, sans-serif', fontSize: 13, fontWeight: 600,
              cursor: form.nom.trim() ? 'pointer' : 'not-allowed',
            }}>{editing ? 'Save Changes' : 'Add Book'}</button>
          )}
        </div>
      </div>
    </div>
  );
};

// ── BookDetailPanel ───────────────────────────────────────────────────────────
const BookDetailPanel = ({ book, onClose, onSave, onDelete, shelves, shelfBooks, onToggleShelf }) => {
  const [form, setForm] = useState({ ...book });
  const [editing, setEditing] = useState(false);
  const [imgFailed, setImgFailed] = useState(false);

  useEffect(() => { setForm({ ...book }); setEditing(false); setImgFailed(false); }, [book.isbn]);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const save = () => { onSave({ ...form, any: parseInt(form.any) || null, preu: parseFloat(form.preu) || 0, pagines: parseInt(form.pagines) || 0, paginesLlegides: parseInt(form.paginesLlegides) || 0 }); setEditing(false); };
  const pct = (form.pagines > 0) ? Math.min(1, form.paginesLlegides / form.pagines) : 0;
  const bookShelves = shelves.filter(s => (shelfBooks[s.id] || []).includes(book.isbn));

  const inputStyle = {
    width: '100%', boxSizing: 'border-box',
    border: '1px solid var(--border)', borderRadius: 5,
    padding: '7px 10px', fontFamily: 'Outfit, sans-serif',
    fontSize: 13, color: 'var(--text)', background: '#fdfcfb', outline: 'none',
  };

  return (
    <>
      <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(20,16,12,0.35)', zIndex: 50 }} />
      <div style={{
        position: 'fixed', top: 0, right: 0, bottom: 0,
        width: 420, maxWidth: '95vw',
        background: '#fff', zIndex: 51,
        display: 'flex', flexDirection: 'column',
        boxShadow: '-6px 0 40px rgba(0,0,0,0.18)',
        animation: 'slideIn 0.22s ease-out',
      }}>
        {/* Cover header */}
        <div style={{ position: 'relative', height: 260, background: '#1a1714', flexShrink: 0, overflow: 'hidden' }}>
          {/* Blurred bg */}
          <div style={{
            position: 'absolute', inset: 0,
            backgroundImage: `url(${COVERS(book.isbn, 'M')})`,
            backgroundSize: 'cover', backgroundPosition: 'center',
            filter: 'blur(20px) brightness(0.4)',
            transform: 'scale(1.1)',
          }} />
          {/* Cover book */}
          <div style={{ position: 'absolute', bottom: 20, left: 24, width: 110, height: 165, borderRadius: 4, overflow: 'hidden', boxShadow: '0 8px 24px rgba(0,0,0,0.5)' }}>
            <CoverImage isbn={book.isbn} nom={book.nom} autor={book.autor} size="M" fullHeight />
          </div>
          {/* Close */}
          <button onClick={onClose} style={{ position: 'absolute', top: 14, right: 14, background: 'rgba(0,0,0,0.4)', border: 'none', borderRadius: '50%', width: 32, height: 32, color: '#fff', cursor: 'pointer', fontSize: 18, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>×</button>
          {/* Read badge */}
          <div style={{ position: 'absolute', top: 14, left: 14 }}>
            <span style={{
              padding: '3px 10px', borderRadius: 20,
              background: form.llegit ? 'rgba(45,158,107,0.85)' : 'rgba(100,100,100,0.6)',
              color: '#fff', fontSize: 11, fontFamily: 'Outfit, sans-serif', fontWeight: 500,
              backdropFilter: 'blur(4px)',
            }}>{form.llegit ? '✓ Read' : '○ Unread'}</span>
          </div>
          {/* Title overlay */}
          <div style={{ position: 'absolute', bottom: 20, left: 152, right: 16 }}>
            <div style={{ fontFamily: 'Lora, serif', fontStyle: 'italic', fontSize: 18, fontWeight: 600, color: '#fff', lineHeight: 1.3, textShadow: '0 1px 6px rgba(0,0,0,0.6)' }}>{book.nom}</div>
            <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'rgba(255,255,255,0.7)', marginTop: 4 }}>{book.autor}</div>
            {book.any && <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'rgba(255,255,255,0.5)', marginTop: 2 }}>{book.any}</div>}
          </div>
        </div>

        {/* Body */}
        <div style={{ flex: 1, overflow: 'auto', padding: '20px 24px' }}>
          {/* Rating */}
          <div style={{ marginBottom: 18 }}>
            <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 6 }}>Rating</div>
            <StarRating value={form.valoracio} onChange={editing ? v => set('valoracio', v) : undefined} size={24} readOnly={!editing} />
          </div>

          {/* Read toggle */}
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 18, cursor: 'pointer' }}>
            <div onClick={() => { set('llegit', !form.llegit); }} style={{
              width: 42, height: 24, borderRadius: 12,
              background: form.llegit ? 'var(--accent2)' : '#ddd',
              position: 'relative', transition: 'background 0.2s', cursor: 'pointer', flexShrink: 0,
            }}>
              <div style={{
                position: 'absolute', top: 3, left: form.llegit ? 21 : 3,
                width: 18, height: 18, borderRadius: '50%',
                background: '#fff', transition: 'left 0.2s',
                boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
              }} />
            </div>
            <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'var(--text)' }}>
              {form.llegit ? 'Marked as read' : 'Not yet read'}
            </span>
          </label>

          {/* Progress */}
          {(form.pagines > 0 || editing) && (
            <div style={{ marginBottom: 18 }}>
              <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 6 }}>Reading Progress</div>
              {editing ? (
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <input value={form.paginesLlegides} onChange={e => set('paginesLlegides', e.target.value)} type="number" style={{ ...inputStyle, width: 70 }} />
                  <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>/</span>
                  <input value={form.pagines} onChange={e => set('pagines', e.target.value)} type="number" style={{ ...inputStyle, width: 70 }} />
                  <span style={{ color: 'var(--text-muted)', fontSize: 13, fontFamily: 'Outfit, sans-serif' }}>pages</span>
                </div>
              ) : (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5 }}>
                    <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)' }}>{form.paginesLlegides} / {form.pagines} pages</span>
                    <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--accent)', fontWeight: 600 }}>{Math.round(pct * 100)}%</span>
                  </div>
                  <div style={{ height: 5, background: '#eee', borderRadius: 3 }}>
                    <div style={{ height: '100%', width: `${pct * 100}%`, background: 'var(--accent)', borderRadius: 3, transition: 'width 0.4s' }} />
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Description */}
          {(form.descripcio || editing) && (
            <div style={{ marginBottom: 18 }}>
              <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 6 }}>Description</div>
              {editing ? (
                <textarea value={form.descripcio || ''} onChange={e => set('descripcio', e.target.value)} rows={4} style={{ ...inputStyle, resize: 'vertical', lineHeight: 1.55 }} />
              ) : (
                <p style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'var(--text)', lineHeight: 1.65, margin: 0, textWrap: 'pretty' }}>{form.descripcio}</p>
              )}
            </div>
          )}

          {/* Notes */}
          <div style={{ marginBottom: 18 }}>
            <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 6 }}>Personal Notes</div>
            {editing ? (
              <textarea value={form.notes || ''} onChange={e => set('notes', e.target.value)} placeholder="Your thoughts on this book…" rows={3} style={{ ...inputStyle, resize: 'vertical', lineHeight: 1.55 }} />
            ) : (
              <p style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: form.notes ? 'var(--text)' : 'var(--text-muted)', fontStyle: form.notes ? 'normal' : 'italic', lineHeight: 1.65, margin: 0 }}>{form.notes || 'No notes yet.'}</p>
            )}
          </div>

          {/* Meta grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 18 }}>
            {[['ISBN', 'isbn'], ['Year', 'any'], ['Price (€)', 'preu']].map(([lbl, key]) => (
              <div key={key}>
                <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 4 }}>{lbl}</div>
                {editing ? (
                  <input value={form[key] || ''} onChange={e => set(key, e.target.value)} style={inputStyle} />
                ) : (
                  <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'var(--text)' }}>{form[key] || '—'}</div>
                )}
              </div>
            ))}
          </div>

          {/* Shelves */}
          {shelves.length > 0 && (
            <div style={{ marginBottom: 18 }}>
              <div style={{ fontSize: 10, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 6 }}>Shelves</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {shelves.map(s => {
                  const onShelf = (shelfBooks[s.id] || []).includes(book.isbn);
                  return (
                    <button key={s.id} onClick={() => onToggleShelf(book.isbn, s.id)} style={{
                      padding: '3px 11px', borderRadius: 20,
                      border: `1px solid ${onShelf ? 'var(--accent)' : 'var(--border)'}`,
                      background: onShelf ? 'oklch(96% 0.02 34)' : '#fff',
                      color: onShelf ? 'var(--accent)' : 'var(--text-muted)',
                      fontFamily: 'Outfit, sans-serif', fontSize: 12,
                      cursor: 'pointer', transition: 'all 0.12s',
                    }}>{s.nom}</button>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div style={{ display: 'flex', gap: 8, padding: '14px 24px', borderTop: '1px solid var(--border)' }}>
          <button onClick={() => { if (window.confirm('Delete this book?')) onDelete(book.isbn); }} style={{
            padding: '8px 14px', borderRadius: 7,
            border: '1px solid #fcc', background: '#fff8f8',
            color: '#c0392b', fontFamily: 'Outfit, sans-serif', fontSize: 13, cursor: 'pointer',
          }}>Delete</button>
          <div style={{ flex: 1 }} />
          {editing ? (
            <>
              <button onClick={() => { setForm({ ...book }); setEditing(false); }} style={{ padding: '8px 16px', borderRadius: 7, border: '1px solid var(--border)', background: '#fff', fontFamily: 'Outfit, sans-serif', fontSize: 13, cursor: 'pointer', color: 'var(--text-muted)' }}>Cancel</button>
              <button onClick={save} style={{ padding: '8px 20px', borderRadius: 7, border: 'none', background: 'var(--accent)', color: '#fff', fontFamily: 'Outfit, sans-serif', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}>Save</button>
            </>
          ) : (
            <button onClick={() => setEditing(true)} style={{ padding: '8px 20px', borderRadius: 7, border: '1px solid var(--border)', background: '#fff', fontFamily: 'Outfit, sans-serif', fontSize: 13, fontWeight: 500, cursor: 'pointer', color: 'var(--text)' }}>Edit</button>
          )}
        </div>
      </div>
      <style>{`@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }`}</style>
    </>
  );
};

// ── StatsModal ────────────────────────────────────────────────────────────────
const StatsModal = ({ books, shelves, shelfBooks, onClose }) => {
  const totalRead = books.filter(b => b.llegit).length;
  const avgRating = books.length ? (books.reduce((s, b) => s + (b.valoracio || 0), 0) / books.length).toFixed(1) : '—';
  const totalPages = books.reduce((s, b) => s + (b.pagines || 0), 0);
  const pagesRead = books.reduce((s, b) => s + (b.paginesLlegides || 0), 0);
  const topGenres = shelves.map(s => ({ nom: s.nom, count: (shelfBooks[s.id] || []).length })).sort((a, b) => b.count - a.count);

  const statCard = (label, value, sub) => (
    <div style={{ background: '#faf8f5', borderRadius: 8, padding: '16px 18px', border: '1px solid var(--border)' }}>
      <div style={{ fontFamily: 'Lora, serif', fontSize: 28, fontWeight: 600, color: 'var(--accent)', lineHeight: 1 }}>{value}</div>
      <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'var(--text)', fontWeight: 500, marginTop: 4 }}>{label}</div>
      {sub && <div style={{ fontFamily: 'Outfit, sans-serif', fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>{sub}</div>}
    </div>
  );

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(20,16,12,0.55)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center' }} onClick={e => e.target === e.currentTarget && onClose()}>
      <div style={{ background: '#fff', borderRadius: 12, width: 520, maxWidth: '95vw', maxHeight: '90vh', overflow: 'auto', boxShadow: '0 20px 60px rgba(0,0,0,0.25)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '22px 24px 0' }}>
          <h2 style={{ fontFamily: 'Lora, serif', fontSize: 22, fontWeight: 600, margin: 0, color: 'var(--text)' }}>Library Statistics</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 22, color: 'var(--text-muted)' }}>×</button>
        </div>
        <div style={{ padding: '20px 24px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
            {statCard('Books total', books.length)}
            {statCard('Books read', totalRead, `${books.length > 0 ? Math.round(totalRead / books.length * 100) : 0}% of library`)}
            {statCard('Avg. rating', avgRating, 'out of 10')}
            {statCard('Total pages', totalPages.toLocaleString())}
            {statCard('Pages read', pagesRead.toLocaleString(), `${totalPages > 0 ? Math.round(pagesRead / totalPages * 100) : 0}% coverage`)}
            {statCard('Shelves', shelves.length)}
          </div>

          {/* Read progress bar */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
              <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, fontWeight: 500, color: 'var(--text)' }}>Reading progress</span>
              <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 13, color: 'var(--accent)', fontWeight: 600 }}>{totalRead} / {books.length}</span>
            </div>
            <div style={{ height: 8, background: '#eee', borderRadius: 4 }}>
              <div style={{ height: '100%', width: `${books.length > 0 ? (totalRead / books.length) * 100 : 0}%`, background: 'var(--accent2)', borderRadius: 4, transition: 'width 0.5s' }} />
            </div>
          </div>

          {/* Per-shelf */}
          {topGenres.length > 0 && (
            <div>
              <div style={{ fontSize: 11, fontFamily: 'Outfit, sans-serif', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginBottom: 10 }}>Books per shelf</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {topGenres.map(g => (
                  <div key={g.nom}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
                      <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text)' }}>{g.nom}</span>
                      <span style={{ fontFamily: 'Outfit, sans-serif', fontSize: 12, color: 'var(--text-muted)' }}>{g.count}</span>
                    </div>
                    <div style={{ height: 4, background: '#eee', borderRadius: 2 }}>
                      <div style={{ height: '100%', width: `${books.length > 0 ? (g.count / books.length) * 100 : 0}%`, background: 'var(--accent)', borderRadius: 2 }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// Export everything
Object.assign(window, {
  bookHue, COVERS, fetchByISBN, searchByTitle,
  CoverImage, StarRating, BookCard, BookRow,
  Sidebar, SidebarRow, TopBar,
  AddBookModal, BookDetailPanel, StatsModal,
});
