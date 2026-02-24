package com.example.lunch.service;

import com.example.lunch.model.Order;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.TextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.linecorp.bot.model.message.Message;

@Service
@ConditionalOnProperty(name = { "line.channel.access.token", "line.group.id" })
public class LineNotificationService {

    private final LineMessagingClient lineMessagingClient;
    private final String groupId;

    public LineNotificationService(
            @Value("${line.channel.access.token}") String channelAccessToken,
            @Value("${line.group.id}") String groupId) {
        this.lineMessagingClient = LineMessagingClient.builder(channelAccessToken).build();
        this.groupId = groupId;
    }

    /**
     * Helper to parse deadline string which might be ISO Zoned or Local
     */
    private ZonedDateTime parseDeadline(String deadline) {
        try {
            return ZonedDateTime.parse(deadline);
        } catch (Exception e) {
            // Fallback: Assume it's a local ISO format (e.g. from datetime-local input) and
            // treat as Taipei time
            try {
                return java.time.LocalDateTime.parse(deadline).atZone(ZoneId.of("Asia/Taipei"));
            } catch (Exception e2) {
                // Last resort, try appending current date if it's just time? No, assume format
                // is always full date time
                System.err.println("Failed to parse deadline: " + deadline);
                throw e2; // Rethrow to be caught by caller
            }
        }
    }

    /**
     * 發送結單前 5 分鐘提醒
     */
    public void sendDeadlineReminder(String groupName, String deadline, String groupId, String appUrl) {
        try {
            ZonedDateTime deadlineTime = parseDeadline(deadline);
            // 轉換為台北時區顯示
            String formattedTime = deadlineTime.withZoneSameInstant(ZoneId.of("Asia/Taipei"))
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String message = String.format(
                    "🔔 結單提醒\n" +
                            "還有幾分鐘就要結單囉！\n\n" +
                            "團購：%s\n" +
                            "結單時間：%s\n\n" +
                            "👉 %s/#/group/%s",
                    groupName, formattedTime, appUrl, groupId);

            sendMessage(message);
        } catch (Exception e) {
            System.err.println("Failed to send deadline reminder: " + e.getMessage());
        }
    }

    /**
     * 發送結單訂單摘要與統計（合併在一個 Request 發送，節省額度）
     */
    public void sendOrderSummaryAndStatistics(String groupName, String deadline, String restaurantPhone,
            List<Order> orders) {
        try {
            ZonedDateTime deadlineTime = parseDeadline(deadline);
            String formattedTime = deadlineTime.withZoneSameInstant(ZoneId.of("Asia/Taipei"))
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String orderSummary = formatOrders(orders);
            int totalAmount = orders.stream().mapToInt(Order::getTotalPrice).sum();

            String summaryMessage = String.format(
                    "📋 訂單摘要\n" +
                            "%s 結單\n\n" +
                            "%s\n" +
                            "總金額：$%d",
                    formattedTime, orderSummary, totalAmount);

            // 統計訊息
            Map<String, Long> stats = orders.stream()
                    .collect(Collectors.groupingBy(
                            this::getGroupingKey,
                            Collectors.summingLong(o -> o.getQuantity() != null ? o.getQuantity() : 1L)));

            StringBuilder sb = new StringBuilder();
            if (restaurantPhone != null && !restaurantPhone.trim().isEmpty()) {
                sb.append("店家電話：").append(restaurantPhone).append("\n");
            }

            stats.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        sb.append(entry.getKey()).append("*").append(entry.getValue()).append("\n");
                    });

            String statsMessage = sb.toString().trim();

            String combinedMessage = summaryMessage + "\n\n" + statsMessage;
            sendMessage(combinedMessage); // 兩個訊息合併在一個氣泡發出，只扣 1 倍額度
        } catch (Exception e) {
            System.err.println("Failed to send order summary and statistics: " + e.getMessage());
        }
    }

    /**
     * 格式化訂單：依品項+飯量分組
     * 格式：
     * 五香雞腿 飯少 $115
     * Far
     * 
     * 青蔥海鹽雞胸 飯少 $135
     * Renee, 小婕
     */
    private String formatOrders(List<Order> orders) {
        // 建立分組 key: "品項名稱 + 飯量 + 備註"
        Map<String, List<Order>> groupedOrders = orders.stream()
                .collect(Collectors.groupingBy(this::getGroupingKey));

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<Order>> entry : groupedOrders.entrySet()) {
            String itemKey = entry.getKey();
            List<Order> itemOrders = entry.getValue();

            // 取第一筆訂單的價格 (同品項組價格相同)
            int price = itemOrders.get(0).getBasePrice();

            // 收集並加總相同人名的數量
            Map<String, Integer> userQtyMap = new LinkedHashMap<>();
            for (Order order : itemOrders) {
                int qty = order.getQuantity() != null ? order.getQuantity() : 1;
                userQtyMap.merge(order.getUserName(), qty, Integer::sum);
            }

            // 格式化人名
            String userNames = userQtyMap.entrySet().stream()
                    .map(e -> e.getValue() > 1 ? e.getKey() + "*" + e.getValue() : e.getKey())
                    .collect(Collectors.joining(", "));

            // 格式：[品項 飯量 備註] $價格 人名, 人名
            sb.append(itemKey).append(" $").append(price).append(" ").append(userNames).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * 取得分組 Key (品項 + 飯量 + 備註)
     */
    private String getGroupingKey(Order order) {
        String riceLabel = getRiceLabel(order.getRiceLevel());
        String note = order.getNote() != null ? order.getNote().trim() : "";

        StringBuilder sb = new StringBuilder(order.getItemName());
        if (!riceLabel.isEmpty()) {
            sb.append(" ").append(riceLabel);
        }
        if (!note.isEmpty()) {
            sb.append(" ").append(note);
        }
        return sb.toString();
    }

    /**
     * 將飯量代碼轉換為顯示文字
     */
    private String getRiceLabel(String riceLevel) {
        if (riceLevel == null || riceLevel.equals("FULL")) {
            return "";
        }
        switch (riceLevel) {
            case "HALF":
                return "飯半";
            case "LESS":
                return "飯少";
            default:
                return "";
        }
    }

    /**
     * 發送訊息到 LINE 群組 (單一文字泡泡)
     */
    private void sendMessage(String message) {
        sendMessages(message);
    }

    /**
     * 發送多個訊息到 LINE 群組 (一次 Request 最多 5 個泡泡)
     * 這可以大幅減少 Push API 扣除的群組訊息額度
     */
    private void sendMessages(String... messages) {
        try {
            List<Message> lineMessages = Arrays.stream(messages)
                    .map(TextMessage::new)
                    .collect(Collectors.toList());

            PushMessage pushMessage = new PushMessage(groupId, lineMessages);
            lineMessagingClient.pushMessage(pushMessage).get();
            System.out.println("LINE message(s) sent successfully, count: " + messages.length);
        } catch (Exception e) {
            System.err.println("Failed to send LINE message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
