package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.ClassEntity;
import com.sms.model.School;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Integer>{

	boolean existsByClassNameAndSectionAndSchool(String className, String section, School school);

	List<ClassEntity> findBySchoolId(int id);

	ClassEntity findByClassNameAndSectionAndSchoolId(String classname, String sec,int id);

	Optional<ClassEntity> findAllByClassNameAndSection(String className, String section);

	List<ClassEntity> findByClassTeacherIdAndSchoolId(int id, int id2);

//	Optional<ClassEntity> findByClassNameAndSection(String className, String section);

}
