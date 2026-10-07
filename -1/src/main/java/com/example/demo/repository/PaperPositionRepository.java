package com.example.demo.repository;

import com.example.demo.model.PaperPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperPositionRepository
        extends JpaRepository<PaperPosition, Long> {

    Optional<PaperPosition> findByInstrumentKey(String instrumentKey);

    List<PaperPosition> findAllByOrderByBuyDateDesc();
}