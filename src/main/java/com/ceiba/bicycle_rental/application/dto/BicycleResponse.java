package com.ceiba.bicycle_rental.application.dto;

import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import lombok.Getter;

@Getter
public class BicycleResponse {
    private final Long id;
    private final String code;
    private final BicycleType type;
    private final BicycleStatus status;

    public BicycleResponse(Bicycle bicycle) {
        this.id = bicycle.getId();
        this.code = bicycle.getCode();
        this.type = bicycle.getType();
        this.status = bicycle.getStatus();
    }
}
