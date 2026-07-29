package com.sms.serviceIMPL;

import com.sms.repository.AssignmentRepository;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.NotificationReceiver;
import com.sms.model.Student;
import com.sms.model.StudentAssignment;
import com.sms.model.StudentAssignmentDto;
import com.sms.model.StudentFee;
import com.sms.repository.AttendenceRepository;
import com.sms.repository.NotificationReceiverRepository;
import com.sms.repository.StudentAssignmentRepository;
import com.sms.repository.StudentFeeRepository;
import com.sms.repository.StudentRepository;
import com.sms.serviceInterface.StudentServiceInterface;
import com.sms.serviceInterface.TeacherServiceInterface;

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
	
	@Autowired
	private TeacherServiceInterface teacherServiceInterface;
	
	@Autowired
	private StudentFeeRepository studentFeeRepository;
	
	@Autowired
	private NotificationReceiverRepository notificationReceiverRepository;

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
			return year + "-" + (year +1);
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
	
//	DASHBOARD----------------------------------------------------------------------------------------------------
	
	@Override
	public boolean findCurrentMongthStudentFeeStatus(int stdId) {
		
		String month = LocalDate.now()
		        .getMonth()
		        .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		
		return studentFeeRepository.existsByStudentIdAndSessionAndMonth(stdId, getAcademicSession(), month);
	}

//	ASSIGNMENT----------------------------------------------------------------------------------------------------
	
	@Override
	public List<Assignment> findTop3AssignByClsId(int clsId) {
		return assignmentRepository.findTop3ByClassEntityIdAndSessionOrderByAssignDateDescIdDesc(clsId, getAcademicSession());
	}

	@Override
	public List<StudentAssignment> findStdAssignBtStdId(int id) {
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

	@Override
	public List<StudentAssignment> findAllStudentAssignment(Student student) {
		
		Student s1 = student;

		List<Assignment> assignments = teacherServiceInterface.findAllAsinmtByClsId(s1.getClassEntity().getId());
		Collections.reverse(assignments);
		List<StudentAssignment> sAssignments =findStdAssignBtStdId(s1.getId());
		
		List<StudentAssignment> nAssignments = new ArrayList<>();

		for (Assignment a : assignments) {

			StudentAssignment s = new StudentAssignment();

			s.setAssignment(a);
			s.setSchool(a.getSchool());
			s.setSession(a.getSession());
			s.setStudent(s1);

			for (StudentAssignment sa : sAssignments) {
				if (s.getAssignment().equals(sa.getAssignment())) {
					s.setIsComplete(false);
				}
			}

			nAssignments.add(s);

		}

		return nAssignments;
	}
	
	@Override
	public List<StudentAssignmentDto> studentAssignmentDtos(Student student){
		Student s1 = student;
		return studentAssignmentRepository.findAssignmentDtosByStudentId(s1.getId(), getAcademicSession());
	}
	
	@Override
	public List<StudentAssignmentDto> studentPendingAssignment(Student student){
		
		Student s1 = student;
		List<StudentAssignmentDto> ca = studentAssignmentDtos(s1);
		
		List<Assignment> assignments = teacherServiceInterface.findAllAsinmtByClsId(s1.getClassEntity().getId());
		Collections.reverse(assignments);
		
		List<StudentAssignmentDto> ps = new ArrayList<>();
		
		for(Assignment a : assignments) {
			
			StudentAssignmentDto ps2 = new StudentAssignmentDto();
			boolean f = false;
			
			for(StudentAssignmentDto asd : ca) {
				if(a.getId() == asd.getId()) {
					f = true;
				}
			}
			if(!f) {
				ps2.setId(a.getId());
				ps2.setTitle(a.getTitle());
				ps2.setSubject(a.getSubject());
				ps2.setDescription(a.getDescription());
				ps2.setAssignDate(a.getAssignDate());
				ps2.setDueDate(a.getDueDate());
				
				ps.add(ps2);
			}
			
		}
		
		
		return ps;
	}
	
//	NOTIFICATION----------------------------------------------------------------------------------------------------
	
	@Override
	public List<NotificationReceiver> studentNotice(Student student){
		return notificationReceiverRepository.findAllByStudent(student);
	}
	
	@Override
	public void readNotice(int id){
		NotificationReceiver nr = notificationReceiverRepository.findById(id).get();
		nr.setStatus("READ");
		notificationReceiverRepository.save(nr);
		
	}
}
