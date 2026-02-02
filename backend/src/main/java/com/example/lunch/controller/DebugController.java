package com.example.lunch.controller;

import com.example.lunch.repository.GoogleSheetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private GoogleSheetsRepository repository;

    @GetMapping("/restaurants")
    public Map<String, Object> debugRestaurants() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<List<Object>> rawData = repository.readData("Restaurants!A2:F");
            result.put("status", "success");
            result.put("rowCount", rawData != null ? rawData.size() : 0);
            result.put("rawData", rawData);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("stackTrace", e.getStackTrace());
        }
        return result;
    }
}
