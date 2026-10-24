package com.tickethub.dao;

import com.tickethub.util.Jdbc;
import com.tickethub.util.Web;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Báo cáo doanh thu theo khoảng ngày / sự kiện, gom theo ngày – tuần – tháng. */
public class ReportDAO {

    private static final String PAID = OrderDAO.PAID, CANCELLED = OrderDAO.CANCELLED, PENDING = OrderDAO.PENDING;

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    public Map<String, Object> report(LocalDate from, LocalDate to, int eventId, String gran) {
        Timestamp a = Web.startOf(from), b = Web.endOf(to);
        String ev = eventId > 0 ? " AND o.event_id = ?" : "";
        Object[] base = eventId > 0 ? new Object[]{a, b, eventId} : new Object[]{a, b};

        // ---- đơn trong kỳ (gom trong Java: chạy được trên mọi CSDL) ----
        List<Object[]> rows = Jdbc.list(
            "SELECT o.created_at, o.total_amount, o.service_fee, o.discount_amount, o.total_quantity, o.payment_status, o.payment_method " +
            "FROM Orders o WHERE o.created_at >= ? AND o.created_at < ?" + ev,
            rs -> new Object[]{rs.getTimestamp(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getInt(5), rs.getString(6), rs.getString(7)},
            base);

        double revenue = 0, fee = 0, discount = 0;
        int paidOrders = 0, pending = 0, cancelled = 0, tickets = 0, paidTickets = 0;
        Map<String, double[]> series = new TreeMap<>();      // key sắp xếp -> {doanh thu, vé, đơn}
        Map<String, String> label = new LinkedHashMap<>();
        Map<String, Integer> pay = new LinkedHashMap<>();
        pay.put("bank", 0); pay.put("qr", 0); pay.put("wallet", 0); pay.put("intl", 0);

        // tạo sẵn các mốc để biểu đồ không bị "đứt"
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String[] k = bucket(d, gran);
            series.putIfAbsent(k[0], new double[3]);
            label.putIfAbsent(k[0], k[1]);
        }
        for (Object[] r : rows) {
            LocalDate d = ((Timestamp) r[0]).toLocalDateTime().toLocalDate();
            double amt = ((BigDecimal) r[1]).doubleValue();
            int qty = (Integer) r[4];
            String st = (String) r[5];
            String[] k = bucket(d, gran);
            double[] s = series.computeIfAbsent(k[0], x -> new double[3]);
            label.putIfAbsent(k[0], k[1]);
            s[2]++;
            if (PAID.equals(st)) {
                paidOrders++; revenue += amt; paidTickets += qty;
                fee += ((BigDecimal) r[2]).doubleValue();
                discount += ((BigDecimal) r[3]).doubleValue();
                s[0] += amt;
            } else if (PENDING.equals(st)) pending++;
            else if (CANCELLED.equals(st)) cancelled++;
            if (!CANCELLED.equals(st)) {
                tickets += qty; s[1] += qty;
                pay.merge(r[6] == null ? "bank" : (String) r[6], 1, Integer::sum);
            }
        }
        int totalOrders = rows.size();

        List<String> labels = new ArrayList<>();
        List<Long> sRev = new ArrayList<>();
        List<Integer> sTix = new ArrayList<>(), sOrd = new ArrayList<>();
        for (Map.Entry<String, double[]> e : series.entrySet()) {
            labels.add(label.get(e.getKey()));
            sRev.add((long) e.getValue()[0]); sTix.add((int) e.getValue()[1]); sOrd.add((int) e.getValue()[2]);
        }

        Map<String, Object> summary = map(
            "revenue", (long) revenue, "revenueText", Web.vnd((long) revenue),
            "revenueShort", Web.vndShort(BigDecimal.valueOf((long) revenue)),
            "netShort", Web.vndShort(BigDecimal.valueOf((long) (revenue - fee))),
            "aovShort", Web.vndShort(BigDecimal.valueOf(paidOrders == 0 ? 0 : (long) (revenue / paidOrders))),
            "net", (long) (revenue - fee), "netText", Web.vnd((long) (revenue - fee)),
            "fee", (long) fee, "feeText", Web.vnd((long) fee),
            "discount", (long) discount, "discountText", Web.vnd((long) discount),
            "orders", totalOrders, "paidOrders", paidOrders, "pending", pending, "cancelled", cancelled,
            "tickets", tickets, "paidTickets", paidTickets,
            "aov", paidOrders == 0 ? 0 : (long) (revenue / paidOrders),
            "aovText", Web.vnd(paidOrders == 0 ? 0 : (long) (revenue / paidOrders)),
            "cancelRate", Web.pct(cancelled, totalOrders));

        String[] pmNames = {"Thẻ ATM", "QR chuyển khoản", "Ví điện tử", "Visa / Mastercard"};
        String[] pmKeys = {"bank", "qr", "wallet", "intl"};
        List<Map<String, Object>> payments = new ArrayList<>();
        for (int i = 0; i < 4; i++) payments.add(map("name", pmNames[i], "value", pay.getOrDefault(pmKeys[i], 0)));

        final double totalRevenue = revenue;
        // ---- theo sự kiện ----
        List<Map<String, Object>> byEvent = Jdbc.list(
            "SELECT e.event_id, e.title, e.total_capacity, " +
            " COUNT(o.order_id) AS orders, " +
            " COALESCE(SUM(CASE WHEN o.payment_status <> ? THEN o.total_quantity ELSE 0 END), 0) AS tix, " +
            " COALESCE(SUM(CASE WHEN o.payment_status = ? THEN o.total_amount ELSE 0 END), 0) AS rev " +
            "FROM Events e LEFT JOIN Orders o ON o.event_id = e.event_id AND o.created_at >= ? AND o.created_at < ? " +
            (eventId > 0 ? "WHERE e.event_id = ? " : "") +
            "GROUP BY e.event_id, e.title, e.total_capacity ORDER BY rev DESC",
            rs -> {
                BigDecimal rv = rs.getBigDecimal("rev");
                return map("id", rs.getInt(1), "title", rs.getString(2), "orders", rs.getInt("orders"), "tickets", rs.getInt("tix"),
                        "revenue", rv.longValue(), "revenueText", Web.vnd(rv), "capacity", rs.getInt(3),
                        "share", totalRevenue <= 0 ? 0 : Web.pct(rv.doubleValue(), totalRevenue));
            },
            eventId > 0 ? new Object[]{CANCELLED, PAID, a, b, eventId} : new Object[]{CANCELLED, PAID, a, b});

        // ---- theo loại vé ----
        List<Map<String, Object>> byType = Jdbc.list(
            "SELECT e.title, tt.type_name, " +
            " COALESCE(SUM(CASE WHEN o.payment_status <> ? THEN od.quantity ELSE 0 END), 0) AS qty, " +
            " COALESCE(SUM(CASE WHEN o.payment_status = ? THEN od.amount ELSE 0 END), 0) AS amt " +
            "FROM TicketTypes tt JOIN Events e ON e.event_id = tt.event_id " +
            "LEFT JOIN OrderDetails od ON od.ticket_type_id = tt.ticket_type_id " +
            "LEFT JOIN Orders o ON o.order_id = od.order_id AND o.created_at >= ? AND o.created_at < ? " +
            (eventId > 0 ? "WHERE tt.event_id = ? " : "") +
            "GROUP BY e.title, tt.type_name, tt.ticket_type_id ORDER BY amt DESC",
            rs -> map("event", rs.getString(1), "type", rs.getString(2), "tickets", rs.getInt(3),
                    "revenue", rs.getBigDecimal(4).longValue(), "revenueText", Web.vnd(rs.getBigDecimal(4))),
            eventId > 0 ? new Object[]{CANCELLED, PAID, a, b, eventId} : new Object[]{CANCELLED, PAID, a, b});

        // ---- khách hàng chi nhiều nhất ----
        List<Map<String, Object>> top = Jdbc.list(
            "SELECT o.customer_name, o.customer_email, COUNT(*) AS orders, " +
            " COALESCE(SUM(CASE WHEN o.payment_status = ? THEN o.total_amount ELSE 0 END), 0) AS spent " +
            "FROM Orders o WHERE o.created_at >= ? AND o.created_at < ?" + ev + " " +
            "GROUP BY o.customer_name, o.customer_email ORDER BY spent DESC OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY",
            rs -> map("name", rs.getString(1), "email", rs.getString(2), "orders", rs.getInt(3),
                    "spent", rs.getBigDecimal(4).longValue(), "spentText", Web.vnd(rs.getBigDecimal(4))),
            eventId > 0 ? new Object[]{PAID, a, b, eventId} : new Object[]{PAID, a, b});

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", summary);
        out.put("labels", labels);
        out.put("revenueSeries", sRev);
        out.put("ticketSeries", sTix);
        out.put("orderSeries", sOrd);
        out.put("payments", payments);
        out.put("byEvent", byEvent);
        out.put("byType", byType);
        out.put("topCustomers", top);
        return out;
    }

    /** {khóa sắp xếp, nhãn hiển thị} cho 1 ngày theo cách gom. */
    private static String[] bucket(LocalDate d, String gran) {
        if ("month".equals(gran)) {
            return new String[]{d.format(DateTimeFormatter.ofPattern("yyyy-MM")), "T" + d.getMonthValue() + "/" + d.getYear()};
        }
        if ("week".equals(gran)) {
            LocalDate mon = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new String[]{mon.toString(), mon.format(DateTimeFormatter.ofPattern("dd/MM"))};
        }
        return new String[]{d.toString(), d.format(DateTimeFormatter.ofPattern("dd/MM"))};
    }
}
