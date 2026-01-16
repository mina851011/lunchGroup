package com.example.lunch.service;

import com.example.lunch.model.Order;
import com.example.lunch.repository.GoogleSheetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private GoogleSheetsRepository repository;

    private static final String RANGE_ORDERS = "Orders!A2:K";

    public Order addOrder(Order order) throws IOException {
        log.info("[ADD_ORDER] Starting - User: {}, Item: {}, GroupId: {}",
                order.getUserName(), order.getItemName(), order.getGroupId());

        try {
            // Pricing Logic
            int quantity = (order.getQuantity() == null || order.getQuantity() < 1) ? 1 : order.getQuantity();
            order.setQuantity(quantity);
            order.setTotalPrice(order.getBasePrice() * quantity);

            String id = UUID.randomUUID().toString();
            order.setId(id);
            order.setPaid(false); // 新訂單預設未收款

            // Use Taiwan timezone
            ZonedDateTime nowTaipei = ZonedDateTime.now(ZoneId.of("Asia/Taipei"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            order.setCreatedAt(nowTaipei.format(formatter));

            log.info("[ADD_ORDER] Order prepared - ID: {}, Total: ${}", id, order.getTotalPrice());

            // Full Rewrite Approach to ensure single TOTAL row and avoid intercalation
            log.info("[ADD_ORDER] Reading existing orders from Sheets...");
            List<List<Object>> allRows = repository.readData(RANGE_ORDERS);
            log.info("[ADD_ORDER] Read {} rows from Sheets", allRows == null ? 0 : allRows.size());

            List<List<Object>> realRows = new ArrayList<>();
            if (allRows != null) {
                for (List<Object> r : allRows) {
                    if (r.size() >= 1 && !"TOTAL".equals(r.get(0).toString())) {
                        realRows.add(r);
                    }
                }
            }
            log.info("[ADD_ORDER] Found {} existing real orders (excluding TOTAL)", realRows.size());

            // Add new order to realRows
            List<Object> row = new ArrayList<>();
            row.add(id);
            row.add(order.getGroupId());
            row.add(order.getUserName());
            row.add(order.getItemName());
            row.add(order.getBasePrice());
            row.add(order.getRiceLevel());
            row.add(order.getQuantity());
            row.add(order.getTotalPrice());
            row.add(order.getNote() == null ? "" : order.getNote());
            row.add(order.getCreatedAt());
            row.add("false"); // Paid column (K)
            realRows.add(row);
            log.info("[ADD_ORDER] Added new order to list. Total orders now: {}", realRows.size());

            // Calculate and add Total Row
            int totalSum = 0;
            int totalCount = 0;
            for (List<Object> r : realRows) {
                if (r.size() >= 8) {
                    try {
                        totalSum += Integer.parseInt(r.get(7).toString());
                        totalCount++;
                    } catch (Exception e) {
                        log.warn("[ADD_ORDER] Failed to parse total for row: {}", r.get(0));
                    }
                }
            }

            List<Object> totalRow = new ArrayList<>();
            totalRow.add("TOTAL");
            totalRow.add("");
            totalRow.add("---");
            totalRow.add("總計");
            totalRow.add("");
            totalRow.add("");
            totalRow.add(totalCount + " 份");
            totalRow.add(totalSum);
            totalRow.add(""); // Note column
            totalRow.add(""); // CreatedAt column
            totalRow.add(""); // Paid column
            realRows.add(totalRow);
            log.info("[ADD_ORDER] Calculated totals - Count: {}, Sum: ${}", totalCount, totalSum);

            log.info("[ADD_ORDER] Clearing existing data...");
            repository.clearData(RANGE_ORDERS);

            log.info("[ADD_ORDER] Writing {} rows back to Sheets...", realRows.size());
            repository.updateData(RANGE_ORDERS, realRows);

            log.info("[ADD_ORDER] SUCCESS - Order {} saved for user {}", id, order.getUserName());
            return order;

        } catch (Exception e) {
            log.error("[ADD_ORDER] FAILED - User: {}, Error: {}", order.getUserName(), e.getMessage(), e);
            throw e;
        }
    }

    private static final String RANGE_HISTORY_ORDERS = "History Orders!A2:K";

    public List<Order> getOrdersByGroup(String groupId) throws IOException {
        List<List<Object>> liveValues = repository.readData(RANGE_ORDERS);
        List<List<Object>> historyValues = repository.readData(RANGE_HISTORY_ORDERS);

        List<List<Object>> allValues = new ArrayList<>();
        if (liveValues != null)
            allValues.addAll(liveValues);
        if (historyValues != null)
            allValues.addAll(historyValues);

        if (allValues.isEmpty()) {
            return Collections.emptyList();
        }

        return allValues.stream()
                .filter(row -> row.size() >= 8 && !row.get(0).toString().equals("TOTAL")
                        && row.get(1).toString().equals(groupId))
                .map(row -> Order.builder()
                        .id(row.get(0).toString())
                        .groupId(row.get(1).toString())
                        .userName(row.get(2).toString())
                        .itemName(row.get(3).toString())
                        .basePrice(Integer.parseInt(row.get(4).toString()))
                        .riceLevel(row.get(5).toString())
                        .quantity(Integer.parseInt(row.get(6).toString()))
                        .totalPrice(Integer.parseInt(row.get(7).toString()))
                        .note(row.size() >= 9 ? row.get(8).toString() : "")
                        .createdAt(row.size() >= 10 ? row.get(9).toString() : "")
                        .paid(row.size() >= 11 && "true".equalsIgnoreCase(row.get(10).toString()))
                        .build())
                .collect(Collectors.toList());
    }

    public boolean deleteOrder(String groupId, String orderId) throws IOException {
        List<List<Object>> allRows = repository.readData(RANGE_ORDERS);
        if (allRows == null || allRows.isEmpty()) {
            return false;
        }

        List<List<Object>> remainingRows = new ArrayList<>();
        boolean found = false;

        for (List<Object> row : allRows) {
            if (row.size() >= 2 && row.get(0).toString().equals(orderId)
                    && row.get(1).toString().equals(groupId)) {
                found = true;
                continue; // Skip this row (delete it)
            }
            if (!"TOTAL".equals(row.get(0).toString())) {
                remainingRows.add(row);
            }
        }

        if (!found) {
            return false;
        }

        // Recalculate total
        int totalSum = 0;
        int totalCount = 0;
        for (List<Object> r : remainingRows) {
            if (r.size() >= 8) {
                try {
                    totalSum += Integer.parseInt(r.get(7).toString());
                    totalCount++;
                } catch (Exception e) {
                }
            }
        }

        // Add total row
        List<Object> totalRow = new ArrayList<>();
        totalRow.add("TOTAL");
        totalRow.add("");
        totalRow.add("---");
        totalRow.add("總計");
        totalRow.add("");
        totalRow.add("");
        totalRow.add(totalCount + " 份");
        totalRow.add(totalSum);
        totalRow.add("");
        totalRow.add("");
        totalRow.add("");
        remainingRows.add(totalRow);

        repository.clearData(RANGE_ORDERS);
        if (!remainingRows.isEmpty()) {
            repository.updateData(RANGE_ORDERS, remainingRows);
        }

        return true;
    }

    public boolean updatePaymentStatus(String groupId, String orderId, boolean paid) throws IOException {
        List<List<Object>> allRows = repository.readData(RANGE_ORDERS);
        if (allRows == null || allRows.isEmpty()) {
            return false;
        }

        boolean found = false;
        for (List<Object> row : allRows) {
            if (row.size() >= 2 && row.get(0).toString().equals(orderId)
                    && row.get(1).toString().equals(groupId)) {
                // Ensure row has 11 columns
                while (row.size() < 11) {
                    row.add("");
                }
                row.set(10, paid ? "true" : "false");
                found = true;
                break;
            }
        }

        if (found) {
            repository.clearData(RANGE_ORDERS);
            repository.updateData(RANGE_ORDERS, allRows);
        }

        return found;
    }
}
