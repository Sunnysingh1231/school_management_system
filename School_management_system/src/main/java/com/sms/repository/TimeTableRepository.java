package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Timetable;

@Repository
public interface TimeTableRepository extends JpaRepository<Timetable, Integer>{

	List<Timetable> findAllByClassEntityId(int id);

}
