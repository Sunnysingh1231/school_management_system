package com.example.demo.repository;

import com.example.demo.model.EmaTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmaTradeRepository
        extends JpaRepository<EmaTrade, Long> {

    Optional<EmaTrade> findFirstByInstrumentKeyAndStatus(
            String instrumentKey,
            String status
    );
}