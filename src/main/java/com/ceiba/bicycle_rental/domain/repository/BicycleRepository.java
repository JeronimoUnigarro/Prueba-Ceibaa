package com.ceiba.bicycle_rental.domain.repository;

import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BicycleRepository extends JpaRepository<Bicycle, Long> {
    Optional<Bicycle> findByCode(String code);
    boolean existsByCode(String code);
    List<Bicycle> findByStatus(BicycleStatus status);
    List<Bicycle> findByStatusAndType(BicycleStatus status, BicycleType type);
}
