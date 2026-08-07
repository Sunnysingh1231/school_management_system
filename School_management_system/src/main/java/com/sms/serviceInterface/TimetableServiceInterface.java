package com.sms.serviceInterface;

import java.time.LocalTime;
import java.util.List;

import com.sms.model.ClassEntity;
import com.sms.model.Timetable;

public interface TimetableServiceInterface {

	void saveTimrTable(List<LocalTime> st, List<LocalTime> et, List<String> subject, List<String> days, int periods, ClassEntity cs);

	List<Timetable> findAlltimetable(int id);

	List<Timetable> findTimeTableOfClass(int clsId);

	void deleteTimetable(int id);

}
