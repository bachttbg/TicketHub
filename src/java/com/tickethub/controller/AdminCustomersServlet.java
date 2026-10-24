package com.tickethub.controller;

import com.tickethub.dao.CustomerDAO;
import com.tickethub.dao.OrderDAO;
import com.tickethub.dao.OrderFilter;
import com.tickethub.model.Order;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Danh sách khách hàng kèm số đơn, số vé, tổng chi tiêu. */
@WebServlet(name = "AdminCustomersServlet", urlPatterns = {"/admin/customers"})
public class AdminCustomersServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;
    private final CustomerDAO dao = new CustomerDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = Web.str(req, "q");
        String sort = Web.str(req, "sort");
        try {
            if ("json".equals(req.getParameter("format"))) {
                int id = Web.intp(req, "id", 0);
                Map<String, Object> c = dao.get(id);
                if (c == null) { Web.fail(resp, 404, "Không tìm thấy khách hàng."); return; }
                OrderFilter f = new OrderFilter();
                f.userId = id;
                List<Map<String, Object>> orders = new ArrayList<>();
                for (Order o : orderDAO.search(f, 1, 50)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", o.getOrderCode());
                    m.put("event", o.getEventTitle());
                    m.put("items", o.getTicketTypeSummary() + " × " + o.getTotalQuantity());
                    m.put("total", Web.vnd(o.getTotalAmount()));
                    m.put("status", o.getPaymentStatus());
                    m.put("time", o.getCreatedText());
                    orders.add(m);
                }
                Map<String, Object> out = new LinkedHashMap<>(c);
                out.put("success", true);
                out.put("orderList", orders);
                Web.json(resp, 200, out);
                return;
            }
            if ("csv".equals(req.getParameter("export"))) {
                PrintWriter w = Csv.begin(resp, "khach-hang-" + java.time.LocalDate.now() + ".csv");
                Csv.row(w, "Họ tên", "Email", "SĐT", "Ngày đăng ký", "Số đơn", "Số vé", "Tổng chi tiêu (đã thanh toán)", "Đơn gần nhất");
                for (Map<String, Object> c : dao.list(q, sort, 1, 100000)) {
                    Csv.row(w, c.get("name"), c.get("email"), c.get("phone"), c.get("joined"), c.get("orders"),
                            c.get("tickets"), ((java.math.BigDecimal) c.get("spent")).longValue(), c.get("lastOrder"));
                }
                return;
            }

            int total = dao.count(q);
            int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            int page = Math.min(Math.max(1, Web.intp(req, "page", 1)), pages);
            Admin.prepare(req, "customers", "Khách hàng", "Danh sách và mức chi tiêu của khách hàng");
            req.setAttribute("customers", dao.list(q, sort, page, PAGE_SIZE));
            req.setAttribute("summary", dao.summary());
            req.setAttribute("total", total);
            req.setAttribute("page", page);
            req.setAttribute("pages", pages);
            req.setAttribute("pageSize", PAGE_SIZE);
        } catch (Exception e) {
            e.printStackTrace();
            Admin.prepare(req, "customers", "Khách hàng", "");
            req.setAttribute("loadError", e.getMessage());
            req.setAttribute("customers", new ArrayList<>());
            req.setAttribute("summary", new LinkedHashMap<String, Object>());
            req.setAttribute("total", 0); req.setAttribute("page", 1); req.setAttribute("pages", 1);
        }
        req.setAttribute("q", q);
        req.setAttribute("sort", sort.isEmpty() ? "spent" : sort);
        req.getRequestDispatcher("/WEB-INF/views/admin/customers.jsp").forward(req, resp);
    }
}
