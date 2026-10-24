<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<form class="toolbar no-print" method="get" action="${ctx}/admin/events">
    <div class="search grow">
        <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <input class="inp" type="search" name="q" value="${q}" placeholder="Tìm theo tên hoặc địa điểm...">
    </div>
    <select class="inp" name="category" style="width:180px" onchange="this.form.submit()">
        <option value="0">Mọi danh mục</option>
        <c:forEach items="${categories}" var="c"><option value="${c.categoryId}" ${c.categoryId == categoryId ? 'selected' : ''}>${c.categoryName}</option></c:forEach>
    </select>
    <select class="inp" name="status" style="width:170px" onchange="this.form.submit()">
        <option value="">Mọi trạng thái</option>
        <c:forEach items="${statuses}" var="s"><option value="${s}" ${s == statusFilter ? 'selected' : ''}>${s}</option></c:forEach>
    </select>
    <button class="btn" type="submit">Lọc</button>
    <button class="btn btn-primary" type="button" id="btnNew">＋ Tạo sự kiện</button>
</form>

<c:choose>
<c:when test="${empty events}">
    <div class="empty"><b>Chưa có sự kiện nào</b>Bấm “Tạo sự kiện” để bắt đầu bán vé.</div>
</c:when>
<c:otherwise>
<div class="ev-grid">
<c:forEach items="${events}" var="e">
    <article class="evc" data-id="${e.eventId}">
        <div class="im">
            <img src="${ctx}/${e.imagePath}" alt="${e.title}" loading="lazy">
            <div class="tl">
                <span class="pill ${e.status == 'Đang mở bán' ? 'ok' : e.status == 'Sắp diễn ra' ? 'info' : 'mut'}">${e.status}</span>
                <span class="pill" style="background:${e.categoryColor}">${e.categoryName}</span>
            </div>
            <button class="star ${e.featured ? 'on' : ''}" data-act="feature" data-v="${e.featured ? 0 : 1}" title="${e.featured ? 'Bỏ ghim nổi bật' : 'Ghim làm sự kiện nổi bật ở trang chủ'}">★</button>
        </div>
        <div class="bd">
            <h3>${e.title}</h3>
            <div class="meta"><span>🗓 ${e.fullDateStr}</span><span>📍 ${e.location}</span></div>
            <div class="nums">
                <div><b>${e.soldTickets}</b><small>vé đã bán</small></div>
                <div><b>${e.orderCount}</b><small>đơn hàng</small></div>
                <div><b>${e.formattedPrice}</b><small>giá từ</small></div>
            </div>
            <div>
                <div style="display:flex;justify-content:space-between;font-weight:800;font-size:12.5px;margin-bottom:5px"><span>Lấp đầy</span><span>${e.soldTickets}/${e.totalCapacity} · ${e.fillPercent}%</span></div>
                <div class="bar thin"><i data-w="${e.fillPercent}"></i></div>
            </div>
            <div style="font-weight:800">Doanh thu: <span class="money" data-v="${e.revenue}">0đ</span></div>
        </div>
        <div class="acts">
            <button class="btn btn-sm btn-lime" data-act="edit">✎ Sửa</button>
            <a class="btn btn-sm" href="${ctx}/admin/ticket-types?eventId=${e.eventId}">Loại vé</a>
            <a class="btn btn-sm" href="${ctx}/dashboard?eventId=${e.eventId}">Thống kê</a>
            <c:choose>
                <c:when test="${e.status == 'Tạm ẩn'}"><button class="btn btn-sm btn-yel" data-act="status" data-v="Sắp diễn ra">Hiện lại</button></c:when>
                <c:otherwise><button class="btn btn-sm" data-act="status" data-v="Tạm ẩn">Ẩn</button></c:otherwise>
            </c:choose>
            <button class="btn btn-sm btn-danger" data-act="delete" data-n="${e.orderCount}">Xóa</button>
        </div>
    </article>
</c:forEach>
</div>
</c:otherwise>
</c:choose>

