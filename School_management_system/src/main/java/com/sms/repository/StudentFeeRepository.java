package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.StudentFee;

@Repository
public interface StudentFeeRepository extends JpaRepository<StudentFee, Integer>{

	List<StudentFee> findByStudentId(int id);

}
