package com.shelf_sense_backend.model;

public enum SuggestionType {
    BUY_NOW("Buy now"),
    LOW_STOCK("Low stock"),
    CHECK_NOW("Not purchased in a while"),;
    
    private final String displayName;
    
    SuggestionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
