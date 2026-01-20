package com.example.lunch.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningGroup {
    private String id;
    private String name;
    private String deadline;
    private String createdAt;
    private List<MenuItem> menu;
    private String restaurantName;
    private String menuImageUrl;
    private String note; // 菜單備註
    private String restaurantPhone; // 店家電話

    public DiningGroup(String name, String deadline) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.deadline = deadline;
        this.createdAt = java.time.LocalDateTime.now().toString();
    }
}
