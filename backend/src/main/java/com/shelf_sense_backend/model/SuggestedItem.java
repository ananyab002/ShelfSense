package com.shelf_sense_backend.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "suggested_items")
public class SuggestedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private LocalDate lastPurchaseDate;

    @Enumerated(EnumType.STRING)
    private SuggestionType suggestionType;

    public SuggestedItem() {
    }

    public SuggestedItem(String itemName, LocalDate lastPurchaseDate, SuggestionType suggestionType) {
        this.itemName = itemName;
        this.lastPurchaseDate = lastPurchaseDate;
        this.suggestionType = suggestionType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public LocalDate getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(LocalDate lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public SuggestionType getSuggestionType() {
        return suggestionType;
    }

    public void setSuggestionTye(SuggestionType suggestionType) {
        this.suggestionType = suggestionType;
    }

}
