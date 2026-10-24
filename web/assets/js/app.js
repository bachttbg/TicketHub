/**
 * TicketHub Front-end Logic - MVC Integration (Servlets + SQL Server)
 */

var currentEvent = null;
var currentTicketTypes = [];
var selectedQuantities = {};
var discountRate = 0;
var paymentMethod = 'bank';

var tt;
function toast(m) {
    var t = document.getElementById('toast');
    if (!t) return;
    t.textContent = m;
    t.classList.add('on');
    clearTimeout(tt);
    tt = setTimeout(function() {
        t.classList.remove('on');
    }, 1800);
}

// Tickie Mascot 3D Tilt & Eye Tracking
var tilt = document.getElementById('tilt');
var pupils = document.querySelectorAll('.pupil');
var eyes = document.getElementById('eyes');
var say = document.getElementById('say');
var svgElem = document.querySelector('.stage svg');
var hold = false;
var fe = false;

function msg(x) {
    if (say) say.textContent = x;
}

function look(dx, dy) {
    if (pupils) {
        pupils.forEach(function(p) {
            p.style.transform = 'translate(' + (dx * 7 - 2) + 'px,' + (dy * 6 - 2) + 'px)';
        });
    }
}

if (svgElem) {
    // Gom các sự kiện chuột vào 1 lần cập nhật / khung hình để mascot không bị giật
    var tiltRaf = 0, lastPointer = null;
    window.addEventListener('pointermove', function(e) {
        lastPointer = e;
        if (tiltRaf) return;
        tiltRaf = requestAnimationFrame(function() {
            tiltRaf = 0;
            if (!tilt || !lastPointer) return;
            var r = svgElem.getBoundingClientRect();
            var dx = Math.max(-1, Math.min(1, (lastPointer.clientX - r.left - r.width / 2) / (window.innerWidth / 2)));
            var dy = Math.max(-1, Math.min(1, (lastPointer.clientY - r.top - r.height / 2) / (window.innerHeight / 2)));
            tilt.style.transform = 'rotateY(' + (dx * 20) + 'deg) rotateX(' + (-dy * 12) + 'deg)';
            if (!fe && !hold) look(dx, dy);
        });
    }, { passive: true });
}

setInterval(function() {
    if (hold || !eyes) return;
    eyes.classList.add('blink');
    setTimeout(function() {
        if (!hold && eyes) eyes.classList.remove('blink');
    }, 140);
}, 3200);

// Stage Slot Helpers
var stage = document.querySelector('.stage');
var heroSlot = document.getElementById('heroSlot');

function resetMascot() {
    hold = fe = false;
    if (eyes) eyes.classList.remove('blink');
}

// Chuyển mascot sang khung khác kèm hiệu ứng "nảy vào" (tránh bị nhảy cóc / mất chuyển động)
function moveStage(slot) {
    if (!slot || !stage || stage.parentNode === slot) return;
    slot.appendChild(stage);
    stage.classList.remove('arrive');
    void stage.offsetWidth;
    stage.classList.add('arrive');
    setTimeout(function() { stage.classList.remove('arrive'); }, 600);
}

var closeTimer = null;
function cancelClose() {
    clearTimeout(closeTimer);
    ['login', 'reg', 'pay'].forEach(function(id) {
        var m = document.getElementById(id);
        if (m) m.classList.remove('out');
    });
}

function closeL() {
    var open = ['login', 'reg', 'pay'].map(function(id) {
        return document.getElementById(id);
    }).filter(function(m) { return m && m.classList.contains('on'); });
    if (!open.length) return;

    // Hiệu ứng đóng 0.22s rồi mới ẩn hẳn
    open.forEach(function(m) { m.classList.add('out'); });
    clearTimeout(closeTimer);
    closeTimer = setTimeout(function() {
        open.forEach(function(m) { m.classList.remove('on', 'out'); });
        document.body.style.overflow = '';
        moveStage(heroSlot);
        resetMascot();
        msg('Mua vé nào!');
    }, 220);
}

