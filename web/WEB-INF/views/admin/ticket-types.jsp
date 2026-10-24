<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:choose>
<c:when test="${empty selected}">
    <div class="empty"><b>Chưa có sự kiện nào</b>Hãy <a href="${ctx}/admin/events" style="font-weight:800">tạo sự kiện</a> trước khi thêm hạng vé.</div>
</c:when>
<c:otherwise>

<div class="toolbar no-print">
    <div class="field" style="min-width:300px">
        <select class="inp" id="evSel" aria-label="Chọn sự kiện">
            <c:forEach items="${events}" var="e"><option value="${e.eventId}" ${e.eventId == selected.eventId ? 'selected' : ''}>${e.title}</option></c:forEach>
        </select>
    </div>
    <span class="grow"></span>
    <button class="btn btn-primary" id="btnNew" ${remainingCapacity <= 0 ? 'disabled title="Đã phát hành đủ sức chứa"' : ''}>＋ Thêm hạng vé</button>
</div>

<div class="card" style="margin-bottom:20px">
    <div class="hero-ev">
        <img src="${ctx}/${selected.imagePath}" alt="">
        <div class="grow">
            <h2 style="font-size:20px;font-weight:800">${selected.title}</h2>
            <div class="mut" style="margin-top:4px">🗓 ${selected.fullDateStr} · 📍 ${selected.location}</div>
            <div style="margin-top:12px;max-width:520px">
                <div style="display:flex;justify-content:space-between;font-weight:800;margin-bottom:6px">
                    <span>Đã phát hành ${selected.issuedTickets} / ${selected.totalCapacity} chỗ</span>
                    <span class="${remainingCapacity <= 0 ? '' : 'mut'}">${remainingCapacity <= 0 ? 'Đã đủ sức chứa' : 'Còn có thể phát hành '.concat(remainingCapacity)}</span>
                </div>
                <div class="bar"><i data-w="${selected.totalCapacity > 0 ? selected.issuedTickets * 100.0 / selected.totalCapacity : 0}"></i></div>
            </div>
        </div>
        <div style="display:grid;gap:8px;text-align:right">
            <div><div class="live-big" style="font-size:30px">${selected.soldTickets}</div><span class="mut">vé đã bán</span></div>
            <div style="font-weight:800" class="money" data-v="${selected.revenue}">0đ</div>
        </div>
    </div>
</div>

<c:choose>
<c:when test="${empty types}">
    <div class="empty"><b>Chưa có hạng vé nào</b>Thêm ít nhất 1 hạng vé để khách có thể mua.</div>
</c:when>
<c:otherwise>
<div class="tbl-wrap"><table class="tbl">
    <thead><tr><th>Hạng vé</th><th class="num">Giá</th><th>Đã bán / Phát hành</th><th class="num">Còn lại</th><th class="num">Doanh thu</th><th>Tình trạng</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${types}" var="t">
        <tr data-id="${t.ticketTypeId}">
            <td><div style="display:flex;gap:12px;align-items:center"><span style="width:16px;height:16px;border-radius:5px;border:2.5px solid #111;background:${t.barColor};flex:none"></span><div><b>${t.typeName}</b><div class="mut" style="font-size:12.5px">${t.description}</div></div></div></td>
            <td class="num strong">${t.formattedPrice}</td>
            <td style="min-width:200px"><div style="display:flex;justify-content:space-between;font-weight:800;font-size:12.5px;margin-bottom:4px"><span>${t.soldQuantity} / ${t.totalQuantity}</span><span>${t.fillPercent}%</span></div><div class="bar thin" style="--c:${t.barColor}"><i data-w="${t.fillPercent}"></i></div></td>
            <td class="num">${t.remaining}</td>
            <td class="num strong money" data-v="${t.revenue}">0đ</td>
            <td><span class="pill ${t.remaining == 0 ? 'bad' : t.fillPercent >= 85 ? 'wait' : 'ok'}">${t.remaining == 0 ? 'Hết vé' : t.fillPercent >= 85 ? 'Sắp hết' : 'Còn vé'}</span></td>
            <td style="white-space:nowrap"><button class="btn btn-sm btn-lime" data-act="edit">Sửa</button> <button class="btn btn-sm btn-danger" data-act="delete" data-sold="${t.soldQuantity}">Xóa</button></td>
        </tr>
    </c:forEach>
    </tbody>
</table></div>
</c:otherwise>
</c:choose>

<div class="alert info mt"><span class="ic">💡</span><span class="tx">Giá “Từ …đ” hiển thị ngoài trang chủ tự động lấy theo hạng vé rẻ nhất. Loại vé đã có người mua không thể xóa, nhưng bạn có thể giảm số lượng phát hành bằng số vé đã bán để ngừng bán.</span></div>

