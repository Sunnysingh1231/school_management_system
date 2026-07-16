package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer>{

	List<Assignment> findAllByClassEntityIdAndSession(int clsId, String sess);

}
