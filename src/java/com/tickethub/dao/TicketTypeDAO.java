package com.tickethub.dao;

import com.tickethub.context.DBContext;
import com.tickethub.model.TicketType;
import com.tickethub.util.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TicketTypeDAO {

    public List<TicketType> getTicketTypesByEventId(int eventId) {
        List<TicketType> list = new ArrayList<>();
        String sql = "SELECT * FROM TicketTypes WHERE event_id = ? ORDER BY price DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, eventId);
            rs = ps.executeQuery();
            while (rs.next()) {
                TicketType tt = new TicketType();
                tt.setTicketTypeId(rs.getInt("ticket_type_id"));
                tt.setEventId(rs.getInt("event_id"));
                tt.setTypeName(rs.getString("type_name"));
                tt.setDescription(rs.getString("description"));
                tt.setPrice(rs.getBigDecimal("price"));
                tt.setTotalQuantity(rs.getInt("total_quantity"));
                tt.setSoldQuantity(rs.getInt("sold_quantity"));
                tt.setBarColor(rs.getString("bar_color"));
                list.add(tt);
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

    public TicketType getTicketTypeById(int ticketTypeId) {
        String sql = "SELECT * FROM TicketTypes WHERE ticket_type_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, ticketTypeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                TicketType tt = new TicketType();
                tt.setTicketTypeId(rs.getInt("ticket_type_id"));
                tt.setEventId(rs.getInt("event_id"));
                tt.setTypeName(rs.getString("type_name"));
                tt.setDescription(rs.getString("description"));
                tt.setPrice(rs.getBigDecimal("price"));
                tt.setTotalQuantity(rs.getInt("total_quantity"));
                tt.setSoldQuantity(rs.getInt("sold_quantity"));
                tt.setBarColor(rs.getString("bar_color"));
                return tt;
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

    public boolean updateSoldQuantity(Connection conn, int ticketTypeId, int addQty) throws Exception {
        String sql = "UPDATE TicketTypes SET sold_quantity = sold_quantity + ? WHERE ticket_type_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addQty);
            ps.setInt(2, ticketTypeId);
            return ps.executeUpdate() > 0;
        }
    }

    // =====================================================================
    //  QUẢN TRỊ LOẠI VÉ
    // =====================================================================

    private TicketType mapAdmin(java.sql.ResultSet rs) throws java.sql.SQLException {
        TicketType t = new TicketType();
        t.setTicketTypeId(rs.getInt("ticket_type_id"));
        t.setEventId(rs.getInt("event_id"));
        t.setTypeName(rs.getString("type_name"));
        t.setDescription(rs.getString("description"));
        t.setPrice(rs.getBigDecimal("price"));
        t.setTotalQuantity(rs.getInt("total_quantity"));
        t.setSoldQuantity(rs.getInt("sold_quantity"));
        t.setBarColor(rs.getString("bar_color"));
        t.setRevenue(rs.getBigDecimal("revenue"));
        return t;
    }

    private static final String ADMIN_SELECT =
        "SELECT tt.*, COALESCE((SELECT SUM(od.amount) FROM OrderDetails od JOIN Orders o ON o.order_id = od.order_id " +
        "WHERE od.ticket_type_id = tt.ticket_type_id AND o.payment_status = N'Đã thanh toán'), 0) AS revenue FROM TicketTypes tt ";

    public List<TicketType> adminListByEvent(int eventId) {
        return Jdbc.list(ADMIN_SELECT + "WHERE tt.event_id = ? ORDER BY tt.price DESC", this::mapAdmin, eventId);
    }

    public TicketType adminGet(int id) {
        return Jdbc.one(ADMIN_SELECT + "WHERE tt.ticket_type_id = ?", this::mapAdmin, id);
    }

    /** Tổng vé phát hành của sự kiện (có thể loại trừ 1 loại vé đang sửa). */
    public int totalIssued(int eventId, int excludeId) {
        return (int) Jdbc.count("SELECT COALESCE(SUM(total_quantity), 0) FROM TicketTypes WHERE event_id = ? AND ticket_type_id <> ?", eventId, excludeId);
    }

    public int create(TicketType t) {
        return Jdbc.tx(c -> {
            int id = Jdbc.insert(c,
                "INSERT INTO TicketTypes (event_id, type_name, description, price, total_quantity, sold_quantity, bar_color) VALUES (?,?,?,?,?,0,?)",
                t.getEventId(), t.getTypeName(), t.getDescription(), t.getPrice(), t.getTotalQuantity(), t.getBarColor());
            EventDAO.refreshMinPrice(c, t.getEventId());
            return id;
        });
    }

    public void update(TicketType t) {
        Jdbc.tx(c -> {
            Jdbc.exec(c, "UPDATE TicketTypes SET type_name=?, description=?, price=?, total_quantity=?, bar_color=? WHERE ticket_type_id=?",
                t.getTypeName(), t.getDescription(), t.getPrice(), t.getTotalQuantity(), t.getBarColor(), t.getTicketTypeId());
            EventDAO.refreshMinPrice(c, t.getEventId());
            return null;
        });
    }

    public void delete(int id) {
        Jdbc.tx(c -> {
            TicketType t = Jdbc.one(c, "SELECT ticket_type_id, event_id FROM TicketTypes WHERE ticket_type_id = ?", rs -> {
                TicketType x = new TicketType(); x.setTicketTypeId(rs.getInt(1)); x.setEventId(rs.getInt(2)); return x; }, id);
            if (t == null) throw new Jdbc.DataAccessException("Loại vé không tồn tại.");
            Long n = Jdbc.one(c, "SELECT COUNT(*) FROM OrderDetails WHERE ticket_type_id = ?", rs -> rs.getLong(1), id);
            if (n != null && n > 0) throw new Jdbc.DataAccessException("Loại vé đã có đơn hàng nên không thể xóa. Bạn có thể giảm số lượng phát hành bằng số đã bán để ngừng bán.");
            Jdbc.exec(c, "DELETE FROM TicketTypes WHERE ticket_type_id = ?", id);
            EventDAO.refreshMinPrice(c, t.getEventId());
            return null;
        });
    }
}
