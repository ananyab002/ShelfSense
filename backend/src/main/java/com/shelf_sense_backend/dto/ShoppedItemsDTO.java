package com.shelf_sense_backend.dto;

public class ShoppedItemsDTO {
    private String rawName; // The "base" name without weight/volume if separated
    private int quantity;
    private String weightOrVolume; // e.g., "125 g", "1.1 kg", "6 pcs"

    // Default constructor
    public ShoppedItemsDTO() {
    }

    // Constructor with parameters
    public ShoppedItemsDTO(String rawName, int quantity, String weightOrVolume) {
        this.rawName = rawName;
        this.quantity = quantity;
        this.weightOrVolume = weightOrVolume;
    }

    // Getters and setters
    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getWeightOrVolume() {
        return weightOrVolume;
    }

    public void setWeightOrVolume(String weightOrVolume) {
        this.weightOrVolume = weightOrVolume;
    }



    @Override
    public String toString() {
        return "ShoppedItemsDTO{" +
                "rawName='" + rawName + '\'' +
                ", quantity=" + quantity +
                ", weightOrVolume='" + weightOrVolume + '\'' +
                '}';
    }
}