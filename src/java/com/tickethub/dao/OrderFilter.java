package com.tickethub.dao;

import com.tickethub.util.Web;

import java.time.LocalDate;
import java.util.List;

/** Bộ lọc đơn hàng dùng chung cho trang quản lý, xuất CSV, báo cáo. */
public class OrderFilter {
    public String keyword = "";
    public String status = "";   // rỗng = tất cả
    public String payment = "";  // bank | qr | wallet | intl
    public int eventId = 0;      // 0 = tất cả
    public Integer userId = null;
    public LocalDate from;
    public LocalDate to;

    /** Sinh mệnh đề WHERE (bắt đầu bằng "WHERE 1=1") và nạp tham số. */
    public String where(List<Object> p, boolean withStatus) {
        StringBuilder w = new StringBuilder("WHERE 1=1 ");
        if (keyword != null && !keyword.isEmpty()) {
            w.append("AND (LOWER(o.order_code) LIKE ? OR LOWER(o.customer_name) LIKE ? OR LOWER(o.customer_email) LIKE ? OR o.customer_phone LIKE ?) ");
            String t = "%" + keyword.toLowerCase() + "%";
            p.add(t); p.add(t); p.add(t); p.add("%" + keyword + "%");
        }
        if (withStatus && status != null && !status.isEmpty()) { w.append("AND o.payment_status = ? "); p.add(status); }
        if (payment != null && !payment.isEmpty()) { w.append("AND o.payment_method = ? "); p.add(payment); }
        if (eventId > 0) { w.append("AND o.event_id = ? "); p.add(eventId); }
        if (userId != null) { w.append("AND o.user_id = ? "); p.add(userId); }
        if (from != null) { w.append("AND o.created_at >= ? "); p.add(Web.startOf(from)); }
        if (to != null) { w.append("AND o.created_at < ? "); p.add(Web.endOf(to)); }
        return w.toString();
    }

    // getter cho EL trong JSP
    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public String getPayment() { return payment; }
    public int getEventId() { return eventId; }
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
}
