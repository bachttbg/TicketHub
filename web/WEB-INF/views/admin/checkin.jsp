<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="grid g-main-r">
    <%-- ===== Khu vực quét ===== --%>
    <div class="stack">
        <div class="card">
            <div class="card-h"><h2>Quét hoặc nhập mã đơn</h2><span class="hint">Mã dạng #TK-1001 (có trong QR của khách)</span></div>
            <div class="scan-box">
                <select class="inp" id="scope" aria-label="Sự kiện đang soát">
                    <option value="0">Tất cả sự kiện</option>
                    <c:forEach items="${events}" var="e"><option value="${e.eventId}" ${e.eventId == eventId ? 'selected' : ''}>${e.title}</option></c:forEach>
                </select>
                <div class="scan-input">
                    <input class="inp" id="code" placeholder="#TK-1001" autocomplete="off" autocapitalize="characters" spellcheck="false" autofocus>
                    <button class="btn btn-primary" id="btnLookup" type="button">Kiểm tra</button>
                    <button class="btn btn-yel" id="btnCam" type="button" title="Quét bằng camera">📷</button>
                </div>
                <div class="cam" id="camBox" hidden><video id="video" playsinline muted></video></div>
                <small class="mut" id="camHint" hidden>Đưa mã QR trên vé của khách vào khung hình. Camera chỉ hoạt động trên HTTPS hoặc localhost.</small>
            </div>
        </div>
        <div id="result"></div>
    </div>

    <%-- ===== Thống kê trực tiếp ===== --%>
    <div class="stack">
        <div class="card">
            <div class="card-h"><h2>Đã vào cổng</h2><span class="pill info" id="livePct">0%</span></div>
            <div class="live-big"><span id="liveAdm">${admitted}</span><span class="mut" style="font-size:22px"> / <span id="liveVal">${valid}</span> vé</span></div>
            <div class="bar" style="margin-top:12px"><i id="liveBar" data-w="${valid > 0 ? admitted * 100.0 / valid : 0}"></i></div>
        </div>
        <div class="card">
            <div class="card-h"><h2>Vừa soát gần đây</h2><button class="btn btn-sm" id="btnRefresh">Làm mới</button></div>
            <div class="list" id="recent">
                <c:forEach items="${recent}" var="o">
                    <div class="li"><span class="avatar" style="width:34px;height:34px;font-size:13px">${o.customerName.substring(0,1)}</span>
                        <span class="grow"><b>${o.customerName}</b><small>${o.orderCode} · ${o.checkinText}</small></span><span class="pill info">${o.checkedInQty}/${o.totalQuantity}</span></div>
                </c:forEach>
                <c:if test="${empty recent}"><div class="mut">Chưa có lượt soát nào.</div></c:if>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<script src="${ctx}/assets/js/vendor/jsQR.js"></script>
