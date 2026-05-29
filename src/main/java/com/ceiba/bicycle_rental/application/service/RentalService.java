package com.ceiba.bicycle_rental.application.service;

import com.ceiba.bicycle_rental.application.dto.RentalResponse;
import com.ceiba.bicycle_rental.application.dto.StartRentalRequest;
import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import com.ceiba.bicycle_rental.domain.model.Rental;
import com.ceiba.bicycle_rental.domain.repository.BicycleRepository;
import com.ceiba.bicycle_rental.domain.repository.RentalRepository;
import com.ceiba.bicycle_rental.infrastructure.exception.BusinessException;
import com.ceiba.bicycle_rental.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RentalService {

    private final BicycleRepository bicycleRepository;
    private final RentalRepository rentalRepository;
    private final Clock clock;

    public RentalService(BicycleRepository bicycleRepository,
                         RentalRepository rentalRepository,
                         Clock clock) {
        this.bicycleRepository = bicycleRepository;
        this.rentalRepository = rentalRepository;
        this.clock = clock;
    }

    @Transactional
    public RentalResponse startRental(StartRentalRequest request) {
        Bicycle bicycle = bicycleRepository.findByCode(request.getBicycleCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bicicleta no encontrada con codigo: " + request.getBicycleCode()));

        if (!bicycle.isAvailable()) {
            throw new BusinessException(
                    "La bicicleta " + request.getBicycleCode()
                    + " no esta disponible para alquiler. Estado actual: " + bicycle.getStatus());
        }

        bicycle.setStatus(BicycleStatus.ALQUILADA);
        bicycleRepository.save(bicycle);

        Rental rental = new Rental(bicycle, request.getClientName(),
                LocalDateTime.now(clock), request.getEstimatedHours());
        return new RentalResponse(rentalRepository.save(rental));
    }

    @Transactional
    public RentalResponse finishRental(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alquiler no encontrado con id: " + rentalId));

        if (rental.isFinished()) {
            throw new BusinessException("El alquiler con id " + rentalId + " ya fue finalizado");
        }

        LocalDateTime returnTime = LocalDateTime.now(clock);
        rental.setEndTime(returnTime);

        int hourlyRate = rental.getBicycle().getType().getHourlyRate();

        long actualMinutes = ChronoUnit.MINUTES.between(rental.getStartTime(), returnTime);
        long actualHours = (long) Math.ceil(actualMinutes / 60.0);

        BigDecimal baseCost = BigDecimal.valueOf(actualHours)
                .multiply(BigDecimal.valueOf(hourlyRate));

        BigDecimal penalty = calculatePenalty(rental.getStartTime(), rental.getEstimatedHours(),
                returnTime, hourlyRate);

        rental.setBaseCost(baseCost);
        rental.setPenalty(penalty);
        rental.setTotalCost(baseCost.add(penalty));
        rental.setFinished(true);

        rental.getBicycle().setStatus(BicycleStatus.DISPONIBLE);
        bicycleRepository.save(rental.getBicycle());

        return new RentalResponse(rentalRepository.save(rental));
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getRentalHistory(String bicycleCode) {
        if (!bicycleRepository.existsByCode(bicycleCode)) {
            throw new ResourceNotFoundException("Bicicleta no encontrada con codigo: " + bicycleCode);
        }
        return rentalRepository.findByBicycleCodeOrderByStartTimeDesc(bicycleCode)
                .stream()
                .map(RentalResponse::new)
                .collect(Collectors.toList());
    }

    private BigDecimal calculatePenalty(LocalDateTime startTime, int estimatedHours,
                                        LocalDateTime returnTime, int hourlyRate) {
        LocalDateTime estimatedEnd = startTime.plusHours(estimatedHours);
        if (!returnTime.isAfter(estimatedEnd)) {
            return BigDecimal.ZERO;
        }
        long delayMinutes = ChronoUnit.MINUTES.between(estimatedEnd, returnTime);
        long delayHours = (long) Math.ceil(delayMinutes / 60.0);
        BigDecimal penaltyPerHour = BigDecimal.valueOf(hourlyRate)
                .multiply(BigDecimal.valueOf(0.5));
        return penaltyPerHour.multiply(BigDecimal.valueOf(delayHours));
    }
}
