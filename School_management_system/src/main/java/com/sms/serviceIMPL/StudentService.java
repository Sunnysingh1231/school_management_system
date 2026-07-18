package com.sms.serviceIMPL;

import com.sms.repository.AssignmentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.Student;
import com.sms.model.StudentAssignment;
import com.sms.repository.AttendenceRepository;
import com.sms.repository.StudentAssignmentRepository;
import com.sms.repository.StudentRepository;
import com.sms.serviceInterface.StudentServiceInterface;

import lombok.Data;

@Service
public class StudentService implements StudentServiceInterface {

	private final AssignmentRepository assignmentRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private AttendenceRepository attendenceRepository;
	
	@Autowired
	private StudentAssignmentRepository studentAssignmentRepository;

	StudentService(AssignmentRepository assignmentRepository) {
		this.assignmentRepository = assignmentRepository;
	}

	public Student getCurrentStudent() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		Student s1 = studentRepository.findByEmail(email);

		return s1;
	}

	@Override
	public String getAcademicSession() {
		LocalDate today = LocalDate.now();
		int year = today.getYear();
		int month = today.getMonthValue();

		if (month >= 4) {
			return year + "-" + (year + 1);
		} else {
			return (year - 1) + "-" + year;
		}
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
	
	@Override
	public List<Assignment> findTop3AssignByClsId(int clsId){
		return assignmentRepository.findTop3ByClassEntityIdAndSession(clsId, getAcademicSession());
	}
	
	@Override
	public List<StudentAssignment> findStdAssignBtStdId(int id){
		return studentAssignmentRepository.findAllByStudentIdAndSession(id, getAcademicSession());
	}

	@Override
	public void markAssignmentComplete(int id, int stdId) {
		
		Assignment a = assignmentRepository.findById(id).get();
		Student s = findStudentByStudentId(stdId).get();
		
		StudentAssignment sa = new StudentAssignment();
		sa.setAssignment(a);
		sa.setStudent(s);
		sa.setSchool(a.getSchool());
		sa.setSession(getAcademicSession());
		
	    studentAssignmentRepository.save(sa);
		
	}
}
