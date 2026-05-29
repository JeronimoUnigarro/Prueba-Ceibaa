package com.ceiba.bicycle_rental.application.dto;

import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BicycleRequest {

    @NotBlank(message = "El codigo de la bicicleta es requerido")
    private String code;

    @NotNull(message = "El tipo de bicicleta es requerido (URBANA, MONTANA, ELECTRICA)")
    private BicycleType type;

    @NotNull(message = "El estado de la bicicleta es requerido (DISPONIBLE, ALQUILADA, EN_MANTENIMIENTO)")
    private BicycleStatus status;
}
