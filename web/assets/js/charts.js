/**
 * TicketHub Charts – biểu đồ SVG nhẹ, không phụ thuộc thư viện ngoài.
 *  Charts.line(el, cfg)   : đường cong mượt + vùng tô + tooltip khi rê chuột
 *  Charts.donut(el, items, opts)
 */
(function () {
    'use strict';
    var NS = 'http://www.w3.org/2000/svg';
    var INK = '#111';

    function svg(tag, attrs, parent) {
        var e = document.createElementNS(NS, tag);
        Object.keys(attrs || {}).forEach(function (k) { e.setAttribute(k, attrs[k]); });
        if (parent) parent.appendChild(e);
        return e;
    }

    function niceMax(v) {
        if (v <= 0) return 4;
        var p = Math.pow(10, Math.floor(Math.log10(v)));
        var n = v / p;
        var m = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
        return m * p;
    }

    /** Nội suy đơn điệu (Fritsch–Carlson): đường cong mượt, không vọt xuống dưới 0. */
    function curve(pts) {
        var n = pts.length;
        if (n < 2) return 'M' + pts[0][0] + ',' + pts[0][1];
        var dx = [], dy = [], m = [], t = [];
        for (var i = 0; i < n - 1; i++) { dx[i] = pts[i + 1][0] - pts[i][0]; dy[i] = pts[i + 1][1] - pts[i][1]; m[i] = dy[i] / dx[i]; }
        t[0] = m[0]; t[n - 1] = m[n - 2];
        for (i = 1; i < n - 1; i++) t[i] = m[i - 1] * m[i] <= 0 ? 0 : (m[i - 1] + m[i]) / 2;
        for (i = 0; i < n - 1; i++) {
            if (m[i] === 0) { t[i] = 0; t[i + 1] = 0; continue; }
            var a = t[i] / m[i], b = t[i + 1] / m[i], s = a * a + b * b;
            if (s > 9) { var k = 3 / Math.sqrt(s); t[i] = k * a * m[i]; t[i + 1] = k * b * m[i]; }
        }
        var d = 'M' + pts[0][0] + ',' + pts[0][1];
        for (i = 0; i < n - 1; i++) {
            var x1 = pts[i][0] + dx[i] / 3, y1 = pts[i][1] + t[i] * dx[i] / 3;
            var x2 = pts[i + 1][0] - dx[i] / 3, y2 = pts[i + 1][1] - t[i + 1] * dx[i] / 3;
            d += ' C' + x1 + ',' + y1 + ' ' + x2 + ',' + y2 + ' ' + pts[i + 1][0] + ',' + pts[i + 1][1];
        }
        return d;
    }

    var Charts = window.Charts = {};

    /**
     * cfg: { labels:[], series:{ key:{name,color,data:[],fmt:fn,axis:fn} }, active:'revenue', height:300 }
     * Trả về { show(key) } để đổi chuỗi dữ liệu đang hiển thị.
     */
    Charts.line = function (el, cfg) {
        var active = cfg.active, H = cfg.height || 300;
        var tip = document.createElement('div'); tip.className = 'tip';
        var raf = 0;

        function draw(animate) {
            el.innerHTML = '';
            el.appendChild(tip);
            var W = Math.max(280, el.clientWidth);
            var s = cfg.series[active];
            var data = s.data, n = data.length;
            var padL = 54, padR = 16, padT = 14, padB = 30;
            var iw = W - padL - padR, ih = H - padT - padB;
            var max = niceMax(Math.max.apply(null, data.concat([1])));
            var root = svg('svg', { viewBox: '0 0 ' + W + ' ' + H, height: H, role: 'img' }, el);
            var defs = svg('defs', {}, root);
            var gid = 'g' + Math.random().toString(36).slice(2, 8);
            var g = svg('linearGradient', { id: gid, x1: 0, y1: 0, x2: 0, y2: 1 }, defs);
            svg('stop', { offset: '0%', 'stop-color': s.color, 'stop-opacity': .55 }, g);
            svg('stop', { offset: '100%', 'stop-color': s.color, 'stop-opacity': .02 }, g);

            // lưới + trục Y
            for (var i = 0; i <= 4; i++) {
                var y = padT + ih - ih * i / 4;
                svg('line', { x1: padL, x2: W - padR, y1: y, y2: y, stroke: i === 0 ? INK : '#d9dce6', 'stroke-width': i === 0 ? 2.5 : 1.5, 'stroke-dasharray': i === 0 ? '' : '4 5' }, root);
                var t = svg('text', { x: padL - 10, y: y + 4, 'text-anchor': 'end', 'font-size': 11.5, 'font-weight': 700, fill: '#5d6370' }, root);
                t.textContent = (s.axis || A.short)(max * i / 4);
            }
            // nhãn trục X
            var step = Math.max(1, Math.ceil(n / Math.max(2, Math.floor(iw / 62))));
            var xs = [];
            for (i = 0; i < n; i++) {
                var x = n === 1 ? padL + iw / 2 : padL + iw * i / (n - 1);
                xs.push(x);
                if (i % step === 0 || i === n - 1) {
                    if (i === n - 1 && i % step !== 0 && (n - 1 - (i - i % step)) < step * .6) continue;
                    var tx = svg('text', { x: x, y: H - 8, 'text-anchor': 'middle', 'font-size': 11.5, 'font-weight': 700, fill: '#5d6370' }, root);
                    tx.textContent = cfg.labels[i];
                }
            }
            var pts = data.map(function (v, k) { return [xs[k], padT + ih - ih * (v / max)]; });
            var line = curve(pts);
            var area = svg('path', { d: line + ' L' + xs[n - 1] + ',' + (padT + ih) + ' L' + xs[0] + ',' + (padT + ih) + ' Z', fill: 'url(#' + gid + ')' }, root);
            var path = svg('path', { d: line, fill: 'none', stroke: INK, 'stroke-width': 3.5, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, root);
            var path2 = svg('path', { d: line, fill: 'none', stroke: s.color, 'stroke-width': 1.6, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, root);
            if (animate !== false && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                var len = path.getTotalLength ? path.getTotalLength() : 2000;
                [path, path2].forEach(function (p) { p.style.strokeDasharray = len; p.style.strokeDashoffset = len; p.style.animation = 'drawLine 1s ease forwards'; });
                area.style.animation = 'fadeArea 1s ease both';
            }
            // điểm đánh dấu khi hover
            var cross = svg('line', { y1: padT, y2: padT + ih, stroke: INK, 'stroke-width': 2, 'stroke-dasharray': '4 4', opacity: 0 }, root);
            var dot = svg('circle', { r: 7, fill: s.color, stroke: INK, 'stroke-width': 3, opacity: 0 }, root);
            var hit = svg('rect', { x: padL, y: padT, width: iw, height: ih, fill: 'transparent' }, root);

            function onMove(ev) {
                var rect = root.getBoundingClientRect();
                var cx = (ev.touches ? ev.touches[0].clientX : ev.clientX) - rect.left;
                var idx = 0, best = 1e9;
                for (var k = 0; k < n; k++) { var d = Math.abs(xs[k] - cx); if (d < best) { best = d; idx = k; } }
                cross.setAttribute('x1', xs[idx]); cross.setAttribute('x2', xs[idx]); cross.setAttribute('opacity', 1);
                dot.setAttribute('cx', xs[idx]); dot.setAttribute('cy', pts[idx][1]); dot.setAttribute('opacity', 1);
                var rows = Object.keys(cfg.series).map(function (key) {
                    var o = cfg.series[key];
                    return '<div style="display:flex;justify-content:space-between;gap:14px;opacity:' + (key === active ? 1 : .7) + '"><span>' + A.esc(o.name) + '</span><span>' + A.esc((o.fmt || String)(o.data[idx])) + '</span></div>';
                }).join('');
                tip.innerHTML = '<span style="color:#c8f51f;font-size:12px">' + A.esc(cfg.labels[idx]) + '</span>' + rows;
                tip.classList.add('on');
                var left = xs[idx] + 16;
                if (left + 170 > W) left = xs[idx] - 170 - 16;
                tip.style.left = Math.max(4, left) + 'px';
                tip.style.top = Math.max(0, pts[idx][1] - 30) + 'px';
            }
            function onLeave() { cross.setAttribute('opacity', 0); dot.setAttribute('opacity', 0); tip.classList.remove('on'); }
            hit.addEventListener('mousemove', onMove);
            hit.addEventListener('touchmove', onMove, { passive: true });
            hit.addEventListener('touchstart', onMove, { passive: true });
            hit.addEventListener('mouseleave', onLeave);
        }

        draw(true);
        if ('ResizeObserver' in window) {
            var last = el.clientWidth;
            new ResizeObserver(function () {
                if (Math.abs(el.clientWidth - last) < 2) return;
                last = el.clientWidth;
                cancelAnimationFrame(raf);
                raf = requestAnimationFrame(function () { draw(false); });
            }).observe(el);
        }
        return { show: function (key) { active = key; draw(true); } };
    };

    /** items: [{name,value,color}]. legendEl: phần tử chứa chú thích. */
    Charts.donut = function (el, legendEl, items, opts) {
        opts = opts || {};
        var total = items.reduce(function (a, b) { return a + b.value; }, 0);
        el.innerHTML = '';
        var S = 170, R = 62, SW = 30, C = S / 2;
        var root = svg('svg', { viewBox: '0 0 ' + S + ' ' + S, width: S, height: S }, el);
        root.style.overflow = 'visible';
        svg('circle', { cx: C, cy: C, r: R, fill: 'none', stroke: '#eceef4', 'stroke-width': SW }, root);
        var circ = 2 * Math.PI * R, off = 0;
        var gap = items.filter(function (i) { return i.value > 0; }).length > 1 ? 3 : 0;
        items.forEach(function (it, k) {
            if (!it.value || total <= 0) return;
            var len = circ * it.value / total;
            var arc = svg('circle', {
                cx: C, cy: C, r: R, fill: 'none', stroke: it.color, 'stroke-width': SW,
                'stroke-dasharray': Math.max(0, len - gap) + ' ' + circ, 'stroke-dashoffset': -off,
                transform: 'rotate(-90 ' + C + ' ' + C + ')'
            }, root);
            arc.style.transition = 'stroke-width .2s';
            arc.addEventListener('mouseenter', function () { arc.setAttribute('stroke-width', SW + 6); });
            arc.addEventListener('mouseleave', function () { arc.setAttribute('stroke-width', SW); });
            var title = svg('title', {}, arc); title.textContent = it.name + ': ' + it.value + ' (' + Math.round(it.value * 100 / total) + '%)';
            off += len;
        });
        // viền mực
        svg('circle', { cx: C, cy: C, r: R + SW / 2, fill: 'none', stroke: INK, 'stroke-width': 2.5 }, root);
        svg('circle', { cx: C, cy: C, r: R - SW / 2, fill: 'none', stroke: INK, 'stroke-width': 2.5 }, root);
        var t1 = svg('text', { x: C, y: C - 2, 'text-anchor': 'middle', 'font-size': 24, 'font-weight': 800, fill: INK }, root);
        t1.textContent = opts.center != null ? opts.center : total;
        var t2 = svg('text', { x: C, y: C + 16, 'text-anchor': 'middle', 'font-size': 11, 'font-weight': 700, fill: '#5d6370' }, root);
        t2.textContent = opts.caption || '';
        if (legendEl) {
            legendEl.innerHTML = items.map(function (it) {
                var p = total > 0 ? Math.round(it.value * 100 / total) : 0;
                return '<span><span><i style="background:' + it.color + '"></i>' + A.esc(it.name) + '</span><b>' + it.value + ' · ' + p + '%</b></span>';
            }).join('');
        }
    };
})();