<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    var scope = function () { return $('scope').value; };
    var cur = null, qty = 1;

    function pct(a, b) { return b > 0 ? Math.round(a * 1000 / b) / 10 : 0; }
    function setLive(adm, val, p) {
        $('liveAdm').textContent = adm; $('liveVal').textContent = val; $('livePct').textContent = p + '%';
        $('liveBar').style.width = Math.min(100, p) + '%';
    }
    setLive(${admitted}, ${valid}, pct(${admitted}, ${valid}));

    function refresh() {
        return A.get(A.ctx + '/admin/checkin?format=json&eventId=' + scope()).then(function (d) {
            setLive(d.admitted, d.valid, d.percent);
            $('recent').innerHTML = d.recent.length ? d.recent.map(function (r) {
                return '<div class="li"><span class="avatar" style="width:34px;height:34px;font-size:13px">' + A.esc((r.name || '?').charAt(0)) + '</span><span class="grow"><b>' + A.esc(r.name) + '</b><small>' + A.esc(r.code) + ' · ' + A.esc(r.time) + '</small></span><span class="pill info">' + r.qty + '/' + r.total + '</span></div>';
            }).join('') : '<div class="mut">Chưa có lượt soát nào.</div>';
        }).catch(function () {});
    }
    $('btnRefresh').addEventListener('click', refresh);
    $('scope').addEventListener('change', function () { history.replaceState(null, '', '?eventId=' + scope()); refresh(); $('result').innerHTML = ''; $('code').focus(); });
    setInterval(function () { if (!document.hidden) refresh(); }, 20000);

    var cls = { ok: 'ok', partial: 'partial', done: 'done' };
    var icon = { ok: '✅', partial: '🟡', done: 'ℹ️', unpaid: '⏳', cancelled: '🚫', wrong_event: '⚠️' };

    function show(d) {
        cur = d; qty = Math.max(1, d.remaining);
        var items = (d.items || []).map(A.esc).join(' · ');
        var html = '<div class="verdict ' + (cls[d.state] || 'bad') + '">' +
            '<div class="big"><span>' + (icon[d.state] || '⚠️') + '</span><span>' + A.esc(d.message) + '</span></div>' +
            '<div class="info"><img src="' + A.ctx + '/' + A.esc(d.image) + '" alt=""><div><b style="font-size:17px">' + A.esc(d.event) + '</b>' +
            '<div class="mut" style="margin-top:3px"><span class="code">' + A.esc(d.code) + '</span> &nbsp;' + A.esc(d.name) + ' · ' + A.esc(d.phone) + '</div>' +
            '<div style="margin-top:4px;font-weight:700">' + items + '</div></div></div>' +
            '<div style="display:flex;justify-content:space-between;font-weight:800"><span>Đã vào cổng</span><span>' + d.checkedIn + ' / ' + d.quantity + ' vé</span></div>' +
            '<div class="bar"><i style="width:' + pct(d.checkedIn, d.quantity) + '%"></i></div>';
        if (d.canAdmit) {
            html += '<div style="display:flex;align-items:center;gap:14px;flex-wrap:wrap"><span style="font-weight:800">Số khách vào:</span>' +
                '<div class="stepper"><button type="button" id="qMinus">−</button><b id="qVal">' + qty + '</b><button type="button" id="qPlus">+</button></div>' +
                '<button class="btn btn-primary" id="btnAdmit" style="flex:1;min-width:180px;padding:12px">✔ Cho vào cổng</button></div>';
        } else if (d.checkedIn > 0) {
            html += '<div><button class="btn btn-sm" id="btnUndo">Hoàn tác soát vé</button></div>';
        }
        $('result').innerHTML = html + '</div>';
        var mi = $('qMinus'), pl = $('qPlus');
        if (mi) { mi.onclick = function () { qty = Math.max(1, qty - 1); $('qVal').textContent = qty; }; pl.onclick = function () { qty = Math.min(cur.remaining, qty + 1); $('qVal').textContent = qty; }; }
        var ad = $('btnAdmit'); if (ad) ad.onclick = admit;
        var un = $('btnUndo'); if (un) un.onclick = undo;
    }

    function showError(msg) {
        cur = null;
        $('result').innerHTML = '<div class="verdict bad"><div class="big"><span>❌</span><span>' + A.esc(msg) + '</span></div></div>';
    }

    function lookup(code) {
        code = (code || $('code').value).trim();
        if (!code) { $('code').focus(); return; }
        $('code').value = code.toUpperCase();
        return A.post(A.ctx + '/admin/checkin', { action: 'lookup', code: code, eventId: scope() }).then(show).catch(function (e) { showError(e.message); });
    }

    function admit() {
        var b = $('btnAdmit');
        A.run(b, function () { return A.post(A.ctx + '/admin/checkin', { action: 'admit', id: cur.id, qty: qty, eventId: scope() }); }, false).then(function (d) {
            show(d); A.toast(d.message); refresh();
            $('code').value = ''; $('code').focus();
        }).catch(function () {});
    }
    function undo() {
        A.confirm({ title: 'Hoàn tác soát vé?', text: 'Số vé đã vào cổng của đơn ' + cur.code + ' sẽ được đặt lại về 0.', ok: 'Hoàn tác', danger: true }).then(function (y) {
            if (y) A.post(A.ctx + '/admin/checkin', { action: 'undo', id: cur.id }).then(function () { A.toast('Đã hoàn tác'); lookup(cur.code); refresh(); }).catch(function (e) { A.toast(e.message, 'err'); });
        });
    }

    $('btnLookup').addEventListener('click', function () { lookup(); });
    $('code').addEventListener('keydown', function (e) { if (e.key === 'Enter') { e.preventDefault(); lookup(); } });

    // ----- Quét bằng camera (jsQR) -----
    var stream = null, scanning = false, last = '', lastAt = 0;
    var video = $('video'), canvas = document.createElement('canvas'), ctx2 = canvas.getContext('2d', { willReadFrequently: true });
    function stopCam() {
        scanning = false;
        if (stream) stream.getTracks().forEach(function (t) { t.stop(); });
        stream = null; $('camBox').hidden = true; $('camHint').hidden = true; $('btnCam').classList.remove('btn-pink'); $('btnCam').classList.add('btn-yel');
    }
    function tick() {
        if (!scanning) return;
        if (video.readyState === video.HAVE_ENOUGH_DATA && window.jsQR) {
            canvas.width = video.videoWidth; canvas.height = video.videoHeight;
            ctx2.drawImage(video, 0, 0);
            var img = ctx2.getImageData(0, 0, canvas.width, canvas.height);
            var r = jsQR(img.data, img.width, img.height, { inversionAttempts: 'dontInvert' });
            if (r && r.data && (r.data !== last || Date.now() - lastAt > 4000)) {
                last = r.data; lastAt = Date.now();
                if (navigator.vibrate) navigator.vibrate(60);
                lookup(r.data);
            }
        }
        requestAnimationFrame(tick);
    }
    $('btnCam').addEventListener('click', function () {
        if (stream) return stopCam();
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) { A.toast('Trình duyệt/kết nối này không hỗ trợ camera (cần HTTPS hoặc localhost).', 'err'); return; }
        navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } }).then(function (s) {
            stream = s; video.srcObject = s; video.play();
            $('camBox').hidden = false; $('camHint').hidden = false; $('btnCam').classList.remove('btn-yel'); $('btnCam').classList.add('btn-pink');
            scanning = true; requestAnimationFrame(tick);
        }).catch(function () { A.toast('Không mở được camera. Hãy cho phép quyền truy cập camera.', 'err'); });
    });
    window.addEventListener('pagehide', stopCam);
})();
</script>
</body>
</html>
