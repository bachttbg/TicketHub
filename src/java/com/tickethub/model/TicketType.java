package com.tickethub.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class TicketType {
    private int ticketTypeId;
    private int eventId;
    private String typeName;
    private String description;
    private BigDecimal price;
    private int totalQuantity;
    private int soldQuantity;
    private String barColor;
    private java.math.BigDecimal revenue = java.math.BigDecimal.ZERO;

    public java.math.BigDecimal getRevenue() { return revenue; }
    public void setRevenue(java.math.BigDecimal revenue) { this.revenue = revenue == null ? java.math.BigDecimal.ZERO : revenue; }
    public int getRemaining() { return Math.max(0, totalQuantity - soldQuantity); }
    public double getFillPercent() {
        return totalQuantity <= 0 ? 0 : Math.min(100, Math.round(soldQuantity * 1000.0 / totalQuantity) / 10.0);
    }

    public TicketType() {}

    public int getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(int ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public int getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(int soldQuantity) { this.soldQuantity = soldQuantity; }

    public String getBarColor() { return barColor; }
    public void setBarColor(String barColor) { this.barColor = barColor; }

    public int getRemainingQuantity() {
        return Math.max(0, totalQuantity - soldQuantity);
    }

    public double getSoldPercentage() {
        if (totalQuantity <= 0) return 0;
        return (double) soldQuantity * 100.0 / totalQuantity;
    }

    public String getFormattedPrice() {
        if (price == null) return "0đ";
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(price).replace(",", ".") + "đ";
    }
}
