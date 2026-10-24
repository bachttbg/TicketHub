<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%-- ===== Bộ lọc ===== --%>
<form class="toolbar no-print" method="get" action="${ctx}/dashboard" id="filterForm">
    <div class="field" style="min-width:280px">
        <select class="inp" name="eventId" onchange="this.form.submit()" aria-label="Chọn sự kiện">
            <option value="0">📊 Tất cả sự kiện</option>
            <c:forEach items="${events}" var="e">
                <option value="${e.eventId}" ${e.eventId == eventId ? 'selected' : ''}>${e.title}</option>
            </c:forEach>
        </select>
    </div>
    <div class="seg" role="group" aria-label="Khoảng thời gian">
        <a href="?days=7&eventId=${eventId}"  class="${days == 7 ? 'on' : ''}">7 ngày</a>
        <a href="?days=30&eventId=${eventId}" class="${days == 30 ? 'on' : ''}">30 ngày</a>
        <a href="?days=90&eventId=${eventId}" class="${days == 90 ? 'on' : ''}">90 ngày</a>
    </div>
    <input type="hidden" name="days" value="${days}">
    <span class="grow"></span>
    <c:if test="${not empty d}"><span class="mut">Kỳ báo cáo: <b>${d.rangeText}</b></span></c:if>
    <a class="btn btn-lime" href="${ctx}/admin/events">＋ Tạo sự kiện</a>
    <a class="btn btn-yel" href="${ctx}/admin/checkin${eventId > 0 ? '?eventId='.concat(eventId) : ''}">Soát vé</a>
</form>

<c:if test="${not empty d}">

<%-- ===== Thông tin sự kiện đang chọn ===== --%>
<c:if test="${not empty selected}">
    <div class="card" style="margin-bottom:20px">
        <div class="hero-ev">
            <img src="${ctx}/${selected.imagePath}" alt="${selected.title}">
            <div class="grow">
                <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:6px">
                    <span class="pill ${selected.status == 'Đang mở bán' ? 'ok' : (selected.status == 'Tạm ẩn' || selected.status == 'Đã kết thúc') ? 'mut' : 'info'}">${selected.status}</span>
                    <span class="pill" style="background:${selected.categoryColor}">${selected.categoryName}</span>
                    <c:if test="${selected.featured}"><span class="pill pink">★ Nổi bật</span></c:if>
                </div>
                <h2 style="font-size:22px;font-weight:800">${selected.title}</h2>
                <div class="mut" style="margin-top:4px">📍 ${selected.location} &nbsp;·&nbsp; 🗓 ${selected.fullDateStr}</div>
            </div>
            <div style="min-width:240px">
                <div style="display:flex;justify-content:space-between;font-weight:800;margin-bottom:6px"><span>Đã bán</span><span>${selected.soldTickets} / ${selected.totalCapacity}</span></div>
                <div class="bar"><i data-w="${selected.fillPercent}"></i></div>
                <div style="display:flex;gap:8px;margin-top:12px">
                    <a class="btn btn-sm" href="${ctx}/admin/events">Sửa</a>
                    <a class="btn btn-sm" href="${ctx}/admin/ticket-types?eventId=${selected.eventId}">Loại vé</a>
                    <a class="btn btn-sm" href="${ctx}/admin/orders?event=${selected.eventId}">Đơn hàng</a>
                </div>
            </div>
        </div>
    </div>
</c:if>

<%-- ===== KPI ===== --%>
<section class="kpis" aria-label="Chỉ số chính">
    <c:forEach items="${d.kpis}" var="k">
        <div class="kpi ${k.color}" title="${k.full}">
            <div class="lab"><i>${k.icon}</i>${k.label}</div>
            <div class="val" data-animate-num>${k.value}</div>
            <div class="meta">
                <c:choose>
                    <c:when test="${k.delta == null}"></c:when>
                    <c:when test="${k.delta > 0}"><span class="delta up">▲ ${k.delta}%</span></c:when>
                    <c:when test="${k.delta < 0}"><span class="delta down">▼ ${-k.delta}%</span></c:when>
                    <c:otherwise><span class="delta flat">— 0%</span></c:otherwise>
                </c:choose>
                <span>${k.sub}</span>
            </div>
            <c:if test="${not empty k.progress}"><div class="bar thin"><i data-w="${k.progress}"></i></div></c:if>
        </div>
    </c:forEach>
</section>

