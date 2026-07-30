package com.sms.serviceInterface;

import java.util.List;
import java.util.Optional;

import com.sms.model.Assignment;
import com.sms.model.Attendence;
import com.sms.model.Notification;
import com.sms.model.NotificationReceiver;
import com.sms.model.Student;
import com.sms.model.StudentAssignment;
import com.sms.model.StudentAssignmentDto;

public interface StudentServiceInterface {

	Student getCurrentStudent();
	
	List<Attendence> findAttendenceByStudentId(int id);
	
	void updateStudent(Student student);
	
	List<Attendence> findTop6AttendenceOfStudentByStudentId(int id);
	
	Optional<Student> findStudentByStudentId(int id);

	String getAcademicSession();

	List<Assignment> findTop3AssignByClsId(int clsId);

	List<StudentAssignment> findStdAssignBtStdId(int id);

	void markAssignmentComplete(int id, int stdId);

	List<StudentAssignment> findAllStudentAssignment(Student student);

	List<StudentAssignmentDto> studentAssignmentDtos(Student student);

	List<StudentAssignmentDto> studentPendingAssignment(Student student);

	boolean findCurrentMongthStudentFeeStatus(int stdId);

	List<NotificationReceiver> studentNotice(Student student);

	void readNotice(int id);

	List<NotificationReceiver> top3notification(Student s);
}
