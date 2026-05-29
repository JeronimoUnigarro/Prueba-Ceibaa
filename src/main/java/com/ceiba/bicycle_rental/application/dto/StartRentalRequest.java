package com.ceiba.bicycle_rental.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartRentalRequest {

    @NotBlank(message = "El codigo de la bicicleta es requerido")
    private String bicycleCode;

    @NotBlank(message = "El nombre del cliente es requerido")
    private String clientName;

    @NotNull(message = "La duracion estimada es requerida")
    @Positive(message = "La duracion estimada debe ser mayor a cero")
    private Integer estimatedHours;
}
