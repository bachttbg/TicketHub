package com.tickethub.dao;

import com.tickethub.util.Jdbc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cài đặt hệ thống (phí dịch vụ, mã giảm giá, thông tin liên hệ...). */
public class SettingsDAO {

    public static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("service_fee", "5000");
        DEFAULTS.put("coupon_code", "TICKIE10");
        DEFAULTS.put("coupon_percent", "10");
        DEFAULTS.put("hotline", "1900 6868");
        DEFAULTS.put("support_email", "support@tickethub.vn");
        DEFAULTS.put("address", "Hà Nội, Việt Nam");
        DEFAULTS.put("support_hours", "08:00 – 22:00 mỗi ngày");
    }

    /** Luôn trả về đủ khóa (thiếu thì dùng mặc định); lỗi DB cũng không làm hỏng trang. */
    public Map<String, String> getAll() {
        Map<String, String> m = new LinkedHashMap<>(DEFAULTS);
        try {
            Jdbc.list("SELECT setting_key, setting_value FROM Settings",
                    rs -> { m.put(rs.getString(1), rs.getString(2)); return null; });
        } catch (Exception ignored) {
            // dùng giá trị mặc định
        }
        return m;
    }

    public void save(Map<String, String> values) {
        Jdbc.tx(c -> {
            for (Map.Entry<String, String> e : values.entrySet()) {
                if (!DEFAULTS.containsKey(e.getKey())) continue;
                int n = Jdbc.exec(c, "UPDATE Settings SET setting_value = ? WHERE setting_key = ?", e.getValue(), e.getKey());
                if (n == 0) Jdbc.exec(c, "INSERT INTO Settings (setting_key, setting_value) VALUES (?, ?)", e.getKey(), e.getValue());
            }
            return null;
        });
    }

    public BigDecimal serviceFee(Map<String, String> s) {
        try { return new BigDecimal(s.get("service_fee")); } catch (Exception e) { return new BigDecimal("5000"); }
    }

    public String couponCode(Map<String, String> s) {
        String c = s.get("coupon_code");
        return c == null ? "" : c.trim();
    }

    public int couponPercent(Map<String, String> s) {
        try { return Math.max(0, Math.min(100, Integer.parseInt(s.get("coupon_percent").trim()))); } catch (Exception e) { return 0; }
    }
}
