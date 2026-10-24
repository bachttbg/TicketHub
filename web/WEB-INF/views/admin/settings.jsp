<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/common/admin-top.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="tabs">
    <button class="${tab == 'profile' ? 'on' : ''}" data-tab="profile">👤 Hồ sơ</button>
    <button class="${tab == 'security' ? 'on' : ''}" data-tab="security">🔒 Bảo mật</button>
    <button class="${tab == 'system' ? 'on' : ''}" data-tab="system">⚙️ Hệ thống bán vé</button>
</div>

<div class="tab-pane ${tab == 'profile' ? 'on' : ''}" id="tab-profile">
    <form class="card" id="fProfile" style="max-width:640px" autocomplete="off">
        <div class="card-h"><h2>Hồ sơ quản trị viên</h2></div>
        <div class="form-grid">
            <input type="hidden" name="action" value="profile">
            <div class="field"><label>Email đăng nhập</label><input class="inp" value="${me.email}" disabled><small>Email không thể thay đổi.</small></div>
            <div class="field"><label for="pName">Họ và tên <em>*</em></label><input class="inp" id="pName" name="fullName" value="${me.fullName}" maxlength="100" required></div>
            <div class="field"><label for="pPhone">Số điện thoại <em>*</em></label><input class="inp" id="pPhone" name="phone" value="${me.phone}" maxlength="15" required></div>
            <div><button class="btn btn-primary" type="submit">Lưu hồ sơ</button></div>
        </div>
    </form>
</div>

<div class="tab-pane ${tab == 'security' ? 'on' : ''}" id="tab-security">
    <form class="card" id="fPass" style="max-width:640px" autocomplete="off">
        <div class="card-h"><h2>Đổi mật khẩu</h2></div>
        <div class="form-grid">
            <input type="hidden" name="action" value="password">
            <div class="field"><label for="cPw">Mật khẩu hiện tại <em>*</em></label><input class="inp" type="password" id="cPw" name="current" autocomplete="current-password" required></div>
            <div class="row2">
                <div class="field"><label for="nPw">Mật khẩu mới <em>*</em></label><input class="inp" type="password" id="nPw" name="newPassword" minlength="6" autocomplete="new-password" required><small>Ít nhất 6 ký tự</small></div>
                <div class="field"><label for="rPw">Nhập lại mật khẩu mới <em>*</em></label><input class="inp" type="password" id="rPw" name="confirm" autocomplete="new-password" required></div>
            </div>
            <div><button class="btn btn-primary" type="submit">Đổi mật khẩu</button></div>
        </div>
    </form>
</div>

<div class="tab-pane ${tab == 'system' ? 'on' : ''}" id="tab-system">
    <form id="fSys" autocomplete="off">
        <input type="hidden" name="action" value="system">
        <div class="grid g2">
            <div class="card">
                <div class="card-h"><h2>Phí &amp; khuyến mãi</h2></div>
                <div class="form-grid">
                    <div class="field"><label for="sFee">Phí dịch vụ mỗi vé (đ)</label><input class="inp" type="number" min="0" id="sFee" name="service_fee" value="${cfg.service_fee}"><small>Áp dụng cho mọi đơn mới tại trang bán vé.</small></div>
                    <div class="row2">
                        <div class="field"><label for="sCode">Mã giảm giá</label><input class="inp" id="sCode" name="coupon_code" value="${cfg.coupon_code}" maxlength="20" style="text-transform:uppercase"><small>Để trống để tắt mã.</small></div>
                        <div class="field"><label for="sPct">Giảm (%)</label><input class="inp" type="number" min="0" max="100" id="sPct" name="coupon_percent" value="${cfg.coupon_percent}"></div>
                    </div>
                    <div class="alert info"><span class="ic">💡</span><span class="tx">Thay đổi có hiệu lực ngay cho khách tiếp theo; các đơn cũ giữ nguyên giá đã thanh toán.</span></div>
                </div>
            </div>
            <div class="card">
                <div class="card-h"><h2>Thông tin liên hệ (hiện ở chân trang)</h2></div>
                <div class="form-grid">
                    <div class="row2">
                        <div class="field"><label for="sHot">Hotline</label><input class="inp" id="sHot" name="hotline" value="${cfg.hotline}" maxlength="30"></div>
                        <div class="field"><label for="sMail">Email hỗ trợ</label><input class="inp" type="email" id="sMail" name="support_email" value="${cfg.support_email}"></div>
                    </div>
                    <div class="field"><label for="sAddr">Địa chỉ</label><input class="inp" id="sAddr" name="address" value="${cfg.address}" maxlength="200"></div>
                    <div class="field"><label for="sHrs">Giờ hỗ trợ</label><input class="inp" id="sHrs" name="support_hours" value="${cfg.support_hours}" maxlength="100"></div>
                </div>
            </div>
        </div>
        <div class="mt"><button class="btn btn-primary" type="submit">Lưu cài đặt hệ thống</button></div>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/admin-bottom.jsp" %>
<script>
(function () {
    function bind(id, validate) {
        var f = document.getElementById(id);
        f.addEventListener('submit', function (e) {
            e.preventDefault();
            var err = validate && validate(f);
            if (err) { A.toast(err, 'err'); return; }
            var btn = f.querySelector('button[type=submit]');
            A.run(btn, function () { return A.post(A.ctx + '/admin/settings', new FormData(f)); }).then(function () {
                if (id === 'fPass') f.reset();
                if (id === 'fProfile') document.querySelectorAll('.side-user .who b').forEach(function (b) { b.textContent = f.fullName.value; });
            }).catch(function () {});
        });
    }
    bind('fProfile');
    bind('fSys');
    bind('fPass', function (f) {
        if (f.newPassword.value.length < 6) return 'Mật khẩu mới phải có ít nhất 6 ký tự.';
        if (f.newPassword.value !== f.confirm.value) return 'Xác nhận mật khẩu không khớp.';
    });
})();
</script>
</body>
</html>
