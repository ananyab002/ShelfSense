package com.shelf_sense_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shelf_sense_backend.model.Order;
import com.shelf_sense_backend.model.ShoppedItem;

@Service
public class GroceryReceiptAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(GroceryReceiptAnalyzer.class);

    @Autowired
    private AIModelClient aiModelClient;

    public List<ShoppedItem> parseReceipt(String receiptText, Order order) {

        try {
            List<ShoppedItem> items = extractItemsUsingAI(receiptText, order);
            if (!items.isEmpty()) {
                logger.info("Successfully extracted {} items using AI", items.size());
                return items;
            }
        } catch (Exception e) {
            logger.warn("AI extraction failed, falling back to pattern matching: {}", e.getMessage());
        }

        return null;
    }

    private List<ShoppedItem> extractItemsUsingAI(String receiptText, Order order) {
        List<ShoppedItem> items = new ArrayList<>();

        List<Map<String, Object>> extractedItems = aiModelClient.processReceiptText(receiptText);

        for (Map<String, Object> extractedItem : extractedItems) {
            ShoppedItem item = new ShoppedItem();

            if (extractedItem.containsKey("product_name")) {
                item.setRawName((String) extractedItem.get("product_name"));
            }

            if (extractedItem.containsKey("quantity")) {
                Object quantityObj = extractedItem.get("quantity");
                if (quantityObj instanceof Number) {
                    item.setQuantity(((Number) quantityObj).intValue());
                } else if (quantityObj instanceof String) {
                    try {
                        item.setQuantity((int) Double.parseDouble((String) quantityObj));
                    } catch (NumberFormatException e) {
                        item.setQuantity(1);
                    }
                }
            } else {
                item.setQuantity(1);
            }

            if (extractedItem.containsKey("weight")) {
                String weightValue = (String) extractedItem.get("weight");
                // Check if the weight is just a unit without a value
                if (weightValue.matches("(?i)grams|kg|ml|piece|g")) {
                    // Extract the actual weight from the product name
                    String extractedWeight = extractWeightFromProductName(item.getRawName());
                    if (extractedWeight != null) {
                        item.setWeightOrVolume(extractedWeight);
                    } else {
                        item.setWeightOrVolume(weightValue); // Fall back to the original value
                    }
                } else {
                    item.setWeightOrVolume(weightValue);
                }
            }
            if (extractedItem.containsKey("general_name")) {
                item.setGeneralName((String) extractedItem.get("general_name"));
            }

            if (extractedItem.containsKey("food_type")) {
                item.setFoodType((String) extractedItem.get("food_type"));
            }
            item.setOrder(order);

            items.add(item);
        }

        return items;
    }

    private String extractWeightFromProductName(String productName) {
        Pattern weightPattern = Pattern.compile(
                "(\\d+(?:[.,]\\d+)?\\s*(?:g|kg|ml|l|stk))\\b|" + // Match 125 g, 1,1 kg, 500 ml, 1 l
                        "(\\d+\\s+stk)\\b", // Match "6 stk", "2 stk" format
                Pattern.CASE_INSENSITIVE);

        Matcher weightMatcher = weightPattern.matcher(productName);
        if (weightMatcher.find()) {
            String weight = weightMatcher.group(0);
            return weight.trim();
        }

        return null;
    }

   
}