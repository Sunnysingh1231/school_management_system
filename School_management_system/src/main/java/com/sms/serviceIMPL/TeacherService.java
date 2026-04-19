package com.sms.serviceIMPL;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sms.model.Attendence;
import com.sms.model.ClassEntity;
import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.AttendenceRepository;
import com.sms.repository.ClassRepository;
import com.sms.repository.SchoolRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;
import com.sms.serviceInterface.TeacherServiceInterface;

@Service
public class TeacherService implements TeacherServiceInterface{

	@Autowired
	private ClassRepository classRepository;
	
	@Autowired
	private SchoolRepository schoolRepository;
	
	@Autowired
	private TeacherRepository teacherRepository;
	
	@Autowired
	private StudentRepository studentRepository;
	
	@Autowired
	private AttendenceRepository attendenceRepository;
	
	public Teacher getCurrentTeacher() {
	    String email = SecurityContextHolder.getContext()
	            .getAuthentication().getName();

	    return teacherRepository.findByEmail(email);
//	            .orElseThrow(() -> new RuntimeException("User not found"));
	}

	@Override
	public List<ClassEntity> findAllClassByTeacher() {
		
		return classRepository.findByClassTeacherIdAndSchoolId(getCurrentTeacher().getId(),getCurrentTeacher().getSchool().getId());
	}

	@Override
	public List<Student> findAllStudentByClassNameAndSection(String cls,String sec) {
		
		return studentRepository.findByClassEntityClassNameAndClassEntitySectionAndSchoolId(cls,sec, getCurrentTeacher().getSchool().getId());
	}

	@Override
	public Optional<ClassEntity> findClassByClassId(int id) {
		return classRepository.findById(id);
	}

	@Override
	public List<Student> findAllStudentByClsId(int id) {
		
		return studentRepository.findByClassEntityIdAndSchoolId(id, getCurrentTeacher().getSchool().getId());
	}

	@Override
	public Optional<Attendence> findAttendenceByStudentIdAndDateAndClassEntityId(int stdId, LocalDate date, int clsId) {
		
		return attendenceRepository.findByStudentIdAndDateAndClassEntityId(stdId,date,clsId);
	}
	
	
	@Override
	public void saveAttendence(int clsId, List<Integer> attends) {
		
		
		
		for(Student student : findAllStudentByClsId(clsId)) {
			
			Attendence attendence =
				    findAttendenceByStudentIdAndDateAndClassEntityId(
				        student.getId(), LocalDate.now(), clsId
				    ).orElse(new Attendence());
			
			attendence.setDate(LocalDate.now());
			attendence.setStudent(student);
			attendence.setClassEntity(classRepository.findById(clsId).get());
			attendence.setSchool(schoolRepository.findById(getCurrentTeacher().getSchool().getId()).get());
			
			if(attends != null && attends.contains(student.getId())) {
				attendence.setStatus("PRESENT");
			}else {
				attendence.setStatus("ABSENT");
			}
			
			attendenceRepository.save(attendence);
			
		}
		
		
		
	}

	@Override
	public List<Attendence> findAttendenceByAndDate(LocalDate date,int clsId) {
		
		return attendenceRepository.findBySchoolIdAndClassEntityIdAndDate(getCurrentTeacher().getSchool().getId(), clsId, date);
	}
	
	

}
