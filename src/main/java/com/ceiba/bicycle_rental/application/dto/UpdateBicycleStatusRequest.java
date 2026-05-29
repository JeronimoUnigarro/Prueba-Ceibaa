package com.ceiba.bicycle_rental.application.dto;

import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBicycleStatusRequest {

    @NotNull(message = "El estado es requerido (DISPONIBLE, ALQUILADA, EN_MANTENIMIENTO)")
    private BicycleStatus status;
}