<%-- ===== Biểu đồ + cảnh báo ===== --%>
<section class="grid g-main mt">
    <div class="card">
        <div class="card-h">
            <div><h2>Xu hướng bán vé</h2><div class="hint">So sánh theo từng ngày trong ${days} ngày gần nhất</div></div>
            <div class="seg" id="chartSeg">
                <button class="on" data-k="revenue">Doanh thu</button>
                <button data-k="tickets">Vé</button>
                <button data-k="orders">Đơn</button>
            </div>
        </div>
        <div class="chart" id="lineChart"></div>
    </div>

    <div class="stack">
        <div class="card">
            <div class="card-h"><h2>Cần chú ý</h2><span class="pill ${empty d.alerts ? 'ok' : 'pink'}">${empty d.alerts ? 'Ổn định' : d.alerts.size()}</span></div>
            <div class="list">
                <c:forEach items="${d.alerts}" var="a">
                    <div class="alert ${a.level}">
                        <span class="ic">${a.icon}</span>
                        <span class="tx">${a.text}</span>
                        <a class="btn btn-sm" href="${ctx}${a.href}">${a.cta}</a>
                    </div>
                </c:forEach>
                <c:if test="${empty d.alerts}">
                    <div class="alert good"><span class="ic">🎉</span><span class="tx">Mọi thứ đều ổn! Không có đơn tồn đọng hay loại vé sắp hết.</span></div>
                </c:if>
            </div>
        </div>
        <div class="card">
            <div class="card-h"><h2>Phương thức thanh toán</h2></div>
            <div class="donut-wrap"><div id="donut"></div><div class="legend" id="donutLegend"></div></div>
        </div>
    </div>
</section>

<%-- ===== Đơn gần đây + Top sự kiện / Loại vé ===== --%>
<section class="grid g-main mt">
    <div class="card">
        <div class="card-h"><h2>Đơn hàng gần đây</h2><a class="btn btn-sm" href="${ctx}/admin/orders${eventId > 0 ? '?event='.concat(eventId) : ''}">Xem tất cả →</a></div>
        <c:choose>
            <c:when test="${empty d.recent}"><div class="empty"><b>Chưa có đơn hàng</b>Đơn mới sẽ xuất hiện tại đây.</div></c:when>
            <c:otherwise>
                <div class="tbl-wrap"><table class="tbl" style="min-width:0">
                    <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Sự kiện</th><th class="num">Tổng tiền</th><th>Trạng thái</th></tr></thead>
                    <tbody>
                    <c:forEach items="${d.recent}" var="o">
                        <tr class="click" onclick="location.href='${ctx}/admin/orders?q=${o.orderCode.replace('#','%23')}'">
                            <td><span class="code">${o.orderCode}</span><div class="mut" style="font-size:12px;margin-top:3px">${o.createdText}</div></td>
                            <td><b>${o.customerName}</b><div class="mut" style="font-size:12px">${o.totalQuantity} vé</div></td>
                            <td style="max-width:220px"><span style="display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${o.eventTitle}</span></td>
                            <td class="num strong">${o.formattedTotal}</td>
                            <td><span class="pill ${o.paymentStatus == 'Đã thanh toán' ? 'ok' : o.paymentStatus == 'Chờ thanh toán' ? 'wait' : 'bad'}">${o.paymentStatus}</span></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table></div>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <c:choose>
            <c:when test="${not empty selected}">
                <div class="card-h"><h2>Hạng vé</h2><a class="btn btn-sm" href="${ctx}/admin/ticket-types?eventId=${selected.eventId}">Quản lý</a></div>
                <div class="list" style="gap:16px">
                    <c:forEach items="${ticketTypes}" var="t">
                        <div>
                            <div style="display:flex;justify-content:space-between;font-weight:800;margin-bottom:6px">
                                <span>${t.typeName}</span><span>${t.soldQuantity} / ${t.totalQuantity}</span>
                            </div>
                            <div class="bar" style="--c:${t.barColor}"><i data-w="${t.fillPercent}"></i></div>
                            <div class="mut" style="font-size:12.5px;margin-top:4px">${t.formattedPrice} · còn ${t.remaining} vé</div>
                        </div>
                    </c:forEach>
                    <c:if test="${empty ticketTypes}"><div class="empty"><b>Chưa có hạng vé</b>Thêm hạng vé để mở bán.</div></c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="card-h"><h2>Top sự kiện</h2><span class="hint">theo doanh thu kỳ này</span></div>
                <div class="list" style="gap:14px">
                    <c:forEach items="${d.topEvents}" var="t" varStatus="st">
                        <a class="li" href="${ctx}/dashboard?eventId=${t.id}&days=${days}" style="text-decoration:none">
                            <span class="rank r${st.index + 1}">${st.index + 1}</span>
                            <img src="${ctx}/${t.image}" alt="" loading="lazy">
                            <span class="grow"><b>${t.title}</b><small>${t.tickets} vé</small></span>
                            <span class="end">${t.revenueText}</span>
                        </a>
                    </c:forEach>
                    <c:if test="${empty d.topEvents}"><div class="empty"><b>Chưa có dữ liệu</b></div></c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>


</c:if>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<c:if test="${not empty d}">
<script>
(function () {
    var C = ${chartJson};
    var fmtMoney = function (v) { return A.vnd(v); };
    var chart = Charts.line(document.getElementById('lineChart'), {
        labels: C.labels, active: 'revenue', height: 300,
        series: {
            revenue: { name: 'Doanh thu', color: '#c8f51f', data: C.revenue, fmt: fmtMoney },
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
    var total = items.reduce(function (a, b) { return a + b.value; }, 0);
    Charts.donut(document.getElementById('donut'), document.getElementById('donutLegend'), items, { center: total, caption: 'đơn' });
})();
</script>
</c:if>
</body>
</html>
