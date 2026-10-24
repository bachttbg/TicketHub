package com.tickethub.dao;

import com.tickethub.model.Event;
import com.tickethub.model.Order;
import com.tickethub.util.Jdbc;
import com.tickethub.util.Web;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Số liệu THẬT cho trang Tổng quan: mọi con số đều tính từ bảng Orders / TicketTypes / Events,
 * không còn số liệu mẫu cố định.
 */
public class DashboardDAO {

    private static final String PAID = OrderDAO.PAID;
    private static final String CANCELLED = OrderDAO.CANCELLED;
    private static final String PENDING = OrderDAO.PENDING;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Tăng/giảm % so với kỳ trước; null nếu kỳ trước = 0. */
    private static Double delta(double cur, double prev) {
        if (prev <= 0) return cur > 0 ? null : 0.0;
        return Math.round((cur - prev) * 1000.0 / prev) / 10.0;
    }

    public Map<String, Object> load(int eventId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        LocalDate prevStart = start.minusDays(days);
        Timestamp tsPrev = Web.startOf(prevStart);
        Timestamp tsStart = Web.startOf(start);
        Timestamp tsEnd = Web.endOf(today);

        // ---- 1) Đơn hàng của kỳ hiện tại + kỳ trước (gộp 1 truy vấn, tính trong Java) ----
        String sql = "SELECT created_at, total_amount, total_quantity, payment_status, payment_method FROM Orders " +
                     "WHERE created_at >= ? AND created_at < ? " + (eventId > 0 ? "AND event_id = ?" : "");
        Object[] p = eventId > 0 ? new Object[]{tsPrev, tsEnd, eventId} : new Object[]{tsPrev, tsEnd};
        List<Object[]> rows = Jdbc.list(sql, rs -> new Object[]{
            rs.getTimestamp(1), rs.getBigDecimal(2), rs.getInt(3), rs.getString(4), rs.getString(5)}, p);

        double revCur = 0, revPrev = 0;
        int tixCur = 0, tixPrev = 0, ordCur = 0, ordPrev = 0, paidCur = 0, paidPrev = 0, pendCur = 0, cancelCur = 0;
        long[] dayRev = new long[days];
        int[] dayTix = new int[days];
        int[] dayOrd = new int[days];
        Map<String, Integer> pay = new LinkedHashMap<>();
        pay.put("bank", 0); pay.put("qr", 0); pay.put("wallet", 0); pay.put("intl", 0);

        for (Object[] r : rows) {
            Timestamp t = (Timestamp) r[0];
            double amt = ((BigDecimal) r[1]).doubleValue();
            int qty = (Integer) r[2];
            String st = (String) r[3];
            boolean cur = !t.before(tsStart);
            boolean paid = PAID.equals(st), cancelled = CANCELLED.equals(st);
            if (cur) {
                ordCur++;
                if (paid) { revCur += amt; paidCur++; }
                if (PENDING.equals(st)) pendCur++;
                if (cancelled) cancelCur++;
                if (!cancelled) {
                    tixCur += qty;
                    String pm = r[4] == null ? "bank" : (String) r[4];
                    pay.merge(pm, 1, Integer::sum);
                }
                int idx = (int) java.time.temporal.ChronoUnit.DAYS.between(start, t.toLocalDateTime().toLocalDate());
                if (idx >= 0 && idx < days) {
                    dayOrd[idx]++;
                    if (paid) dayRev[idx] += (long) amt;
                    if (!cancelled) dayTix[idx] += qty;
                }
            } else {
                ordPrev++;
                if (paid) { revPrev += amt; paidPrev++; }
                if (!cancelled) tixPrev += qty;
            }
        }
        double aovCur = paidCur == 0 ? 0 : revCur / paidCur;
        double aovPrev = paidPrev == 0 ? 0 : revPrev / paidPrev;

        List<String> labels = new ArrayList<>();
        List<Long> sRev = new ArrayList<>();
        List<Integer> sTix = new ArrayList<>(), sOrd = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            labels.add(start.plusDays(i).format(DAY));
            sRev.add(dayRev[i]); sTix.add(dayTix[i]); sOrd.add(dayOrd[i]);
        }

