package com.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Period;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Integer>{

}
