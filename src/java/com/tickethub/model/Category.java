package com.tickethub.model;

public class Category {
    private int categoryId;
    private String categoryName;
    private String colorHex;

    public Category() {}

    public Category(int categoryId, String categoryName, String colorHex) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.colorHex = colorHex;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}
