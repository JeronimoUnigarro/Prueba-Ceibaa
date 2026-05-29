package com.ceiba.bicycle_rental.application.service;

import com.ceiba.bicycle_rental.application.dto.BicycleRequest;
import com.ceiba.bicycle_rental.application.dto.BicycleResponse;
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

    private BicycleRequest buildRequest(String code, BicycleType type, BicycleStatus status) {
        BicycleRequest request = new BicycleRequest();
        request.setCode(code);
        request.setType(type);
        request.setStatus(status);
        return request;
    }
}
