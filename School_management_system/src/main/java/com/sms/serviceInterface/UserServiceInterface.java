package com.sms.serviceInterface;

import java.util.List;
import java.util.Optional;

import com.sms.model.ClassEntity;
import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.User;

public interface UserServiceInterface {
	
	String schoolName();
	
	void initializeClasses();
	
	User getCurrentUser();
	
//	TEACHER CONTROL
	void addTeacher(Teacher teacher, String role);
	List<Teacher> findAllTeachers();
	Optional<Teacher> findTeacherById(int id);
	
//	CLASS CONTROL
	List<ClassEntity> findAllClasses();
	ClassEntity findClassByClsAndSec(String cls,String sec);
	Optional<ClassEntity> findClassById(int id);
	void assignTeacherToClass(int classId, int teacherId);
	
	
//	SSTUDENT CONTROL
	void addStudent(Student student,String role,String classname,String sec);
	List<Student> findAllStudent();
	List<Student> findAllStudentByCls(String cls);
	List<Student> findAllStudentByClsAndSec(String cls,String sec);
	List<Student> findAllStudentBySec(String sec);
	
}
