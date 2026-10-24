package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.dao.ReportDAO;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Báo cáo doanh thu: theo khoảng ngày, sự kiện, loại vé, phương thức thanh toán; xuất CSV / in. */
@WebServlet(name = "AdminReportsServlet", urlPatterns = {"/admin/reports"})
public class AdminReportsServlet extends HttpServlet {

    private final ReportDAO dao = new ReportDAO();
    private final EventDAO eventDAO = new EventDAO();

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        LocalDate from = Web.datep(req, "from");
        LocalDate to = Web.datep(req, "to");
        String preset = Web.str(req, "preset");
        if (!preset.isEmpty()) {
            to = today;
            switch (preset) {
                case "7": from = today.minusDays(6); break;
                case "90": from = today.minusDays(89); break;
                case "month": from = today.withDayOfMonth(1); break;
                case "year": from = today.withDayOfYear(1); break;
                default: from = today.minusDays(29);
            }
        }
        if (to == null) to = today;
        if (from == null) from = to.minusDays(29);
        if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }
        if (ChronoUnit.DAYS.between(from, to) > 730) from = to.minusDays(730);
        int eventId = Web.intp(req, "event", 0);
        long span = ChronoUnit.DAYS.between(from, to) + 1;
        String gran = Web.str(req, "gran");
        if (!gran.equals("day") && !gran.equals("week") && !gran.equals("month")) gran = span > 180 ? "month" : (span > 60 ? "week" : "day");

        try {
            Map<String, Object> r = dao.report(from, to, eventId, gran);

            if ("csv".equals(req.getParameter("export"))) { export(resp, r, from, to, gran); return; }

            Map<String, Object> chart = new LinkedHashMap<>();
            chart.put("labels", r.get("labels"));
            chart.put("revenue", r.get("revenueSeries"));
            chart.put("tickets", r.get("ticketSeries"));
            chart.put("orders", r.get("orderSeries"));
            chart.put("payments", r.get("payments"));

            Admin.prepare(req, "reports", "Báo cáo", "Phân tích doanh thu theo thời gian, sự kiện và loại vé");
            req.setAttribute("r", r);
            req.setAttribute("summary", r.get("summary"));
            req.setAttribute("chartJson", Web.GSON.toJson(chart));
            req.setAttribute("events", eventDAO.adminList("", 0, ""));
        } catch (Exception e) {
            e.printStackTrace();
            Admin.prepare(req, "reports", "Báo cáo", "");
            req.setAttribute("loadError", e.getMessage());
        }
        req.setAttribute("from", from.toString());
        req.setAttribute("to", to.toString());
        req.setAttribute("fromText", Web.date(from));
        req.setAttribute("toText", Web.date(to));
        req.setAttribute("eventId", eventId);
        req.setAttribute("gran", gran);
        req.getRequestDispatcher("/WEB-INF/views/admin/reports.jsp").forward(req, resp);
    }

    @SuppressWarnings("unchecked")
    private void export(HttpServletResponse resp, Map<String, Object> r, LocalDate from, LocalDate to, String gran) throws IOException {
        PrintWriter w = Csv.begin(resp, "bao-cao-" + from + "_" + to + ".csv");
        Map<String, Object> s = (Map<String, Object>) r.get("summary");
        Csv.row(w, "BÁO CÁO DOANH THU TICKETHUB", Web.date(from) + " - " + Web.date(to));
        Csv.row(w);
        Csv.row(w, "TỔNG QUAN");
        Csv.row(w, "Doanh thu (đơn đã thanh toán)", s.get("revenue"));
        Csv.row(w, "Phí dịch vụ đã thu", s.get("fee"));
        Csv.row(w, "Doanh thu thuần (trừ phí dịch vụ)", s.get("net"));
        Csv.row(w, "Giảm giá đã áp dụng", s.get("discount"));
        Csv.row(w, "Tổng số đơn", s.get("orders"));
        Csv.row(w, "Đơn đã thanh toán", s.get("paidOrders"));
        Csv.row(w, "Đơn chờ thanh toán", s.get("pending"));
        Csv.row(w, "Đơn đã hủy", s.get("cancelled"));
        Csv.row(w, "Vé đã đặt (không tính hủy)", s.get("tickets"));
        Csv.row(w, "Giá trị đơn trung bình", s.get("aov"));
        Csv.row(w);
        Csv.row(w, "THEO " + ("month".equals(gran) ? "THÁNG" : "week".equals(gran) ? "TUẦN" : "NGÀY"), "Doanh thu", "Vé", "Đơn");
        List<String> labels = (List<String>) r.get("labels");
        List<Long> rev = (List<Long>) r.get("revenueSeries");
        List<Integer> tix = (List<Integer>) r.get("ticketSeries");
        List<Integer> ord = (List<Integer>) r.get("orderSeries");
        for (int i = 0; i < labels.size(); i++) Csv.row(w, labels.get(i), rev.get(i), tix.get(i), ord.get(i));
        Csv.row(w);
        Csv.row(w, "THEO SỰ KIỆN", "Số đơn", "Số vé", "Doanh thu", "Tỷ trọng %");
        for (Map<String, Object> e : (List<Map<String, Object>>) r.get("byEvent")) Csv.row(w, e.get("title"), e.get("orders"), e.get("tickets"), e.get("revenue"), e.get("share"));
        Csv.row(w);
        Csv.row(w, "THEO LOẠI VÉ", "Sự kiện", "Số vé", "Doanh thu");
        for (Map<String, Object> e : (List<Map<String, Object>>) r.get("byType")) Csv.row(w, e.get("type"), e.get("event"), e.get("tickets"), e.get("revenue"));
        Csv.row(w);
        Csv.row(w, "KHÁCH HÀNG CHI NHIỀU NHẤT", "Email", "Số đơn", "Chi tiêu");
        for (Map<String, Object> e : (List<Map<String, Object>>) r.get("topCustomers")) Csv.row(w, e.get("name"), e.get("email"), e.get("orders"), e.get("spent"));
    }
}