function openL() {
    cancelClose();
    var loginModal = document.getElementById('login');
    var regModal = document.getElementById('reg');
    if (regModal) regModal.classList.remove('on');
    if (loginModal) loginModal.classList.add('on');
    document.body.style.overflow = 'hidden';
    moveStage(document.getElementById('slot'));
    resetMascot();
    msg('Đăng nhập nào!');
}

function openR() {
    cancelClose();
    var loginModal = document.getElementById('login');
    var regModal = document.getElementById('reg');
    if (loginModal) loginModal.classList.remove('on');
    if (regModal) {
        regModal.classList.add('on');
        regModal.scrollTop = 0;
    }
    document.body.style.overflow = 'hidden';
    moveStage(document.getElementById('slot2'));
    resetMascot();
    msg('Tham gia nhé!');
}

var btnNl = document.getElementById('nl');
if (btnNl) btnNl.onclick = openL;

var btnRegNav = document.getElementById('btnRegisterNav');
if (btnRegNav) btnRegNav.onclick = openR;

var btnCl = document.getElementById('cl');
if (btnCl) btnCl.onclick = closeL;

var btnCr = document.getElementById('cr');
if (btnCr) btnCr.onclick = closeL;

var btnCx = document.getElementById('cx');
if (btnCx) btnCx.onclick = closeL;

var btnHm = document.getElementById('hm');
if (btnHm) btnHm.onclick = closeL;

window.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeL();
});

// ĐIỀU HƯỚNG SAU KHI ĐĂNG NHẬP / ĐĂNG KÝ
// Nếu bị chuyển từ Dashboard/Đơn hàng về đây (?login=1&next=/dashboard) thì quay lại đúng trang đó.
function getNextUrl() {
    var n = new URLSearchParams(window.location.search).get('next');
    if (n && /^\/(?![\/\\])/.test(n)) return window.APP_CONTEXT + n;
    return null;
}

function goAfterAuth(data) {
    var next = getNextUrl();
    // Tài khoản khách hàng không có quyền vào khu vực quản trị -> bỏ qua trang "next" đó
    if (next && /\/(dashboard|admin\/)/.test(next) && !(data && data.role === 'ADMIN')) next = null;
    var dest = next || (data && data.redirect) || '';
    if (!dest && window.location.search.indexOf('login=') > -1) {
        dest = window.APP_CONTEXT + '/home';
    }
    if (dest) {
        window.location.href = dest;
    } else {
        window.location.reload();
    }
}

// LOGIN LOGIC
var em = document.getElementById('em');
var pw = document.getElementById('pw');
var tg = document.getElementById('tg');

if (em) {
    em.addEventListener('focus', function() {
        fe = true; hold = false;
        if (eyes) eyes.classList.remove('blink');
        look(-0.6, 0.8);
        msg('Email nào?');
    });
    em.addEventListener('input', function() {
        look(Math.min(1, em.value.length / 22) * 2 - 1, 0.8);
    });
    em.addEventListener('blur', function() { fe = false; });
}

if (pw) {
    pw.addEventListener('focus', function() {
        if (pw.type === 'password') {
            hold = true;
            if (eyes) eyes.classList.add('blink');
            msg('Mình nhắm mắt!');
        }
    });
    pw.addEventListener('blur', function() {
        hold = false;
        if (eyes) eyes.classList.remove('blink');
        msg('Đăng nhập nào!');
    });
}

if (tg && pw) {
    tg.addEventListener('click', function() {
        var sh = pw.type === 'password';
        pw.type = sh ? 'text' : 'password';
        tg.textContent = sh ? 'ẨN' : 'HIỆN';
        hold = !sh && document.activeElement === pw;
        if (eyes) eyes.classList.toggle('blink', hold);
        msg(sh ? 'Hí hí, mình thấy rồi!' : 'Nhắm mắt lại!');
    });
}

