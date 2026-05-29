package com.ceiba.bicycle_rental.infrastructure.config;

import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import com.ceiba.bicycle_rental.domain.repository.BicycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final BicycleRepository bicycleRepository;

    @Override
    public void run(String... args) {
        if (bicycleRepository.count() > 0) return;

        List<Bicycle> bicycles = List.of(
                new Bicycle("BIC-001", BicycleType.URBANA,   BicycleStatus.DISPONIBLE),
                new Bicycle("BIC-002", BicycleType.MONTANA,  BicycleStatus.DISPONIBLE),
                new Bicycle("BIC-003", BicycleType.ELECTRICA, BicycleStatus.DISPONIBLE),
                new Bicycle("BIC-004", BicycleType.MONTANA,  BicycleStatus.EN_MANTENIMIENTO),
                new Bicycle("BIC-005", BicycleType.URBANA,   BicycleStatus.DISPONIBLE)
        );

        bicycleRepository.saveAll(bicycles);
        log.info("Datos iniciales cargados: {} bicicletas", bicycles.size());
    }
}
