package com.example.models;

import java.util.ArrayList;
import java.util.List;

public class Farmer {
    private String id;
    private String fullName;
    private String username;
    private String email;
    private String phoneNumber;
    private String farmName;
    private String farmLocation;
    private List<Product> products;
    private String password; // In a real app, this should be hashed

    public Farmer(String fullName, String username, String email, String phoneNumber,
            String farmName, String farmLocation, String password) {
        this.id = java.util.UUID.randomUUID().toString();
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.farmName = farmName;
        this.farmLocation = farmLocation;
        this.password = password;
        this.products = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFarmLocation() {
        return farmLocation;
    }

    public void setFarmLocation(String farmLocation) {
        this.farmLocation = farmLocation;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void addProduct(Product product) {
        this.products.add(product);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}