package com.tickethub.dao;

import com.tickethub.util.Jdbc;
import com.tickethub.util.Web;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thống kê khách hàng (tài khoản vai trò CUSTOMER) kèm số đơn, số vé, tổng chi tiêu. */
public class CustomerDAO {

    private static final String SELECT =
        "SELECT u.user_id, u.full_name, u.email, u.phone, u.created_at, " +
        " COUNT(o.order_id) AS orders, " +
        " COALESCE(SUM(CASE WHEN o.payment_status <> ? THEN o.total_quantity ELSE 0 END), 0) AS tickets, " +
        " COALESCE(SUM(CASE WHEN o.payment_status = ? THEN o.total_amount ELSE 0 END), 0) AS spent, " +
        " MAX(o.created_at) AS last_order " +
        "FROM Users u LEFT JOIN Orders o ON o.user_id = u.user_id " +
        "WHERE u.role = 'CUSTOMER' ";

    private static final String GROUP = "GROUP BY u.user_id, u.full_name, u.email, u.phone, u.created_at ";

    private Map<String, Object> map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getInt("user_id"));
        m.put("name", rs.getString("full_name"));
        m.put("email", rs.getString("email"));
        m.put("phone", rs.getString("phone"));
        m.put("joined", Web.date(rs.getTimestamp("created_at")));
        m.put("orders", rs.getInt("orders"));
        m.put("tickets", rs.getInt("tickets"));
        BigDecimal spent = rs.getBigDecimal("spent");
        m.put("spent", spent);
        m.put("spentText", Web.vnd(spent));
        Timestamp lo = rs.getTimestamp("last_order");
        m.put("lastOrder", lo == null ? "—" : Web.date(lo));
        String n = rs.getString("full_name");
        m.put("initial", n == null || n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase());
        return m;
    }

    private static String order(String sort) {
        switch (sort == null ? "" : sort) {
            case "orders": return "ORDER BY orders DESC, u.user_id ";
            case "new":    return "ORDER BY u.created_at DESC, u.user_id ";
            case "name":   return "ORDER BY u.full_name ASC, u.user_id ";
            default:       return "ORDER BY spent DESC, u.user_id ";
        }
    }

    public List<Map<String, Object>> list(String keyword, String sort, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> p = new ArrayList<>();
        p.add(OrderDAO.CANCELLED); p.add(OrderDAO.PAID);
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR u.phone LIKE ?) ");
            String t = "%" + keyword.toLowerCase() + "%";
            p.add(t); p.add(t); p.add("%" + keyword + "%");
        }
        sql.append(GROUP).append(order(sort)).append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        p.add(Math.max(0, (page - 1) * size)); p.add(size);
        return Jdbc.list(sql.toString(), this::map, p.toArray());
    }

    public int count(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Users u WHERE u.role = 'CUSTOMER' ");
        List<Object> p = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR u.phone LIKE ?)");
            String t = "%" + keyword.toLowerCase() + "%";
            p.add(t); p.add(t); p.add("%" + keyword + "%");
        }
        return (int) Jdbc.count(sql.toString(), p.toArray());
    }

    /** Tổng khách, khách mới 30 ngày, khách quay lại (>=2 đơn), khách đã mua. */
    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        long total = Jdbc.count("SELECT COUNT(*) FROM Users WHERE role = 'CUSTOMER'");
        long fresh = Jdbc.count("SELECT COUNT(*) FROM Users WHERE role = 'CUSTOMER' AND created_at >= ?",
                Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(30)));
        long buyers = Jdbc.count("SELECT COUNT(DISTINCT user_id) FROM Orders WHERE user_id IS NOT NULL AND payment_status <> ?", OrderDAO.CANCELLED);
        long repeat = Jdbc.count("SELECT COUNT(*) FROM (SELECT user_id FROM Orders WHERE user_id IS NOT NULL AND payment_status <> ? GROUP BY user_id HAVING COUNT(*) >= 2) t", OrderDAO.CANCELLED);
        m.put("total", total);
        m.put("fresh", fresh);
        m.put("buyers", buyers);
        m.put("repeat", repeat);
        m.put("repeatRate", Web.pct(repeat, buyers));
        return m;
    }

    public Map<String, Object> get(int userId) {
        return Jdbc.one(SELECT + "AND u.user_id = ? " + GROUP, this::map, OrderDAO.CANCELLED, OrderDAO.PAID, userId);
    }
}
