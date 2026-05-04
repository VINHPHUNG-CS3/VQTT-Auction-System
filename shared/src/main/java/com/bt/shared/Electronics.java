package com.bt.shared;

public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice); // Pass basic info up to the Item class
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ELECTRONICS;
    }

    @Override
    public void displayInfo() {
        super.displayInfo(); // Prints the standard ID, Name, Starting Price [cite: 121]
        System.out.println(
                "Category: " + getCategory() + " | Brand: " + brand + " | Warranty: " + warrantyMonths + " months");
    }
}