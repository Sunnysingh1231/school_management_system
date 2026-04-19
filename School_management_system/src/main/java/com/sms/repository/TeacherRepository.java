package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.ClassEntity;
import com.sms.model.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer>{

	@Query("SELECT t FROM Teacher t JOIN FETCH t.role WHERE t.email = :email")
	Teacher findByEmail(@Param("email") String email);
	
//	Teacher findByEmail(String username);
	
	List<Teacher> findBySchoolId(int id);

	Optional<Teacher> findByName(String teacher);

	Teacher findByIdAndSchoolId(int id, int id2);
	
}
