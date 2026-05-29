package com.ceiba.bicycle_rental.application.service;

import com.ceiba.bicycle_rental.application.dto.BicycleRequest;
import com.ceiba.bicycle_rental.application.dto.BicycleResponse;
import com.ceiba.bicycle_rental.domain.enums.BicycleStatus;
import com.ceiba.bicycle_rental.domain.enums.BicycleType;
import com.ceiba.bicycle_rental.domain.model.Bicycle;
import com.ceiba.bicycle_rental.domain.repository.BicycleRepository;
import com.ceiba.bicycle_rental.infrastructure.exception.BusinessException;
import com.ceiba.bicycle_rental.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BicycleService {

    private final BicycleRepository bicycleRepository;

    @Transactional
    public BicycleResponse register(BicycleRequest request) {
        if (bicycleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Ya existe una bicicleta con el codigo: " + request.getCode());
        }
        Bicycle bicycle = new Bicycle(request.getCode(), request.getType(), request.getStatus());
        return new BicycleResponse(bicycleRepository.save(bicycle));
    }

    @Transactional(readOnly = true)
    public List<BicycleResponse> getAvailable(BicycleType type) {
        List<Bicycle> bicycles = (type != null)
                ? bicycleRepository.findByStatusAndType(BicycleStatus.DISPONIBLE, type)
                : bicycleRepository.findByStatus(BicycleStatus.DISPONIBLE);
        return bicycles.stream().map(BicycleResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BicycleResponse getByCode(String code) {
        Bicycle bicycle = bicycleRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Bicicleta no encontrada con codigo: " + code));
        return new BicycleResponse(bicycle);
    }
}