        // ---- 2) Sức chứa / đã bán / đã vào cổng (toàn thời gian) ----
        String w = eventId > 0 ? " WHERE event_id = ?" : "";
        Object[] pe = eventId > 0 ? new Object[]{eventId} : new Object[0];
        long capacity = Jdbc.count("SELECT COALESCE(SUM(total_capacity), 0) FROM Events" + w, pe);
        long issued = Jdbc.count("SELECT COALESCE(SUM(total_quantity), 0) FROM TicketTypes" + w, pe);
        long sold = Jdbc.count("SELECT COALESCE(SUM(sold_quantity), 0) FROM TicketTypes" + w, pe);
        long checkedIn = Jdbc.count("SELECT COALESCE(SUM(checked_in_qty), 0) FROM Orders" + w, pe);
        long validTickets = Jdbc.count("SELECT COALESCE(SUM(total_quantity), 0) FROM Orders WHERE payment_status = ?" + (eventId > 0 ? " AND event_id = ?" : ""),
                eventId > 0 ? new Object[]{PAID, eventId} : new Object[]{PAID});
        long base = capacity > 0 ? capacity : issued;

        List<Map<String, Object>> kpis = new ArrayList<>();
        kpis.add(map("key", "revenue", "label", "Doanh thu", "icon", "💰", "color", "lime",
                "value", Web.vndShort(BigDecimal.valueOf(revCur)), "full", Web.vnd(BigDecimal.valueOf(revCur)),
                "delta", delta(revCur, revPrev), "sub", paidCur + " đơn đã thanh toán"));
        kpis.add(map("key", "tickets", "label", "Vé đã đặt", "icon", "🎟️", "color", "yellow",
                "value", String.format("%,d", tixCur).replace(',', '.'), "full", "", "delta", delta(tixCur, tixPrev),
                "sub", "không tính đơn đã hủy"));
        kpis.add(map("key", "orders", "label", "Đơn hàng", "icon", "🧾", "color", "pink",
                "value", String.valueOf(ordCur), "full", "", "delta", delta(ordCur, ordPrev),
                "sub", pendCur + " chờ thanh toán · " + cancelCur + " đã hủy"));
        kpis.add(map("key", "aov", "label", "Giá trị đơn TB", "icon", "📈", "color", "blue",
                "value", Web.vndShort(BigDecimal.valueOf(aovCur)), "full", Web.vnd(BigDecimal.valueOf((long) aovCur)),
                "delta", delta(aovCur, aovPrev), "sub", "trên đơn đã thanh toán"));
        double fill = Web.pct(sold, base);
        kpis.add(map("key", "fill", "label", "Tỷ lệ lấp đầy", "icon", "🏟️", "color", "lime",
                "value", fill + "%", "full", "", "delta", null, "sub", sold + " / " + base + " vé", "progress", fill));
        double ci = Web.pct(checkedIn, validTickets);
        kpis.add(map("key", "checkin", "label", "Đã vào cổng", "icon", "✅", "color", "yellow",
                "value", String.valueOf(checkedIn), "full", "", "delta", null,
                "sub", ci + "% trên " + validTickets + " vé hợp lệ", "progress", ci));

        // ---- 3) Top sự kiện theo doanh thu trong kỳ ----
        List<Map<String, Object>> top = new ArrayList<>();
        if (eventId == 0) {
            top = Jdbc.list(
                "SELECT e.event_id, e.category_id, e.title, e.image_url, " +
                " COALESCE(SUM(CASE WHEN o.payment_status = ? THEN o.total_amount ELSE 0 END), 0) AS rev, " +
                " COALESCE(SUM(CASE WHEN o.payment_status <> ? THEN o.total_quantity ELSE 0 END), 0) AS tix " +
                "FROM Events e LEFT JOIN Orders o ON o.event_id = e.event_id AND o.created_at >= ? AND o.created_at < ? " +
                "GROUP BY e.event_id, e.category_id, e.title, e.image_url ORDER BY rev DESC OFFSET 0 ROWS FETCH NEXT 5 ROWS ONLY",
                rs -> {
                    Event ev = new Event();
                    ev.setEventId(rs.getInt(1)); ev.setCategoryId(rs.getInt(2)); ev.setImageUrl(rs.getString(4));
                    BigDecimal rev = rs.getBigDecimal(5);
                    return map("id", ev.getEventId(), "title", rs.getString(3), "image", ev.getImagePath(),
                            "revenue", rev.longValue(), "revenueText", Web.vnd(rev), "tickets", rs.getInt(6));
                }, PAID, CANCELLED, tsStart, tsEnd);
        }

