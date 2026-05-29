package com.ceiba.bicycle_rental.api.controller;

import com.ceiba.bicycle_rental.application.dto.BicycleRequest;
import com.ceiba.bicycle_rental.application.dto.BicycleResponse;
import com.ceiba.bicycle_rental.application.dto.RentalResponse;
import com.ceiba.bicycle_rental.application.dto.UpdateBicycleStatusRequest;
import com.ceiba.bicycle_rental.application.service.BicycleService;
import com.ceiba.bicycle_rental.application.service.RentalService;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bikes")
public class BicycleController {

    private final BicycleService bicycleService;
    private final RentalService rentalService;

    public BicycleController(BicycleService bicycleService, RentalService rentalService) {
        this.bicycleService = bicycleService;
        this.rentalService = rentalService;
    }

    @PostMapping
    public ResponseEntity<BicycleResponse> register(@Valid @RequestBody BicycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bicycleService.register(request));
    }

    @GetMapping("/available")
    public ResponseEntity<List<BicycleResponse>> getAvailable(
            @RequestParam(required = false) BicycleType type) {
        return ResponseEntity.ok(bicycleService.getAvailable(type));
    }

    @GetMapping("/{code}")
    public ResponseEntity<BicycleResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(bicycleService.getByCode(code));
    }

    @GetMapping("/{code}/rentals")
    public ResponseEntity<List<RentalResponse>> getRentalHistory(@PathVariable String code) {
        return ResponseEntity.ok(rentalService.getRentalHistory(code));
    }

    @PutMapping("/{code}/status")
    public ResponseEntity<BicycleResponse> updateStatus(@PathVariable String code,
            @Valid @RequestBody UpdateBicycleStatusRequest request) {
        return ResponseEntity.ok(bicycleService.updateStatus(code, request));
    }
}
