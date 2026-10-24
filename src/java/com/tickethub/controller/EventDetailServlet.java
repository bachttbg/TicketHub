package com.tickethub.controller;

import com.google.gson.Gson;
import com.tickethub.dao.EventDAO;
import com.tickethub.dao.TicketTypeDAO;
import com.tickethub.model.Event;
import com.tickethub.model.TicketType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "EventDetailServlet", urlPatterns = {"/api/event-tickets"})
public class EventDetailServlet extends HttpServlet {

    private EventDAO eventDAO;
    private TicketTypeDAO ticketTypeDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        eventDAO = new EventDAO();
        ticketTypeDAO = new TicketTypeDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        String eventIdStr = request.getParameter("eventId");

        Map<String, Object> result = new HashMap<>();
        try {
            int eventId = Integer.parseInt(eventIdStr);
            Event event = eventDAO.getEventById(eventId);
            List<TicketType> tickets = ticketTypeDAO.getTicketTypesByEventId(eventId);

            if (event != null && !event.isHidden()) {
                com.tickethub.dao.SettingsDAO sd = new com.tickethub.dao.SettingsDAO();
                Map<String, String> st = sd.getAll();
                result.put("serviceFee", sd.serviceFee(st));
                result.put("couponCode", sd.couponCode(st));
                result.put("couponPercent", sd.couponPercent(st));
                result.put("purchasable", event.isPurchasable());
                result.put("success", true);
                result.put("event", event);
                result.put("tickets", tickets);
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy sự kiện");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "ID sự kiện không hợp lệ: " + e.getMessage());
        }

        response.getWriter().write(gson.toJson(result));
    }
}
