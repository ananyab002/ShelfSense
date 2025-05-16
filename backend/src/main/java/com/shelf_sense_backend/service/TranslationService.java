package com.shelf_sense_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String endpoint = "https://translation.googleapis.com/language/translate/v2";
    
    public TranslationService(@Value("${google.translate.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Translates text from the source language to the target language
     * 
     * @param text Text to translate
     * @param sourceLanguage Source language code (e.g., "no" for Norwegian)
     * @param targetLanguage Target language code (e.g., "en" for English)
     * @return Translated text or original text if translation fails
     */
    public String translateText(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.isEmpty()) {
            log.warn("Empty text provided for translation. Returning empty string.");
            return "";
        }
        
        try {
            log.debug("Translating text from {} to {}, text length: {}", 
                     sourceLanguage, targetLanguage, text.length());
            
            // Build the URL with query parameters
            String url = UriComponentsBuilder.fromHttpUrl(endpoint)
                    .queryParam("key", apiKey)
                    .toUriString();

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request body
            Map<String, Object> body = new HashMap<>();
            body.put("q", text);
            body.put("source", sourceLanguage);
            body.put("target", targetLanguage);

            // Create the request entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Send request and get response
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            // Extract the translated text from response
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data.containsKey("translations")) {
                    // Fix: Handle translations as List instead of Object array
                    List<Map<String, Object>> translations = (List<Map<String, Object>>) data.get("translations");
                    if (!translations.isEmpty()) {
                        Map<String, Object> translation = translations.get(0);
                        String translatedText = (String) translation.get("translatedText");
                        log.debug("Translation successful");
                        return translatedText;
                    }
                }
            }
            
            log.error("Translation response format unexpected: {}", response);
            return text; // Return original text as fallback
            
        } catch (Exception e) {
            log.error("Translation error: {}", e.getMessage(), e);
            return text; // Return original text on error
        }
    }
    
    /**
     * Translates text with auto-detection of source language
     * 
     * @param text Text to translate
     * @param targetLanguage Target language code
     * @return Translated text or original text if translation fails
     */
    public String translateText(String text, String targetLanguage) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        try {
            // Build the URL with query parameters
            String url = UriComponentsBuilder.fromHttpUrl(endpoint)
                    .queryParam("key", apiKey)
                    .toUriString();

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request body (without source language for auto-detection)
            Map<String, Object> body = new HashMap<>();
            body.put("q", text);
            body.put("target", targetLanguage);

            // Create the request entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Send the request and get response
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            // Extract the translated text from response
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data.containsKey("translations")) {
                    // Fix: Handle translations as List instead of Object array
                    List<Map<String, Object>> translations = (List<Map<String, Object>>) data.get("translations");
                    if (!translations.isEmpty()) {
                        Map<String, Object> translation = translations.get(0);
                        return (String) translation.get("translatedText");
                    }
                }
            }
            
            return text; // Return original text as fallback
            
        } catch (Exception e) {
            log.error("Translation error: {}", e.getMessage(), e);
            return text; // Return original text on error
        }
    }

}