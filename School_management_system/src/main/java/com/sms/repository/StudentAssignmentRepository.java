package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.Assignment;
import com.sms.model.StudentAssignment;
import com.sms.model.StudentAssignmentDto;

@Repository
public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Integer>{

	List<StudentAssignment> findAllByStudentIdAndSession(int id, String academicSession);

	@Query("SELECT sa.assignment FROM StudentAssignment sa WHERE sa.student.id = :studentId")
	List<Assignment> findAssignmentsByStudentId(@Param("studentId") int studentId);
	
	@Query("""
			SELECT new com.sms.model.StudentAssignmentDto(
			    a.id,
			    a.title,
			    a.subject,
			    a.description,
			    a.dueDate,
			    a.assignDate
			)
			FROM StudentAssignment sa
			JOIN sa.assignment a
			WHERE sa.student.id = :studentId
			AND sa.session = :session
			""")
			List<StudentAssignmentDto> findAssignmentDtosByStudentId(@Param("studentId") int studentId, @Param("session") String session);

}
