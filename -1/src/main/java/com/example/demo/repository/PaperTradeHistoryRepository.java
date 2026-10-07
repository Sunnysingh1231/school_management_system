package com.example.demo.repository;

import com.example.demo.model.PaperTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperTradeHistoryRepository
        extends JpaRepository<PaperTradeHistory, Long> {

    List<PaperTradeHistory> findAllByOrderBySellDateDesc();
}