package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.School;
import com.sms.model.Student;

@Repository
public interface SchoolRepository extends JpaRepository<School, Integer>{

	School findByName(String school);


}
