package com.ceiba.bicycle_rental.application.service;

import com.ceiba.bicycle_rental.application.dto.BicycleRequest;
import com.ceiba.bicycle_rental.application.dto.BicycleResponse;
import com.ceiba.bicycle_rental.application.dto.UpdateBicycleStatusRequest;
import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import com.ceiba.bicycle_rental.domain.repository.BicycleRepository;
import com.ceiba.bicycle_rental.infrastructure.exception.BusinessException;
import com.ceiba.bicycle_rental.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BicycleServiceTest {

    @Mock
    private BicycleRepository bicycleRepository;

    @InjectMocks
    private BicycleService bicycleService;

    @Test
    @DisplayName("RF-01: registrar bicicleta con codigo nuevo retorna la bicicleta creada")
    void register_newBicycle_returnsCreatedBicycle() {
        BicycleRequest request = buildRequest("BIC-010", BicycleType.URBANA, BicycleStatus.DISPONIBLE);

        when(bicycleRepository.existsByCode("BIC-010")).thenReturn(false);
        when(bicycleRepository.save(any())).thenAnswer(i -> {
            Bicycle b = i.getArgument(0);
            b.setId(10L);
            return b;
        });

        BicycleResponse response = bicycleService.register(request);

        assertThat(response.getCode()).isEqualTo("BIC-010");
        assertThat(response.getType()).isEqualTo(BicycleType.URBANA);
        assertThat(response.getStatus()).isEqualTo(BicycleStatus.DISPONIBLE);
    }

    @Test
    @DisplayName("RF-01: registrar bicicleta con codigo duplicado lanza BusinessException")
    void register_duplicateCode_throwsBusinessException() {
        BicycleRequest request = buildRequest("BIC-001", BicycleType.URBANA, BicycleStatus.DISPONIBLE);
        when(bicycleRepository.existsByCode("BIC-001")).thenReturn(true);

        assertThatThrownBy(() -> bicycleService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BIC-001");
    }

    @Test
    @DisplayName("RF-04: sin filtro de tipo, retorna solo bicicletas DISPONIBLES")
    void getAvailable_noFilter_returnsOnlyDisponible() {
        Bicycle b1 = new Bicycle("BIC-001", BicycleType.URBANA,   BicycleStatus.DISPONIBLE);
        Bicycle b2 = new Bicycle("BIC-003", BicycleType.ELECTRICA, BicycleStatus.DISPONIBLE);

        when(bicycleRepository.findByStatus(BicycleStatus.DISPONIBLE)).thenReturn(List.of(b1, b2));

        List<BicycleResponse> result = bicycleService.getAvailable(null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BicycleResponse::getStatus)
                .containsOnly(BicycleStatus.DISPONIBLE);
    }

    @Test
    @DisplayName("RF-04: filtrar por tipo URBANA retorna solo bicicletas urbanas disponibles")
    void getAvailable_withTypeFilter_returnsOnlyMatchingType() {
        Bicycle b1 = new Bicycle("BIC-001", BicycleType.URBANA, BicycleStatus.DISPONIBLE);

        when(bicycleRepository.findByStatusAndType(BicycleStatus.DISPONIBLE, BicycleType.URBANA))
                .thenReturn(List.of(b1));

        List<BicycleResponse> result = bicycleService.getAvailable(BicycleType.URBANA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(BicycleType.URBANA);
    }

    @Test
    @DisplayName("RF-04: filtrar por tipo sin disponibles retorna lista vacia")
    void getAvailable_noMatchingBikes_returnsEmptyList() {
        when(bicycleRepository.findByStatusAndType(BicycleStatus.DISPONIBLE, BicycleType.ELECTRICA))
                .thenReturn(List.of());

        List<BicycleResponse> result = bicycleService.getAvailable(BicycleType.ELECTRICA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("RF-01: buscar bicicleta por codigo existente retorna la bicicleta")
    void getByCode_existingCode_returnsBicycle() {
        Bicycle bicycle = new Bicycle("BIC-001", BicycleType.URBANA, BicycleStatus.DISPONIBLE);
        bicycle.setId(1L);
        when(bicycleRepository.findByCode("BIC-001")).thenReturn(Optional.of(bicycle));

        BicycleResponse response = bicycleService.getByCode("BIC-001");

        assertThat(response.getCode()).isEqualTo("BIC-001");
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("RF-01: buscar bicicleta por codigo inexistente lanza ResourceNotFoundException")
    void getByCode_nonExistingCode_throwsResourceNotFoundException() {
        when(bicycleRepository.findByCode("BIC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bicycleService.getByCode("BIC-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BIC-999");
    }

    // =========================================================
    // getAll
    // =========================================================

    @Test
    @DisplayName("getAll sin filtro retorna todas las bicicletas sin importar estado")
    void getAll_noFilter_returnsAllBicycles() {
        Bicycle b1 = new Bicycle("BIC-001", BicycleType.URBANA,   BicycleStatus.DISPONIBLE);
        Bicycle b2 = new Bicycle("BIC-002", BicycleType.MONTANA,  BicycleStatus.ALQUILADA);
        Bicycle b3 = new Bicycle("BIC-004", BicycleType.MONTANA,  BicycleStatus.EN_MANTENIMIENTO);

        when(bicycleRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        List<BicycleResponse> result = bicycleService.getAll(null);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(BicycleResponse::getStatus)
                .containsExactlyInAnyOrder(
                        BicycleStatus.DISPONIBLE,
                        BicycleStatus.ALQUILADA,
                        BicycleStatus.EN_MANTENIMIENTO);
    }

    @Test
    @DisplayName("getAll con filtro EN_MANTENIMIENTO retorna solo las que estan en mantenimiento")
    void getAll_withStatusFilter_returnsOnlyMatchingStatus() {
        Bicycle b4 = new Bicycle("BIC-004", BicycleType.MONTANA, BicycleStatus.EN_MANTENIMIENTO);

        when(bicycleRepository.findByStatus(BicycleStatus.EN_MANTENIMIENTO)).thenReturn(List.of(b4));

        List<BicycleResponse> result = bicycleService.getAll(BicycleStatus.EN_MANTENIMIENTO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BicycleStatus.EN_MANTENIMIENTO);
        assertThat(result.get(0).getCode()).isEqualTo("BIC-004");
    }

    // =========================================================
    // updateStatus
    // =========================================================

    @Test
    @DisplayName("updateStatus: bicicleta EN_MANTENIMIENTO pasa a DISPONIBLE correctamente")
    void updateStatus_fromMaintenanceToAvailable_updatesSuccessfully() {
        Bicycle bicycle = new Bicycle("BIC-004", BicycleType.MONTANA, BicycleStatus.EN_MANTENIMIENTO);
        bicycle.setId(4L);

        UpdateBicycleStatusRequest request = new UpdateBicycleStatusRequest();
        request.setStatus(BicycleStatus.DISPONIBLE);

        when(bicycleRepository.findByCode("BIC-004")).thenReturn(Optional.of(bicycle));
        when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BicycleResponse response = bicycleService.updateStatus("BIC-004", request);

        assertThat(response.getStatus()).isEqualTo(BicycleStatus.DISPONIBLE);
        assertThat(response.getCode()).isEqualTo("BIC-004");
        verify(bicycleRepository).save(bicycle);
    }

    @Test
    @DisplayName("updateStatus: bicicleta inexistente lanza ResourceNotFoundException")
    void updateStatus_nonExistingCode_throwsResourceNotFoundException() {
        UpdateBicycleStatusRequest request = new UpdateBicycleStatusRequest();
        request.setStatus(BicycleStatus.DISPONIBLE);

        when(bicycleRepository.findByCode("BIC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bicycleService.updateStatus("BIC-999", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BIC-999");
    }

    private BicycleRequest buildRequest(String code, BicycleType type, BicycleStatus status) {
        BicycleRequest request = new BicycleRequest();
        request.setCode(code);
        request.setType(type);
        request.setStatus(status);
        return request;
    }
}
