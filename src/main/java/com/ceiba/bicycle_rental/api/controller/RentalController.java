package com.ceiba.bicycle_rental.api.controller;

import com.ceiba.bicycle_rental.application.dto.RentalResponse;
import com.ceiba.bicycle_rental.application.dto.StartRentalRequest;
import com.ceiba.bicycle_rental.application.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping
    public ResponseEntity<RentalResponse> startRental(@Valid @RequestBody StartRentalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rentalService.startRental(request));
    }

    @PutMapping("/{id}/finish")
    public ResponseEntity<RentalResponse> finishRental(@PathVariable Long id) {
        return ResponseEntity.ok(rentalService.finishRental(id));
    }
}
