package com.sms.repository;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Student;
import com.sms.model.StudentFee;

@Repository
public interface StudentFeeRepository extends JpaRepository<StudentFee, Integer>{

	List<StudentFee> findByStudentId(int id);

	boolean existsByStudentIdAndSessionAndMonth(int stdId, String academicSession, String month);
	

}
