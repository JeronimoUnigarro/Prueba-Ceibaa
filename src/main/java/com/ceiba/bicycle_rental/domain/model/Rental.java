package com.ceiba.bicycle_rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rentals")
@Getter
@Setter
@NoArgsConstructor
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id", nullable = false)
    private Bicycle bicycle;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private int estimatedHours;

    private LocalDateTime endTime;

    private BigDecimal baseCost;

    private BigDecimal penalty;

    private BigDecimal totalCost;

    private boolean finished;

    public Rental(Bicycle bicycle, String clientName, LocalDateTime startTime, int estimatedHours) {
        this.bicycle = bicycle;
        this.clientName = clientName;
        this.startTime = startTime;
        this.estimatedHours = estimatedHours;
        this.finished = false;
    }

    public boolean hasLateReturn() {
        return penalty != null && penalty.compareTo(BigDecimal.ZERO) > 0;
    }
}
