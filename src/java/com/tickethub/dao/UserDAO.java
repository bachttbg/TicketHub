package com.tickethub.dao;

import com.tickethub.context.DBContext;
import com.tickethub.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public User login(String emailOrPhone, String password) {
        String sql = "SELECT * FROM Users WHERE (email = ? OR phone = ?) AND password = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, emailOrPhone.trim());
            ps.setString(2, emailOrPhone.trim());
            ps.setString(3, password);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getTimestamp("created_at")
                );
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

    public boolean register(User user) {
        String sql = "INSERT INTO Users (full_name, email, phone, password, role, created_at) VALUES (?, ?, ?, ?, 'CUSTOMER', GETDATE())";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getPassword());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
    }

    public boolean checkEmailExists(String email) {
        String sql = "SELECT user_id FROM Users WHERE email = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBContext.closeQuietly(rs);
            DBContext.closeQuietly(ps);
            DBContext.closeQuietly(conn);
        }
        return false;
    }

    // ---------- Cài đặt tài khoản (trang quản trị) ----------

    public User getById(int userId) {
        return com.tickethub.util.Jdbc.one("SELECT * FROM Users WHERE user_id = ?", rs -> {
            User u = new User();
            u.setUserId(rs.getInt("user_id"));
            u.setFullName(rs.getString("full_name"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setRole(rs.getString("role"));
            u.setCreatedAt(rs.getTimestamp("created_at"));
            return u;
        }, userId);
    }

    public void updateProfile(int userId, String fullName, String phone) {
        long dup = com.tickethub.util.Jdbc.count("SELECT COUNT(*) FROM Users WHERE phone = ? AND user_id <> ?", phone, userId);
        if (dup > 0) throw new com.tickethub.util.Jdbc.DataAccessException("Số điện thoại này đã được tài khoản khác sử dụng.");
        com.tickethub.util.Jdbc.exec("UPDATE Users SET full_name = ?, phone = ? WHERE user_id = ?", fullName, phone, userId);
    }

    public boolean checkPassword(int userId, String password) {
        return com.tickethub.util.Jdbc.count("SELECT COUNT(*) FROM Users WHERE user_id = ? AND password = ?", userId, password) > 0;
    }

    public void changePassword(int userId, String newPassword) {
        com.tickethub.util.Jdbc.exec("UPDATE Users SET password = ? WHERE user_id = ?", newPassword, userId);
    }
}
