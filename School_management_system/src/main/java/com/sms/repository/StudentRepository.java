package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.ClassEntity;
import com.sms.model.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>{


	List<Student> findByClassEntityClassNameAndClassEntitySectionAndSchoolId(String cls, String sec, int id);

	List<Student> findAllBySchoolId(int id);

	List<Student> findByClassEntityIdAndSchoolId(Integer id, int id2);

	List<Student> findByClassEntityClassNameAndSchoolId(String cls, int id);

	List<Student> findByClassEntitySectionAndSchoolId(String sec, int id);

	Student findByEmail(String username);

	
}