var btnGo = document.getElementById('go');
if (btnGo) {
    btnGo.addEventListener('click', function() {
        var emailVal = em.value.trim();
        var passVal = pw.value;
        var box = document.getElementById('fc');

        if (!emailVal || passVal.length < 6) {
            if (box) {
                box.classList.remove('shake');
                void box.offsetWidth;
                box.classList.add('shake');
            }
            msg(!emailVal ? 'Điền email nhé!' : 'Mật khẩu ≥ 6 ký tự!');
            return;
        }

        btnGo.textContent = 'ĐANG XÁC THỰC...';
        msg('Đang kiểm tra...');

        var params = new URLSearchParams();
        params.append('email', emailVal);
        params.append('password', passVal);

        fetch(window.APP_CONTEXT + '/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                btnGo.textContent = 'THÀNH CÔNG ✓';
                btnGo.style.background = '#1d8f3a';
                msg('Chào mừng ' + data.fullName + '! 🎉');
                toast('Đăng nhập thành công!');
                setTimeout(function() { goAfterAuth(data); }, 1000);
            } else {
                btnGo.textContent = 'ĐĂNG NHẬP';
                msg('Sai thông tin rồi!');
                toast(data.message || 'Email hoặc mật khẩu chưa đúng!');
                if (box) {
                    box.classList.remove('shake');
                    void box.offsetWidth;
                    box.classList.add('shake');
                }
            }
        })
        .catch(err => {
            btnGo.textContent = 'ĐĂNG NHẬP';
            toast('Lỗi kết nối máy chủ!');
        });
    });
}

// REGISTER LOGIC
var rn = document.getElementById('rn');
var re = document.getElementById('re');
var rp = document.getElementById('rp');
var rw = document.getElementById('rw');
var rc = document.getElementById('rc');
var tg2 = document.getElementById('tg2');
var sm = document.getElementById('sm') ? document.getElementById('sm').children : [];
var sl = document.getElementById('sl');
var SC = ['#ff4d4d', '#ff9f1a', '#ffd400', '#3ddc4a'];
var ST = ['Quá yếu', 'Yếu', 'Khá ổn', 'Mạnh lắm!'];

function evalStrength(v) {
    var n = 0;
    if (v.length >= 8) n++;
    if (/[a-z]/.test(v) && /[A-Z]/.test(v)) n++;
    if (/\d/.test(v)) n++;
    if (/[^A-Za-z0-9]/.test(v)) n++;
    return n;
}

if (rw) {
    rw.addEventListener('input', function() {
        var n = evalStrength(rw.value);
        if (!rw.value) n = 0;
        for (var i = 0; i < sm.length; i++) {
            sm[i].style.background = i < n ? SC[n - 1] : '#fff';
        }
        if (sl) sl.textContent = rw.value ? 'Độ mạnh: ' + ST[Math.max(0, n - 1)] : '';
        if (rw.value) msg(n >= 3 ? 'Mạnh lắm!' : n === 2 ? 'Thêm chút nữa!' : 'Yếu quá!');
    });
}

if (tg2 && rw && rc) {
    tg2.addEventListener('click', function() {
        var sh = rw.type === 'password';
        rw.type = rc.type = sh ? 'text' : 'password';
        tg2.textContent = sh ? 'ẨN' : 'HIỆN';
        hold = !sh && document.activeElement === rw;
        if (eyes) eyes.classList.toggle('blink', hold);
        msg(sh ? 'Hí hí, mình thấy rồi!' : 'Nhắm mắt lại!');
    });
}

