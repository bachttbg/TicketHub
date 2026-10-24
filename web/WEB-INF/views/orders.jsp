<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pagerBase" value="${baseQuery}" scope="request"/>

<form class="toolbar no-print" method="get" action="${ctx}/admin/orders">
    <div class="search grow">
        <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input class="inp" type="search" name="q" value="${f.keyword}" placeholder="Tìm mã đơn, tên, email, số điện thoại...">
    </div>
    <select class="inp" name="event" style="width:210px" onchange="this.form.submit()">
        <option value="0">Tất cả sự kiện</option>
        <c:forEach items="${events}" var="e"><option value="${e.eventId}" ${e.eventId == f.eventId ? 'selected' : ''}>${e.title}</option></c:forEach>
    </select>
    <select class="inp" name="pay" style="width:170px" onchange="this.form.submit()">
        <option value="">Mọi hình thức</option>
        <option value="bank" ${f.payment == 'bank' ? 'selected' : ''}>Thẻ ATM</option>
        <option value="qr" ${f.payment == 'qr' ? 'selected' : ''}>QR chuyển khoản</option>
        <option value="wallet" ${f.payment == 'wallet' ? 'selected' : ''}>Ví điện tử</option>
        <option value="intl" ${f.payment == 'intl' ? 'selected' : ''}>Visa / Mastercard</option>
    </select>
    <input class="inp" type="date" name="from" value="${f.from}" style="width:150px" title="Từ ngày">
    <input class="inp" type="date" name="to" value="${f.to}" style="width:150px" title="Đến ngày">
    <input type="hidden" name="status" value="${f.status}">
    <button class="btn btn-primary" type="submit">Lọc</button>
    <a class="btn" href="${ctx}/admin/orders">Xóa lọc</a>
    <a class="btn btn-lime" href="${ctx}/admin/orders?export=csv${baseQuery}">⬇ Xuất CSV</a>
</form>

<div class="toolbar">
    <div class="seg">
        <c:forEach items="${counts}" var="c">
            <a href="?status=${c.key == 'Tất cả' ? '' : c.key}${baseNoStatus}" class="${(c.key == 'Tất cả' && empty f.status) || c.key == f.status ? 'on' : ''}">${c.key}<span class="n">${c.value}</span></a>
        </c:forEach>
    </div>
    <span class="grow"></span>
    <span class="mut">Doanh thu đã thanh toán (theo bộ lọc): <b style="color:var(--ink)">${revenue}</b></span>
</div>

<c:choose>
<c:when test="${empty orders}">
    <div class="empty"><b>Không có đơn hàng phù hợp</b>Thử thay đổi hoặc xóa bớt bộ lọc.</div>
</c:when>
<c:otherwise>
<div class="tbl-wrap"><table class="tbl">
    <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Sự kiện / Vé</th><th class="num">Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th>Vào cổng</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${orders}" var="o">
        <tr class="click" data-id="${o.orderId}">
            <td><span class="code">${o.orderCode}</span><div class="mut" style="font-size:12px;margin-top:3px">${o.createdText}</div></td>
            <td><div class="cell-user"><span class="avatar" style="width:32px;height:32px;font-size:13px">${o.customerName.substring(0,1)}</span><div><b>${o.customerName}</b><small>${o.customerPhone}</small></div></div></td>
            <td style="max-width:230px"><b style="display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${o.eventTitle}</b><small class="mut">${o.ticketTypeSummary} × ${o.totalQuantity}</small></td>
            <td class="num strong">${o.formattedTotal}</td>
            <td class="mut">${o.paymentMethod == 'qr' ? 'QR' : o.paymentMethod == 'wallet' ? 'Ví điện tử' : o.paymentMethod == 'intl' ? 'Visa/MC' : 'Thẻ ATM'}</td>
            <td><span class="pill ${o.paymentStatus == 'Đã thanh toán' ? 'ok' : o.paymentStatus == 'Chờ thanh toán' ? 'wait' : 'bad'}">${o.paymentStatus}</span></td>
            <td><c:choose><c:when test="${o.paid}"><span class="pill ${o.checkedInQty >= o.totalQuantity ? 'info' : 'mut'}">${o.checkedInQty}/${o.totalQuantity}</span></c:when><c:otherwise><span class="mut">—</span></c:otherwise></c:choose></td>
            <td><button class="btn btn-sm" type="button">Chi tiết</button></td>
        </tr>
    </c:forEach>
    </tbody>
</table></div>
<%@ include file="/WEB-INF/views/common/pager.jsp" %>
</c:otherwise>
</c:choose>

<%-- ===== Ngăn kéo chi tiết đơn ===== --%>
<div class="back drawer" id="orderDrawer">
    <div class="modal">
        <div class="modal-h"><h2 id="dTitle">Chi tiết đơn</h2><button class="x" data-close aria-label="Đóng">✕</button></div>
        <div class="modal-b" id="dBody"><div class="skeleton" style="height:200px"></div></div>
        <div class="modal-f" id="dFoot"></div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<script>
