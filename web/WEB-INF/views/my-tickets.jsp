<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <meta name="color-scheme" content="light">
    <script>document.documentElement.classList.add('js');</script>
    <title>Vé của tôi – TicketHub</title>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@500;600;700;800;900&display=swap">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>

<nav>
    <div class="w">
        <a href="${pageContext.request.contextPath}/home" class="logo-container" title="TicketHub">
            <div class="logo-badge">
                <img src="${pageContext.request.contextPath}/assets/images/logo.svg" alt="TicketHub Logo">
            </div>
            <div class="logo-pill">TICKETHUB</div>
        </a>
        <a class="l" href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <a class="l" href="${pageContext.request.contextPath}/home#ev">Sự kiện</a>
        <a class="l active" href="${pageContext.request.contextPath}/orders">Vé của tôi</a>
        <div class="sp"></div>
        <span class="b" style="background: #e6f9e6; border-color: #1b8a38;">👋 ${sessionScope.currentUser.fullName}</span>
        <a href="${pageContext.request.contextPath}/auth/logout" class="b">Đăng xuất</a>
    </div>
</nav>

<main class="w">
    <section class="mt-head reveal">
        <div>
            <span class="tag">TÀI KHOẢN KHÁCH HÀNG</span>
            <h1>Vé của tôi</h1>
            <p>Chỉ hiển thị các đơn đặt vé của tài khoản <b>${sessionScope.currentUser.email}</b>.</p>
        </div>
        <div class="mt-stats">
            <div class="mt-stat" style="background: var(--lime);"><b>${orders.size()}</b><span>Đơn hàng</span></div>
            <div class="mt-stat" style="background: var(--yel);"><b>${totalTickets}</b><span>Vé đã đặt</span></div>
        </div>
    </section>

    <c:choose>
        <c:when test="${not empty orders}">
            <div class="mt-list">
                <c:forEach items="${orders}" var="o" varStatus="st">
                    <article class="mt-card reveal" style="--i:${st.index};">
                        <div class="mt-img">
                            <img src="${pageContext.request.contextPath}/${eventMap[o.eventId].imagePath}" alt="${o.eventTitle}" loading="lazy">
                        </div>
                        <div class="mt-info">
                            <div class="mt-top">
                                <span class="mt-code">${o.orderCode}</span>
                                <c:choose>
                                    <c:when test="${o.paymentStatus == 'Đã thanh toán'}"><span class="mt-st ok">✔ ${o.paymentStatus}</span></c:when>
                                    <c:when test="${o.paymentStatus == 'Chờ thanh toán'}"><span class="mt-st wait">⏳ ${o.paymentStatus}</span></c:when>
                                    <c:otherwise><span class="mt-st bad">✖ ${o.paymentStatus}</span></c:otherwise>
                                </c:choose>
                            </div>
                            <h3>${o.eventTitle}</h3>
                            <div class="mt-meta">
                                <span>🎟 ${o.ticketTypeSummary} × ${o.totalQuantity}</span>
                                <span>🕘 ${o.createdText}</span>
                                <span>💳
                                    <c:choose>
                                        <c:when test="${o.paymentMethod == 'qr'}">QR chuyển khoản</c:when>
                                        <c:when test="${o.paymentMethod == 'wallet'}">Ví điện tử</c:when>
                                        <c:when test="${o.paymentMethod == 'intl'}">Visa / Mastercard</c:when>
                                        <c:otherwise>Thẻ ATM</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </div>
                        <div class="mt-total">
                            <small>Tổng tiền</small>
                            <b>${o.formattedTotal}</b>
                            <c:if test="${o.paymentStatus != 'Đã hủy'}">
                                <button type="button" class="mt-qr" data-code="${o.orderCode}" data-event="<c:out value='${o.eventTitle}'/>"
                                        data-qty="${o.totalQuantity}" data-in="${o.checkedInQty}" data-paid="${o.paid}">▦ Mã QR vào cổng</button>
                            </c:if>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <div class="empty-state reveal" style="margin-top: 28px;">
                <b>🎫 Bạn chưa đặt vé nào</b>
                <span>Chọn một sự kiện yêu thích và đặt vé chỉ trong 1 phút nhé!</span>
                <a class="buy" href="${pageContext.request.contextPath}/home#ev" style="text-decoration: none; display: inline-block; margin: 10px auto 0;">Khám phá sự kiện</a>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<footer class="site-footer" style="margin-top: 56px;">
    <div class="w">
        <div class="ft-bottom" style="margin-top: 0; border-top: 0;">
            <span>© 2026 TicketHub. Hệ thống đặt vé sự kiện trực tuyến.</span>
            <span>📞 Hotline: ${siteInfo.hotline} · ✉️ ${siteInfo.support_email}</span>
        </div>
    </div>
</footer>

<div class="qr-back" id="qrBack" hidden>
    <div class="qr-box" role="dialog" aria-modal="true" aria-labelledby="qrEvent">
        <button type="button" class="qr-x" id="qrClose" aria-label="Đóng">✕</button>
        <span class="tag" id="qrTag">VÉ ĐIỆN TỬ</span>
        <h3 id="qrEvent"></h3>
        <div class="qr-img" id="qrImg"></div>
        <div class="qr-code" id="qrCode"></div>
        <p id="qrNote"></p>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/js/page.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/vendor/qrcode.js"></script>
<script>
(function () {
    var back = document.getElementById('qrBack');
    function close() { back.hidden = true; document.body.style.overflow = ''; }
    document.addEventListener('click', function (e) {
        var b = e.target.closest ? e.target.closest('.mt-qr') : null;
        if (b) {
            var code = b.getAttribute('data-code');
            var qr = qrcode(0, 'M'); qr.addData(code); qr.make();
            document.getElementById('qrImg').innerHTML = qr.createSvgTag({ cellSize: 6, margin: 2, scalable: true });
            document.getElementById('qrEvent').textContent = b.getAttribute('data-event');
            document.getElementById('qrCode').textContent = code;
            var paid = b.getAttribute('data-paid') === 'true', qty = b.getAttribute('data-qty'), inn = b.getAttribute('data-in');
            document.getElementById('qrNote').textContent = !paid ? 'Đơn chưa thanh toán nên chưa thể dùng để vào cổng.'
                : 'Đưa mã này cho nhân viên soát vé. Đơn có ' + qty + ' vé' + (Number(inn) > 0 ? ', đã vào cổng ' + inn + '.' : '.');
            back.hidden = false; document.body.style.overflow = 'hidden';
        } else if (e.target === back || e.target.id === 'qrClose') close();
    });
    document.addEventListener('keydown', function (ev) { if (ev.key === 'Escape') close(); });
})();
</script>
</body>
</html>
