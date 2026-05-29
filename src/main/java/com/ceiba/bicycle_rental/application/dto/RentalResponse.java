package com.ceiba.bicycle_rental.application.dto;

import com.ceiba.bicycle_rental.domain.model.Rental;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalResponse {
    private final Long id;
    private final String bicycleCode;
    private final String bicycleType;
    private final String clientName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int estimatedHours;
    private final Long actualDurationHours;
    private final BigDecimal baseCost;
    private final BigDecimal penalty;
    private final BigDecimal totalCost;
    private final boolean hasLateReturn;
    private final boolean finished;

    public RentalResponse(Rental rental) {
        this.id = rental.getId();
        this.bicycleCode = rental.getBicycle().getCode();
        this.bicycleType = rental.getBicycle().getType().name();
        this.clientName = rental.getClientName();
        this.startTime = rental.getStartTime();
        this.endTime = rental.getEndTime();
        this.estimatedHours = rental.getEstimatedHours();
        this.baseCost = rental.getBaseCost();
        this.penalty = rental.getPenalty();
        this.totalCost = rental.getTotalCost();
        this.hasLateReturn = rental.hasLateReturn();
        this.finished = rental.isFinished();

        if (rental.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(rental.getStartTime(), rental.getEndTime());
            this.actualDurationHours = (long) Math.ceil(minutes / 60.0);
        } else {
            this.actualDurationHours = null;
        }
    }
}
