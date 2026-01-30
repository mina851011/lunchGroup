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
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime deadline;

            try {
                deadline = ZonedDateTime.parse(latestGroup.getDeadline());
            } catch (Exception e) {
                // 如果解析失敗，跳過
                return;
            }

            // 檢查是否已過期
            if (now.isAfter(deadline)) {
                return;
            }

            String groupId = latestGroup.getId();

            // 1. 檢查是否需要發送 5 分鐘提醒
            long minutesUntilDeadline = ChronoUnit.MINUTES.between(now, deadline);
            if (minutesUntilDeadline <= 5 && minutesUntilDeadline >= 0) {
                if (!sentReminders.contains(groupId)) {
                    lineNotificationService.sendDeadlineReminder(
                            latestGroup.getName(),
                            latestGroup.getDeadline(),
                            groupId,
                            appUrl);
                    sentReminders.add(groupId);
                    System.out.println("Sent 5-minute reminder for group: " + groupId);
                }
            }

            // 2. 檢查是否需要發送結單摘要（結單時間到了）
            if (now.isAfter(deadline) || minutesUntilDeadline == 0) {
                if (!sentSummaries.contains(groupId)) {
                    List<Order> orders = orderService.getOrdersByGroup(groupId);
                    if (!orders.isEmpty()) {
                        lineNotificationService.sendOrderSummary(
                                latestGroup.getName(),
                                latestGroup.getDeadline(),
                                orders);
                        sentSummaries.add(groupId);
                        System.out.println("Sent order summary for group: " + groupId);
                    }
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
}
