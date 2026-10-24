package com.tickethub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int orderId;
    private String orderCode;
    private Integer userId;
    private int eventId;
    private String eventTitle;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String ticketTypeSummary;
    private int totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal serviceFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderTimeDesc;
    private Timestamp createdAt;
    private int checkedInQty;
    private Timestamp lastCheckinAt;
    private String eventImage;

    public int getCheckedInQty() { return checkedInQty; }
    public void setCheckedInQty(int checkedInQty) { this.checkedInQty = checkedInQty; }
    public Timestamp getLastCheckinAt() { return lastCheckinAt; }
    public void setLastCheckinAt(Timestamp lastCheckinAt) { this.lastCheckinAt = lastCheckinAt; }
    public String getEventImage() { return eventImage; }
    public void setEventImage(String eventImage) { this.eventImage = eventImage; }
    public int getRemainingToCheckin() { return Math.max(0, totalQuantity - checkedInQty); }
    public boolean isPaid() { return "Đã thanh toán".equals(paymentStatus); }
    public boolean isCancelled() { return "Đã hủy".equals(paymentStatus); }
    public String getCreatedText() { return com.tickethub.util.Web.dateTime(createdAt); }
    public String getCheckinText() { return com.tickethub.util.Web.dateTime(lastCheckinAt); }

    private List<OrderDetail> details = new ArrayList<>();

    public Order() {}

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getTicketTypeSummary() { return ticketTypeSummary; }
    public void setTicketTypeSummary(String ticketTypeSummary) { this.ticketTypeSummary = ticketTypeSummary; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getOrderTimeDesc() { return orderTimeDesc; }
    public void setOrderTimeDesc(String orderTimeDesc) { this.orderTimeDesc = orderTimeDesc; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<OrderDetail> getDetails() { return details; }
    public void setDetails(List<OrderDetail> details) { this.details = details; }

    public String getFormattedTotal() {
        if (totalAmount == null) return "0đ";
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(totalAmount).replace(",", ".") + "đ";
    }

    public String getStatusBadgeClass() {
        if ("Đã thanh toán".equalsIgnoreCase(paymentStatus)) {
            return "st-paid";
        } else if ("Chờ thanh toán".equalsIgnoreCase(paymentStatus)) {
            return "st-pending";
        } else if ("Đã hủy".equalsIgnoreCase(paymentStatus)) {
            return "st-cancelled";
        }
        return "st-pending";
    }
}
