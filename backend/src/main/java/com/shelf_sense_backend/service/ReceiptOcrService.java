package com.shelf_sense_backend.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import com.shelf_sense_backend.model.Order;
import com.shelf_sense_backend.model.ShoppedItem;
import com.shelf_sense_backend.repo.OrderRepository;
import com.shelf_sense_backend.repo.ShoppedItemRepository;

@Service
public class ReceiptOcrService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptOcrService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShoppedItemRepository shoppedItemRepository;

    @Autowired
    private GroceryReceiptAnalyzer groceryReceiptAnalyzer;

    @Value("${google.cloud.credentials-location}")
    private String credentialsPath;

    public Long processReceipt(MultipartFile file) throws IOException {
        String extractedText = extractTextFromImage(file);

        Order order = createOrderFromReceipt(extractedText);

        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty() || order.getOrderDate() == null) {
            logger.error("Failed to extract order number or date from the receipt");
            throw new IOException("No order number found in the receipt");
        }

        List<ShoppedItem> items = groceryReceiptAnalyzer.parseReceipt(extractedText, order);

        Order savedOrder = orderRepository.save(order);

        for (ShoppedItem item : items) {
            shoppedItemRepository.save(item);
        }

        logger.info("Processed receipt with order number: {}, found {} items",
                order.getOrderNumber(), items.size());

        return savedOrder.getId();
    }

    private String extractTextFromImage(MultipartFile file) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(credentialsPath));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            logger.info("Connecting to Google Cloud Vision API");

            ByteString imgBytes = ByteString.copyFrom(file.getBytes());
            Image image = Image.newBuilder().setContent(imgBytes).build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));

            AnnotateImageResponse res = response.getResponses(0);
            if (res.hasError()) {
                logger.error("Error in text detection: {}", res.getError().getMessage());
                throw new IOException("Error in text detection: " + res.getError().getMessage());
            }
            if (res.getTextAnnotationsCount() > 0) {
                return res.getTextAnnotations(0).getDescription();
            } else {
                logger.warn("No text detected in the image");
                return "";
            }
        }
    }

    public Order createOrderFromReceipt(String text) {
        Order order = new Order();
        order.setOrderNumber(getOrderNumber(text));
        order.setOrderDate(extractDate(text));
        return order;
    }

    public String getOrderNumber(String text) {
        Pattern orderPattern = Pattern.compile("(?:Ordre\\s*nr|Bestillingsnummer|#)([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher orderMatcher = orderPattern.matcher(text);
        if (orderMatcher.find()) {
            return orderMatcher.group(1);
        } else {
            return null;
        }
    }

    public LocalDate getOrderDate(String text) {
        Pattern datePattern = Pattern.compile(
                "(?:Dato:|Date:)?\\s*(\\d{1,2})\\. (\\p{L}+)\\s+(\\d{4})\\s+(\\d{1,2}:\\d{2})",
                Pattern.CASE_INSENSITIVE);
        Matcher dateMatcher = datePattern.matcher(text);
        dateMatcher.reset();

        if (dateMatcher.find()) {
            String day = dateMatcher.group(1);
            String monthName = dateMatcher.group(2).toLowerCase();
            String year = dateMatcher.group(3);
            String time = dateMatcher.group(4);
            Map<String, Integer> monthMap = new HashMap<>();
            monthMap.put("januar", 1);
            monthMap.put("january", 1);
            monthMap.put("februar", 2);
            monthMap.put("february", 2);
            monthMap.put("mars", 3);
            monthMap.put("march", 3);
            monthMap.put("april", 4);
            monthMap.put("mai", 5);
            monthMap.put("may", 5);
            monthMap.put("juni", 6);
            monthMap.put("june", 6);
            monthMap.put("juli", 7);
            monthMap.put("july", 7);
            monthMap.put("august", 8);
            monthMap.put("september", 9);
            monthMap.put("oktober", 10);
            monthMap.put("october", 10);
            monthMap.put("november", 11);
            monthMap.put("desember", 12);
            monthMap.put("december", 12);

            Integer monthNum = monthMap.get(monthName);
            if (monthNum == null) {
                return null;
            }

            // Create the date string in the format we can parse
            String dateStr = String.format("%02d.%02d.%s %s",
                    Integer.parseInt(day), monthNum, year, time);

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                return dateTime.toLocalDate();
            } catch (DateTimeParseException e) {
                return null;
            }
        } else {
            Pattern altDatePattern = Pattern.compile( "(?:(\\p{L}+)[ ,]*)?(\\d{1,2})[-./](\\d{1,2})[-./](\\d{4})\\s+(\\d{1,2}:\\d{2}(?::\\d{2})?)",
                    Pattern.CASE_INSENSITIVE);
            Matcher altDateMatcher = altDatePattern.matcher(text);

            if (altDateMatcher.find()) {
                String dateStr = altDateMatcher.group(1).replace("/", ".") + " " + altDateMatcher.group(2);
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                    return dateTime.toLocalDate();
                } catch (DateTimeParseException e) {
                    return null;
                }
            } else {

                return null;
            }
        }
    }

    public LocalDate extractDate(String text) {
    // Normalize input
    text = text.toLowerCase()
               .replaceAll(",", "")
               .replaceAll("kl\\.?\\s*", "")
               .replaceAll("søndag|mandag|tirsdag|onsdag|torsdag|fredag|lørdag", "") // remove day names
               .trim();

    // Month mapping
    Map<String, Integer> monthMap = Map.ofEntries(
        Map.entry("januar", 1), Map.entry("februar", 2), Map.entry("mars", 3),
        Map.entry("april", 4), Map.entry("mai", 5), Map.entry("juni", 6),
        Map.entry("juli", 7), Map.entry("august", 8), Map.entry("september", 9),
        Map.entry("oktober", 10), Map.entry("november", 11), Map.entry("desember", 12),
        Map.entry("january", 1), Map.entry("february", 2), Map.entry("march", 3),
        Map.entry("may", 5), Map.entry("june", 6), Map.entry("july", 7),
        Map.entry("october", 10), Map.entry("december", 12)
    );

    // 1. Match dd.MM.yyyy with optional time
    Pattern numericDatePattern = Pattern.compile("(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})(?:\\s+(\\d{1,2}:\\d{2}))?");
    Matcher numericMatcher = numericDatePattern.matcher(text);
    if (numericMatcher.find()) {
        int day = Integer.parseInt(numericMatcher.group(1));
        int month = Integer.parseInt(numericMatcher.group(2));
        int year = Integer.parseInt(numericMatcher.group(3));
        return LocalDate.of(year, month, day);
    }

    // 2. Match dd. MMMM yyyy with optional time
    Pattern namedMonthPattern = Pattern.compile("(\\d{1,2})[.]?\\s*(\\p{L}+)[ ]+(\\d{4})");
    Matcher namedMatcher = namedMonthPattern.matcher(text);
    if (namedMatcher.find()) {
        int day = Integer.parseInt(namedMatcher.group(1));
        String monthName = namedMatcher.group(2);
        int year = Integer.parseInt(namedMatcher.group(3));
        Integer month = monthMap.get(monthName);
        if (month != null) {
            return LocalDate.of(year, month, day);
        }
    }

    // 3. Fallback: ISO-style yyyy-MM-dd
    Pattern isoPattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    Matcher isoMatcher = isoPattern.matcher(text);
    if (isoMatcher.find()) {
        int year = Integer.parseInt(isoMatcher.group(1));
        int month = Integer.parseInt(isoMatcher.group(2));
        int day = Integer.parseInt(isoMatcher.group(3));
        return LocalDate.of(year, month, day);
    }

    return null; // No matching date
}

}