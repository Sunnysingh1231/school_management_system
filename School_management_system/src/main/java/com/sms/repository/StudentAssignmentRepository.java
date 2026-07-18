package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.StudentAssignment;

@Repository
public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Integer>{

	List<StudentAssignment> findAllByStudentIdAndSession(int id, String academicSession);

}
