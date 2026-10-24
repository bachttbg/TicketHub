<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="pagerBase" value="&q=${q}&sort=${sort}" scope="request"/>

<section class="kpis" style="grid-template-columns:repeat(4,minmax(0,1fr));margin-bottom:20px">
    <div class="kpi lime"><div class="lab"><i>👥</i>Tổng khách hàng</div><div class="val" data-animate-num>${summary.total}</div><div class="meta"><span>tài khoản đã đăng ký</span></div></div>
    <div class="kpi yellow"><div class="lab"><i>🆕</i>Khách mới</div><div class="val" data-animate-num>${summary.fresh}</div><div class="meta"><span>đăng ký trong 30 ngày qua</span></div></div>
    <div class="kpi pink"><div class="lab"><i>🛍️</i>Đã mua vé</div><div class="val" data-animate-num>${summary.buyers}</div><div class="meta"><span>khách có ít nhất 1 đơn hợp lệ</span></div></div>
    <div class="kpi blue"><div class="lab"><i>🔁</i>Khách quay lại</div><div class="val" data-animate-num>${summary.repeatRate}%</div><div class="meta"><span>${summary.repeat} khách mua từ 2 đơn</span></div></div>
</section>

<form class="toolbar no-print" method="get" action="${ctx}/admin/customers">
    <div class="search grow">
        <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input class="inp" type="search" name="q" value="${q}" placeholder="Tìm theo tên, email, số điện thoại...">
    </div>
    <select class="inp" name="sort" style="width:210px" onchange="this.form.submit()">
        <option value="spent" ${sort == 'spent' ? 'selected' : ''}>Chi tiêu nhiều nhất</option>
        <option value="orders" ${sort == 'orders' ? 'selected' : ''}>Nhiều đơn nhất</option>
        <option value="new" ${sort == 'new' ? 'selected' : ''}>Đăng ký mới nhất</option>
        <option value="name" ${sort == 'name' ? 'selected' : ''}>Tên A → Z</option>
    </select>
    <button class="btn btn-primary" type="submit">Tìm</button>
    <a class="btn btn-lime" href="${ctx}/admin/customers?export=csv&q=${q}&sort=${sort}">⬇ Xuất CSV</a>
</form>

<c:choose>
<c:when test="${empty customers}"><div class="empty"><b>Không tìm thấy khách hàng</b>Thử từ khóa khác.</div></c:when>
<c:otherwise>
<div class="tbl-wrap"><table class="tbl">
    <thead><tr><th>Khách hàng</th><th>Điện thoại</th><th>Đăng ký</th><th class="num">Đơn</th><th class="num">Vé</th><th class="num">Chi tiêu</th><th>Đơn gần nhất</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${customers}" var="u" varStatus="st">
        <tr class="click" data-id="${u.id}">
            <td><div class="cell-user"><span class="avatar">${u.initial}</span><div><b>${u.name}</b><small>${u.email}</small></div></div></td>
            <td class="mut">${u.phone}</td>
            <td class="mut">${u.joined}</td>
            <td class="num strong">${u.orders}</td>
            <td class="num">${u.tickets}</td>
            <td class="num strong">${u.spentText}</td>
            <td class="mut">${u.lastOrder}</td>
            <td><button class="btn btn-sm" type="button">Xem đơn</button></td>
        </tr>
    </c:forEach>
    </tbody>
</table></div>
<%@ include file="/WEB-INF/views/common/pager.jsp" %>
</c:otherwise>
</c:choose>

<div class="back drawer" id="custDrawer">
    <div class="modal">
        <div class="modal-h"><h2 id="cTitle">Khách hàng</h2><button class="x" data-close aria-label="Đóng">✕</button></div>
        <div class="modal-b" id="cBody"></div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<script>
(function () {
    var pill = { 'Đã thanh toán': 'ok', 'Chờ thanh toán': 'wait', 'Đã hủy': 'bad' };
    var tb = document.querySelector('.tbl tbody');
    tb && tb.addEventListener('click', function (e) {
        var tr = e.target.closest('tr[data-id]'); if (!tr) return;
        A.open('custDrawer');
        document.getElementById('cBody').innerHTML = '<div class="skeleton" style="height:220px"></div>';
        A.get(A.ctx + '/admin/customers?format=json&id=' + tr.getAttribute('data-id')).then(function (c) {
            document.getElementById('cTitle').textContent = c.name;
            var rows = c.orderList.length ? c.orderList.map(function (o) {
                return '<div class="card flat" style="padding:12px;margin-bottom:10px;box-shadow:none"><div style="display:flex;justify-content:space-between;gap:8px;align-items:center"><span class="code">' + A.esc(o.code) + '</span><span class="pill ' + (pill[o.status] || '') + '">' + A.esc(o.status) + '</span></div>' +
                    '<div style="font-weight:800;margin-top:8px">' + A.esc(o.event) + '</div><div class="mut" style="font-size:13px">' + A.esc(o.items) + ' · ' + A.esc(o.time) + '</div><div style="font-weight:800;margin-top:4px">' + A.esc(o.total) + '</div></div>';
            }).join('') : '<div class="empty"><b>Chưa có đơn hàng</b></div>';
            document.getElementById('cBody').innerHTML =
                '<div style="display:flex;gap:14px;align-items:center;margin-bottom:12px"><span class="avatar" style="width:54px;height:54px;font-size:22px">' + A.esc(c.initial) + '</span><div><b style="font-size:17px">' + A.esc(c.name) + '</b><div class="mut">' + A.esc(c.email) + ' · ' + A.esc(c.phone) + '</div></div></div>' +
                '<div class="row3" style="margin:14px 0"><div class="kpi lime" style="box-shadow:none;padding:12px"><div class="meta">Đơn</div><div class="val" style="font-size:22px;margin:2px 0">' + c.orders + '</div></div>' +
                '<div class="kpi yellow" style="box-shadow:none;padding:12px"><div class="meta">Vé</div><div class="val" style="font-size:22px;margin:2px 0">' + c.tickets + '</div></div>' +
                '<div class="kpi pink" style="box-shadow:none;padding:12px"><div class="meta">Chi tiêu</div><div class="val" style="font-size:16px;margin:6px 0">' + A.esc(c.spentText) + '</div></div></div>' +
                '<div class="sec-t">Lịch sử đơn hàng (tối đa 50 đơn gần nhất)</div>' + rows;
        }).catch(function (er) { A.toast(er.message, 'err'); A.close('custDrawer'); });
    });
})();
</script>
</body>
</html>
