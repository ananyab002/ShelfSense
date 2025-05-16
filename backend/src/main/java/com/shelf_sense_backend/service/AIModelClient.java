package com.shelf_sense_backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AIModelClient {

    private static final Logger logger = LoggerFactory.getLogger(AIModelClient.class);

    @Value("${lmstudio.api.url:http://localhost:1234/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIModelClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> processReceiptText(String receiptText) {
        if (apiUrl == null || apiUrl.isEmpty()) {
            logger.warn("AI model API URL not configured. Skipping AI processing.");
            return new ArrayList<>();
        }

        try {
            String prompt = buildPrompt(receiptText);
            Map<String, Object> requestBody = createRequestBody(prompt);
            HttpHeaders headers = createRequestHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            logger.debug("Sending request to AI model API: {}", apiUrl);
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            return parseAIResponse(response);

        } catch (Exception e) {
            logger.error("Error processing receipt with AI model: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String buildPrompt(String receiptText) {
        return "You are a specialized grocery receipt analyzer.\n\n" +
                "Your task is to extract all product items from the receipt text below and return only valid JSON in the same order as receipt and same number of items as in receipt that matches the following schema:\n\n"
                +
                "{\n" +
                "  \"products_list\": [\n" +
                "    {\n" +
                "      \"product_name\": \"string (exactly as on receipt)\",\n" +
                "      \"quantity\": \"number or string (e.g., 1 or '0.5')\",\n" +
                "      \"weight\": \"string (exactly as on receipt including the unit - e.g., '125 g', '1 stk', '1,1 kg', '350 g')\",\n"
                +
                "      \"general_name\": \"string (the basic product name without details - e.g., Bringebær → Raspberry, Tørkerull → Paper Towels)\",\n"
                +
                "      \"food_type\": \"one of the predefined categories listed below\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "For each product, extract and classify the following:\n" +
                "1. product_name – the exact name as printed on the receipt.\n" +
                "2. quantity – the total count or measurable quantity purchased (e.g., 1, 2, 0.5).\n" +
                "3. weight – extract the complete weight or volume specification including units as shown on receipt:\n"
                +
                "4. general_name – the basic name of the item (e.g., for 'Bringebær Marokko / Portugal' the general_name is 'Raspberry').\n"
                +
                "   The general_name should be simplified compared to product_name and MUST NOT be identical to food_type.\n"
                +
                "5. food_type – select the closest matching value from the list below:\n\n" +
                "Soft Fruits, Hard Fruits, Citrus Fruits, Berries,\n" +
                "Root Vegetables, Cruciferous Vegetables, Squash & Gourds, Tomatoes & Peppers,\n" +
                "Herbs, Proteins, Bakery & Bread, Frozen Foods, Grains, Pasta & Noodles,\n" +
                "Cooking Oils, Flour, Sugar, Baking Ingredients, Salt & Pepper, Nut Butters,\n" +
                "Condiments, Sauces, Dried Fruits, Nuts & Seeds, Snacks, Dairy, Legumes,\n" +
                "Whole Spices, Powdered Spices, Kitchen hygiene, Bathroom hygiene\n\n" +
                "Ensure:\n" +
                "- Output is a single JSON object.\n" +
                "- Look carefully at the receipt format to extract the correct weight/volume/unit information.\n" +
                "- Pay special attention to items that have weight specifications at the end of product descriptions.\n"
                +
                "- For general_name, provide the basic ingredient/product name, not the category.\n" +
                "- Do not include any extra explanation or commentary—just the raw JSON.\n\n" +
                "Receipt Text:\n" +
                receiptText;
    }

    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("temperature", 0.0);
        // requestBody.put("max_tokens", 300);
        requestBody.put("top_p", 0.1);
        requestBody.put("frequency_penalty", 0.0);
        requestBody.put("presence_penalty", 0.0);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        return requestBody;
    }

    private HttpHeaders createRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private List<Map<String, Object>> parseAIResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            String content = extractContentFromResponse(rootNode);

            if (content == null || content.isEmpty()) {
                logger.warn("Could not extract content from AI response");
                return new ArrayList<>();
            }
            String jsonContent = extractJsonFromMarkdown(content);
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode itemsNode = root.get("products_list");
            List<Map<String, Object>> items = new ArrayList<>();

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    Map<String, Object> item = objectMapper.convertValue(itemNode, Map.class);
                    items.add(item);
                }
            }

            return items;

        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String extractContentFromResponse(JsonNode rootNode) {
        // For OpenAI
        if (rootNode.has("choices") && rootNode.get("choices").isArray()) {
            JsonNode choices = rootNode.get("choices");
            if (choices.size() > 0) {
                JsonNode choice = choices.get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }
        }

        if (rootNode.has("content") && rootNode.get("content").isArray()) {
            JsonNode contents = rootNode.get("content");
            if (contents.size() > 0) {
                JsonNode contentObj = contents.get(0);
                if (contentObj.has("text")) {
                    return contentObj.get("text").asText();
                }
            }
        }
        if (rootNode.has("output") || rootNode.has("generated_text")) {
            return rootNode.has("output") ? rootNode.get("output").asText() : rootNode.get("generated_text").asText();
        }

        return null;
    }

    private String extractJsonFromMarkdown(String content) {
        if (content.contains("```json")) {
            content = content.substring(content.indexOf("```json") + 7);
            content = content.substring(0, content.indexOf("```"));
        } else if (content.contains("```")) {
            content = content.substring(content.indexOf("```") + 3);
            content = content.substring(0, content.indexOf("```"));
        }

        if ((content.trim().startsWith("[") && content.trim().endsWith("]")) ||
                (content.trim().startsWith("{") && content.trim().endsWith("}"))) {
            return content;
        }
        return "[]";
    }
}
