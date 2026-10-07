package com.example.demo.repository;

import com.example.demo.model.PaperOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperOrderRepository
        extends JpaRepository<PaperOrder, Long> {

    List<PaperOrder> findByStatusOrderByExecutedAtDesc(
            String status
    );
}