<%-- ===== Form tạo / sửa ===== --%>
<div class="back" id="evModal" data-static>
    <form class="modal lg" id="evForm" autocomplete="off" novalidate>
        <div class="modal-h"><h2 id="evTitle">Tạo sự kiện</h2><button class="x" type="button" data-close aria-label="Đóng">✕</button></div>
        <div class="modal-b">
            <input type="hidden" name="action" value="save">
            <input type="hidden" name="id" id="fId" value="0">
            <input type="hidden" name="imageChoice" id="fImageChoice" value="">
            <div class="grid g2" style="gap:24px;align-items:start">
                <div class="form-grid">
                    <div class="field"><label for="fTitle">Tên sự kiện <em>*</em></label><input class="inp" id="fTitle" name="title" maxlength="200" placeholder="VD: Đêm nhạc Mùa hè 2026" required></div>
                    <div class="row2">
                        <div class="field"><label for="fCat">Danh mục <em>*</em></label>
                            <select class="inp" id="fCat" name="categoryId"><c:forEach items="${categories}" var="c"><option value="${c.categoryId}">${c.categoryName}</option></c:forEach></select></div>
                        <div class="field"><label for="fStatus">Trạng thái</label>
                            <select class="inp" id="fStatus" name="status"><c:forEach items="${statuses}" var="s"><option value="${s}">${s}</option></c:forEach></select></div>
                    </div>
                    <div class="field"><label for="fDate">Ngày giờ diễn ra <em>*</em></label><input class="inp" type="datetime-local" id="fDate" name="date" required></div>
                    <div class="field"><label for="fLoc">Địa điểm <em>*</em></label><input class="inp" id="fLoc" name="location" maxlength="255" placeholder="VD: Sân vận động Quốc gia Mỹ Đình, Hà Nội" required></div>
                    <div class="row2">
                        <div class="field"><label for="fCap">Sức chứa (vé) <em>*</em></label><input class="inp" type="number" min="1" id="fCap" name="capacity" placeholder="2500" required></div>
                        <div class="field" id="priceField"><label for="fPrice">Giá vé thấp nhất (đ)</label><input class="inp" type="number" min="0" step="1000" id="fPrice" name="minPrice" placeholder="350000"><small>Giá từng hạng chỉnh ở mục Loại vé</small></div>
                    </div>
                </div>
                <div class="form-grid">
                    <div class="img-pick">
                        <label style="font-weight:800;font-size:13px">Ảnh sự kiện</label>
                        <div class="img-preview" id="imgPrev"><span>Chưa chọn ảnh</span></div>
                        <div class="presets" id="presets">
                            <c:forEach begin="1" end="6" var="i"><button type="button" data-p="assets/images/events/event-${i}.svg" title="Ảnh mẫu ${i}"><img src="${ctx}/assets/images/events/event-${i}.svg" alt=""></button></c:forEach>
                        </div>
                        <label class="btn btn-sm" style="justify-self:start;cursor:pointer">⬆ Tải ảnh lên<input type="file" id="fFile" name="image" accept="image/png,image/jpeg,image/gif,image/webp" hidden></label>
                        <small class="mut">PNG, JPG, GIF hoặc WEBP, tối đa 3MB. Tỷ lệ 3:2 hiển thị đẹp nhất.</small>
                    </div>
                    <div class="field"><label for="fTag">Nhãn nổi bật</label><input class="inp" id="fTag" name="tagLabel" maxlength="100" placeholder="VD: NỔI BẬT · 20/07"></div>
                    <div class="field"><label for="fDesc">Mô tả ngắn</label><textarea class="inp" id="fDesc" name="description" maxlength="2000" placeholder="Giới thiệu sự kiện, hiển thị ở banner trang chủ..."></textarea></div>
                    <div class="color-inp"><div class="field" style="flex:1"><label for="fColor">Màu chủ đạo</label>
                        <div class="color-inp"><input type="color" id="fColor" name="bannerColor" value="#3a68ff"><div class="swatches" id="swatches"></div></div></div></div>
                    <label class="check"><input type="checkbox" id="fFeat" name="featured" value="1"> Ghim làm sự kiện nổi bật ở trang chủ</label>
                </div>
            </div>
        </div>
        <div class="modal-f">
            <button class="btn" type="button" data-close>Hủy</button>
            <button class="btn btn-primary" type="submit" id="evSave">Lưu sự kiện</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<script>
