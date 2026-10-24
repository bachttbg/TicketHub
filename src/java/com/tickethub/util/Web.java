package com.tickethub.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Tiện ích web: đọc tham số, trả JSON, định dạng tiền/ngày. */
public final class Web {

    private Web() {}

    public static final Gson GSON = new GsonBuilder().create();

    // ---------- tham số ----------
    public static String str(HttpServletRequest r, String name) {
        String v = r.getParameter(name);
        return v == null ? "" : v.trim();
    }

    public static int intp(HttpServletRequest r, String name, int def) {
        try { return Integer.parseInt(str(r, name)); } catch (Exception e) { return def; }
    }

    public static LocalDate datep(HttpServletRequest r, String name) {
        try { return LocalDate.parse(str(r, name)); } catch (Exception e) { return null; }
    }

    public static BigDecimal decimal(String s) {
        return new BigDecimal(s.replace(".", "").replace(",", "").trim());
    }

    // ---------- JSON ----------
    public static void json(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(GSON.toJson(body));
    }

    public static void ok(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        json(resp, 200, m);
    }

    public static void ok(HttpServletResponse resp, String message, Map<String, Object> extra) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        m.putAll(extra);
        json(resp, 200, m);
    }

    public static void fail(HttpServletResponse resp, int status, String message) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        json(resp, status, m);
    }

    // ---------- định dạng ----------
    private static final DecimalFormat VND =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("vi", "VN")));
    static { VND.setGroupingUsed(true); }

    public static String vnd(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return VND.format(v).replace(',', '.') + "đ";
    }

    public static String vnd(long v) { return vnd(BigDecimal.valueOf(v)); }

    /** 1,2 tỷ / 482,5 tr / 350.000đ - ngắn gọn cho thẻ KPI. */
    public static String vndShort(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        double d = v.doubleValue();
        if (d >= 1_000_000_000d) return trim(d / 1_000_000_000d) + " tỷ";
        if (d >= 1_000_000d) return trim(d / 1_000_000d) + " tr";
        if (d >= 1_000d) return trim(d / 1_000d) + "k";
        return (long) d + "đ";
    }

    private static String trim(double d) {
        String s = String.format(Locale.US, "%.1f", d);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s.replace('.', ',');
    }

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String dateTime(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(DT);
    }

    public static String date(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(D);
    }

    public static String date(LocalDate d) { return d == null ? "" : d.format(D); }

    public static Timestamp startOf(LocalDate d) { return Timestamp.valueOf(d.atStartOfDay()); }

    public static Timestamp endOf(LocalDate d) { return Timestamp.valueOf(d.plusDays(1).atStartOfDay()); }

    public static LocalDateTime now() { return LocalDateTime.now(); }

    public static double pct(double a, double b) {
        return b <= 0 ? 0 : Math.round(a * 1000.0 / b) / 10.0;
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String payLabel(String m) {
        if (m == null) return "Thẻ ATM";
        switch (m) {
            case "qr": return "QR chuyển khoản";
            case "wallet": return "Ví điện tử";
            case "intl": return "Visa / Mastercard";
            default: return "Thẻ ATM";
        }
    }

    /** Ảnh của sự kiện: ảnh riêng (image_url) hoặc ảnh mặc định theo mã sự kiện. */
    public static String eventImage(int eventId, String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) return imageUrl.trim();
        return "assets/images/events/event-" + (eventId >= 1 && eventId <= 6 ? eventId : 1) + ".svg";
    }
}