        // ---- 4) Cơ cấu thanh toán ----
        String[] pmNames = {"Thẻ ATM", "QR chuyển khoản", "Ví điện tử", "Visa / Mastercard"};
        String[] pmKeys = {"bank", "qr", "wallet", "intl"};
        List<Map<String, Object>> payments = new ArrayList<>();
        for (int i = 0; i < 4; i++) payments.add(map("key", pmKeys[i], "name", pmNames[i], "value", pay.getOrDefault(pmKeys[i], 0)));

        // ---- 5) Đơn gần đây ----
        OrderFilter f = new OrderFilter();
        f.eventId = eventId;
        List<Order> recent = new OrderDAO().search(f, 1, 8);

        // ---- 6) Cảnh báo / việc cần làm ----
        List<Map<String, Object>> alerts = new ArrayList<>();
        Timestamp dayAgo = Timestamp.valueOf(LocalDateTime.now().minusHours(24));
        long stale = Jdbc.count("SELECT COUNT(*) FROM Orders WHERE payment_status = ? AND created_at < ?" + (eventId > 0 ? " AND event_id = ?" : ""),
                eventId > 0 ? new Object[]{PENDING, dayAgo, eventId} : new Object[]{PENDING, dayAgo});
        if (stale > 0) alerts.add(map("level", "warn", "icon", "⏳", "text", stale + " đơn chờ thanh toán quá 24 giờ", "href", "/admin/orders?status=" + java.net.URLEncoder.encode(PENDING, java.nio.charset.StandardCharsets.UTF_8), "cta", "Xử lý"));
        List<Map<String, Object>> low = Jdbc.list(
            "SELECT tt.type_name, e.title, tt.sold_quantity, tt.total_quantity, tt.event_id FROM TicketTypes tt JOIN Events e ON e.event_id = tt.event_id " +
            "WHERE tt.total_quantity > 0 AND tt.sold_quantity * 100 >= tt.total_quantity * 85 AND e.status <> N'Đã kết thúc' " + (eventId > 0 ? "AND tt.event_id = ? " : "") +
            "ORDER BY tt.sold_quantity * 1.0 / tt.total_quantity DESC OFFSET 0 ROWS FETCH NEXT 3 ROWS ONLY",
            rs -> map("level", rs.getInt(3) >= rs.getInt(4) ? "danger" : "warn", "icon", rs.getInt(3) >= rs.getInt(4) ? "🚫" : "🔥",
                    "text", "Vé \"" + rs.getString(1) + "\" – " + rs.getString(2) + (rs.getInt(3) >= rs.getInt(4) ? " đã HẾT" : " sắp hết (" + rs.getInt(3) + "/" + rs.getInt(4) + ")"),
                    "href", "/admin/ticket-types?eventId=" + rs.getInt(5), "cta", "Thêm vé"),
            eventId > 0 ? new Object[]{eventId} : new Object[0]);
        alerts.addAll(low);
        List<Map<String, Object>> soon = Jdbc.list(
            "SELECT event_id, title, event_date FROM Events WHERE event_date >= ? AND event_date < ? AND status <> N'Tạm ẩn' AND status <> N'Đã kết thúc' " +
            (eventId > 0 ? "AND event_id = ? " : "") + "ORDER BY event_date ASC OFFSET 0 ROWS FETCH NEXT 3 ROWS ONLY",
            rs -> map("level", "info", "icon", "📅", "text", "\"" + rs.getString(2) + "\" diễn ra lúc " + Web.dateTime(rs.getTimestamp(3)),
                    "href", "/admin/checkin?eventId=" + rs.getInt(1), "cta", "Soát vé"),
            eventId > 0 ? new Object[]{Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now().plusDays(14)), eventId}
                        : new Object[]{Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now().plusDays(14))});
        alerts.addAll(soon);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days);
        out.put("rangeText", Web.date(start) + " – " + Web.date(today));
        out.put("kpis", kpis);
        out.put("labels", labels);
        out.put("revenueSeries", sRev);
        out.put("ticketSeries", sTix);
        out.put("orderSeries", sOrd);
        out.put("payments", payments);
        out.put("topEvents", top);
        out.put("recent", recent);
        out.put("alerts", alerts);
        out.put("pending", pendCur);
        return out;
    }

    /** Số đơn đang chờ thanh toán (hiện badge trên sidebar). */
    public long pendingOrders() {
        try { return Jdbc.count("SELECT COUNT(*) FROM Orders WHERE payment_status = ?", PENDING); }
        catch (Exception e) { return 0; }
    }
}
