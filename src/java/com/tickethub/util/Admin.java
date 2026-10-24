package com.tickethub.util;

import com.tickethub.dao.DashboardDAO;
import jakarta.servlet.http.HttpServletRequest;

/** Thông tin dùng chung cho mọi trang quản trị (menu đang chọn, số đơn chờ xử lý...). */
public final class Admin {

    private Admin() {}

    public static void prepare(HttpServletRequest request, String activeMenu, String title, String subtitle) {
        request.setAttribute("activeMenu", activeMenu);
        request.setAttribute("pageTitle", title);
        request.setAttribute("pageSub", subtitle);
        request.setAttribute("pendingCount", new DashboardDAO().pendingOrders());
    }

    public static void handleError(jakarta.servlet.http.HttpServletResponse resp, Exception e) throws java.io.IOException {
        if (e instanceof Jdbc.DataAccessException) {
            Web.fail(resp, 400, e.getMessage());
        } else {
            e.printStackTrace();
            Web.fail(resp, 500, "Lỗi hệ thống: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }
}
