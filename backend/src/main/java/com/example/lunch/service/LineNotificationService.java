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
     * 發送結單前 5 分鐘提醒
     */
    public void sendDeadlineReminder(String groupName, String deadline, String groupId, String appUrl) {
        try {
            ZonedDateTime deadlineTime = ZonedDateTime.parse(deadline);
            // 轉換為台北時區顯示
            String formattedTime = deadlineTime.withZoneSameInstant(ZoneId.of("Asia/Taipei"))
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String message = String.format(
                    "🔔 結單提醒\n" +
                            "還有幾分鐘就要結單囉！\n\n" +
                            "團購：%s\n" +
                            "結單時間：%s\n\n" +
                            "👉 %s/group/%s",
                    groupName, formattedTime, appUrl, groupId);

            sendMessage(message);
        } catch (Exception e) {
            System.err.println("Failed to send deadline reminder: " + e.getMessage());
        }
    }

    /**
     * 發送結單訂單摘要
     */
    public void sendOrderSummary(String groupName, String deadline, List<Order> orders) {
        try {
            ZonedDateTime deadlineTime = ZonedDateTime.parse(deadline);
            // 轉換為台北時區顯示
            String formattedTime = deadlineTime.withZoneSameInstant(ZoneId.of("Asia/Taipei"))
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String orderSummary = formatOrders(orders);
            int totalAmount = orders.stream().mapToInt(Order::getTotalPrice).sum();

            String message = String.format(
                    "📋 訂單摘要\n" +
                            "%s 結單\n\n" +
                            "%s\n" +
                            "總金額：$%d",
                    formattedTime, orderSummary, totalAmount);

            sendMessage(message);
        } catch (Exception e) {
            System.err.println("Failed to send order summary: " + e.getMessage());
        }
    }

    /**
     * 發送訂單統計與店家資訊
     */
    public void sendOrderStatistics(String restaurantPhone, List<Order> orders) {
        try {
            // 統計各品項數量
            Map<String, Long> stats = orders.stream()
                    .collect(Collectors.groupingBy(order -> {
                        String riceLabel = getRiceLabel(order.getRiceLevel());
                        if (riceLabel.isEmpty()) {
                            return order.getItemName();
                        }
                        return order.getItemName() + " " + riceLabel;
                    }, Collectors.counting()));

            StringBuilder sb = new StringBuilder();

            // 1. 店家電話 (如果有)
            if (restaurantPhone != null && !restaurantPhone.trim().isEmpty()) {
                sb.append("店家電話：").append(restaurantPhone).append("\n");
            }

            // 2. 統計清單
            stats.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        sb.append(entry.getKey()).append("*").append(entry.getValue()).append("\n");
                    });

            sendMessage(sb.toString().trim());
            System.out.println("Line statistics message sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send order statistics: " + e.getMessage());
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
        // 建立分組 key: "品項名稱 + 飯量"
        Map<String, List<Order>> groupedOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> {
                    String riceLabel = getRiceLabel(order.getRiceLevel());
                    if (riceLabel.isEmpty()) {
                        return order.getItemName();
                    }
                    return order.getItemName() + " " + riceLabel;
                }));

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<Order>> entry : groupedOrders.entrySet()) {
            String itemKey = entry.getKey();
            List<Order> itemOrders = entry.getValue();

            // 取第一筆訂單的價格（同品項+飯量價格應該相同）
            int price = itemOrders.get(0).getBasePrice();

            // 收集所有人名
            String userNames = itemOrders.stream()
                    .map(Order::getUserName)
                    .collect(Collectors.joining(", "));

            sb.append(itemKey).append(" $").append(price).append("\n");
            sb.append(userNames).append("\n");
        }

        return sb.toString().trim();
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
     * 發送訊息到 LINE 群組
     */
    private void sendMessage(String message) {
        try {
            PushMessage pushMessage = new PushMessage(groupId, new TextMessage(message));
            lineMessagingClient.pushMessage(pushMessage).get();
            System.out.println("LINE message sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send LINE message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
