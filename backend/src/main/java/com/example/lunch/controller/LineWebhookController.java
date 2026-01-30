package com.example.lunch.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用於取得 LINE Group ID 的輔助 Controller
 * 當機器人收到訊息時，可以透過這個 endpoint 查看 webhook payload
 */
@RestController
@RequestMapping("/api/line")
public class LineWebhookController {

    /**
     * LINE Webhook endpoint
     * 用於接收 LINE 平台發送的事件
     * 
     * 使用方法：
     * 1. 在 LINE Developers Console 設定 Webhook URL:
     * https://your-domain.com/api/line/webhook
     * 2. 在群組中發送訊息給機器人
     * 3. 查看後端 log，找到 groupId
     */
    private static boolean hasLoggedGroupId = false;

    @PostMapping("/webhook")
    public String handleWebhook(@RequestBody Map<String, Object> payload) {
        if (hasLoggedGroupId) {
            return "OK";
        }

        System.out.println("=== LINE Webhook Received ===");
        System.out.println(payload);

        // 嘗試提取 groupId
        try {
            if (payload.containsKey("events")) {
                Object events = payload.get("events");
                if (events instanceof java.util.List) {
                    java.util.List<?> eventList = (java.util.List<?>) events;
                    if (!eventList.isEmpty() && eventList.get(0) instanceof Map) {
                        Map<?, ?> event = (Map<?, ?>) eventList.get(0);
                        if (event.containsKey("source")) {
                            Map<?, ?> source = (Map<?, ?>) event.get("source");
                            if (source.containsKey("groupId")) {
                                String groupId = source.get("groupId").toString();
                                System.out.println(">>> GROUP ID: " + groupId);
                                System.out.println(">>> 請將此 GROUP ID 設定到環境變數 LINE_GROUP_ID");
                                hasLoggedGroupId = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract groupId: " + e.getMessage());
        }

        return "OK";
    }

    /**
     * 驗證 webhook 的 endpoint
     */
    @GetMapping("/webhook")
    public String verifyWebhook() {
        return "LINE Webhook is ready";
    }
}
