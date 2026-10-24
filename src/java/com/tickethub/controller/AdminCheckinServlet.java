package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.dao.OrderDAO;
import com.tickethub.model.Order;
import com.tickethub.model.OrderDetail;
import com.tickethub.util.Admin;
import com.tickethub.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Soát vé tại cổng: nhập/quét mã đơn -> kiểm tra hợp lệ -> ghi nhận số vé vào cổng. */
@WebServlet(name = "AdminCheckinServlet", urlPatterns = {"/admin/checkin"})
public class AdminCheckinServlet extends HttpServlet {

    private final OrderDAO dao = new OrderDAO();
    private final EventDAO eventDAO = new EventDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int eventId = Web.intp(req, "eventId", 0);
        Admin.prepare(req, "checkin", "Soát vé", "Quét mã QR hoặc nhập mã đơn để cho khách vào cổng");
        try {
            if ("json".equals(req.getParameter("format"))) { // làm mới thống kê + danh sách gần đây
                Web.json(resp, 200, live(eventId));
                return;
            }
            req.setAttribute("events", eventDAO.adminList("", 0, ""));
            req.setAttribute("recent", dao.recentCheckins(eventId, 10));
            int[] t = dao.checkinTotals(eventId);
            req.setAttribute("admitted", t[0]);
            req.setAttribute("valid", t[1]);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("loadError", e.getMessage());
        }
        req.setAttribute("eventId", eventId);
        req.getRequestDispatcher("/WEB-INF/views/admin/checkin.jsp").forward(req, resp);
    }

    private Map<String, Object> live(int eventId) {
        int[] t = dao.checkinTotals(eventId);
        List<Map<String, Object>> recent = new ArrayList<>();
        for (Order o : dao.recentCheckins(eventId, 10)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", o.getOrderCode());
            r.put("name", o.getCustomerName());
            r.put("event", o.getEventTitle());
            r.put("qty", o.getCheckedInQty());
            r.put("total", o.getTotalQuantity());
            r.put("time", Web.dateTime(o.getLastCheckinAt()));
            recent.add(r);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("admitted", t[0]);
        m.put("valid", t[1]);
        m.put("percent", Web.pct(t[0], t[1]));
        m.put("recent", recent);
        return m;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = Web.str(req, "action");
        int scope = Web.intp(req, "eventId", 0);
        try {
            if ("lookup".equals(action)) {
                String code = Web.str(req, "code").toUpperCase().replaceAll("\\s+", "");
                if (code.isEmpty()) { Web.fail(resp, 400, "Vui lòng nhập hoặc quét mã đơn."); return; }
                if (!code.startsWith("#")) code = "#" + code;
                Order o = dao.byCode(code);
                if (o == null) { Web.fail(resp, 404, "Không tìm thấy đơn \"" + code + "\"."); return; }
                Web.json(resp, 200, describe(o, scope));
            } else if ("admit".equals(action)) {
                int id = Web.intp(req, "id", 0);
                Order o = dao.detail(id);
                if (o == null) { Web.fail(resp, 404, "Không tìm thấy đơn hàng."); return; }
                if (scope > 0 && o.getEventId() != scope) { Web.fail(resp, 400, "Vé này thuộc sự kiện khác (" + o.getEventTitle() + ")."); return; }
                dao.checkIn(id, Web.intp(req, "qty", 0));
                Map<String, Object> m = describe(dao.detail(id), scope);
                m.put("message", "Đã cho " + Web.intp(req, "qty", 0) + " khách vào cổng.");
                Web.json(resp, 200, m);
            } else if ("undo".equals(action)) {
                dao.resetCheckIn(Web.intp(req, "id", 0));
                Web.ok(resp, "Đã hoàn tác soát vé.");
            } else {
                Web.fail(resp, 400, "Yêu cầu không hợp lệ.");
            }
        } catch (Exception e) {
            Admin.handleError(resp, e);
        }
    }

    /** Trạng thái hợp lệ của vé: ok / partial / done / unpaid / cancelled / wrong_event. */
    private Map<String, Object> describe(Order o, int scope) {
        String state, message;
        if (o.isCancelled()) { state = "cancelled"; message = "Đơn đã bị hủy – KHÔNG hợp lệ."; }
        else if (!o.isPaid()) { state = "unpaid"; message = "Đơn chưa thanh toán – chưa được vào cổng."; }
        else if (scope > 0 && o.getEventId() != scope) { state = "wrong_event"; message = "Vé thuộc sự kiện khác: " + o.getEventTitle(); }
        else if (o.getRemainingToCheckin() == 0) { state = "done"; message = "Tất cả vé của đơn này đã vào cổng."; }
        else if (o.getCheckedInQty() > 0) { state = "partial"; message = "Hợp lệ – còn " + o.getRemainingToCheckin() + " vé chưa vào."; }
        else { state = "ok"; message = "Vé hợp lệ – cho phép vào cổng."; }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("state", state);
        m.put("message", message);
        m.put("id", o.getOrderId());
        m.put("code", o.getOrderCode());
        m.put("name", o.getCustomerName());
        m.put("phone", o.getCustomerPhone());
        m.put("event", o.getEventTitle());
        m.put("image", Web.eventImage(o.getEventId(), o.getEventImage()));
        m.put("quantity", o.getTotalQuantity());
        m.put("checkedIn", o.getCheckedInQty());
        m.put("remaining", o.getRemainingToCheckin());
        m.put("lastCheckin", Web.dateTime(o.getLastCheckinAt()));
        List<String> items = new ArrayList<>();
        if (o.getDetails() != null) for (OrderDetail d : o.getDetails()) items.add(d.getTicketTypeName() + " × " + d.getQuantity());
        m.put("items", items);
        m.put("canAdmit", "ok".equals(state) || "partial".equals(state));
        return m;
    }
}
