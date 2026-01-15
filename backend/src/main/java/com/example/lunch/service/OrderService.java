package com.example.lunch.service;

import com.example.lunch.model.Order;
import com.example.lunch.repository.GoogleSheetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GoogleSheetsRepository repository;

    private static final String RANGE_ORDERS = "Orders!A2:J";

    public Order addOrder(Order order) throws IOException {
        // Pricing Logic
        int quantity = (order.getQuantity() == null || order.getQuantity() < 1) ? 1 : order.getQuantity();
        order.setQuantity(quantity);
        order.setTotalPrice(order.getBasePrice() * quantity);

        String id = UUID.randomUUID().toString();
        order.setId(id);

        // Use Taiwan timezone
        ZonedDateTime nowTaipei = ZonedDateTime.now(ZoneId.of("Asia/Taipei"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        order.setCreatedAt(nowTaipei.format(formatter));

        // Full Rewrite Approach to ensure single TOTAL row and avoid intercalation
        List<List<Object>> allRows = repository.readData(RANGE_ORDERS);
        List<List<Object>> realRows = new ArrayList<>();
        if (allRows != null) {
            for (List<Object> r : allRows) {
                if (r.size() >= 1 && !"TOTAL".equals(r.get(0).toString())) {
                    realRows.add(r);
                }
            }
        }

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
        realRows.add(row);

        // Calculate and add Total Row
        int totalSum = 0;
        int totalCount = 0;
        for (List<Object> r : realRows) {
            if (r.size() >= 8) {
                try {
                    totalSum += Integer.parseInt(r.get(7).toString());
                    totalCount++;
                } catch (Exception e) {
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
        realRows.add(totalRow);

        repository.clearData(RANGE_ORDERS);
        repository.updateData(RANGE_ORDERS, realRows);

        return order;
    }

    private static final String RANGE_HISTORY_ORDERS = "History Orders!A2:J";

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
                        .build())
                .collect(Collectors.toList());
    }
}
