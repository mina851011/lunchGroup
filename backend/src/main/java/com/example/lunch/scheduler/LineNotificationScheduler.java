package com.example.lunch.scheduler;

import com.example.lunch.model.DiningGroup;
import com.example.lunch.model.Order;
import com.example.lunch.service.GroupService;
import com.example.lunch.service.LineNotificationService;
import com.example.lunch.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LineNotificationScheduler {

    @Autowired
    private GroupService groupService;

    @Autowired
    private OrderService orderService;

    @Autowired(required = false)
    private LineNotificationService lineNotificationService;

    @Value("${app.url:http://localhost:5173}")
    private String appUrl;

    // 追蹤已發送的通知，避免重複發送
    private Set<String> sentReminders = new HashSet<>();
    private Set<String> sentSummaries = new HashSet<>();

    /**
     * 每分鐘檢查一次是否需要發送通知
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒執行一次
    public void checkAndSendNotifications() {
        // 如果 LINE 服務未啟用，直接返回
        if (lineNotificationService == null) {
            return;
        }

        try {
            List<DiningGroup> groups = groupService.getAllGroups();
            if (groups.isEmpty()) {
                return;
            }

            // 只檢查最新的團購
            DiningGroup latestGroup = groups.get(groups.size() - 1);
            ZonedDateTime deadline;

            try {
                deadline = ZonedDateTime.parse(latestGroup.getDeadline());
            } catch (Exception e) {
                try {
                    deadline = java.time.LocalDateTime.parse(latestGroup.getDeadline())
                            .atZone(java.time.ZoneId.of("Asia/Taipei"));
                } catch (Exception e2) {
                    // 如果解析失敗，跳過
                    System.err.println("Failed to parse deadline: " + latestGroup.getDeadline());
                    return;
                }
            }

            String groupId = latestGroup.getId();

            ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of("Asia/Taipei"));
            long minutesUntilDeadline = ChronoUnit.MINUTES.between(now, deadline);

            System.out.println("=== LINE Notification Check ===");
            System.out.println("Group: " + latestGroup.getName());
            System.out.println("Deadline: " + deadline);
            System.out.println("Now: " + now);
            System.out.println("Minutes until deadline: " + minutesUntilDeadline);

            // 1. 檢查是否需要發送 5 分鐘提醒
            // 由於 ChronoUnit.MINUTES 會捨去秒數，例如 4 分 59 秒會變成 4
            // 所以我們檢查 4 到 5 之間，確保每分鐘執行的排程器一定會抓到一次
            if (minutesUntilDeadline >= 4 && minutesUntilDeadline <= 5) {
                System.out.println("5-minute reminder condition met");
                System.out.println("sentReminders contains groupId? " + sentReminders.contains(groupId));
                System.out.println("sentReminders set: " + sentReminders);
                if (!sentReminders.contains(groupId)) {
                    System.out.println("Sending 5-minute reminder...");
                    lineNotificationService.sendDeadlineReminder(
                            latestGroup.getName(),
                            latestGroup.getDeadline(),
                            groupId,
                            appUrl);
                    sentReminders.add(groupId);
                    System.out.println("✓ Sent 5-minute reminder for group: " + groupId);
                } else {
                    System.out.println("5-minute reminder already sent for group: " + groupId);
                }
            }

            // 2. 檢查是否需要發送結單摘要（結單時間已過，且在 1 分鐘內）
            if (now.isAfter(deadline) || now.equals(deadline)) {
                // 只發送最近 1 分鐘內結單的摘要，避免重新部署時重複發送舊團購
                long minutesSinceDeadline = ChronoUnit.MINUTES.between(deadline, now);
                if (minutesSinceDeadline <= 1) {
                    if (!sentSummaries.contains(groupId)) {
                        List<Order> orders = orderService.getOrdersByGroup(groupId);
                        if (!orders.isEmpty()) {
                            System.out.println("Sending order summary...");
                            lineNotificationService.sendOrderSummary(
                                    latestGroup.getName(),
                                    latestGroup.getDeadline(),
                                    orders);
                            sentSummaries.add(groupId);
                            System.out.println("✓ Sent order summary for group: " + groupId);

                            // 發送統計訊息
                            System.out.println("Sending order statistics...");
                            lineNotificationService.sendOrderStatistics(
                                    latestGroup.getRestaurantPhone(),
                                    orders);
                            System.out.println("✓ Sent order statistics for group: " + groupId);
                        } else {
                            System.out.println("No orders to send summary for group: " + groupId);
                        }
                    } else {
                        System.out.println("Order summary already sent for group: " + groupId);
                    }
                } else {
                    System.out.println("Deadline was more than 1 minutes ago, skipping summary");
                }
            }

            // 清理舊的追蹤記錄（保留最近 10 個團購的記錄）
            if (sentReminders.size() > 10) {
                sentReminders.clear();
            }
            if (sentSummaries.size() > 10) {
                sentSummaries.clear();
            }

        } catch (Exception e) {
            System.err.println("Error in notification scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 手動標記群組已發送摘要（用於靜默結單）
     */
    public void markGroupAsSummarySent(String groupId) {
        sentSummaries.add(groupId);
        System.out.println("Manually marked group as summary sent: " + groupId);
    }
}
