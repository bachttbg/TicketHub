<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Phần cuối của khung quản trị: đóng thẻ + nạp script dùng chung --%>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/page.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/admin.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/charts.js"></script>
<script>
    (function () {
        var c = document.getElementById('todayChip');
        if (c) c.textContent = '📅 ' + new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
    })();
</script>
