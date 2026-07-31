package com.sms.serviceIMPL;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sms.model.ClassEntity;
import com.sms.model.Period;
import com.sms.model.Timetable;
import com.sms.repository.PeriodRepository;
import com.sms.repository.TimeTableRepository;
import com.sms.serviceInterface.TimetableInterface;

@Service
public class TimetableService implements TimetableInterface{
	
	@Autowired
	private TimeTableRepository timeTableRepository;
	
	@Autowired
	private PeriodRepository periodRepository;

	@Override
	public void saveTimrTable(List<LocalTime> st, List<LocalTime> et, List<String> subject, List<String> days, int periods, ClassEntity cs) {
		
		System.out.println("----------------------------------------------------------");
		
		int c = 0;
		for(int i=0;i<days.size();i++) {
			
			Timetable timetable = new Timetable();
			List<Period> ht = new ArrayList<>();
			
			for(int j=0;j<periods;j++) {
				
				Period rt = new Period();
				
				rt.setStartTime(st.get(c));
				rt.setEndTime(et.get(c));
				rt.setSubject(subject.get(c));
				rt.setTimetable(timetable);
				
				ht.add(rt);
				c++;
			}
			timetable.setDay(days.get(i));
			timetable.setPeriods(ht);
			timetable.setClassEntity(cs);
			
			timeTableRepository.save(timetable);
			
		}
		
	}
	
	@Override
	public List<Timetable> findAlltimetable(int id){
		return timeTableRepository.findAllByClassEntityId(id);
	}
	
	@Override
	public List<Timetable> findTimeTableOfClass(int clsId){
		return timeTableRepository.findAllByClassEntityId(clsId);
	}
		
	
}
