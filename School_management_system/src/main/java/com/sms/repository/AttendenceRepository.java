package com.sms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sms.model.Attendence;

public interface AttendenceRepository extends JpaRepository<Attendence, Integer>{

	Optional<Attendence> findByStudentIdAndDateAndClassEntityId(int stdId, LocalDate date, int clsId);

	void save(Optional<Attendence> attendence);

	List<Attendence> findBySchoolIdAndClassEntityIdAndDate(int sclId, int clsId, LocalDate date);

	
	//STUDENT-------------------------------------------------------------
	
	List<Attendence> findByStudentId(int id);
	
	List<Attendence> findTop6ByStudent_IdOrderByDateDesc(int id);
	
	


}
