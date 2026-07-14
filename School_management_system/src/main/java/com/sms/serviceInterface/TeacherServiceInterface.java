package com.sms.serviceInterface;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.sms.model.Attendence;
import com.sms.model.ClassEntity;
import com.sms.model.Student;
import com.sms.model.StudentFee;
import com.sms.model.Teacher;

public interface TeacherServiceInterface {
	
	Teacher getCurrentTeacher();
	String getAcademicSession();
	
	List<ClassEntity> findAllClassByTeacher();

	List<Student> findAllStudentByClassNameAndSection(String cls, String sec);
	
	Optional<ClassEntity> findClassByClassId(int id);
	
	List<Student> findAllStudentByClsId(int id);
	
	void saveAttendence(int clsId, List<Integer> attends);
	
	Optional<Attendence> findAttendenceByStudentIdAndDateAndClassEntityId(int stdId, LocalDate date, int clsId);  
	
	List<Attendence> findAttendenceByAndDate(LocalDate date,int clsId);
	
	BigDecimal findStudentFeeByClsId(int id);
	
//	STUDENT FEE CONTROLLER
	
	List<StudentFee> findStudentFeeDetail(Student student);
	
	void makeStudentPaymebnt(int id, String[] month);
	
	
}
