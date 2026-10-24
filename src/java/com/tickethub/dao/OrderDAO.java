package com.tickethub.dao;

import com.tickethub.context.DBContext;
import com.tickethub.util.Jdbc;
import java.sql.Timestamp;
import com.tickethub.model.Order;
import com.tickethub.model.OrderDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderDAO {

    public List<Order> getRecentOrders(int limit) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT TOP (?) o.*, e.title AS event_title " +
                     "FROM Orders o " +
                     "LEFT JOIN Events e ON o.event_id = e.event_id " +
                     "ORDER BY o.order_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToOrder(rs));
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

    /** Chỉ lấy các đơn thuộc về 1 tài khoản (dùng cho trang "Vé của tôi"). */
    public List<Order> getOrdersByUser(int userId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, e.title AS event_title " +
                     "FROM Orders o " +
                     "LEFT JOIN Events e ON o.event_id = e.event_id " +
                     "WHERE o.user_id = ? " +
                     "ORDER BY o.order_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToOrder(rs));
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

    public List<Order> getFilteredOrders(String keyword, String status, String ticketType, int page, int pageSize) {
        List<Order> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT o.*, e.title AS event_title " +
            "FROM Orders o " +
            "LEFT JOIN Events e ON o.event_id = e.event_id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(o.order_code) LIKE ? OR LOWER(o.customer_name) LIKE ? OR LOWER(o.customer_email) LIKE ?) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("Tất cả")) {
            sql.append("AND o.payment_status = ? ");
            params.add(status.trim());
        }

        if (ticketType != null && !ticketType.trim().isEmpty() && !ticketType.equalsIgnoreCase("Tất cả")) {
            sql.append("AND o.ticket_type_summary = ? ");
            params.add(ticketType.trim());
        }

        sql.append("ORDER BY o.order_id DESC ");
        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add((page - 1) * pageSize);
        params.add(pageSize);

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
                list.add(mapResultSetToOrder(rs));
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

    public int countFilteredOrders(String keyword, String status, String ticketType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Orders o WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(o.order_code) LIKE ? OR LOWER(o.customer_name) LIKE ? OR LOWER(o.customer_email) LIKE ?) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("Tất cả")) {
            sql.append("AND o.payment_status = ? ");
            params.add(status.trim());
        }

        if (ticketType != null && !ticketType.trim().isEmpty() && !ticketType.equalsIgnoreCase("Tất cả")) {
            sql.append("AND o.ticket_type_summary = ? ");
            params.add(ticketType.trim());
        }

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
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return 0;
    }

    public Map<String, Integer> getStatusCounts() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT payment_status, COUNT(*) AS cnt FROM Orders GROUP BY payment_status";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            int total = 0;
            while (rs.next()) {
                String st = rs.getString("payment_status");
                int cnt = rs.getInt("cnt");
                map.put(st, cnt);
                total += cnt;
            }
            map.put("Tất cả", total);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return map;
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT o.*, e.title AS event_title FROM Orders o LEFT JOIN Events e ON o.event_id = e.event_id WHERE o.order_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setDetails(getOrderDetails(conn, orderId));
                return order;
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

    private List<OrderDetail> getOrderDetails(Connection conn, int orderId) throws Exception {
        List<OrderDetail> details = new ArrayList<>();
        String sql = "SELECT od.*, tt.type_name FROM OrderDetails od LEFT JOIN TicketTypes tt ON od.ticket_type_id = tt.ticket_type_id WHERE od.order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail d = new OrderDetail();
                    d.setOrderDetailId(rs.getInt("order_detail_id"));
                    d.setOrderId(rs.getInt("order_id"));
                    d.setTicketTypeId(rs.getInt("ticket_type_id"));
                    d.setTicketTypeName(rs.getString("type_name"));
                    d.setQuantity(rs.getInt("quantity"));
                    d.setUnitPrice(rs.getBigDecimal("unit_price"));
                    d.setAmount(rs.getBigDecimal("amount"));
                    details.add(d);
                }
            }
        }
        return details;
    }

    private Order mapResultSetToOrder(ResultSet rs) throws Exception {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setOrderCode(rs.getString("order_code"));
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) o.setUserId(uid);
        o.setEventId(rs.getInt("event_id"));
        try { o.setEventTitle(rs.getString("event_title")); } catch (Exception ignored) {}
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerEmail(rs.getString("customer_email"));
        o.setCustomerPhone(rs.getString("customer_phone"));
        o.setTicketTypeSummary(rs.getString("ticket_type_summary"));
        o.setTotalQuantity(rs.getInt("total_quantity"));
        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setServiceFee(rs.getBigDecimal("service_fee"));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setOrderTimeDesc(rs.getString("order_time_desc"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            o.setCheckedInQty(rs.getInt("checked_in_qty"));
            o.setLastCheckinAt(rs.getTimestamp("last_checkin_at"));
        } catch (Exception ignored) { /* DB cũ chưa có cột soát vé */ }
        try { o.setEventImage(rs.getString("event_image")); } catch (Exception ignored) {}
        return o;
    }

    // =====================================================================
    //  TẠO ĐƠN (mua vé) - có kiểm tra tồn kho, mã đơn không bao giờ trùng
    // =====================================================================

    /**
     * Lưu đơn + chi tiết và trừ tồn kho trong 1 transaction.
     * Ném DataAccessException (thông báo tiếng Việt) nếu hết vé hoặc lỗi DB.
     * Sau khi thành công, order.orderCode / orderId đã được gán.
     */
    public void createOrder(Order order, List<OrderDetail> details) {
        Jdbc.tx(c -> {
            // 1) trừ tồn kho có điều kiện: chỉ trừ khi còn đủ vé
            for (OrderDetail d : details) {
                int n = Jdbc.exec(c,
                    "UPDATE TicketTypes SET sold_quantity = sold_quantity + ? WHERE ticket_type_id = ? AND sold_quantity + ? <= total_quantity",
                    d.getQuantity(), d.getTicketTypeId(), d.getQuantity());
                if (n == 0) {
                    throw new Jdbc.DataAccessException("Loại vé \"" + d.getTicketTypeName() + "\" không còn đủ " + d.getQuantity() + " vé. Vui lòng giảm số lượng!");
                }
            }
            // 2) tạo đơn với mã tạm rồi gán mã chính thức theo order_id (không bao giờ trùng)
            String tmp = "TMP-" + System.nanoTime();
            int id = Jdbc.insert(c,
                "INSERT INTO Orders (order_code, user_id, event_id, customer_name, customer_email, customer_phone, ticket_type_summary, " +
                "total_quantity, subtotal, service_fee, discount_amount, total_amount, payment_method, payment_status, order_time_desc, created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tmp, order.getUserId(), order.getEventId(), order.getCustomerName(), order.getCustomerEmail(),
                order.getCustomerPhone(), order.getTicketTypeSummary(), order.getTotalQuantity(), order.getSubtotal(),
                order.getServiceFee(), order.getDiscountAmount(), order.getTotalAmount(), order.getPaymentMethod(),
                order.getPaymentStatus(), order.getOrderTimeDesc(), new Timestamp(System.currentTimeMillis()));
            String code = String.format("#TK-%04d", 1000 + id);
            if (Jdbc.count("SELECT COUNT(*) FROM Orders WHERE order_code = ?", code) > 0) code = code + "-" + id;
            Jdbc.exec(c, "UPDATE Orders SET order_code = ? WHERE order_id = ?", code, id);
            for (OrderDetail d : details) {
                Jdbc.exec(c, "INSERT INTO OrderDetails (order_id, ticket_type_id, quantity, unit_price, amount) VALUES (?,?,?,?,?)",
                    id, d.getTicketTypeId(), d.getQuantity(), d.getUnitPrice(), d.getAmount());
            }
            order.setOrderId(id);
            order.setOrderCode(code);
            return null;
        });
    }

    // =====================================================================
    //  THANH TOÁN QUA CỔNG VNPAY (SANDBOX)
    // =====================================================================

    /** Dấu hiệu "đơn đang chờ khách thanh toán trên VNPay" (lưu ở cột payment_ref). */
    public static final String GATEWAY_WAITING = "WAITING-VNPAY";

    /** Đánh dấu đơn vừa tạo đang chờ thanh toán trên cổng VNPay. */
    public void markAwaitingGateway(int orderId) {
        Jdbc.exec("UPDATE Orders SET payment_ref = ? WHERE order_id = ?", GATEWAY_WAITING, orderId);
    }

    /**
     * Hủy các đơn VNPay chờ thanh toán quá hạn và trả vé về kho
     * (chỉ đơn do cổng thanh toán tạo ra, không đụng đến đơn "Chờ thanh toán" nhập tay).
     */
    public int expirePendingGateway(int minutes) {
        Timestamp limit = new Timestamp(System.currentTimeMillis() - minutes * 60_000L);
        List<Integer> ids = Jdbc.list(
            "SELECT order_id FROM Orders WHERE payment_status = ? AND payment_ref = ? AND created_at < ?",
            rs -> rs.getInt(1), PENDING, GATEWAY_WAITING, limit);
        for (Integer id : ids) {
            updateStatus(id, CANCELLED);
            Jdbc.exec("UPDATE Orders SET payment_ref = NULL WHERE order_id = ?", id);
        }
        return ids.size();
    }

    /**
     * Ghi nhận kết quả từ VNPay. Chạy lại nhiều lần vẫn an toàn (chỉ xử lý đơn đang chờ).
     *  - success: chuyển "Đã thanh toán", lưu mã giao dịch VNPay.
     *  - thất bại: hủy đơn và trả vé về kho.
     * @return true nếu lần gọi này đã thay đổi trạng thái đơn
     */
    public boolean completeGatewayPayment(int orderId, boolean success, String transactionNo) {
        Order o = getOrderById(orderId);
        if (o == null || !PENDING.equals(o.getPaymentStatus())) return false;
        if (success) {
            updateStatus(orderId, PAID);
            Jdbc.exec("UPDATE Orders SET payment_ref = ? WHERE order_id = ?",
                      transactionNo == null ? "VNPAY" : transactionNo, orderId);
        } else {
            updateStatus(orderId, CANCELLED);
            Jdbc.exec("UPDATE Orders SET payment_ref = NULL WHERE order_id = ?", orderId);
        }
        return true;
    }

    // =====================================================================
    //  QUẢN TRỊ ĐƠN HÀNG
    // =====================================================================

    private static final String ORDER_SELECT =
        "SELECT o.*, e.title AS event_title, e.image_url AS event_image FROM Orders o LEFT JOIN Events e ON o.event_id = e.event_id ";

    private Order mapSafe(java.sql.ResultSet rs) throws java.sql.SQLException {
        try { return mapResultSetToOrder(rs); }
        catch (java.sql.SQLException e) { throw e; }
        catch (Exception e) { throw new java.sql.SQLException(e); }
    }

    public List<Order> search(OrderFilter f, int page, int size) {
        List<Object> p = new ArrayList<>();
        String sql = ORDER_SELECT + f.where(p, true) + "ORDER BY o.order_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        p.add(Math.max(0, (page - 1) * size));
        p.add(size);
        return Jdbc.list(sql, this::mapSafe, p.toArray());
    }

    public int count(OrderFilter f) {
        List<Object> p = new ArrayList<>();
        return (int) Jdbc.count("SELECT COUNT(*) FROM Orders o " + f.where(p, true), p.toArray());
    }

    /** Doanh thu (đơn đã thanh toán) trong phạm vi bộ lọc, bỏ qua điều kiện trạng thái. */
    public java.math.BigDecimal paidRevenue(OrderFilter f) {
        List<Object> p = new ArrayList<>();
        String sql = "SELECT COALESCE(SUM(o.total_amount), 0) FROM Orders o " + f.where(p, false) + "AND o.payment_status = ?";
        p.add("Đã thanh toán");
        java.math.BigDecimal v = Jdbc.one(sql, rs -> rs.getBigDecimal(1), p.toArray());
        return v == null ? java.math.BigDecimal.ZERO : v;
    }

    /** Số đơn theo từng trạng thái (bỏ qua điều kiện trạng thái) + khóa "Tất cả". */
    public Map<String, Integer> statusCounts(OrderFilter f) {
        List<Object> p = new ArrayList<>();
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Tất cả", 0); m.put("Đã thanh toán", 0); m.put("Chờ thanh toán", 0); m.put("Đã hủy", 0);
        String sql = "SELECT o.payment_status, COUNT(*) FROM Orders o " + f.where(p, false) + "GROUP BY o.payment_status";
        int total = 0;
        for (Object[] r : Jdbc.list(sql, rs -> new Object[]{rs.getString(1), rs.getInt(2)}, p.toArray())) {
            m.put((String) r[0], (Integer) r[1]);
            total += (Integer) r[1];
        }
        m.put("Tất cả", total);
        return m;
    }

    /** Đơn kèm chi tiết các hạng vé. */
    public Order detail(int orderId) {
        return Jdbc.tx(c -> {
            Order o = Jdbc.one(c, ORDER_SELECT + "WHERE o.order_id = ?", this::mapSafe, orderId);
            if (o != null) o.setDetails(detailsOf(c, orderId));
            return o;
        });
    }

    public Order byCode(String code) {
        return Jdbc.tx(c -> {
            Order o = Jdbc.one(c, ORDER_SELECT + "WHERE UPPER(o.order_code) = ?", this::mapSafe, code.trim().toUpperCase());
            if (o != null) o.setDetails(detailsOf(c, o.getOrderId()));
            return o;
        });
    }

    private List<OrderDetail> detailsOf(java.sql.Connection c, int orderId) throws java.sql.SQLException {
        return Jdbc.list(c, "SELECT od.*, tt.type_name FROM OrderDetails od LEFT JOIN TicketTypes tt ON od.ticket_type_id = tt.ticket_type_id WHERE od.order_id = ? ORDER BY od.order_detail_id",
            rs -> {
                OrderDetail d = new OrderDetail();
                d.setOrderDetailId(rs.getInt("order_detail_id"));
                d.setOrderId(rs.getInt("order_id"));
                d.setTicketTypeId(rs.getInt("ticket_type_id"));
                d.setTicketTypeName(rs.getString("type_name"));
                d.setQuantity(rs.getInt("quantity"));
                d.setUnitPrice(rs.getBigDecimal("unit_price"));
                d.setAmount(rs.getBigDecimal("amount"));
                return d;
            }, orderId);
    }

    public static final String PAID = "Đã thanh toán", PENDING = "Chờ thanh toán", CANCELLED = "Đã hủy";

    /**
     * Đổi trạng thái đơn và đồng bộ tồn kho:
     *  - Hủy đơn  -> trả vé về kho; khôi phục đơn đã hủy -> trừ lại (kiểm tra còn đủ vé).
     *  - Không cho hủy đơn đã có vé được soát vào cổng.
     */
    public void updateStatus(int orderId, String newStatus) {
        if (!PAID.equals(newStatus) && !PENDING.equals(newStatus) && !CANCELLED.equals(newStatus)) {
            throw new Jdbc.DataAccessException("Trạng thái không hợp lệ.");
        }
        Jdbc.tx(c -> {
            Order o = Jdbc.one(c, "SELECT * FROM Orders WHERE order_id = ?", this::mapSafe, orderId);
            if (o == null) throw new Jdbc.DataAccessException("Không tìm thấy đơn hàng.");
            String old = o.getPaymentStatus();
            if (old.equals(newStatus)) return null;
            if (CANCELLED.equals(newStatus) && o.getCheckedInQty() > 0) {
                throw new Jdbc.DataAccessException("Đơn này đã có " + o.getCheckedInQty() + " vé được soát vào cổng nên không thể hủy.");
            }
            List<OrderDetail> ds = detailsOf(c, orderId);
            if (CANCELLED.equals(newStatus)) {
                for (OrderDetail d : ds) {
                    Jdbc.exec(c, "UPDATE TicketTypes SET sold_quantity = CASE WHEN sold_quantity >= ? THEN sold_quantity - ? ELSE 0 END WHERE ticket_type_id = ?",
                        d.getQuantity(), d.getQuantity(), d.getTicketTypeId());
                }
            } else if (CANCELLED.equals(old)) {
                for (OrderDetail d : ds) {
                    int n = Jdbc.exec(c, "UPDATE TicketTypes SET sold_quantity = sold_quantity + ? WHERE ticket_type_id = ? AND sold_quantity + ? <= total_quantity",
                        d.getQuantity(), d.getTicketTypeId(), d.getQuantity());
                    if (n == 0) throw new Jdbc.DataAccessException("Không thể khôi phục đơn: loại vé \"" + d.getTicketTypeName() + "\" đã hết chỗ.");
                }
            }
            Jdbc.exec(c, "UPDATE Orders SET payment_status = ? WHERE order_id = ?", newStatus, orderId);
            return null;
        });
    }

    /** Ghi nhận cho qty vé vào cổng (chỉ đơn đã thanh toán, không vượt quá số vé trong đơn). */
    public void checkIn(int orderId, int qty) {
        Jdbc.tx(c -> {
            Order o = Jdbc.one(c, "SELECT * FROM Orders WHERE order_id = ?", this::mapSafe, orderId);
            if (o == null) throw new Jdbc.DataAccessException("Không tìm thấy đơn hàng.");
            if (!o.isPaid()) throw new Jdbc.DataAccessException(o.isCancelled() ? "Đơn đã bị hủy." : "Đơn chưa thanh toán.");
            if (qty < 1) throw new Jdbc.DataAccessException("Số vé soát phải từ 1 trở lên.");
            if (o.getCheckedInQty() + qty > o.getTotalQuantity()) {
                throw new Jdbc.DataAccessException("Chỉ còn " + o.getRemainingToCheckin() + " vé chưa vào cổng trong đơn này.");
            }
            Jdbc.exec(c, "UPDATE Orders SET checked_in_qty = checked_in_qty + ?, last_checkin_at = ? WHERE order_id = ?",
                qty, new Timestamp(System.currentTimeMillis()), orderId);
            return null;
        });
    }

    /** Hoàn tác lần soát gần nhất của đơn (đưa về 0) - dùng khi soát nhầm. */
    public void resetCheckIn(int orderId) {
        Jdbc.exec("UPDATE Orders SET checked_in_qty = 0, last_checkin_at = NULL WHERE order_id = ?", orderId);
    }

    public List<Order> recentCheckins(int eventId, int limit) {
        List<Object> p = new ArrayList<>();
        String sql = ORDER_SELECT + "WHERE o.last_checkin_at IS NOT NULL ";
        if (eventId > 0) { sql += "AND o.event_id = ? "; p.add(eventId); }
        sql += "ORDER BY o.last_checkin_at DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        p.add(limit);
        return Jdbc.list(sql, this::mapSafe, p.toArray());
    }

    /** {vé đã vào cổng, vé hợp lệ (đã thanh toán)} của 1 sự kiện (0 = tất cả). */
    public int[] checkinTotals(int eventId) {
        String sql = "SELECT COALESCE(SUM(o.checked_in_qty), 0), COALESCE(SUM(CASE WHEN o.payment_status = ? THEN o.total_quantity ELSE 0 END), 0) FROM Orders o " + (eventId > 0 ? "WHERE o.event_id = ?" : "");
        Object[] p = eventId > 0 ? new Object[]{PAID, eventId} : new Object[]{PAID};
        int[] r = Jdbc.one(sql, rs -> new int[]{rs.getInt(1), rs.getInt(2)}, p);
        return r == null ? new int[]{0, 0} : r;
    }
}
