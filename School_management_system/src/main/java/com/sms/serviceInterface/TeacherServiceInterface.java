package com.sms.serviceInterface;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.ClassEntity;
import com.sms.model.Notification;
import com.sms.model.Student;
import com.sms.model.StudentFee;
import com.sms.model.Teacher;

public interface TeacherServiceInterface {
	
//USING Dashboard, 
	Teacher getCurrentTeacher();
	String getAcademicSession();

//USING Dashboard, 
	List<ClassEntity> findAllClsByTeacherIdAndSclId(int tcrId, int sclId);
	
	List<ClassEntity> findAllClassByTeacher();
	
//USING Dashboard, Student List, 
	List<Student> findAllStdByClsId(int id);

	List<Student> findAllStudentByClassNameAndSection(String cls, String sec);
	
	Optional<ClassEntity> findClassByClassId(int id);
	
	List<Student> findAllStudentByClsId(int id);
	
	void saveAttendence(int clsId, List<Integer> attends);
	
	Optional<Attendence> findAttendenceByStudentIdAndDateAndClassEntityId(int stdId, LocalDate date, int clsId);  
	
//USING Dashboard, 
	List<Attendence> findAttendenceByAndDate(LocalDate date,int clsId);
	
	BigDecimal findStudentFeeByClsId(int id);
	
//	STUDENT FEE CONTROLLER
	
	List<StudentFee> findStudentFeeDetail(Student student);
	
	void createStdAssignment(Assignment assignment,int clsId);
	
	List<Assignment> findAllAsinmtByClsId(int clsId);
	
	void makeStudentPaymentbnt(int id, String[] month);
	
	List<Notification> findAllNotification(int clsId);
	void createNotification(int clsId, Notification notice);
	void deleteNotification(int id);
	
	
}
