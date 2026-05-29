package com.ceiba.bicycle_rental.application.service;

import com.ceiba.bicycle_rental.application.dto.RentalResponse;
import com.ceiba.bicycle_rental.application.dto.StartRentalRequest;
import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import com.ceiba.bicycle_rental.domain.model.Rental;
import com.ceiba.bicycle_rental.domain.repository.BicycleRepository;
import com.ceiba.bicycle_rental.domain.repository.RentalRepository;
import com.ceiba.bicycle_rental.infrastructure.exception.BusinessException;
import com.ceiba.bicycle_rental.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private BicycleRepository bicycleRepository;

    @Mock
    private RentalRepository rentalRepository;

    private RentalService rentalService;

    // Fixed clock: 2024-01-01T10:00:00 UTC
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 1, 1, 10, 0, 0);

    private Bicycle mountainBike;
    private Bicycle urbanBike;
    private Bicycle electricBike;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        rentalService = new RentalService(bicycleRepository, rentalRepository, fixedClock);

        mountainBike = new Bicycle("BIC-002", BicycleType.MONTANA, BicycleStatus.DISPONIBLE);
        urbanBike    = new Bicycle("BIC-001", BicycleType.URBANA,   BicycleStatus.DISPONIBLE);
        electricBike = new Bicycle("BIC-003", BicycleType.ELECTRICA, BicycleStatus.DISPONIBLE);
    }

    // =========================================================
    // startRental
    // =========================================================

    @Test
    @DisplayName("RN-04: iniciar alquiler en bicicleta disponible cambia estado a ALQUILADA")
    void startRental_availableBike_changesBikeStatusToAlquilada() {
        StartRentalRequest request = buildStartRequest("BIC-001", "Juan Garcia", 2);

        when(bicycleRepository.findByCode("BIC-001")).thenReturn(Optional.of(urbanBike));
        when(rentalRepository.save(any())).thenAnswer(i -> withId(i.getArgument(0), 1L));

        rentalService.startRental(request);

        assertThat(urbanBike.getStatus()).isEqualTo(BicycleStatus.ALQUILADA);
        verify(bicycleRepository).save(urbanBike);
    }

    @Test
    @DisplayName("RN-04: iniciar alquiler en bicicleta ALQUILADA lanza BusinessException")
    void startRental_bikeAlreadyRented_throwsBusinessException() {
        mountainBike.setStatus(BicycleStatus.ALQUILADA);
        when(bicycleRepository.findByCode("BIC-002")).thenReturn(Optional.of(mountainBike));

        assertThatThrownBy(() -> rentalService.startRental(buildStartRequest("BIC-002", "Pedro", 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no esta disponible");
    }

    @Test
    @DisplayName("RN-04: iniciar alquiler en bicicleta EN_MANTENIMIENTO lanza BusinessException")
    void startRental_bikeInMaintenance_throwsBusinessException() {
        mountainBike.setStatus(BicycleStatus.EN_MANTENIMIENTO);
        when(bicycleRepository.findByCode("BIC-002")).thenReturn(Optional.of(mountainBike));

        assertThatThrownBy(() -> rentalService.startRental(buildStartRequest("BIC-002", "Ana", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no esta disponible");
    }

    @Test
    @DisplayName("RF-02: bicicleta inexistente lanza ResourceNotFoundException")
    void startRental_bikeNotFound_throwsResourceNotFoundException() {
        when(bicycleRepository.findByCode("BIC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.startRental(buildStartRequest("BIC-999", "Luis", 2)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    // finishRental — calculo de costo (RN-02)
    // =========================================================

    @Test
    @DisplayName("RN-02: tiempo exacto se cobra sin redondeo adicional (2h exactas = 2h)")
    void finishRental_exactTwoHours_chargesExactHours() {
        // start at 08:00, return at 10:00 → 120 min → ceil(120/60) = 2h
        Rental rental = buildRental(mountainBike, LocalDateTime.of(2024, 1, 1, 8, 0), 3);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RentalResponse response = rentalService.finishRental(1L);

        // 2h x 5000 = 10000, no penalty (estimated 3h, used 2h)
        assertThat(response.getBaseCost()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getPenalty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("RN-02: hora parcial se redondea al alza (1h 10min = 2h cobradas)")
    void finishRental_oneHourTenMinutes_roundsUpToTwoHours() {
        // start at 08:50, return at 10:00 → 70 min → ceil(70/60) = 2h
        Rental rental = buildRental(urbanBike, LocalDateTime.of(2024, 1, 1, 8, 50), 5);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RentalResponse response = rentalService.finishRental(1L);

        // 2h x 3500 = 7000
        assertThat(response.getBaseCost()).isEqualByComparingTo(new BigDecimal("7000"));
        assertThat(response.getPenalty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("7000"));
    }

    @Test
    @DisplayName("RN-03: ejemplo del enunciado — MONTANA 2h estimadas, devuelta 3h20min → total $25.000")
    void finishRental_lateReturnSpecExample_calculatesCorrectPenalty() {
        // Clock fixed at 10:00. start at 06:40 → 3h20min actual (200 min)
        // estimated 2h → estimated end = 08:40
        // delay = 10:00 - 08:40 = 1h20min (80 min) → ceil = 2h delay
        Rental rental = buildRental(mountainBike, LocalDateTime.of(2024, 1, 1, 6, 40), 2);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RentalResponse response = rentalService.finishRental(1L);

        assertThat(response.getBaseCost()).isEqualByComparingTo(new BigDecimal("20000")); // 4h x 5000
        assertThat(response.getPenalty()).isEqualByComparingTo(new BigDecimal("5000"));   // 2 x (5000 x 0.5)
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(response.isHasLateReturn()).isTrue();
    }

    @Test
    @DisplayName("RN-03: devolucion exactamente a tiempo no genera multa")
    void finishRental_returnedExactlyOnTime_noPenalty() {
        // start at 08:00, estimated 2h, return at 10:00 → exactly on time
        Rental rental = buildRental(mountainBike, LocalDateTime.of(2024, 1, 1, 8, 0), 2);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RentalResponse response = rentalService.finishRental(1L);

        assertThat(response.getPenalty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.isHasLateReturn()).isFalse();
    }

    @Test
    @DisplayName("RN-03: retraso de 1 minuto se redondea a 1h de multa")
    void finishRental_oneMinuteLate_chargesOneHourPenalty() {
        // start at 07:59, estimated 2h → estimated end = 09:59; return at 10:00 → 1 min late
        Rental rental = buildRental(urbanBike, LocalDateTime.of(2024, 1, 1, 7, 59), 2);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RentalResponse response = rentalService.finishRental(1L);

        // delay = 1 min → ceil(1/60) = 1h → penalty = 1 x (3500 x 0.5) = 1750
        assertThat(response.getPenalty()).isEqualByComparingTo(new BigDecimal("1750"));
        assertThat(response.isHasLateReturn()).isTrue();
    }

    @Test
    @DisplayName("RN-01: tarifa ELECTRICA aplicada correctamente")
    void finishRental_electricBike_appliesCorrectRate() {
        // start at 09:00, return at 10:00 → 1h exact
        Rental rental = buildRental(electricBike, LocalDateTime.of(2024, 1, 1, 9, 0), 3);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RentalResponse response = rentalService.finishRental(1L);

        // 1h x 7500 = 7500
        assertThat(response.getBaseCost()).isEqualByComparingTo(new BigDecimal("7500"));
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("7500"));
    }

    // =========================================================
    // finishRental — validaciones (RN-05)
    // =========================================================

    @Test
    @DisplayName("RN-05: finalizar alquiler ya terminado lanza BusinessException")
    void finishRental_alreadyFinished_throwsBusinessException() {
        Rental rental = buildRental(mountainBike, LocalDateTime.of(2024, 1, 1, 8, 0), 2);
        rental.setFinished(true);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertThatThrownBy(() -> rentalService.finishRental(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue finalizado");
    }

    @Test
    @DisplayName("RN-05: finalizar alquiler inexistente lanza ResourceNotFoundException")
    void finishRental_notFound_throwsResourceNotFoundException() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.finishRental(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RF-03: al finalizar, la bicicleta vuelve a estado DISPONIBLE")
    void finishRental_completesSuccessfully_changesBikeStatusToDisponible() {
        mountainBike.setStatus(BicycleStatus.ALQUILADA);
        Rental rental = buildRental(mountainBike, LocalDateTime.of(2024, 1, 1, 8, 0), 2);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        rentalService.finishRental(1L);

        assertThat(mountainBike.getStatus()).isEqualTo(BicycleStatus.DISPONIBLE);
        verify(bicycleRepository).save(mountainBike);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private StartRentalRequest buildStartRequest(String code, String client, int hours) {
        StartRentalRequest req = new StartRentalRequest();
        req.setBicycleCode(code);
        req.setClientName(client);
        req.setEstimatedHours(hours);
        return req;
    }

    private Rental buildRental(Bicycle bicycle, LocalDateTime startTime, int estimatedHours) {
        Rental rental = new Rental(bicycle, "Test Client", startTime, estimatedHours);
        rental.setId(1L);
        return rental;
    }

    private Rental withId(Rental rental, Long id) {
        rental.setId(id);
        return rental;
    }
}
