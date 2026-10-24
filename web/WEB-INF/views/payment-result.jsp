<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <meta name="color-scheme" content="light">
    <title>Kết quả thanh toán – TicketHub</title>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@500;600;700;800;900&display=swap">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <style>
        .pr-box { max-width: 560px; margin: 56px auto; border: 3px solid var(--ink); border-radius: 24px;
                  box-shadow: 8px 8px 0 var(--ink); padding: 32px 28px; text-align: center; background: #fff; }
        .pr-box.ok  { background: var(--lime); }
        .pr-box.bad { background: #ffc4d9; }
        .pr-icon { font-size: 56px; line-height: 1; }
        .pr-box h1 { margin: 12px 0 6px; font-size: 26px; font-weight: 900; }
        .pr-box p  { font-weight: 600; margin: 0 0 18px; }
        .pr-info { text-align: left; background: #fff; border: 2.5px solid var(--ink); border-radius: 16px; padding: 14px 18px; margin: 18px 0; }
        .pr-info div { display: flex; justify-content: space-between; gap: 12px; padding: 5px 0; font-weight: 700; }
        .pr-info span:first-child { color: var(--mut); font-weight: 600; }
        .pr-actions { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
        .pr-actions a { text-decoration: none; display: inline-block; }
    </style>
</head>
<body>
<nav>
    <div class="w">
        <a href="${pageContext.request.contextPath}/home" class="logo-container" title="TicketHub">
            <div class="logo-badge"><img src="${pageContext.request.contextPath}/assets/images/logo.svg" alt="TicketHub Logo"></div>
            <div class="logo-pill">TICKETHUB</div>
        </a>
        <div class="sp"></div>
    </div>
</nav>

<main class="w">
    <div class="pr-box ${success ? 'ok' : 'bad'}">
        <div class="pr-icon">${success ? '🎉' : '⚠️'}</div>
        <h1>${success ? 'THANH TOÁN THÀNH CÔNG!' : 'THANH TOÁN KHÔNG THÀNH CÔNG'}</h1>
        <p><c:out value="${message}"/></p>

        <c:if test="${not empty order}">
            <div class="pr-info">
                <div><span>Mã đơn hàng</span><span><c:out value="${order.orderCode}"/></span></div>
                <div><span>Sự kiện</span><span><c:out value="${order.eventTitle}"/></span></div>
                <div><span>Vé</span><span><c:out value="${order.ticketTypeSummary}"/> × ${order.totalQuantity}</span></div>
                <div><span>Tổng tiền</span><span>${order.formattedTotal}</span></div>
                <div><span>Trạng thái đơn</span><span><c:out value="${order.paymentStatus}"/></span></div>
                <c:if test="${success and not empty transactionNo}">
                    <div><span>Mã giao dịch VNPay</span><span><c:out value="${transactionNo}"/></span></div>
                </c:if>
                <c:if test="${success and not empty bankCode}">
                    <div><span>Ngân hàng</span><span><c:out value="${bankCode}"/></span></div>
                </c:if>
            </div>
        </c:if>

        <div class="pr-actions">
            <a class="go" style="max-width: 220px;" href="${pageContext.request.contextPath}/orders">VÉ CỦA TÔI</a>
            <a class="go" style="max-width: 220px; background: #fff;" href="${pageContext.request.contextPath}/home">VỀ TRANG CHỦ</a>
        </div>
        <p style="margin-top: 18px; font-size: 12.5px; color: var(--mut);">Môi trường thử nghiệm VNPay Sandbox – không phát sinh giao dịch tiền thật.</p>
    </div>
</main>
</body>
</html>
