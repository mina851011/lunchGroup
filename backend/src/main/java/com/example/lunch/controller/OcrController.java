package com.example.lunch.controller;

import com.example.lunch.model.MenuItem;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Cloudinary getCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryCloudName,
                "api_key", cloudinaryApiKey,
                "api_secret", cloudinaryApiSecret));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadOnly(@RequestParam("file") MultipartFile file) {
        try {
            Cloudinary cloudinary = getCloudinary();
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "lunch-menus",
                    "resource_type", "image"));

            String imageUrl = (String) uploadResult.get("secure_url");
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/menu")
    public ResponseEntity<?> parseMenu(@RequestParam("file") MultipartFile file) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("REPLACE")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error",
                            "API Key not configured. Please set GEMINI_API_KEY environment variable"));
        }

        try {
            // Upload to Cloudinary first
            Cloudinary cloudinary = getCloudinary();
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "lunch-menus",
                    "resource_type", "image"));

            String imageUrl = (String) uploadResult.get("secure_url");

            // Encode image to Base64 for Gemini
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

            // Prepare JSON payload for Gemini
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                    + geminiApiKey;

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            parts.add(Map.of("text",
                    "You are a menu parsing assistant. Analyze the provided image of a food menu. " +
                            "Extract all food items and their prices. " +
                            "Return ONLY a raw JSON array of objects with keys 'name' (string) and 'price' (integer). "
                            +
                            "Do not include markdown formatting (```json). " +
                            "Ignore section headers or non-food text."));

            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", file.getContentType() != null ? file.getContentType() : "image/jpeg",
                    "data", base64Image)));

            content.put("parts", parts);
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                text = text.replaceAll("```json", "").replaceAll("```", "").trim();

                MenuItem[] menuItems = objectMapper.readValue(text, MenuItem[].class);

                return ResponseEntity.ok(Map.of(
                        "items", menuItems,
                        "imageUrl", imageUrl));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Gemini API Error: " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getResponseBodyAsString()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process image: " + e.getMessage()));
        }
    }
}