var btnGo2 = document.getElementById('go2');
if (btnGo2) {
    btnGo2.addEventListener('click', function() {
        var nameVal = rn ? rn.value.trim() : '';
        var mailVal = re ? re.value.trim() : '';
        var phoneVal = rp ? rp.value.trim() : '';
        var passVal = rw ? rw.value : '';
        var confVal = rc ? rc.value : '';
        var agreeChk = document.getElementById('ag');
        var box2 = document.getElementById('fc2');

        function shakeBox(text) {
            if (box2) {
                box2.classList.remove('shake');
                void box2.offsetWidth;
                box2.classList.add('shake');
            }
            msg(text);
            toast(text);
        }

        if (!nameVal) return shakeBox('Điền họ và tên nhé!');
        if (!/^\S+@\S+\.\S+$/.test(mailVal)) return shakeBox('Email chưa đúng định dạng!');
        if (!/^[0-9 +]{9,13}$/.test(phoneVal)) return shakeBox('Số điện thoại chưa đúng!');
        if (passVal.length < 6) return shakeBox('Mật khẩu tối thiểu 6 ký tự!');
        if (confVal !== passVal) return shakeBox('Mật khẩu nhập lại chưa khớp!');
        if (agreeChk && !agreeChk.checked) return shakeBox('Vui lòng đồng ý điều khoản!');

        btnGo2.textContent = 'ĐANG ĐĂNG KÝ...';
        msg('Đang tạo tài khoản...');

        var params = new URLSearchParams();
        params.append('fullName', nameVal);
        params.append('email', mailVal);
        params.append('phone', phoneVal);
        params.append('password', passVal);

        fetch(window.APP_CONTEXT + '/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                btnGo2.textContent = 'THÀNH CÔNG ✓';
                btnGo2.style.background = '#1d8f3a';
                msg('Chào mừng ' + data.fullName + '! 🎉');
                toast('Tạo tài khoản thành công!');
                setTimeout(function() { goAfterAuth(data); }, 1000);
            } else {
                btnGo2.textContent = 'TẠO TÀI KHOẢN';
                shakeBox(data.message || 'Đăng ký thất bại!');
            }
        })
        .catch(err => {
            btnGo2.textContent = 'TẠO TÀI KHOẢN';
            toast('Lỗi kết nối máy chủ!');
        });
    });
}

function formatVND(n) {
    return n.toLocaleString('vi-VN') + 'đ';
}

function generateQR(seed) {
    var x = 0, i;
    for (i = 0; i < seed.length; i++) x = (x * 31 + seed.charCodeAt(i)) >>> 0;
    var r = function() {
        x = (x * 1664525 + 1013904223) >>> 0;
        return x / 4294967296;
    };
    var p = '';
    function fd(a, b) {
        return (a < 7 && b < 7) || (a > 13 && b < 7) || (a < 7 && b > 13);
    }
    for (var a = 0; a < 21; a++) {
        for (var b = 0; b < 21; b++) {
            var on;
            if (fd(a, b)) {
                var u = a > 13 ? a - 14 : a;
                var v = b > 13 ? b - 14 : b;
                on = (u === 0 || u === 6 || v === 0 || v === 6 || (u > 1 && u < 5 && v > 1 && v < 5));
            } else {
                on = r() > 0.5;
            }
            if (on) p += 'M' + a + ' ' + b + 'h1v1h-1z';
        }
    }
    return '<svg viewBox="0 0 21 21" shape-rendering="crispEdges"><path d="' + p + '" fill="#111"/></svg>';
}

function setBusy(on) {
    var bar = document.getElementById('topbar');
    if (!bar) {
        bar = document.createElement('div');
        bar.id = 'topbar';
        document.body.appendChild(bar);
    }
    document.body.classList.toggle('busy', !!on);
}

function openOrderModal(eventId) {
    var payModal = document.getElementById('pay');
    if (!payModal || document.body.classList.contains('busy')) return;

    // Phân quyền: chỉ tài khoản khách hàng đã đăng nhập mới mua vé
    if (!window.IS_LOGGED_IN) {
        openL();
        toast('Vui lòng đăng nhập để mua vé');
        return;
    }
    if (window.USER_ROLE === 'ADMIN') {
        toast('Tài khoản quản trị không dùng để mua vé. Hãy dùng tài khoản khách hàng!');
        return;
    }
    setBusy(true);

    fetch(window.APP_CONTEXT + '/api/event-tickets?eventId=' + eventId)
        .then(res => res.json())
        .then(data => {
            setBusy(false);
            if (!data.success) {
                toast('Không lấy được thông tin vé: ' + data.message);
                return;
            }
            cancelClose();

            currentEvent = data.event;
            currentTicketTypes = data.tickets;
            if (data.serviceFee != null) window.SERVICE_FEE = Number(data.serviceFee);
            window.COUPON_CODE = data.couponCode || '';
            window.COUPON_PERCENT = data.couponPercent || 0;
            if (data.purchasable === false) { toast('Sự kiện này hiện không mở bán vé'); return; }
            selectedQuantities = {};
            discountRate = 0;

            if (currentTicketTypes.length > 0) {
                selectedQuantities[currentTicketTypes[0].ticketTypeId] = 1;
            }

            var evn = document.getElementById('evn');
            if (evn) {
                evn.textContent = currentEvent.title + ' · ' + currentEvent.location;
            }

            renderTicketOptions();
            renderOrderSummary();


            var cpn = document.getElementById('cpn');
            if (cpn) cpn.value = '';

            document.getElementById('pg').style.display = '';
            document.getElementById('ok').style.display = 'none';
            document.getElementById('s3').className = '';

            payModal.classList.add('on');
            payModal.scrollTop = 0;
            document.body.style.overflow = 'hidden';

            moveStage(document.getElementById('slot3'));
            resetMascot();
            msg('Chọn vé nào!');
        })
        .catch(err => {
            setBusy(false);
            toast('Lỗi kết nối tới cơ sở dữ liệu: ' + err);
        });
}