<div class="back" id="ttModal" data-static>
    <form class="modal sm" id="ttForm" autocomplete="off" novalidate>
        <div class="modal-h"><h2 id="ttTitle">Thêm hạng vé</h2><button class="x" type="button" data-close aria-label="Đóng">✕</button></div>
        <div class="modal-b form-grid">
            <input type="hidden" name="action" value="save"><input type="hidden" name="id" id="tId" value="0"><input type="hidden" name="eventId" value="${selected.eventId}">
            <div class="field"><label for="tName">Tên hạng vé <em>*</em></label><input class="inp" id="tName" name="name" maxlength="100" placeholder="VD: VIP, Thường, Sinh viên" required></div>
            <div class="field"><label for="tDesc">Mô tả quyền lợi</label><input class="inp" id="tDesc" name="description" maxlength="255" placeholder="VD: Khu vực gần sân khấu, tặng kèm quà"></div>
            <div class="row2">
                <div class="field"><label for="tPrice">Giá (đ) <em>*</em></label><input class="inp" type="number" min="0" step="1000" id="tPrice" name="price" required></div>
                <div class="field"><label for="tTotal">Số lượng phát hành <em>*</em></label><input class="inp" type="number" min="1" id="tTotal" name="total" required><small id="tHint"></small></div>
            </div>
            <div class="field"><label>Màu nhận diện</label><div class="color-inp"><input type="color" id="tColor" name="color" value="#3a68ff"><div class="swatches" id="tSw"></div></div></div>
        </div>
        <div class="modal-f"><button class="btn" type="button" data-close>Hủy</button><button class="btn btn-primary" type="submit" id="ttSave">Lưu hạng vé</button></div>
    </form>
</div>

</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<c:if test="${not empty selected}">
<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    var remaining = ${remainingCapacity};
    A.$$('.money').forEach(function (m) { m.textContent = A.vnd(m.getAttribute('data-v')); });
    $('evSel').addEventListener('change', function () { location.href = A.ctx + '/admin/ticket-types?eventId=' + this.value; });
    var cols = ['#3a68ff', '#ffd400', '#ff62aa', '#c8f51f', '#20d5ec', '#ff7a1a', '#8b5cf6'];
    $('tSw').innerHTML = cols.map(function (c) { return '<span class="swatch" data-c="' + c + '" style="background:' + c + '"></span>'; }).join('');
    $('tSw').addEventListener('click', function (e) { var s = e.target.closest('.swatch'); if (s) $('tColor').value = s.getAttribute('data-c'); });

    function openForm(t) {
        $('ttForm').reset();
        $('tId').value = t ? t.id : 0;
        $('ttTitle').textContent = t ? 'Sửa hạng vé' : 'Thêm hạng vé';
        $('tName').value = t ? t.name : ''; $('tDesc').value = t ? (t.description || '') : '';
        $('tPrice').value = t ? t.price : ''; $('tTotal').value = t ? t.total : '';
        $('tColor').value = t ? (t.color || '#3a68ff') : cols[Math.floor(Math.random() * cols.length)];
        $('tHint').textContent = t ? ('Đã bán ' + t.sold + ' vé – không thể đặt thấp hơn.') : ('Còn tối đa ' + remaining + ' vé có thể phát hành.');
        A.$$('.inp.err').forEach(function (i) { i.classList.remove('err'); });
        A.open('ttModal');
    }
    var nb = $('btnNew'); if (nb) nb.addEventListener('click', function () { openForm(null); });

    $('ttForm').addEventListener('submit', function (ev) {
        ev.preventDefault();
        var bad = null;
        ['tName', 'tPrice', 'tTotal'].forEach(function (id) { var el = $(id), ok = el.value.trim() !== ''; el.classList.toggle('err', !ok); if (!ok && !bad) bad = el; });
        if (bad) { bad.focus(); A.toast('Vui lòng điền đủ thông tin', 'err'); return; }
        var fd = new FormData(this);
        A.run($('ttSave'), function () { return A.post(A.ctx + '/admin/ticket-types', fd); }).then(function () { A.close('ttModal'); setTimeout(function () { location.reload(); }, 600); }).catch(function () {});
    });

    var tb = document.querySelector('.tbl tbody');
    tb && tb.addEventListener('click', function (e) {
        var b = e.target.closest('[data-act]'); if (!b) return;
        var tr = b.closest('tr'), id = tr.getAttribute('data-id');
        if (b.getAttribute('data-act') === 'edit') return A.get(A.ctx + '/admin/ticket-types?format=json&id=' + id).then(openForm).catch(function (er) { A.toast(er.message, 'err'); });
        A.confirm({ title: 'Xóa hạng vé?', text: 'Hạng vé “' + tr.querySelector('b').textContent + '” sẽ bị xóa vĩnh viễn.', ok: 'Xóa', danger: true }).then(function (y) {
            if (y) A.run(b, function () { return A.post(A.ctx + '/admin/ticket-types', { action: 'delete', id: id }); }).then(function () { setTimeout(function () { location.reload(); }, 500); }).catch(function () {});
        });
    });
})();
</script>
</c:if>
</body>
</html>
