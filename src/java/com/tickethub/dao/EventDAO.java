package com.tickethub.dao;

import com.tickethub.context.DBContext;
import com.tickethub.model.Category;
import com.tickethub.model.Event;
import com.tickethub.util.Jdbc;
import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public List<Event> getAllEvents() {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT e.*, c.category_name, c.color_hex AS cat_color " +
                     "FROM Events e " +
                     "JOIN Categories c ON e.category_id = c.category_id " +
                     "WHERE e.status <> N'Tạm ẩn' " +
                     "ORDER BY e.event_date ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToEvent(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return list;
    }

    public Event getFeaturedEvent() {
        String sql = "SELECT TOP 1 e.*, c.category_name, c.color_hex AS cat_color " +
                     "FROM Events e " +
                     "JOIN Categories c ON e.category_id = c.category_id " +
                     "WHERE e.is_featured = 1 AND e.status <> N'Tạm ẩn'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEvent(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return null;
    }

    public Event getEventById(int eventId) {
        String sql = "SELECT e.*, c.category_name, c.color_hex AS cat_color " +
                     "FROM Events e " +
                     "JOIN Categories c ON e.category_id = c.category_id " +
                     "WHERE e.event_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, eventId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEvent(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return null;
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM Categories ORDER BY category_id ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Category(
                    rs.getInt("category_id"),
                    rs.getString("category_name"),
                    rs.getString("color_hex")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return list;
    }

    public List<Event> searchEvents(String keyword, String categoryName) {
        List<Event> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT e.*, c.category_name, c.color_hex AS cat_color " +
            "FROM Events e " +
            "JOIN Categories c ON e.category_id = c.category_id " +
            "WHERE e.status <> N'Tạm ẩn' "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(e.title) LIKE ? OR LOWER(e.location) LIKE ?) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }

        if (categoryName != null && !categoryName.trim().isEmpty() && !categoryName.equalsIgnoreCase("Tất cả")) {
            sql.append("AND c.category_name = ? ");
            params.add(categoryName.trim());
        }

        sql.append("ORDER BY e.event_date ASC");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToEvent(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return list;
    }

    private Event mapResultSetToEvent(ResultSet rs) throws Exception {
        Event e = new Event();
        e.setEventId(rs.getInt("event_id"));
        e.setCategoryId(rs.getInt("category_id"));
        e.setCategoryName(rs.getString("category_name"));
        e.setCategoryColor(rs.getString("cat_color"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setLocation(rs.getString("location"));
        e.setEventDate(rs.getTimestamp("event_date"));
        e.setMinPrice(rs.getBigDecimal("min_price"));
        e.setBannerColor(rs.getString("banner_color"));
        e.setTagLabel(rs.getString("tag_label"));
        e.setFeatured(rs.getBoolean("is_featured"));
        e.setTotalCapacity(rs.getInt("total_capacity"));
        e.setStatus(rs.getString("status"));
        try {
            e.setImageUrl(rs.getString("image_url"));
        } catch (Exception ignored) {
            // DB cũ chưa có cột image_url -> Event.getImagePath() sẽ dùng ảnh mặc định
        }
        return e;
    }

    // =====================================================================
    //  QUẢN TRỊ SỰ KIỆN
    // =====================================================================

    private static final String ADMIN_SELECT =
        "SELECT e.*, c.category_name, c.color_hex AS cat_color, " +
        " COALESCE((SELECT SUM(tt.sold_quantity) FROM TicketTypes tt WHERE tt.event_id = e.event_id), 0) AS sold_tickets, " +
        " COALESCE((SELECT SUM(tt.total_quantity) FROM TicketTypes tt WHERE tt.event_id = e.event_id), 0) AS issued_tickets, " +
        " COALESCE((SELECT SUM(o.total_amount) FROM Orders o WHERE o.event_id = e.event_id AND o.payment_status = N'Đã thanh toán'), 0) AS revenue, " +
        " (SELECT COUNT(*) FROM Orders o WHERE o.event_id = e.event_id) AS order_count, " +
        " COALESCE((SELECT SUM(o.checked_in_qty) FROM Orders o WHERE o.event_id = e.event_id), 0) AS checked_in " +
        "FROM Events e JOIN Categories c ON e.category_id = c.category_id ";

    private Event mapAdmin(java.sql.ResultSet rs) throws java.sql.SQLException {
        try {
            Event e = mapResultSetToEvent(rs);
            e.setSoldTickets(rs.getInt("sold_tickets"));
            e.setIssuedTickets(rs.getInt("issued_tickets"));
            e.setRevenue(rs.getBigDecimal("revenue"));
            e.setOrderCount(rs.getInt("order_count"));
            e.setCheckedIn(rs.getInt("checked_in"));
            return e;
        } catch (java.sql.SQLException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.sql.SQLException(ex);
        }
    }

    /** Danh sách sự kiện cho trang quản trị (kèm vé bán, doanh thu). */
    public List<Event> adminList(String keyword, int categoryId, String status) {
        StringBuilder sql = new StringBuilder(ADMIN_SELECT).append("WHERE 1=1 ");
        List<Object> p = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (LOWER(e.title) LIKE ? OR LOWER(e.location) LIKE ?) ");
            String t = "%" + keyword.toLowerCase() + "%";
            p.add(t); p.add(t);
        }
        if (categoryId > 0) { sql.append("AND e.category_id = ? "); p.add(categoryId); }
        if (status != null && !status.isEmpty()) { sql.append("AND e.status = ? "); p.add(status); }
        sql.append("ORDER BY e.event_date DESC");
        return Jdbc.list(sql.toString(), this::mapAdmin, p.toArray());
    }

    public Event adminGet(int id) {
        return Jdbc.one(ADMIN_SELECT + "WHERE e.event_id = ?", this::mapAdmin, id);
    }

    /** Tạo sự kiện kèm 1 loại vé "Thường" mặc định (giá thấp nhất, số lượng = sức chứa). */
    public int create(Event e) {
        return Jdbc.tx(c -> {
            int id = Jdbc.insert(c,
                "INSERT INTO Events (category_id, title, description, location, event_date, min_price, banner_color, " +
                "tag_label, is_featured, total_capacity, status, image_url) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                e.getCategoryId(), e.getTitle(), e.getDescription(), e.getLocation(), e.getEventDate(),
                e.getMinPrice(), e.getBannerColor(), e.getTagLabel(), e.isFeatured() ? 1 : 0,
                e.getTotalCapacity(), e.getStatus(), e.getImageUrl());
            Jdbc.exec(c, "INSERT INTO TicketTypes (event_id, type_name, description, price, total_quantity, sold_quantity, bar_color) " +
                "VALUES (?, N'Thường', N'Vé tiêu chuẩn', ?, ?, 0, '#3a68ff')", id, e.getMinPrice(), e.getTotalCapacity());
            if (e.isFeatured()) Jdbc.exec(c, "UPDATE Events SET is_featured = 0 WHERE event_id <> ?", id);
            return id;
        });
    }

    public void update(Event e) {
        Jdbc.tx(c -> {
            Jdbc.exec(c,
                "UPDATE Events SET category_id=?, title=?, description=?, location=?, event_date=?, banner_color=?, " +
                "tag_label=?, total_capacity=?, status=?, image_url=?, is_featured=? WHERE event_id=?",
                e.getCategoryId(), e.getTitle(), e.getDescription(), e.getLocation(), e.getEventDate(),
                e.getBannerColor(), e.getTagLabel(), e.getTotalCapacity(), e.getStatus(), e.getImageUrl(),
                e.isFeatured() ? 1 : 0, e.getEventId());
            if (e.isFeatured()) Jdbc.exec(c, "UPDATE Events SET is_featured = 0 WHERE event_id <> ?", e.getEventId());
            return null;
        });
    }

    public long countOrders(int eventId) {
        return Jdbc.count("SELECT COUNT(*) FROM Orders WHERE event_id = ?", eventId);
    }

    /** Chỉ xóa được khi chưa có đơn hàng nào; nếu đã có đơn hãy chuyển sang "Tạm ẩn". */
    public void delete(int eventId) {
        Jdbc.tx(c -> {
            Long n = Jdbc.one(c, "SELECT COUNT(*) FROM Orders WHERE event_id = ?", rs -> rs.getLong(1), eventId);
            if (n != null && n > 0) throw new Jdbc.DataAccessException("Sự kiện đã có " + n + " đơn hàng nên không thể xóa. Hãy chuyển sang trạng thái \"Tạm ẩn\".");
            Jdbc.exec(c, "DELETE FROM TicketTypes WHERE event_id = ?", eventId);
            Jdbc.exec(c, "DELETE FROM Events WHERE event_id = ?", eventId);
            return null;
        });
    }

    public void setFeatured(int eventId, boolean featured) {
        Jdbc.tx(c -> {
            if (featured) Jdbc.exec(c, "UPDATE Events SET is_featured = 0");
            Jdbc.exec(c, "UPDATE Events SET is_featured = ? WHERE event_id = ?", featured ? 1 : 0, eventId);
            return null;
        });
    }

    public void setStatus(int eventId, String status) {
        Jdbc.exec("UPDATE Events SET status = ? WHERE event_id = ?", status, eventId);
    }

    /** Cập nhật "Từ ...đ" của sự kiện theo loại vé rẻ nhất. */
    public static void refreshMinPrice(java.sql.Connection c, int eventId) throws java.sql.SQLException {
        Jdbc.exec(c, "UPDATE Events SET min_price = COALESCE((SELECT MIN(price) FROM TicketTypes WHERE event_id = ?), min_price) WHERE event_id = ?",
                eventId, eventId);
    }
}
