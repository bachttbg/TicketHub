package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.dao.OrderDAO;
import com.tickethub.dao.OrderFilter;
import com.tickethub.model.Order;
import com.tickethub.model.OrderDetail;
import com.tickethub.util.Admin;
import com.tickethub.util.Csv;
import com.tickethub.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quản lý TOÀN BỘ đơn hàng - chỉ dành cho ADMIN (được AuthFilter bảo vệ). */
@WebServlet(name = "OrdersServlet", urlPatterns = {"/admin/orders"})
public class OrdersServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;
    private final OrderDAO dao = new OrderDAO();
    private final EventDAO eventDAO = new EventDAO();

    private OrderFilter filter(HttpServletRequest req) {
        OrderFilter f = new OrderFilter();
        f.keyword = Web.str(req, "q");
        f.status = Web.str(req, "status");
        f.payment = Web.str(req, "pay");
        f.eventId = Web.intp(req, "event", 0);
        f.from = Web.datep(req, "from");
        f.to = Web.datep(req, "to");
        return f;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("json".equals(req.getParameter("format"))) { detail(req, resp); return; }
            OrderFilter f = filter(req);
            if ("csv".equals(req.getParameter("export"))) { export(f, resp); return; }

            int page = Math.max(1, Web.intp(req, "page", 1));
            int total = dao.count(f);
            int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            if (page > pages) page = pages;

            Admin.prepare(req, "orders", "Đơn hàng", "Theo dõi, xác nhận thanh toán và xử lý đơn");
            req.setAttribute("orders", dao.search(f, page, PAGE_SIZE));
            req.setAttribute("counts", dao.statusCounts(f));
            req.setAttribute("revenue", Web.vnd(dao.paidRevenue(f)));
            req.setAttribute("events", eventDAO.adminList("", 0, ""));
            req.setAttribute("total", total);
            req.setAttribute("page", page);
            req.setAttribute("pages", pages);
            req.setAttribute("pageSize", PAGE_SIZE);
            req.setAttribute("f", f);
            req.setAttribute("baseQuery", baseQuery(f, true));
            req.setAttribute("baseNoStatus", baseQuery(f, false));
            req.getRequestDispatcher("/WEB-INF/views/orders.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            Admin.prepare(req, "orders", "Đơn hàng", "");
            req.setAttribute("loadError", e.getMessage());
            req.setAttribute("f", new OrderFilter());
            req.setAttribute("orders", new ArrayList<Order>());
            req.setAttribute("counts", new LinkedHashMap<String, Integer>());
            req.setAttribute("events", new ArrayList<>());
            req.setAttribute("page", 1); req.setAttribute("pages", 1); req.setAttribute("total", 0);
            req.setAttribute("baseQuery", ""); req.setAttribute("baseNoStatus", ""); req.setAttribute("revenue", "0đ");
            req.getRequestDispatcher("/WEB-INF/views/orders.jsp").forward(req, resp);
        }
    }

    private String baseQuery(OrderFilter f, boolean withStatus) {
        StringBuilder sb = new StringBuilder();
        add(sb, "q", f.keyword); if (withStatus) add(sb, "status", f.status); add(sb, "pay", f.payment);
        if (f.eventId > 0) add(sb, "event", String.valueOf(f.eventId));
        if (f.from != null) add(sb, "from", f.from.toString());
        if (f.to != null) add(sb, "to", f.to.toString());
        return sb.toString();
    }

    private void add(StringBuilder sb, String k, String v) {
        if (v == null || v.isEmpty()) return;
        sb.append('&').append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Order o = dao.detail(Web.intp(req, "id", 0));
        if (o == null) { Web.fail(resp, 404, "Không tìm thấy đơn hàng."); return; }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("id", o.getOrderId());
        m.put("code", o.getOrderCode());
        m.put("status", o.getPaymentStatus());
        m.put("created", Web.dateTime(o.getCreatedAt()));
        m.put("name", o.getCustomerName());
        m.put("email", o.getCustomerEmail());
        m.put("phone", o.getCustomerPhone());
        m.put("eventId", o.getEventId());
        m.put("event", o.getEventTitle());
        m.put("image", Web.eventImage(o.getEventId(), o.getEventImage()));
        m.put("payment", Web.payLabel(o.getPaymentMethod()));
        m.put("subtotal", Web.vnd(o.getSubtotal()));
        m.put("fee", Web.vnd(o.getServiceFee()));
        m.put("discount", Web.vnd(o.getDiscountAmount()));
        m.put("hasDiscount", o.getDiscountAmount() != null && o.getDiscountAmount().signum() > 0);
        m.put("total", Web.vnd(o.getTotalAmount()));
        m.put("quantity", o.getTotalQuantity());
        m.put("checkedIn", o.getCheckedInQty());
        m.put("lastCheckin", Web.dateTime(o.getLastCheckinAt()));
        List<Map<String, Object>> items = new ArrayList<>();
        if (o.getDetails() != null) for (OrderDetail d : o.getDetails()) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("type", d.getTicketTypeName());
            it.put("qty", d.getQuantity());
            it.put("unit", Web.vnd(d.getUnitPrice()));
            it.put("amount", Web.vnd(d.getAmount()));
            items.add(it);
        }
        m.put("items", items);
        Web.json(resp, 200, m);
    }

    private void export(OrderFilter f, HttpServletResponse resp) throws IOException {
        PrintWriter w = Csv.begin(resp, "don-hang-" + java.time.LocalDate.now() + ".csv");
        Csv.row(w, "Mã đơn", "Thời gian", "Khách hàng", "Email", "SĐT", "Sự kiện", "Loại vé", "Số vé", "Tạm tính", "Phí dịch vụ", "Giảm giá", "Tổng tiền", "Thanh toán", "Trạng thái", "Đã soát vào cổng");
        for (Order o : dao.search(f, 1, 100000)) {
            Csv.row(w, o.getOrderCode(), o.getCreatedText(), o.getCustomerName(), o.getCustomerEmail(), o.getCustomerPhone(),
                    o.getEventTitle(), o.getTicketTypeSummary(), o.getTotalQuantity(), o.getSubtotal().longValue(),
                    o.getServiceFee().longValue(), o.getDiscountAmount().longValue(), o.getTotalAmount().longValue(),
                    Web.payLabel(o.getPaymentMethod()), o.getPaymentStatus(), o.getCheckedInQty());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String action = Web.str(req, "action");
            int id = Web.intp(req, "id", 0);
            if ("status".equals(action)) {
                dao.updateStatus(id, Web.str(req, "value"));
                Web.ok(resp, "Đã cập nhật trạng thái đơn hàng.");
            } else if ("resetCheckin".equals(action)) {
                dao.resetCheckIn(id);
                Web.ok(resp, "Đã hoàn tác soát vé của đơn này.");
            } else {
                Web.fail(resp, 400, "Yêu cầu không hợp lệ.");
            }
        } catch (Exception e) {
            Admin.handleError(resp, e);
        }
    }
}
