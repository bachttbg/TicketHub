package com.tickethub.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class OrderDetail {
    private int orderDetailId;
    private int orderId;
    private int ticketTypeId;
    private String ticketTypeName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;

    public OrderDetail() {}

    public int getOrderDetailId() { return orderDetailId; }
    public void setOrderDetailId(int orderDetailId) { this.orderDetailId = orderDetailId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(int ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    public String getTicketTypeName() { return ticketTypeName; }
    public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFormattedPrice() {
        if (unitPrice == null) return "0đ";
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(unitPrice).replace(",", ".") + "đ";
    }

    public String getFormattedAmount() {
        if (amount == null) return "0đ";
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(amount).replace(",", ".") + "đ";
    }
}
