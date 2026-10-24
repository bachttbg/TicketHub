package com.tickethub.controller;

import com.tickethub.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PHÂN QUYỀN THEO TÀI KHOẢN
 *
 *  ADMIN    : /dashboard, /admin/*  (quản trị: tổng quan, tất cả đơn hàng, tạo sự kiện)
 *  CUSTOMER : /orders               (chỉ xem vé của chính mình) và mua vé ở trang chủ
 *
 * - Chưa đăng nhập: chuyển về /home?login=1&next=... (GET) hoặc trả 401 JSON (POST).
 * - Đã đăng nhập nhưng sai vai trò: chuyển về /home?denied=1 (GET) hoặc trả 403 JSON (POST).
 */
@WebFilter(filterName = "AuthFilter",
           urlPatterns = {"/dashboard", "/orders", "/admin/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean adminArea = path.equals("/dashboard") || path.startsWith("/admin/");
        boolean isGet = "GET".equalsIgnoreCase(request.getMethod());

        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("currentUser") : null;

        // 1) Chưa đăng nhập
        if (user == null) {
            if (!isGet) {
                json(response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện chức năng này!");
                return;
            }
            String query = request.getQueryString();
            String next = path + (query != null ? "?" + query : "");
            response.sendRedirect(request.getContextPath() + "/home?login=1&next="
                    + URLEncoder.encode(next, StandardCharsets.UTF_8));
            return;
        }

        // 2) Khu vực quản trị chỉ dành cho ADMIN
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (adminArea && !isAdmin) {
            if (!isGet) {
                json(response, HttpServletResponse.SC_FORBIDDEN, "Tài khoản của bạn không có quyền quản trị!");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/home?denied=1");
            return;
        }

        // 3) Hợp lệ: không cho trình duyệt cache trang riêng tư (đăng xuất xong bấm Back sẽ không còn thấy)
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        chain.doFilter(req, resp);
    }

    private void json(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
