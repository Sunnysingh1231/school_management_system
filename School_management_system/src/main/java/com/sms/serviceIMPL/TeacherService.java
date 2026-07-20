package com.sms.serviceIMPL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.ClassEntity;
import com.sms.model.Student;
import com.sms.model.StudentFee;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.AssignmentRepository;
import com.sms.repository.AttendenceRepository;
import com.sms.repository.ClassRepository;
import com.sms.repository.FeeStructureRepository;
import com.sms.repository.SchoolRepository;
import com.sms.repository.StudentFeeRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;
import com.sms.serviceInterface.TeacherServiceInterface;
import com.sms.userDetailService.TeacherDetails;

@Service
public class TeacherService implements TeacherServiceInterface {

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

	@Autowired
	private FeeStructureRepository feeStructureRepository;

	@Autowired
	private StudentFeeRepository studentFeeRepository;
	
	@Autowired
	private AssignmentRepository assignmentRepository;


	public Teacher getCurrentTeacher() {
		
		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		return teacherRepository.findByEmail(email);
//	            .orElseThrow(() -> new RuntimeException("User not found"));
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
// DASHBOARD --------------------------------------------------------------------------------
	
	@Override
	public List<Student> findAllStdByClsId(int id) {
		return studentRepository.findByClassEntityId(id);
	}
	
	@Override
	public List<ClassEntity> findAllClsByTeacherIdAndSclId(int tcrId, int sclId) {

		return classRepository.findByClassTeacherIdAndSchoolId(tcrId, sclId);
	}

	@Override
	public List<ClassEntity> findAllClassByTeacher() {

		return classRepository.findByClassTeacherIdAndSchoolId(getCurrentTeacher().getId(),
				getCurrentTeacher().getSchool().getId());
	}

	@Override
	public List<Student> findAllStudentByClassNameAndSection(String cls, String sec) {

		return studentRepository.findByClassEntityClassNameAndClassEntitySectionAndSchoolId(cls, sec,
				getCurrentTeacher().getSchool().getId());
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

		return attendenceRepository.findByStudentIdAndDateAndClassEntityId(stdId, date, clsId);
	}

	@Override
	public void saveAttendence(int clsId, List<Integer> attends) {

		for (Student student : findAllStudentByClsId(clsId)) {

			Attendence attendence = findAttendenceByStudentIdAndDateAndClassEntityId(student.getId(), LocalDate.now(),
					clsId).orElse(new Attendence());

			attendence.setDate(LocalDate.now());
			attendence.setStudent(student);
			attendence.setClassEntity(classRepository.findById(clsId).get());
			attendence.setSchool(schoolRepository.findById(getCurrentTeacher().getSchool().getId()).get());

			if (attends != null && attends.contains(student.getId())) {
				attendence.setStatus("PRESENT");
			} else {
				attendence.setStatus("ABSENT");
			}

			attendenceRepository.save(attendence);

		}

	}

	@Override
	public List<Attendence> findAttendenceByAndDate(LocalDate date, int clsId) {

		return attendenceRepository.findAllByClassEntityIdAndDate(clsId, date);
	}

	@Override
	public BigDecimal findStudentFeeByClsId(int id) {
		return feeStructureRepository.findAmountByClassEntityId(id);
	}

// STUDENT FEE---------------------------------------------------------------------------------------------------

	@Override
	public List<StudentFee> findStudentFeeDetail(Student student) {

		BigDecimal amount = feeStructureRepository.findAmountByClassEntityId(student.getClassEntity().getId());

		List<StudentFee> feeList = studentFeeRepository.findByStudentId(student.getId());

		String[] monthList = { "April", "May", "June", "July", "August", "September", "October", "November", "December",
				"January", "February", "March" };

		List<StudentFee> resultFees = new ArrayList<>();

		for (int i = 0; i < 12; i++) {

			StudentFee rowFee = new StudentFee();

			rowFee.setMonth(monthList[i]);
			rowFee.setAmount(amount);
			rowFee.setStatus("PENDING");

			for (StudentFee fee : feeList) {

				if (fee.getMonth().equals(monthList[i]) && fee.getSession().equals(getAcademicSession())) {
					rowFee.setStatus("PAID");
					break;
				}

			}

			resultFees.add(rowFee);

		}

		return resultFees;

	}

	@Override
	public void makeStudentPaymentbnt(int id, String[] month) {

		Student student = studentRepository.findById(id).get();

		for (int i = 0; i < month.length; i++) {

			StudentFee sFee = new StudentFee();

			sFee.setStudent(student);
			sFee.setMonth(month[i]);
			sFee.setPaymentDate(LocalDateTime.now());
			sFee.setSchool(student.getSchool());
			sFee.setStatus("PAID");
			sFee.setTransactionId("By " + getCurrentTeacher().getName());
			sFee.setSession(getAcademicSession());
			sFee.setAmount(feeStructureRepository.findAmountByClassEntityId(student.getClassEntity().getId()));

			studentFeeRepository.save(sFee);
		}

	}
	
// ASSIGNMENT ---------------------------------------------------------------------------------------------------	
	
	@Override
	public void createStdAssignment(Assignment assignment, int clsId) {
		
		ClassEntity classEntity = findClassByClassId(clsId).get();
		
		assignment.setSession(getAcademicSession());
		assignment.setTeacher(getCurrentTeacher());
		assignment.setClassEntity(classEntity);
		assignment.setSchool(classEntity.getSchool());
		
		assignmentRepository.save(assignment);
		
	}
	
	@Override
	public List<Assignment> findAllAsinmtByClsId(int clsId){
		return assignmentRepository.findAllByClassEntityIdAndSession(clsId,getAcademicSession());
	}

	

}
