package com.example.lunch.repository;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.lunch.model.DiningGroup;
import com.example.lunch.model.MenuItem;
import com.example.lunch.model.Order;
import com.example.lunch.model.Restaurant;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class GoogleSheetsRepository {

    @Value("${google.sheets.application-name:LunchOrderingSystem}")
    private String applicationName;

    @Value("${google.sheets.credentials-path:src/main/resources/credentials.json}")
    private String credentialsPath;

    @Value("${google.sheets.spreadsheet-id:}")
    private String spreadsheetId;

    @Value("${google.sheets.credentials-json:}")
    private String credentialsJson;

    private Sheets sheetsService;
    private boolean isMockMode = false;
    private List<List<Object>> mockData = new ArrayList<>();

    private final List<DiningGroup> mockGroups = new ArrayList<>();
    private final List<Order> mockOrders = new ArrayList<>();
    private final List<MenuItem> mockMenu = new ArrayList<>(); // Legacy single menu
    private final List<Restaurant> mockRestaurants = new ArrayList<>(); // Supports multiple saved stores

    @PostConstruct
    public void init() throws IOException, GeneralSecurityException {
        // Try Environment Variable JSON first
        if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
            try {
                log.info("Initializing Google Sheets service with credentials-json (Env Var)");
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

                initializeSheetsService(credentials);
                return;
            } catch (Exception e) {
                log.error("Failed to load credentials from JSON: {}", e.getMessage());
            }
        }

        // Fallback to File path
        File textFile = new File(credentialsPath);
        if (!textFile.exists()) {
            log.warn("credentials.json not found at {}. Running in MOCK MODE.", credentialsPath);
            isMockMode = true;
            return;
        }

        try {
            log.info("Initializing Google Sheets service with credentials-path: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(textFile))
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            initializeSheetsService(credentials);
        } catch (Exception e) {
            log.error("Failed to initialize Google Sheets service from file: {}", e.getMessage());
            isMockMode = true;
        }
    }

    private void initializeSheetsService(GoogleCredentials credentials) throws GeneralSecurityException, IOException {
        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }

    public List<List<Object>> readData(String range) throws IOException {
        if (isMockMode) {
            if (range.contains("Orders")) {
                return new ArrayList<>(mockOrdersData);
            } else if (range.contains("Restaurants")) {
                // Restaurants handled by getAllRestaurants/saveRestaurant separately,
                // but if generic read needed:
                return new ArrayList<>();
            } else {
                return new ArrayList<>(mockData); // Default to Groups
            }
        }
        return sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
                .getValues();
    }

    public void appendData(String range, List<List<Object>> values) throws IOException {
        if (isMockMode) {
            if (range.contains("Orders")) {
                mockOrdersData.addAll(values);
            } else if (range.contains("Restaurants")) {
                // Handled in saveRestaurant
            } else {
                mockData.addAll(values); // Groups
            }
            System.out.println("MOCK: Appended data to " + range + ": " + values);
            return;
        }
        var body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(values);
        sheetsService.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public void updateData(String range, List<List<Object>> values) throws IOException {
        if (isMockMode) {
            log.info("MOCK: Updated {} with {} rows", range, values.size());
            return;
        }
        var body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(values);
        sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public void clearData(String range) throws IOException {
        if (isMockMode) {
            if (range.contains("Orders"))
                mockOrdersData.clear();
            else
                mockData.clear();
            return;
        }
        sheetsService.spreadsheets().values()
                .clear(spreadsheetId, range, new com.google.api.services.sheets.v4.model.ClearValuesRequest())
                .execute();
    }

    public void archiveOrders() throws IOException {
        String sourceRange = "Orders!A2:K";
        List<List<Object>> allRows = readData(sourceRange);

        if (allRows != null && !allRows.isEmpty()) {
            // Filter: Only archive real orders (no TOTAL rows)
            List<List<Object>> realOrders = allRows.stream()
                    .filter(row -> row.size() >= 1 && !"TOTAL".equals(row.get(0).toString()))
                    .collect(Collectors.toList());

            if (!realOrders.isEmpty()) {
                if (isMockMode) {
                    mockHistoryOrdersData.addAll(realOrders);
                } else {
                    appendData("History Orders!A:A", realOrders);
                }
            }
            // Clear the whole live orders sheet
            clearData(sourceRange);
            log.info("Archived {} real orders to History Orders", realOrders.size());
        }
    }

    private List<List<Object>> mockHistoryOrdersData = new ArrayList<>();

    // Separate storage for generic data
    private List<List<Object>> mockOrdersData = new ArrayList<>();

    // Menu Support
    private List<List<Object>> mockMenuData = new ArrayList<>(); // [GroupId, Name, Price]

    public void saveMenu(String groupId, List<com.example.lunch.model.MenuItem> menu) throws IOException {
        if (menu == null || menu.isEmpty())
            return;

        List<List<Object>> rows = new ArrayList<>();
        for (com.example.lunch.model.MenuItem item : menu) {
            rows.add(List.of(groupId, item.getName(), item.getPrice()));
        }

        if (isMockMode) {
            mockMenuData.addAll(rows);
            System.out.println("MOCK: Saved menu for group " + groupId);
            return;
        }

        // Real Sheets Implementation
        try {
            var body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(rows);
            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, "Menus!A:C", body) // Assuming 'Menus' sheet exists
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (IOException e) {
            System.err.println("Failed to save menu to Sheets: " + e.getMessage());
            // Don't re-throw to avoid breaking the group creation flow?
            // Better to log and continue, or throw? GroupService catches IO, so throw is
            // fine.
            throw e;
        }
    }

    public List<com.example.lunch.model.MenuItem> getMenu(String groupId) throws IOException {
        List<com.example.lunch.model.MenuItem> menu = new ArrayList<>();

        List<List<Object>> allRows;
        if (isMockMode) {
            allRows = mockMenuData;
        } else {
            // Real Sheets Implementation
            try {
                com.google.api.services.sheets.v4.model.ValueRange response = sheetsService.spreadsheets().values()
                        .get(spreadsheetId, "Menus!A2:C")
                        .execute();
                allRows = response.getValues();
                if (allRows == null) {
                    allRows = Collections.emptyList();
                }
            } catch (IOException e) {
                System.err.println("Failed to load menu from Sheets: " + e.getMessage());
                allRows = Collections.emptyList();
            }
        }

        for (List<Object> row : allRows) {
            if (row.size() >= 3 && row.get(0).toString().equals(groupId)) {
                menu.add(new com.example.lunch.model.MenuItem(
                        row.get(1).toString(),
                        Integer.parseInt(row.get(2).toString())));
            }
        }
        return menu;
    }

    private final ObjectMapper objectMapper = new ObjectMapper(); // Fix broken structure

    public void saveRestaurant(Restaurant restaurant) throws IOException {
        if (restaurant.getId() == null) {
            restaurant.setId(UUID.randomUUID().toString());
        }

        if (isMockMode) {
            mockRestaurants.removeIf(r -> r.getId().equals(restaurant.getId()));
            mockRestaurants.add(restaurant);
            System.out.println("MOCK: Upserted restaurant " + restaurant.getName());
            return;
        }

        // Real Sheets Implementation with Upsert
        List<List<Object>> allRows = readData("Restaurants!A:D");
        int rowIndex = -1;
        if (allRows != null) {
            for (int i = 0; i < allRows.size(); i++) {
                List<Object> row = allRows.get(i);
                if (row.size() >= 1 && row.get(0).toString().equals(restaurant.getId())) {
                    rowIndex = i + 1; // 1-indexed for Sheets
                    break;
                }
            }
        }

        String menuJson = objectMapper.writeValueAsString(restaurant.getMenu());
        List<Object> rowData = new ArrayList<>();
        rowData.add(restaurant.getId());
        rowData.add(restaurant.getName());
        rowData.add(menuJson);
        rowData.add(restaurant.getMenuImageUrl() != null ? restaurant.getMenuImageUrl() : "");

        if (rowIndex != -1) {
            // Update existing row
            String updateRange = "Restaurants!A" + rowIndex + ":D" + rowIndex;
            var body = new com.google.api.services.sheets.v4.model.ValueRange()
                    .setValues(Collections.singletonList(rowData));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, updateRange, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            log.info("Updated restaurant: {} at row {}", restaurant.getName(), rowIndex);
        } else {
            // Append new row
            appendData("Restaurants!A:D", Collections.singletonList(rowData));
            log.info("Appended new restaurant: {}", restaurant.getName());
        }
    }

    public List<Restaurant> getAllRestaurants() {
        if (isMockMode) {
            return new ArrayList<>(mockRestaurants);
        }

        List<Restaurant> results = new ArrayList<>();
        try {
            // Read from A2:D since user confirmed headers are in the first row
            List<List<Object>> rows = readData("Restaurants!A2:D");
            if (rows != null) {
                for (List<Object> row : rows) {
                    if (row.size() >= 3) {
                        try {
                            String id = row.get(0).toString();
                            String name = row.get(1).toString();
                            String menuJson = row.get(2).toString();
                            String menuImageUrl = row.size() >= 4 ? row.get(3).toString() : null;

                            // Skip if strictly header logic
                            if (menuJson.equalsIgnoreCase("MenuJSON") || menuJson.equalsIgnoreCase("Menu"))
                                continue;

                            List<MenuItem> menu = objectMapper.readValue(menuJson, new TypeReference<List<MenuItem>>() {
                            });
                            results.add(new Restaurant(id, name, menu, menuImageUrl));
                        } catch (Exception e) {
                            // Ignore malformed rows (e.g. headers)
                            System.err.println("Skipping invalid restaurant row: " + row + " Error: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load restaurants: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public void updateGroupDeadline(String groupId, String newDeadline) throws IOException {
        if (isMockMode) {
            log.info("MOCK: Updated deadline for group {} to {}", groupId, newDeadline);
            return;
        }
        log.info("Attempting to update deadline for group: {}", groupId);
        List<List<Object>> allRows = readData("Groups!A:A");
        int rowIndex = -1;
        if (allRows != null) {
            log.info("Searching through {} rows in Groups!A:A", allRows.size());
            for (int i = 0; i < allRows.size(); i++) {
                if (allRows.get(i).size() >= 1) {
                    String currentId = allRows.get(i).get(0).toString().trim();
                    if (currentId.equals(groupId)) {
                        rowIndex = i + 1;
                        break;
                    }
                }
            }
        }

        if (rowIndex != -1) {
            log.info("Found group {} at row {}. Updating to {}", groupId, rowIndex, newDeadline);
            String range = "Groups!C" + rowIndex;
            var body = new com.google.api.services.sheets.v4.model.ValueRange()
                    .setValues(Collections.singletonList(Collections.singletonList(newDeadline)));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } else {
            log.warn("Group ID {} not found in column A", groupId);
            throw new IOException("Group not found with ID: " + groupId);
        }
    }
}
