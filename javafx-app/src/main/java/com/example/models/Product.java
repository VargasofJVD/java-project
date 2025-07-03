package com.example.models;

import java.time.LocalDateTime;

public class Product {
    private String id;
    private String name;
    private double price;
    private String description;
    private String unit; // e.g., kg, piece, bag
    private int quantity;
    private LocalDateTime createdAt;
    private String farmerId;
    private String imagePath;

    public Product(String name, double price, String description, String unit, int quantity, String farmerId) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.price = price;
        this.description = description;
        this.unit = unit;
        this.quantity = quantity;
        this.farmerId = farmerId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}