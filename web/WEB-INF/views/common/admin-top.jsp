<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Khung chung của mọi trang quản trị: <head>, thanh bên, thanh trên --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light">
    <title>${pageTitle} – TicketHub Admin</title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/images/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@500;600;700;800;900&display=swap">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css">
</head>
<body class="admin-body" data-ctx="${pageContext.request.contextPath}">
<div class="admin">

    <aside class="side" id="side">
        <a class="side-logo" href="${pageContext.request.contextPath}/dashboard">
            <span class="badge"><img src="${pageContext.request.contextPath}/assets/images/logo.svg" alt="TicketHub"></span>
            <span class="name">TicketHub<small>ADMIN</small></span>
        </a>

        <div class="nav-group">Tổng quan</div>
        <a class="nav-item ${activeMenu == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/dashboard">
            <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/></svg>
            Tổng quan
        </a>

        <div class="nav-group">Quản lý bán vé</div>
        <a class="nav-item ${activeMenu == 'events' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/events">
            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="17" rx="2.5"/><path d="M8 2v4M16 2v4M3 10h18"/></svg>
            Sự kiện
        </a>
        <a class="nav-item ${activeMenu == 'tickets' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/ticket-types">
            <svg viewBox="0 0 24 24"><path d="M3 9a2 2 0 0 0 0 6v3a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-3a2 2 0 0 1 0-6V6a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1z"/><path d="M13 5v14" stroke-dasharray="2 3"/></svg>
            Loại vé
        </a>
        <a class="nav-item ${activeMenu == 'orders' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/orders">
            <svg viewBox="0 0 24 24"><circle cx="9" cy="20" r="1.5"/><circle cx="18" cy="20" r="1.5"/><path d="M2 3h3l2.7 12.4a1 1 0 0 0 1 .8h9.2a1 1 0 0 0 1-.8L21 7H6"/></svg>
            Đơn hàng
            <c:if test="${pendingCount > 0}"><span class="count" title="Đơn chờ thanh toán">${pendingCount}</span></c:if>
        </a>
        <a class="nav-item ${activeMenu == 'checkin' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/checkin">
            <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1.2"/><rect x="14" y="3" width="7" height="7" rx="1.2"/><rect x="3" y="14" width="7" height="7" rx="1.2"/><path d="M14 14h3v3h-3zM20 14v.01M14 20h3M20 17v4"/></svg>
            Soát vé QR
        </a>

        <div class="nav-group">Phân tích</div>
        <a class="nav-item ${activeMenu == 'customers' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/customers">
            <svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14.4c2.3.7 3.5 2.6 3.5 5.6"/></svg>
            Khách hàng
        </a>
        <a class="nav-item ${activeMenu == 'reports' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/reports">
            <svg viewBox="0 0 24 24"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>
            Báo cáo
        </a>

        <div class="nav-group">Hệ thống</div>
        <a class="nav-item ${activeMenu == 'settings' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/settings">
            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></svg>
            Cài đặt
        </a>

        <div class="side-foot">
            <div class="side-user" title="${sessionScope.currentUser.email}">
                <span class="avatar">${sessionScope.currentUser.fullName.substring(0, 1)}</span>
                <span class="who"><b>${sessionScope.currentUser.fullName}</b><span>Quản trị viên</span></span>
            </div>
            <a class="nav-item" href="${pageContext.request.contextPath}/home">
                <svg viewBox="0 0 24 24"><path d="M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>
                Xem website
            </a>
            <a class="nav-item" href="${pageContext.request.contextPath}/auth/logout">
                <svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/></svg>
                Đăng xuất
            </a>
        </div>
    </aside>
    <div class="scrim"></div>

    <div class="main">
        <header class="top">
            <button class="btn btn-ico burger" data-burger aria-label="Mở menu"><svg viewBox="0 0 24 24"><path d="M3 6h18M3 12h18M3 18h18"/></svg></button>
            <div class="grow">
                <h1>${pageTitle}</h1>
                <div class="sub">${pageSub}</div>
            </div>
            <span class="date-chip" id="todayChip"></span>
        </header>
        <main class="content">
            <c:if test="${not empty loadError}">
                <div class="page-error">⚠️ Không tải được dữ liệu: ${loadError}<br><small>Kiểm tra kết nối SQL Server rồi tải lại trang.</small></div>
            </c:if>
