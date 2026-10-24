package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.dao.OrderDAO;
import com.tickethub.model.Event;
import com.tickethub.model.Order;
import com.tickethub.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Vé của tôi" - dành cho CUSTOMER: chỉ hiển thị các đơn của chính tài khoản đang đăng nhập.
 * ADMIN không có vé cá nhân nên được chuyển sang trang quản lý đơn hàng.
 */
@WebServlet(name = "MyTicketsServlet", urlPatterns = {"/orders"})
public class MyTicketsServlet extends HttpServlet {

    private OrderDAO orderDAO;
    private EventDAO eventDAO;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        eventDAO = new EventDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        if (user == null) { // AuthFilter đã chặn, kiểm tra thêm cho chắc
            response.sendRedirect(request.getContextPath() + "/home?login=1&next=%2Forders");
            return;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/admin/orders");
            return;
        }

        List<Order> orders = orderDAO.getOrdersByUser(user.getUserId());

        Map<Integer, Event> eventMap = new HashMap<>();
        for (Event e : eventDAO.getAllEvents()) {
            eventMap.put(e.getEventId(), e);
        }

        int totalTickets = 0;
        for (Order o : orders) {
            if (!"Đã hủy".equals(o.getPaymentStatus())) totalTickets += o.getTotalQuantity();
        }

        request.setAttribute("orders", orders);
        request.setAttribute("eventMap", eventMap);
        request.setAttribute("totalTickets", totalTickets);
        request.setAttribute("siteInfo", new com.tickethub.dao.SettingsDAO().getAll());
        request.getRequestDispatcher("/WEB-INF/views/my-tickets.jsp").forward(request, response);
    }
}