function renderTicketOptions() {
    var container = document.getElementById('tks');
    if (!container) return;

    var html = '';
    currentTicketTypes.forEach(function(tt) {
        var qty = selectedQuantities[tt.ticketTypeId] || 0;
        var formattedPrice = formatVND(tt.price);
        html += '<div class="tk">' +
            '<div>' +
            '<b>' + tt.typeName + '</b>' +
            '<small>' + (tt.description || 'Hạng vé tiêu chuẩn') + ' · ' + formattedPrice + '</small>' +
            '</div>' +
            '<div class="qs">' +
            '<button type="button" onclick="changeQty(' + tt.ticketTypeId + ', -1)">−</button>' +
            '<b id="qty_' + tt.ticketTypeId + '">' + qty + '</b>' +
            '<button type="button" onclick="changeQty(' + tt.ticketTypeId + ', 1)">+</button>' +
            '</div>' +
            '</div>';
    });
    container.innerHTML = html;
}

function changeQty(ttId, delta) {
    var cur = selectedQuantities[ttId] || 0;
    var next = Math.max(0, Math.min(10, cur + delta));
    selectedQuantities[ttId] = next;

    var lbl = document.getElementById('qty_' + ttId);
    if (lbl) lbl.textContent = next;

    renderOrderSummary();

    var totalTickets = 0;
    for (var k in selectedQuantities) totalTickets += selectedQuantities[k];
    msg(totalTickets > 0 ? (totalTickets + ' vé nhé!') : 'Chọn ít nhất 1 vé!');
}

function renderOrderSummary() {
    var linesContainer = document.getElementById('lines');
    var totContainer = document.getElementById('tot');
    var pbContainer = document.getElementById('pb');
    if (!linesContainer || !totContainer || !pbContainer) return;

    var html = '';
    var subtotal = 0;
    var totalCount = 0;

    currentTicketTypes.forEach(function(tt) {
        var qty = selectedQuantities[tt.ticketTypeId] || 0;
        if (qty > 0) {
            var lineTotal = qty * tt.price;
            subtotal += lineTotal;
            totalCount += qty;
            html += '<div class="sl2"><span>' + tt.typeName + ' × ' + qty + '</span><b>' + formatVND(lineTotal) + '</b></div>';
        }
    });

    var serviceFee = totalCount * (window.SERVICE_FEE != null ? window.SERVICE_FEE : 5000);
    html += '<div class="sl2"><span>Phí dịch vụ</span><b>' + formatVND(serviceFee) + '</b></div>';

    var discountAmount = Math.round(subtotal * discountRate);
    if (discountAmount > 0) {
        html += '<div class="sl2" style="color:#1d8f3a"><span>Giảm giá 10%</span><b>-' + formatVND(discountAmount) + '</b></div>';
    }

    var grandTotal = subtotal - discountAmount + serviceFee;

    linesContainer.innerHTML = html;
    totContainer.textContent = formatVND(grandTotal);
    pbContainer.textContent = 'THANH TOÁN VNPAY ' + formatVND(grandTotal);
}