(function () {
    var form = document.getElementById('evForm');
    var $ = function (id) { return document.getElementById(id); };
    var colors = ['#3a68ff', '#ffd400', '#ff62aa', '#c8f51f', '#20d5ec', '#ff7a1a', '#8b5cf6'];
    $('swatches').innerHTML = colors.map(function (c) { return '<span class="swatch" data-c="' + c + '" style="background:' + c + '"></span>'; }).join('');
    $('swatches').addEventListener('click', function (e) { var s = e.target.closest('.swatch'); if (s) $('fColor').value = s.getAttribute('data-c'); });

    // định dạng tiền cho "Doanh thu"
    A.$$('.money').forEach(function (m) { m.textContent = A.vnd(m.getAttribute('data-v')); });

    function setImage(path, objectUrl) {
        $('fImageChoice').value = path || '';
        var box = $('imgPrev');
        if (!path && !objectUrl) { box.innerHTML = '<span>Chưa chọn ảnh</span>'; }
        else box.innerHTML = '<img src="' + (objectUrl || (A.ctx + '/' + path)) + '" alt="">';
        A.$$('#presets button').forEach(function (b) { b.classList.toggle('on', b.getAttribute('data-p') === path); });
    }
    $('presets').addEventListener('click', function (e) {
        var b = e.target.closest('button[data-p]'); if (!b) return;
        $('fFile').value = ''; setImage(b.getAttribute('data-p'));
    });
    $('fFile').addEventListener('change', function () {
        var f = this.files[0]; if (!f) return;
        if (f.size > 3 * 1024 * 1024) { A.toast('Ảnh quá lớn (tối đa 3MB)', 'err'); this.value = ''; return; }
        A.$$('#presets button').forEach(function (b) { b.classList.remove('on'); });
        $('fImageChoice').value = '';
        $('imgPrev').innerHTML = '<img src="' + URL.createObjectURL(f) + '" alt="">';
    });

    function reset() {
        form.reset();
        $('fId').value = '0';
        $('fColor').value = '#3a68ff';
        setImage('assets/images/events/event-1.svg');
        $('priceField').style.display = '';
        A.$$('.inp.err', form).forEach(function (i) { i.classList.remove('err'); });
    }

    $('btnNew').addEventListener('click', function () {
        reset();
        $('evTitle').textContent = 'Tạo sự kiện mới';
        $('fStatus').value = 'Sắp diễn ra';
        A.open('evModal');
    });

    function edit(id) {
        A.get(A.ctx + '/admin/events?format=json&id=' + id).then(function (e) {
            reset();
            $('evTitle').textContent = 'Sửa sự kiện';
            $('fId').value = e.id; $('fTitle').value = e.title || ''; $('fCat').value = e.categoryId;
            $('fStatus').value = e.status || 'Sắp diễn ra'; $('fDate').value = e.date || '';
            $('fLoc').value = e.location || ''; $('fCap').value = e.capacity; $('fTag').value = e.tagLabel || '';
            $('fDesc').value = e.description || ''; $('fColor').value = e.bannerColor || '#3a68ff';
            $('fFeat').checked = !!e.featured;
            $('priceField').style.display = 'none';
            setImage(e.image);
            A.open('evModal');
        }).catch(function (er) { A.toast(er.message, 'err'); });
    }

    form.addEventListener('submit', function (ev) {
        ev.preventDefault();
        var bad = null;
        ['fTitle', 'fLoc', 'fDate', 'fCap'].forEach(function (id) {
            var el = $(id), ok = el.value.trim() !== '';
            el.classList.toggle('err', !ok); if (!ok && !bad) bad = el;
        });
        if ($('fId').value === '0' && ($('fPrice').value === '' || Number($('fPrice').value) < 0)) { $('fPrice').classList.add('err'); bad = bad || $('fPrice'); }
        else $('fPrice').classList.remove('err');
        if (bad) { bad.focus(); A.toast('Vui lòng điền đủ các trường bắt buộc', 'err'); return; }
        var fd = new FormData(form);
        if (!$('fFeat').checked) fd.delete('featured');
        if (!$('fFile').files.length) fd.delete('image');
        A.run($('evSave'), function () { return A.post(A.ctx + '/admin/events', fd); }).then(function (r) {
            A.close('evModal');
            setTimeout(function () { location.reload(); }, 700);
        }).catch(function () {});
    });

    document.querySelector('.ev-grid') && document.querySelector('.ev-grid').addEventListener('click', function (e) {
        var b = e.target.closest('[data-act]'); if (!b) return;
        var card = b.closest('.evc'), id = card.getAttribute('data-id'), act = b.getAttribute('data-act');
        var title = card.querySelector('h3').textContent;
        if (act === 'edit') return edit(id);
        var reload = function () { setTimeout(function () { location.reload(); }, 600); };
        if (act === 'feature') return A.run(b, function () { return A.post(A.ctx + '/admin/events', { action: 'feature', id: id, value: b.getAttribute('data-v') }); }).then(reload);
        if (act === 'status') return A.run(b, function () { return A.post(A.ctx + '/admin/events', { action: 'status', id: id, value: b.getAttribute('data-v') }); }).then(reload);
        if (act === 'delete') {
            var n = Number(b.getAttribute('data-n'));
            if (n > 0) return A.confirm({ title: 'Không thể xóa', text: 'Sự kiện “' + title + '” đã có ' + n + ' đơn hàng. Bạn có muốn chuyển sang trạng thái “Tạm ẩn” để ngừng bán không?', ok: 'Tạm ẩn sự kiện' })
                .then(function (y) { if (y) A.run(b, function () { return A.post(A.ctx + '/admin/events', { action: 'status', id: id, value: 'Tạm ẩn' }); }).then(reload); });
            return A.confirm({ title: 'Xóa sự kiện?', text: 'Xóa vĩnh viễn “' + title + '” cùng các hạng vé của nó. Hành động này không thể hoàn tác.', ok: 'Xóa', danger: true })
                .then(function (y) { if (y) A.run(b, function () { return A.post(A.ctx + '/admin/events', { action: 'delete', id: id }); }).then(function () { card.style.transition = 'all .3s'; card.style.opacity = 0; card.style.transform = 'scale(.9)'; reload(); }); });
        }
    });
})();
</script>
</body>
</html>
