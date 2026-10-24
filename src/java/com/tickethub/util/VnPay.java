package com.tickethub.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tích hợp cổng thanh toán VNPay SANDBOX (API 2.1.0).
 *  - buildPaymentUrl : tạo URL chuyển khách sang cổng thanh toán (có chữ ký HMAC-SHA512)
 *  - verify          : kiểm tra chữ ký VNPay gửi về (return URL / IPN)
 *  - describe        : đổi mã phản hồi vnp_ResponseCode sang tiếng Việt
 */
public final class VnPay {

    private VnPay() {}

    private static final Properties CFG = new Properties();

    static {
        try (InputStream in = VnPay.class.getClassLoader().getResourceAsStream("vnpay.properties")) {
            if (in != null) CFG.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[VnPay] Không đọc được vnpay.properties: " + e.getMessage());
        }
    }

    private static String cfg(String key, String envName) {
        String env = System.getenv(envName);
        if (env != null && !env.trim().isEmpty()) return env.trim();
        return CFG.getProperty(key, "").trim();
    }

    public static String tmnCode()    { return cfg("vnpay.tmnCode", "VNPAY_TMN_CODE"); }
    private static String secret()    { return cfg("vnpay.hashSecret", "VNPAY_HASH_SECRET"); }
    public static String payUrl()     {
        String u = cfg("vnpay.payUrl", "VNPAY_PAY_URL");
        return u.isEmpty() ? "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" : u;
    }
    public static int expireMinutes() {
        try { return Integer.parseInt(cfg("vnpay.expireMinutes", "VNPAY_EXPIRE_MINUTES")); } catch (Exception e) { return 15; }
    }

    /** Đã điền TmnCode + HashSecret thật chưa? */
    public static boolean isConfigured() {
        String t = tmnCode(), s = secret();
        return !t.isEmpty() && !s.isEmpty() && !t.startsWith("YOUR_") && !s.startsWith("YOUR_");
    }

    /** URL VNPay gọi lại trình duyệt sau khi thanh toán xong. */
    public static String returnUrl(HttpServletRequest req) {
        String fixed = cfg("vnpay.returnUrl", "VNPAY_RETURN_URL");
        if (!fixed.isEmpty()) return fixed;
        StringBuilder sb = new StringBuilder(req.getScheme()).append("://").append(req.getServerName());
        int port = req.getServerPort();
        boolean std = ("http".equals(req.getScheme()) && port == 80) || ("https".equals(req.getScheme()) && port == 443);
        if (!std) sb.append(':').append(port);
        return sb.append(req.getContextPath()).append("/payment/vnpay-return").toString();
    }

    /** Tạo URL thanh toán. amountVnd là số tiền VND (VNPay yêu cầu nhân 100). */
    public static String buildPaymentUrl(HttpServletRequest req, String txnRef, BigDecimal amountVnd,
                                         String orderInfo, String bankCode) {
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZonedDateTime now = ZonedDateTime.now(vn);

        Map<String, String> p = new HashMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", tmnCode());
        p.put("vnp_Amount", amountVnd.multiply(new BigDecimal(100)).toBigInteger().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_OrderInfo", orderInfo);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", returnUrl(req));
        p.put("vnp_IpAddr", clientIp(req));
        p.put("vnp_CreateDate", f.format(now));
        p.put("vnp_ExpireDate", f.format(now.plusMinutes(expireMinutes())));
        if (bankCode != null && !bankCode.isEmpty()) p.put("vnp_BankCode", bankCode);

        String hashData = hashData(p);
        String query = hashData; // VNPay: query và dữ liệu ký dùng cùng cách mã hóa
        String secureHash = hmacSha512(secret(), hashData);
        return payUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /** Kiểm tra chữ ký của tham số VNPay gửi về. */
    public static boolean verify(HttpServletRequest req) {
        String received = req.getParameter("vnp_SecureHash");
        if (received == null || received.isEmpty()) return false;
        Map<String, String> p = new HashMap<>();
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            String k = e.getKey();
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")
                    && e.getValue().length > 0 && !e.getValue()[0].isEmpty()) {
                p.put(k, e.getValue()[0]);
            }
        }
        String expected = hmacSha512(secret(), hashData(p));
        return expected.equalsIgnoreCase(received);
    }

    /** key=value&key=value (sắp xếp theo tên, giá trị URL-encode US-ASCII). */
    private static String hashData(Map<String, String> params) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            String v = params.get(n);
            if (v == null || v.isEmpty()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(n).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    private static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC-SHA512", e);
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = req.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        if (ip == null || ip.isEmpty() || ip.contains(":")) ip = "127.0.0.1"; // IPv6 (::1) -> IPv4
        return ip;
    }

    /** Mã phản hồi vnp_ResponseCode -> thông báo tiếng Việt. */
    public static String describe(String code) {
        if (code == null) return "Không nhận được phản hồi từ VNPay.";
        switch (code) {
            case "00": return "Giao dịch thành công.";
            case "07": return "Giao dịch bị nghi ngờ gian lận, VNPay đã trừ tiền nhưng chờ xác minh.";
            case "09": return "Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking.";
            case "10": return "Xác thực thông tin thẻ/tài khoản quá 3 lần.";
            case "11": return "Đã hết thời gian chờ thanh toán.";
            case "12": return "Thẻ/Tài khoản của bạn đang bị khóa.";
            case "13": return "Bạn nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24": return "Bạn đã hủy giao dịch.";
            case "51": return "Tài khoản không đủ số dư để thanh toán.";
            case "65": return "Tài khoản đã vượt hạn mức giao dịch trong ngày.";
            case "75": return "Ngân hàng thanh toán đang bảo trì.";
            case "79": return "Bạn nhập sai mật khẩu thanh toán quá số lần quy định.";
            default:   return "Giao dịch không thành công (mã lỗi " + code + ").";
        }
    }
}