var btnApplyCoupon = document.getElementById('ap');
if (btnApplyCoupon) {
    btnApplyCoupon.onclick = function() {
        var code = document.getElementById('cpn').value.trim().toUpperCase();
        var activeCode = (window.COUPON_CODE || '').toUpperCase();
        var pct = window.COUPON_PERCENT || 0;
        if (activeCode && code === activeCode) {
            discountRate = pct / 100;
            msg('Giảm ' + pct + '% rồi! 🎉');
            toast('Áp dụng mã giảm giá thành công (-' + pct + '%)');
        } else {
            discountRate = 0;
            msg('Mã không đúng!');
            toast('Mã giảm giá không hợp lệ');
        }
        renderOrderSummary();
    };
}

var pmContainer = document.getElementById('pm');
if (pmContainer) {
    pmContainer.addEventListener('click', function(e) {
        var b = e.target.closest('button');
        if (!b) return;
        paymentMethod = b.dataset.m;
        var btns = pmContainer.querySelectorAll('button');
        btns.forEach(function(x) { x.classList.toggle('on', x === b); });
        msg(paymentMethod === 'qr' ? 'Quét mã đi nào!' : paymentMethod === 'intl' ? 'Thẻ quốc tế nhé!' : 'Chọn ngân hàng nhé!');
    });
}

