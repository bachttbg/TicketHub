<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Phân trang dùng chung. Cần: page, pages, total, pageSize, pagerBase (chuỗi query không gồm page) --%>
<c:if test="${total > 0}">
<div class="pager no-print">
    <span>Hiển thị ${(page - 1) * pageSize + 1}–${page * pageSize > total ? total : page * pageSize} / ${total}</span>
    <c:if test="${pages > 1}">
    <div class="pages">
        <c:if test="${page > 1}"><a href="?page=${page - 1}${pagerBase}" aria-label="Trang trước">‹</a></c:if>
        <c:forEach begin="1" end="${pages}" var="i">
            <c:choose>
                <c:when test="${i == page}"><span class="cur">${i}</span></c:when>
                <c:when test="${i == 1 || i == pages || (i >= page - 1 && i <= page + 1)}"><a href="?page=${i}${pagerBase}">${i}</a></c:when>
                <c:when test="${i == page - 2 || i == page + 2}"><span class="dots">…</span></c:when>
            </c:choose>
        </c:forEach>
        <c:if test="${page < pages}"><a href="?page=${page + 1}${pagerBase}" aria-label="Trang sau">›</a></c:if>
    </div>
    </c:if>
</div>
</c:if>
