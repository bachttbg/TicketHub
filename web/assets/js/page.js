/**
 * TicketHub - hiệu ứng chuyển trang dùng chung (trang chủ, dashboard, đơn hàng)
 *  - Hiện dần các khối khi cuộn tới (.reveal)
 *  - Làm mờ nhẹ khi bấm link nội bộ để chuyển trang mượt, không bị "giật"
 */
(function () {
    'use strict';

    // 1) Hiện dần khi cuộn tới
    function initReveal() {
        var items = document.querySelectorAll('.reveal');
        if (!items.length) return;
        if (!('IntersectionObserver' in window)) {
            items.forEach(function (el) { el.classList.add('in'); });
            return;
        }
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (en) {
                if (en.isIntersecting) {
                    en.target.classList.add('in');
                    io.unobserve(en.target);
                }
            });
        }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });
        items.forEach(function (el) { io.observe(el); });
        // Phòng hờ: sau 2.5s nếu còn phần tử chưa hiện thì hiện hết (tránh trang trống)
        setTimeout(function () {
            document.querySelectorAll('.reveal:not(.in)').forEach(function (el) {
                var r = el.getBoundingClientRect();
                if (r.top < window.innerHeight) el.classList.add('in');
            });
        }, 2500);
    }

    // 2) Chuyển trang mượt
    function isInternalNav(a, e) {
        if (!a || e.defaultPrevented) return false;
        if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return false;
        var href = a.getAttribute('href');
        if (!href || href.charAt(0) === '#' || href.indexOf('javascript:') === 0 || href.indexOf('mailto:') === 0) return false;
        if (a.target && a.target !== '_self') return false;
        if (a.hasAttribute('download')) return false;
        var url;
        try { url = new URL(a.href, location.href); } catch (err) { return false; }
        if (url.origin !== location.origin) return false;
        // cùng trang, chỉ khác phần #hash -> để trình duyệt tự cuộn mượt
        if (url.pathname === location.pathname && url.search === location.search) return false;
        return true;
    }

    document.addEventListener('click', function (e) {
        var a = e.target.closest ? e.target.closest('a[href]') : null;
        if (!isInternalNav(a, e)) return;
        e.preventDefault();
        document.body.classList.add('leaving');
        var dest = a.href;
        setTimeout(function () { window.location.href = dest; }, 180);
    });

    // Quay lại bằng nút Back (bfcache) thì bỏ trạng thái mờ
    window.addEventListener('pageshow', function () {
        document.body.classList.remove('leaving');
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initReveal);
    } else {
        initReveal();
    }
})();
