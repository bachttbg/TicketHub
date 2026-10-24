package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.dao.TicketTypeDAO;
import com.tickethub.model.Event;
import com.tickethub.model.TicketType;
import com.tickethub.util.Admin;
import com.tickethub.util.Jdbc;
import com.tickethub.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quản lý các hạng vé (giá, số lượng phát hành) của từng sự kiện. */
@WebServlet(name = "AdminTicketTypesServlet", urlPatterns = {"/admin/ticket-types"})
public class AdminTicketTypesServlet extends HttpServlet {

    private final EventDAO eventDAO = new EventDAO();
    private final TicketTypeDAO dao = new TicketTypeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("json".equals(req.getParameter("format"))) {
            try {
                TicketType t = dao.adminGet(Web.intp(req, "id", 0));
                if (t == null) { Web.fail(resp, 404, "Không tìm thấy loại vé."); return; }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("success", true);
                m.put("id", t.getTicketTypeId());
                m.put("name", t.getTypeName());
                m.put("description", t.getDescription());
                m.put("price", t.getPrice());
                m.put("total", t.getTotalQuantity());
                m.put("sold", t.getSoldQuantity());
                m.put("color", t.getBarColor());
                Web.json(resp, 200, m);
            } catch (Exception e) { Admin.handleError(resp, e); }
            return;
        }

        Admin.prepare(req, "tickets", "Loại vé", "Giá bán và số lượng phát hành theo từng hạng vé");
        try {
            List<Event> events = eventDAO.adminList("", 0, "");
            int eventId = Web.intp(req, "eventId", 0);
            Event selected = null;
            for (Event e : events) if (e.getEventId() == eventId) selected = e;
            if (selected == null && !events.isEmpty()) selected = events.get(0);
            req.setAttribute("events", events);
            req.setAttribute("selected", selected);
            if (selected != null) {
                req.setAttribute("types", dao.adminListByEvent(selected.getEventId()));
                req.setAttribute("remainingCapacity", selected.getTotalCapacity() - selected.getIssuedTickets());
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("loadError", e.getMessage());
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/ticket-types.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = Web.str(req, "action");
        try {
            if ("delete".equals(action)) {
                dao.delete(Web.intp(req, "id", 0));
                Web.ok(resp, "Đã xóa loại vé.");
                return;
            }
            if (!"save".equals(action)) { Web.fail(resp, 400, "Yêu cầu không hợp lệ."); return; }

            int id = Web.intp(req, "id", 0);
            int eventId = Web.intp(req, "eventId", 0);
            String name = Web.str(req, "name");
            String desc = Web.str(req, "description");
            String color = Web.str(req, "color");
            int total = Web.intp(req, "total", -1);
            BigDecimal price;
            try { price = Web.decimal(Web.str(req, "price")); } catch (Exception e) { Web.fail(resp, 400, "Giá vé không hợp lệ."); return; }

            if (name.isEmpty() || name.length() > 100) { Web.fail(resp, 400, "Tên hạng vé từ 1 đến 100 ký tự."); return; }
            if (desc.length() > 255) { Web.fail(resp, 400, "Mô tả tối đa 255 ký tự."); return; }
            if (price.signum() < 0 || price.compareTo(new BigDecimal("100000000")) > 0) { Web.fail(resp, 400, "Giá vé phải từ 0 đến 100.000.000đ."); return; }
            if (total < 1) { Web.fail(resp, 400, "Số lượng phát hành phải từ 1 trở lên."); return; }
            if (!color.matches("#[0-9a-fA-F]{6}")) color = "#3a68ff";

            Event ev = eventDAO.adminGet(eventId);
            if (ev == null) { Web.fail(resp, 404, "Sự kiện không tồn tại."); return; }

            TicketType old = id > 0 ? dao.adminGet(id) : null;
            if (id > 0 && (old == null || old.getEventId() != eventId)) { Web.fail(resp, 404, "Loại vé không tồn tại."); return; }
            if (old != null && total < old.getSoldQuantity()) {
                Web.fail(resp, 400, "Đã bán " + old.getSoldQuantity() + " vé nên số lượng phát hành không thể nhỏ hơn con số này.");
                return;
            }
            int others = dao.totalIssued(eventId, id);
            if (others + total > ev.getTotalCapacity()) {
                Web.fail(resp, 400, "Vượt sức chứa sự kiện (" + ev.getTotalCapacity() + " chỗ). Tối đa còn có thể phát hành " + Math.max(0, ev.getTotalCapacity() - others) + " vé cho hạng này.");
                return;
            }

            TicketType t = new TicketType();
            t.setEventId(eventId);
            t.setTypeName(name);
            t.setDescription(desc);
            t.setPrice(price);
            t.setTotalQuantity(total);
            t.setBarColor(color);
            if (id > 0) { t.setTicketTypeId(id); dao.update(t); Web.ok(resp, "Đã lưu loại vé."); }
            else { dao.create(t); Web.ok(resp, "Đã thêm loại vé mới."); }
        } catch (Exception e) {
            Admin.handleError(resp, e);
        }
    }
}