var btnPay = document.getElementById('pb');
if (btnPay) {
    btnPay.onclick = function() {
        var pn = document.getElementById('pn').value.trim();
        var pe = document.getElementById('pe').value.trim();
        var pt = document.getElementById('pt').value.trim();
        var boxSum = document.querySelector('.sum .fc');

        function shakePay(text) {
            if (boxSum) {
                boxSum.classList.remove('shake');
                void boxSum.offsetWidth;
                boxSum.classList.add('shake');
            }
            msg(text);
            toast(text);
        }

        var totalQty = 0;
        for (var k in selectedQuantities) totalQty += selectedQuantities[k];

        if (totalQty === 0) return shakePay('Vui lòng chọn ít nhất 1 vé!');
        if (!pn) return shakePay('Điền họ và tên người nhận nhé!');
        if (!/^\S+@\S+\.\S+$/.test(pe)) return shakePay('Email nhận vé chưa đúng!');
        if (!/^[0-9 +]{9,13}$/.test(pt)) return shakePay('Số điện thoại chưa đúng!');

        btnPay.textContent = 'ĐANG CHUYỂN SANG VNPAY...';
        btnPay.disabled = true;
        msg('Đang tạo đơn hàng...');

        var params = new URLSearchParams();
        params.append('eventId', currentEvent.eventId);
        params.append('customerName', pn);
        params.append('customerEmail', pe);
        params.append('customerPhone', pt);
        params.append('paymentMethod', paymentMethod);
        params.append('couponCode', document.getElementById('cpn').value.trim());

        currentTicketTypes.forEach(function(tt) {
            var qty = selectedQuantities[tt.ticketTypeId] || 0;
            if (qty > 0) {
                params.append('ticketTypeId', tt.ticketTypeId);
                params.append('quantity', qty);
            }
        });

        fetch(window.APP_CONTEXT + '/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        })
        .then(res => res.json())
        .then(data => {
            if (data.success && data.paymentUrl) {
                // Vé đã được giữ ở trạng thái "Chờ thanh toán" -> chuyển sang cổng VNPay Sandbox
                toast('Đang chuyển sang cổng thanh toán VNPay...');
                window.location.href = data.paymentUrl;
            } else {
                btnPay.disabled = false;
                btnPay.textContent = 'THANH TOÁN QUA VNPAY';
                if (data.needLogin) { window.IS_LOGGED_IN = false; closeL(); setTimeout(openL, 300); }
                shakePay(data.message || 'Không tạo được giao dịch thanh toán!');
            }
        })
        .catch(err => {
            btnPay.disabled = false;
            btnPay.textContent = 'THANH TOÁN QUA VNPAY';
            toast('Lỗi kết nối: ' + err);
        });
    };
}


// =====================================================================
// HERO: SLIDER ẢNH SỰ KIỆN (tự chạy, đổi luôn tiêu đề / giá / nút MUA VÉ)
// =====================================================================
(function initHeroSlider() {
    var wrap = document.getElementById('posterWrap');
    if (!wrap) return;
    var posters = Array.prototype.slice.call(wrap.querySelectorAll('.poster'));
    if (!posters.length) return;

    var cur = 0;
    posters.forEach(function(p, i) { if (p.classList.contains('on')) cur = i; });
    posters.forEach(function(p, i) { p.classList.toggle('on', i === cur); });

    var el = {
        tag: document.getElementById('heroTag'),
        title: document.getElementById('heroTitle'),
        desc: document.getElementById('heroDesc'),
        price: document.getElementById('heroPrice'),
        date: document.getElementById('heroDate'),
        loc: document.getElementById('heroLoc')
    };

    function applyText(p, animate) {
        var d = p.dataset;
        window.heroEventId = parseInt(d.id, 10) || window.heroEventId;
        if (el.tag) el.tag.textContent = d.tag || '';
        if (el.title) el.title.textContent = d.title || '';
        if (el.desc) el.desc.textContent = d.desc || '';
        if (el.price) el.price.textContent = d.price || '';
        if (el.date) el.date.textContent = d.date ? '🗓 ' + d.date : '';
        if (el.loc) el.loc.textContent = d.loc ? '📍 ' + d.loc : '';
        if (animate) {
            [el.tag, el.title, el.desc].forEach(function(n, i) {
                if (!n) return;
                n.classList.remove('swap');
                void n.offsetWidth;
                n.style.animationDelay = (i * 60) + 'ms';
                n.classList.add('swap');
            });
        }
    }
    applyText(posters[cur], false);

    var dotsBox = document.getElementById('posterDots');
    var dots = [];
    if (dotsBox && posters.length > 1) {
        posters.forEach(function(p, i) {
            var b = document.createElement('button');
            b.type = 'button';
            b.setAttribute('aria-label', 'Sự kiện ' + (i + 1));
            if (i === cur) b.className = 'on';
            b.addEventListener('click', function(e) {
                e.stopPropagation();
                show(i);
                restart();
            });
            dotsBox.appendChild(b);
            dots.push(b);
        });
    }

    function show(n) {
        n = (n + posters.length) % posters.length;
        if (n === cur) return;
        var old = posters[cur];
        old.classList.remove('on');
        old.classList.add('off');
        setTimeout(function() { old.classList.remove('off'); }, 750);
        posters[n].classList.remove('off');
        posters[n].classList.add('on');
        cur = n;
        dots.forEach(function(d, i) { d.classList.toggle('on', i === n); });
        applyText(posters[n], true);
    }

    if (posters.length < 2) return;

    var timer = null, hovering = false;
    function tick() {
        // Không chuyển khi đang mở cửa sổ đặt vé/đăng nhập, đang rê chuột hoặc tab bị ẩn
        if (hovering || document.hidden || document.body.style.overflow === 'hidden') return;
        show(cur + 1);
    }
    function restart() {
        clearInterval(timer);
        timer = setInterval(tick, 4200);
    }
    var hero = document.querySelector('.hero');
    if (hero) {
        hero.addEventListener('mouseenter', function() { hovering = true; });
        hero.addEventListener('mouseleave', function() { hovering = false; });
    }
    restart();
})();

// Đăng nhập nhưng sai vai trò (khách hàng cố vào trang quản trị)
(function denied() {
    if (new URLSearchParams(window.location.search).get('denied') !== '1') return;
    setTimeout(function() { toast('Tài khoản của bạn không có quyền vào khu vực quản trị'); }, 500);
})();

// Bị chuyển từ Dashboard / Đơn hàng về do chưa đăng nhập -> tự mở form đăng nhập
(function autoOpenLogin() {
    var qs = new URLSearchParams(window.location.search);
    if (qs.get('login') !== '1' || window.IS_LOGGED_IN) return;
    setTimeout(function() {
        openL();
        toast('Vui lòng đăng nhập để tiếp tục');
    }, 400);
})();
