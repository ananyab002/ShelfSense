package com.shelf_sense_backend.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "orders", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"orderNumber"})
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String orderNumber;

    @Column(nullable = false)
    private LocalDate orderDate;
   
    @OneToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "processed_email_id", referencedColumnName = "id", unique = true)
    private ProcessedEmail processedEmail;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShoppedItem> items = new ArrayList<>();

    public void addItem(ShoppedItem item) {
        items.add(item);
        item.setOrder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderNumber, order.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderNumber);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public ProcessedEmail getProcessedEmail() {
        return processedEmail;
    }

    public void setProcessedEmail(ProcessedEmail processedEmail) {
        this.processedEmail = processedEmail;
    }

    public List<ShoppedItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppedItem> items) {
        this.items = items;
    }
    
    // Helper method to remove item
    public void removeItem(ShoppedItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}