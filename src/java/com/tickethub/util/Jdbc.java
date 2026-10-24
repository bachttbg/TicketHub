package com.tickethub.util;

import com.tickethub.context.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Tiện ích JDBC dùng chung cho các DAO quản trị (tự đóng kết nối, ném lỗi rõ ràng). */
public final class Jdbc {

    private Jdbc() {}

    @FunctionalInterface
    public interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    @FunctionalInterface
    public interface TxWork<T> { T run(Connection c) throws Exception; }

    public static class DataAccessException extends RuntimeException {
        public DataAccessException(String msg, Throwable cause) { super(msg, cause); }
        public DataAccessException(String msg) { super(msg); }
    }

    public static Connection open() {
        try {
            return DBContext.getConnection();
        } catch (Exception e) {
            throw new DataAccessException("Không kết nối được cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }

    public static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    public static <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = open()) {
            return list(c, sql, mapper, params);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi truy vấn: " + e.getMessage(), e);
        }
    }

    public static <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
        }
        return out;
    }

    public static <T> T one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> l = list(sql, mapper, params);
        return l.isEmpty() ? null : l.get(0);
    }

    public static <T> T one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> l = list(c, sql, mapper, params);
        return l.isEmpty() ? null : l.get(0);
    }

    public static long count(String sql, Object... params) {
        Long v = one(sql, rs -> rs.getLong(1), params);
        return v == null ? 0 : v;
    }

    public static int exec(String sql, Object... params) {
        try (Connection c = open()) {
            return exec(c, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật: " + e.getMessage(), e);
        }
    }

    public static int exec(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT và trả về khóa tự tăng. */
    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /** Chạy một nhóm thao tác trong 1 transaction (lỗi thì rollback). */
    public static <T> T tx(TxWork<T> work) {
        Connection c = open();
        try {
            c.setAutoCommit(false);
            T r = work.run(c);
            c.commit();
            return r;
        } catch (DataAccessException e) {
            rollback(c);
            throw e;
        } catch (Exception e) {
            rollback(c);
            throw new DataAccessException(e.getMessage() != null ? e.getMessage() : e.toString(), e);
        } finally {
            try { c.setAutoCommit(true); } catch (Exception ignored) {}
            DBContext.closeQuietly(c);
        }
    }

    private static void rollback(Connection c) {
        try { c.rollback(); } catch (Exception ignored) {}
    }
}
