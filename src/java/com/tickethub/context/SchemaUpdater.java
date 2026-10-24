package com.tickethub.context;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Tự nâng cấp cấu trúc CSDL khi ứng dụng khởi động (chạy lặp lại vẫn an toàn).
 * Nhờ vậy database cũ chỉ cần chạy lại ứng dụng là có đủ cột/bảng cho trang quản trị mới.
 */
@WebListener
public class SchemaUpdater implements ServletContextListener {

    private static final String[] STEPS = {
        "IF COL_LENGTH('Events','image_url') IS NULL ALTER TABLE Events ADD image_url VARCHAR(255) NULL",
        "IF COL_LENGTH('Orders','checked_in_qty') IS NULL ALTER TABLE Orders ADD checked_in_qty INT NOT NULL DEFAULT 0",
        "IF COL_LENGTH('Orders','last_checkin_at') IS NULL ALTER TABLE Orders ADD last_checkin_at DATETIME NULL",
        "IF COL_LENGTH('Orders','payment_ref') IS NULL ALTER TABLE Orders ADD payment_ref VARCHAR(100) NULL",
        "IF OBJECT_ID('Settings','U') IS NULL CREATE TABLE Settings (setting_key VARCHAR(60) PRIMARY KEY, setting_value NVARCHAR(500) NOT NULL)",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='service_fee') INSERT INTO Settings VALUES ('service_fee','5000')",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='coupon_code') INSERT INTO Settings VALUES ('coupon_code','TICKIE10')",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='coupon_percent') INSERT INTO Settings VALUES ('coupon_percent','10')",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='hotline') INSERT INTO Settings VALUES ('hotline','1900 6868')",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='support_email') INSERT INTO Settings VALUES ('support_email','support@tickethub.vn')",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='address') INSERT INTO Settings VALUES ('address',N'Hà Nội, Việt Nam')",
        "IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key='support_hours') INSERT INTO Settings VALUES ('support_hours',N'08:00 – 22:00 mỗi ngày')"
    };

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (Boolean.getBoolean("tickethub.skipSchemaUpdate")) return;
        try (Connection c = DBContext.getConnection(); Statement st = c.createStatement()) {
            for (String sql : STEPS) {
                try {
                    st.execute(sql);
                } catch (Exception e) {
                    System.err.println("[SchemaUpdater] Bỏ qua bước: " + e.getMessage());
                }
            }
            System.out.println("[SchemaUpdater] Cơ sở dữ liệu đã được kiểm tra / nâng cấp.");
        } catch (Exception e) {
            System.err.println("[SchemaUpdater] Không kết nối được CSDL lúc khởi động: " + e.getMessage());
        }
    }
}
