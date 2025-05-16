package com.shelf_sense_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "shopped_items")
public class ShoppedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String rawName;

    @Column(length = 100) // Allow null or empty string if not always present
    private String weightOrVolume;

    @Column(name = "general_name")
    private String generalName;

    @Column(name = "food_type")
    private String foodType;

    private int quantity;
    //private String price;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Every item MUST belong to an order
    @JoinColumn(name = "order_id", nullable = false) // Foreign key column in shopped_items table
    private Order order;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public String getGeneralName() {
        return generalName;
    }

    public void setGeneralName(String generalName) {
        this.generalName = generalName;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    
}