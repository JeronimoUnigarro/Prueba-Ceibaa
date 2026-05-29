package com.ceiba.bicycle_rental.domain.model;

import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bicycles")
@Getter
@Setter
@NoArgsConstructor
public class Bicycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BicycleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BicycleStatus status;

    public Bicycle(String code, BicycleType type, BicycleStatus status) {
        this.code = code;
        this.type = type;
        this.status = status;
    }

    public boolean isAvailable() {
        return this.status == BicycleStatus.DISPONIBLE;
    }
}
