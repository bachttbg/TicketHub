package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.dao.TicketTypeDAO;
import com.tickethub.model.Category;
import com.tickethub.model.Event;
import com.tickethub.model.TicketType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "HomeController", urlPatterns = {"", "/home"})
public class HomeController extends HttpServlet {

    private EventDAO eventDAO;
    private TicketTypeDAO ticketTypeDAO;

    @Override
    public void init() throws ServletException {
        eventDAO = new EventDAO();
        ticketTypeDAO = new TicketTypeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String keyword = request.getParameter("q");
        String category = request.getParameter("category");

        List<Event> eventList;
        if ((keyword != null && !keyword.trim().isEmpty()) || (category != null && !category.trim().isEmpty())) {
            eventList = eventDAO.searchEvents(keyword, category);
        } else {
            eventList = eventDAO.getAllEvents();
        }

        Event featuredEvent = eventDAO.getFeaturedEvent();
        if (featuredEvent == null && !eventList.isEmpty()) {
            featuredEvent = eventList.get(0);
        }

        List<Category> categories = eventDAO.getAllCategories();

        List<TicketType> featuredTicketTypes = null;
        if (featuredEvent != null) {
            featuredTicketTypes = ticketTypeDAO.getTicketTypesByEventId(featuredEvent.getEventId());
        }

        request.setAttribute("featuredEvent", featuredEvent);
        request.setAttribute("eventList", eventList);
        request.setAttribute("categories", categories);
        request.setAttribute("featuredTicketTypes", featuredTicketTypes);
        request.setAttribute("currentCategory", category != null ? category : "Tất cả");
        request.setAttribute("keyword", keyword);

        request.setAttribute("siteInfo", new com.tickethub.dao.SettingsDAO().getAll());
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    }
}
