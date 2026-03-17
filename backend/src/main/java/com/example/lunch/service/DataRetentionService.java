package com.example.lunch.service;

import com.example.lunch.config.RegionContext;
import com.example.lunch.repository.GoogleSheetsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DataRetentionService {

    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter LEGACY_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int QUANTITY_COLUMN_INDEX = 6;
    private static final int TOTAL_PRICE_COLUMN_INDEX = 7;

    private final GoogleSheetsRepository repository;

    @Value("${data.retention.days:10}")
    private int retentionDays;

    public DataRetentionService(GoogleSheetsRepository repository) {
        this.repository = repository;
    }

    public void cleanupAllRegions() {
        cleanupRegion("taichung");
        cleanupRegion("taipei");
    }

    public void cleanupRegion(String region) {
        ZonedDateTime cutoff = ZonedDateTime.now(TAIPEI_ZONE).minusDays(retentionDays);
        RegionContext.set(region);
        try {
            log.info("[RETENTION] Start cleanup for region={}, retentionDays={}, cutoff={}",
                    region, retentionDays, cutoff);

            List<List<Object>> groups = safeRows(repository.readData("Groups!A2:I"));
            List<List<Object>> keptGroups = keepRowsAfter(groups, 3, cutoff);
            rewriteRange("Groups!A2:I", keptGroups);

            Set<String> aliveGroupIds = new HashSet<>();
            for (List<Object> row : keptGroups) {
                if (!row.isEmpty() && row.get(0) != null) {
                    aliveGroupIds.add(row.get(0).toString());
                }
            }

            List<List<Object>> menus = safeRows(repository.readData("Menus!A2:C"));
            List<List<Object>> keptMenus = new ArrayList<>();
            for (List<Object> row : menus) {
                if (!row.isEmpty() && aliveGroupIds.contains(row.get(0).toString())) {
                    keptMenus.add(row);
                }
            }
            rewriteRange("Menus!A2:C", keptMenus);

            List<List<Object>> orders = safeRows(repository.readData("Orders!A2:K"));
            List<List<Object>> keptOrders = keepRowsAfter(skipTotalRows(orders), 9, cutoff);
            rewriteRange("Orders!A2:K", addTotalRow(keptOrders));

            List<List<Object>> historyOrders = safeRows(repository.readData("History Orders!A2:K"));
            List<List<Object>> keptHistory = keepRowsAfter(skipTotalRows(historyOrders), 9, cutoff);
            rewriteRange("History Orders!A2:K", keptHistory);

            log.info("[RETENTION] Cleanup done for region={} (groups={}, menus={}, orders={}, history={})",
                    region, keptGroups.size(), keptMenus.size(), keptOrders.size(), keptHistory.size());
        } catch (Exception e) {
            log.error("[RETENTION] Cleanup failed for region={}: {}", region, e.getMessage(), e);
        } finally {
            RegionContext.clear();
        }
    }

    private List<List<Object>> safeRows(List<List<Object>> rows) {
        return rows == null ? new ArrayList<>() : rows;
    }

    private List<List<Object>> skipTotalRows(List<List<Object>> rows) {
        List<List<Object>> output = new ArrayList<>();
        for (List<Object> row : rows) {
            if (!row.isEmpty() && "TOTAL".equals(String.valueOf(row.get(0)))) {
                continue;
            }
            output.add(row);
        }
        return output;
    }

    private List<List<Object>> keepRowsAfter(List<List<Object>> rows, int dateColumnIndex, ZonedDateTime cutoff) {
        List<List<Object>> output = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() <= dateColumnIndex || row.get(dateColumnIndex) == null) {
                // If date is missing, keep data to avoid accidental deletion.
                output.add(row);
                continue;
            }

            ZonedDateTime date = parseDate(row.get(dateColumnIndex).toString().trim());
            if (date == null || !date.isBefore(cutoff)) {
                output.add(row);
            }
        }
        return output;
    }

    private ZonedDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ZonedDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value).atZone(TAIPEI_ZONE);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, LEGACY_DATETIME).atZone(TAIPEI_ZONE);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(value).atStartOfDay(TAIPEI_ZONE);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private List<List<Object>> addTotalRow(List<List<Object>> orders) {
        if (orders.isEmpty()) {
            return orders;
        }

        int totalSum = 0;
        int totalCount = 0;
        for (List<Object> row : orders) {
            if (row.size() > TOTAL_PRICE_COLUMN_INDEX) {
                try {
                    totalSum += Integer.parseInt(String.valueOf(row.get(TOTAL_PRICE_COLUMN_INDEX)));
                    totalCount += parseQuantity(row);
                } catch (Exception ignored) {
                }
            }
        }

        List<List<Object>> output = new ArrayList<>(orders);
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
        output.add(totalRow);
        return output;
    }

    private void rewriteRange(String range, List<List<Object>> rows) throws IOException {
        repository.clearData(range);
        if (!rows.isEmpty()) {
            repository.updateData(range, rows);
        }
    }

    private int parseQuantity(List<Object> row) {
        if (row.size() <= QUANTITY_COLUMN_INDEX) {
            return 1;
        }

        try {
            return Integer.parseInt(String.valueOf(row.get(QUANTITY_COLUMN_INDEX)));
        } catch (Exception e) {
            return 1;
        }
    }
}
