package com.tickethub.controller;

import com.tickethub.dao.DashboardDAO;
import com.tickethub.dao.EventDAO;
import com.tickethub.dao.TicketTypeDAO;
import com.tickethub.model.Event;
import com.tickethub.util.Admin;
import com.tickethub.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tổng quan quản trị (ADMIN): số liệu thật theo sự kiện và khoảng thời gian. */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final TicketTypeDAO ticketTypeDAO = new TicketTypeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int eventId = Web.intp(request, "eventId", 0);
        int days = Web.intp(request, "days", 30);
        if (days != 7 && days != 30 && days != 90) days = 30;

        Admin.prepare(request, "dashboard", "Tổng quan", "Số liệu bán vé theo thời gian thực");
        try {
            Map<String, Object> d = dashboardDAO.load(eventId, days);
            Event selected = eventId > 0 ? eventDAO.adminGet(eventId) : null;
            if (eventId > 0 && selected == null) { eventId = 0; d = dashboardDAO.load(0, days); }

            Map<String, Object> chart = new LinkedHashMap<>();
            chart.put("labels", d.get("labels"));
            chart.put("revenue", d.get("revenueSeries"));
            chart.put("tickets", d.get("ticketSeries"));
            chart.put("orders", d.get("orderSeries"));
            chart.put("payments", d.get("payments"));

            request.setAttribute("d", d);
            request.setAttribute("chartJson", Web.GSON.toJson(chart));
            request.setAttribute("events", eventDAO.adminList("", 0, ""));
            request.setAttribute("selected", selected);
            request.setAttribute("ticketTypes", selected != null ? ticketTypeDAO.adminListByEvent(eventId) : null);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("loadError", e.getMessage());
        }
        request.setAttribute("eventId", eventId);
        request.setAttribute("days", days);
        request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
    }
}
