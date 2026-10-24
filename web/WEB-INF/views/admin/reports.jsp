<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<form class="toolbar no-print" method="get" action="${ctx}/admin/reports" id="rf">
    <div class="seg">
        <a href="?preset=7&event=${eventId}">7 ngày</a>
        <a href="?preset=30&event=${eventId}">30 ngày</a>
        <a href="?preset=90&event=${eventId}">90 ngày</a>
        <a href="?preset=month&event=${eventId}">Tháng này</a>
        <a href="?preset=year&event=${eventId}">Năm nay</a>
    </div>
    <input class="inp" type="date" name="from" value="${from}" style="width:150px" title="Từ ngày">
    <input class="inp" type="date" name="to" value="${to}" style="width:150px" title="Đến ngày">
    <select class="inp" name="event" style="width:220px">
        <option value="0">Tất cả sự kiện</option>
        <c:forEach items="${events}" var="e"><option value="${e.eventId}" ${e.eventId == eventId ? 'selected' : ''}>${e.title}</option></c:forEach>
    </select>
    <select class="inp" name="gran" style="width:130px">
        <option value="day" ${gran == 'day' ? 'selected' : ''}>Theo ngày</option>
        <option value="week" ${gran == 'week' ? 'selected' : ''}>Theo tuần</option>
        <option value="month" ${gran == 'month' ? 'selected' : ''}>Theo tháng</option>
    </select>
    <button class="btn btn-primary" type="submit">Xem báo cáo</button>
    <span class="grow"></span>
    <a class="btn btn-lime" href="${ctx}/admin/reports?export=csv&from=${from}&to=${to}&event=${eventId}&gran=${gran}">⬇ Xuất CSV</a>
    <button class="btn" type="button" onclick="window.print()">🖨 In</button>
</form>

<c:if test="${not empty summary}">
<div class="mut" style="margin-bottom:14px;font-weight:800">Báo cáo từ <b style="color:var(--ink)">${fromText}</b> đến <b style="color:var(--ink)">${toText}</b></div>

<section class="kpis">
    <div class="kpi lime" title="${summary.revenueText}"><div class="lab"><i>💰</i>Doanh thu</div><div class="val" data-animate-num>${summary.revenueShort}</div><div class="meta"><span>${summary.paidOrders} đơn đã thanh toán</span></div></div>
    <div class="kpi yellow" title="${summary.netText}"><div class="lab"><i>🧮</i>Doanh thu thuần</div><div class="val" data-animate-num>${summary.netShort}</div><div class="meta"><span>đã trừ phí dịch vụ ${summary.feeText}</span></div></div>
    <div class="kpi pink"><div class="lab"><i>🧾</i>Tổng đơn</div><div class="val" data-animate-num>${summary.orders}</div><div class="meta"><span>${summary.pending} chờ · ${summary.cancelled} hủy</span></div></div>
    <div class="kpi blue"><div class="lab"><i>🎟️</i>Vé đã đặt</div><div class="val" data-animate-num>${summary.tickets}</div><div class="meta"><span>${summary.paidTickets} vé đã thanh toán</span></div></div>
    <div class="kpi lime" title="${summary.aovText}"><div class="lab"><i>📈</i>Đơn trung bình</div><div class="val" data-animate-num>${summary.aovShort}</div><div class="meta"><span>trên đơn đã thanh toán</span></div></div>
    <div class="kpi yellow"><div class="lab"><i>↩️</i>Tỷ lệ hủy</div><div class="val">${summary.cancelRate}%</div><div class="meta"><span>giảm giá đã dùng ${summary.discountText}</span></div></div>
</section>

<section class="grid g-main mt">
    <div class="card">
        <div class="card-h"><div><h2>Biểu đồ theo ${gran == 'month' ? 'tháng' : gran == 'week' ? 'tuần' : 'ngày'}</h2></div>
            <div class="seg no-print" id="chartSeg"><button class="on" data-k="revenue">Doanh thu</button><button data-k="tickets">Vé</button><button data-k="orders">Đơn</button></div></div>
        <div class="chart" id="lineChart"></div>
    </div>
    <div class="card">
        <div class="card-h"><h2>Phương thức thanh toán</h2></div>
        <div class="donut-wrap"><div id="donut"></div><div class="legend" id="donutLegend"></div></div>
    </div>
