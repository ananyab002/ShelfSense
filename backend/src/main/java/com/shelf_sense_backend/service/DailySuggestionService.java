package com.shelf_sense_backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.shelf_sense_backend.model.ShoppedItem;
import com.shelf_sense_backend.model.SuggestedItem;
import com.shelf_sense_backend.model.SuggestionType;
import com.shelf_sense_backend.repo.ShoppedItemRepository;
import com.shelf_sense_backend.repo.SuggestedItemRepository;

import jakarta.transaction.Transactional;

@Service
public class DailySuggestionService {

    @Autowired
    private ShoppedItemRepository shoppedItemRepository;
    @Autowired
    private SuggestedItemRepository suggestedItemRepository;

    @Scheduled(cron = "0 0 8 * * ?")
    // @Scheduled(fixedRate = 60000)
    @Transactional
    public void generateDailySuggestions() {

        suggestedItemRepository.deleteAllSuggestedItems();

        List<ShoppedItem> allItems = shoppedItemRepository.findAllWithOrderDate();

        Map<String, List<LocalDate>> itemPurchaseDates = getShoppedItemsWithDates(allItems);
        List<SuggestedItem> suggestedItems = getSuggestedItems(itemPurchaseDates);

        System.out.println("Today's Suggested Items:");

        suggestedItemRepository.saveAll(suggestedItems);

    }

    public Map<String, List<LocalDate>> getShoppedItemsWithDates(List<ShoppedItem> allItems) {
        Map<String, List<LocalDate>> itemPurchaseDates = new HashMap<>();
        for (ShoppedItem item : allItems) {
            String key = item.getGeneralName() != null ? item.getGeneralName().toLowerCase()
                    : item.getRawName().toLowerCase();
            itemPurchaseDates
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(item.getOrder().getOrderDate());
        }

        return itemPurchaseDates;
    }

    public List<SuggestedItem> getSuggestedItems(Map<String, List<LocalDate>> itemPurchaseDates) {
        LocalDate today = LocalDate.now();
        List<SuggestedItem> suggestedItems = new ArrayList<>();

        for (Map.Entry<String, List<LocalDate>> entry : itemPurchaseDates.entrySet()) {
            List<LocalDate> dates = entry.getValue().stream()
                    .distinct()
                    .sorted()
                    .toList();

            if (dates.size() < 2)
                continue;

            List<Long> intervals = new ArrayList<>();
            for (int i = 1; i < dates.size(); i++) {
                intervals.add(ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i)));
            }

            long avgInterval = (long) intervals.stream().mapToLong(Long::longValue).average().orElse(Long.MAX_VALUE);
            LocalDate lastPurchaseDate = dates.get(dates.size() - 1);
            long daysSinceLastPurchase = ChronoUnit.DAYS.between(lastPurchaseDate, today);
            long daysOverAvg = avgInterval - daysSinceLastPurchase;

            SuggestionType suggestionType = null;

            if (daysSinceLastPurchase > 45) {
                suggestionType = SuggestionType.CHECK_NOW;
            }

            if (daysSinceLastPurchase >= avgInterval * 2 || (daysOverAvg >= 4 && daysOverAvg <= 5)) {
                suggestionType = SuggestionType.LOW_STOCK;
            }
            if (daysSinceLastPurchase >= avgInterval) {
                suggestionType = SuggestionType.BUY_NOW;
            }
            if (suggestionType != null) {
                suggestedItems.add(new SuggestedItem(entry.getKey(), lastPurchaseDate, suggestionType));
            }
        }
        return suggestedItems;
    }
}