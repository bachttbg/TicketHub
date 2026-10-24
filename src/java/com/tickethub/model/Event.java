package com.tickethub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Event {
    private int eventId;
    private int categoryId;
    private String categoryName;
    private String categoryColor;
    private String title;
    private String description;
    private String location;
    private Timestamp eventDate;
    private BigDecimal minPrice;
    private String bannerColor;
    private String tagLabel;
    private boolean isFeatured;
    private int totalCapacity;
    private String status;
    private String imageUrl;

    // --- thống kê (chỉ có giá trị khi lấy từ trang quản trị) ---
    private int soldTickets;
    private int issuedTickets;
    private int orderCount;
    private int checkedIn;
    private BigDecimal revenue = BigDecimal.ZERO;

    public static final String STATUS_UPCOMING = "Sắp diễn ra";
    public static final String STATUS_SELLING = "Đang mở bán";
    public static final String STATUS_ENDED = "Đã kết thúc";
    public static final String STATUS_HIDDEN = "Tạm ẩn";
    public static final String[] STATUSES = {STATUS_UPCOMING, STATUS_SELLING, STATUS_ENDED, STATUS_HIDDEN};

    public Event() {}

    public int getSoldTickets() { return soldTickets; }
    public void setSoldTickets(int soldTickets) { this.soldTickets = soldTickets; }
    public int getIssuedTickets() { return issuedTickets; }
    public void setIssuedTickets(int issuedTickets) { this.issuedTickets = issuedTickets; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public int getCheckedIn() { return checkedIn; }
    public void setCheckedIn(int checkedIn) { this.checkedIn = checkedIn; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue == null ? BigDecimal.ZERO : revenue; }

    /** % vé đã bán so với sức chứa. */
    public double getFillPercent() {
        return totalCapacity <= 0 ? 0 : Math.min(100, Math.round(soldTickets * 1000.0 / totalCapacity) / 10.0);
    }

    /** Khách có thể mua vé khi sự kiện đang "Sắp diễn ra" hoặc "Đang mở bán". */
    public boolean isPurchasable() {
        return STATUS_UPCOMING.equals(status) || STATUS_SELLING.equals(status) || status == null;
    }

    public boolean isHidden() { return STATUS_HIDDEN.equals(status); }

    /** Giá trị cho ô datetime-local (yyyy-MM-ddTHH:mm). */
    public String getDateInput() {
        return eventDate == null ? "" : eventDate.toLocalDateTime().toString().substring(0, 16);
    }


    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Timestamp getEventDate() { return eventDate; }
    public void setEventDate(Timestamp eventDate) { this.eventDate = eventDate; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public String getBannerColor() { return bannerColor; }
    public void setBannerColor(String bannerColor) { this.bannerColor = bannerColor; }

    public String getTagLabel() { return tagLabel; }
    public void setTagLabel(String tagLabel) { this.tagLabel = tagLabel; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean isFeatured) { this.isFeatured = isFeatured; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /**
     * Đường dẫn ảnh (tương đối so với context path) dùng cho giao diện.
     * Ưu tiên cột image_url trong DB; nếu trống thì dùng ảnh mặc định
     * theo mã sự kiện (1-6) hoặc theo danh mục (cho sự kiện tạo mới).
     */
    public String getImagePath() {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) return imageUrl.trim();
        if (eventId >= 1 && eventId <= 6) return "assets/images/events/event-" + eventId + ".svg";
        switch (categoryId) {
            case 1:  return "assets/images/events/event-1.svg"; // Âm nhạc
            case 2:  return "assets/images/events/event-2.svg"; // Thể thao
            case 3:  return "assets/images/events/event-3.svg"; // Hội thảo
            case 4:  return "assets/images/events/event-4.svg"; // Sân khấu
            default: return "assets/images/events/event-1.svg";
        }
    }

    public String getFormattedPrice() {
        if (minPrice == null) return "0đ";
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(minPrice).replace(",", ".") + "đ";
    }

    public String getDayStr() {
        if (eventDate == null) return "01";
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        return sdf.format(eventDate);
    }

    public String getMonthStr() {
        if (eventDate == null) return "THG 1";
        SimpleDateFormat sdf = new SimpleDateFormat("MM");
        return "THG " + sdf.format(eventDate);
    }

    public String getFullDateStr() {
        if (eventDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        return sdf.format(eventDate);
    }
}
