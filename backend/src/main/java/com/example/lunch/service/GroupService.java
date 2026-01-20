package com.example.lunch.service;

import com.example.lunch.model.DiningGroup;
import com.example.lunch.repository.GoogleSheetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GoogleSheetsRepository repository;

    private static final String RANGE_GROUPS = "Groups!A2:H";

    public DiningGroup createGroup(String name, String deadline, List<com.example.lunch.model.MenuItem> menu,
            String restaurantName, String menuImageUrl, String note, String restaurantPhone)
            throws IOException {
        // Archive old orders before starting a new group
        repository.archiveOrders();

        DiningGroup group = new DiningGroup(name, deadline);
        group.setMenu(menu);
        group.setRestaurantName(restaurantName);
        group.setMenuImageUrl(menuImageUrl);
        group.setNote(note);
        group.setRestaurantPhone(restaurantPhone);

        // Use ArrayList to allow nulls, though Frontend sends default
        List<Object> row = new ArrayList<>();
        row.add(group.getId());
        row.add(group.getName());
        row.add(group.getDeadline());
        row.add(group.getCreatedAt());
        row.add(group.getRestaurantName() != null ? group.getRestaurantName() : "");
        row.add(group.getMenuImageUrl() != null ? group.getMenuImageUrl() : "");
        row.add(group.getNote() != null ? group.getNote() : ""); // Note column (G)
        row.add(group.getRestaurantPhone() != null ? group.getRestaurantPhone() : ""); // Phone column (H)

        repository.appendData(RANGE_GROUPS, Collections.singletonList(row));

        if (menu != null && !menu.isEmpty()) {
            repository.saveMenu(group.getId(), menu);
        }

        return group;
    }

    public List<DiningGroup> getAllGroups() throws IOException {
        List<List<Object>> values = repository.readData(RANGE_GROUPS);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(row -> row.size() >= 4)
                .map(row -> DiningGroup.builder()
                        .id(row.get(0).toString())
                        .name(row.get(1).toString())
                        .deadline(row.get(2).toString())
                        .createdAt(row.get(3).toString())
                        .restaurantName(row.size() >= 5 ? row.get(4).toString() : null)
                        .menuImageUrl(row.size() >= 6 ? row.get(5).toString() : null)
                        .note(row.size() >= 7 ? row.get(6).toString() : null)
                        .restaurantPhone(row.size() >= 8 ? row.get(7).toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public DiningGroup getGroup(String id) throws IOException {
        DiningGroup group = getAllGroups().stream()
                .filter(g -> g.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (group != null) {
            group.setMenu(repository.getMenu(id));
        }
        return group;
    }

    public void updateDeadline(String groupId, String newDeadline) throws IOException {
        repository.updateGroupDeadline(groupId, newDeadline);
    }
}
