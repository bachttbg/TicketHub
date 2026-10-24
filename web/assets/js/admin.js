/**
 * TicketHub Admin – tiện ích dùng chung cho mọi trang quản trị.
 * Toast, modal, hộp xác nhận, gọi API JSON, định dạng tiền, đếm số, thanh bên (mobile).
 */
(function () {
    'use strict';
    var A = window.A = {};
    A.ctx = document.body.getAttribute('data-ctx') || '';

    // ---------- Tiện ích ----------
    A.esc = function (s) {
        return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    };
    A.vnd = function (n) { return new Intl.NumberFormat('vi-VN').format(Math.round(Number(n) || 0)) + 'đ'; };
    A.short = function (n) {
        n = Number(n) || 0;
        var f = function (v) { return (Math.round(v * 10) / 10).toString().replace('.', ','); };
        if (Math.abs(n) >= 1e9) return f(n / 1e9) + ' tỷ';
        if (Math.abs(n) >= 1e6) return f(n / 1e6) + ' tr';
        if (Math.abs(n) >= 1e3) return f(n / 1e3) + 'k';
        return String(Math.round(n));
    };
    A.debounce = function (fn, ms) {
        var t; return function () { var a = arguments, c = this; clearTimeout(t); t = setTimeout(function () { fn.apply(c, a); }, ms || 300); };
    };
    A.$ = function (s, r) { return (r || document).querySelector(s); };
    A.$$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

    // ---------- Toast ----------
    A.toast = function (msg, type) {
        var box = document.getElementById('toasts');
        if (!box) { box = document.createElement('div'); box.id = 'toasts'; document.body.appendChild(box); }
        var t = document.createElement('div');
        t.className = 'toast' + (type === 'err' ? ' err' : '');
        t.innerHTML = '<span class="ic">' + (type === 'err' ? '⚠️' : '✅') + '</span><span>' + A.esc(msg) + '</span>';
        box.appendChild(t);
        var life = type === 'err' ? 5200 : 3200;
        setTimeout(function () { t.classList.add('out'); setTimeout(function () { t.remove(); }, 260); }, life);
    };

    // ---------- Modal ----------
    A.open = function (id) {
        var m = document.getElementById(id);
        if (!m) return;
        m.classList.remove('out');
        m.classList.add('on');
        document.body.style.overflow = 'hidden';
        var f = m.querySelector('[autofocus], .inp');
        if (f && !('ontouchstart' in window)) setTimeout(function () { f.focus(); }, 120);
    };
    A.close = function (id) {
        var m = typeof id === 'string' ? document.getElementById(id) : id;
        if (!m || !m.classList.contains('on')) return;
        m.classList.add('out');
        setTimeout(function () {
            m.classList.remove('on', 'out');
            if (!document.querySelector('.back.on')) document.body.style.overflow = '';
        }, 180);
    };
    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') return;
        var open = A.$$('.back.on');
        if (open.length) A.close(open[open.length - 1]);
    });
    document.addEventListener('mousedown', function (e) {
        if (e.target.classList && e.target.classList.contains('back') && !e.target.hasAttribute('data-static')) A.close(e.target);
    });
    document.addEventListener('click', function (e) {
        var c = e.target.closest ? e.target.closest('[data-close]') : null;
        if (c) { var b = c.closest('.back'); if (b) A.close(b); }
    });

    // ---------- Hộp xác nhận ----------
    A.confirm = function (o) {
        return new Promise(function (resolve) {
            var el = document.createElement('div');
            el.className = 'back on';
            el.style.zIndex = 300;
            el.innerHTML =
                '<div class="modal sm"><div class="modal-h" style="background:' + (o.danger ? 'var(--pnk)' : 'var(--yel)') + '"><h2>' + A.esc(o.title || 'Xác nhận') + '</h2></div>' +
                '<div class="modal-b"><p style="margin:0;font-weight:600;line-height:1.55">' + (o.html || A.esc(o.text || '')) + '</p></div>' +
                '<div class="modal-f"><button class="btn" data-r="0">' + A.esc(o.cancel || 'Hủy') + '</button>' +
                '<button class="btn ' + (o.danger ? 'btn-danger' : 'btn-primary') + '" data-r="1">' + A.esc(o.ok || 'Đồng ý') + '</button></div></div>';
            document.body.appendChild(el);
            document.body.style.overflow = 'hidden';
            function done(v) {
                el.classList.add('out');
                setTimeout(function () { el.remove(); if (!document.querySelector('.back.on')) document.body.style.overflow = ''; }, 180);
                document.removeEventListener('keydown', key, true);
                resolve(v);
            }
            function key(e) { if (e.key === 'Escape') { e.stopPropagation(); done(false); } }
            document.addEventListener('keydown', key, true);
            el.addEventListener('click', function (e) {
                var b = e.target.closest('[data-r]');
                if (b) done(b.getAttribute('data-r') === '1');
                else if (e.target === el) done(false);
            });
            var ok = el.querySelector('[data-r="1"]'); if (ok) ok.focus();
        });
    };

    // ---------- Gọi API ----------
    A.request = function (url, opts) {
        opts = opts || {};
        opts.credentials = 'same-origin';
        opts.headers = Object.assign({ 'X-Requested-With': 'fetch' }, opts.headers || {});
        return fetch(url, opts).then(function (res) {
            return res.text().then(function (txt) {
                var data = null;
                try { data = JSON.parse(txt); } catch (e) { /* không phải JSON */ }
                if (res.status === 401) {
                    A.toast('Phiên đăng nhập đã hết hạn, đang chuyển về trang đăng nhập...', 'err');
                    setTimeout(function () { location.href = A.ctx + '/home?login=1&next=' + encodeURIComponent(location.pathname.replace(A.ctx, '')); }, 1200);
                    throw new Error('Hết phiên đăng nhập');
                }
                if (!data) throw new Error('Máy chủ phản hồi không hợp lệ (' + res.status + ')');
                if (data.success === false) { var er = new Error(data.message || 'Có lỗi xảy ra'); er.data = data; throw er; }
                return data;
            });
        });
    };
    A.get = function (url) { return A.request(url); };
    A.post = function (url, data) {
        var body;
        if (data instanceof FormData) {
            // Chỉ gửi multipart khi thật sự có file tải lên; còn lại gửi dạng form thường
            var hasFile = false;
            data.forEach(function (v) { if (typeof File !== 'undefined' && v instanceof File && v.size > 0) hasFile = true; });
            if (hasFile) body = data;
            else {
                body = new URLSearchParams();
                data.forEach(function (v, k) { if (typeof v === 'string') body.append(k, v); });
            }
        } else {
            body = new URLSearchParams();
            Object.keys(data || {}).forEach(function (k) { if (data[k] != null) body.append(k, data[k]); });
        }
        return A.request(url, { method: 'POST', body: body });
    };

    /** Chạy 1 thao tác bất đồng bộ cho nút: hiện loading, báo lỗi bằng toast. */
    A.run = function (btn, fn, okMsg) {
        if (btn) { if (btn.classList.contains('loading')) return Promise.resolve(); btn.classList.add('loading'); }
        return Promise.resolve().then(fn).then(function (r) {
            if (okMsg !== false) A.toast(okMsg || (r && r.message) || 'Thành công');
            return r;
        }).catch(function (e) {
            A.toast(e.message || 'Có lỗi xảy ra', 'err');
            throw e;
        }).finally(function () { if (btn) btn.classList.remove('loading'); });
    };

    A.go = function (url, ms) { setTimeout(function () { location.href = url; }, ms || 600); };

    // ---------- Đếm số (hiệu ứng) ----------
    A.animateNumber = function (el) {
        var txt = (el.textContent || '').trim();
        var m = txt.match(/^(-?[\d.,]+)(.*)$/);
        if (!m) return;
        var raw = m[1], suffix = m[2], decimals = 0, val;
        if (/^\d{1,3}(\.\d{3})+$/.test(raw)) { val = parseFloat(raw.replace(/\./g, '')); }
        else if (/,/.test(raw)) { var p = raw.replace(/\./g, '').split(','); decimals = (p[1] || '').length; val = parseFloat(p[0] + '.' + (p[1] || '0')); }
        else { val = parseFloat(raw); }
        if (!isFinite(val) || val === 0) return;
        var thousands = /^\d{1,3}(\.\d{3})+$/.test(raw);
        var t0 = null, dur = 900;
        function fmt(v) {
            if (thousands) return new Intl.NumberFormat('vi-VN').format(Math.round(v));
            return decimals ? v.toFixed(decimals).replace('.', ',') : String(Math.round(v));
        }
        function step(ts) {
            if (!t0) t0 = ts;
            var p = Math.min(1, (ts - t0) / dur), e = 1 - Math.pow(1 - p, 3);
            el.textContent = fmt(val * e) + suffix;
            if (p < 1) requestAnimationFrame(step); else el.textContent = txt;
        }
        requestAnimationFrame(step);
    };

    // ---------- Thanh bên (mobile) ----------
    document.addEventListener('click', function (e) {
        var b = e.target.closest ? e.target.closest('[data-burger]') : null;
        var side = document.querySelector('.side'), scrim = document.querySelector('.scrim');
        if (b && side) { side.classList.toggle('open'); if (scrim) scrim.classList.toggle('on'); }
        else if (e.target.classList && e.target.classList.contains('scrim')) { side.classList.remove('open'); scrim.classList.remove('on'); }
    });

    // ---------- Tabs trong trang ----------
    A.tabs = function (root) {
        root = root || document;
        A.$$('.tabs', root).forEach(function (tabs) {
            tabs.addEventListener('click', function (e) {
                var b = e.target.closest('button[data-tab]'); if (!b) return;
                A.$$('button', tabs).forEach(function (x) { x.classList.toggle('on', x === b); });
                A.$$('.tab-pane').forEach(function (p) { p.classList.toggle('on', p.id === 'tab-' + b.getAttribute('data-tab')); });
                try { history.replaceState(null, '', '?tab=' + b.getAttribute('data-tab')); } catch (er) {}
            });
        });
    };

    // ---------- Khởi tạo ----------
    document.addEventListener('DOMContentLoaded', function () {
        A.$$('[data-animate-num]').forEach(A.animateNumber);
        A.$$('.bar > i[data-w]').forEach(function (i) { setTimeout(function () { i.style.width = i.getAttribute('data-w') + '%'; }, 120); });
        A.tabs();
    });
})();
