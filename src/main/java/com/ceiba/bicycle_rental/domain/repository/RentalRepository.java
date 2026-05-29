package com.ceiba.bicycle_rental.domain.repository;

import com.ceiba.bicycle_rental.domain.model.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    @Query("SELECT r FROM Rental r JOIN FETCH r.bicycle b WHERE b.code = :code ORDER BY r.startTime DESC")
    List<Rental> findByBicycleCodeOrderByStartTimeDesc(@Param("code") String code);
}
