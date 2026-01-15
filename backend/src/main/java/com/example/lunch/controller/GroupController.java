package com.example.lunch.controller;

import com.example.lunch.model.DiningGroup;
import com.example.lunch.model.Order;
import com.example.lunch.service.GroupService;
import com.example.lunch.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.lunch.model.MenuItem;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<DiningGroup> createGroup(@RequestBody Map<String, Object> payload) throws IOException {
        String name = (String) payload.get("name");
        String deadline = (String) payload.get("deadline");
        String restaurantName = (String) payload.get("restaurantName");
        String menuImageUrl = (String) payload.get("menuImageUrl");

        List<MenuItem> menu = new ArrayList<>();
        if (payload.containsKey("menu")) {
            List<Map<String, Object>> rawMenu = (List<Map<String, Object>>) payload.get("menu");
            if (rawMenu != null) {
                for (Map<String, Object> item : rawMenu) {
                    menu.add(new MenuItem(
                            (String) item.get("name"),
                            ((Number) item.get("price")).intValue()));
                }
            }
        }
        return ResponseEntity.ok(groupService.createGroup(name, deadline, menu, restaurantName, menuImageUrl));
    }

    @PatchMapping("/{id}/deadline")
    public ResponseEntity<?> updateDeadline(@PathVariable String id, @RequestBody Map<String, String> payload) {
        try {
            String newDeadline = payload.get("deadline");
            groupService.updateDeadline(id, newDeadline);
            return ResponseEntity.ok(Map.of("message", "Deadline updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update deadline: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<DiningGroup>> getHistory() throws IOException {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable String id) throws IOException {
        DiningGroup group = groupService.getGroup(id);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        List<Order> orders = orderService.getOrdersByGroup(id);
        return ResponseEntity.ok(Map.of("group", group, "orders", orders));
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<Order> addOrder(@PathVariable String id, @RequestBody Order order) throws IOException {
        DiningGroup group = groupService.getGroup(id);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        order.setGroupId(id);
        return ResponseEntity.ok(orderService.addOrder(order));
    }
}