</section>

<section class="card mt">
    <div class="card-h"><h2>Doanh thu theo sự kiện</h2></div>
    <div class="tbl-wrap"><table class="tbl">
        <thead><tr><th>Sự kiện</th><th class="num">Đơn</th><th class="num">Vé</th><th class="num">Doanh thu</th><th style="min-width:200px">Tỷ trọng</th></tr></thead>
        <tbody>
        <c:forEach items="${r.byEvent}" var="e">
            <tr><td class="strong">${e.title}</td><td class="num">${e.orders}</td><td class="num">${e.tickets}</td><td class="num strong">${e.revenueText}</td>
                <td><div style="display:flex;align-items:center;gap:10px"><div class="bar thin" style="flex:1"><i data-w="${e.share}"></i></div><b style="min-width:46px;text-align:right">${e.share}%</b></div></td></tr>
        </c:forEach>
        </tbody>
    </table></div>
</section>

<section class="grid g2 mt">
    <div class="card">
        <div class="card-h"><h2>Theo loại vé</h2></div>
        <div class="tbl-wrap"><table class="tbl" style="min-width:0">
            <thead><tr><th>Hạng vé</th><th class="num">Vé</th><th class="num">Doanh thu</th></tr></thead>
            <tbody>
            <c:forEach items="${r.byType}" var="t">
                <tr><td><b>${t.type}</b><div class="mut" style="font-size:12px">${t.event}</div></td><td class="num">${t.tickets}</td><td class="num strong">${t.revenueText}</td></tr>
            </c:forEach>
            </tbody>
        </table></div>
    </div>
    <div class="card">
        <div class="card-h"><h2>Khách hàng chi nhiều nhất</h2></div>
        <c:choose><c:when test="${empty r.topCustomers}"><div class="empty"><b>Chưa có dữ liệu</b></div></c:when>
        <c:otherwise><div class="list" style="gap:14px">
            <c:forEach items="${r.topCustomers}" var="u" varStatus="st">
                <div class="li"><span class="rank r${st.index + 1}">${st.index + 1}</span><span class="grow"><b>${u.name}</b><small>${u.email} · ${u.orders} đơn</small></span><span class="end">${u.spentText}</span></div>
            </c:forEach>
        </div></c:otherwise></c:choose>
    </div>
</section>
</c:if>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<c:if test="${not empty summary}">
<script>
(function () {
    var C = ${chartJson};
    var chart = Charts.line(document.getElementById('lineChart'), {
        labels: C.labels, active: 'revenue', height: 300,
        series: {
            revenue: { name: 'Doanh thu', color: '#c8f51f', data: C.revenue, fmt: function (v) { return A.vnd(v); } },
            tickets: { name: 'Vé đặt', color: '#ffd400', data: C.tickets, fmt: function (v) { return v + ' vé'; }, axis: function (v) { return Math.round(v); } },
            orders:  { name: 'Đơn hàng', color: '#ff62aa', data: C.orders, fmt: function (v) { return v + ' đơn'; }, axis: function (v) { return Math.round(v); } }
        }
    });
    document.getElementById('chartSeg').addEventListener('click', function (e) {
        var b = e.target.closest('button[data-k]'); if (!b) return;
        A.$$('#chartSeg button').forEach(function (x) { x.classList.toggle('on', x === b); });
        chart.show(b.getAttribute('data-k'));
    });
    var colors = ['#3a68ff', '#ff62aa', '#ffd400', '#20d5ec'];
    var items = C.payments.map(function (p, i) { return { name: p.name, value: p.value, color: colors[i % 4] }; });
    Charts.donut(document.getElementById('donut'), document.getElementById('donutLegend'), items, { center: items.reduce(function (a, b) { return a + b.value; }, 0), caption: 'đơn' });
})();
</script>
</c:if>
</body>
</html>
