package com.example.lunch.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String id;
    private String groupId;
    private String userName;
    private String itemName;
    private Integer basePrice;
    private String riceLevel; // FULL, HALF, LESS
    private Integer quantity;
    private Integer totalPrice;
    private String note;
    private String createdAt;
    private Boolean paid; // 是否已收款
}
