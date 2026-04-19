package com.sms.serviceIMPL;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sms.model.Attendence;
import com.sms.model.Student;
import com.sms.repository.AttendenceRepository;
import com.sms.repository.StudentRepository;
import com.sms.serviceInterface.StudentServiceInterface;

import lombok.Data;


@Service
public class StudentService implements StudentServiceInterface {

	
	
	@Autowired
	private StudentRepository studentRepository;
	
	@Autowired
	private AttendenceRepository attendenceRepository;	
	
	
	public Student getCurrentStudent() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName(); 
		
		Student s1 = studentRepository.findByEmail(email);

		return s1;
	}





	@Override
	public List<Attendence> findAttendenceByStudentId(int id) {
		
		return attendenceRepository.findByStudentId(id);
		
	}





	@Override
	public void updateStudent(Student student) {
		studentRepository.save(student);
		
	}





	@Override
	public List<Attendence> findTop6AttendenceOfStudentByStudentId(int id) {
		
		return attendenceRepository.findTop6ByStudent_IdOrderByDateDesc(id);
	}





	@Override
	public Optional<Student> findStudentByStudentId(int id) {
		return studentRepository.findById(id);
	}
}