(function () {
    var cur = null;
    var pillCls = { 'Đã thanh toán': 'ok', 'Chờ thanh toán': 'wait', 'Đã hủy': 'bad' };

    document.querySelector('.tbl') && document.querySelector('.tbl tbody').addEventListener('click', function (e) {
        var tr = e.target.closest('tr[data-id]'); if (tr) openOrder(tr.getAttribute('data-id'));
    });

    function openOrder(id) {
        A.open('orderDrawer');
        document.getElementById('dBody').innerHTML = '<div class="skeleton" style="height:240px"></div>';
        document.getElementById('dFoot').innerHTML = '';
        A.get(A.ctx + '/admin/orders?format=json&id=' + id).then(render).catch(function (e) { A.toast(e.message, 'err'); A.close('orderDrawer'); });
    }

    function render(o) {
        cur = o;
        document.getElementById('dTitle').textContent = 'Đơn ' + o.code;
        var items = o.items.map(function (i) {
            return '<div class="sum-row"><span>' + A.esc(i.type) + ' <span class="mut">× ' + i.qty + ' (' + A.esc(i.unit) + ')</span></span><span>' + A.esc(i.amount) + '</span></div>';
        }).join('');
        document.getElementById('dBody').innerHTML =
            '<div style="display:flex;gap:14px;align-items:center;margin-bottom:6px"><img src="' + A.ctx + '/' + A.esc(o.image) + '" style="width:92px;height:62px;object-fit:cover;border:2.5px solid #111;border-radius:11px" alt="">' +
            '<div><b style="font-size:16px">' + A.esc(o.event) + '</b><div style="margin-top:6px"><span class="pill ' + (pillCls[o.status] || '') + '">' + A.esc(o.status) + '</span></div></div></div>' +
            '<div class="sec-t">Khách hàng</div><dl class="kv"><dt>Họ tên</dt><dd>' + A.esc(o.name) + '</dd><dt>Email</dt><dd>' + A.esc(o.email) + '</dd><dt>Điện thoại</dt><dd>' + A.esc(o.phone) + '</dd>' +
            '<dt>Đặt lúc</dt><dd>' + A.esc(o.created) + '</dd><dt>Thanh toán</dt><dd>' + A.esc(o.payment) + '</dd></dl>' +
            '<div class="sec-t">Chi tiết vé</div>' + items +
            '<div class="sum-row"><span class="mut">Tạm tính</span><span>' + A.esc(o.subtotal) + '</span></div>' +
            '<div class="sum-row"><span class="mut">Phí dịch vụ</span><span>' + A.esc(o.fee) + '</span></div>' +
            (o.hasDiscount ? '<div class="sum-row"><span class="mut">Giảm giá</span><span>-' + A.esc(o.discount) + '</span></div>' : '') +
            '<div class="sum-row total"><span>Tổng cộng</span><span>' + A.esc(o.total) + '</span></div>' +
            '<div class="sec-t">Soát vé</div><div class="alert ' + (o.checkedIn >= o.quantity ? 'good' : 'info') + '"><span class="ic">🎫</span><span class="tx">Đã vào cổng <b>' + o.checkedIn + ' / ' + o.quantity + '</b> vé' + (o.lastCheckin ? ' · gần nhất ' + A.esc(o.lastCheckin) : '') + '</span></div>';

        var f = '';
        if (o.checkedIn > 0) f += '<button class="btn btn-sm" data-a="reset">Hoàn tác soát vé</button>';
        if (o.status === 'Chờ thanh toán') f += '<button class="btn btn-danger" data-a="Đã hủy">Hủy đơn</button><button class="btn btn-lime" data-a="Đã thanh toán">✔ Xác nhận đã thanh toán</button>';
        else if (o.status === 'Đã thanh toán') f += '<button class="btn btn-danger" data-a="Đã hủy">Hủy đơn & hoàn vé</button>';
        else f += '<button class="btn btn-yel" data-a="Chờ thanh toán">Khôi phục đơn</button>';
        f += '<a class="btn" href="' + A.ctx + '/admin/checkin?eventId=' + o.eventId + '">Mở soát vé</a>';
        document.getElementById('dFoot').innerHTML = f;
    }

    document.getElementById('dFoot').addEventListener('click', function (e) {
        var b = e.target.closest('button[data-a]'); if (!b || !cur) return;
        var a = b.getAttribute('data-a');
        var run = function () {
            return A.run(b, function () {
                return a === 'reset'
                    ? A.post(A.ctx + '/admin/orders', { action: 'resetCheckin', id: cur.id })
                    : A.post(A.ctx + '/admin/orders', { action: 'status', id: cur.id, value: a });
            }).then(function () { setTimeout(function () { location.reload(); }, 500); });
        };
        if (a === 'Đã hủy') {
            A.confirm({ title: 'Hủy đơn ' + cur.code + '?', text: 'Vé sẽ được trả về kho và khách không còn được vào cổng. Bạn có chắc chắn?', ok: 'Hủy đơn', danger: true }).then(function (y) { if (y) run(); });
        } else run();
    });
})();
</script>
</body>
</html>
