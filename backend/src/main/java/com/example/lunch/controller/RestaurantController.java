package com.example.lunch.controller;

import com.example.lunch.model.Restaurant;
import com.example.lunch.repository.GoogleSheetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    @Autowired
    private GoogleSheetsRepository repository;

    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        return ResponseEntity.ok(repository.getAllRestaurants());
    }

    @PostMapping
    public ResponseEntity<Restaurant> createRestaurant(@RequestBody Restaurant restaurant) throws java.io.IOException {
        repository.saveRestaurant(restaurant);
        return ResponseEntity.ok(restaurant);
    }
}
